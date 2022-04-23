//package tcpserver;// TCPServer2.java: Multithreaded server
import java.net.*;
import java.io.*;
import java.util.ArrayList;
/*
*
* TCP SERVER PORT -> 6000
* TCP BackUPServer PORT -> 6055
*
* UDP (heartbeat) [ Server <-> BackUp Server ] Port -> 6050
* UDP (files) Server Port -> 6061
* UDP (files) BackUpServer -> 6060
*
* */
public class TCPServer{
    public ArrayList<Client> allClients;
    private static int serverPort = 6000;
    private static int serverPortUDP = 6050;
    private static int serverPortTransferUDP = 6061;

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

    public static void main(String args[]) throws SocketException, UnknownHostException, InterruptedException {
        TCPServer tcp= new TCPServer();
    }
    public TCPServer() throws SocketException, UnknownHostException, InterruptedException {
        int numero=0;
        allClients= new ArrayList<Client>();

        try (DatagramSocket ds = new DatagramSocket()) {

            detectHeartbeat d=new detectHeartbeat(ds,serverPortUDP);
            DatagramSocket dsf = new DatagramSocket(serverPortTransferUDP);
            ReceiveFile rf =new ReceiveFile(dsf,serverPortTransferUDP);

            d.join();
            //rf.join();
            ds.close();
            //dsf.close();

            DatagramSocket ds2=new DatagramSocket(serverPortUDP);
            new heartBeat(ds2);

            try (ServerSocket listenSocket = new ServerSocket(serverPort)) {
                System.out.println("A escuta no porto 6000");
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

// Thread para tratar de cada canal de comunicação com um cliente
class Connection extends Thread {
    DataInputStream in;
    DataOutputStream out;
    Socket clientSocket;
    ArrayList<Client> allClients;
    int thread_number;
    int serverPortTransferUDP;
    //list files of directory
    public void listDirectory(int i) throws IOException {

        System.out.println("Client [ "+thread_number+" ] ->"+" Estou na diretoria: "+System.getProperty("user.dir"));
        File dirAtual = new File(allClients.get(i).homePATH);
        String[] files=dirAtual.list();
        String send="";
        for (String pathname : files) {
            send+=pathname+"\n";

        }
        System.out.println( "Client [ "+thread_number+" ] ->"+" List of files sent");
        out.writeUTF(send);
    }
    //change directory
    public void changeDirectory(int i) throws IOException {
        System.out.println("Client [ "+thread_number+" ] ->"+" Estou na diretoria: "+System.getProperty("user.dir"));
        String newPath=in.readUTF();
        if(newPath.equals("..")){
            System.out.println(allClients.get(i).homePATH);
            String[] dir=allClients.get(i).homePATH.split("[/\\\\]");
            System.out.println(dir.length);
            if(dir[dir.length-2].equals("ServerHomes" )||dir[dir.length-2].equals("BackUpServerHomes")){
                out.writeUTF("\nJá chegou à Home (não pode andar mais para trás): "+allClients.get(i).homePATH);
            }
            else{
                //System.setProperty("user.dir",new File(System.getProperty("user.dir")).getParent());

                allClients.get(i).setHomePATH(new File(allClients.get(i).homePATH).getParent());
                System.out.println("Client [ "+thread_number+" ] ->"+" Mudei para: "+allClients.get(i).homePATH);
                out.writeUTF("\nDiretoria atual: "+allClients.get(i).homePATH);
            }
        }
        else{
            File newDir= new File( allClients.get(i).homePATH+newPath);
            if(newDir.exists()){
                //System.setProperty("user.dir",System.getProperty("user.dir")+newPath);
                allClients.get(i).setHomePATH(allClients.get(i).homePATH+newPath);
                System.out.println("Client [ "+thread_number+" ] ->"+" Mudei para: "+allClients.get(i).homePATH);
                out.writeUTF("\nDiretoria atual: "+allClients.get(i).homePATH);
            }
            else{
                out.writeUTF("Diretoria pedida não existe");

            }
        }
    }

    //Autentica user
    public int autenticate(String username, String password){
        for(int i=0;i<allClients.size();i++){
            if(allClients.get(i).username.equals(username) && allClients.get(i).password.equals(password)){
                System.out.println("Autenticado Com Sucesso");
                return i;
            }
        }
        System.out.println("Autenticacao Falhada");
        return -1;
    }
    //Connection
    public Connection (Socket aClientSocket, int numero,ArrayList<Client> allClients, int serverPortTransferUDP) {
        thread_number = numero;
        this.allClients=allClients;
        this.serverPortTransferUDP=serverPortTransferUDP;
        try{

            clientSocket = aClientSocket;
            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());
            this.start();


        }catch(IOException e){System.out.println("server.Connection:" + e.getMessage());}
    }
    //RUN
    public void run(){
        System.out.println(allClients);
        String resposta;
        String auxHomePath="";
        try {
            //autentica o cliente
            int autenticate =autenticate(in.readUTF(), in.readUTF());
            while(autenticate==-1){
                out.writeUTF("Tente Novamente\n");
                autenticate =autenticate(in.readUTF(), in.readUTF());
            }
            //vai a diretoria do serverHomes

            //acede a diretoria da Home do cliente pretendido criando-a se nao existir
            File homedir= new File(allClients.get(autenticate).homePATH);
            System.out.println("Client [ "+thread_number+" ] ->"+" deu login :");
            System.out.println("Client [ "+thread_number+" ] ->"+"Home: "+homedir);
            if(!homedir.exists()){
                if(homedir.mkdir()){
                    System.out.println("Directory has been created");
                }else{
                    System.out.println("Directory cannot be created");
                }
            }
            else{
                System.out.println("Directory already exists");
            }
            //Coloca cliente na home
            System.setProperty("user.dir",allClients.get(autenticate).homePATH);
            out.writeUTF("Home\n"+"PATH: "+allClients.get(autenticate).homePATH+"\n");

            while (true) {

                int op =in.read();
                if(op == 1){
                    String newPass = in.readUTF();
                    if(!newPass.equals(allClients.get(autenticate).getPassword())){
                        allClients.get(autenticate).setPassword(newPass);
                        out.writeUTF("\nPassword atualizada");
                        FileWriter change = new FileWriter("utilizadores.txt", false);
                        for(Client c: allClients ){
                            change.write(c.username+" "+c.password+" "+c.dep+" "+c.numeroT+" "+c.morada+" "+c.validadeCC+"\n");
                        }
                        change.close();
                        clientSocket.close();
                        this.interrupt();
                        break;

                    }else{
                        out.writeUTF("Password igual à existente");
                    }

                }
                else if(op == 3){

                   listDirectory(autenticate);
                }
                else if(op == 4){

                    changeDirectory(autenticate);
                }
                else if(op == 5){

                    //listClDirectory(autenticate);

                }
                else if(op == 6){

                   //changeClDirectory(autenticate);
                }
                else if(op == 7){
                    out.writeUTF(allClients.get(autenticate).homePATH);

                    try(ServerSocket listenSocket = new ServerSocket(6002)){
                        Socket clientSocket = listenSocket.accept();
                        TransferD t = new TransferD(clientSocket,this.thread_number,allClients.get(autenticate));
                        t.join();
                        clientSocket.close();


                    }catch(IOException e) {
                        System.out.println("Listen:" + e.getMessage());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                else if(op == 8){
                    out.writeUTF(allClients.get(autenticate).homePATH);
                    try(ServerSocket listenSocket = new ServerSocket(6006)){
                        Socket clientSocket = listenSocket.accept();
                        TransferU t = new TransferU(clientSocket,this.thread_number,allClients.get(autenticate),serverPortTransferUDP);

                        t.join();
                        clientSocket.close();

                    }catch(IOException e) {
                        System.out.println("Listen:" + e.getMessage());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                else if(op == 9){
                    System.out.println("Client [ "+thread_number+" ] ->"+" logged out");
                    clientSocket.close();
                    this.interrupt();
                    break;
                }
                else{
                    out.writeUTF("Opcção Inválida");

                }

            }

        } catch(EOFException e) {
            System.out.println("EOF:" + e);
        } catch(IOException e) {
            System.out.println("IO:" + e);
        }
    }
}
// Thread para tratar do downlaoad
class TransferD extends Thread{
    DataInputStream in;
    DataOutputStream out;
    Socket clientSocket;
    Client c;
    int thread_number;

    public TransferD (Socket aClientSocket, int numero,Client c) {
        thread_number = numero;
        this.c=c;
        try{

            clientSocket = aClientSocket;
            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());
            this.start();

        }catch(IOException e){System.out.println("server.Connection:" + e.getMessage());}
    }
    public void run(){

        try {

            String fileName=in.readUTF();
            System.out.println(fileName);
            String filePath=c.homePATH+"/"+fileName;

            File file=new File(filePath);
            if(file.exists()){
                System.out.println("Client [ "+thread_number+" ] ->"+" Encontrei Ficheiro transferencia ira ser iniciada");
                out.writeUTF("Ficheiro pretendido encontrado");
                FileInputStream fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis);
                OutputStream os = clientSocket.getOutputStream();
                byte[] contents;
                long fileLength = file.length();
                long current = 0;
                long start = System.nanoTime();
                while(current!=fileLength){
                    int size = 10000;
                    if(fileLength - current >= size)
                        current += size;
                    else{
                        size = (int)(fileLength - current);
                        current = fileLength;
                    }
                    contents = new byte[size];
                    bis.read(contents, 0, size);
                    os.write(contents);
                    System.out.print("Client [ "+thread_number+" ] ->"+" Downloading file ... "+(current*100)/fileLength+"% complete!\n");
                }
                os.flush();
            }else{
                System.out.println("Client [ "+thread_number+" ] ->"+" File does not exist");
                out.writeUTF("File not found");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
// Thread para tratar do upload
class TransferU extends Thread {
    DataInputStream in;
    DataOutputStream out;
    Socket ServerSocket;
    Client c;
    int thread_number;
    int serverPortUdptransfer;

    public TransferU(Socket aServerSocket, int numero, Client c, int serverPortUdptransfer) {
        thread_number = numero;
        this.c = c;
        this.serverPortUdptransfer=serverPortUdptransfer;
        try {

            ServerSocket = aServerSocket;
            in = new DataInputStream(ServerSocket.getInputStream());
            out = new DataOutputStream(ServerSocket.getOutputStream());
            this.start();

        } catch (IOException e) {
            System.out.println("server.Connection:" + e.getMessage());
        }
    }

    //send do secundary server
    public void send2Secondary(String filePath){
        try (DatagramSocket aSocket = new DatagramSocket()) {
            int sendPort=0;
            if(this.serverPortUdptransfer==6061){
                sendPort=6060;
            }
            else{
                sendPort=6061;
            }

            String texto = filePath;
            byte [] m = texto.getBytes();

            InetAddress aHost = InetAddress.getByName("localhost");

            //envia diretoria
            while(true){

                DatagramPacket request = new DatagramPacket(m,m.length,aHost,sendPort);
                aSocket.send(request);

                byte[] buffer = new byte[1000];
                DatagramPacket ack = new DatagramPacket(buffer, buffer.length);
                aSocket.receive(ack);
                String s1=new String(request.getData(), 0, request.getLength());
                String s2=new String(ack.getData(), 0, ack.getLength());

                if(s1.equals(s2)){
                    System.out.println("Recebeu Ack: " + new String(ack.getData(), 0, ack.getLength()));

                    break;
                }

            }

            //envia tamanho fich
            File upload = new File(filePath);
            String  fichLen = Long.toString(upload.length());
            byte [] m2 = fichLen.getBytes();
            DatagramPacket len = new DatagramPacket(m2,m2.length,aHost,sendPort);
            aSocket.send(len);

            //envia ficheiro
            FileInputStream fis = new FileInputStream(upload);
            BufferedInputStream bis = new BufferedInputStream(fis);
            byte[] contents2;
            long fileLength = upload.length();
            long current = 0;
            long start = System.nanoTime();
            while (current != fileLength) {
                int size = 10000;
                if (fileLength - current >= size)
                    current += size;
                else {
                    size = (int) (fileLength - current);
                    current = fileLength;
                }
                contents2 = new byte[size];
                bis.read(contents2, 0, size);


                DatagramPacket request = new DatagramPacket(contents2,contents2.length,aHost,sendPort);
                aSocket.send(request);

                byte[] buffer2 = new byte[10000];
                DatagramPacket ack = new DatagramPacket(buffer2, buffer2.length);
                aSocket.receive(ack);
                System.out.println(ack.getData().length);



                System.out.print("Passing file to BackUp ... " + (current * 100) / fileLength + "% complete!\n");
            }


        }catch (SocketException e){
            System.out.println("Socket: " + e.getMessage());
        }catch (IOException e){
            System.out.println("IO: " + e.getMessage());
        }catch (NumberFormatException e) {
            System.out.println("Nao e numero");
        }

    }

    public void run() {

        try {

            String fileName = in.readUTF();
            System.out.println(fileName);
            String filePath = c.homePATH + "/" + fileName;
            System.out.println(filePath);

            String response=in.readUTF();


            if(!response.equals("File not found")){
                byte[] contents = new byte[10000];
                File upFile=new File(filePath);
                if(upFile.exists()){
                    out.writeUTF("File already uploaded, won't be saved again");
                    System.out.println("Client [ "+thread_number+" ] ->"+" File already uploaded, won't be saved again");
                }
                else{
                    out.writeUTF("a");
                    FileOutputStream fos = new FileOutputStream(upFile);
                    BufferedOutputStream bos = new BufferedOutputStream(fos);
                    InputStream is = ServerSocket.getInputStream();

                    int bytesRead = 0;
                    while((bytesRead=is.read(contents))!=-1)
                        bos.write(contents, 0, bytesRead);
                    bos.flush();
                    ServerSocket.close();
                    System.out.println("Client [ "+thread_number+" ] -> "+"File saved successfully!");

                    send2Secondary(filePath);

                }

            }else{
                System.out.println(response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
// HeartBeat
class heartBeat extends Thread{
    DatagramSocket ds;
    int bufsize=4096;
    public heartBeat(DatagramSocket ds){
        this.ds=ds;
        this.start();
    }
    public void run(){
        while (true) {
            try{
                byte buf[] = new byte[bufsize];
                DatagramPacket dp = new DatagramPacket(buf, buf.length);
                ds.receive(dp);
                ByteArrayInputStream bais = new ByteArrayInputStream(buf, 0, dp.getLength());
                DataInputStream dis = new DataInputStream(bais);
                int count = dis.readInt();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);
                dos.writeInt(count);
                byte resp[] = baos.toByteArray();
                DatagramPacket dpresp = new DatagramPacket(resp, resp.length, dp.getAddress(), dp.getPort());
                System.out.println("Enviei para BackServer "+dpresp);
                ds.send(dpresp);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
// Cliente data
class Client{
    protected String username;
    protected String password;
    protected String dep;
    protected String numeroT;
    protected String morada;
    protected String validadeCC;
    protected String homePATH=new File(System.getProperty("user.dir")).getParent()+"/ServerHomes";
    protected String path="/Users/goncalocorreia/Desktop/Clients";

    public Client(String username, String password,String dep,String numeroT,String morada,String validadeCC) {
        this.username=username;
        this.password=password;
        this.dep=dep;
        this.numeroT=numeroT;
        this.morada=morada;
        this.validadeCC=validadeCC;
        this.homePATH=this.homePATH+"/"+username;
        this.path=path+"/"+username;
    }

    @Override
    public String toString() {
        return "Client{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", dep='" + dep + '\'' +
                ", numeroT='" + numeroT + '\'' +
                ", morada='" + morada + '\'' +
                ", validadeCC='" + validadeCC + '\'' +
                '}';
    }

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDep() {
        return dep;
    }

    public void setDep(String dep) {
        this.dep = dep;
    }

    public String getNumeroT() {
        return numeroT;
    }

    public void setNumeroT(String numeroT) {
        this.numeroT = numeroT;
    }

    public String getMorada() {
        return morada;
    }

    public void setMorada(String morada) {
        this.morada = morada;
    }

    public String getValidadeCC() {
        return validadeCC;
    }

    public void setValidadeCC(String validadeCC) {
        this.validadeCC = validadeCC;
    }

    public String getHomePATH() {
        return homePATH;
    }

    public void setHomePATH(String homePATH) {
        this.homePATH = homePATH;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}