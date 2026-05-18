import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GamePanel extends JPanel implements KeyListener {
    private static final int TILE_SIZE  = 40;
    private static final int MAP_WIDTH  = 20;
    private static final int MAP_HEIGHT = 15;

    // Objetos interactivos: {x, y, id}
    private static final int[][] OBJECTS = {
        {8,  5,  1},
        {3,  8,  2},
        {14, 10, 3}
    };

    private final Client client;
    private int playerX = 5, playerY = 5;

    public GamePanel(Client client) {
        this.client = client;
        setPreferredSize(new Dimension(MAP_WIDTH * TILE_SIZE, MAP_HEIGHT * TILE_SIZE));
        setBackground(new Color(30, 30, 30));
        setFocusable(true);
        addKeyListener(this);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawGrid(g2);
        drawObjects(g2);
        drawPlayer(g2);
        drawHint(g2);
    }

    private void drawGrid(Graphics2D g) {
        g.setColor(new Color(50, 50, 50));
        for (int x = 0; x < MAP_WIDTH; x++)
            for (int y = 0; y < MAP_HEIGHT; y++)
                g.drawRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
    }

    private void drawObjects(Graphics2D g) {
        for (int[] obj : OBJECTS) {
            int px = obj[0] * TILE_SIZE;
            int py = obj[1] * TILE_SIZE;
            boolean nearby = isNearby(obj[0], obj[1]);

            g.setColor(nearby ? new Color(255, 220, 50) : new Color(180, 150, 30));
            g.fillRoundRect(px + 6, py + 6, TILE_SIZE - 12, TILE_SIZE - 12, 8, 8);

            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 18));
            FontMetrics fm = g.getFontMetrics();
            g.drawString("?", px + (TILE_SIZE - fm.stringWidth("?")) / 2, py + TILE_SIZE / 2 + fm.getAscent() / 2 - 2);
        }
    }

    private void drawPlayer(Graphics2D g) {
        int px = playerX * TILE_SIZE;
        int py = playerY * TILE_SIZE;
        g.setColor(new Color(50, 180, 255));
        g.fillOval(px + 6, py + 6, TILE_SIZE - 12, TILE_SIZE - 12);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2));
        g.drawOval(px + 6, py + 6, TILE_SIZE - 12, TILE_SIZE - 12);
    }

    private void drawHint(Graphics2D g) {
        for (int[] obj : OBJECTS) {
            if (isNearby(obj[0], obj[1])) {
                g.setColor(new Color(255, 255, 255, 200));
                g.setFont(new Font("SansSerif", Font.PLAIN, 11));
                g.drawString("Pulsa E para interactuar", 6, getHeight() - 8);
                break;
            }
        }
    }

    private boolean isNearby(int ox, int oy) {
        return Math.abs(ox - playerX) <= 1 && Math.abs(oy - playerY) <= 1;
    }

    public void updatePlayerPosition(int x, int y) {
        this.playerX = x;
        this.playerY = y;
        SwingUtilities.invokeLater(this::repaint);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:    client.sendMessage("MOVE|UP");    break;
            case KeyEvent.VK_DOWN:  client.sendMessage("MOVE|DOWN");  break;
            case KeyEvent.VK_LEFT:  client.sendMessage("MOVE|LEFT");  break;
            case KeyEvent.VK_RIGHT: client.sendMessage("MOVE|RIGHT"); break;
            case KeyEvent.VK_E:
            case KeyEvent.VK_SPACE:
                for (int[] obj : OBJECTS) {
                    if (isNearby(obj[0], obj[1])) {
                        client.sendMessage("INTERACT|" + obj[2]);
                        break;
                    }
                }
                break;
        }
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
}
