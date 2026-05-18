import java.net.*;
import java.io.*;

public class ClientHandler implements Runnable {
    private static final int MAP_WIDTH  = 20;
    private static final int MAP_HEIGHT = 15;

    private final Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private int x = 5, y = 5;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("POSITION|" + x + "|" + y);

            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Recibido: " + message);
                handleMessage(message);
            }
        } catch (IOException e) {
            System.out.println("Cliente desconectado");
        }
    }

    private void handleMessage(String message) {
        String[] parts = message.split("\\|", 2);
        switch (parts[0]) {
            case "MOVE":     handleMove(parts[1]);     break;
            case "INTERACT": handleInteract(parts[1]); break;
        }
    }

    private void handleMove(String direction) {
        switch (direction) {
            case "UP":    if (y > 0)              y--; break;
            case "DOWN":  if (y < MAP_HEIGHT - 1) y++; break;
            case "LEFT":  if (x > 0)              x--; break;
            case "RIGHT": if (x < MAP_WIDTH  - 1) x++; break;
        }
        out.println("POSITION|" + x + "|" + y);
        System.out.println("Posición: " + x + ", " + y);
    }

    private void handleInteract(String objectId) {
        // TODO: consultar PostgreSQL según objectId
        String title   = "Objeto " + objectId;
        String content = "Información del objeto " + objectId + " (pendiente conectar con la BD)";
        out.println("OBJECT_DATA|" + title + "|" + content);
    }
}
