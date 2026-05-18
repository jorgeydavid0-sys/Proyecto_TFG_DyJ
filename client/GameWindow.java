import javax.swing.*;
import java.awt.*;

public class GameWindow extends JFrame {
    private final GamePanel gamePanel;

    public GameWindow(Client client) {
        setTitle("Proyecto TFG - Cliente");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        gamePanel = new GamePanel(client);
        add(gamePanel);

        pack();
        setLocationRelativeTo(null);
        setResizable(false);
    }

    public void handleServerMessage(String message) {
        String[] parts = message.split("\\|", 3);
        switch (parts[0]) {
            case "POSITION":
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                gamePanel.updatePlayerPosition(x, y);
                break;
            case "OBJECT_DATA":
                showObjectDialog(parts[1], parts[2]);
                break;
        }
    }

    private void showObjectDialog(String title, String content) {
        SwingUtilities.invokeLater(() -> {
            JDialog dialog = new JDialog(this, title, false);
            dialog.setLayout(new BorderLayout());

            JTextArea text = new JTextArea(content);
            text.setEditable(false);
            text.setLineWrap(true);
            text.setWrapStyleWord(true);
            text.setMargin(new Insets(10, 10, 10, 10));
            text.setFont(new Font("SansSerif", Font.PLAIN, 13));

            dialog.add(new JScrollPane(text), BorderLayout.CENTER);
            dialog.setSize(320, 180);
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        });
    }
}
