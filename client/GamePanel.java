import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class GamePanel extends JPanel implements KeyListener {

    // ── Constantes de mapa ─────────────────────────────────────────────
    static final int TILE_SIZE  = 40;
    static final int MAP_COLS   = 21;
    static final int MAP_ROWS   = 15;
    static final int W          = MAP_COLS * TILE_SIZE;  // 840
    static final int H          = MAP_ROWS * TILE_SIZE;  // 600

    // ── Escena 1: puertas de entrada (fila 7) ─────────────────────────
    static final int S1_DOOR_ROW = 7;
    static final int S1_DOOR_L1 = 6,  S1_DOOR_L2 = 7;
    static final int S1_DOOR_R1 = 13, S1_DOOR_R2 = 14;

    // ── Escena 2: puertas de salida (fila 14) y puerta izq (col 0) ───
    static final int S2_EXIT_ROW  = 14;
    static final int S2_EXIT_L1   = 2,  S2_EXIT_L2  = 3;
    static final int S2_EXIT_R1   = 17, S2_EXIT_R2  = 18;
    static final int S2_LEFT_DOOR_COL = 0;

    // ── Escena 3: puerta derecha (col 20) ─────────────────────────────
    static final int S3_RIGHT_DOOR_COL = 20;

    // ── Mapas de tiles ─────────────────────────────────────────────────
    private final int[][] MAP_ENTRADA     = buildEntradaMap();
    private final int[][] MAP_RECEPCION   = buildRecepcionMap();
    private final int[][] MAP_CONSERJERIA = buildConserjeriaMap();

    // ── Estado del jugador local ───────────────────────────────────────
    private final Client client;
    private final String myName, myRol;
    private final Color  myColor;
    private final int    myUserId;
    private int          playerX, playerY;
    private String       direction = "down";
    private int          animTimer = 0;
    private boolean      moving    = false;
    private String       currentZone;

    // ── Otros jugadores ────────────────────────────────────────────────
    private final ConcurrentHashMap<String, PlayerData> otherPlayers = new ConcurrentHashMap<>();

    // ── Fundido ────────────────────────────────────────────────────────
    private int     fadeAlpha   = 0;
    private boolean fadingOut   = false;
    private String  fadeTarget  = null;
    private int     fadeSpawnX, fadeSpawnY;
    private Timer   fadeTimer;

    // ── Chat ───────────────────────────────────────────────────────────
    private final List<String[]> chatMessages = new ArrayList<>(); // [nombre, color, texto]
    private boolean chatActive = false;
    private final StringBuilder chatInput = new StringBuilder();

    // ── Input ──────────────────────────────────────────────────────────
    private final Set<Integer> pressedKeys = new HashSet<>();
    private Timer moveTimer;

    // ── Interacción ────────────────────────────────────────────────────
    private String nearbyInteraction = null;

    public GamePanel(Client client, String name, String color, String rol,
                     int userId, int x, int y, String zone) {
        this.client    = client;
        this.myName    = name;
        this.myColor   = parseColor(color);
        this.myRol     = rol;
        this.myUserId  = userId;
        this.playerX   = x;
        this.playerY   = y;
        this.currentZone = zone;

        setPreferredSize(new Dimension(W, H + 60));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        // Bucle de movimiento: 100ms = 10 pasos/seg
        moveTimer = new Timer(100, e -> tickMovement());
        moveTimer.start();

        // Bucle de animación: 33ms ≈ 30fps
        Timer renderTimer = new Timer(33, e -> { animTimer++; repaint(); });
        renderTimer.start();
    }

    // ── Construcción de mapas de tiles ─────────────────────────────────

    private int[][] buildEntradaMap() {
        // Tiles: 19=Hormigon, 20=Cielo, 21=LadrilloSalmon, 22=CreamaTorre,
        //        23=Ventana, 24=Portico, 25=Escalon, 5=Puerta, 1=Camino, 6/7=Arbol
        int[][] m = new int[MAP_COLS][MAP_ROWS];

        // Base: hormigón/acera
        for (int c = 0; c < MAP_COLS; c++)
            for (int r = 0; r < MAP_ROWS; r++)
                m[c][r] = 19;

        // ── Fila 0: cielo ──────────────────────────────────────────────
        for (int c = 0; c < MAP_COLS; c++) m[c][0] = 20;

        // ── Cielo lateral (fuera del edificio, filas 1-6) ─────────────
        for (int r = 1; r <= 6; r++) {
            m[0][r] = 20; m[1][r] = 20;
            m[19][r] = 20; m[20][r] = 20;
        }

        // ── Fila 1: cornisa/remate del edificio ───────────────────────
        for (int c = 2; c <= 18; c++) m[c][1] = 21; // ladrillo
        for (int c = 8; c <= 12; c++) m[c][1] = 22; // torre central cream

        // ── Fila 2: primera planta – ventanas ─────────────────────────
        for (int c = 2; c <= 18; c++) m[c][2] = 21;
        // Ventanas ala izquierda
        m[3][2] = 23; m[4][2] = 23; m[6][2] = 23;
        // Torre central: cream + ventanas verticales grandes
        m[8][2] = 22; m[9][2] = 23; m[10][2] = 23; m[11][2] = 23; m[12][2] = 22;
        // Ventanas ala derecha
        m[14][2] = 23; m[16][2] = 23; m[17][2] = 23;

        // ── Fila 3: franja de ladrillo entre plantas ───────────────────
        for (int c = 2; c <= 18; c++) m[c][3] = 21;
        for (int c = 8; c <= 12; c++) m[c][3] = 22; // torre sigue cream

        // ── Fila 4: segunda planta – friso "COLEGIO LAGOMAR" ──────────
        for (int c = 2; c <= 18; c++) m[c][4] = 21;
        // Friso cream central con letrero
        for (int c = 7; c <= 13; c++) m[c][4] = 22;
        // Ventanas ala izquierda (segunda planta)
        m[3][4] = 23; m[4][4] = 23; m[6][4] = 23;
        // Ventanas ala derecha
        m[14][4] = 23; m[16][4] = 23; m[17][4] = 23;

        // ── Fila 5: ladrillo salmón continuo ──────────────────────────
        for (int c = 2; c <= 18; c++) m[c][5] = 21;

        // ── Fila 6: ladrillo salmón (cols 6-14 igual que el resto) ───
        for (int c = 2; c <= 18; c++) m[c][6] = 21;

        // ── Fila 7: umbral con puertas ────────────────────────────────
        m[2][7] = 21; m[3][7] = 21; m[4][7] = 21; m[5][7] = 21;
        m[6][7]  = 5;  m[7][7]  = 5;              // puerta izquierda
        for (int c = 8; c <= 12; c++) m[c][7] = 21; // muro entre puertas
        m[13][7] = 5;  m[14][7] = 5;              // puerta derecha
        m[15][7] = 21; m[16][7] = 21; m[17][7] = 21; m[18][7] = 21;

        // ── Fila 8: escalones de entrada ──────────────────────────────
        for (int c = 5; c <= 15; c++) m[c][8] = 25;
        // El camino central corta los escalones
        m[9][8] = 1; m[10][8] = 1; m[11][8] = 1;

        // ── Camino central desde fila 9 hasta abajo ───────────────────
        for (int r = 9; r < MAP_ROWS; r++) {
            m[9][r] = 1; m[10][r] = 1; m[11][r] = 1;
        }

        // ── Árboles decorativos ────────────────────────────────────────
        m[1][9]  = 6; m[1][10]  = 7;
        m[19][9] = 6; m[19][10] = 7;
        m[3][12] = 6; m[3][13]  = 7;
        m[17][12]= 6; m[17][13] = 7;

        return m;
    }

    private int[][] buildRecepcionMap() {
        int[][] m = new int[MAP_COLS][MAP_ROWS];
        for (int c = 0; c < MAP_COLS; c++)
            for (int r = 0; r < MAP_ROWS; r++) m[c][r] = 8; // suelo

        for (int c = 0; c < MAP_COLS; c++) { m[c][0] = 9; m[c][1] = 9; } // paredes

        // Tablón de anuncios (fila 1, cols 3-5)
        m[3][1] = 10; m[4][1] = 10; m[5][1] = 10;
        // Horario/Calendario (fila 1, cols 15-17)
        m[15][1] = 15; m[16][1] = 15; m[17][1] = 15;
        // Mostrador central (filas 5-7, cols 7-13)
        for (int c = 7; c <= 13; c++) for (int r = 5; r <= 7; r++) m[c][r] = 11;
        // Puerta izquierda a conserjería (col 0, filas 5-8)
        m[0][5] = 5; m[0][6] = 5; m[0][7] = 5; m[0][8] = 5;
        // Puertas de salida a entrada (fila 14)
        m[2][14] = 5; m[3][14] = 5;
        m[17][14] = 5; m[18][14] = 5;
        return m;
    }

    private int[][] buildConserjeriaMap() {
        int[][] m = new int[MAP_COLS][MAP_ROWS];
        for (int c = 0; c < MAP_COLS; c++)
            for (int r = 0; r < MAP_ROWS; r++) m[c][r] = 12; // suelo madera

        for (int c = 0; c < MAP_COLS; c++) { m[c][0] = 13; m[c][1] = 13; } // paredes
        // Menú del comedor (fila 1, cols 2-4)
        m[2][1] = 16; m[3][1] = 16; m[4][1] = 16;
        // Notas/Trabajos (fila 1, cols 16-18)
        m[16][1] = 17; m[17][1] = 17; m[18][1] = 17;
        // Mostrador (filas 7-8, cols 5-15)
        for (int c = 5; c <= 15; c++) { m[c][7] = 18; m[c][8] = 18; }
        // Archivadores (filas 11-14, cols 1,3,5)
        for (int r = 11; r <= 14; r++) { m[1][r] = 14; m[3][r] = 14; m[5][r] = 14; }
        // Puerta derecha a recepción (col 20, filas 5-8)
        m[20][5] = 5; m[20][6] = 5; m[20][7] = 5; m[20][8] = 5;
        return m;
    }

    // ── Renderizado principal ──────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        switch (currentZone) {
            case "entrada":     drawZone(g2, MAP_ENTRADA);      break;
            case "recepcion":   drawZone(g2, MAP_RECEPCION);    break;
            case "conserjeria": drawZone(g2, MAP_CONSERJERIA);  break;
        }

        drawOtherPlayers(g2);
        drawLocalPlayer(g2);

        if (currentZone.equals("entrada")) drawEntradaText(g2);
        else drawInteractionHint(g2);
        drawRecepcionWalls(g2);

        drawChatOverlay(g2);

        // Fundido a negro
        if (fadeAlpha > 0) {
            g2.setColor(new Color(0, 0, 0, Math.min(fadeAlpha, 255)));
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    // ── Escena 1: texto superpuesto del edificio Lagomar ─────────────────

    private void drawEntradaText(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                           RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // "COLEGIO LAGOMAR" horizontal en el friso crema (fila 4, cols 7-13)
        int frisoY  = 4 * TILE_SIZE + TILE_SIZE / 2 + 4;
        int frisoX1 = 7 * TILE_SIZE, frisoX2 = 14 * TILE_SIZE;
        int frisoW  = frisoX2 - frisoX1;
        g.setFont(new Font("SansSerif", Font.BOLD, 9));
        FontMetrics fm = g.getFontMetrics();
        String colegio = "COLEGIO LAGOMAR";
        int tx = frisoX1 + frisoW / 2 - fm.stringWidth(colegio) / 2;
        g.setColor(new Color(100, 70, 30, 180));
        g.drawString(colegio, tx+1, frisoY+1);
        g.setColor(new Color(40, 60, 110));
        g.drawString(colegio, tx, frisoY);
    }

    // ── Escenas 2 y 3: Renderizado por tiles ───────────────────────────

    private void drawZone(Graphics2D g, int[][] tileMap) {
        for (int c = 0; c < MAP_COLS; c++)
            for (int r = 0; r < MAP_ROWS; r++)
                drawTile(g, c * TILE_SIZE, r * TILE_SIZE, tileMap[c][r]);
    }

    private void drawTile(Graphics2D g, int x, int y, int tile) {
        int s = TILE_SIZE;
        switch (tile) {
            case 5: // Puerta
                g.setColor(new Color(92, 58, 33));
                g.fillRect(x, y, s, s);
                g.setColor(new Color(61, 37, 20));
                g.setStroke(new BasicStroke(2));
                g.drawRect(x+3, y+3, s-6, s-6);
                g.setColor(new Color(209, 161, 58));
                g.fillOval(x+s-10, y+s/2-3, 6, 6);
                break;
            case 8: // Suelo recepción (tablero de ajedrez)
                g.setColor(((x/s + y/s) % 2 == 0) ? new Color(224,224,224) : new Color(245,245,245));
                g.fillRect(x, y, s, s);
                break;
            case 9: // Pared recepción
                g.setColor(new Color(74, 107, 140));
                g.fillRect(x, y, s, s);
                g.setColor(new Color(58, 84, 110));
                g.fillRect(x, y+s-6, s, 6);
                break;
            case 10: // Tablón de anuncios
                g.setColor(new Color(74, 107, 140));
                g.fillRect(x, y, s, s);
                g.setColor(new Color(138, 98, 59));
                g.fillRect(x+3, y+5, s-6, s-10);
                g.setColor(Color.WHITE);
                g.fillRect(x+7, y+9, 8, 10);
                g.setColor(new Color(240, 216, 144));
                g.fillRect(x+18, y+11, 10, 8);
                g.setColor(new Color(255, 107, 107));
                g.fillOval(x+9, y+7, 4, 4);
                g.fillOval(x+22, y+9, 4, 4);
                break;
            case 11: // Mostrador recepción
                g.setColor(((x/s + y/s) % 2 == 0) ? new Color(224,224,224) : new Color(245,245,245));
                g.fillRect(x, y, s, s);
                g.setColor(new Color(115, 77, 38));
                g.fillRect(x+3, y+5, s-6, s-10);
                g.setColor(new Color(92, 58, 26));
                g.fillRect(x+3, y+5, s-6, 5);
                break;
            case 12: // Suelo conserjería (madera)
                g.setColor(new Color(140, 98, 57));
                g.fillRect(x, y, s, s);
                g.setColor(new Color(110, 74, 41));
                g.setStroke(new BasicStroke(1));
                for (int i = y+10; i < y+s; i += 10) { g.drawLine(x, i, x+s, i); }
                g.drawLine(x+s/2, y, x+s/2, y+10);
                break;
            case 13: // Pared conserjería
                g.setColor(new Color(92, 69, 48));
                g.fillRect(x, y, s, s);
                g.setColor(new Color(69, 50, 33));
                g.fillRect(x, y+s-6, s, 6);
                break;
            case 14: // Archivador
                g.setColor(new Color(140, 98, 57));
                g.fillRect(x, y, s, s);
                g.setColor(new Color(122, 122, 122));
                g.fillRect(x+4, y+3, s-8, s-6);
                g.setColor(new Color(148, 148, 148));
                g.fillRect(x+6, y+5, s-12, s/2-8);
                g.fillRect(x+6, y+s/2+2, s-12, s/2-8);
                g.setColor(Color.DARK_GRAY);
                g.fillRect(x+13, y+7, 10, 3);
                g.fillRect(x+13, y+s/2+4, 10, 3);
                break;
            case 15: // Horario/Calendario
                g.setColor(new Color(74, 107, 140));
                g.fillRect(x, y, s, s);
                g.setColor(Color.WHITE);
                g.fillRect(x+4, y+4, s-8, s-8);
                g.setColor(new Color(230, 69, 69));
                g.fillRect(x+4, y+4, s-8, 7);
                g.setColor(new Color(204, 204, 204));
                for (int i = y+14; i < y+s-4; i += 5) g.fillRect(x+7, i, s-14, 2);
                break;
            case 16: // Menú del comedor
                g.setColor(new Color(92, 69, 48));
                g.fillRect(x, y, s, s);
                g.setColor(new Color(42, 90, 42));
                g.fillRect(x+3, y+5, s-6, s-10);
                g.setColor(Color.WHITE);
                g.fillRect(x+7, y+9,  s-14, 3);
                g.fillRect(x+7, y+16, s-18, 3);
                g.fillRect(x+7, y+23, s-16, 3);
                break;
            case 17: // Notas/Trabajos
                g.setColor(new Color(92, 69, 48));
                g.fillRect(x, y, s, s);
                g.setColor(new Color(90, 42, 42));
                g.fillRect(x+3, y+5, s-6, s-10);
                g.setColor(new Color(240, 216, 144));
                g.fillRect(x+7, y+9, 10, 10);
                g.setColor(Color.WHITE);
                g.fillRect(x+20, y+13, 10, 12);
                g.setColor(new Color(255, 107, 107));
                g.fillOval(x+11, y+7, 4, 4);
                g.fillOval(x+23, y+11, 4, 4);
                break;
            case 18: // Mostrador conserjería
                g.setColor(new Color(140, 98, 57));
                g.fillRect(x, y, s, s);
                g.setColor(new Color(74, 51, 32));
                g.fillRect(x+3, y+5, s-6, s-10);
                g.setColor(new Color(56, 37, 22));
                g.fillRect(x+3, y+5, s-6, 5);
                break;
            case 19: { // Hormigón / Acera
                int cx19 = x / s, cy19 = y / s;
                // Losas alternadas ligeramente (como Kenney's tileset)
                Color base19 = ((cx19 + cy19) % 2 == 0)
                    ? new Color(175, 174, 168)
                    : new Color(162, 161, 155);
                g.setColor(base19);
                g.fillRect(x, y, s, s);
                // Juntas de mortero
                g.setColor(new Color(120, 118, 112));
                g.setStroke(new BasicStroke(1));
                g.drawLine(x, y, x + s, y);       // borde superior
                g.drawLine(x, y, x, y + s);        // borde izquierdo
                // Juntura interior en damero
                if ((cx19 + cy19) % 2 == 0) {
                    g.drawLine(x + s/2, y, x + s/2, y + s/2);
                    g.drawLine(x, y + s/2, x + s/2, y + s/2);
                } else {
                    g.drawLine(x + s/2, y + s/2, x + s/2, y + s);
                    g.drawLine(x + s/2, y + s/2, x + s, y + s/2);
                }
                // Pequeñas imperfecciones de textura
                g.setColor(new Color(148, 147, 141));
                g.fillRect(x + 7, y + 9,  2, 1);
                g.fillRect(x + 22, y + 16, 3, 1);
                g.fillRect(x + 11, y + 29, 2, 1);
                g.fillRect(x + 31, y + 22, 2, 1);
                g.fillRect(x + 26, y + 36, 3, 1);
                break;
            }
            case 20: { // Cielo (sobre el edificio)
                // Degradado pixelart de cielo
                int skyRow = y / s;
                Color skyColor = skyRow <= 1
                    ? new Color(100, 160, 220)   // cielo más alto, más azul
                    : new Color(135, 195, 240);  // cielo más bajo, más claro
                g.setColor(skyColor);
                g.fillRect(x, y, s, s);
                // Nubecita decorativa en posición fija (solo cada ~5 tiles)
                if ((x/s == 2 && y/s == 1) || (x/s == 18 && y/s == 2)) {
                    g.setColor(new Color(255, 255, 255, 200));
                    g.fillRoundRect(x+4, y+12, 28, 14, 10, 10);
                    g.fillRoundRect(x+8, y+8, 18, 12, 8, 8);
                }
                break;
            }
            case 21: { // Ladrillo salmón (fachada Lagomar)
                g.setColor(new Color(196, 110, 79));
                g.fillRect(x, y, s, s);
                // Juntas de mortero
                g.setColor(new Color(165, 90, 62));
                g.setStroke(new BasicStroke(1));
                // Líneas horizontales cada 10px
                for (int row = y + 9; row < y + s; row += 10) g.drawLine(x, row, x+s, row);
                // Líneas verticales alternadas (apareado inglés)
                int rowIdx = (y / s) % 2;
                int offset = (rowIdx == 0) ? 0 : s/2;
                for (int col = x + offset; col < x + s + s/2; col += s/2) g.drawLine(col, y, col, y+s);
                // Sombra superior sutil
                g.setColor(new Color(0, 0, 0, 30));
                g.fillRect(x, y, s, 3);
                break;
            }
            case 22: { // Crema/beige torre central
                g.setColor(new Color(245, 230, 195));
                g.fillRect(x, y, s, s);
                // Textura fina horizontal
                g.setColor(new Color(225, 210, 172));
                g.setStroke(new BasicStroke(1));
                for (int row = y + 7; row < y + s; row += 7) g.drawLine(x, row, x+s, row);
                // Línea lateral sutil
                g.setColor(new Color(210, 193, 155));
                g.drawLine(x, y, x, y+s);
                g.drawLine(x+s-1, y, x+s-1, y+s);
                break;
            }
            case 23: { // Ventana oscura
                // Marco de ladrillo salmón alrededor
                g.setColor(new Color(196, 110, 79));
                g.fillRect(x, y, s, s);
                // Hueco de ventana oscuro (vidrio)
                g.setColor(new Color(28, 45, 65));
                g.fillRect(x+5, y+4, s-10, s-8);
                // Marco interior claro
                g.setColor(new Color(200, 185, 155));
                g.setStroke(new BasicStroke(2));
                g.drawRect(x+5, y+4, s-10, s-8);
                // Cruz divisoria de ventana
                g.setColor(new Color(180, 165, 135));
                g.setStroke(new BasicStroke(1));
                g.drawLine(x+s/2, y+4, x+s/2, y+s-4);
                g.drawLine(x+5, y+s/2, x+s-5, y+s/2);
                // Reflejo sutil
                g.setColor(new Color(100, 150, 220, 60));
                g.fillRect(x+7, y+6, 8, 10);
                break;
            }
            case 24: { // Pórtico / marquesina
                // Techo del pórtico (vista desde debajo)
                g.setColor(new Color(220, 200, 165));
                g.fillRect(x, y, s, s);
                // Costillas estructurales
                g.setColor(new Color(196, 175, 140));
                g.setStroke(new BasicStroke(2));
                for (int col = x + 8; col < x + s; col += 12) g.drawLine(col, y, col, y+s);
                // Borde inferior del pórtico (sombra interna)
                g.setColor(new Color(0, 0, 0, 40));
                g.fillRect(x, y+s-5, s, 5);
                // Franja decorativa superior
                g.setColor(new Color(180, 140, 90));
                g.fillRect(x, y, s, 4);
                break;
            }
            case 25: { // Escalón / columna de hormigón
                int col25 = x / s, row25 = y / s;
                if (row25 == 6) {
                    // Columna cilíndrica
                    g.setColor(new Color(200, 195, 188));
                    g.fillRect(x, y, s, s);
                    // Fuste de la columna
                    g.setColor(new Color(220, 215, 208));
                    g.fillOval(x+8, y+4, s-16, s-4);
                    // Sombra lateral de la columna
                    g.setColor(new Color(160, 155, 148));
                    g.fillOval(x+s-14, y+4, 8, s-4);
                    // Capitel y basa
                    g.setColor(new Color(190, 185, 178));
                    g.fillRect(x+4, y, s-8, 5);
                    g.fillRect(x+4, y+s-5, s-8, 5);
                } else {
                    // Escalón de piedra/hormigón
                    g.setColor(new Color(185, 183, 175));
                    g.fillRect(x, y, s, s);
                    // Borde superior del escalón (arista iluminada)
                    g.setColor(new Color(220, 218, 210));
                    g.fillRect(x, y, s, 4);
                    // Borde inferior (sombra)
                    g.setColor(new Color(150, 148, 140));
                    g.fillRect(x, y+s-3, s, 3);
                    // Juntas laterales
                    g.setColor(new Color(160, 158, 150));
                    g.setStroke(new BasicStroke(1));
                    g.drawLine(x, y, x, y+s);
                    g.drawLine(x+s-1, y, x+s-1, y+s);
                }
                break;
            }
            default: // Vacío
                g.setColor(Color.DARK_GRAY);
                g.fillRect(x, y, s, s);
        }
    }

    // ── Paredes laterales de recepción (proyecto viejo) ─────────────────

    private void drawRecepcionWalls(Graphics2D g) {
        if (!currentZone.equals("recepcion")) return;
        int top = 12 * TILE_SIZE;
        int bot = MAP_ROWS * TILE_SIZE;
        g.setColor(new Color(220, 50, 50));
        g.setStroke(new BasicStroke(3));
        g.drawLine(6  * TILE_SIZE, top, 6  * TILE_SIZE, bot); // izquierda
        g.drawLine(15 * TILE_SIZE, top, 15 * TILE_SIZE, bot); // derecha
    }

    // ── Pistas de interacción ─────────────────────────────────────────

    private void drawInteractionHint(Graphics2D g) {
        nearbyInteraction = getNearbyInteraction();
        if (nearbyInteraction == null) return;
        String label;
        switch (nearbyInteraction) {
            case "tablon":       label = "Tablón de Anuncios  [E]"; break;
            case "horario":      label = "Horario / Calendario  [E]"; break;
            case "menu_comedor": label = "Menú del Comedor  [E]"; break;
            case "notas":        label = "Notas y Calificaciones  [E]"; break;
            default: return;
        }
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(label) + 16;
        int tx = W/2 - tw/2;
        int ty = H - 2 * TILE_SIZE;
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRoundRect(tx, ty - fm.getAscent() - 4, tw, fm.getHeight() + 8, 8, 8);
        g.setColor(new Color(255, 230, 100));
        g.drawString(label, tx + 8, ty);
    }

    // ── Sprites de jugadores ───────────────────────────────────────────

    private void drawLocalPlayer(Graphics2D g) {
        drawSprite(g, playerX, playerY, direction, moving, animTimer, myColor, myName);
    }

    private void drawOtherPlayers(Graphics2D g) {
        for (PlayerData pd : otherPlayers.values())
            drawSprite(g, pd.x, pd.y, pd.direction, pd.moving, pd.animTimer, pd.color, pd.name);
    }

    private void drawSprite(Graphics2D g, int tileX, int tileY, String dir,
                             boolean isMoving, int anim, Color base, String name) {
        int x  = tileX * TILE_SIZE;
        int y  = tileY * TILE_SIZE;
        int s  = TILE_SIZE;        // 40
        int hs = s / 2;            // 20

        // Sombra
        g.setColor(new Color(0, 0, 0, 60));
        g.fillOval(x + 7, y + s - 8, s - 14, 8);

        // Bounce (rebote al caminar)
        double bounce = isMoving ? Math.sin(anim * 0.55) * 2.5 : 0;
        int    by     = (int)(y + bounce);

        // Cuerpo
        g.setColor(base);
        g.fillRoundRect(x+9, by+15, s-18, s-20, 6, 6);

        // Cabeza
        g.setColor(lighten(base, 50));
        g.fillOval(x + hs - 11, by + 2, 22, 22);

        // Ojos (posición según dirección)
        int ex1, ey1, ex2, ey2;
        switch (dir) {
            case "up":    ex1=x+12; ey1=by+7;  ex2=x+20; ey2=by+7;  break;
            case "left":  ex1=x+9;  ey1=by+10; ex2=x+17; ey2=by+10; break;
            case "right": ex1=x+17; ey1=by+10; ex2=x+25; ey2=by+10; break;
            default:      ex1=x+12; ey1=by+11; ex2=x+20; ey2=by+11; // down
        }
        g.setColor(Color.WHITE);
        g.fillOval(ex1-3, ey1-3, 7, 7); g.fillOval(ex2-3, ey2-3, 7, 7);
        g.setColor(new Color(34, 34, 34));
        g.fillOval(ex1-2, ey1-2, 4, 4); g.fillOval(ex2-2, ey2-2, 4, 4);

        // Piernas animadas
        g.setColor(darken(base, 50));
        if (isMoving) {
            int leg = (int)(Math.sin(anim * 0.65) * 5);
            g.fillRect(x+11, by+s-11, 7, 11+leg);
            g.fillRect(x+s-18, by+s-11, 7, 11-leg);
        } else {
            g.fillRect(x+11, by+s-11, 7, 11);
            g.fillRect(x+s-18, by+s-11, 7, 11);
        }

        // Etiqueta de nombre
        g.setFont(new Font("SansSerif", Font.BOLD, 9));
        FontMetrics fm = g.getFontMetrics();
        int nw = fm.stringWidth(name) + 8;
        int nx = x + hs - nw/2;
        int nyt = by - 4;
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRoundRect(nx, nyt - fm.getAscent() - 2, nw, fm.getHeight() + 4, 4, 4);
        g.setColor(Color.WHITE);
        g.drawString(name, nx + 4, nyt);
    }

    // ── Chat overlay ──────────────────────────────────────────────────

    private void drawChatOverlay(Graphics2D g) {
        // Panel de historial
        int panelX = 6, panelY = H + 4;
        int panelW = W - 12, panelH = 50;
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRoundRect(panelX, panelY, panelW, panelH, 8, 8);

        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        FontMetrics fm = g.getFontMetrics();
        int lineH = fm.getHeight();
        int maxLines = (panelH - 4) / lineH;

        int start = Math.max(0, chatMessages.size() - maxLines);
        int lineY = panelY + fm.getAscent() + 4;
        for (int i = start; i < chatMessages.size(); i++) {
            String[] m = chatMessages.get(i);
            Color c = parseColor(m[1]);
            g.setColor(c);
            String full = "[" + m[0] + "] " + m[2];
            g.drawString(full, panelX + 6, lineY);
            lineY += lineH;
        }

        // Input de chat
        if (chatActive) {
            g.setColor(new Color(20, 20, 50, 220));
            g.fillRoundRect(panelX, panelY - 28, panelW, 24, 6, 6);
            g.setColor(new Color(74, 144, 217));
            g.setStroke(new BasicStroke(1));
            g.drawRoundRect(panelX, panelY - 28, panelW, 24, 6, 6);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            String txt = "T: " + chatInput.toString() + (System.currentTimeMillis() % 1000 < 500 ? "|" : "");
            g.drawString(txt, panelX + 8, panelY - 28 + 16);
        } else {
            g.setColor(new Color(120, 140, 180));
            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g.drawString("T = Chat   E = Interactuar   Enter = Entrar", panelX + 8, panelY - 6);
        }
    }

    // ── Gestión de mensajes del servidor ──────────────────────────────

    public void handleServerMessage(String msg) {
        String[] p = msg.split("\\|", 6);
        switch (p[0]) {
            case "POSITION":
                if (p.length >= 3) {
                    playerX = Integer.parseInt(p[1]);
                    playerY = Integer.parseInt(p[2]);
                }
                break;
            case "PLAYER_JOINED":
                // PLAYER_JOINED|name|color|x|y|direction
                if (p.length >= 6) {
                    PlayerData pd = new PlayerData(p[1], parseColor(p[2]),
                        Integer.parseInt(p[3]), Integer.parseInt(p[4]), p[5]);
                    otherPlayers.put(p[1], pd);
                }
                break;
            case "PLAYER_MOVED":
                // PLAYER_MOVED|name|x|y|direction|color
                if (p.length >= 6) {
                    PlayerData pd = otherPlayers.computeIfAbsent(p[1],
                        n -> new PlayerData(n, parseColor(p[5]),
                            Integer.parseInt(p[2]), Integer.parseInt(p[3]), p[4]));
                    pd.update(Integer.parseInt(p[2]), Integer.parseInt(p[3]), p[4]);
                    pd.color = parseColor(p[5]);
                }
                break;
            case "PLAYER_LEFT":
                if (p.length >= 2) otherPlayers.remove(p[1]);
                break;
            case "ZONE_OK":
                // ZONE_OK|zone|x|y
                if (p.length >= 4) {
                    currentZone = p[1];
                    playerX = Integer.parseInt(p[2]);
                    playerY = Integer.parseInt(p[3]);
                    otherPlayers.clear();
                }
                break;
            case "CHAT_MSG":
                // CHAT_MSG|name|color|message
                if (p.length >= 4) {
                    String chatTxt = msg.substring(p[0].length()+p[1].length()+p[2].length()+3);
                    synchronized(chatMessages) {
                        chatMessages.add(new String[]{p[1], p[2], chatTxt});
                        if (chatMessages.size() > 30) chatMessages.remove(0);
                    }
                }
                break;
            case "DATA_TABLON":
                showData("tablon", msg.substring("DATA_TABLON|".length()));
                break;
            case "DATA_HORARIO":
                showData("horario", msg.substring("DATA_HORARIO|".length()));
                break;
            case "DATA_MENU_COMEDOR":
                showData("menu_comedor", msg.substring("DATA_MENU_COMEDOR|".length()));
                break;
            case "DATA_NOTAS":
                showData("notas", msg.substring("DATA_NOTAS|".length()));
                break;
        }
    }

    private void showData(String type, String data) {
        SwingUtilities.invokeLater(() -> {
            JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
            if (parent instanceof GameWindow)
                ((GameWindow) parent).showDataDialog(type, data);
        });
    }

    // ── Movimiento ────────────────────────────────────────────────────

    private void tickMovement() {
        if (chatActive || fadeTimer != null) return;
        String dir = null;
        if (pressedKeys.contains(KeyEvent.VK_UP)    || pressedKeys.contains(KeyEvent.VK_W))    dir = "UP";
        else if (pressedKeys.contains(KeyEvent.VK_DOWN)  || pressedKeys.contains(KeyEvent.VK_S))  dir = "DOWN";
        else if (pressedKeys.contains(KeyEvent.VK_LEFT)  || pressedKeys.contains(KeyEvent.VK_A))  dir = "LEFT";
        else if (pressedKeys.contains(KeyEvent.VK_RIGHT) || pressedKeys.contains(KeyEvent.VK_D))  dir = "RIGHT";

        if (dir != null) {
            moving = true;
            switch (dir) {
                case "UP":    direction = "up";    break;
                case "DOWN":  direction = "down";  break;
                case "LEFT":  direction = "left";  break;
                case "RIGHT": direction = "right"; break;
            }
            client.sendMessage("MOVE|" + dir);
        } else {
            moving = false;
        }
    }

    // ── Detección de zonas de cambio ──────────────────────────────────

    private String getDoorTransition() {
        if (currentZone.equals("entrada")) {
            if (playerY == S1_DOOR_ROW &&
                (playerX == S1_DOOR_L1 || playerX == S1_DOOR_L2 ||
                 playerX == S1_DOOR_R1 || playerX == S1_DOOR_R2))
                return "recepcion";
        } else if (currentZone.equals("recepcion")) {
            if (playerX == S2_LEFT_DOOR_COL && playerY >= 5 && playerY <= 8)
                return "conserjeria";
            if (playerY == S2_EXIT_ROW &&
                (playerX == S2_EXIT_L1 || playerX == S2_EXIT_L2 ||
                 playerX == S2_EXIT_R1 || playerX == S2_EXIT_R2))
                return "entrada";
        } else if (currentZone.equals("conserjeria")) {
            if (playerX == S3_RIGHT_DOOR_COL && playerY >= 5 && playerY <= 8)
                return "recepcion";
        }
        return null;
    }

    private int[] computeSpawn(String fromZone, String toZone) {
        if (fromZone.equals("entrada") && toZone.equals("recepcion")) {
            boolean leftDoor = (playerX == S1_DOOR_L1 || playerX == S1_DOOR_L2);
            return leftDoor ? new int[]{2, 13} : new int[]{18, 13};
        }
        if (fromZone.equals("recepcion") && toZone.equals("entrada")) {
            boolean leftExit = (playerX == S2_EXIT_L1 || playerX == S2_EXIT_L2);
            return leftExit ? new int[]{6, 8} : new int[]{14, 8};
        }
        if (fromZone.equals("recepcion") && toZone.equals("conserjeria"))
            return new int[]{19, 6};
        if (fromZone.equals("conserjeria") && toZone.equals("recepcion"))
            return new int[]{1, 6};
        return new int[]{MAP_COLS/2, MAP_ROWS-2};
    }

    private void startFade(String targetZone, int spawnX, int spawnY) {
        if (fadeTimer != null) return;
        fadeTarget = targetZone; fadeSpawnX = spawnX; fadeSpawnY = spawnY;
        fadingOut = true; fadeAlpha = 0;
        fadeTimer = new Timer(16, e -> {
            if (fadingOut) {
                fadeAlpha += 15;
                if (fadeAlpha >= 255) {
                    fadeAlpha = 255; fadingOut = false;
                    client.sendMessage("CHANGE_ZONE|" + fadeTarget + "|" + fadeSpawnX + "|" + fadeSpawnY);
                }
            } else {
                fadeAlpha -= 15;
                if (fadeAlpha <= 0) {
                    fadeAlpha = 0;
                    ((Timer)e.getSource()).stop();
                    fadeTimer = null;
                }
            }
            repaint();
        });
        fadeTimer.start();
    }

    // ── Interacciones ─────────────────────────────────────────────────

    private String getNearbyInteraction() {
        if (currentZone.equals("recepcion")) {
            if (playerY <= 3 && playerX >= 2 && playerX <= 7) return "tablon";
            if (playerY <= 3 && playerX >= 13 && playerX <= 19) return "horario";
        } else if (currentZone.equals("conserjeria")) {
            if (playerY <= 3 && playerX >= 1 && playerX <= 6)  return "menu_comedor";
            if (playerY <= 3 && playerX >= 14 && playerX <= 20) return "notas";
        }
        return null;
    }

    // ── Input ─────────────────────────────────────────────────────────

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        pressedKeys.add(code);

        if (chatActive) {
            if (code == KeyEvent.VK_ENTER) {
                String msg = chatInput.toString().trim();
                if (!msg.isEmpty()) client.sendMessage("CHAT|" + msg);
                chatActive = false; chatInput.setLength(0);
            } else if (code == KeyEvent.VK_ESCAPE) {
                chatActive = false; chatInput.setLength(0);
            } else if (code == KeyEvent.VK_BACK_SPACE && chatInput.length() > 0) {
                chatInput.deleteCharAt(chatInput.length()-1);
            }
            return;
        }

        switch (code) {
            case KeyEvent.VK_T:
                chatActive = true; chatInput.setLength(0);
                break;
            case KeyEvent.VK_ENTER: {
                String target = getDoorTransition();
                if (target != null) {
                    int[] spawn = computeSpawn(currentZone, target);
                    startFade(target, spawn[0], spawn[1]);
                }
                break;
            }
            case KeyEvent.VK_E: {
                String inter = getNearbyInteraction();
                if (inter != null) client.sendMessage("INTERACT|" + inter);
                break;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) { pressedKeys.remove(e.getKeyCode()); }

    @Override
    public void keyTyped(KeyEvent e) {
        if (chatActive) {
            char c = e.getKeyChar();
            if (c != KeyEvent.CHAR_UNDEFINED && c >= 32 && chatInput.length() < 120)
                chatInput.append(c);
        }
    }

    // ── Utilidades de color ────────────────────────────────────────────

    private Color parseColor(String hex) {
        try { return Color.decode(hex); }
        catch (Exception e) { return new Color(74, 144, 217); }
    }

    private Color lighten(Color c, int a) {
        return new Color(Math.min(255,c.getRed()+a), Math.min(255,c.getGreen()+a), Math.min(255,c.getBlue()+a));
    }

    private Color darken(Color c, int a) {
        return new Color(Math.max(0,c.getRed()-a), Math.max(0,c.getGreen()-a), Math.max(0,c.getBlue()-a));
    }
}
