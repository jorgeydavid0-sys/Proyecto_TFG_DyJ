"""
Gestor de Base de Datos — Escuela Interactiva 2D
Maneja todas las consultas a PostgreSQL con fallback dinámico en memoria.
"""
import psycopg2
import psycopg2.extras
from contextlib import contextmanager
from datetime import date, datetime
from decimal import Decimal
from werkzeug.security import generate_password_hash, check_password_hash
from server.config import DB_CONFIG


class DBManager:
    """Gestiona la conexión y consultas a PostgreSQL con fallback en memoria."""

    def __init__(self):
        self._pool = None
        self.connected = False

        # ── Estado de Base de Datos en Memoria (Fallback) ────────────────
        self.mock_usuarios = [
            {"id": 1, "nombre": "Jorge", "email": "jorge@escuela.com", "password": generate_password_hash("1234"), "color": "#4A90D9", "rol": "alumno", "nivel": 1, "xp": 0},
            {"id": 2, "nombre": "María", "email": "maria@escuela.com", "password": generate_password_hash("1234"), "color": "#E74C3C", "rol": "profesor", "nivel": 2, "xp": 150},
            {"id": 3, "nombre": "Carlos", "email": "carlos@escuela.com", "password": generate_password_hash("1234"), "color": "#2ECC71", "rol": "admin", "nivel": 5, "xp": 450},
            {"id": 4, "nombre": "Ana", "email": "ana@escuela.com", "password": generate_password_hash("1234"), "color": "#F39C12", "rol": "alumno", "nivel": 1, "xp": 20},
        ]

        self.mock_notas = [
            {"id": 1, "id_alumno": 1, "materia": "Programación", "calificacion": 8.5, "tipo": "Examen", "comentario": "Buen trabajo en POO", "fecha": "2026-05-15"},
            {"id": 2, "id_alumno": 1, "materia": "Base de Datos", "calificacion": 9.0, "tipo": "Examen", "comentario": "Excelente diseño de esquema", "fecha": "2026-05-10"},
            {"id": 3, "id_alumno": 1, "materia": "Sistemas Informáticos", "calificacion": 7.25, "tipo": "Práctica", "comentario": "Mejorar documentación", "fecha": "2026-05-05"},
            {"id": 4, "id_alumno": 1, "materia": "Entornos de Desarrollo", "calificacion": 8.75, "tipo": "Proyecto", "comentario": "Proyecto TFG sobresaliente", "fecha": "2026-05-01"},
            {"id": 5, "id_alumno": 1, "materia": "Desarrollo Web", "calificacion": 9.5, "tipo": "Examen", "comentario": "Dominio de frontend y backend", "fecha": "2026-04-20"},
            {"id": 6, "id_alumno": 4, "materia": "Programación", "calificacion": 7.0, "tipo": "Examen", "comentario": "Revisar estructuras de datos", "fecha": "2026-05-15"},
            {"id": 7, "id_alumno": 4, "materia": "Base de Datos", "calificacion": 8.25, "tipo": "Examen", "comentario": "Buenas consultas SQL", "fecha": "2026-05-10"},
        ]

        self.mock_anuncios = [
            {"id": 1, "titulo": "📢 Bienvenidos al nuevo curso", "contenido": "Bienvenidos al curso 2025-2026 de DAM. Recordad revisar los horarios actualizados en recepción.", "autor": "Dirección", "fecha": "2026-05-20T08:00:00", "importante": True},
            {"id": 2, "titulo": "🏆 Concurso de Programación", "contenido": "Se abre la inscripción para el concurso anual de programación. Fecha límite: 15 de junio.", "autor": "Dpto. Informática", "fecha": "2026-05-18T12:30:00", "importante": False},
            {"id": 3, "titulo": "📅 Entrega TFG", "contenido": "La fecha límite para la entrega del Trabajo Final de Grado es el 30 de junio.", "autor": "Secretaría", "fecha": "2026-05-17T09:15:00", "importante": True},
            {"id": 4, "titulo": "🎮 Torneo de eSports", "contenido": "El club de gaming organiza un torneo de League of Legends. Inscripciones abiertas.", "autor": "Club Gaming", "fecha": "2026-05-15T18:00:00", "importante": False},
        ]

        self.mock_horario = [
            {"id": 1, "id_alumno": 1, "dia": "Lunes", "hora_inicio": "08:30", "hora_fin": "10:15", "asignatura": "Programación", "aula": "Aula 201"},
            {"id": 2, "id_alumno": 1, "dia": "Lunes", "hora_inicio": "10:30", "hora_fin": "12:15", "asignatura": "Base de Datos", "aula": "Aula 203"},
            {"id": 3, "id_alumno": 1, "dia": "Lunes", "hora_inicio": "12:30", "hora_fin": "14:15", "asignatura": "Entornos de Desarrollo", "aula": "Lab 1"},
            {"id": 4, "id_alumno": 1, "dia": "Martes", "hora_inicio": "08:30", "hora_fin": "10:15", "asignatura": "Desarrollo Web", "aula": "Lab 2"},
            {"id": 5, "id_alumno": 1, "dia": "Martes", "hora_inicio": "10:30", "hora_fin": "12:15", "asignatura": "Sistemas Informáticos", "aula": "Aula 105"},
            {"id": 6, "id_alumno": 1, "dia": "Miércoles", "hora_inicio": "08:30", "hora_fin": "10:15", "asignatura": "Programación", "aula": "Aula 201"},
            {"id": 7, "id_alumno": 1, "dia": "Miércoles", "hora_inicio": "10:30", "hora_fin": "12:15", "asignatura": "Base de Datos", "aula": "Aula 203"},
            {"id": 8, "id_alumno": 1, "dia": "Jueves", "hora_inicio": "08:30", "hora_fin": "10:15", "asignatura": "Desarrollo Web", "aula": "Lab 2"},
            {"id": 9, "id_alumno": 1, "dia": "Jueves", "hora_inicio": "10:30", "hora_fin": "12:15", "asignatura": "Entornos de Desarrollo", "aula": "Lab 1"},
            {"id": 10, "id_alumno": 1, "dia": "Viernes", "hora_inicio": "08:30", "hora_fin": "10:15", "asignatura": "Sistemas Informáticos", "aula": "Aula 105"},
            {"id": 11, "id_alumno": 1, "dia": "Viernes", "hora_inicio": "10:30", "hora_fin": "12:15", "asignatura": "Programación", "aula": "Aula 201"},
        ]

        self.mock_menu = [
            {"id": 1, "fecha": "2026-05-20", "primer_plato": "Crema de calabaza", "segundo_plato": "Pollo al horno con patatas", "postre": "Fruta de temporada", "alergenos": "Lácteos"},
            {"id": 2, "fecha": "2026-05-21", "primer_plato": "Ensalada mixta", "segundo_plato": "Merluza a la plancha", "postre": "Yogur natural", "alergenos": "Pescado, Lácteos"},
            {"id": 3, "fecha": "2026-05-22", "primer_plato": "Lentejas estofadas", "segundo_plato": "Tortilla española", "postre": "Helado", "alergenos": "Huevo, Lácteos"},
            {"id": 4, "fecha": "2026-05-23", "primer_plato": "Sopa de pollo", "segundo_plato": "Lomo a la plancha", "postre": "Natillas", "alergenos": "Lácteos"},
        ]

    def connect(self):
        """Intenta conectar a la base de datos PostgreSQL."""
        try:
            conn = psycopg2.connect(**DB_CONFIG)
            conn.autocommit = True
            conn.close()
            self.connected = True
            print(f"  ✅ Conectado a PostgreSQL ({DB_CONFIG['database']})")
        except Exception as e:
            self.connected = False
            print(f"  ⚠️  No se pudo conectar a PostgreSQL: {e}")
            print(f"  ℹ️  El servidor funcionará con datos de demostración.")

    @contextmanager
    def get_connection(self):
        """Context manager para obtener una conexión."""
        conn = psycopg2.connect(**DB_CONFIG)
        try:
            yield conn
            conn.commit()
        except Exception:
            conn.rollback()
            raise
        finally:
            conn.close()

    def query(self, sql, params=None):
        """Ejecuta una consulta SELECT y retorna los resultados como lista de dicts."""
        if not self.connected:
            return []
        try:
            with self.get_connection() as conn:
                with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
                    cur.execute(sql, params or ())
                    return [dict(row) for row in cur.fetchall()]
        except Exception as e:
            print(f"  ❌ Error en query: {e}")
            return []

    def execute(self, sql, params=None):
        """Ejecuta una operación INSERT/UPDATE/DELETE."""
        if not self.connected:
            return None
        try:
            with self.get_connection() as conn:
                with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
                    cur.execute(sql, params or ())
                    try:
                        return dict(cur.fetchone())
                    except Exception:
                        return None
        except Exception as e:
            print(f"  ❌ Error en execute: {e}")
            return None

    # ─── MÓDULO: AUTENTICACIÓN Y ROLES ───────────────────────────────

    def get_usuario(self, nombre):
        """Busca un usuario por nombre."""
        if self.connected:
            res = self.query(
                "SELECT id, nombre, email, color, rol, nivel, xp FROM usuarios WHERE nombre = %s",
                (nombre,)
            )
            return res[0] if res else None
        else:
            for u in self.mock_usuarios:
                if u["nombre"].lower() == nombre.lower():
                    # Retornar copia sin contraseña
                    u_copy = u.copy()
                    u_copy.pop("password", None)
                    return u_copy
            return None

    def get_usuarios(self):
        """Obtiene la lista completa de usuarios."""
        if self.connected:
            return self.query("SELECT id, nombre, email, color, rol, nivel, xp FROM usuarios ORDER BY id")
        else:
            return [{k: v for k, v in u.items() if k != "password"} for u in self.mock_usuarios]

    def create_usuario(self, nombre, password, email=None, rol="alumno", color="#4A90D9"):
        """Registra un nuevo usuario en la base de datos o en memoria."""
        pw_hash = generate_password_hash(password)
        if self.connected:
            res = self.execute(
                "INSERT INTO usuarios (nombre, email, password, color, rol) "
                "VALUES (%s, %s, %s, %s, %s) RETURNING id, nombre, email, color, rol, nivel, xp",
                (nombre, email, pw_hash, color, rol)
            )
            return res
        else:
            # Comprobar nombre único
            for u in self.mock_usuarios:
                if u["nombre"].lower() == nombre.lower():
                    return None
            new_id = max([u["id"] for u in self.mock_usuarios]) + 1 if self.mock_usuarios else 1
            new_user = {
                "id": new_id,
                "nombre": nombre,
                "email": email or f"{nombre.lower()}@escuela.com",
                "password": pw_hash,
                "color": color,
                "rol": rol,
                "nivel": 1,
                "xp": 0
            }
            self.mock_usuarios.append(new_user)
            # Copia sin la contraseña
            res = new_user.copy()
            res.pop("password", None)
            return res

    def verify_usuario(self, nombre, password):
        """Verifica las credenciales y devuelve el perfil del usuario."""
        if self.connected:
            res = self.query(
                "SELECT id, nombre, email, password, color, rol, nivel, xp FROM usuarios WHERE nombre = %s",
                (nombre,)
            )
            if res and check_password_hash(res[0]["password"], password):
                u = res[0]
                u.pop("password", None)
                return u
            return None
        else:
            for u in self.mock_usuarios:
                if u["nombre"].lower() == nombre.lower() and check_password_hash(u["password"], password):
                    u_copy = u.copy()
                    u_copy.pop("password", None)
                    return u_copy
            return None

    def update_usuario_rol(self, user_id, nuevo_rol):
        """Actualiza el rol de un usuario."""
        if self.connected:
            self.execute("UPDATE usuarios SET rol = %s WHERE id = %s", (nuevo_rol, user_id))
            return True
        else:
            for u in self.mock_usuarios:
                if u["id"] == int(user_id):
                    u["rol"] = nuevo_rol
                    return True
            return False

    def delete_usuario(self, user_id):
        """Elimina un usuario."""
        if self.connected:
            self.execute("DELETE FROM usuarios WHERE id = %s", (user_id,))
            return True
        else:
            initial_len = len(self.mock_usuarios)
            self.mock_usuarios = [u for u in self.mock_usuarios if u["id"] != int(user_id)]
            return len(self.mock_usuarios) < initial_len

    # ─── MÓDULO: CALIFICACIONES (CRUD) ───────────────────────────────

    def get_notas(self, id_alumno):
        """Obtiene las calificaciones de un alumno."""
        if self.connected:
            return self.query(
                "SELECT id, materia, calificacion, tipo, comentario, fecha "
                "FROM notas WHERE id_alumno = %s ORDER BY fecha DESC",
                (id_alumno,)
            )
        else:
            return [n for n in self.mock_notas if n["id_alumno"] == int(id_alumno)]

    def get_all_notas(self):
        """Obtiene todas las calificaciones cruzando con el nombre del alumno (para Profesores)."""
        if self.connected:
            return self.query(
                "SELECT n.id, n.id_alumno, u.nombre as nombre_alumno, n.materia, "
                "n.calificacion, n.tipo, n.comentario, n.fecha "
                "FROM notas n JOIN usuarios u ON n.id_alumno = u.id ORDER BY n.fecha DESC"
            )
        else:
            res = []
            for n in self.mock_notas:
                # Buscar nombre de alumno
                alumno_name = "Alumno Desconocido"
                for u in self.mock_usuarios:
                    if u["id"] == n["id_alumno"]:
                        alumno_name = u["nombre"]
                        break
                res.append({
                    "id": n["id"],
                    "id_alumno": n["id_alumno"],
                    "nombre_alumno": alumno_name,
                    "materia": n["materia"],
                    "calificacion": n["calificacion"],
                    "tipo": n["tipo"],
                    "comentario": n["comentario"],
                    "fecha": n["fecha"]
                })
            return res

    def create_nota(self, id_alumno, materia, calificacion, tipo="Examen", comentario=""):
        """Crea una nueva calificación."""
        fecha = date.today().isoformat()
        if self.connected:
            res = self.execute(
                "INSERT INTO notas (id_alumno, materia, calificacion, tipo, comentario, fecha) "
                "VALUES (%s, %s, %s, %s, %s, CURRENT_DATE) RETURNING id, id_alumno, materia, calificacion, tipo, comentario, fecha",
                (id_alumno, materia, Decimal(str(calificacion)), tipo, comentario)
            )
            return res
        else:
            new_id = max([n["id"] for n in self.mock_notas]) + 1 if self.mock_notas else 1
            new_nota = {
                "id": new_id,
                "id_alumno": int(id_alumno),
                "materia": materia,
                "calificacion": float(calificacion),
                "tipo": tipo,
                "comentario": comentario,
                "fecha": fecha
            }
            self.mock_notas.append(new_nota)
            return new_nota

    def update_nota(self, id_nota, calificacion, tipo, comentario):
        """Actualiza una calificación existente."""
        if self.connected:
            res = self.execute(
                "UPDATE notas SET calificacion = %s, tipo = %s, comentario = %s "
                "WHERE id = %s RETURNING id, id_alumno, materia, calificacion, tipo, comentario, fecha",
                (Decimal(str(calificacion)), tipo, comentario, id_nota)
            )
            return res
        else:
            for n in self.mock_notas:
                if n["id"] == int(id_nota):
                    n["calificacion"] = float(calificacion)
                    n["tipo"] = tipo
                    n["comentario"] = comentario
                    return n
            return None

    def delete_nota(self, id_nota):
        """Elimina una calificación."""
        if self.connected:
            self.execute("DELETE FROM notas WHERE id = %s", (id_nota,))
            return True
        else:
            initial_len = len(self.mock_notas)
            self.mock_notas = [n for n in self.mock_notas if n["id"] != int(id_nota)]
            return len(self.mock_notas) < initial_len

    # ─── MÓDULO: TABLÓN DE ANUNCIOS (CRUD) ───────────────────────────

    def get_anuncios(self):
        """Obtiene los anuncios ordenados por relevancia y fecha."""
        if self.connected:
            return self.query(
                "SELECT id, titulo, contenido, autor, fecha, importante "
                "FROM anuncios ORDER BY importante DESC, fecha DESC LIMIT 20"
            )
        else:
            # Ordenar por importante DESC, fecha DESC
            return sorted(self.mock_anuncios, key=lambda a: (a["importante"], a["fecha"]), reverse=True)

    def create_anuncio(self, titulo, contenido, autor="Dirección", importante=False):
        """Publica un nuevo anuncio."""
        fecha = datetime.now().isoformat()
        if self.connected:
            res = self.execute(
                "INSERT INTO anuncios (titulo, contenido, autor, fecha, importante) "
                "VALUES (%s, %s, %s, CURRENT_TIMESTAMP, %s) RETURNING id, titulo, contenido, autor, fecha, importante",
                (titulo, contenido, autor, importante)
            )
            return res
        else:
            new_id = max([a["id"] for a in self.mock_anuncios]) + 1 if self.mock_anuncios else 1
            new_anuncio = {
                "id": new_id,
                "titulo": titulo,
                "contenido": contenido,
                "autor": autor,
                "fecha": fecha,
                "importante": bool(importante)
            }
            self.mock_anuncios.append(new_anuncio)
            return new_anuncio

    def update_anuncio(self, id_anuncio, titulo, contenido, importante):
        """Actualiza un anuncio."""
        if self.connected:
            res = self.execute(
                "UPDATE anuncios SET titulo = %s, contenido = %s, importante = %s "
                "WHERE id = %s RETURNING id, titulo, contenido, autor, fecha, importante",
                (titulo, contenido, importante, id_anuncio)
            )
            return res
        else:
            for a in self.mock_anuncios:
                if a["id"] == int(id_anuncio):
                    a["titulo"] = titulo
                    a["contenido"] = contenido
                    a["importante"] = bool(importante)
                    return a
            return None

    def delete_anuncio(self, id_anuncio):
        """Elimina un anuncio."""
        if self.connected:
            self.execute("DELETE FROM anuncios WHERE id = %s", (id_anuncio,))
            return True
        else:
            initial_len = len(self.mock_anuncios)
            self.mock_anuncios = [a for a in self.mock_anuncios if a["id"] != int(id_anuncio)]
            return len(self.mock_anuncios) < initial_len

    # ─── MÓDULO: MENÚ DEL COMEDOR (CRUD) ─────────────────────────────

    def get_menu_comedor(self):
        """Obtiene el menú del comedor para los próximos días."""
        if self.connected:
            return self.query(
                "SELECT id, fecha, primer_plato, segundo_plato, postre, alergenos "
                "FROM menu_comedor WHERE fecha >= CURRENT_DATE "
                "ORDER BY fecha LIMIT 7"
            )
        else:
            # Filtrar por fecha hoy o futura
            today_str = date.today().isoformat()
            future_menus = [m for m in self.mock_menu if m["fecha"] >= today_str]
            return sorted(future_menus, key=lambda m: m["fecha"])

    def create_or_update_menu(self, fecha, primer_plato, segundo_plato, postre, alergenos=None):
        """Inserta o actualiza el menú para una fecha concreta."""
        if self.connected:
            res = self.execute(
                "INSERT INTO menu_comedor (fecha, primer_plato, segundo_plato, postre, alergenos) "
                "VALUES (%s, %s, %s, %s, %s) "
                "ON CONFLICT (fecha) DO UPDATE SET "
                "primer_plato = EXCLUDED.primer_plato, "
                "segundo_plato = EXCLUDED.segundo_plato, "
                "postre = EXCLUDED.postre, "
                "alergenos = EXCLUDED.alergenos "
                "RETURNING id, fecha, primer_plato, segundo_plato, postre, alergenos",
                (fecha, primer_plato, segundo_plato, postre, alergenos)
            )
            return res
        else:
            # Buscar si ya existe la fecha
            for m in self.mock_menu:
                if m["fecha"] == fecha:
                    m["primer_plato"] = primer_plato
                    m["segundo_plato"] = segundo_plato
                    m["postre"] = postre
                    m["alergenos"] = alergenos
                    return m
            new_id = max([m["id"] for m in self.mock_menu]) + 1 if self.mock_menu else 1
            new_menu = {
                "id": new_id,
                "fecha": fecha,
                "primer_plato": primer_plato,
                "segundo_plato": segundo_plato,
                "postre": postre,
                "alergenos": alergenos
            }
            self.mock_menu.append(new_menu)
            return new_menu

    def delete_menu(self, id_menu):
        """Elimina un menú diario."""
        if self.connected:
            self.execute("DELETE FROM menu_comedor WHERE id = %s", (id_menu,))
            return True
        else:
            initial_len = len(self.mock_menu)
            self.mock_menu = [m for m in self.mock_menu if m["id"] != int(id_menu)]
            return len(self.mock_menu) < initial_len

    # ─── MÓDULO: HORARIOS (CRUD) ─────────────────────────────────────

    def get_horario(self, user_id):
        """Obtiene el horario semanal de un alumno."""
        if self.connected:
            return self.query(
                "SELECT id, dia, hora_inicio, hora_fin, asignatura, aula "
                "FROM horarios WHERE id_alumno = %s "
                "ORDER BY CASE dia "
                "  WHEN 'Lunes' THEN 1 WHEN 'Martes' THEN 2 "
                "  WHEN 'Miércoles' THEN 3 WHEN 'Jueves' THEN 4 "
                "  WHEN 'Viernes' THEN 5 END, hora_inicio",
                (user_id,)
            )
        else:
            user_schedules = [h for h in self.mock_horario if h["id_alumno"] == int(user_id)]
            # Si el usuario no tiene horario propio, le asignamos el del alumno 1 de prueba
            if not user_schedules:
                user_schedules = [h for h in self.mock_horario if h["id_alumno"] == 1]
            
            dias_order = {'Lunes': 1, 'Martes': 2, 'Miércoles': 3, 'Jueves': 4, 'Viernes': 5}
            return sorted(user_schedules, key=lambda h: (dias_order.get(h["dia"], 9), h["hora_inicio"]))

    def get_all_horarios(self):
        """Obtiene todos los horarios de clases."""
        if self.connected:
            return self.query(
                "SELECT h.id, h.id_alumno, u.nombre as nombre_alumno, h.dia, "
                "h.hora_inicio, h.hora_fin, h.asignatura, h.aula "
                "FROM horarios h JOIN usuarios u ON h.id_alumno = u.id "
                "ORDER BY u.nombre, CASE h.dia "
                "  WHEN 'Lunes' THEN 1 WHEN 'Martes' THEN 2 "
                "  WHEN 'Miércoles' THEN 3 WHEN 'Jueves' THEN 4 "
                "  WHEN 'Viernes' THEN 5 END, h.hora_inicio"
            )
        else:
            res = []
            for h in self.mock_horario:
                alumno_name = "Alumno Desconocido"
                for u in self.mock_usuarios:
                    if u["id"] == h["id_alumno"]:
                        alumno_name = u["nombre"]
                        break
                res.append({
                    "id": h["id"],
                    "id_alumno": h["id_alumno"],
                    "nombre_alumno": alumno_name,
                    "dia": h["dia"],
                    "hora_inicio": h["hora_inicio"],
                    "hora_fin": h["hora_fin"],
                    "asignatura": h["asignatura"],
                    "aula": h["aula"]
                })
            dias_order = {'Lunes': 1, 'Martes': 2, 'Miércoles': 3, 'Jueves': 4, 'Viernes': 5}
            return sorted(res, key=lambda h: (h["nombre_alumno"], dias_order.get(h["dia"], 9), h["hora_inicio"]))

    def create_horario(self, id_alumno, dia, hora_inicio, hora_fin, asignatura, aula=""):
        """Añade una hora de clase al horario de un alumno."""
        if self.connected:
            res = self.execute(
                "INSERT INTO horarios (id_alumno, dia, hora_inicio, hora_fin, asignatura, aula) "
                "VALUES (%s, %s, %s, %s, %s, %s) RETURNING id, id_alumno, dia, hora_inicio, hora_fin, asignatura, aula",
                (id_alumno, dia, hora_inicio, hora_fin, asignatura, aula)
            )
            return res
        else:
            new_id = max([h["id"] for h in self.mock_horario]) + 1 if self.mock_horario else 1
            new_h = {
                "id": new_id,
                "id_alumno": int(id_alumno),
                "dia": dia,
                "hora_inicio": hora_inicio,
                "hora_fin": hora_fin,
                "asignatura": asignatura,
                "aula": aula
            }
            self.mock_horario.append(new_h)
            return new_h

    def delete_horario(self, id_horario):
        """Elimina una clase del horario."""
        if self.connected:
            self.execute("DELETE FROM horarios WHERE id = %s", (id_horario,))
            return True
        else:
            initial_len = len(self.mock_horario)
            self.mock_horario = [h for h in self.mock_horario if h["id"] != int(id_horario)]
            return len(self.mock_horario) < initial_len

    # ─── MÓDULO: CHAT ────────────────────────────────────────────────

    def get_chat_mensajes(self, zona, limit=50):
        """Obtiene mensajes recientes del chat en una zona."""
        if self.connected:
            return self.query(
                "SELECT u.nombre, c.mensaje, c.timestamp "
                "FROM chat_mensajes c JOIN usuarios u ON c.emisor_id = u.id "
                "WHERE c.zona = %s ORDER BY c.timestamp DESC LIMIT %s",
                (zona, limit)
            )
        else:
            # En modo demo no registramos en DB, pero devolvemos una lista vacía o lo gestionamos en memoria
            return []

    def save_chat_mensaje(self, user_id, mensaje, zona):
        """Guarda un mensaje de chat."""
        if self.connected:
            self.execute(
                "INSERT INTO chat_mensajes (emisor_id, mensaje, zona) VALUES (%s, %s, %s)",
                (user_id, mensaje, zona)
            )
        # En memoria ya se difunde por websockets, no requiere persistencia estricta en fallback
