"""
Configuración del servidor - Escuela Interactiva 2D
TFG Jorge González - DAM 2º
"""
import os
from dotenv import load_dotenv

load_dotenv()

# ─── Configuración del Servidor ───────────────────────────────────────
SERVER_HOST = os.getenv("SERVER_HOST", "0.0.0.0")
SERVER_PORT = int(os.getenv("SERVER_PORT", 5000))
SECRET_KEY = os.getenv("SECRET_KEY", "escuela-interactiva-2d-secret-key-tfg")

# ─── Configuración de PostgreSQL ──────────────────────────────────────
DB_CONFIG = {
    "host": os.getenv("DB_HOST", "localhost"),
    "port": int(os.getenv("DB_PORT", 5432)),
    "database": os.getenv("DB_NAME", "escuela_interactiva"),
    "user": os.getenv("DB_USER", "postgres"),
    "password": os.getenv("DB_PASSWORD", "postgres"),
}

# ─── Configuración del Mapa ──────────────────────────────────────────
# Tamaño de cada zona en píxeles
ZONE_WIDTH = 960
ZONE_HEIGHT = 640

# Zonas del mapa
ZONES = {
    "entrada": {
        "name": "Entrada Exterior",
        "width": ZONE_WIDTH,
        "height": ZONE_HEIGHT,
        "spawn": {"x": 480, "y": 550},
        "transitions": [
            {
                "target_zone": "recepcion",
                "rect": {"x": 400, "y": 0, "w": 160, "h": 40},
                "spawn": {"x": 480, "y": 560},
            }
        ],
        "interactions": [],
    },
    "recepcion": {
        "name": "Recepción",
        "width": ZONE_WIDTH,
        "height": ZONE_HEIGHT,
        "spawn": {"x": 480, "y": 550},
        "transitions": [
            {
                "target_zone": "entrada",
                "rect": {"x": 400, "y": 600, "w": 160, "h": 40},
                "spawn": {"x": 480, "y": 60},
            },
            {
                "target_zone": "conserjeria",
                "rect": {"x": 0, "y": 250, "w": 40, "h": 140},
                "spawn": {"x": 880, "y": 320},
            },
        ],
        "interactions": [
            {
                "id": "tablon",
                "type": "tablon",
                "label": "Tablón de Anuncios",
                "rect": {"x": 200, "y": 60, "w": 120, "h": 80},
            },
            {
                "id": "horario",
                "type": "horario",
                "label": "Horario / Calendario",
                "rect": {"x": 640, "y": 60, "w": 120, "h": 80},
            },
        ],
    },
    "conserjeria": {
        "name": "Conserjería",
        "width": ZONE_WIDTH,
        "height": ZONE_HEIGHT,
        "spawn": {"x": 880, "y": 320},
        "transitions": [
            {
                "target_zone": "recepcion",
                "rect": {"x": 920, "y": 250, "w": 40, "h": 140},
                "spawn": {"x": 60, "y": 320},
            },
        ],
        "interactions": [
            {
                "id": "menu_comedor",
                "type": "menu_comedor",
                "label": "Menú del Comedor",
                "rect": {"x": 200, "y": 60, "w": 120, "h": 80},
            },
            {
                "id": "notas",
                "type": "notas",
                "label": "Notas / Trabajos",
                "rect": {"x": 640, "y": 60, "w": 120, "h": 80},
            },
        ],
    },
}

# ─── Configuración del Jugador ────────────────────────────────────────
PLAYER_SPEED = 4
PLAYER_SIZE = 32
TICK_RATE = 30  # Actualizaciones por segundo
