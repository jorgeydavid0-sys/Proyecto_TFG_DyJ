/**
 * ═══════════════════════════════════════════════════════════════════════
 *   Middleware de Autenticación y RBAC — Escuela Interactiva 2D
 *   TFG Jorge González — DAM 2º
 *
 *   - verifyToken: Verifica el JWT en el header Authorization
 *   - requireRole: Middleware factory para comprobar roles (RBAC)
 * ═══════════════════════════════════════════════════════════════════════
 */
const jwt = require('jsonwebtoken');
require('dotenv').config();

const JWT_SECRET = process.env.JWT_SECRET || 'escuela-interactiva-2d-tfg-secret';

/**
 * Verifica que el request tenga un JWT válido en el header
 * Authorization: Bearer <token>
 */
const verifyToken = (req, res, next) => {
    const authHeader = req.headers['authorization'];
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return res.status(401).json({ error: 'Token no proporcionado. Acceso denegado.' });
    }

    const token = authHeader.split(' ')[1];
    try {
        const decoded = jwt.verify(token, JWT_SECRET);
        req.user = decoded; // { id_usuario, username, rol }
        next();
    } catch (err) {
        return res.status(401).json({ error: 'Token inválido o expirado.' });
    }
};

/**
 * Middleware factory RBAC: comprueba que el usuario tenga uno de los roles permitidos.
 * Un usuario con rol 'alumno' que intenta acceder a rutas de 'profesor' recibe HTTP 403.
 *
 * Uso: router.post('/notas', verifyToken, requireRole('profesor','admin'), ...)
 */
const requireRole = (...roles) => {
    return (req, res, next) => {
        if (!req.user) {
            return res.status(401).json({ error: 'No autenticado.' });
        }
        if (!roles.includes(req.user.rol)) {
            return res.status(403).json({
                error: `Acceso denegado. Se requiere rol: ${roles.join(' o ')}.`,
                tu_rol: req.user.rol,
            });
        }
        next();
    };
};

/**
 * Genera un JWT para el usuario dado
 */
const generateToken = (payload) => {
    return jwt.sign(payload, JWT_SECRET, {
        expiresIn: process.env.JWT_EXPIRES_IN || '24h',
    });
};

module.exports = { verifyToken, requireRole, generateToken };
