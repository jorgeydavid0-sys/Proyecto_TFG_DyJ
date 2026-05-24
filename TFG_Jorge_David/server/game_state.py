"""
Estado del Juego Compartido — Escuela Interactiva 2D
Mantiene el estado de todos los jugadores conectados (thread-safe).
"""
import threading
import time
from server.config import ZONES


class GameState:
    """
    Estado global del juego, compartido entre todos los hilos de clientes.
    Utiliza un Lock para garantizar acceso seguro desde múltiples hilos.
    """

    def __init__(self):
        self._lock = threading.Lock()
        self._players = {}  # sid -> { name, x, y, zone, color, direction, ... }
        self._chat_history = {}  # zone -> [messages]

    def add_player(self, sid, name, color="#4A90D9", rol="alumno", user_id=None):
        """Añade un jugador al estado cuando se conecta."""
        spawn = ZONES["entrada"]["spawn"]
        with self._lock:
            self._players[sid] = {
                "name": name,
                "x": spawn["x"],
                "y": spawn["y"],
                "zone": "entrada",
                "color": color,
                "direction": "down",
                "rol": rol,
                "user_id": user_id,
                "last_update": time.time(),
            }

    def remove_player(self, sid):
        """Elimina un jugador cuando se desconecta."""
        with self._lock:
            self._players.pop(sid, None)

    def update_position(self, sid, x, y, direction="down"):
        """Actualiza la posición de un jugador."""
        with self._lock:
            if sid in self._players:
                self._players[sid]["x"] = x
                self._players[sid]["y"] = y
                self._players[sid]["direction"] = direction
                self._players[sid]["last_update"] = time.time()

    def change_zone(self, sid, new_zone):
        """Cambia la zona de un jugador y le pone en el spawn de la nueva zona."""
        with self._lock:
            if sid in self._players and new_zone in ZONES:
                spawn = ZONES[new_zone]["spawn"]
                self._players[sid]["zone"] = new_zone
                self._players[sid]["x"] = spawn["x"]
                self._players[sid]["y"] = spawn["y"]

    def get_players_in_zone(self, zone, exclude_sid=None):
        """Retorna todos los jugadores en una zona (excepto uno opcional)."""
        with self._lock:
            return [
                {
                    "name": p["name"],
                    "x": p["x"],
                    "y": p["y"],
                    "color": p["color"],
                    "direction": p["direction"],
                }
                for sid, p in self._players.items()
                if p["zone"] == zone and sid != exclude_sid
            ]

    def get_player(self, sid):
        """Obtiene los datos de un jugador."""
        with self._lock:
            return self._players.get(sid, None)

    def get_player_count(self):
        """Cuenta total de jugadores conectados."""
        with self._lock:
            return len(self._players)

    def get_all_zones_count(self):
        """Cuenta jugadores por zona."""
        with self._lock:
            counts = {}
            for p in self._players.values():
                z = p["zone"]
                counts[z] = counts.get(z, 0) + 1
            return counts

    def add_chat_message(self, zone, name, message):
        """Añade un mensaje al historial de chat de una zona."""
        with self._lock:
            if zone not in self._chat_history:
                self._chat_history[zone] = []
            self._chat_history[zone].append({
                "name": name,
                "message": message,
                "time": time.strftime("%H:%M"),
            })
            # Limitar historial a 100 mensajes por zona
            if len(self._chat_history[zone]) > 100:
                self._chat_history[zone] = self._chat_history[zone][-100:]

    def get_chat_history(self, zone, limit=20):
        """Obtiene los últimos mensajes de chat de una zona."""
        with self._lock:
            return self._chat_history.get(zone, [])[-limit:]
