import java.net.*;
import java.io.*;

public class ClientHandler implements Runnable {

    // ── Dimensiones del mapa (21 cols × 15 filas) ───────────────────────
    static final int MAP_COLS = 21;
    static final int MAP_ROWS = 15;

    // ── Mapas de colisión por zona ────────────────────────────────────────
    private static final boolean[][] COL_ENTRADA     = buildEntradaCollision();
    private static final boolean[][] COL_RECEPCION   = buildRecepcionCollision();
    private static final boolean[][] COL_CONSERJERIA = buildConserjeriaCollision();

    private static boolean[][] buildEntradaCollision() {
        boolean[][] m = new boolean[MAP_COLS][MAP_ROWS];
        // Filas 0-6: edificio bloqueado
        for (int x = 0; x < MAP_COLS; x++)
            for (int y = 0; y <= 6; y++)
                m[x][y] = true;
        // Fila 7 (puertas): paredes laterales bloqueadas; puertas (cols 6-7 y 13-14) libres
        for (int x = 2; x <= 5;  x++) m[x][7] = true;
        for (int x = 8; x <= 12; x++) m[x][7] = true;
        for (int x = 15; x <= 18; x++) m[x][7] = true;
        // Árboles decorativos (bloqueantes)
        m[1][9]  = true; m[1][10]  = true;
        m[19][9] = true; m[19][10] = true;
        m[3][12] = true; m[3][13]  = true;
        m[17][12]= true; m[17][13] = true;
        return m;
    }

    private static boolean[][] buildRecepcionCollision() {
        boolean[][] m = new boolean[MAP_COLS][MAP_ROWS];
        // filas 0-1 son paredes
        for (int x = 0; x < MAP_COLS; x++) { m[x][0] = true; m[x][1] = true; }
        // mostrador central (filas 5-7, cols 7-13)
        for (int x = 7; x <= 13; x++) for (int y = 5; y <= 7; y++) m[x][y] = true;
        // Col 0 filas 5-8 = puerta a conserjería → libre
        return m;
    }

    private static boolean[][] buildConserjeriaCollision() {
        boolean[][] m = new boolean[MAP_COLS][MAP_ROWS];
        // filas 0-1 son paredes
        for (int x = 0; x < MAP_COLS; x++) { m[x][0] = true; m[x][1] = true; }
        // mostrador (filas 7-8, cols 5-15)
        for (int x = 5; x <= 15; x++) { m[x][7] = true; m[x][8] = true; }
        // archivadores (filas 11-14, cols 1, 3, 5)
        for (int y = 11; y <= 14; y++) { m[1][y] = true; m[3][y] = true; m[5][y] = true; }
        // Col 20 filas 5-8 = puerta a recepción → libre
        return m;
    }

    // ── Pared lateral de recepción (del proyecto viejo): borde entre col 5-6
    //    y col 14-15, activa en filas 12-14 ───────────────────────────────
    private static final int S2_WALL_TOP = MAP_ROWS - 3; // fila 12

    private static boolean isRecepcionWallCollision(int ox, int oy, int nx) {
        if (oy < S2_WALL_TOP) return false;
        if ((ox == 5 && nx == 6) || (ox == 6 && nx == 5))   return true;
        if ((ox == 14 && nx == 15) || (ox == 15 && nx == 14)) return true;
        return false;
    }

    // ── Estado del handler ────────────────────────────────────────────────
    private final Socket socket;
    private PrintWriter  out;

    private String currentZone = "entrada";
    private int    x = MAP_COLS / 2;  // 10
    private int    y = MAP_ROWS - 2;  // 13
    private String direction = "down";

    private String  playerName;
    private String  playerColor;
    private String  playerRol;
    private int     playerId;
    private boolean authenticated = false;

    public ClientHandler(Socket socket) { this.socket = socket; }

