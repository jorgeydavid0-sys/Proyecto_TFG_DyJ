import java.net.*;
import java.io.*;

public class Server {
    static final int PORT = 5000;

    // Estado global y BD compartidos entre todos los hilos
    static final GameState state = new GameState();
    static final DBManager db    = new DBManager();

    public static void main(String[] args) throws IOException {
        System.out.println("==============================================");
        System.out.println("  Escuela Interactiva 2D — Servidor Java");
        System.out.println("==============================================");
        db.connect();
        System.out.println("  Puerto: " + PORT);
        System.out.println("----------------------------------------------");
        System.out.println("  Esperando conexiones...");

        ServerSocket serverSocket = new ServerSocket(PORT);
        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("  Nueva conexion: " + clientSocket.getInetAddress());
            new Thread(new ClientHandler(clientSocket)).start();
        }
    }
}
