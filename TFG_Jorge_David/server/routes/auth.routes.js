/**
 * Rutas de Autenticación — Escuela Interactiva 2D
 * POST /api/auth/login    — Login con username + contraseña
 * POST /api/auth/register — Registro de nuevo alumno
 * GET  /api/auth/me       — Info del usuario autenticado
 */
const express = require('express');
const router = express.Router();
const AuthController = require('../controllers/auth.controller');
const { verifyToken } = require('../middleware/auth.middleware');

router.post('/login',    AuthController.login);
router.post('/register', AuthController.register);
router.get('/me',        verifyToken, AuthController.me);

module.exports = router;