    @Override
    public void run() {
        try {
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

            String line;
            while ((line = in.readLine()) != null) {
                if (!authenticated) handleAuth(line);
                else                handleGame(line);
            }
        } catch (IOException ignored) {
        } finally {
            onDisconnect();
        }
    }

    public void sendMessage(String msg) {
        if (out != null) out.println(msg);
    }

    // ── Autenticación ─────────────────────────────────────────────────────

    private void handleAuth(String line) {
        String[] parts = line.split("\\|", 4);
        switch (parts[0]) {
            case "LOGIN":
                if (parts.length < 3) { sendMessage("LOGIN_ERR|Formato invalido"); return; }
                String[] u = Server.db.verifyUser(parts[1], parts[2]);
                if (u == null) { sendMessage("LOGIN_ERR|Usuario o contrasena incorrectos"); return; }
                loginSuccess(u[0], u[1], u[2], u[3]);
                break;
            case "REGISTER":
                if (parts.length < 4) { sendMessage("REGISTER_ERR|Formato invalido"); return; }
                String[] nu = Server.db.createUser(parts[1], parts[2], parts[3]);
                if (nu == null) { sendMessage("REGISTER_ERR|El nombre de usuario ya existe"); return; }
                loginSuccess(nu[0], nu[1], nu[2], nu[3]);
                break;
        }
    }

    private void loginSuccess(String id, String nombre, String color, String rol) {
        playerId    = Integer.parseInt(id);
        playerName  = nombre;
        playerColor = color;
        playerRol   = rol;
        authenticated = true;

        if ("admin".equals(playerRol)) {
            sendMessage("LOGIN_OK|" + playerName + "|" + playerColor + "|admin|" + playerId + "|0|0|admin");
        } else {
            Server.state.addPlayer(this, playerName, playerColor, playerRol, playerId, x, y, currentZone);
            sendMessage("LOGIN_OK|" + playerName + "|" + playerColor + "|" + playerRol
                        + "|" + playerId + "|" + x + "|" + y + "|" + currentZone);
            for (GameState.PlayerInfo pi : Server.state.getPlayersInZone(currentZone, this))
                sendMessage("PLAYER_JOINED|" + pi.name + "|" + pi.color
                            + "|" + pi.x + "|" + pi.y + "|" + pi.direction);
            Server.state.broadcastToZone(currentZone,
                "PLAYER_JOINED|" + playerName + "|" + playerColor + "|" + x + "|" + y + "|down", this);
            System.out.println("  Online: " + Server.state.getPlayerCount());
        }

        System.out.println("  Conectado: " + playerName + " (" + playerRol + ")");
    }

    // ── Lógica del juego ──────────────────────────────────────────────────

    private void handleGame(String line) {
        String[] parts = line.split("\\|", 4);
        switch (parts[0]) {
            case "MOVE":              if (parts.length >= 2) handleMove(parts[1]);                            break;
            case "CHANGE_ZONE":       if (parts.length >= 4) handleChangeZone(parts[1], parts[2], parts[3]); break;
            case "INTERACT":          if (parts.length >= 2) handleInteract(parts[1]);                        break;
            case "CHAT":              if (parts.length >= 2) handleChat(line.substring(5));                   break;
            case "PROF_GET_ALUMNOS":      handleProfGetAlumnos();                                              break;
            case "PROF_ADD_NOTA":         handleProfAddNota(line);                                              break;
            case "PROF_ADD_ANUNCIO":      handleProfAddAnuncio(line);                                           break;
            case "ADMIN_GET_USERS":       handleAdminGetUsers();                                                break;
            case "ADMIN_CREATE_USER":     handleAdminCreateUser(line);                                          break;
            case "ADMIN_DELETE_USER":     if (parts.length >= 2) handleAdminDeleteUser(parts[1]);              break;
            case "ADMIN_EDIT_USER":       handleAdminEditUser(line);                                            break;
            case "ADMIN_GET_ANUNCIOS":    if ("admin".equals(playerRol)) sendMessage("DATA_ANUNCIOS_ADMIN|" + Server.db.getAnunciosAdminSerial()); break;
            case "ADMIN_ADD_ANUNCIO":     handleAdminAddAnuncio(line);                                          break;
            case "ADMIN_DEL_ANUNCIO":     if (parts.length >= 2) handleAdminDelAnuncio(parts[1]);              break;
            case "ADMIN_GET_MENU":        if ("admin".equals(playerRol)) sendMessage("DATA_MENU_ADMIN|" + Server.db.getMenuAdminSerial()); break;
            case "ADMIN_UPSERT_MENU":     handleAdminUpsertMenu(line);                                          break;
            case "ADMIN_DEL_MENU":        if (parts.length >= 2) handleAdminDelMenu(parts[1]);                  break;
            case "ADMIN_GET_HORARIO":     handleAdminGetHorario(line);                                          break;
            case "ADMIN_ADD_HORARIO":     handleAdminAddHorario(line);                                          break;
            case "ADMIN_DEL_HORARIO":     if (parts.length >= 2) handleAdminDelHorario(parts[1]);              break;
        }
    }

