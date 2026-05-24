"""
═══════════════════════════════════════════════════════════════════════
  Escuela Interactiva 2D — Servidor Principal
  TFG Jorge González — DAM 2º

  Servidor Flask + Flask-SocketIO multihilo.
  Gestiona conexiones de clientes, sincronización de posiciones
  en tiempo real y consultas a la base de datos.
═══════════════════════════════════════════════════════════════════════
"""
import os
import sys
import json
from datetime import date, datetime
from decimal import Decimal

from datetime import timedelta
from flask import Flask, send_from_directory, jsonify, request, session
from flask_socketio import SocketIO, emit, join_room, leave_room
from flask_cors import CORS

# Añadir directorio raíz al path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from server.config import SERVER_HOST, SERVER_PORT, SECRET_KEY, ZONES
from server.db_manager import (
    DBManager, DEMO_ANUNCIOS, DEMO_HORARIO, DEMO_MENU, DEMO_NOTAS
)
from server.game_state import GameState

# ─── Inicialización ──────────────────────────────────────────────────
app = Flask(
    __name__,
    static_folder=os.path.join(os.path.dirname(os.path.dirname(__file__)), "client"),
    static_url_path=""
)
app.config["SECRET_KEY"] = SECRET_KEY
app.config["PERMANENT_SESSION_LIFETIME"] = timedelta(days=7)
app.config["SESSION_COOKIE_SAMESITE"] = "Lax"
app.config["SESSION_COOKIE_HTTPONLY"] = True
CORS(app, supports_credentials=True)

socketio = SocketIO(app, cors_allowed_origins="*", async_mode="threading")

# Estado global del juego (compartido entre hilos)
game_state = GameState()

# Gestor de base de datos
db = DBManager()


# ─── Serializador JSON personalizado ─────────────────────────────────
class CustomEncoder(json.JSONEncoder):
    def default(self, obj):
        if isinstance(obj, (date, datetime)):
            return obj.isoformat()
        if isinstance(obj, Decimal):
            return float(obj)
        return super().default(obj)


app.json_encoder = CustomEncoder


# ═══════════════════════════════════════════════════════════════════════
# RUTAS HTTP (Servir archivos del cliente y API REST)
# ═══════════════════════════════════════════════════════════════════════

# Helper para respuestas JSON seguras
def to_json_response(data, status_code=200):
    return app.response_class(
        response=json.dumps(data, cls=CustomEncoder),
        status=status_code,
        mimetype='application/json'
    )

def get_current_user():
    from flask import session
    return session.get("user")

def require_roles(allowed_roles):
    u = get_current_user()
    if not u:
        return None, (jsonify({"error": "No autenticado"}), 401)
    if u["rol"] not in allowed_roles:
        return None, (jsonify({"error": "Acceso denegado (Permisos insuficientes)"}), 403)
    return u, None


@app.route("/")
def index():
    """Sirve la página principal del cliente."""
    return send_from_directory(app.static_folder, "index.html")


@app.route("/api/zones")
def api_zones():
    """Devuelve la configuración de zonas del mapa."""
    return jsonify(ZONES)


@app.route("/api/status")
def api_status():
    """Estado del servidor."""
    return jsonify({
        "status": "online",
        "players": game_state.get_player_count(),
        "zones": game_state.get_all_zones_count(),
        "db_connected": db.connected,
    })


# ─── API DE AUTENTICACIÓN ────────────────────────────────────────────

@app.route("/api/auth/register", methods=["POST"])
def auth_register():
    data = request.get_json() or {}
    nombre = data.get("nombre", "").strip()
    password = data.get("password", "").strip()
    email = data.get("email", "").strip()
    rol = data.get("rol", "alumno").strip().lower()
    color = data.get("color", "#4A90D9").strip()

    if not nombre or not password:
        return jsonify({"error": "Nombre y contraseña obligatorios"}), 400

    if rol not in ["alumno", "profesor", "admin"]:
        return jsonify({"error": "Rol inválido"}), 400

    user = db.create_usuario(nombre, password, email or None, rol, color)
    if not user:
        return jsonify({"error": "El nombre de usuario ya está registrado"}), 400

    return to_json_response({"status": "ok", "user": user})


