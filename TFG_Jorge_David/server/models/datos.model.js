/**
 * ═══════════════════════════════════════════════════════════════════════
 *   Modelo de Datos Académicos — Escuela Interactiva 2D
 *   TFG Jorge González — DAM 2º
 *
 *   Consultas: anuncios, horario, menú comedor, calificaciones,
 *              puntos de interés del mapa
 * ═══════════════════════════════════════════════════════════════════════
 */
const pool = require('./db');

// ─── Datos de demostración (cuando no hay MySQL disponible) ───────────
const DEMO = {
    anuncios: [
        { id_anuncio: 1, titulo: '📢 Bienvenidos al nuevo curso', contenido: 'Bienvenidos al curso 2025-2026 de DAM. Revisad los horarios actualizados.', autor: 'Dirección', importante: true },
        { id_anuncio: 2, titulo: '🏆 Concurso de Programación', contenido: 'Inscripción abierta para el concurso anual. Fecha límite: 15 de mayo.', autor: 'Dpto. Informática', importante: false },
        { id_anuncio: 3, titulo: '📅 Entrega TFG', contenido: 'Fecha límite para la entrega del TFG: 30 de junio.', autor: 'Secretaría', importante: true },
        { id_anuncio: 4, titulo: '🎮 Torneo de eSports', contenido: 'Torneo de League of Legends. Inscripciones hasta el viernes.', autor: 'Club Gaming', importante: false },
    ],
    horario: [
        { dia: 'Lunes',      hora_inicio: '08:30', hora_fin: '10:15', asignatura: 'Programación',          aula: 'Aula 201' },
        { dia: 'Lunes',      hora_inicio: '10:30', hora_fin: '12:15', asignatura: 'Base de Datos',         aula: 'Aula 202' },
        { dia: 'Martes',     hora_inicio: '08:30', hora_fin: '10:15', asignatura: 'Entornos de Desarrollo', aula: 'Lab 1' },
        { dia: 'Miércoles',  hora_inicio: '08:30', hora_fin: '10:15', asignatura: 'Sistemas Informáticos', aula: 'Aula 105' },
        { dia: 'Jueves',     hora_inicio: '10:30', hora_fin: '12:15', asignatura: 'Desarrollo Web',        aula: 'Lab 2' },
        { dia: 'Viernes',    hora_inicio: '08:30', hora_fin: '10:15', asignatura: 'Programación',          aula: 'Aula 201' },
    ],
    menu: [
        { fecha: 'Hoy',      primer_plato: 'Crema de calabaza',   segundo_plato: 'Pollo al horno con patatas',    postre: 'Fruta', alergenos: 'Lácteos' },
        { fecha: 'Mañana',   primer_plato: 'Ensalada mixta',      segundo_plato: 'Merluza a la plancha',          postre: 'Yogur', alergenos: 'Pescado, Lácteos' },
        { fecha: 'Miércoles',primer_plato: 'Lentejas estofadas',  segundo_plato: 'Tortilla española',             postre: 'Helado',alergenos: 'Huevo' },
    ],
    notas: [
        { asignatura: 'Programación',          evaluacion: '1ª Eval', nota: 8.50, tipo: 'Examen',   comentario: 'Buen trabajo en POO' },
        { asignatura: 'Base de Datos',         evaluacion: '1ª Eval', nota: 9.00, tipo: 'Examen',   comentario: 'Excelente diseño de esquema' },
        { asignatura: 'Sistemas Informáticos', evaluacion: '1ª Eval', nota: 7.25, tipo: 'Práctica', comentario: 'Mejorar documentación' },
        { asignatura: 'Entornos de Desarrollo',evaluacion: '1ª Eval', nota: 8.75, tipo: 'Proyecto', comentario: 'Proyecto TFG sobresaliente' },
        { asignatura: 'Desarrollo Web',        evaluacion: '1ª Eval', nota: 9.50, tipo: 'Examen',   comentario: 'Dominio de frontend y backend' },
    ],
};

