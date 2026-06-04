import java.awt.*;

public class TutorialNPC {

    // ── Constantes de pasos ──────────────────────────────────────────
    private static final int WAIT_Y        = 0;
    private static final int WAIT_ZONE     = 1;
    private static final int WAIT_INTERACT = 2;
    private static final int WAIT_CHAT     = 3;
    private static final int WAIT_PM       = 4;

    // Datos por paso: zona destino del NPC
    private static final String[] STEP_ZONE = {
        "entrada","entrada","entrada",
        "recepcion","recepcion","recepcion",
        "conserjeria","conserjeria","conserjeria","conserjeria",
        null, null, null, null  // follow player
    };

    // Posición destino del NPC (-1 = seguir al jugador)
    // Colocado al lado del tile de interacción para no taparlo con la burbuja
    private static final int[] STEP_TX = { -1,-1, 16,  6, 18,  2,  5, 19, 11,  8, -1,-1,-1,-1 };
    private static final int[] STEP_TY = { -1,-1,  9,  3,  3,  7,  3,  3,  3,  3, -1,-1,-1,-1 };

    // Tipo de espera por paso
    private static final int[] WAIT_TYPE = {
        WAIT_Y, WAIT_Y, WAIT_ZONE,
        WAIT_INTERACT, WAIT_INTERACT, WAIT_ZONE,
        WAIT_INTERACT, WAIT_INTERACT, WAIT_Y, WAIT_Y,
        WAIT_CHAT, WAIT_Y, WAIT_PM, WAIT_Y
    };

    // Condición de zona / interacción
    private static final String[] WAIT_ZONE_TARGET     = { null,null,"recepcion", null,null,"conserjeria", null,null,null,null, null,null,null,null };
    private static final String[] WAIT_INTERACT_TARGET = { null,null,null, "tablon","horario",null, "menu_comedor","notas",null,null, null,null,null,null };

    // Diálogos (líneas cortas para la burbuja)
    private static final String[][] DIALOGUES = {
        {"¡Hola! Soy Sol, tu guía.", "Pulsa Y para hablar."},
        {"¡Bienvenido/a al colegio!", "Voy a enseñarte todo.", "¡Sígueme! [Y]"},
        {"Esta es la puerta principal.", "¡Úsala para entrar", "a la recepción!"},
        {"¡Recepción! Aquí está","el tablón de anuncios.","Acércate y pulsa E."},
        {"¡Genial! Y aquí está","el horario de clases.","Ábrelo con E."},
        {"¡Muy bien! Ahora al comedor.","Usa la puerta","de la izquierda."},
        {"¡El comedor! Aquí está","el menú del día.","Ábrelo con E."},
        {"Aquí están tus notas","y calificaciones.","Ábrelas con E."},
        {"El temario está aquí.","Los profes subirán PDFs","para descargar. [Y]"},
        {"¡Y los trabajos aquí!","Los profes publican tareas","y entregas PDFs. [Y]"},
        {"¡Genial! El chat global.","Pulsa T, escribe algo","y envíalo con Enter."},
        {"Chat privado: acércate","a un jugador y pulsa R.","¡Te abro uno ahora! [Y]"},
        {"Escríbeme algo en el","chat privado que", "ha abierto."},
        {"¡Enhorabuena! Ya sabes","usar el colegio.","¡Buena suerte! [Y]"},
    };

    // ── Estado ───────────────────────────────────────────────────────
    private int    step = 0;
    private float  visualX, visualY;
    private int    npcX, npcY;
    private int    targetX, targetY;
    private String currentZone;

    private final GamePanel panel;

    // ── Constructor ──────────────────────────────────────────────────
    public TutorialNPC(GamePanel panel, int playerX, int playerY, String zone) {
        this.panel       = panel;
        this.currentZone = zone;
        npcX    = playerX + 1 < GamePanel.MAP_COLS ? playerX + 1 : playerX - 1;
        npcY    = playerY;
        visualX = npcX;
        visualY = npcY;
        targetX = npcX;
        targetY = npcY;
    }

