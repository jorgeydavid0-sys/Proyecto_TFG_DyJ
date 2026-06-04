import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameState {

    public static class PlayerInfo {
        public String name;
        public String color;
        public String rol;
        public int    userId;
        public int    x, y;
        public String zone;
        public String direction;

        public PlayerInfo(String name, String color, String rol, int userId, int x, int y, String zone) {
            this.name = name; this.color = color; this.rol = rol; this.userId = userId;
            this.x = x; this.y = y; this.zone = zone; this.direction = "down";
        }
    }

    private final ConcurrentHashMap<ClientHandler, PlayerInfo> players = new ConcurrentHashMap<>();

    // Chat history per zone: last 100 messages each
    private final Map<String, List<String>> chatHistory = new ConcurrentHashMap<>();

    // ── Ciclo de vida de jugadores ───────────────────────────────────────

    public void addPlayer(ClientHandler handler, String name, String color, String rol,
                          int userId, int x, int y, String zone) {
        players.put(handler, new PlayerInfo(name, color, rol, userId, x, y, zone));
    }

    public void removePlayer(ClientHandler handler) {
        players.remove(handler);
    }

    public PlayerInfo getPlayer(ClientHandler handler) {
        return players.get(handler);
    }

    public int getPlayerCount() { return players.size(); }

    // ── Posición y zona ─────────────────────────────────────────────────

    public synchronized void updatePosition(ClientHandler handler, int x, int y, String direction) {
        PlayerInfo p = players.get(handler);
        if (p != null) { p.x = x; p.y = y; p.direction = direction; }
    }

    public synchronized void changeZone(ClientHandler handler, String newZone, int newX, int newY) {
        PlayerInfo p = players.get(handler);
        if (p != null) { p.zone = newZone; p.x = newX; p.y = newY; p.direction = "down"; }
    }

    // ── Broadcast a una zona ─────────────────────────────────────────────

    public void broadcastToZone(String zone, String message, ClientHandler exclude) {
        for (Map.Entry<ClientHandler, PlayerInfo> e : players.entrySet()) {
            if (e.getValue().zone.equals(zone) && e.getKey() != exclude)
                e.getKey().sendMessage(message);
        }
    }

    public void broadcastToAll(String message, ClientHandler exclude) {
        for (Map.Entry<ClientHandler, PlayerInfo> e : players.entrySet()) {
            if (e.getKey() != exclude)
                e.getKey().sendMessage(message);
        }
    }

    public void sendToPlayer(String name, String message) {
        for (Map.Entry<ClientHandler, PlayerInfo> e : players.entrySet()) {
            if (e.getValue().name.equals(name)) {
                e.getKey().sendMessage(message);
                return;
            }
        }
    }

    // ── Lista de jugadores en zona ────────────────────────────────────────

    public List<PlayerInfo> getPlayersInZone(String zone, ClientHandler exclude) {
        List<PlayerInfo> list = new ArrayList<>();
        for (Map.Entry<ClientHandler, PlayerInfo> e : players.entrySet()) {
            if (e.getValue().zone.equals(zone) && e.getKey() != exclude)
                list.add(e.getValue());
        }
        return list;
    }

    // ── Chat ─────────────────────────────────────────────────────────────

    public synchronized void addChatMessage(String zone, String formatted) {
        List<String> hist = chatHistory.computeIfAbsent(zone, z -> new ArrayList<>());
        hist.add(formatted);
        if (hist.size() > 100) hist.remove(0);
    }
}
