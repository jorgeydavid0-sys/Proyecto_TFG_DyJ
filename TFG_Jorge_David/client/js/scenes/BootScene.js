/**
 * ═══════════════════════════════════════════════════════════════════════
 *   BootScene — Escena de Carga
 *   Escuela Interactiva 2D | TFG Jorge González — DAM 2º
 *
 *   Genera todas las texturas de tiles programáticamente usando
 *   Phaser.GameObjects.Graphics y generateTexture(), sin necesidad
 *   de archivos de imagen externos.
 * ═══════════════════════════════════════════════════════════════════════
 */
class BootScene extends Phaser.Scene {
    constructor() {
        super({ key: 'BootScene' });
    }

    preload() {
        // Barra de carga
        const w = this.scale.width;
        const h = this.scale.height;

        const barBg = this.add.rectangle(w / 2, h / 2 + 40, 300, 10, 0x1e2530);
        const bar   = this.add.rectangle(w / 2 - 150, h / 2 + 40, 0, 10, 0x4a90d9);
        bar.setOrigin(0, 0.5);

        const label = this.add.text(w / 2, h / 2 + 20, 'Generando mapa...', {
            fontFamily: 'Inter, sans-serif', fontSize: '13px', color: '#8b949e',
        }).setOrigin(0.5);

        this.load.on('progress', (v) => { bar.width = 300 * v; });
        this.load.on('complete', () => { label.setText('¡Listo!'); });
    }

    create() {
        // Generar todas las texturas de tiles
        this._generateTileTextures();
        this._generatePlayerTextures();
        this._generateUITextures();

        // Pequeña pausa para que se vea la pantalla de carga
        this.time.delayedCall(300, () => {
            this.scene.start('MapScene');
        });
    }

