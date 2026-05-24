import java.net.*;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    static final int PORT = 5000;
    static final ConcurrentHashMap<ClientHandler, int[]> playerPositions = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Servidor escuchando en puerto " + PORT);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Cliente conectado: " + clientSocket.getInetAddress());
            ClientHandler handler = new ClientHandler(clientSocket);
            new Thread(handler).start();
        }
    }
}