    private void handleProfGetAlumnos() {
        if (!"profesor".equals(playerRol) && !"admin".equals(playerRol)) return;
        sendMessage("DATA_ALUMNOS|" + Server.db.getAlumnosSerial());
    }

    private void handleProfAddNota(String line) {
        if (!"profesor".equals(playerRol) && !"admin".equals(playerRol)) return;
        // PROF_ADD_NOTA|alumnoId|materia|calificacion|tipo|comentario
        String[] p = line.split("\\|", 6);
        if (p.length < 6) return;
        try {
            int uid = Integer.parseInt(p[1]);
            boolean ok = Server.db.addNota(uid, p[2], p[3], p[4], p[5]);
            sendMessage(ok ? "PROF_OK|nota" : "PROF_ERR|Error al guardar la nota");
        } catch (NumberFormatException e) { sendMessage("PROF_ERR|ID invalido"); }
    }

    private void handleProfAddAnuncio(String line) {
        if (!"profesor".equals(playerRol) && !"admin".equals(playerRol)) return;
        // PROF_ADD_ANUNCIO|titulo|contenido|importante
        String[] p = line.split("\\|", 4);
        if (p.length < 4) return;
        boolean imp = "1".equals(p[3]);
        boolean ok = Server.db.addAnuncio(p[1], p[2], playerName, playerId, imp);
        sendMessage(ok ? "PROF_OK|anuncio" : "PROF_ERR|Error al publicar el anuncio");
    }

    private void handleAdminGetUsers() {
        if (!"admin".equals(playerRol)) return;
        sendMessage("DATA_USERS|" + Server.db.getUsersSerial());
    }

    private void handleAdminCreateUser(String line) {
        if (!"admin".equals(playerRol)) return;
        // ADMIN_CREATE_USER|nombre|password|color|rol|curso|clase
        String[] p = line.split("\\|", 7);
        if (p.length < 7) return;
        String[] nu = Server.db.createUserAdmin(p[1], p[2], p[3], p[4], p[5], p[6]);
        if (nu != null) sendMessage("ADMIN_OK|create");
        else            sendMessage("ADMIN_ERR|El nombre de usuario ya existe");
    }

    private void handleAdminDeleteUser(String idStr) {
        if (!"admin".equals(playerRol)) return;
        try {
            int uid = Integer.parseInt(idStr);
            if (uid == playerId) { sendMessage("ADMIN_ERR|No puedes eliminar tu propia cuenta"); return; }
            sendMessage(Server.db.deleteUser(uid) ? "ADMIN_OK|delete" : "ADMIN_ERR|Usuario no encontrado");
        } catch (NumberFormatException e) { sendMessage("ADMIN_ERR|ID invalido"); }
    }

