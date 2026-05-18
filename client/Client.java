import java.net.*;
import java.io.*;

public class Client {
    static final String HOST = "localhost";
    static final int    PORT = 5000;

    private Socket socket;
    private PrintWriter out;
    private GameWindow gameWindow;

    public void connect() throws IOException {
        socket = new Socket(HOST, PORT);
        out    = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        gameWindow = new GameWindow(this);
        gameWindow.setVisible(true);

        new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    gameWindow.handleServerMessage(message);
                }
            } catch (IOException e) {
                System.out.println("Desconectado del servidor");
            }
        }).start();
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public static void main(String[] args) throws IOException {
        new Client().connect();
    }
}
