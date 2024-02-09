//package tcpserver;// TCPServer2.java: Multithreaded server
import java.net.*;
import java.io.*;
import java.util.ArrayList;

public class BackUpServer{
    public ArrayList<Client> allClients;

    private static int serverPortTCP = 6055;
    private static int serverPortUDP = 6050;
    private static int serverPortTransferUDP = 6060;

    //funcao que coloca info num ArrayList
    public void getClientData(){
        File f = new File("utilizadores.txt");
        if (f.exists() && f.isFile()) {
            try {
                FileReader fr = new FileReader(f);
                BufferedReader br = new BufferedReader(fr);
                String line;
                while ((line = br.readLine()) != null) {

                    String[] strs;

                    strs = line.split(" ");
                    Client c=new Client(strs[0],strs[1],strs[2],strs[3],strs[4],strs[5]);
                    c.setHomePATH(new File(System.getProperty("user.dir")).getParent()+"/BackUpServerHomes/"+c.username);
                    allClients.add(c);
                }
                br.close();

                //possiveis erros
            } catch (FileNotFoundException ex) {
                System.out.println("Erro a abrir ficheiro de texto.");
            } catch (IOException ex ) {
                System.out.println("Erro a ler ficheiro de texto.");
            }
        } else {
            System.out.println("Ficheiro nao existe.");

        }

    }

    public static void main(String args[]) throws IOException, InterruptedException {
        BackUpServer tcp= new BackUpServer();
    }
    public BackUpServer()  throws IOException, InterruptedException {
        int numero=0;

        allClients= new ArrayList<Client>();

        try(DatagramSocket ds = new DatagramSocket()){

            detectHeartbeat d=new detectHeartbeat(ds,serverPortUDP);

            DatagramSocket dsf = new DatagramSocket(serverPortTransferUDP);
            ReceiveFile rf =new ReceiveFile(dsf,serverPortTransferUDP);

            d.join();
            //rf.join();
            ds.close();
            //dsf.close();

            DatagramSocket ds2=new DatagramSocket(serverPortUDP);
            new heartBeat(ds2);

            try (ServerSocket listenSocket = new ServerSocket(serverPortTCP)) {

                System.out.println("A escuta no porto "+serverPortTCP);
                System.out.println("user.dir: "+System.getProperty("user.dir"));
                System.out.println("LISTEN SOCKET=" + listenSocket);
                getClientData();

                while(true) {

                    Socket clientSocket = listenSocket.accept(); // BLOQUEANTE
                    System.out.println("CLIENT_SOCKET (created at accept())="+clientSocket);
                    numero++;

                    new Connection(clientSocket, numero,allClients,serverPortTransferUDP);

                }

            } catch(IOException e) {
                System.out.println("Listen:" + e.getMessage());
            }
        }
    }
}

//Deteta hearbeat
class detectHeartbeat extends Thread{
    DatagramSocket ds;
    int bufsize=4096;
    int maxfailedrounds = 5;
    int period = 1000;
    int timeout = 250;
    int serverPortUDP;
    InetAddress ia = InetAddress.getByName("localhost");
    int count = 1;
    public detectHeartbeat(DatagramSocket ds,int serverPortUDP) throws UnknownHostException {
        this.ds=ds;
        this.start();
        this.serverPortUDP=serverPortUDP;
    }
    public void run(){
        int failedheartbeats = 0;
        while (failedheartbeats < maxfailedrounds) {
            try {
                ds.setSoTimeout(timeout);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);
                dos.writeInt(count++);
                byte [] buf = baos.toByteArray();

                DatagramPacket dp = new DatagramPacket(buf, buf.length, ia, serverPortUDP);
                ds.send(dp);

                byte [] rbuf = new byte[bufsize];
                DatagramPacket dr = new DatagramPacket(rbuf, rbuf.length);

                ds.receive(dr);
                failedheartbeats = 0;
                ByteArrayInputStream bais = new ByteArrayInputStream(rbuf, 0, dr.getLength());
                DataInputStream dis = new DataInputStream(bais);
                int n = dis.readInt();
                System.out.println("Got: " + n + ". "+dr.getPort());
            }
            catch (SocketTimeoutException ste) {
                failedheartbeats++;
                System.out.println("Failed heartbeats: " + failedheartbeats);
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(period);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

}

class ReceiveFile extends Thread{
    DatagramSocket dsf;
    int serverPortTransferUDP;
    public ReceiveFile(DatagramSocket dsf,int serverPortTransferUDP){
        this.dsf=dsf;
        this.serverPortTransferUDP=serverPortTransferUDP;
        this.start();
    }
    public void run(){
        while(true){
            System.out.println("Socket Datagram à escuta no porto " + serverPortTransferUDP);
            try {
                //Recebe diretoria
                byte[] buffer = new byte[1000];
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                dsf.receive(request);
                String s = new String(request.getData(), 0, request.getLength());
                System.out.println("BackUPServer Recebeu: " + s);


                DatagramPacket ack = new DatagramPacket(request.getData(),
                        request.getLength(), request.getAddress(), request.getPort());
                dsf.send(ack);

                //coloca sua diretoria
                String[] path =s.split("/");
                String finalPath="";
                for(int i=0;i<path.length;i++){

                    if(path[i].equals("ServerHomes")){
                        path[i]="BackUpServerHomes";
                    }
                    else if(path[i].equals("BackUpServerHomes")){
                        path[i]="ServerHomes";
                    }
                    if(i< (path.length-1)){
                        finalPath+=path[i]+"/";
                    }else{
                        finalPath+=path[i];
                    }
                }
                System.out.println("My true path "+finalPath);


                //Cria ficheiro
                File recFile=new File(finalPath);
                FileOutputStream fos = new FileOutputStream(recFile);
                BufferedOutputStream bos = new BufferedOutputStream(fos);

                int bytesRead = 0;
                byte[] contents = new byte[10000];

                //recebe tamanho do ficheiro
                DatagramPacket fileLen = new DatagramPacket(contents, contents.length);
                dsf.receive(fileLen);
                String fileLenght=new String(fileLen.getData(),0,fileLen.getLength());
                int len=Integer.parseInt(fileLenght);
                System.out.println("Len "+len);

                //escreve no ficheiro
                while(len>0) {
                    DatagramPacket request2 = new DatagramPacket(contents, contents.length);
                    dsf.receive(request2);

                    DatagramPacket ack2 = new DatagramPacket(request2.getData(),
                            request2.getLength(), request2.getAddress(), request2.getPort());
                    dsf.send(ack2);
                    System.out.println(ack2.getData().length);
                    bos.write(contents, 0, request2.getData().length);
                    len-=request2.getData().length;
                }
                bos.flush();

                System.out.println("File saved successfully!");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }



    }
}