    private void handleAdminEditUser(String line) {
        if (!"admin".equals(playerRol)) return;
        // ADMIN_EDIT_USER|userId|nombre|password|color|rol|curso|clase
        String[] p = line.split("\\|", 8);
        if (p.length < 8) return;
        try {
            int uid = Integer.parseInt(p[1]);
            sendMessage(Server.db.editUser(uid, p[2], p[3], p[4], p[5], p[6], p[7]) ? "ADMIN_OK|edit" : "ADMIN_ERR|Usuario no encontrado");
        } catch (NumberFormatException e) { sendMessage("ADMIN_ERR|ID invalido"); }
    }

    private void handleAdminAddAnuncio(String line) {
        if (!"admin".equals(playerRol)) return;
        // ADMIN_ADD_ANUNCIO|titulo|contenido|importante
        String[] p = line.split("\\|", 4);
        if (p.length < 4) return;
        boolean ok = Server.db.addAnuncio(p[1], p[2], playerName, playerId, "1".equals(p[3]));
        sendMessage(ok ? "ADMIN_OK|anuncio" : "ADMIN_ERR|Error al crear el anuncio");
    }

    private void handleAdminDelAnuncio(String idStr) {
        if (!"admin".equals(playerRol)) return;
        try {
            sendMessage(Server.db.deleteAnuncio(Integer.parseInt(idStr)) ? "ADMIN_OK|del_anuncio" : "ADMIN_ERR|Anuncio no encontrado");
        } catch (NumberFormatException e) { sendMessage("ADMIN_ERR|ID invalido"); }
    }

    private void handleAdminUpsertMenu(String line) {
        if (!"admin".equals(playerRol)) return;
        // ADMIN_UPSERT_MENU|fecha|primer_plato|segundo_plato|postre|alergenos
        String[] p = line.split("\\|", 6);
        if (p.length < 6) return;
        sendMessage(Server.db.upsertMenuDia(p[1], p[2], p[3], p[4], p[5]) ? "ADMIN_OK|menu" : "ADMIN_ERR|Error al guardar el menu");
    }

    private void handleAdminDelMenu(String idStr) {
        if (!"admin".equals(playerRol)) return;
        try {
            sendMessage(Server.db.deleteMenuDia(Integer.parseInt(idStr)) ? "ADMIN_OK|del_menu" : "ADMIN_ERR|Dia no encontrado");
        } catch (NumberFormatException e) { sendMessage("ADMIN_ERR|ID invalido"); }
    }

    private void handleAdminGetHorario(String line) {
        if (!"admin".equals(playerRol)) return;
        // ADMIN_GET_HORARIO|curso|clase
        String[] p = line.split("\\|", 3);
        if (p.length < 3) return;
        sendMessage("DATA_HORARIO_ADMIN|" + Server.db.getHorarioAdminSerial(p[1], p[2]));
    }

    private void handleAdminAddHorario(String line) {
        if (!"admin".equals(playerRol)) return;
        // ADMIN_ADD_HORARIO|curso|clase|dia|hora_inicio|hora_fin|asignatura|aula
        String[] p = line.split("\\|", 8);
        if (p.length < 8) return;
        sendMessage(Server.db.addHorarioSlot(p[1], p[2], p[3], p[4], p[5], p[6], p[7]) ? "ADMIN_OK|horario" : "ADMIN_ERR|Error al guardar el horario");
    }

    private void handleAdminDelHorario(String idStr) {
        if (!"admin".equals(playerRol)) return;
        try {
            sendMessage(Server.db.deleteHorarioSlot(Integer.parseInt(idStr)) ? "ADMIN_OK|del_horario" : "ADMIN_ERR|Slot no encontrado");
        } catch (NumberFormatException e) { sendMessage("ADMIN_ERR|ID invalido"); }
    }

