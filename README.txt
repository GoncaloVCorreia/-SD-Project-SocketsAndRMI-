Correr sem .jar:

Compilar os ficheiros -> javac *.java

Correr o servidor -> java -cp . TCPServer
Correr o backUp server -> java -cp . BackUpServer
Correr o cliente -> java -cp . TCPClient port (port = 6000 caso seja o TCPServer o servidor primário,
                                               port = 6055 caso seja o BackUpServer o servidor primário)

Correr com .jar:

Os ficheiros .jar para o servidor primário, o cliente e o servidor secundário estão gerados na diretoria ProjetoSD\out\artifacts

Para estes serem executados colocámos os "META-INF/MANIFEST.MF" dos 3 mains (server, cliente e backup) em pastas diferentes,
pois os Manifests são gerados automaticamente e, desta maneira, não dão override ao serem gerados

Para correr com os ficheiros .jar basta clicar em "edit configurations" ao lado esquerdo do botão de Run, depois onde diz "Path to jar"
coloque a diretoria onde estão os ficheiros jar, por exemplo, "..\ProjetoSD\out\artifacts\Client\ProjetoSD.jar". De seguida, na
"working directory" tem de mudar o path para acabar em "..\src". Por último, em baixo, onde diz "before launch", clique no "+" e
depois no "build artifacts" e selecione o artifact específico desse jar. Replique estes passos para os 3 ficheiros (server,backup
e cliente) sendo a unica diferença no cliente onde temos de meter como "program arguments" o porto, neste caso, o 6000 caso o "TCPServer" seja
o primário ou 6055 caso seja o "BackUpServer" o primário.

Concluindo estes passos, pode selecionar qualquer ficheiro criado no "edit configurations" que queira correr e clicar no botão de Run.

Guia de utilização:

1º Correr os dois servidores (não importa a ordem)
2º Correr o cliente indicando o porto como parâmetro (6000 casa o server primário seja o TCPServer e 6055 caso seja o BackUpServer)
3º Após o início do cliente irá ser pedido autenticação, sendo usado um dos utilizadores presentes no ficheiro "utilizadores.txt" para se autenticar
4º Após autenticação irá aparecer um menu com as várias opções
5º Na operação de mudar de diretoria(quer do cliente quer do server), para andar para a frente basta colocar "/dir_name" e para andar para trás ".."

Nota: Dentro do projeto terá de haver uma pasta "ServerHomes" e uma pasta "BackUpServerHomes", dentro da Home de cada cliente as diretorias, a título de
exemplo, foram criadas à mão, tanto no "ServerHomes" como no "BackUpServerHomes".