    // ── Update (llamado cada frame) ───────────────────────────────────
    public void update(int playerX, int playerY, String zone) {
        // Teleport NPC when player changes zone
        if (!zone.equals(currentZone)) {
            currentZone = zone;
            npcX    = playerX + 1 < GamePanel.MAP_COLS ? playerX + 1 : playerX - 1;
            npcY    = playerY;
            visualX = npcX;
            visualY = npcY;
            recalcTarget(playerX, playerY);
        }

        // Recalculate target for "follow player" steps
        if (step < STEP_TX.length && STEP_TX[step] == -1) {
            targetX = playerX + 1 < GamePanel.MAP_COLS ? playerX + 1 : playerX - 1;
            targetY = playerY;
        }

        // Smooth movement toward target — clamped to avoid overshoot/vibration
        float speed = 0.18f;
        float dx = targetX - visualX;
        float dy = targetY - visualY;
        if (Math.abs(dx) > 0.01f)
            visualX += Math.signum(dx) * Math.min(speed, Math.abs(dx));
        else
            visualX = targetX;
        if (Math.abs(dy) > 0.01f)
            visualY += Math.signum(dy) * Math.min(speed, Math.abs(dy));
        else
            visualY = targetY;
        npcX = Math.round(visualX);
        npcY = Math.round(visualY);
    }

    private void recalcTarget(int px, int py) {
        if (step >= STEP_TX.length) return;
        if (STEP_TX[step] == -1) {
            targetX = px + 1 < GamePanel.MAP_COLS ? px + 1 : px - 1;
            targetY = py;
        } else if (STEP_ZONE[step] == null || STEP_ZONE[step].equals(currentZone)) {
            targetX = STEP_TX[step];
            targetY = STEP_TY[step];
        }
    }

    // ── Eventos ───────────────────────────────────────────────────────
    public void onYPressed(int px, int py) {
        if (step >= WAIT_TYPE.length) return;
        if (WAIT_TYPE[step] != WAIT_Y) return;
        int dist = Math.max(Math.abs(px - npcX), Math.abs(py - npcY));
        if (dist > 2) return;
        advance();
    }

    public void onZoneChanged(String newZone) {
        if (step >= WAIT_TYPE.length) return;
        if (WAIT_TYPE[step] != WAIT_ZONE) return;
        if (newZone.equals(WAIT_ZONE_TARGET[step])) advance();
    }

    public void onInteracted(String type) {
        if (step >= WAIT_TYPE.length) return;
        if (WAIT_TYPE[step] != WAIT_INTERACT) return;
        if (type.equals(WAIT_INTERACT_TARGET[step])) advance();
    }

    public void onChatSent() {
        if (step >= WAIT_TYPE.length) return;
        if (WAIT_TYPE[step] != WAIT_CHAT) return;
        advance();
    }

    public void onFakePMSent() {
        if (step >= WAIT_TYPE.length) return;
        if (WAIT_TYPE[step] != WAIT_PM) return;
        panel.addFakePMMessage("¡Muy bien hecho! 🌟");
        advance();
    }

    private void advance() {
        step++;
        if (step >= DIALOGUES.length) {
            panel.onTutorialComplete();
            return;
        }
        // Si el nuevo paso necesita moverse a una posición fija, actualizar target
        if (step < STEP_TX.length && STEP_TX[step] != -1
                && (STEP_ZONE[step] == null || STEP_ZONE[step].equals(currentZone))) {
            targetX = STEP_TX[step];
            targetY = STEP_TY[step];
        }
        // Abrir fake PM en paso 12
        if (step == 12) panel.openFakePM("Sol");
    }

    // ── Dibujo ────────────────────────────────────────────────────────
    public void draw(Graphics2D g, int ts) {
        if (step >= DIALOGUES.length) return;

        int px = (int)(visualX * ts) + ts / 2;
        int py = (int)(visualY * ts) + ts / 2;
        int r  = 10;

        // Rayos del sol (8 rayos)
        g.setColor(new Color(255, 210, 50));
        Stroke old = g.getStroke();
        g.setStroke(new BasicStroke(2.5f));
        long t = System.currentTimeMillis();
        for (int i = 0; i < 8; i++) {
            double ang = Math.toRadians(i * 45 + (t / 120) % 360);
            int x1 = (int)(px + (r + 2)  * Math.cos(ang));
            int y1 = (int)(py + (r + 2)  * Math.sin(ang));
            int x2 = (int)(px + (r + 8)  * Math.cos(ang));
            int y2 = (int)(py + (r + 8)  * Math.sin(ang));
            g.drawLine(x1, y1, x2, y2);
        }
        g.setStroke(old);

        // Cuerpo circular
        g.setColor(new Color(255, 230, 60));
        g.fillOval(px - r, py - r, r * 2, r * 2);
        g.setColor(new Color(200, 155, 0));
        g.setStroke(new BasicStroke(1.5f));
        g.drawOval(px - r, py - r, r * 2, r * 2);
        g.setStroke(old);

        // Ojos (estilo sprite jugador)
        g.setColor(new Color(55, 35, 5));
        g.fillOval(px - 4, py - 3, 3, 4);
        g.fillOval(px + 1, py - 3, 3, 4);

        // Sonrisa
        g.setStroke(new BasicStroke(1.5f));
        g.drawArc(px - 4, py + 2, 8, 5, 0, -180);
        g.setStroke(old);

        // Burbuja de diálogo
        drawBubble(g, px, py - r - 4, ts * GamePanel.MAP_COLS);
    }

