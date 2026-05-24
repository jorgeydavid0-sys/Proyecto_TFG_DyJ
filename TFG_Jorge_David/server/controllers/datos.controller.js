/**
 * ═══════════════════════════════════════════════════════════════════════
 *   Controlador de Datos Académicos — Escuela Interactiva 2D
 *   TFG Jorge González — DAM 2º
 *
 *   Capa de Controladores: lógica de negocio y validación de roles
 *   para los datos académicos (anuncios, horario, menú, notas)
 * ═══════════════════════════════════════════════════════════════════════
 */
const DatosModel = require('../models/datos.model');

const DatosController = {

    /** GET /api/datos/anuncios — Tablón de anuncios (todos los roles) */
    async getAnuncios(req, res) {
        try {
            const anuncios = await DatosModel.getAnuncios();
            return res.json({ anuncios });
        } catch (err) {
            return res.status(500).json({ error: 'Error al obtener anuncios.' });
        }
    },

    /** GET /api/datos/horario — Horario del alumno autenticado */
    async getHorario(req, res) {
        try {
            const horario = await DatosModel.getHorario(req.user.id_usuario);
            return res.json({ horario });
        } catch (err) {
            return res.status(500).json({ error: 'Error al obtener horario.' });
        }
    },

    /** GET /api/datos/menu — Menú del comedor (todos los roles) */
    async getMenu(req, res) {
        try {
            const menu = await DatosModel.getMenu();
            return res.json({ menu });
        } catch (err) {
            return res.status(500).json({ error: 'Error al obtener menú.' });
        }
    },

    /** GET /api/datos/notas — Calificaciones del alumno autenticado */
    async getNotas(req, res) {
        try {
            const notas = await DatosModel.getNotas(req.user.id_usuario);
            return res.json({ notas });
        } catch (err) {
            return res.status(500).json({ error: 'Error al obtener notas.' });
        }
    },

    /**
     * POST /api/datos/notas — Añadir calificación
     * RBAC: solo profesor o admin (HTTP 403 para alumno)
     * Body: { id_alumno, id_asignatura, evaluacion, nota, comentario?, tipo? }
     */
    async addNota(req, res) {
        try {
            const { id_alumno, id_asignatura, evaluacion, nota, comentario, tipo } = req.body;
            if (!id_alumno || !id_asignatura || !evaluacion || nota === undefined) {
                return res.status(400).json({ error: 'Faltan campos obligatorios.' });
            }
            if (nota < 0 || nota > 10) {
                return res.status(400).json({ error: 'La nota debe estar entre 0 y 10.' });
            }

            const id = await DatosModel.addNota({ id_alumno, id_asignatura, evaluacion, nota, comentario, tipo });
            return res.status(201).json({ message: 'Calificación registrada.', id_calificacion: id });
        } catch (err) {
            return res.status(500).json({ error: 'Error al registrar calificación.' });
        }
    },

    /** GET /api/datos/alumnos — Lista de alumnos (solo profesor/admin) */
    async getAlumnos(req, res) {
        try {
            const alumnos = await DatosModel.getAlumnos();
            return res.json({ alumnos });
        } catch (err) {
            return res.status(500).json({ error: 'Error al obtener alumnos.' });
        }
    },

    /** GET /api/datos/asignaturas — Lista de asignaturas */
    async getAsignaturas(req, res) {
        try {
            const asignaturas = await DatosModel.getAsignaturas();
            return res.json({ asignaturas });
        } catch (err) {
            return res.status(500).json({ error: 'Error al obtener asignaturas.' });
        }
    },
};

module.exports = DatosController;
