import java.net.*;
import java.io.*;

public class Client {
    static final String HOST = "192.168.11.153";
    static final int    PORT = 5000;

    private Socket     socket;
    private PrintWriter out;
    private GameWindow  gameWindow;

    public void connect() throws IOException {
        socket     = new Socket(HOST, PORT);
        out        = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
        gameWindow = new GameWindow(this);
        gameWindow.setVisible(true);

        new Thread(() -> {
            try {
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), "UTF-8"));
                String line;
                while ((line = in.readLine()) != null)
                    gameWindow.handleServerMessage(line);
            } catch (IOException e) {
                System.out.println("Desconectado del servidor");
            }
        }).start();
    }

    public void sendMessage(String message) {
        if (out != null) out.println(message);
    }

    public static void main(String[] args) throws IOException {
        new Client().connect();
    }
}