@app.route("/api/auth/login", methods=["POST"])
def auth_login():
    from flask import session
    data = request.get_json() or {}
    nombre = data.get("nombre", "").strip()
    password = data.get("password", "").strip()

    if not nombre or not password:
        return jsonify({"error": "Nombre y contraseña obligatorios"}), 400

    user = db.verify_usuario(nombre, password)
    if not user:
        return jsonify({"error": "Nombre de usuario o contraseña incorrectos"}), 401

    session["user"] = user
    return to_json_response({"status": "ok", "user": user})


@app.route("/api/auth/logout", methods=["POST"])
def auth_logout():
    from flask import session
    session.pop("user", None)
    return jsonify({"status": "ok"})


@app.route("/api/auth/me", methods=["GET"])
def auth_me():
    from flask import session
    u = session.get("user")
    if not u:
        return jsonify({"error": "No autenticado"}), 401
    
    # Refrescar los datos por si han cambiado en el panel
    refreshed = db.get_usuario(u["nombre"])
    if refreshed:
        session["user"] = refreshed
        return to_json_response({"status": "ok", "user": refreshed})
    return to_json_response({"status": "ok", "user": u})


# ─── API DE CALIFICACIONES (CRUD) ────────────────────────────────────

@app.route("/api/notas", methods=["GET"])
def get_notas_api():
    u = get_current_user()
    if not u:
        return jsonify({"error": "No autenticado"}), 401

    if u["rol"] in ["profesor", "admin"]:
        return to_json_response(db.get_all_notas())
    else:
        return to_json_response(db.get_notas(u["id"]))


@app.route("/api/notas", methods=["POST"])
def create_nota_api():
    u, error = require_roles(["profesor", "admin"])
    if error:
        return error

    data = request.get_json() or {}
    id_alumno = data.get("id_alumno")
    materia = data.get("materia", "").strip()
    calificacion = data.get("calificacion")
    tipo = data.get("tipo", "Examen").strip()
    comentario = data.get("comentario", "").strip()

    if not id_alumno or not materia or calificacion is None:
        return jsonify({"error": "id_alumno, materia y calificacion son obligatorios"}), 400

    res = db.create_nota(id_alumno, materia, calificacion, tipo, comentario)
    if not res:
        return jsonify({"error": "Error al registrar la nota"}), 500
    return to_json_response(res)


@app.route("/api/notas/<int:id_nota>", methods=["PUT"])
def update_nota_api(id_nota):
    u, error = require_roles(["profesor", "admin"])
    if error:
        return error

    data = request.get_json() or {}
    calificacion = data.get("calificacion")
    tipo = data.get("tipo", "Examen").strip()
    comentario = data.get("comentario", "").strip()

    if calificacion is None:
        return jsonify({"error": "calificacion requerida"}), 400

    res = db.update_nota(id_nota, calificacion, tipo, comentario)
    if not res:
        return jsonify({"error": "Calificación no encontrada"}), 404
    return to_json_response(res)


@app.route("/api/notas/<int:id_nota>", methods=["DELETE"])
def delete_nota_api(id_nota):
    u, error = require_roles(["profesor", "admin"])
    if error:
        return error

    success = db.delete_nota(id_nota)
    if not success:
        return jsonify({"error": "Calificación no encontrada"}), 404
    return jsonify({"status": "ok"})


# ─── API DE ANUNCIOS (CRUD) ──────────────────────────────────────────

@app.route("/api/anuncios", methods=["GET"])
def get_anuncios_api():
    return to_json_response(db.get_anuncios())


@app.route("/api/anuncios", methods=["POST"])
def create_anuncio_api():
    u, error = require_roles(["profesor", "admin"])
    if error:
        return error

    data = request.get_json() or {}
    titulo = data.get("titulo", "").strip()
    contenido = data.get("contenido", "").strip()
    importante = data.get("importante", False)

    if not titulo or not contenido:
        return jsonify({"error": "Título y contenido obligatorios"}), 400

    res = db.create_anuncio(titulo, contenido, u["nombre"], importante)
    return to_json_response(res)