    // ─── Generador de Texturas de Tiles ──────────────────────────────
    _generateTileTextures() {
        const g = this.make.graphics({ x: 0, y: 0, add: false });
        const S = 32; // Tamaño de tile

        // ── TILE 0: Césped (Entrada Exterior) ──
        this._makeTile(g, 'tile_grass', S, () => {
            g.fillStyle(0x5db84e); g.fillRect(0, 0, S, S);
            g.fillStyle(0x4da03e); g.fillRect(4, 4, 5, 5);
            g.fillRect(20, 16, 4, 4);
            g.fillRect(8, 24, 5, 5);
            g.fillStyle(0x6ecf5e); g.fillRect(14, 10, 3, 3);
        });

        // ── TILE 1: Camino / Sendero ──
        this._makeTile(g, 'tile_path', S, () => {
            g.fillStyle(0xe0c98a); g.fillRect(0, 0, S, S);
            g.fillStyle(0xceb576); g.fillRect(2, 2, 5, 5);
            g.fillRect(24, 12, 6, 4);
            g.fillRect(10, 20, 4, 5);
        });

        // ── TILE 2: Muro de edificio ──
        this._makeTile(g, 'tile_wall', S, () => {
            g.fillStyle(0xc47f4e); g.fillRect(0, 0, S, S);
            g.lineStyle(1, 0x9e6038);
            g.strokeRect(0, 0, S, S);
            g.beginPath(); g.moveTo(0, 16); g.lineTo(S, 16); g.strokePath();
            g.beginPath(); g.moveTo(16, 0); g.lineTo(16, 16); g.strokePath();
            g.beginPath(); g.moveTo(8, 16); g.lineTo(8, S); g.strokePath();
            g.beginPath(); g.moveTo(24, 16); g.lineTo(24, S); g.strokePath();
        });

        // ── TILE 3: Tejado ──
        this._makeTile(g, 'tile_roof', S, () => {
            g.fillStyle(0xa83030); g.fillRect(0, 0, S, S);
            g.fillStyle(0x8a2020); g.fillRect(0, S - 5, S, 5);
            g.lineStyle(1, 0x6e1515); g.strokeRect(0, 0, S, S);
        });

        // ── TILE 4: Ventana ──
        this._makeTile(g, 'tile_window', S, () => {
            g.fillStyle(0xc47f4e); g.fillRect(0, 0, S, S);
            g.fillStyle(0x7ac5e6); g.fillRect(4, 4, S - 8, S - 8);
            g.fillStyle(0xffffff); g.fillRect(8, 8, 8, 4);
            g.lineStyle(2, 0x5a3e26); g.strokeRect(4, 4, S - 8, S - 8);
            g.beginPath(); g.moveTo(16, 4); g.lineTo(16, S - 4); g.strokePath();
            g.beginPath(); g.moveTo(4, 16); g.lineTo(S - 4, 16); g.strokePath();
        });

        // ── TILE 5: Puerta ──
        this._makeTile(g, 'tile_door', S, () => {
            g.fillStyle(0x5c3a21); g.fillRect(0, 0, S, S);
            g.lineStyle(2, 0x3d2514); g.strokeRect(2, 2, S - 4, S - 4);
            g.fillStyle(0xd1a13a);
            g.fillCircle(S - 8, S / 2, 3);
        });

        // ── TILE 6: Copa de árbol ──
        this._makeTile(g, 'tile_tree_top', S, () => {
            g.fillStyle(0x5db84e); g.fillRect(0, 0, S, S);
            g.fillStyle(0x2d8a2d); g.fillCircle(16, 20, 16);
            g.fillStyle(0x3eb53e); g.fillCircle(16, 16, 14);
            g.fillStyle(0x55d055); g.fillCircle(12, 12, 7);
        });

        // ── TILE 7: Tronco de árbol ──
        this._makeTile(g, 'tile_tree_bot', S, () => {
            g.fillStyle(0x5db84e); g.fillRect(0, 0, S, S);
            g.fillStyle(0x664229); g.fillRect(12, 0, 8, 28);
            g.fillStyle(0x2d8a2d); g.fillCircle(16, 0, 16);
        });

        // ── TILE 8: Suelo de recepción (baldosa) ──
        this._makeTile(g, 'tile_floor_rec', S, () => {
            g.fillStyle(0xe8e8e8); g.fillRect(0, 0, S, S);
            g.lineStyle(1, 0xcccccc);
            g.strokeRect(0, 0, S, S);
            g.fillStyle(0xd0d0d0); g.fillRect(1, 1, 14, 14);
            g.fillRect(17, 17, 14, 14);
        });

        // ── TILE 9: Pared de recepción ──
        this._makeTile(g, 'tile_wall_rec', S, () => {
            g.fillStyle(0x4a6b8c); g.fillRect(0, 0, S, S);
            g.fillStyle(0x3a546e); g.fillRect(0, S - 6, S, 6);
            g.lineStyle(1, 0x2d4158); g.strokeRect(0, 0, S, S);
        });

        // ── TILE 10: Tablón de anuncios ──
        this._makeTile(g, 'tile_tablon', S, () => {
            g.fillStyle(0x4a6b8c); g.fillRect(0, 0, S, S);
            g.fillStyle(0x8a623b); g.fillRect(2, 4, S - 4, S - 8);
            g.fillStyle(0xfdf3d0); g.fillRect(5, 7, 22, 18);
            // Notas en el tablón
            g.fillStyle(0xff6b6b); g.fillRect(7, 9, 8, 6);
            g.fillStyle(0xffd700); g.fillRect(17, 9, 8, 6);
            g.fillStyle(0x6bdfff); g.fillRect(7, 17, 8, 6);
            g.fillStyle(0xff6b6b); g.fillCircle(8, 9, 2);
            g.fillStyle(0xffd700); g.fillCircle(18, 9, 2);
        });

        // ── TILE 11: Mesa de recepción ──
        this._makeTile(g, 'tile_desk', S, () => {
            g.fillStyle(0xe8e8e8); g.fillRect(0, 0, S, S);
            g.fillStyle(0x734d26); g.fillRect(2, 4, S - 4, S - 8);
            g.fillStyle(0x5c3a1a); g.fillRect(2, 4, S - 4, 5);
            g.fillStyle(0x8a6030); g.fillRect(4, 10, 10, 2);
        });

        // ── TILE 12: Suelo de conserjería ──
        this._makeTile(g, 'tile_floor_con', S, () => {
            g.fillStyle(0x8c6239); g.fillRect(0, 0, S, S);
            g.lineStyle(1, 0x6e4a29);
            g.beginPath(); g.moveTo(0, 8); g.lineTo(S, 8); g.strokePath();
            g.beginPath(); g.moveTo(0, 16); g.lineTo(S, 16); g.strokePath();
            g.beginPath(); g.moveTo(0, 24); g.lineTo(S, 24); g.strokePath();
            g.beginPath(); g.moveTo(10, 0); g.lineTo(10, S); g.strokePath();
            g.beginPath(); g.moveTo(22, 0); g.lineTo(22, S); g.strokePath();
        });

        // ── TILE 13: Pared de conserjería ──
        this._makeTile(g, 'tile_wall_con', S, () => {
            g.fillStyle(0x5c4530); g.fillRect(0, 0, S, S);
            g.fillStyle(0x453221); g.fillRect(0, S - 6, S, 6);
            g.lineStyle(1, 0x362817); g.strokeRect(0, 0, S, S);
        });

        // ── TILE 14: Archivador ──
        this._makeTile(g, 'tile_cabinet', S, () => {
            g.fillStyle(0x8c6239); g.fillRect(0, 0, S, S);
            g.fillStyle(0x7a7a7a); g.fillRect(4, 2, S - 8, S - 4);
            g.fillStyle(0x949494); g.fillRect(6, 4, S - 12, 12);
            g.fillRect(6, 18, S - 12, 12);
            g.fillStyle(0x333333); g.fillRect(12, 9, 8, 2);
            g.fillRect(12, 23, 8, 2);
        });

        // ── TILE 15: Calendario ──
        this._makeTile(g, 'tile_calendar', S, () => {
            g.fillStyle(0x4a6b8c); g.fillRect(0, 0, S, S);
            g.fillStyle(0xffffff); g.fillRect(4, 4, S - 8, S - 8);
            g.fillStyle(0xe64545); g.fillRect(4, 4, S - 8, 8);
            g.fillStyle(0xcccccc);
            for (let i = 14; i < S - 4; i += 4) {
                g.fillRect(6, i, S - 12, 1);
            }
            g.fillStyle(0x333333); g.fillRect(10, 6, 4, 4); g.fillRect(18, 6, 4, 4);
        });

        // ── TILE 16: Menú del comedor ──
        this._makeTile(g, 'tile_menu', S, () => {
            g.fillStyle(0x5c4530); g.fillRect(0, 0, S, S);
            g.fillStyle(0x2a5a2a); g.fillRect(2, 4, S - 4, S - 8);
            g.fillStyle(0xffffff);
            g.fillRect(6, 8, S - 12, 2);
            g.fillRect(6, 14, S - 16, 2);
            g.fillRect(6, 20, S - 14, 2);
        });

        // ── TILE 17: Tablilla de notas (conserjería) ──
        this._makeTile(g, 'tile_notas', S, () => {
            g.fillStyle(0x5c4530); g.fillRect(0, 0, S, S);
            g.fillStyle(0x5a2a2a); g.fillRect(2, 4, S - 4, S - 8);
            g.fillStyle(0xf0d890); g.fillRect(6, 8, 8, 10);
            g.fillStyle(0xffffff); g.fillRect(16, 10, 10, 8);
            g.fillStyle(0xff6b6b); g.fillRect(10, 7, 2, 2); g.fillRect(20, 9, 2, 2);
        });

        // ── TILE 18: Mostrador de conserjería ──
        this._makeTile(g, 'tile_counter', S, () => {
            g.fillStyle(0x8c6239); g.fillRect(0, 0, S, S);
            g.fillStyle(0x4a3320); g.fillRect(2, 4, S - 4, S - 8);
            g.fillStyle(0x382516); g.fillRect(2, 4, S - 4, 5);
        });

        g.destroy();
    }

