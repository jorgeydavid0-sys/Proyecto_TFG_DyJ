/**
 * Rutas de Datos Académicos — Escuela Interactiva 2D
 * Todas las rutas requieren JWT (verifyToken se aplica en app.js)
 *
 * GET  /api/datos/anuncios     — Tablón de anuncios (todos)
 * GET  /api/datos/horario      — Horario del alumno autenticado
 * GET  /api/datos/menu         — Menú del comedor (todos)
 * GET  /api/datos/notas        — Notas del alumno autenticado
 * POST /api/datos/notas        — Añadir nota (solo profesor/admin) → HTTP 403 si alumno
 * GET  /api/datos/alumnos      — Lista de alumnos (solo profesor/admin)
 * GET  /api/datos/asignaturas  — Lista de asignaturas (todos)
 */
const express = require('express');
const router = express.Router();
const DatosController = require('../controllers/datos.controller');
const { requireRole } = require('../middleware/auth.middleware');

// Rutas accesibles para todos los roles autenticados
router.get('/anuncios',    DatosController.getAnuncios);
router.get('/horario',     DatosController.getHorario);
router.get('/menu',        DatosController.getMenu);
router.get('/notas',       DatosController.getNotas);
router.get('/asignaturas', DatosController.getAsignaturas);

// Rutas restringidas a profesor y admin (RBAC)
// Un alumno recibirá HTTP 403 Forbidden
router.post('/notas',   requireRole('profesor', 'admin'), DatosController.addNota);
router.get('/alumnos',  requireRole('profesor', 'admin'), DatosController.getAlumnos);

module.exports = router;
