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
    private static class PmChat {
        final String       name;
        final Color        color;
        final List<String[]> messages = new ArrayList<>(); // ["1"|"0", texto]
        boolean hasUnread = false;
        boolean isFake    = false;
        PmChat(String name, Color color) { this.name = name; this.color = color; }
    }

    private final List<String[]> chatMessages  = new ArrayList<>(); // [nombre, color, texto]
    private final List<PmChat>   pmChats        = new ArrayList<>();
    private int                  activePmIdx    = -1; // -1 = chat global
    private final List<Rectangle> pmTabRects    = new ArrayList<>();
    private final List<Rectangle> pmCloseRects  = new ArrayList<>();
    private final Rectangle       globalTabRect = new Rectangle();
    private boolean chatActive    = false;
    private boolean chatVisible   = true;
    private boolean skipNextTyped = false;
    private final StringBuilder chatInput     = new StringBuilder();
    private final Rectangle     chatToggleBtn = new Rectangle();

    // ── Input ──────────────────────────────────────────────────────────
    private final Set<Integer> pressedKeys = new HashSet<>();
    private Timer moveTimer;

    // ── Interacción ────────────────────────────────────────────────────
    private String nearbyInteraction = null;
    private TemarioDialog  temarioDialog  = null;
    private TrabajoDialog  trabajoDialog  = null;

    // ── Tutorial NPC ───────────────────────────────────────────────────
    private TutorialNPC    tutorialNPC    = null;
    private boolean        primerLogin    = false;

    public GamePanel(Client client, String name, String color, String rol,
                     int userId, int x, int y, String zone, boolean primerLogin) {
        this.client    = client;
        this.myName    = name;
        this.myColor   = parseColor(color);
        this.myRol     = rol;
        this.myUserId  = userId;
        this.playerX   = x;
        this.playerY   = y;
        this.currentZone = zone;
        this.primerLogin = primerLogin;

        setPreferredSize(new Dimension(W, H + 60));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point p = e.getPoint();
                if (chatToggleBtn.contains(p)) {
                    chatVisible = !chatVisible;
                    if (!chatVisible) chatActive = false;
                    repaint();
                    return;
                }
                if (!chatVisible) return;
                // Cerrar pestaña PM (✕)
                for (int i = 0; i < pmCloseRects.size(); i++) {
                    if (pmCloseRects.get(i).contains(p)) {
                        pmChats.remove(i);
                        pmTabRects.remove(i);
                        pmCloseRects.remove(i);
                        if (activePmIdx >= pmChats.size())
                            activePmIdx = pmChats.isEmpty() ? -1 : pmChats.size() - 1;
                        repaint();
                        return;
                    }
                }
                // Cambiar pestaña PM
                for (int i = 0; i < pmTabRects.size(); i++) {
                    if (pmTabRects.get(i).contains(p)) {
                        activePmIdx = i;
                        pmChats.get(i).hasUnread = false;
                        repaint();
                        return;
                    }
                }
                // Pestaña Global
                if (globalTabRect.contains(p)) {
                    activePmIdx = -1;
                    repaint();
                }
            }
        });

        // Bucle de movimiento: 100ms = 10 pasos/seg
        moveTimer = new Timer(100, e -> tickMovement());
        moveTimer.start();

        // Bucle de animación: 33ms ≈ 30fps
        Timer renderTimer = new Timer(33, e -> {
            animTimer++;
            if (tutorialNPC != null) tutorialNPC.update(playerX, playerY, currentZone);
            repaint();
        });
        renderTimer.start();

        // Iniciar NPC tutorial solo si es alumno en primer login
        if (primerLogin && "alumno".equals(rol)) {
            tutorialNPC = new TutorialNPC(this, playerX, playerY, zone);
        }
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
        m[6][7]  = 5;  m[7][7]  = 26;             // puerta izquierda (der: volteada)
        for (int c = 8; c <= 12; c++) m[c][7] = 21; // muro entre puertas
        m[13][7] = 5;  m[14][7] = 26;             // puerta derecha (der: volteada)
        m[15][7] = 21; m[16][7] = 21; m[17][7] = 21; m[18][7] = 21;

        // ── Fila 8: escalones de entrada ──────────────────────────────
        for (int c = 5; c <= 15; c++) m[c][8] = 25;
        // El camino central corta los escalones
        m[9][8] = 1; m[10][8] = 1; m[11][8] = 1;

        // ── Camino central desde fila 9 hasta abajo ───────────────────
        for (int r = 9; r < MAP_ROWS; r++) {
            m[9][r] = 1; m[10][r] = 1; m[11][r] = 1;
        }


        return m;
    }

    private int[][] buildRecepcionMap() {
        int[][] m = new int[MAP_COLS][MAP_ROWS];
        for (int c = 0; c < MAP_COLS; c++)
            for (int r = 0; r < MAP_ROWS; r++) m[c][r] = 8; // suelo

        for (int c = 0; c < MAP_COLS; c++) { m[c][0] = 9; m[c][1] = 9; } // paredes

        // Tablón de anuncios (fila 1, cols 3-4)
        m[3][1] = 10; m[4][1] = 10;
        // Horario/Calendario (fila 1, cols 15-16)
        m[15][1] = 15; m[16][1] = 15;
        // Bordes izquierdo (col 0) y derecho (col 20), filas 2-13
        for (int r = 2; r <= 13; r++) {
            if (r != 6 && r != 7) m[0][r] = 9; // izquierda, excepto puertas
            m[20][r] = 9;                        // derecha
        }
        // Borde inferior (fila 14): solo esquinas con tile de pared
        m[0][14] = 9;
        m[20][14] = 9;
        // Escalones (cols 6-14): fila 12 claro, fila 13 medio, fila 14 oscuro
        for (int c = 6; c <= 14; c++) { m[c][12] = 29; m[c][13] = 30; m[c][14] = 31; }
        // Puerta izquierda a conserjería (col 0, filas 6-7)
        m[0][6] = 28; m[0][7] = 27;
        // Puertas de salida a entrada (fila 14)
        m[2][14] = 5; m[3][14] = 26;
        m[17][14] = 5; m[18][14] = 26;
        return m;
    }

    private int[][] buildConserjeriaMap() {
        int[][] m = new int[MAP_COLS][MAP_ROWS];
        for (int c = 0; c < MAP_COLS; c++)
            for (int r = 0; r < MAP_ROWS; r++) m[c][r] = 12; // suelo madera

        for (int c = 0; c < MAP_COLS; c++) { m[c][0] = 13; m[c][1] = 13; } // paredes superiores
        // Bordes laterales (col 0 izquierda, col 20 derecha) filas 2-14
        for (int r = 2; r <= 14; r++) m[0][r] = 13;
        for (int r = 2; r <= 5;  r++) m[20][r] = 13;
        for (int r = 8; r <= 14; r++) m[20][r] = 13;
        // Borde inferior (fila 14) cols 1-19
        for (int c = 1; c <= 19; c++) m[c][14] = 13;
        // Menú del comedor (fila 1, cols 2-3)
        m[2][1] = 16; m[3][1] = 16;
        // Notas/Trabajos (fila 1, cols 16-17)
        m[16][1] = 17; m[17][1] = 17;
        // Trabajo (fila 1, cols 10-11 — separado 1 tile a la izquierda de Temario)
        m[10][1] = 33; m[11][1] = 33;
        // Temario (fila 1, cols 13-14 — separado 1 tile a la izquierda de Notas)
        m[13][1] = 32; m[14][1] = 32;
        // Mesas comedor: 4 grupos de 2 filas × 6 cols, simétricos
        int[][] grupos = {{2,5},{11,5},{2,9},{11,9}};
        for (int[] g : grupos)
            for (int c = g[0]; c <= g[0]+5; c++)
                for (int r = g[1]; r <= g[1]+1; r++)
                    m[c][r] = 18;
        // Puerta derecha a recepción (col 20, filas 6-7)
        m[20][6] = 28; m[20][7] = 27;
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

        if (currentZone.equals("entrada")) {
            drawEntradaText(g2);
            if (!primerLogin) TutorialNPC.drawDecorative(g2, TILE_SIZE);
        } else drawInteractionHint(g2);
        drawDoorHint(g2);
        drawRecepcionWalls(g2);

        if (tutorialNPC != null) tutorialNPC.draw(g2, TILE_SIZE);

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
            case 5: // Puerta (manija derecha)
                g.setColor(new Color(92, 58, 33));
                g.fillRect(x, y, s, s);
                g.setColor(new Color(61, 37, 20));
                g.setStroke(new BasicStroke(2));
                g.drawRect(x+3, y+3, s-6, s-6);
                g.setColor(new Color(209, 161, 58));
                g.fillOval(x+s-10, y+s/2-3, 6, 6);
                break;
            case 26: // Puerta volteada en Y (manija izquierda)
                g.setColor(new Color(92, 58, 33));
                g.fillRect(x, y, s, s);
                g.setColor(new Color(61, 37, 20));
                g.setStroke(new BasicStroke(2));
                g.drawRect(x+3, y+3, s-6, s-6);
                g.setColor(new Color(209, 161, 58));
                g.fillOval(x+4, y+s/2-3, 6, 6);
                break;
            case 27: // Puerta rotada 90° horario (manija arriba)
                g.setColor(new Color(92, 58, 33));
                g.fillRect(x, y, s, s);
                g.setColor(new Color(61, 37, 20));
                g.setStroke(new BasicStroke(2));
                g.drawRect(x+3, y+3, s-6, s-6);
                g.setColor(new Color(209, 161, 58));
                g.fillOval(x+s/2-3, y+4, 6, 6);
                break;
            case 28: // Puerta rotada 90° antihorario (manija abajo)
                g.setColor(new Color(92, 58, 33));
                g.fillRect(x, y, s, s);
                g.setColor(new Color(61, 37, 20));
                g.setStroke(new BasicStroke(2));
                g.drawRect(x+3, y+3, s-6, s-6);
                g.setColor(new Color(209, 161, 58));
                g.fillOval(x+s/2-3, y+s-10, 6, 6);
                break;
            case 29: // Escalón (fila alta, más claro)
                g.setColor(new Color(190, 190, 190));
                g.fillRect(x, y, s, s/2);
                g.setColor(new Color(110, 110, 110));
                g.fillRect(x, y+s/2, s, s/2);
                break;
            case 30: // Escalón (fila media, más oscuro)
                g.setColor(new Color(140, 140, 140));
                g.fillRect(x, y, s, s/2);
                g.setColor(new Color(75, 75, 75));
                g.fillRect(x, y+s/2, s, s/2);
                break;
            case 31: // Escalón (fila baja, más oscuro aún)
                g.setColor(new Color(90, 90, 90));
                g.fillRect(x, y, s, s/2);
                g.setColor(new Color(40, 40, 40));
                g.fillRect(x, y+s/2, s, s/2);
                break;
            case 33: { // Trabajo — tablón con folios/entregas
                g.setColor(new Color(30, 55, 35));
                g.fillRect(x, y, s, s);
                // Marco del tablón
                g.setColor(new Color(110, 75, 40));
                g.fillRect(x+2, y+2, s-4, s-4);
                g.setColor(new Color(160, 110, 60));
                g.drawRect(x+2, y+2, s-4, s-4);
                // Folios
                int[][] folios = {{6,6,12,14},{20,6,12,14},{6,22,12,14},{20,22,12,14}};
                Color[] fColores = {Color.WHITE, new Color(255,255,180), Color.WHITE, new Color(220,240,255)};
                for (int i=0; i<folios.length; i++) {
                    g.setColor(fColores[i]);
                    g.fillRect(x+folios[i][0], y+folios[i][1], folios[i][2], folios[i][3]);
                    g.setColor(new Color(180,180,180));
                    g.drawLine(x+folios[i][0]+2, y+folios[i][1]+4,  x+folios[i][0]+folios[i][2]-2, y+folios[i][1]+4);
                    g.drawLine(x+folios[i][0]+2, y+folios[i][1]+7,  x+folios[i][0]+folios[i][2]-2, y+folios[i][1]+7);
                    g.drawLine(x+folios[i][0]+2, y+folios[i][1]+10, x+folios[i][0]+folios[i][2]-2, y+folios[i][1]+10);
                }
                g.setFont(new Font("SansSerif", Font.BOLD, 6));
                g.setColor(new Color(200,230,200));
                g.drawString("TAREAS", x+4, y+s-4);
                break;
            }
            case 32: { // Temario — estantería con carpetas
                g.setColor(new Color(45, 30, 80));
                g.fillRect(x, y, s, s);
                // Estantes
                g.setColor(new Color(80, 55, 120));
                g.fillRect(x+2, y+s-6, s-4, 4);
                g.fillRect(x+2, y+s/2-2, s-4, 4);
                // Carpetas
                int[] cw = {7, 6, 7, 6}; int[] ch = {14, 12, 13, 11};
                Color[] cc = {new Color(220,80,80), new Color(80,160,220), new Color(80,200,120), new Color(230,180,60)};
                int cx2 = x + 3;
                for (int i = 0; i < 4; i++) {
                    g.setColor(cc[i]); g.fillRect(cx2, y+s/2-ch[i]-2, cw[i], ch[i]);
                    g.setColor(cc[i].darker()); g.fillRect(cx2, y+s/2-ch[i]-2, cw[i], 3);
                    cx2 += cw[i] + 2;
                }
                // Label "PDF"
                g.setFont(new Font("SansSerif", Font.BOLD, 7));
                g.setColor(new Color(200, 180, 255));
                g.drawString("PDF", x+s/2-8, y+s-8);
                break;
            }
            case 8: // Suelo recepción (tablero de ajedrez)
                g.setColor(((x/s + y/s) % 2 == 0) ? new Color(224,224,224) : new Color(245,245,245));
                g.fillRect(x, y, s, s);
                break;
            case 9: // Pared recepción
                g.setColor(new Color(74, 107, 140));
                g.fillRect(x, y, s, s);
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
            case 18: // Mesa comedor (metal)
                g.setColor(new Color(125, 127, 133));
                g.fillRect(x, y, s, s);
                g.setColor(new Color(195, 197, 202));
                g.fillRect(x+3, y+3, s-6, s-6);
                g.setColor(new Color(215, 217, 220));
                g.fillRect(x+4, y+4, s-12, s-12);
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
        int x1 = 6  * TILE_SIZE;   // borde izquierdo de las escaleras
        int x2 = 15 * TILE_SIZE;   // borde derecho de las escaleras
        int y1 = 12 * TILE_SIZE;   // fila donde empieza la barrera
        int y2 = MAP_ROWS * TILE_SIZE;
        g.setStroke(new java.awt.BasicStroke(2f));
        g.setColor(new Color(100, 100, 100, 200));
        g.drawLine(x1, y1, x1, y2);
        g.drawLine(x2, y1, x2, y2);
        g.setStroke(new java.awt.BasicStroke(1f));
    }

    // ── Pistas de interacción ─────────────────────────────────────────

    private void drawInteractionHint(Graphics2D g) {
        nearbyInteraction = getNearbyInteraction();
        if (nearbyInteraction == null) return;
        String label;
        switch (nearbyInteraction) {
            case "tablon":
                label = "profesor".equals(myRol) ? "Tablón / Publicar  [E]" : "Tablón de Anuncios  [E]"; break;
            case "horario":
                label = "Horario / Calendario  [E]"; break;
            case "menu_comedor":
                label = "Menú del Comedor  [E]"; break;
            case "notas":
                label = "profesor".equals(myRol) ? "Poner nota a alumno  [E]" : "Notas y Calificaciones  [E]"; break;
            case "trabajo":
                label = "profesor".equals(myRol) ? "Trabajos — Gestionar  [E]" : "Trabajos — Entregar  [E]"; break;
            case "temario":
                label = "profesor".equals(myRol) ? "Temario — Gestionar  [E]" : "Temario — Descargar  [E]"; break;
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

    private void drawDoorHint(Graphics2D g) {
        String dest = getDoorTransition();
        if (dest == null) return;
        String name;
        switch (dest) {
            case "recepcion":    name = "Recepción";  break;
            case "entrada":      name = "Salida";      break;
            case "conserjeria":  name = "Comedor";     break;
            default:             name = dest;
        }
        String label = "Puerta → " + name + "  [Enter]";
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(label) + 16;
        int tx = W / 2 - tw / 2;
        int ty = H - TILE_SIZE;
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRoundRect(tx, ty - fm.getAscent() - 4, tw, fm.getHeight() + 8, 8, 8);
        g.setColor(new Color(100, 220, 255));
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
        final int CHAT_W  = 280;
        final int MARGIN  = 10;
        final int TAB_H   = 22;
        final int MSG_H   = 148;
        final int INPUT_H = 28;
        final int PAD     = 6;
        final int chatX   = W - CHAT_W - MARGIN;
        final int chatBot = H - MARGIN;

        Font msgFont  = new Font("SansSerif", Font.PLAIN, 11);
        Font nameFont = new Font("SansSerif", Font.BOLD, 10);
        Font tabFont  = new Font("SansSerif", Font.PLAIN, 10);
        Font tabFontB = new Font("SansSerif", Font.BOLD, 10);

        // ── Chat oculto ───────────────────────────────────────────────────
        if (!chatVisible) {
            int tw = 72, th = TAB_H;
            int tx = W - tw - MARGIN, ty = chatBot - th;
            chatToggleBtn.setBounds(tx, ty, tw, th);
            g.setColor(new Color(12, 15, 30, 215));
            g.fillRoundRect(tx, ty, tw, th, 6, 6);
            g.setColor(new Color(60, 90, 140));
            g.setStroke(new BasicStroke(1f));
            g.drawRoundRect(tx, ty, tw, th, 6, 6);
            g.setFont(tabFontB);
            FontMetrics fh = g.getFontMetrics();
            g.setColor(new Color(150, 185, 230));
            String lbl = "Chat  ▲";
            g.drawString(lbl, tx + (tw - fh.stringWidth(lbl)) / 2,
                         ty + (th + fh.getAscent() - fh.getDescent()) / 2);
            return;
        }

        // ── Fondo del panel ───────────────────────────────────────────────
        int inputExtra = chatActive ? INPUT_H + 4 : 0;
        int panelH = TAB_H + 1 + MSG_H + 4 + inputExtra;
        int panelY = chatBot - panelH;

        g.setColor(new Color(12, 15, 30, 215));
        g.fillRoundRect(chatX, panelY, CHAT_W, panelH, 10, 10);
        g.setColor(new Color(60, 90, 140));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(chatX, panelY, CHAT_W, panelH, 10, 10);

        // ── Fila de pestañas ──────────────────────────────────────────────
        // Botón ▼ (esquina derecha del panel)
        int btnW = 22, btnH = 14;
        int btnX = chatX + CHAT_W - btnW - 4;
        int btnY = panelY + (TAB_H - btnH) / 2;
        chatToggleBtn.setBounds(btnX, btnY, btnW, btnH);
        g.setColor(new Color(40, 60, 100, 190));
        g.fillRoundRect(btnX, btnY, btnW, btnH, 3, 3);
        g.setColor(new Color(80, 120, 180));
        g.drawRoundRect(btnX, btnY, btnW, btnH, 3, 3);
        g.setFont(tabFontB);
        FontMetrics fb = g.getFontMetrics();
        g.setColor(new Color(180, 210, 245));
        g.drawString("▼", btnX + (btnW - fb.stringWidth("▼")) / 2,
                     btnY + (btnH + fb.getAscent() - fb.getDescent()) / 2);

        // Pestaña Global + pestañas PM
        int tabX = chatX + 2;
        int tabsMaxX = btnX - 3;
        g.setFont(tabFont);
        FontMetrics ft = g.getFontMetrics();

        // Global
        boolean globalActive = (activePmIdx == -1);
        int globalW = ft.stringWidth("Global") + PAD * 2;
        drawTab(g, tabX, panelY + 3, globalW, TAB_H - 4, "Global", globalActive, false, ft, tabFont, tabFontB);
        globalTabRect.setBounds(tabX, panelY, globalW, TAB_H);
        tabX += globalW + 2;

        // PM tabs
        pmTabRects.clear();
        pmCloseRects.clear();
        synchronized (pmChats) {
            for (int i = 0; i < pmChats.size(); i++) {
                if (tabX >= tabsMaxX - 30) break;
                PmChat pm = pmChats.get(i);
                boolean pmActive = (activePmIdx == i);
                String label = pm.name.length() > 9 ? pm.name.substring(0, 8) + "…" : pm.name;
                final int CLOSE_W = 10;
                int tabW = Math.min(ft.stringWidth(label) + PAD * 2 + CLOSE_W + 4, tabsMaxX - tabX);
                if (tabW < 28) break;
                drawTab(g, tabX, panelY + 3, tabW, TAB_H - 4, label, pmActive, pm.hasUnread, ft, tabFont, tabFontB);
                // ✕
                int cx = tabX + tabW - CLOSE_W - 3, cy = panelY + (TAB_H - CLOSE_W) / 2;
                g.setColor(pmActive ? new Color(200, 100, 100) : new Color(140, 100, 100));
                g.setFont(tabFontB);
                g.drawString("✕", cx + 1, cy + fb.getAscent() - 1);
                pmTabRects.add(new Rectangle(tabX, panelY, tabW, TAB_H));
                pmCloseRects.add(new Rectangle(cx, cy, CLOSE_W + 2, CLOSE_W + 2));
                tabX += tabW + 2;
            }
        }

        // Separador bajo pestañas
        g.setColor(new Color(60, 90, 140));
        g.drawLine(chatX + 1, panelY + TAB_H, chatX + CHAT_W - 1, panelY + TAB_H);

        // ── Input ─────────────────────────────────────────────────────────
        int inputTop = chatBot;
        if (chatActive) {
            inputTop = chatBot - INPUT_H;
            // Hint del destinatario
            String hint = activePmIdx >= 0 && activePmIdx < pmChats.size()
                ? "→ " + pmChats.get(activePmIdx).name : "Global";
            g.setFont(new Font("SansSerif", Font.ITALIC, 10));
            g.setColor(new Color(120, 160, 210));
            g.drawString(hint, chatX + 8, inputTop - 3);
            g.setColor(new Color(20, 20, 50, 230));
            g.fillRoundRect(chatX + 4, inputTop, CHAT_W - 8, INPUT_H, 6, 6);
            g.setColor(new Color(74, 144, 217));
            g.setStroke(new BasicStroke(1f));
            g.drawRoundRect(chatX + 4, inputTop, CHAT_W - 8, INPUT_H, 6, 6);
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            FontMetrics fi = g.getFontMetrics();
            g.setColor(Color.WHITE);
            String txt = chatInput.toString() + (System.currentTimeMillis() % 1000 < 500 ? "|" : "");
            g.drawString(txt, chatX + 12, inputTop + (INPUT_H + fi.getAscent() - fi.getDescent()) / 2);
        }

        // ── Mensajes con clipping ─────────────────────────────────────────
        int msgBot = inputTop - (chatActive ? 14 : 0);
        int msgTop = panelY + TAB_H + 4;
        if (msgBot <= msgTop) return;

        Shape oldClip = g.getClip();
        g.clipRect(chatX + 2, msgTop, CHAT_W - 4, msgBot - msgTop);

        g.setFont(msgFont);
        FontMetrics fm = g.getFontMetrics();
        int lineH   = fm.getHeight();
        int maxTxtW = CHAT_W - PAD * 4;

        if (activePmIdx >= 0 && activePmIdx < pmChats.size()) {
            // ── Mensajes privados ─────────────────────────────────────────
            PmChat pm = pmChats.get(activePmIdx);
            synchronized (pmChats) {
                int curY = msgBot;
                for (int i = pm.messages.size() - 1; i >= 0; i--) {
                    String[] entry = pm.messages.get(i);
                    boolean isOwn  = "1".equals(entry[0]);
                    drawBubble(g, entry[1], isOwn, isOwn ? myName : pm.name,
                               isOwn ? myColor : pm.color,
                               chatX, curY, msgTop, CHAT_W, PAD, lineH, maxTxtW, fm, nameFont);
                    List<String> lines = wrapText(entry[1], fm, maxTxtW);
                    int nameExtra = isOwn ? 0 : lineH;
                    curY -= PAD * 2 + nameExtra + lines.size() * lineH + 4;
                    if (curY + lineH < msgTop) break;
                }
            }
        } else {
            // ── Chat global ───────────────────────────────────────────────
            synchronized (chatMessages) {
                int curY = msgBot;
                for (int i = chatMessages.size() - 1; i >= 0; i--) {
                    String[] entry = chatMessages.get(i);
                    boolean  isOwn = entry[0].equals(myName);
                    drawBubble(g, entry[2], isOwn, entry[0], parseColor(entry[1]),
                               chatX, curY, msgTop, CHAT_W, PAD, lineH, maxTxtW, fm, nameFont);
                    List<String> lines = wrapText(entry[2], fm, maxTxtW);
                    int nameExtra = isOwn ? 0 : lineH;
                    curY -= PAD * 2 + nameExtra + lines.size() * lineH + 4;
                    if (curY + lineH < msgTop) break;
                }
            }
        }
        g.setClip(oldClip);
    }

    private void drawTab(Graphics2D g, int x, int y, int w, int h,
                         String label, boolean active, boolean unread,
                         FontMetrics ft, Font normal, Font bold) {
        g.setColor(active ? new Color(35, 65, 125, 230) : new Color(20, 25, 55, 180));
        g.fillRoundRect(x, y, w, h, 5, 5);
        if (active) {
            g.setColor(new Color(74, 120, 200));
            g.setStroke(new BasicStroke(1f));
            g.drawRoundRect(x, y, w, h, 5, 5);
        }
        g.setFont(unread ? bold : normal);
        FontMetrics fm = g.getFontMetrics();
        g.setColor(active ? Color.WHITE : (unread ? new Color(255, 200, 80) : new Color(160, 190, 220)));
        g.drawString(label, x + 5, y + (h + fm.getAscent() - fm.getDescent()) / 2);
    }

    private void drawBubble(Graphics2D g, String text, boolean isOwn, String authorName,
                            Color authorColor, int chatX, int curY, int msgTop,
                            int chatW, int pad, int lineH, int maxTxtW,
                            FontMetrics fm, Font nameFont) {
        List<String> lines = wrapText(text, fm, maxTxtW);
        int nameExtra = isOwn ? 0 : lineH;
        int bubbleH   = pad * 2 + nameExtra + lines.size() * lineH;
        int textW = 0;
        for (String l : lines) textW = Math.max(textW, fm.stringWidth(l));
        if (!isOwn) {
            g.setFont(nameFont);
            textW = Math.max(textW, g.getFontMetrics().stringWidth(authorName));
            g.setFont(fm.getFont());
        }
        int bubbleW = Math.min(textW + pad * 2, chatW - 4);
        int bubbleY = curY - bubbleH;

        if (isOwn) {
            int bx = chatX + chatW - bubbleW - 2;
            g.setColor(new Color(40, 80, 150, 210));
            g.fillRoundRect(bx, bubbleY, bubbleW, bubbleH, 10, 10);
            g.setFont(fm.getFont());
            g.setColor(Color.WHITE);
            int ty = bubbleY + pad + fm.getAscent();
            for (String line : lines) { g.drawString(line, bx + pad, ty); ty += lineH; }
        } else {
            int bx = chatX + 2;
            g.setColor(new Color(30, 35, 60, 210));
            g.fillRoundRect(bx, bubbleY, bubbleW, bubbleH, 10, 10);
            g.setFont(nameFont);
            FontMetrics fnm = g.getFontMetrics();
            g.setColor(authorColor);
            g.drawString(authorName, bx + pad, bubbleY + pad + fnm.getAscent());
            g.setFont(fm.getFont());
            g.setColor(new Color(220, 230, 245));
            int ty = bubbleY + pad + lineH + fm.getAscent();
            for (String line : lines) { g.drawString(line, bx + pad, ty); ty += lineH; }
        }
    }

    private List<String> wrapText(String text, FontMetrics fm, int maxW) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isEmpty()) { result.add(""); return result; }
        String[] words = text.split(" ", -1);
        StringBuilder cur = new StringBuilder();
        for (String word : words) {
            String test = cur.length() == 0 ? word : cur + " " + word;
            if (fm.stringWidth(test) <= maxW) {
                cur = new StringBuilder(test);
            } else {
                if (cur.length() > 0) result.add(cur.toString());
                cur = new StringBuilder(word);
            }
        }
        if (cur.length() > 0) result.add(cur.toString());
        if (result.isEmpty()) result.add("");
        return result;
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
                    if (tutorialNPC != null) tutorialNPC.onZoneChanged(currentZone);
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
            case "PM_OPENED":
                // PM_OPENED|fromName|fromColor
                if (p.length >= 3) {
                    final String pmFrom = p[1]; final Color pmColor = parseColor(p[2]);
                    SwingUtilities.invokeLater(() -> { openPmTab(pmFrom, pmColor); chatVisible = true; repaint(); });
                }
                break;
            case "PM_RECV":
                // PM_RECV|fromName|fromColor|message
                if (p.length >= 4) {
                    final String pmSender = p[1]; final Color pmSenderColor = parseColor(p[2]);
                    final String pmTxt = msg.substring(p[0].length()+p[1].length()+p[2].length()+3);
                    SwingUtilities.invokeLater(() -> addPmMessage(pmSender, pmSenderColor, pmTxt));
                }
                break;
            case "DATA_TABLON":
                showData("tablon", msg.substring("DATA_TABLON|".length()));
                if (tutorialNPC != null) tutorialNPC.onInteracted("tablon");
                break;
            case "DATA_HORARIO":
                showData("horario", msg.substring("DATA_HORARIO|".length()));
                if (tutorialNPC != null) tutorialNPC.onInteracted("horario");
                break;
            case "DATA_MENU_COMEDOR":
                showData("menu_comedor", msg.substring("DATA_MENU_COMEDOR|".length()));
                if (tutorialNPC != null) tutorialNPC.onInteracted("menu_comedor");
                break;
            case "DATA_NOTAS":
                showData("notas", msg.substring("DATA_NOTAS|".length()));
                if (tutorialNPC != null) tutorialNPC.onInteracted("notas");
                break;
            case "DATA_ALUMNOS":
                showProfesorNotas(msg.substring("DATA_ALUMNOS|".length()));
                break;
            case "PROF_OK":
                showNotif(p.length >= 2 && "nota".equals(p[1]) ? "Nota guardada correctamente" : "Anuncio publicado correctamente", false);
                break;
            case "PROF_ERR":
                showNotif(p.length >= 2 ? p[1] : "Error", true);
                break;
            case "TRABAJO_DATA": {
                // TRABAJO_DATA|myCurso|cursosDisp|trabajos<<STUDENTS>>students<<ENTREGAS>>entregas
                if (p.length >= 4) {
                    final String myCurso    = p[1];
                    final String cursosDisp = p[2];
                    final String rest = msg.substring(p[0].length()+p[1].length()+p[2].length()+3);
                    int s1 = rest.indexOf("<<STUDENTS>>"), s2 = rest.indexOf("<<ENTREGAS>>");
                    final String trabajos = s1 >= 0 ? rest.substring(0, s1) : rest;
                    final String students = (s1 >= 0 && s2 > s1) ? rest.substring(s1+12, s2) : "";
                    final String entregas = s2 >= 0 ? rest.substring(s2+12) : "";
                    SwingUtilities.invokeLater(() -> {
                        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
                        trabajoDialog = new TrabajoDialog(parent, client, myRol, myCurso, myUserId, cursosDisp, trabajos, students, entregas);
                        trabajoDialog.setVisible(true);
                    });
                }
                break;
            }
            case "TRABAJO_FILE": {
                if (p.length >= 3) {
                    final String nombre = p[1];
                    final String b64 = msg.substring(p[0].length()+p[1].length()+2);
                    SwingUtilities.invokeLater(() -> { if (trabajoDialog != null) trabajoDialog.receiveFile(nombre, b64); });
                }
                break;
            }
            case "TRABAJO_OK": {
                final String okData = msg;
                SwingUtilities.invokeLater(() -> { if (trabajoDialog != null) trabajoDialog.handleOk(okData); });
                break;
            }
            case "TRABAJO_ERR": {
                final String errMsg = p.length >= 2 ? p[1] : "Error";
                SwingUtilities.invokeLater(() -> { if (trabajoDialog != null) trabajoDialog.handleErr(errMsg); });
                break;
            }
            case "TEMARIO_DATA": {
                // TEMARIO_DATA|myCurso|cursosDisp|carpetasSerial<<DOCS>>docsSerial
                if (p.length >= 4) {
                    final String myCurso   = p[1];
                    final String cursosDisp = p[2];
                    final String rest = msg.substring(p[0].length()+p[1].length()+p[2].length()+3);
                    int sep = rest.indexOf("<<DOCS>>");
                    final String carpetas = sep >= 0 ? rest.substring(0, sep) : rest;
                    final String docs     = sep >= 0 ? rest.substring(sep + 8) : "";
                    SwingUtilities.invokeLater(() -> {
                        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
                        temarioDialog = new TemarioDialog(parent, client, myRol, myCurso, cursosDisp, carpetas, docs);
                        temarioDialog.setVisible(true);
                    });
                }
                break;
            }
            case "TEMARIO_FILE": {
                // TEMARIO_FILE|nombre|base64data
                if (p.length >= 3) {
                    final String nombre = p[1];
                    final String b64 = msg.substring(p[0].length()+p[1].length()+2);
                    SwingUtilities.invokeLater(() -> { if (temarioDialog != null) temarioDialog.receiveFile(nombre, b64); });
                }
                break;
            }
            case "TEMARIO_OK": {
                final String okData = msg;
                SwingUtilities.invokeLater(() -> { if (temarioDialog != null) temarioDialog.handleOk(okData); });
                break;
            }
            case "TEMARIO_ERR": {
                final String errMsg = p.length >= 2 ? p[1] : "Error";
                SwingUtilities.invokeLater(() -> { if (temarioDialog != null) temarioDialog.handleErr(errMsg); });
                break;
            }
        }
    }

    // ── Callbacks del tutorial NPC ────────────────────────────────────
    public void onTutorialComplete() {
        primerLogin  = false;
        tutorialNPC  = null;
        client.sendMessage("TUTORIAL_DONE");
        repaint();
    }

    public void openFakePM(String name) {
        synchronized (pmChats) {
            for (int i = 0; i < pmChats.size(); i++) {
                if (pmChats.get(i).isFake) { activePmIdx = i; chatVisible = true; repaint(); return; }
            }
            PmChat pm = new PmChat(name, new Color(255, 225, 60));
            pm.isFake = true;
            pmChats.add(pm);
            activePmIdx = pmChats.size() - 1;
        }
        chatVisible = true;
        repaint();
    }

    public void addFakePMMessage(String text) {
        synchronized (pmChats) {
            for (PmChat pm : pmChats) {
                if (pm.isFake) {
                    pm.messages.add(new String[]{"0", text});
                    pm.hasUnread = (activePmIdx < 0 || pmChats.get(activePmIdx) != pm);
                    break;
                }
            }
        }
        repaint();
    }

    private void openPmTab(String name, Color color) {
        synchronized (pmChats) {
            for (int i = 0; i < pmChats.size(); i++) {
                if (pmChats.get(i).name.equals(name)) { activePmIdx = i; return; }
            }
            pmChats.add(new PmChat(name, color));
            activePmIdx = pmChats.size() - 1;
        }
    }

    private void addPmMessage(String fromName, Color color, String text) {
        synchronized (pmChats) {
            for (PmChat pm : pmChats) {
                if (pm.name.equals(fromName)) {
                    pm.messages.add(new String[]{"0", text});
                    if (pm.messages.size() > 50) pm.messages.remove(0);
                    if (activePmIdx < 0 || pmChats.get(activePmIdx) != pm) pm.hasUnread = true;
                    repaint();
                    return;
                }
            }
            // PM no abierto aún: crearlo
            PmChat pm = new PmChat(fromName, color);
            pm.messages.add(new String[]{"0", text});
            pm.hasUnread = true;
            pmChats.add(pm);
            repaint();
        }
    }

    private void showData(String type, String data) {
        SwingUtilities.invokeLater(() -> {
            JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
            if (parent instanceof GameWindow)
                ((GameWindow) parent).showDataDialog(type, data);
        });
    }

    private void showProfesorNotas(String data) {
        SwingUtilities.invokeLater(() -> {
            JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
            new ProfesorNotasDialog(parent, client, data).setVisible(true);
        });
    }

    private void showNotif(String msg, boolean error) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, msg, "Info",
                error ? JOptionPane.ERROR_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
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
            if (playerX == S2_LEFT_DOOR_COL && playerY >= 6 && playerY <= 7)
                return "conserjeria";
            if (playerY == S2_EXIT_ROW &&
                (playerX == S2_EXIT_L1 || playerX == S2_EXIT_L2 ||
                 playerX == S2_EXIT_R1 || playerX == S2_EXIT_R2))
                return "entrada";
        } else if (currentZone.equals("conserjeria")) {
            if (playerX == S3_RIGHT_DOOR_COL && playerY >= 6 && playerY <= 7)
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
            if (playerY <= 3 && playerX >= 2  && playerX <= 5)  return "tablon";
            if (playerY <= 3 && playerX >= 14 && playerX <= 17) return "horario";
        } else if (currentZone.equals("conserjeria")) {
            if (playerY <= 3 && playerX >= 1  && playerX <= 4)  return "menu_comedor";
            if (playerY <= 3 && playerX >= 15 && playerX <= 18) return "notas";
            if (playerY <= 3 && playerX >= 9  && playerX <= 11) return "trabajo";
            if (playerY <= 3 && playerX >= 12 && playerX <= 15) return "temario";
        }
        return null;
    }

    private void handleProfesorInteract(String type) {
        switch (type) {
            case "tablon": {
                String[] opts = {"Ver anuncios", "Publicar anuncio"};
                int choice = JOptionPane.showOptionDialog(this, "¿Qué deseas hacer?", "Tablón",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, opts, opts[0]);
                if (choice == 0) client.sendMessage("INTERACT|tablon");
                else if (choice == 1) {
                    JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
                    new ProfesorAnuncioDialog(parent, client).setVisible(true);
                }
                break;
            }
            case "notas":
                client.sendMessage("PROF_GET_ALUMNOS");
                break;
            default:
                client.sendMessage("INTERACT|" + type);
        }
    }

    // ── Input ─────────────────────────────────────────────────────────

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        pressedKeys.add(code);

        if (chatActive) {
            if (code == KeyEvent.VK_ENTER) {
                String txt = chatInput.toString().trim();
                if (!txt.isEmpty()) {
                    if (activePmIdx >= 0 && activePmIdx < pmChats.size()) {
                        PmChat pm = pmChats.get(activePmIdx);
                        if (pm.isFake) {
                            synchronized (pmChats) {
                                pm.messages.add(new String[]{"1", txt});
                            }
                            if (tutorialNPC != null) tutorialNPC.onFakePMSent();
                        } else {
                            client.sendMessage("PM_MSG|" + pm.name + "|" + txt);
                            synchronized (pmChats) {
                                pm.messages.add(new String[]{"1", txt});
                                if (pm.messages.size() > 50) pm.messages.remove(0);
                            }
                        }
                    } else {
                        client.sendMessage("CHAT|" + txt);
                        if (tutorialNPC != null) tutorialNPC.onChatSent();
                    }
                }
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
                if (chatVisible) { chatActive = true; chatInput.setLength(0); skipNextTyped = true; }
                break;
            case KeyEvent.VK_Y:
                if (tutorialNPC != null) tutorialNPC.onYPressed(playerX, playerY);
                break;
            case KeyEvent.VK_R: {
                String nearest = null; Color nearestColor = Color.WHITE; int nearestDist = 2;
                for (Map.Entry<String, PlayerData> en : otherPlayers.entrySet()) {
                    PlayerData pd = en.getValue();
                    int dist = Math.max(Math.abs(playerX - pd.x), Math.abs(playerY - pd.y));
                    if (dist <= 1 && dist < nearestDist) {
                        nearest = en.getKey(); nearestColor = pd.color; nearestDist = dist;
                    }
                }
                if (nearest != null) {
                    client.sendMessage("PM_OPEN|" + nearest);
                    openPmTab(nearest, nearestColor);
                    chatVisible = true;
                }
                break;
            }
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
                if (inter != null) {
                    if ("profesor".equals(myRol)) handleProfesorInteract(inter);
                    else client.sendMessage("INTERACT|" + inter);
                }
                break;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) { pressedKeys.remove(e.getKeyCode()); }

    @Override
    public void keyTyped(KeyEvent e) {
        if (chatActive) {
            if (skipNextTyped) { skipNextTyped = false; return; }
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
