import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GamePanel extends JPanel implements KeyListener {
    private static final int TILE_SIZE  = 40;
    private static final int MAP_WIDTH  = 21;
    private static final int MAP_HEIGHT = 15;

    // Scene 1 — entrance doors (8th row from bottom, mirrored left/right)
    private static final int S1_DOOR_ROW = MAP_HEIGHT - 8;          // row 7
    private static final int S1_DOOR_L1  = 6;
    private static final int S1_DOOR_L2  = 7;
    private static final int S1_DOOR_R1  = MAP_WIDTH - 1 - S1_DOOR_L2;  // 13
    private static final int S1_DOOR_R2  = MAP_WIDTH - 1 - S1_DOOR_L1;  // 14

    // Scene 2 — exit doors (1st row from bottom, left and right)
    private static final int S2_DOOR_ROW = MAP_HEIGHT - 1;          // row 14
    private static final int S2_DOOR_L1  = 2;
    private static final int S2_DOOR_L2  = 3;
    private static final int S2_DOOR_R1  = MAP_WIDTH - 1 - S2_DOOR_L2;  // 17
    private static final int S2_DOOR_R2  = MAP_WIDTH - 1 - S2_DOOR_L1;  // 18

    // Scene 2 — spawn (column 2, 2nd row from bottom)
    private static final int S2_SPAWN_X  = 2;
    private static final int S2_SPAWN_Y  = MAP_HEIGHT - 2;          // row 13

    private final Client client;
    private int playerX = MAP_WIDTH / 2;
    private int playerY = MAP_HEIGHT - 2;

    private int     currentScene = 1;
    private int     targetScene  = 1;
    private int     fadeAlpha    = 0;
    private boolean fadingOut    = false;
    private Timer   fadeTimer;

    public GamePanel(Client client) {
        this.client = client;
        setPreferredSize(new Dimension(MAP_WIDTH * TILE_SIZE, MAP_HEIGHT * TILE_SIZE));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (currentScene == 1) drawScene1(g2);
        else                   drawScene2(g2);

        if (fadeAlpha > 0) {
            g2.setColor(new Color(0, 0, 0, Math.min(fadeAlpha, 255)));
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    // ── Scene 1 ──────────────────────────────────────────────────────────────

    private void drawScene1(Graphics2D g) {
        int boundary = (MAP_HEIGHT / 2) * TILE_SIZE;

        g.setColor(new Color(55, 50, 65));
        g.fillRect(0, 0, MAP_WIDTH * TILE_SIZE, boundary);

        g.setColor(new Color(38, 38, 38));
        g.fillRect(0, boundary, MAP_WIDTH * TILE_SIZE, MAP_HEIGHT * TILE_SIZE - boundary);

        g.setColor(new Color(100, 90, 120));
        g.setStroke(new BasicStroke(2));
        g.drawLine(0, boundary, MAP_WIDTH * TILE_SIZE, boundary);

        g.setColor(new Color(55, 55, 55));
        g.setStroke(new BasicStroke(1));
        for (int x = 0; x < MAP_WIDTH; x++)
            for (int y = MAP_HEIGHT / 2; y < MAP_HEIGHT; y++)
                g.drawRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);

        drawDoorPair(g, S1_DOOR_L1, S1_DOOR_L2, S1_DOOR_ROW);
        drawDoorPair(g, S1_DOOR_R1, S1_DOOR_R2, S1_DOOR_ROW);
        drawPlayer(g);
    }

    // ── Scene 2 ──────────────────────────────────────────────────────────────

    private void drawScene2(Graphics2D g) {
        g.setColor(new Color(20, 20, 20));
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(new Color(35, 35, 35));
        g.setStroke(new BasicStroke(1));
        for (int x = 0; x < MAP_WIDTH; x++)
            for (int y = 0; y < MAP_HEIGHT; y++)
                g.drawRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);

        drawScene2Walls(g);
        drawDoorPair(g, S2_DOOR_L1, S2_DOOR_L2, S2_DOOR_ROW);
        drawDoorPair(g, S2_DOOR_R1, S2_DOOR_R2, S2_DOOR_ROW);
        drawPlayer(g);
    }

    private void drawScene2Walls(Graphics2D g) {
        int wallTopY = (MAP_HEIGHT - 3) * TILE_SIZE;  // row 12 (3rd from bottom)
        int wallBotY = MAP_HEIGHT * TILE_SIZE;
        g.setColor(new Color(220, 50, 50));
        g.setStroke(new BasicStroke(3));
        // Left wall: left border of col 6 (between col 5 and col 6)
        g.drawLine(6 * TILE_SIZE, wallTopY, 6 * TILE_SIZE, wallBotY);
        // Right wall (mirror): left border of col 15 (between col 14 and col 15)
        g.drawLine(15 * TILE_SIZE, wallTopY, 15 * TILE_SIZE, wallBotY);
    }

    // ── Door pair (shared) ───────────────────────────────────────────────────

    private void drawDoorPair(Graphics2D g, int col1, int col2, int row) {
        int py = row * TILE_SIZE;
        for (int col : new int[]{col1, col2}) {
            int px = col * TILE_SIZE;
            g.setColor(new Color(80, 60, 40));
            g.fillRect(px, py, TILE_SIZE, TILE_SIZE);
            g.setColor(new Color(140, 100, 60));
            g.setStroke(new BasicStroke(2));
            g.drawRect(px + 3, py + 3, TILE_SIZE - 6, TILE_SIZE - 6);
        }

        boolean onDoor   = playerY == row && (playerX == col1 || playerX == col2);
        boolean nearDoor = !onDoor
            && Math.abs(playerY - row) <= 1
            && (Math.abs(playerX - col1) <= 1 || Math.abs(playerX - col2) <= 1);

        if (onDoor || nearDoor) {
            int    centerX  = (col1 * TILE_SIZE + (col2 + 1) * TILE_SIZE) / 2;
            int    fontSize = onDoor ? 17 : 12;
            String text     = "Entrar";

            g.setFont(new Font("SansSerif", Font.BOLD, fontSize));
            FontMetrics fm = g.getFontMetrics();
            int textX = centerX - fm.stringWidth(text) / 2;
            int textY = row * TILE_SIZE + TILE_SIZE / 2 + fm.getAscent() / 2 - 2;

            g.setColor(new Color(0, 0, 0, 180));
            g.drawString(text, textX + 1, textY + 1);
            g.setColor(onDoor ? new Color(255, 240, 150) : new Color(190, 175, 90));
            g.drawString(text, textX, textY);
        }
    }

    // ── Player ───────────────────────────────────────────────────────────────

    private void drawPlayer(Graphics2D g) {
        int px = playerX * TILE_SIZE;
        int py = playerY * TILE_SIZE;
        g.setColor(new Color(50, 180, 255));
        g.fillOval(px + 6, py + 6, TILE_SIZE - 12, TILE_SIZE - 12);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2));
        g.drawOval(px + 6, py + 6, TILE_SIZE - 12, TILE_SIZE - 12);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isOnAnyDoor() {
        if (currentScene == 1) {
            return playerY == S1_DOOR_ROW
                && (playerX == S1_DOOR_L1 || playerX == S1_DOOR_L2
                 || playerX == S1_DOOR_R1 || playerX == S1_DOOR_R2);
        } else {
            return playerY == S2_DOOR_ROW
                && (playerX == S2_DOOR_L1 || playerX == S2_DOOR_L2
                 || playerX == S2_DOOR_R1 || playerX == S2_DOOR_R2);
        }
    }

    // Returns 0 for left side, 1 for right side
    private int currentDoorSide() {
        if (currentScene == 1) {
            return (playerX == S1_DOOR_L1 || playerX == S1_DOOR_L2) ? 0 : 1;
        } else {
            return (playerX == S2_DOOR_L1 || playerX == S2_DOOR_L2) ? 0 : 1;
        }
    }

    private void startFade(int destination) {
        if (fadeTimer != null && fadeTimer.isRunning()) return;
        int side = currentDoorSide();
        fadingOut   = true;
        fadeAlpha   = 0;
        targetScene = destination;
        fadeTimer   = new Timer(16, e -> {
            if (fadingOut) {
                fadeAlpha += 12;
                if (fadeAlpha >= 255) {
                    fadeAlpha    = 255;
                    fadingOut    = false;
                    currentScene = targetScene;
                    if (targetScene == 2) {
                        playerX = (side == 0) ? S2_DOOR_L1 : S2_DOOR_R2;
                        playerY = S2_DOOR_ROW - 1;
                    } else {
                        playerX = (side == 0) ? S1_DOOR_L1 : S1_DOOR_R2;
                        playerY = S1_DOOR_ROW + 1;
                    }
                    client.sendMessage("TELEPORT|" + playerX + "|" + playerY + "|" + targetScene);
                }
            } else {
                fadeAlpha -= 12;
                if (fadeAlpha <= 0) {
                    fadeAlpha = 0;
                    ((Timer) e.getSource()).stop();
                }
            }
            repaint();
        });
        fadeTimer.start();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void updatePlayerPosition(int x, int y) {
        this.playerX = x;
        this.playerY = y;
        SwingUtilities.invokeLater(this::repaint);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:    client.sendMessage("MOVE|UP");              break;
            case KeyEvent.VK_DOWN:  client.sendMessage("MOVE|DOWN");            break;
            case KeyEvent.VK_LEFT:  client.sendMessage("MOVE|LEFT");            break;
            case KeyEvent.VK_RIGHT: client.sendMessage("MOVE|RIGHT");           break;
            case KeyEvent.VK_ENTER: if (isOnAnyDoor()) startFade(currentScene == 1 ? 2 : 1); break;
        }
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
}