@app.route("/api/anuncios/<int:id_anuncio>", methods=["PUT"])
def update_anuncio_api(id_anuncio):
    u, error = require_roles(["profesor", "admin"])
    if error:
        return error

    data = request.get_json() or {}
    titulo = data.get("titulo", "").strip()
    contenido = data.get("contenido", "").strip()
    importante = data.get("importante", False)

    if not titulo or not contenido:
        return jsonify({"error": "Título y contenido obligatorios"}), 400

    res = db.update_anuncio(id_anuncio, titulo, contenido, importante)
    if not res:
        return jsonify({"error": "Anuncio no encontrado"}), 404
    return to_json_response(res)


@app.route("/api/anuncios/<int:id_anuncio>", methods=["DELETE"])
def delete_anuncio_api(id_anuncio):
    u, error = require_roles(["profesor", "admin"])
    if error:
        return error

    success = db.delete_anuncio(id_anuncio)
    if not success:
        return jsonify({"error": "Anuncio no encontrado"}), 404
    return jsonify({"status": "ok"})


# ─── API DE USUARIOS (ADMIN CRUD) ─────────────────────────────────────

@app.route("/api/usuarios", methods=["GET"])
def get_usuarios_api():
    u, error = require_roles(["profesor", "admin"])
    if error:
        return error
    return to_json_response(db.get_usuarios())


@app.route("/api/usuarios/<int:user_id>", methods=["PUT"])
def update_usuario_rol_api(user_id):
    u, error = require_roles(["admin"])
    if error:
        return error

    data = request.get_json() or {}
    nuevo_rol = data.get("rol", "").strip().lower()

    if nuevo_rol not in ["alumno", "profesor", "admin"]:
        return jsonify({"error": "Rol inválido"}), 400

    success = db.update_usuario_rol(user_id, nuevo_rol)
    if not success:
        return jsonify({"error": "Usuario no encontrado"}), 404
    return jsonify({"status": "ok"})


@app.route("/api/usuarios/<int:user_id>", methods=["DELETE"])
def delete_usuario_api(user_id):
    u, error = require_roles(["admin"])
    if error:
        return error

    if user_id == u["id"]:
        return jsonify({"error": "No puedes eliminarte a ti mismo"}), 400

    success = db.delete_usuario(user_id)
    if not success:
        return jsonify({"error": "Usuario no encontrado"}), 404
    return jsonify({"status": "ok"})


# ─── API DE HORARIOS (CRUD) ──────────────────────────────────────────

@app.route("/api/horarios", methods=["GET"])
def get_horarios_api():
    u = get_current_user()
    id_alumno = request.args.get("id_alumno")

    if not id_alumno:
        if u:
            id_alumno = u["id"]
        else:
            id_alumno = 1

    if u and u["rol"] == "alumno" and int(id_alumno) != u["id"]:
        return jsonify({"error": "No autorizado"}), 403

    return to_json_response(db.get_horario(id_alumno))


@app.route("/api/horarios/todos", methods=["GET"])
def get_all_horarios_api():
    u, error = require_roles(["profesor", "admin"])
    if error:
        return error
    return to_json_response(db.get_all_horarios())


@app.route("/api/horarios", methods=["POST"])
def create_horario_api():
    u, error = require_roles(["admin"])
    if error:
        return error

    data = request.get_json() or {}
    id_alumno = data.get("id_alumno")
    dia = data.get("dia", "").strip()
    hora_inicio = data.get("hora_inicio", "").strip()
    hora_fin = data.get("hora_fin", "").strip()
    asignatura = data.get("asignatura", "").strip()
    aula = data.get("aula", "").strip()

    if not id_alumno or not dia or not hora_inicio or not hora_fin or not asignatura:
        return jsonify({"error": "Campos incompletos"}), 400

    res = db.create_horario(id_alumno, dia, hora_inicio, hora_fin, asignatura, aula)
    return to_json_response(res)


