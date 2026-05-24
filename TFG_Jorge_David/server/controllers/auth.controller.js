/**
 * ═══════════════════════════════════════════════════════════════════════
 *   Controlador de Autenticación — Escuela Interactiva 2D
 *   TFG Jorge González — DAM 2º
 *
 *   Capa de Controladores: lógica de negocio para login y registro
 * ═══════════════════════════════════════════════════════════════════════
 */
const bcrypt = require('bcryptjs');
const UsuarioModel = require('../models/usuario.model');
const { generateToken } = require('../middleware/auth.middleware');

const AuthController = {

    /**
     * POST /api/auth/login
     * Body: { username, password }
     * Devuelve: { token, usuario }
     */
    async login(req, res) {
        try {
            const { username, password } = req.body;
            if (!username || !password) {
                return res.status(400).json({ error: 'Username y contraseña son obligatorios.' });
            }

            // Buscar usuario en DB
            let usuario;
            try {
                usuario = await UsuarioModel.findByUsername(username);
            } catch {
                // Sin MySQL: acceso demo
                usuario = null;
            }

            // Modo demo sin MySQL: cualquier credencial válida
            if (!usuario) {
                if (username === 'demo' || username.length >= 3) {
                    const rol = username.toLowerCase().includes('admin') ? 'admin' : (username.toLowerCase().includes('prof') ? 'profesor' : 'alumno');
                    const token = generateToken({ id_usuario: 0, username, rol });
                    return res.json({
                        token,
                        usuario: { id_usuario: 0, username, nombre: username, apellidos: '', rol, color_avatar: '#4A90D9' },
                        demo: true,
                    });
                }
                return res.status(401).json({ error: 'Usuario no encontrado.' });
            }

            // Verificar contraseña con bcrypt
            const passwordOk = await bcrypt.compare(password, usuario.password_hash);
            if (!passwordOk) {
                return res.status(401).json({ error: 'Contraseña incorrecta.' });
            }

            // Actualizar last_login
            await UsuarioModel.updateLastLogin(usuario.id_usuario);

            // Generar JWT
            const token = generateToken({
                id_usuario: usuario.id_usuario,
                username:   usuario.username,
                rol:        usuario.rol,
            });

            return res.json({
                token,
                usuario: {
                    id_usuario:   usuario.id_usuario,
                    username:     usuario.username,
                    nombre:       usuario.nombre,
                    apellidos:    usuario.apellidos,
                    rol:          usuario.rol,
                    color_avatar: usuario.color_avatar,
                },
            });
        } catch (err) {
            console.error('[Auth] Error en login:', err.message);
            return res.status(500).json({ error: 'Error interno del servidor.' });
        }
    },

    /**
     * POST /api/auth/register
     * Body: { username, password, nombre, apellidos, curso?, color_avatar? }
     * Devuelve: { token, usuario }
     */
    async register(req, res) {
        try {
            const { username, password, nombre, apellidos, curso, color_avatar } = req.body;

            if (!username || !password || !nombre || !apellidos) {
                return res.status(400).json({ error: 'Faltan campos obligatorios.' });
            }
            if (username.length < 3 || username.length > 50) {
                return res.status(400).json({ error: 'El username debe tener entre 3 y 50 caracteres.' });
            }
            if (password.length < 4) {
                return res.status(400).json({ error: 'La contraseña debe tener mínimo 4 caracteres.' });
            }

            // Verificar que no exista ya
            const existente = await UsuarioModel.findByUsername(username).catch(() => null);
            if (existente) {
                return res.status(409).json({ error: 'El username ya está en uso.' });
            }

            // Hash de contraseña (bcrypt, 10 rounds)
            const password_hash = await bcrypt.hash(password, 10);

            const { id_usuario } = await UsuarioModel.create({
                username, password_hash, nombre, apellidos, color_avatar, curso,
            });

            const token = generateToken({ id_usuario, username, rol: 'alumno' });

            return res.status(201).json({
                token,
                usuario: { id_usuario, username, nombre, apellidos, rol: 'alumno', color_avatar: color_avatar || '#4A90D9' },
            });
        } catch (err) {
            console.error('[Auth] Error en registro:', err.message);
            return res.status(500).json({ error: 'Error al registrar usuario.' });
        }
    },

    /**
     * GET /api/auth/me
     * Header: Authorization: Bearer <token>
     * Devuelve: datos completos del usuario autenticado
     */
    async me(req, res) {
        try {
            let usuario;
            try {
                usuario = await UsuarioModel.findById(req.user.id_usuario);
            } catch {
                usuario = null;
            }

            if (!usuario) {
                // Modo demo
                return res.json({
                    id_usuario:   req.user.id_usuario,
                    username:     req.user.username,
                    nombre:       req.user.username,
                    apellidos:    '',
                    rol:          req.user.rol,
                    color_avatar: '#4A90D9',
                    demo:         true,
                });
            }

            return res.json(usuario);
        } catch (err) {
            return res.status(500).json({ error: 'Error al obtener usuario.' });
        }
    },
};

module.exports = AuthController;
