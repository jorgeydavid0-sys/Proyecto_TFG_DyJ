/**
 * ═══════════════════════════════════════════════════════════════════════
 *   Modelo de Usuarios — Escuela Interactiva 2D
 *   TFG Jorge González — DAM 2º
 *
 *   Capa de Modelos: consultas directas a la tabla usuarios y alumnos
 * ═══════════════════════════════════════════════════════════════════════
 */
const pool = require('./db');

const UsuarioModel = {

    /**
     * Busca un usuario por su username para login
     */
    async findByUsername(username) {
        const [rows] = await pool.query(
            'SELECT * FROM usuarios WHERE username = ?',
            [username]
        );
        return rows[0] || null;
    },

    /**
     * Busca un usuario por ID (para JWT verify)
     */
    async findById(id_usuario) {
        const [rows] = await pool.query(
            `SELECT u.id_usuario, u.username, u.nombre, u.apellidos, u.rol, u.color_avatar,
                    a.id_alumno, a.curso, a.posicion_x, a.posicion_y, a.zona_actual
             FROM usuarios u
             LEFT JOIN alumnos a ON a.id_usuario = u.id_usuario
             WHERE u.id_usuario = ?`,
            [id_usuario]
        );
        return rows[0] || null;
    },

    /**
     * Crea un nuevo usuario con rol alumno
     */
    async create({ username, password_hash, nombre, apellidos, color_avatar, curso }) {
        const conn = await pool.getConnection();
        try {
            await conn.beginTransaction();

            const [result] = await conn.query(
                `INSERT INTO usuarios (username, password_hash, nombre, apellidos, rol, color_avatar)
                 VALUES (?, ?, ?, ?, 'alumno', ?)`,
                [username, password_hash, nombre, apellidos, color_avatar || '#4A90D9']
            );
            const id_usuario = result.insertId;

            await conn.query(
                `INSERT INTO alumnos (id_usuario, curso, posicion_x, posicion_y)
                 VALUES (?, ?, 480.0, 550.0)`,
                [id_usuario, curso || '2º DAM']
            );

            // Copiar horario por defecto (del alumno demo si existe)
            const [demoRows] = await conn.query(
                `SELECT dia, hora_inicio, hora_fin, asignatura, aula
                 FROM horario WHERE id_usuario = (SELECT id_usuario FROM usuarios WHERE username = 'jorge_dam2' LIMIT 1)`
            );
            if (demoRows.length > 0) {
                for (const h of demoRows) {
                    await conn.query(
                        `INSERT INTO horario (id_usuario, dia, hora_inicio, hora_fin, asignatura, aula) VALUES (?,?,?,?,?,?)`,
                        [id_usuario, h.dia, h.hora_inicio, h.hora_fin, h.asignatura, h.aula]
                    );
                }
            }

            await conn.commit();
            return { id_usuario };
        } catch (err) {
            await conn.rollback();
            throw err;
        } finally {
            conn.release();
        }
    },

    /**
     * Actualiza la posición del avatar
     */
    async updatePosition(id_usuario, x, y, zona) {
        await pool.query(
            `UPDATE alumnos SET posicion_x = ?, posicion_y = ?, zona_actual = ?
             WHERE id_usuario = ?`,
            [x, y, zona, id_usuario]
        );
    },

    /**
     * Actualiza el timestamp de último login
     */
    async updateLastLogin(id_usuario) {
        await pool.query(
            'UPDATE usuarios SET last_login = NOW() WHERE id_usuario = ?',
            [id_usuario]
        );
    },
};

module.exports = UsuarioModel;
