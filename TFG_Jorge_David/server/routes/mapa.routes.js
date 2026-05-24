/**
 * Rutas del Mapa — Escuela Interactiva 2D
 * GET /api/mapa/zonas          — Configuración de zonas
 * GET /api/mapa/puntos-interes — Puntos de interacción por zona
 */
const express = require('express');
const router = express.Router();
const DatosModel = require('../models/datos.model');

// Configuración de zonas del mapa (igual que el PDF: entrada, recepción, conserjería)
const ZONAS = {
    entrada: {
        name: 'Entrada Exterior',
        width: 960, height: 640,
        spawn: { x: 480, y: 550 },
        transitions: [
            { target_zone: 'recepcion', rect: { x: 400, y: 0, w: 160, h: 20 }, spawn: { x: 480, y: 580 } },
        ],
        interactions: [],
    },
    recepcion: {
        name: 'Recepción',
        width: 960, height: 640,
        spawn: { x: 480, y: 580 },
        transitions: [
            { target_zone: 'entrada',     rect: { x: 400, y: 620, w: 160, h: 20 }, spawn: { x: 480, y: 550 } },
            { target_zone: 'conserjeria', rect: { x: 0,   y: 250, w: 20,  h: 140 }, spawn: { x: 930, y: 320 } },
        ],
        interactions: [
            { id: 'tablon',  type: 'tablon',  label: '📋 Tablón de Anuncios',  rect: { x: 160, y: 40, w: 160, h: 80 } },
            { id: 'horario', type: 'horario', label: '📅 Horario / Calendario', rect: { x: 640, y: 40, w: 160, h: 80 } },
        ],
    },
    conserjeria: {
        name: 'Conserjería',
        width: 960, height: 640,
        spawn: { x: 930, y: 320 },
        transitions: [
            { target_zone: 'recepcion', rect: { x: 940, y: 250, w: 20, h: 140 }, spawn: { x: 30, y: 320 } },
        ],
        interactions: [
            { id: 'menu',  type: 'menu',  label: '🍽️ Menú del Comedor', rect: { x: 160, y: 40, w: 160, h: 80 } },
            { id: 'notas', type: 'notas', label: '📊 Notas / Trabajos', rect: { x: 640, y: 40, w: 160, h: 80 } },
        ],
    },
};

/** GET /api/mapa/zonas */
router.get('/zonas', (req, res) => {
    res.json({ zonas: ZONAS });
});

/** GET /api/mapa/puntos-interes?zona=recepcion */
router.get('/puntos-interes', async (req, res) => {
    const { zona } = req.query;
    try {
        // Primero intenta desde MySQL, si falla usa config estática
        const dbPuntos = await DatosModel.getPuntosInteres(zona || 'recepcion');
        if (dbPuntos.length > 0) {
            return res.json({ puntos: dbPuntos });
        }
    } catch { /* usa config estática */ }

    const zonaConfig = ZONAS[zona];
    const puntos = zonaConfig ? zonaConfig.interactions : [];
    res.json({ puntos });
});

module.exports = router;
