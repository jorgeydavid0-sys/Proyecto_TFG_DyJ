/**
 * ═══════════════════════════════════════════════════════════════════════
 *   Escuela Interactiva 2D — Servidor Principal (Node.js)
 *   TFG Jorge González — DAM 2º
 *
 *   Arquitectura en Capas:
 *     Rutas (routes/) → Controladores (controllers/) → Modelos (models/)
 *
 *   Servidor Express + Socket.IO para multiplayer en tiempo real
 * ═══════════════════════════════════════════════════════════════════════
 */
const express    = require('express');
const http       = require('http');
const socketIO   = require('socket.io');
const cors       = require('cors');
const path       = require('path');
require('dotenv').config();

// ─── Capas de Rutas ──────────────────────────────────────────────────
const authRoutes  = require('./routes/auth.routes');
const datosRoutes = require('./routes/datos.routes');
const mapaRoutes  = require('./routes/mapa.routes');
const { verifyToken } = require('./middleware/auth.middleware');

// ─── Inicialización ──────────────────────────────────────────────────
const app    = express();
const server = http.createServer(app);
const io     = socketIO(server, {
    cors: { origin: '*', methods: ['GET', 'POST'] },
});

// ─── Middleware Global ───────────────────────────────────────────────
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Servir archivos estáticos del cliente
app.use(express.static(path.join(__dirname, '..', 'client')));

// ─── Capa de Rutas (API REST) ────────────────────────────────────────
app.use('/api/auth',  authRoutes);
app.use('/api/datos', verifyToken, datosRoutes);  // JWT obligatorio
app.use('/api/mapa',  mapaRoutes);

// Página principal → sirve el cliente Phaser.js
app.get('*', (req, res) => {
    res.sendFile(path.join(__dirname, '..', 'client', 'index.html'));
});

// ─── Socket.IO: Multiplayer en Tiempo Real ───────────────────────────
const players = {};  // { sid: { name, x, y, color, zone, rol } }

io.on('connection', (socket) => {
    console.log(`  [+] Jugador conectado: ${socket.id}`);

    // Jugador entra al juego
    socket.on('join', ({ name, x, y, color, zone, rol }) => {
        players[socket.id] = {
            name,
            x:    x    || 480,
            y:    y    || 550,
            color: color || '#4A90D9',
            zone:  zone  || 'entrada',
            rol:   rol   || 'alumno',
            direction: 'down',
        };
        socket.join(zone || 'entrada');

        // Enviar jugadores existentes en la zona al nuevo jugador
        const others = Object.entries(players)
            .filter(([sid]) => sid !== socket.id)
            .filter(([, p]) => p.zone === (zone || 'entrada'))
            .map(([sid, p]) => ({ id: sid, ...p }));
        socket.emit('players_existing', others);

        // Notificar a la zona que llegó un nuevo jugador
        socket.to(zone || 'entrada').emit('player_joined', {
            id: socket.id, name, x, y, color, zone: zone || 'entrada', rol,
        });
    });

    // Jugador se mueve
    socket.on('move', ({ x, y, direction, zone }) => {
        if (!players[socket.id]) return;
        players[socket.id].x = x;
        players[socket.id].y = y;
        players[socket.id].direction = direction;
        socket.to(zone).emit('player_moved', { id: socket.id, x, y, direction });
    });

    // Jugador cambia de zona (transición)
    socket.on('change_zone', ({ oldZone, newZone, x, y }) => {
        if (!players[socket.id]) return;
        socket.leave(oldZone);
        socket.join(newZone);

        // Avisar a zona anterior que se fue
        socket.to(oldZone).emit('player_left', { id: socket.id });

        // Actualizar estado
        players[socket.id].zone = newZone;
        players[socket.id].x    = x;
        players[socket.id].y    = y;

        // Enviar jugadores de la nueva zona
        const others = Object.entries(players)
            .filter(([sid]) => sid !== socket.id)
            .filter(([, p]) => p.zone === newZone)
            .map(([sid, p]) => ({ id: sid, ...p }));
        socket.emit('players_existing', others);

        // Avisar a la nueva zona
        socket.to(newZone).emit('player_joined', {
            id: socket.id, ...players[socket.id],
        });
    });

    // Chat de zona
    socket.on('chat', ({ message, zone }) => {
        if (!players[socket.id] || !message.trim()) return;
        io.to(zone).emit('chat_message', {
            id:        socket.id,
            name:      players[socket.id].name,
            color:     players[socket.id].color,
            message:   message.trim().substring(0, 200),
            timestamp: new Date().toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' }),
        });
    });

    // Desconexión
    socket.on('disconnect', () => {
        if (players[socket.id]) {
            const { zone } = players[socket.id];
            io.to(zone).emit('player_left', { id: socket.id });
            delete players[socket.id];
        }
        console.log(`  [-] Jugador desconectado: ${socket.id}`);
    });
});

// ─── Inicio del Servidor ─────────────────────────────────────────────
const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
    console.log('');
    console.log('═'.repeat(62));
    console.log('  🏫  Escuela Interactiva 2D  —  Servidor Node.js');
    console.log('  📚  TFG Jorge González  —  DAM 2º');
    console.log('═'.repeat(62));
    console.log(`  🌐  Servidor:  http://localhost:${PORT}`);
    console.log(`  🎮  Cliente:   http://localhost:${PORT}`);
    console.log(`  📡  API REST:  http://localhost:${PORT}/api`);
    console.log('═'.repeat(62));
    console.log('');
    console.log('  Cuentas de demostración:');
    console.log('  • Alumno:   jorge_dam2  / alumno123');
    console.log('  • Profesor: prof_garcia / profesor123');
    console.log('  • Admin:    admin       / admin123');
    console.log('');
    console.log('  ℹ️  Sin MySQL: usa cualquier nombre de usuario (≥3 chars)');
    console.log('     como contraseña usa "demo" o cualquier cosa');
    console.log('');
    console.log('─'.repeat(62));
    console.log('  Esperando conexiones...');
    console.log('─'.repeat(62));
});