    private void handleMove(String dir) {
        int nx = x, ny = y;
        switch (dir) {
            case "UP":    ny--; direction = "up";    break;
            case "DOWN":  ny++; direction = "down";  break;
            case "LEFT":  nx--; direction = "left";  break;
            case "RIGHT": nx++; direction = "right"; break;
            default: return;
        }
        if (nx < 0 || nx >= MAP_COLS || ny < 0 || ny >= MAP_ROWS) return;

        boolean[][] colMap = getCollisionMap();
        if (colMap[nx][ny]) return;
        if (currentZone.equals("recepcion") && isRecepcionWallCollision(x, y, nx)) return;

        x = nx; y = ny;
        Server.state.updatePosition(this, x, y, direction);

        sendMessage("POSITION|" + x + "|" + y);
        Server.state.broadcastToZone(currentZone,
            "PLAYER_MOVED|" + playerName + "|" + x + "|" + y + "|" + direction + "|" + playerColor, this);
    }

    private void handleChangeZone(String targetZone, String sxStr, String syStr) {
        int nx, ny;
        try { nx = Integer.parseInt(sxStr); ny = Integer.parseInt(syStr); }
        catch (NumberFormatException e) { return; }

        if (!isValidZone(targetZone)) return;
        if (nx < 0 || nx >= MAP_COLS || ny < 0 || ny >= MAP_ROWS) return;

        String oldZone = currentZone;
        Server.state.broadcastToZone(oldZone, "PLAYER_LEFT|" + playerName, this);

        currentZone = targetZone; x = nx; y = ny; direction = "down";
        Server.state.changeZone(this, currentZone, x, y);

        sendMessage("ZONE_OK|" + currentZone + "|" + x + "|" + y);

        // Enviar jugadores existentes en la nueva zona
        for (GameState.PlayerInfo pi : Server.state.getPlayersInZone(currentZone, this))
            sendMessage("PLAYER_JOINED|" + pi.name + "|" + pi.color
                        + "|" + pi.x + "|" + pi.y + "|" + pi.direction);

        // Avisar a los demás de la nueva zona
        Server.state.broadcastToZone(currentZone,
            "PLAYER_JOINED|" + playerName + "|" + playerColor + "|" + x + "|" + y + "|down", this);

        System.out.println("  " + playerName + ": " + oldZone + " -> " + currentZone);
    }

    private void handleInteract(String type) {
        String data;
        switch (type) {
            case "tablon":
                data = Server.db.getAnunciosSerial();
                break;
            case "horario": {
                String[] info = Server.db.getUserInfo(playerId);
                data = Server.db.getHorarioSerial(info[0], info[1]);
                break;
            }
            case "menu_comedor":
                data = Server.db.getMenuSerial();
                break;
            case "notas":
                data = Server.db.getNotasSerial(playerId);
                break;
            default: return;
        }
        sendMessage("DATA_" + type.toUpperCase() + "|" + data);
    }

    private void handleChat(String message) {
        if (message.isEmpty() || message.length() > 200) return;
        String msg = "CHAT_MSG|" + playerName + "|" + playerColor + "|" + message;
        Server.state.addChatMessage(currentZone, msg);
        // Broadcast a TODOS en la zona (incluido el emisor para confirmación)
        Server.state.broadcastToZone(currentZone, msg, null);
    }

    // ── Desconexión ───────────────────────────────────────────────────────

    private void onDisconnect() {
        if (authenticated) {
            if (!"admin".equals(playerRol)) {
                Server.state.removePlayer(this);
                Server.state.broadcastToZone(currentZone, "PLAYER_LEFT|" + playerName, this);
                System.out.println("  Online: " + Server.state.getPlayerCount());
            }
            System.out.println("  Desconectado: " + playerName + " (" + playerRol + ")");
        }
        try { socket.close(); } catch (IOException ignored) {}
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private boolean[][] getCollisionMap() {
        switch (currentZone) {
            case "recepcion":   return COL_RECEPCION;
            case "conserjeria": return COL_CONSERJERIA;
            default:            return COL_ENTRADA;
        }
    }

    private boolean isValidZone(String z) {
        return z.equals("entrada") || z.equals("recepcion") || z.equals("conserjeria");
    }
}