    private void drawBubble(Graphics2D g, int cx, int topY, int maxW) {
        if (step >= DIALOGUES.length) return;
        String[] lines     = DIALOGUES[step];
        boolean  showY     = (WAIT_TYPE[step] == WAIT_Y);

        Font f  = new Font("SansSerif", Font.PLAIN, 10);
        Font fb = new Font("SansSerif", Font.BOLD, 10);
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics();

        int lineH = fm.getHeight();
        int textW = 0;
        for (String l : lines) textW = Math.max(textW, fm.stringWidth(l));
        if (showY) {
            g.setFont(fb);
            textW = Math.max(textW, g.getFontMetrics().stringWidth("▶ Y para continuar"));
            g.setFont(f);
        }

        int totalLines = lines.length + (showY ? 1 : 0);
        int bw = textW + 16;
        int bh = totalLines * lineH + 10;
        int bx = cx - bw / 2;
        int by = topY - bh - 6;

        // Mantener en pantalla
        bx = Math.max(4, Math.min(bx, maxW - bw - 4));
        by = Math.max(4, by);

        // Fondo
        g.setColor(new Color(240, 248, 255, 235));
        g.fillRoundRect(bx, by, bw, bh, 10, 10);
        g.setColor(new Color(100, 160, 240));
        Stroke old = g.getStroke();
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(bx, by, bw, bh, 10, 10);
        g.setStroke(old);

        // Cola triangular apuntando al NPC
        int[] tx = { cx - 4, cx + 4, cx };
        int[] ty = { by + bh, by + bh, by + bh + 6 };
        g.setColor(new Color(240, 248, 255, 235));
        g.fillPolygon(tx, ty, 3);
        g.setColor(new Color(100, 160, 240));
        g.drawLine(cx - 4, by + bh, cx, by + bh + 6);
        g.drawLine(cx + 4, by + bh, cx, by + bh + 6);

        // Texto
        g.setFont(f);
        g.setColor(new Color(20, 30, 70));
        int ty2 = by + 6 + fm.getAscent();
        for (String line : lines) { g.drawString(line, bx + 8, ty2); ty2 += lineH; }

        // "Y para continuar" parpadeante
        if (showY) {
            boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
            g.setFont(fb);
            g.setColor(blink ? new Color(40, 100, 210) : new Color(120, 170, 230));
            g.drawString("▶ Y para continuar", bx + 8, ty2);
        }
    }

    // ── Sol decorativo (tutorial completado, esquina entrada) ─────────
    public static void drawDecorative(Graphics2D g, int ts) {
        int px = 18 * ts + ts / 2;
        int py = 2  * ts + ts / 2;
        int r  = 7;

        g.setColor(new Color(255, 215, 50, 160));
        Stroke old = g.getStroke();
        g.setStroke(new BasicStroke(1.5f));
        for (int i = 0; i < 8; i++) {
            double ang = Math.toRadians(i * 45);
            g.drawLine(
                (int)(px + (r + 1) * Math.cos(ang)), (int)(py + (r + 1) * Math.sin(ang)),
                (int)(px + (r + 5) * Math.cos(ang)), (int)(py + (r + 5) * Math.sin(ang)));
        }
        g.setStroke(old);
        g.setColor(new Color(255, 230, 60, 170));
        g.fillOval(px - r, py - r, r * 2, r * 2);
        g.setColor(new Color(200, 155, 0, 170));
        g.drawOval(px - r, py - r, r * 2, r * 2);
        g.setColor(new Color(55, 35, 5, 170));
        g.fillOval(px - 3, py - 2, 2, 2);
        g.fillOval(px + 1, py - 2, 2, 2);
        g.setStroke(new BasicStroke(1f));
        g.drawArc(px - 3, py + 1, 6, 3, 0, -180);
        g.setStroke(old);
    }
}