@app.route("/api/horarios/<int:id_horario>", methods=["DELETE"])
def delete_horario_api(id_horario):
    u, error = require_roles(["admin"])
    if error:
        return error

    success = db.delete_horario(id_horario)
    if not success:
        return jsonify({"error": "Horario no encontrado"}), 404
    return jsonify({"status": "ok"})


# ─── API DE MENÚ DEL COMEDOR (CRUD) ──────────────────────────────────

@app.route("/api/menu_comedor", methods=["GET"])
def get_menu_comedor_api():
    return to_json_response(db.get_menu_comedor())


@app.route("/api/menu_comedor", methods=["POST"])
def create_or_update_menu_api():
    u, error = require_roles(["admin"])
    if error:
        return error

    data = request.get_json() or {}
    fecha = data.get("fecha", "").strip()
    primer_plato = data.get("primer_plato", "").strip()
    segundo_plato = data.get("segundo_plato", "").strip()
    postre = data.get("postre", "").strip()
    alergenos = data.get("alergenos", "").strip()

    if not fecha or not primer_plato or not segundo_plato or not postre:
        return jsonify({"error": "Campos obligatorios incompletos"}), 400

    res = db.create_or_update_menu(fecha, primer_plato, segundo_plato, postre, alergenos or None)
    return to_json_response(res)


@app.route("/api/menu_comedor/<int:id_menu>", methods=["DELETE"])
def delete_menu_api(id_menu):
    u, error = require_roles(["admin"])
    if error:
        return error

    success = db.delete_menu(id_menu)
    if not success:
        return jsonify({"error": "Menú no encontrado"}), 404
    return jsonify({"status": "ok"})


# ═══════════════════════════════════════════════════════════════════════
# EVENTOS WEBSOCKET (Comunicación en tiempo real)
# ═══════════════════════════════════════════════════════════════════════

@socketio.on("connect")
def handle_connect():
    """Un cliente se ha conectado."""
    print(f"  🔌 Cliente conectado: {request.sid}")


@socketio.on("disconnect")
def handle_disconnect():
    """Un cliente se ha desconectado."""
    player = game_state.get_player(request.sid)
    if player:
        zone = player["zone"]
        name = player["name"]
        game_state.remove_player(request.sid)
        leave_room(zone)
        # Notificar a los demás en la zona
        emit("player_left", {"name": name}, room=zone)
        print(f"  ❌ Desconectado: {name} ({request.sid})")
        print(f"  👥 Jugadores online: {game_state.get_player_count()}")


@socketio.on("join")
def handle_join(data):
    """
    Un jugador se une al juego.
    data: { name: str, color?: str, rol?: str, user_id?: int }
    """
    name = data.get("name", "Anónimo").strip()[:20]
    color = data.get("color", "#4A90D9")
    rol = data.get("rol", "alumno")
    user_id = data.get("user_id")

    # Registrar jugador en el estado global
    game_state.add_player(request.sid, name, color, rol, user_id)
    player = game_state.get_player(request.sid)

    # Unirse a la sala de la zona
    join_room(player["zone"])

    # Enviar configuración de zonas y posición inicial
    emit("joined", {
        "name": name,
        "x": player["x"],
        "y": player["y"],
        "zone": player["zone"],
        "color": color,
        "rol": rol,
        "user_id": user_id,
        "zones": ZONES,
    })

    # Notificar a los demás en la zona
    emit("player_joined", {
        "name": name,
        "color": color,
        "rol": rol,
    }, room=player["zone"], include_self=False)

    print(f"  ✅ Conectado: {name} (rol: {rol}, color: {color})")
    print(f"  👥 Jugadores online: {game_state.get_player_count()}")


@socketio.on("move")
def handle_move(data):
    """
    Un jugador se mueve.
    data: { x: float, y: float, direction: str }
    """
    sid = request.sid
    x = data.get("x", 0)
    y = data.get("y", 0)
    direction = data.get("direction", "down")

    game_state.update_position(sid, x, y, direction)
    player = game_state.get_player(sid)
    if player:
        # Broadcast de posición a todos en la misma zona
        emit("player_moved", {
            "name": player["name"],
            "x": x,
            "y": y,
            "direction": direction,
            "color": player["color"],
            "rol": player["rol"],
        }, room=player["zone"], include_self=False)


