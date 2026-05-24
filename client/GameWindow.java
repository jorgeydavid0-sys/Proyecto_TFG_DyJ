import javax.swing.*;
import java.awt.*;

public class GameWindow extends JFrame {

    private final Client     client;
    private final LoginPanel loginPanel;
    private GamePanel        gamePanel;

    public GameWindow(Client client) {
        this.client = client;
        setTitle("Escuela Interactiva 2D");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        loginPanel = new LoginPanel(client);
        setContentPane(loginPanel);
        pack();
        setSize(500, 480);
        setLocationRelativeTo(null);
    }

    // ── Despacho de mensajes del servidor ─────────────────────────────────

    public void handleServerMessage(String message) {
        SwingUtilities.invokeLater(() -> dispatch(message));
    }

    private void dispatch(String message) {
        if (message.startsWith("LOGIN_OK|") || message.startsWith("REGISTER_OK|")) {
            handleLoginOk(message);
        } else if (message.startsWith("LOGIN_ERR|")) {
            loginPanel.setStatus(message.substring(10));
        } else if (message.startsWith("REGISTER_ERR|")) {
            loginPanel.setStatus(message.substring(13));
        } else if (gamePanel != null) {
            gamePanel.handleServerMessage(message);
        }
    }

    private void handleLoginOk(String message) {
        // LOGIN_OK|nombre|color|rol|userId|x|y|zone
        String[] p = message.split("\\|");
        if (p.length < 8) return;
        String nombre = p[1];
        String color  = p[2];
        String rol    = p[3];
        int    userId = Integer.parseInt(p[4]);
        int    px     = Integer.parseInt(p[5]);
        int    py     = Integer.parseInt(p[6]);
        String zone   = p[7];

        gamePanel = new GamePanel(client, nombre, color, rol, userId, px, py, zone);
        setContentPane(gamePanel);
        setSize(840, 600 + 60); // mapa + barra de chat
        setLocationRelativeTo(null);
        revalidate();
        repaint();
        gamePanel.requestFocusInWindow();
    }

    // ── Diálogo de datos (tablón, horario, menú, notas) ──────────────────

    public void showDataDialog(String type, String serialData) {
        SwingUtilities.invokeLater(() -> {
            DataDialog d = new DataDialog(this, type, serialData);
            d.setVisible(true);
        });
    }
}
