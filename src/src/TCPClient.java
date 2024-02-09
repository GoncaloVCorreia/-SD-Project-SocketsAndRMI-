//package client;

import java.net.*;
import java.util.Scanner;
import java.io.*;

public class TCPClient {

	private static int serversocket = 6000;
	private static int serversocket2 = 6055;
	
	public static void main(String args[]) {
		if (args.length == 0) {
			System.out.println("java TCPClient port");
			System.exit(0);
		}
		else{
			try{
				TCPClient tcp = new TCPClient(Integer.parseInt(args[0]));

			}catch (NumberFormatException e) {
				System.out.println("Arg Invalido (Nao e um numero)");
			}
		}
	}
	public TCPClient (int serversocket){

		
		// 1o passo - criar socket
		try (Socket s = new Socket("localhost", serversocket)) {
			System.out.println("SOCKET=" + s);

			// 2o passo
			DataInputStream in = new DataInputStream(s.getInputStream());
			DataOutputStream out = new DataOutputStream(s.getOutputStream());

			// 3o passo
			int verify=0;
			while(verify==0) {
				Scanner scA = new Scanner(System.in);
				System.out.print("Username: ");
				String utilizador = scA.nextLine();
				System.out.print("Password: ");
				String password = scA.nextLine();
				out.writeUTF(utilizador);
				out.writeUTF(password);

				String response = in.readUTF();

				System.out.print(response);

				if(response.equals("Tente Novamente\n")){
					verify=0;
				}
				else{
					verify=1;
				}
			}
			String clientPATH=System.getProperty("user.dir");
			System.out.print("Client: "+clientPATH);


			while(true) {
				System.out.println("\n>>>>>>>>>>>>Menu<<<<<<<<<<<<<\n" +
						"1 - Mudar Password\n" +
						"2 - Configurar Portos \n" +
						"3 - Listar os ficheiros que existem na diretoria atual do servidor\n" +
						"4 - Mudar a diretoria atual do servidor\n" +
						"5 - Listar os ficheiros que existem na diretoria atual do cliente\n" +
						"6 - Mudar a diretoria atual do cliente\n" +
						"7 - Descarregar um ficheiro do servidor\n" +
						"8 - Carregar um ficheiro para o servido\n"+
						"9 - Sair\n");

				System.out.print("Escolha uma opção: ");

				Scanner scOp = new Scanner(System.in);
				try{
					int op = Integer.parseInt(scOp.nextLine());
					out.write(op);

					if (op == 1){
						System.out.print("Insira a nova pasword: ");
						String newPass=scOp.nextLine();
						out.writeUTF(newPass);
						String res=in.readUTF();
						System.out.println(res);
						if(!res.equals("Password igual à existente")) {
							System.out.println("entrei");
							s.close();
							new TCPClient(serversocket);
						}

					}
					else if (op == 3){
						System.out.println(in.readUTF());
					}
					else if (op == 4){
						System.out.print("Qual a diretoria que pretende: ");
						String newDir=scOp.nextLine();
						out.writeUTF(newDir);
						System.out.println(in.readUTF());
					}
					else if (op == 5){
						//System.out.println(in.readUTF());
						String path=clientPATH;
						File clientLocal=new File(path);
						String dir="";
						System.out.println(clientLocal);
						if(clientLocal.exists()){
							String[] dirs=clientLocal.list();
							for (String pathname : dirs) {
								dir+=pathname+"\n";
							}
							System.out.println(dir);
						}
						else{
							out.writeUTF("Nao consegue aceder a caminho");
						}
					}
					else if (op == 6){

						System.out.print("Qual a diretoria que pretende: ");
						String newPath=scOp.nextLine();

						if(newPath.equals("..")){
							String newP=new File(clientPATH).getParent();

							if(newP==null){
								System.out.println("Nao pode ir mais para trás");
							}
							else{
								clientPATH=newP;
								System.out.println(clientPATH);
							}

						}
						else{
							File newDir= new File(clientPATH+newPath);
							if(newDir.exists()){
								clientPATH=clientPATH+newPath;
								System.out.println("\nDiretoria atual do Cliente: "+clientPATH);
							}
							else{
								System.out.println("Diretoria pedida não existe");

							}
						}
					}
					else if (op == 7){
						System.out.println("Encontra-se na diretoria do Server-> "+in.readUTF()+"\n");

						System.out.println("Minha Diretoria-> "+clientPATH+"\n");

						try (Socket s2 = new Socket("localhost", 6002)) {

							DataInputStream in2 = new DataInputStream(s2.getInputStream());
							DataOutputStream out2 = new DataOutputStream(s2.getOutputStream());
							System.out.print("Qual ficheiro quer fazer Download? ");
							String fileName=scOp.nextLine();
							out2.writeUTF(fileName);
							String response=in2.readUTF();
							if(!response.equals("File not found")){
								byte[] contents = new byte[10000];
								File dwFile=new File(clientPATH+"/"+fileName);
								if(dwFile.exists()){
									System.out.println("File already downloaded, won't be saved again");
								}
								else{
									FileOutputStream fos = new FileOutputStream(dwFile);
									BufferedOutputStream bos = new BufferedOutputStream(fos);
									InputStream is = s2.getInputStream();
									int bytesRead = 0;
									while((bytesRead=is.read(contents))!=-1)
										bos.write(contents, 0, bytesRead);
									bos.flush();
									s2.close();
									System.out.println("File saved successfully!");
								}

							}else{
								System.out.println(response);
							}
						} catch (UnknownHostException e) {
						System.out.println("Sock:" + e.getMessage());
						} catch (EOFException e) {
							System.out.println("EOF:" + e.getMessage());
						} catch (IOException e) {
							System.out.println("IO:" + e.getMessage());
						}

					}
					else if (op == 8){
						String ServerDir=in.readUTF();
						System.out.println("Encontra-se na diretoria do Server-> " + ServerDir + "\n");

						System.out.println("Minha Diretoria-> " + clientPATH + "\n");

						try (Socket s3 = new Socket("localhost", 6006)) {

							DataInputStream in2 = new DataInputStream(s3.getInputStream());
							DataOutputStream out2 = new DataOutputStream(s3.getOutputStream());
							System.out.print("Qual ficheiro quer fazer Upload? ");
							String fileName=scOp.nextLine();
							out2.writeUTF(fileName);

							File upload = new File(clientPATH+"/"+fileName);

							if (upload.exists()) {

								System.out.println("Encontrei Ficheiro");
								out2.writeUTF("Ficheiro pretendido encontrado");
								String ack = in2.readUTF();
								if (!ack.equals("File already uploaded, won't be saved again")) {

									FileInputStream fis = new FileInputStream(upload);
									BufferedInputStream bis = new BufferedInputStream(fis);
									OutputStream os = s3.getOutputStream();
									byte[] contents;
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
										contents = new byte[size];
										bis.read(contents, 0, size);
										os.write(contents);
										System.out.print("Uploading file ... " + (current * 100) / fileLength + "% complete!\n");
									}
									os.flush();
								}else{
									System.out.println("File already uploaded");
								}

							} else {
								System.out.println("File does not exist");
								out2.writeUTF("File not found");
							}



						} catch (UnknownHostException e) {
							System.out.println("Sock:" + e.getMessage());
						} catch (EOFException e) {
							System.out.println("EOF:" + e.getMessage());
						} catch (IOException e) {
							System.out.println("IO:" + e.getMessage());
						}
					}
					else if (op == 9){
						System.out.println("Exiting...");
						s.close();
						System.exit(0);
					}
					else{
						System.out.println(in.readUTF());
					}

				} catch (NumberFormatException e) {
					System.out.println("Opcção Inválida (Nao e um numero)");
				} catch (IOException e) {
					if(serversocket==this.serversocket2){
						serversocket=this.serversocket;
					}else{
						serversocket=this.serversocket2;
					}
					new TCPClient(serversocket);
					System.out.println("Loading...");

				}
			}

		} catch (UnknownHostException e) {
			System.out.println("Sock:" + e.getMessage());
		} catch (EOFException e) {
			System.out.println("EOF:" + e.getMessage());
		} catch (IOException e) {
			System.out.println("Loading...");
		}
	}
}