const DatosModel = {

    /** Obtiene todos los anuncios del tablón */
    async getAnuncios() {
        try {
            const [rows] = await pool.query(
                'SELECT * FROM anuncios ORDER BY importante DESC, created_at DESC'
            );
            return rows.length > 0 ? rows : DEMO.anuncios;
        } catch {
            return DEMO.anuncios;
        }
    },

    /** Obtiene el horario de un usuario específico */
    async getHorario(id_usuario) {
        try {
            const [rows] = await pool.query(
                `SELECT dia, hora_inicio, hora_fin, asignatura, aula
                 FROM horario WHERE id_usuario = ?
                 ORDER BY FIELD(dia,'Lunes','Martes','Miércoles','Jueves','Viernes'), hora_inicio`,
                [id_usuario]
            );
            return rows.length > 0 ? rows : DEMO.horario;
        } catch {
            return DEMO.horario;
        }
    },

    /** Obtiene el menú del comedor para la semana actual */
    async getMenu() {
        try {
            const [rows] = await pool.query(
                `SELECT DATE_FORMAT(fecha,'%d/%m/%Y') AS fecha, primer_plato, segundo_plato, postre, alergenos
                 FROM menu_comedor WHERE fecha >= CURDATE() ORDER BY fecha LIMIT 5`
            );
            return rows.length > 0 ? rows : DEMO.menu;
        } catch {
            return DEMO.menu;
        }
    },

    /** Obtiene las calificaciones de un alumno */
    async getNotas(id_usuario) {
        try {
            const [rows] = await pool.query(
                `SELECT as2.nombre_asignatura AS asignatura, c.evaluacion, c.nota,
                        c.tipo, c.comentario, DATE_FORMAT(c.fecha_registro,'%d/%m/%Y') AS fecha
                 FROM calificaciones c
                 JOIN alumnos al ON al.id_alumno = c.id_alumno
                 JOIN asignaturas as2 ON as2.id_asignatura = c.id_asignatura
                 WHERE al.id_usuario = ?
                 ORDER BY c.fecha_registro DESC`,
                [id_usuario]
            );
            return rows.length > 0 ? rows : DEMO.notas;
        } catch {
            return DEMO.notas;
        }
    },

    /** Añade una calificación (solo profesor/admin) */
    async addNota({ id_alumno, id_asignatura, evaluacion, nota, comentario, tipo }) {
        try {
            const [result] = await pool.query(
                `INSERT INTO calificaciones (id_alumno, id_asignatura, evaluacion, nota, comentario, tipo)
                 VALUES (?, ?, ?, ?, ?, ?)`,
                [id_alumno, id_asignatura, evaluacion, nota, comentario || '', tipo || 'Examen']
            );
            return result.insertId;
        } catch (err) {
            console.log('  ⚠️  [DB Fallback] Simulación de inserción de calificación en modo Demo');
            return Math.floor(Math.random() * 1000) + 1; // ID de demostración
        }
    },

    /** Obtiene los puntos de interés de una zona del mapa */
    async getPuntosInteres(zona) {
        try {
            const [rows] = await pool.query(
                'SELECT * FROM puntos_interes WHERE nombre_zona = ?',
                [zona]
            );
            return rows;
        } catch {
            return [];
        }
    },

    /** Obtiene todos los alumnos (para panel profesor) */
    async getAlumnos() {
        try {
            const [rows] = await pool.query(
                `SELECT a.id_alumno, u.nombre, u.apellidos, u.username, a.curso
                 FROM alumnos a JOIN usuarios u ON u.id_usuario = a.id_usuario
                 ORDER BY u.apellidos`
            );
            return rows;
        } catch {
            return [];
        }
    },

    /** Obtiene todas las asignaturas */
    async getAsignaturas() {
        try {
            const [rows] = await pool.query(
                `SELECT as2.id_asignatura, as2.nombre_asignatura, u.nombre AS profesor
                 FROM asignaturas as2
                 LEFT JOIN usuarios u ON u.id_usuario = as2.id_profesor`
            );
            return rows;
        } catch {
            return [];
        }
    },
};

module.exports = DatosModel;