    /** Helper: limpia, dibuja, genera textura */
    _makeTile(g, key, size, drawFn) {
        g.clear();
        drawFn();
        g.generateTexture(key, size, size);
    }

    // ─── Texturas del Jugador (8 frames: 4 dir × 2 pasos) ───────────
    _generatePlayerTextures() {
        const g = this.make.graphics({ x: 0, y: 0, add: false });
        const S = 32;
        const directions = ['down', 'left', 'right', 'up'];
        const frames = 4;

        // Generar sprite sheet básico para cada color
        // Los colores reales se aplican en runtime al dibujar con tint
        this._makePlayerFrame(g, 'player_base', S);
        g.destroy();
    }

    _makePlayerFrame(g, key, S) {
        g.clear();
        // Body
        g.fillStyle(0xffffff, 1); // Blanco base — se colorea con tint
        g.fillRoundedRect(6, 12, S - 12, S - 16, 4);
        // Head
        g.fillStyle(0xffe0b0);
        g.fillCircle(S / 2, 10, 10);
        // Eyes
        g.fillStyle(0x333333);
        g.fillCircle(12, 9, 2);
        g.fillCircle(20, 9, 2);
        g.generateTexture(key, S, S);
    }

    // ─── Texturas UI (zonas de interacción, transición) ─────────────
    _generateUITextures() {
        const g = this.make.graphics({ x: 0, y: 0, add: false });

        // Zona de interacción (128×80, borde azul pulsante)
        g.clear();
        g.fillStyle(0x4a90d9, 0.1); g.fillRect(0, 0, 160, 80);
        g.lineStyle(2, 0x4a90d9, 0.5); g.strokeRect(0, 0, 160, 80);
        g.generateTexture('zone_interact', 160, 80);

        // Zona de transición (160×20, borde verde)
        g.clear();
        g.fillStyle(0x2ecc71, 0.12); g.fillRect(0, 0, 160, 20);
        g.lineStyle(2, 0x2ecc71, 0.5); g.strokeRect(0, 0, 160, 20);
        g.generateTexture('zone_transition', 160, 20);

        g.destroy();
    }
}
