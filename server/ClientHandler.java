import java.net.*;
import java.io.*;

public class ClientHandler implements Runnable {
    private static final int MAP_WIDTH  = 21;
    private static final int MAP_HEIGHT = 15;

    // Scene 1: rows 0 to MAP_HEIGHT/2 - 1 blocked (building facade)
    private static final boolean[][] COLLISION_MAP = buildCollisionMap();

    private static boolean[][] buildCollisionMap() {
        boolean[][] map = new boolean[MAP_WIDTH][MAP_HEIGHT];
        for (int x = 0; x < MAP_WIDTH; x++)
            for (int y = 0; y < MAP_HEIGHT / 2; y++)
                map[x][y] = true;
        return map;
    }

    // Scene 2: vertical walls in the bottom 3 rows
    // Left wall : border between col 5 and col 6
    // Right wall: border between col 14 and col 15 (mirror)
    private static final int S2_WALL_TOP = MAP_HEIGHT - 3;  // row 12

    private final Socket socket;
    private PrintWriter out;
    private int x     = MAP_WIDTH / 2;
    private int y     = MAP_HEIGHT - 2;
    private int scene = 1;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            Server.playerPositions.put(this, new int[]{x, y});
            out.println("POSITION|" + x + "|" + y);

            String message;
            while ((message = in.readLine()) != null) {
                handleMessage(message);
            }
        } catch (IOException e) {
            System.out.println("Cliente desconectado");
        } finally {
            Server.playerPositions.remove(this);
        }
    }

    private void handleMessage(String message) {
        String[] parts = message.split("\\|");
        switch (parts[0]) {
            case "MOVE":     handleMove(parts[1]);                          break;
            case "TELEPORT": handleTeleport(parts[1], parts[2], parts[3]); break;
        }
    }

    private void handleTeleport(String sx, String sy, String ss) {
        int nx = Integer.parseInt(sx);
        int ny = Integer.parseInt(sy);
        int ns = Integer.parseInt(ss);
        if (nx >= 0 && nx < MAP_WIDTH && ny >= 0 && ny < MAP_HEIGHT) {
            x = nx; y = ny; scene = ns;
            Server.playerPositions.put(this, new int[]{x, y});
        }
    }

    private void handleMove(String direction) {
        int nx = x, ny = y;
        switch (direction) {
            case "UP":    ny--; break;
            case "DOWN":  ny++; break;
            case "LEFT":  nx--; break;
            case "RIGHT": nx++; break;
            default: return;
        }

        if (nx < 0 || nx >= MAP_WIDTH || ny < 0 || ny >= MAP_HEIGHT) return;

        if (scene == 1 && COLLISION_MAP[nx][ny]) return;
        if (scene == 2 && isScene2WallCollision(x, y, nx)) return;

        for (int[] pos : Server.playerPositions.values()) {
            if (pos[0] == nx && pos[1] == ny) return;
        }

        x = nx; y = ny;
        Server.playerPositions.put(this, new int[]{x, y});
        out.println("POSITION|" + x + "|" + y);
    }

    private boolean isScene2WallCollision(int ox, int oy, int nx) {
        if (oy < S2_WALL_TOP) return false;
        // Left wall: col 5 <-> col 6
        if ((ox == 5 && nx == 6) || (ox == 6 && nx == 5)) return true;
        // Right wall: col 14 <-> col 15
        if ((ox == 14 && nx == 15) || (ox == 15 && nx == 14)) return true;
        return false;
    }
}