@socketio.on("change_zone")
def handle_change_zone(data):
    """
    Un jugador cambia de zona (transición).
    data: { target_zone: str }
    """
    sid = request.sid
    target_zone = data.get("target_zone")
    player = game_state.get_player(sid)

    if not player or target_zone not in ZONES:
        return

    old_zone = player["zone"]

    # Salir de la zona anterior
    leave_room(old_zone)
    emit("player_left", {"name": player["name"]}, room=old_zone)

    # Cambiar zona en el estado
    game_state.change_zone(sid, target_zone)
    updated = game_state.get_player(sid)

    # Unirse a la nueva zona
    join_room(target_zone)

    # Enviar confirmación con los jugadores ya en la zona
    others = game_state.get_players_in_zone(target_zone, exclude_sid=sid)
    emit("zone_changed", {
        "zone": target_zone,
        "x": updated["x"],
        "y": updated["y"],
        "zone_config": ZONES[target_zone],
        "players": others,
    })

    # Notificar a los demás en la nueva zona
    emit("player_joined", {
        "name": player["name"],
        "color": player["color"],
        "rol": player["rol"],
    }, room=target_zone, include_self=False)

    print(f"  🚪 {player['name']}: {old_zone} → {target_zone}")


@socketio.on("interact")
def handle_interact(data):
    """
    Un jugador interactúa con un elemento del mapa.
    data: { type: str }  →  tablon | horario | menu_comedor | notas
    """
    interaction_type = data.get("type")
    result = None

    player = game_state.get_player(request.sid)
    user_id = player["user_id"] if player and player.get("user_id") is not None else 1

    if interaction_type == "tablon":
        result = db.get_anuncios()

    elif interaction_type == "horario":
        result = db.get_horario(user_id)

    elif interaction_type == "menu_comedor":
        result = db.get_menu_comedor()

    elif interaction_type == "notas":
        result = db.get_notas(user_id)

    if result is not None:
        # Serializar fechas y decimales
        serialized = json.loads(json.dumps(result, cls=CustomEncoder))
        emit("interaction_data", {
            "type": interaction_type,
            "data": serialized,
        })


@socketio.on("chat_message")
def handle_chat(data):
    """
    Un jugador envía un mensaje de chat.
    data: { message: str }
    """
    player = game_state.get_player(request.sid)
    if not player:
        return

    message = data.get("message", "").strip()[:200]
    if not message:
        return

    zone = player["zone"]
    name = player["name"]

    game_state.add_chat_message(zone, name, message)

    # Broadcast a la zona
    emit("chat_broadcast", {
        "name": name,
        "message": message,
        "color": player["color"],
    }, room=zone)


@socketio.on("request_players")
def handle_request_players():
    """Un cliente solicita la lista de jugadores en su zona."""
    player = game_state.get_player(request.sid)
    if player:
        others = game_state.get_players_in_zone(player["zone"], exclude_sid=request.sid)
        emit("players_update", {"players": others})


# ═══════════════════════════════════════════════════════════════════════
# INICIO DEL SERVIDOR
# ═══════════════════════════════════════════════════════════════════════

if __name__ == "__main__":
    print()
    print("═" * 60)
    print("  🏫 Escuela Interactiva 2D — Servidor")
    print("  📚 TFG Jorge González — DAM 2º")
    print("═" * 60)
    print()
    print(f"  🌐 Servidor: http://{SERVER_HOST}:{SERVER_PORT}")
    print(f"  🎮 Cliente:  http://localhost:{SERVER_PORT}")
    print()

    # Intentar conectar a la base de datos
    db.connect()

    print()
    print("─" * 60)
    print("  Esperando conexiones de jugadores...")
    print("─" * 60)
    print()

    socketio.run(
        app,
        host=SERVER_HOST,
        port=SERVER_PORT,
        debug=True,
        allow_unsafe_werkzeug=True,
    )

