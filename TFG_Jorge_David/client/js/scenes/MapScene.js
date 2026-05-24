/**
 * ═══════════════════════════════════════════════════════════════════════
 *   MapScene — Escena Principal del Juego
 *   Escuela Interactiva 2D | TFG Jorge González — DAM 2º
 *
 *   Phaser.js v3 — Motor de juego 2D
 *   Arcade Physics para colisiones con muros
 *   Socket.IO para multijugador en tiempo real
 * ═══════════════════════════════════════════════════════════════════════
 */
class MapScene extends Phaser.Scene {
    constructor() {
        super({ key: 'MapScene' });
    }

    // ─── Configuración del Mapa por Zona ─────────────────────────────
    static get ZONES() {
        return {
            entrada: {
                name: 'Entrada Exterior',
                width: 960, height: 640,
                spawn: { x: 480, y: 550 },
                bgColor: 0x1a2a0a,
                // 30×20 tiles de 32px — 0:césped 1:camino 2:muro 3:tejado 4:ventana 5:puerta 6:copa árbol 7:tronco árbol
                tileMap: MapScene._buildEntradaMap(),
                solidTiles: new Set([2, 3, 6]),
                transitions: [
                    { id: 'to_recepcion', target: 'recepcion', rect: { x: 440, y: 192, w: 80, h: 32 }, spawnX: 480, spawnY: 590 },
                ],
                interactions: [],
            },
            recepcion: {
                name: 'Recepción',
                width: 960, height: 640,
                spawn: { x: 480, y: 590 },
                bgColor: 0x1a1a2e,
                tileMap: MapScene._buildRecepcionMap(),
                solidTiles: new Set([9, 11]),
                transitions: [
                    { id: 'to_entrada',     target: 'entrada',     rect: { x: 400, y: 624, w: 160, h: 16 }, spawnX: 480, spawnY: 230 },
                    { id: 'to_conserjeria', target: 'conserjeria', rect: { x: 0,   y: 256, w: 16,  h: 128 }, spawnX: 940, spawnY: 320 },
                ],
                interactions: [
                    { id: 'tablon',  type: 'tablon',  label: '📋 Tablón de Anuncios',  x: 168, y: 32, w: 160, h: 80 },
                    { id: 'horario', type: 'horario', label: '📅 Horario / Calendario', x: 632, y: 32, w: 160, h: 80 },
                ],
            },
            conserjeria: {
                name: 'Conserjería',
                width: 960, height: 640,
                spawn: { x: 940, y: 320 },
                bgColor: 0x1e1208,
                tileMap: MapScene._buildConserjeriaMap(),
                solidTiles: new Set([13, 14, 18]),
                transitions: [
                    { id: 'to_recepcion2', target: 'recepcion', rect: { x: 944, y: 256, w: 16, h: 128 }, spawnX: 30, spawnY: 320 },
                ],
                interactions: [
                    { id: 'menu',  type: 'menu',  label: '🍽️ Menú del Comedor', x: 168, y: 32, w: 160, h: 80 },
                    { id: 'notas', type: 'notas', label: '📊 Notas / Trabajos',  x: 632, y: 32, w: 160, h: 80 },
                ],
            },
        };
    }

    // ─── Constructores de Tilemaps ────────────────────────────────────
    static _buildEntradaMap() {
        const R = 20, C = 30;
        const m = [];
        for (let r = 0; r < R; r++) {
            const row = [];
            for (let c = 0; c < C; c++) {
                let t = 0; // césped
                if (r >= 1 && r <= 4 && c >= 6 && c <= 23) t = 3;
                else if (r === 5 && c >= 6 && c <= 23) t = (c % 3 === 0 && c > 6 && c < 23) ? 4 : 2;
                else if (r === 6 && c >= 6 && c <= 23) t = (c >= 14 && c <= 15) ? 5 : 2;
                if (r >= 7 && c >= 13 && c <= 16) t = 1;
                if ((r === 9 || r === 15) && (c === 4 || c === 25)) t = 6;
                if ((r === 10 || r === 16) && (c === 4 || c === 25)) t = 7;
                if ((r === 12 || r === 18) && (c === 8 || c === 21)) t = 6;
                if ((r === 13 || r === 19) && (c === 8 || c === 21)) t = 7;
                row.push(t);
            }
            m.push(row);
        }
        return m;
    }
    static _buildRecepcionMap() {
        const R = 20, C = 30;
        const m = [];
        for (let r = 0; r < R; r++) {
            const row = [];
            for (let c = 0; c < C; c++) {
                let t = 8; // suelo baldosa
                if (r <= 1) t = 9;
                if (r === 1 && c >= 5 && c <= 9) t = 10;
                if (r === 1 && c >= 20 && c <= 24) t = 15;
                if (r >= 8 && r <= 10 && c >= 11 && c <= 18) t = 11;
                if (r >= 8 && r <= 11 && c === 0) t = 5;
                if (r === 19 && c >= 13 && c <= 16) t = 5;
                row.push(t);
            }
            m.push(row);
        }
        return m;
    }
    static _buildConserjeriaMap() {
        const R = 20, C = 30;
        const m = [];
        for (let r = 0; r < R; r++) {
            const row = [];
            for (let c = 0; c < C; c++) {
                let t = 12; // suelo madera
                if (r <= 1) t = 13;
                if (r === 1 && c >= 5 && c <= 9) t = 16;
                if (r === 1 && c >= 20 && c <= 24) t = 17;
                if (r >= 9 && r <= 10 && c >= 8 && c <= 21) t = 18;
                if (r >= 15 && r <= 18 && (c === 2 || c === 4 || c === 6)) t = 14;
                if (r >= 8 && r <= 11 && c === 29) t = 5;
                row.push(t);
            }
            m.push(row);
        }
        return m;
    }

    // ─── Mapeo tile → textura Phaser ─────────────────────────────────
    static get TILE_KEYS() {
        return [
            'tile_grass', 'tile_path', 'tile_wall', 'tile_roof', 'tile_window',
            'tile_door', 'tile_tree_top', 'tile_tree_bot', 'tile_floor_rec',
            'tile_wall_rec', 'tile_tablon', 'tile_desk', 'tile_floor_con',
            'tile_wall_con', 'tile_cabinet', 'tile_calendar', 'tile_menu',
            'tile_notas', 'tile_counter',
        ];
    }

    // ═════════════════════════════════════════════════════════════════
    //  LIFECYCLE
    // ═════════════════════════════════════════════════════════════════

    init(data) {
        this.playerData  = data || window._playerData || {};
        this.currentZone = this.playerData.zone || 'entrada';
        this.token       = this.playerData.token || localStorage.getItem('token') || '';
        this.socket      = null;
        this.otherPlayers = {}; // { sid: { x, y, color, name, direction, gfx, nameText } }
        this.transitioning = false;
        this.nearInteraction = null;
        this.nearTransition  = null;
        this.lastMoveTime = 0;
    }

    create() {
        const zoneConfig = MapScene.ZONES[this.currentZone];

        // Cámara
        this.cameras.main.setBackgroundColor(zoneConfig.bgColor);

        // 1. Dibujar mapa estático
        this._buildMap(zoneConfig);

        // 2. Zonas físicas de colisión (muros)
        this._buildWalls(zoneConfig);

        // 3. Crear jugador local
        this._createLocalPlayer(zoneConfig);

        // 4. Zonas de interacción (visual)
        this._buildInteractionZones(zoneConfig);

        // 5. Input
        this.cursors = this.input.keyboard.createCursorKeys();
        this.wasd    = this.input.keyboard.addKeys({
            up:       Phaser.Input.Keyboard.KeyCodes.W,
            down:     Phaser.Input.Keyboard.KeyCodes.S,
            left:     Phaser.Input.Keyboard.KeyCodes.A,
            right:    Phaser.Input.Keyboard.KeyCodes.D,
            interact: Phaser.Input.Keyboard.KeyCodes.E,
            chat:     Phaser.Input.Keyboard.KeyCodes.ENTER,
        });

        this.input.keyboard.on('keydown-E',     () => this._handleInteractKey());
        this.input.keyboard.on('keydown-ENTER', () => this._handleChatKey());

        // 6. Cámara sigue al jugador
        this.cameras.main.setBounds(0, 0, zoneConfig.width, zoneConfig.height);
        this.cameras.main.startFollow(this.playerSprite, true, 0.08, 0.08);

        // 7. Gráficos para jugador local y otros jugadores
        this.playerGfx  = this.add.graphics().setDepth(5);
        this.othersGfx  = this.add.graphics().setDepth(4);
        this.overlaygfx = this.add.graphics().setDepth(6);

        // 8. Socket.IO multijugador
        this._connectSocket();

        // 9. Actualizar HUD
        this._updateHUD();

        // Animar entrada
        this.cameras.main.fadeIn(400, 0, 0, 0);
    }

    update(time) {
        if (this.transitioning) return;
        this._movePlayer();
        this._drawLocalPlayer();
        this._drawOtherPlayers();
        this._checkZoneProximity();
        this._sendPosition(time);
    }

    // ═════════════════════════════════════════════════════════════════
    //  CONSTRUCCIÓN DEL MAPA
    // ═════════════════════════════════════════════════════════════════

    _buildMap(zoneConfig) {
        const S = 32;
        const keys = MapScene.TILE_KEYS;
        this.mapGroup = this.add.group();

        zoneConfig.tileMap.forEach((row, r) => {
            row.forEach((tileId, c) => {
                const key = keys[tileId] || 'tile_grass';
                const img = this.add.image(c * S + S / 2, r * S + S / 2, key);
                img.setDepth(0);
                this.mapGroup.add(img);
            });
        });
    }

    _buildWalls(zoneConfig) {
        const S = 32;
        this.walls = this.physics.add.staticGroup();
        zoneConfig.tileMap.forEach((row, r) => {
            row.forEach((tileId, c) => {
                if (zoneConfig.solidTiles.has(tileId)) {
                    const wall = this.add.rectangle(c * S + S / 2, r * S + S / 2, S, S, 0x000000, 0);
                    this.physics.add.existing(wall, true);
                    this.walls.add(wall);
                }
            });
        });
    }

    _buildInteractionZones(zoneConfig) {
        this.interactionZones  = [];
        this.transitionZones   = [];
        const gfx = this.add.graphics().setDepth(1);

        // Zonas de interacción
        zoneConfig.interactions.forEach(inter => {
            gfx.fillStyle(0x4a90d9, 0.08); gfx.fillRect(inter.x, inter.y, inter.w, inter.h);
            gfx.lineStyle(2, 0x4a90d9, 0.3); gfx.strokeRect(inter.x, inter.y, inter.w, inter.h);
            const label = this.add.text(inter.x + inter.w / 2, inter.y + inter.h / 2, inter.label, {
                fontFamily: 'Inter, sans-serif', fontSize: '10px', color: '#7bb3e8',
                align: 'center', wordWrap: { width: inter.w - 8 },
            }).setOrigin(0.5).setDepth(2).setAlpha(0.8);
            this.interactionZones.push({ ...inter, labelText: label });
        });

        // Zonas de transición
        zoneConfig.transitions.forEach(tr => {
            gfx.fillStyle(0x2ecc71, 0.12); gfx.fillRect(tr.rect.x, tr.rect.y, tr.rect.w, tr.rect.h);
            gfx.lineStyle(2, 0x2ecc71, 0.4); gfx.strokeRect(tr.rect.x, tr.rect.y, tr.rect.w, tr.rect.h);
            this.transitionZones.push(tr);
        });
    }

    // ═════════════════════════════════════════════════════════════════
    //  JUGADOR LOCAL
    // ═════════════════════════════════════════════════════════════════

    _createLocalPlayer(zoneConfig) {
        const { spawnX, spawnY } = this.playerData.spawn || {};
        const sx = spawnX || zoneConfig.spawn.x;
        const sy = spawnY || zoneConfig.spawn.y;

        // Sprite invisible para physics
        this.playerSprite = this.physics.add.image(sx, sy, 'player_base');
        this.playerSprite.setVisible(false);
        this.playerSprite.setCollideWorldBounds(true);
        this.playerSprite.body.setSize(20, 20);
        this.playerSprite.setDepth(5);

        // Colisión con muros
        this.physics.add.collider(this.playerSprite, this.walls);

        // Estado del jugador
        this.playerState = {
            x: sx, y: sy,
            direction: 'down', moving: false,
            frame: 0, animTimer: 0,
            speed: 160,
            color: this.playerData.color_avatar || '#4A90D9',
            name: this.playerData.username || this.playerData.nombre || 'Jugador',
        };
    }

    _movePlayer() {
        const body  = this.playerSprite.body;
        const speed = this.playerState.speed;
        let vx = 0, vy = 0;

        if (this.cursors.left.isDown  || this.wasd.left.isDown)  { vx = -speed; this.playerState.direction = 'left';  }
        if (this.cursors.right.isDown || this.wasd.right.isDown) { vx =  speed; this.playerState.direction = 'right'; }
        if (this.cursors.up.isDown    || this.wasd.up.isDown)    { vy = -speed; this.playerState.direction = 'up';    }
        if (this.cursors.down.isDown  || this.wasd.down.isDown)  { vy =  speed; this.playerState.direction = 'down';  }

        // Normalizar diagonal
        if (vx !== 0 && vy !== 0) { vx *= 0.707; vy *= 0.707; }

        this.playerSprite.setVelocity(vx, vy);
        this.playerState.moving  = vx !== 0 || vy !== 0;
        this.playerState.x = this.playerSprite.x;
        this.playerState.y = this.playerSprite.y;

        if (this.playerState.moving) {
            this.playerState.animTimer++;
            if (this.playerState.animTimer % 10 === 0)
                this.playerState.frame = (this.playerState.frame + 1) % 4;
        } else {
            this.playerState.frame = 0;
        }
    }

    _drawLocalPlayer() {
        const { x, y, color, name, moving, animTimer, direction } = this.playerState;
        this.playerGfx.clear();
        this._drawCharacter(this.playerGfx, x, y, color, name, moving, animTimer, direction, true);
    }

    _drawCharacter(gfx, x, y, color, name, moving, animTimer, direction, isLocal) {
        const S = 32;
        const col = Phaser.Display.Color.HexStringToColor(color);
        const hex = col.color;

        // Sombra
        gfx.fillStyle(0x000000, 0.25);
        gfx.fillEllipse(x, y + S / 2 - 2, S * 0.7, 8);

        // Bounce al caminar
        const bounce = moving ? Math.sin(animTimer * 0.5) * 2 : 0;
        const by = y - S / 2 + bounce;

        // Cuerpo
        gfx.fillStyle(hex, 1);
        gfx.fillRoundedRect(x - S / 2 + 6, by + 12, S - 12, S - 16, 4);

        // Cabeza (piel)
        const headHex = Phaser.Display.Color.HexStringToColor(
            this._lightenColor(color, 40)
        ).color;
        gfx.fillStyle(headHex, 1);
        gfx.fillCircle(x, by + 10, 10);

        // Ojos según dirección
        gfx.fillStyle(0xffffff, 1);
        let [ex1, ey1, ex2, ey2] = [x - 3, by + 9, x + 3, by + 9];
        if (direction === 'up')    { ey1 = ey2 = by + 7; }
        if (direction === 'left')  { ex1 = x - 6; ex2 = x; ey1 = ey2 = by + 8; }
        if (direction === 'right') { ex1 = x; ex2 = x + 6; ey1 = ey2 = by + 8; }
        gfx.fillCircle(ex1, ey1, 2.5);
        gfx.fillCircle(ex2, ey2, 2.5);
        gfx.fillStyle(0x222222, 1);
        gfx.fillCircle(ex1, ey1 + 0.5, 1.2);
        gfx.fillCircle(ex2, ey2 + 0.5, 1.2);

        // Piernas animadas
        const darkHex = Phaser.Display.Color.HexStringToColor(
            this._darkenColor(color, 40)
        ).color;
        gfx.fillStyle(darkHex, 1);
        if (moving) {
            const legOff = Math.sin(animTimer * 0.6) * 3;
            gfx.fillRect(x - S / 2 + 10, by + S - 8, 5, 8 + legOff);
            gfx.fillRect(x + S / 2 - 15, by + S - 8, 5, 8 - legOff);
        } else {
            gfx.fillRect(x - S / 2 + 10, by + S - 8, 5, 8);
            gfx.fillRect(x + S / 2 - 15, by + S - 8, 5, 8);
        }

        // Borde si es jugador local
        if (isLocal) {
            gfx.lineStyle(2, 0xffffff, 0.4);
            gfx.strokeCircle(x, by + 10, 12);
        }

        // Nametag
        const nameTag = name.length > 12 ? name.substring(0, 12) + '…' : name;
        gfx.fillStyle(0x000000, 0.7);
        gfx.fillRoundedRect(x - 35, by - 16, 70, 14, 4);
        // (El texto se dibuja en un Text object o via Canvas — aquí simplificamos)
    }

    // ═════════════════════════════════════════════════════════════════
    //  OTROS JUGADORES (Multijugador)
    // ═════════════════════════════════════════════════════════════════

    _drawOtherPlayers() {
        this.othersGfx.clear();
        Object.values(this.otherPlayers).forEach(p => {
            // Interpolación suave
            p.renderX = Phaser.Math.Linear(p.renderX ?? p.x, p.x, 0.25);
            p.renderY = Phaser.Math.Linear(p.renderY ?? p.y, p.y, 0.25);
            this._drawCharacter(
                this.othersGfx,
                p.renderX, p.renderY,
                p.color || '#999999',
                p.name || '?',
                p.moving || false,
                p.animTimer || 0,
                p.direction || 'down',
                false
            );
            if (p.moving) p.animTimer = (p.animTimer || 0) + 1;
        });
    }

    // ═════════════════════════════════════════════════════════════════
    //  DETECCIÓN DE ZONAS (Interacción + Transición)
    // ═════════════════════════════════════════════════════════════════

    _checkZoneProximity() {
        const px = this.playerState.x;
        const py = this.playerState.y;
        const margin = 24;

        // Interacciones
        this.nearInteraction = null;
        for (const inter of (MapScene.ZONES[this.currentZone].interactions || [])) {
            if (px > inter.x - margin && px < inter.x + inter.w + margin &&
                py > inter.y - margin && py < inter.y + inter.h + margin) {
                this.nearInteraction = inter;
                break;
            }
        }

        // Transiciones
        this.nearTransition = null;
        for (const tr of (MapScene.ZONES[this.currentZone].transitions || [])) {
            const r = tr.rect;
            if (px > r.x - 8 && px < r.x + r.w + 8 &&
                py > r.y - 8 && py < r.y + r.h + 8) {
                this.nearTransition = tr;
                break;
            }
        }

        // Actualizar hints en el DOM
        const iHint = document.getElementById('interaction-hint');
        const tHint = document.getElementById('transition-hint');
        if (iHint && tHint) {
            if (this.nearInteraction) {
                iHint.classList.remove('hidden');
                document.getElementById('interaction-hint-label').textContent = this.nearInteraction.label;
                tHint.classList.add('hidden');
            } else if (this.nearTransition) {
                tHint.classList.remove('hidden');
                document.getElementById('transition-hint-label').textContent =
                    '➡ ' + (MapScene.ZONES[this.nearTransition.target]?.name || this.nearTransition.target);
                iHint.classList.add('hidden');
            } else {
                iHint.classList.add('hidden');
                tHint.classList.add('hidden');
            }
        }

        // Auto-transición al tocar el borde
        if (this.nearTransition) {
            const r = this.nearTransition.rect;
            if (px > r.x && px < r.x + r.w && py > r.y && py < r.y + r.h) {
                this._doZoneTransition(this.nearTransition);
            }
        }
    }

    _handleInteractKey() {
        if (this.nearInteraction && !document.getElementById('data-panel').classList.contains('hidden') === false) {
            this._openDataPanel(this.nearInteraction);
        } else if (this.nearTransition) {
            this._doZoneTransition(this.nearTransition);
        }
    }

    _handleChatKey() {
        const chatPanel = document.getElementById('chat-panel');
        const chatInput = document.getElementById('chat-input');
        if (!chatPanel) return;
        if (chatPanel.classList.contains('hidden')) {
            chatPanel.classList.remove('hidden');
            chatInput.focus();
            chatInput.addEventListener('keydown', this._chatKeyHandler.bind(this), { once: false });
        }
    }

    _chatKeyHandler(e) {
        e.stopPropagation();
        if (e.key === 'Enter') {
            const msg = e.target.value.trim();
            if (msg && this.socket) {
                this.socket.emit('chat', { message: msg, zone: this.currentZone });
                e.target.value = '';
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════
    //  TRANSICIÓN DE ZONAS
    // ═════════════════════════════════════════════════════════════════

    _doZoneTransition(tr) {
        if (this.transitioning) return;
        this.transitioning = true;

        const fade = document.getElementById('zone-fade');
        if (fade) fade.classList.add('fade-in');

        const oldZone = this.currentZone;

        this.time.delayedCall(400, () => {
            if (this.socket) {
                this.socket.emit('change_zone', {
                    oldZone,
                    newZone: tr.target,
                    x: tr.spawnX,
                    y: tr.spawnY,
                });
            }

            this.currentZone = tr.target;
            Object.values(this.otherPlayers).forEach(p => {
                if (p.nameLabel) p.nameLabel.destroy();
            });
            this.otherPlayers = {};

            this.scene.restart({
                ...this.playerData,
                zone: tr.target,
                spawn: { spawnX: tr.spawnX, spawnY: tr.spawnY },
                token: this.token,
            });
        });
    }

    // ═════════════════════════════════════════════════════════════════
    //  PANEL DE DATOS (Modales)
    // ═════════════════════════════════════════════════════════════════

    _openDataPanel(interaction) {
        const panel   = document.getElementById('data-panel');
        const title   = document.getElementById('panel-title');
        const content = document.getElementById('panel-content');
        if (!panel) return;

        title.textContent = interaction.label;
        content.innerHTML = '<div class="loading-spinner">⏳ Cargando datos...</div>';
        panel.classList.remove('hidden');

        // Pausar movimiento
        this.input.keyboard.disableGlobalCapture();

        fetch(`/api/datos/${interaction.type}`, {
            headers: { Authorization: `Bearer ${this.token}` },
        })
        .then(r => r.json())
        .then(data => {
            content.innerHTML = this._renderPanel(interaction.type, data);
        })
        .catch(() => {
            content.innerHTML = '<p style="color:#ff6b6b;padding:1rem">⚠️ Error al cargar datos. ¿Está iniciada la sesión?</p>';
        });

        // Exponer closeDataPanel globalmente
        window.closeDataPanel = () => {
            panel.classList.add('hidden');
            this.input.keyboard.enableGlobalCapture();
        };
    }

    _renderPanel(type, data) {
        switch (type) {
            case 'tablon':    return this._renderTablon(data.anuncios || []);
            case 'horario':   return this._renderHorario(data.horario || []);
            case 'menu':      return this._renderMenu(data.menu || []);
            case 'notas':     return this._renderNotas(data.notas || []);
            default:          return '<p>Sin datos</p>';
        }
    }

    _renderTablon(anuncios) {
        if (!anuncios.length) return '<p style="color:#8b949e">No hay anuncios.</p>';
        return anuncios.map(a => `
            <div class="anuncio-card ${a.importante ? 'importante' : ''}">
                <div class="anuncio-titulo">
                    ${a.titulo}
                    ${a.importante ? '<span class="importante-badge">IMPORTANTE</span>' : ''}
                </div>
                <div class="anuncio-body">${a.contenido}</div>
                <div class="anuncio-autor">— ${a.autor}</div>
            </div>`).join('');
    }

    _renderHorario(horario) {
        const dias = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes'];
        let html = '<table class="panel-table"><thead><tr><th>Día</th><th>Hora</th><th>Asignatura</th><th>Aula</th></tr></thead><tbody>';
        let lastDay = '';
        horario.forEach(h => {
            const inicio = typeof h.hora_inicio === 'string' ? h.hora_inicio.substring(0, 5) : h.hora_inicio;
            const fin    = typeof h.hora_fin    === 'string' ? h.hora_fin.substring(0, 5)    : h.hora_fin;
            html += `<tr>
                <td>${h.dia !== lastDay ? `<span class="horario-dia">${h.dia}</span>` : ''}</td>
                <td>${inicio} – ${fin}</td>
                <td>${h.asignatura}</td>
                <td>${h.aula || '—'}</td>
            </tr>`;
            lastDay = h.dia;
        });
        html += '</tbody></table>';
        return html;
    }

    _renderMenu(menu) {
        if (!menu.length) return '<p style="color:#8b949e">No hay menú disponible.</p>';
        return menu.map(m => `
            <div class="menu-card">
                <div class="menu-fecha">${m.fecha}</div>
                <div>
                    <div class="menu-platos">🥣 ${m.primer_plato}<br>🍽️ ${m.segundo_plato}<br>🍮 ${m.postre}</div>
                    <div class="menu-alergenos">⚠ Alérgenos: ${m.alergenos}</div>
                </div>
            </div>`).join('');
    }

    _renderNotas(notas) {
        if (!notas.length) return '<p style="color:#8b949e">No hay calificaciones registradas.</p>';
        const media = (notas.reduce((s, n) => s + parseFloat(n.nota), 0) / notas.length).toFixed(2);
        const mediaClass = media >= 7 ? 'high' : media >= 5 ? 'med' : 'low';
        let html = `<div class="media-nota"><span class="nota-badge ${mediaClass}">Media: ${media}</span></div>`;
        html += '<table class="panel-table"><thead><tr><th>Asignatura</th><th>Evaluación</th><th>Nota</th><th>Tipo</th></tr></thead><tbody>';
        notas.forEach(n => {
            const nota = parseFloat(n.nota);
            const cls  = nota >= 7 ? 'high' : nota >= 5 ? 'med' : 'low';
            html += `<tr>
                <td>${n.asignatura}</td>
                <td>${n.evaluacion}</td>
                <td><span class="nota-badge ${cls}">${nota.toFixed(2)}</span></td>
                <td>${n.tipo}</td>
            </tr>`;
        });
        html += '</tbody></table>';
        return html;
    }

    // ═════════════════════════════════════════════════════════════════
    //  SOCKET.IO — MULTIJUGADOR EN TIEMPO REAL
    // ═════════════════════════════════════════════════════════════════

    _connectSocket() {
        if (typeof io === 'undefined') return;
        this.socket = io();

        this.socket.emit('join', {
            name:  this.playerState.name,
            x:     this.playerState.x,
            y:     this.playerState.y,
            color: this.playerState.color,
            zone:  this.currentZone,
            rol:   this.playerData.rol || 'alumno',
        });

        this.socket.on('players_existing', (players) => {
            players.forEach(p => this._addOtherPlayer(p));
            this._updateOnlineCount();
        });

        this.socket.on('player_joined', (p) => {
            this._addOtherPlayer(p);
            this._updateOnlineCount();
            this._addChatMessage({ name: '🔔 Sistema', color: '#4a90d9', message: `${p.name} entró a ${MapScene.ZONES[this.currentZone]?.name}`, timestamp: '' });
        });

        this.socket.on('player_moved', ({ id, x, y, direction }) => {
            if (this.otherPlayers[id]) {
                this.otherPlayers[id].x = x;
                this.otherPlayers[id].y = y;
                this.otherPlayers[id].direction = direction;
                this.otherPlayers[id].moving = true;
                clearTimeout(this.otherPlayers[id]._stopTimer);
                this.otherPlayers[id]._stopTimer = setTimeout(() => {
                    if (this.otherPlayers[id]) this.otherPlayers[id].moving = false;
                }, 150);
            }
        });

        this.socket.on('player_left', ({ id }) => {
            delete this.otherPlayers[id];
            this._updateOnlineCount();
        });

        this.socket.on('chat_message', (msg) => {
            this._addChatMessage(msg);
        });
    }

    _addOtherPlayer(p) {
        this.otherPlayers[p.id] = {
            x: p.x, y: p.y,
            renderX: p.x, renderY: p.y,
            color: p.color || '#999',
            name: p.name || '?',
            direction: 'down',
            moving: false,
            animTimer: 0,
        };
    }

    _sendPosition(time) {
        if (!this.socket || !this.playerState.moving) return;
        if (time - this.lastMoveTime < 50) return; // throttle 20fps
        this.lastMoveTime = time;
        this.socket.emit('move', {
            x:         Math.round(this.playerState.x),
            y:         Math.round(this.playerState.y),
            direction: this.playerState.direction,
            zone:      this.currentZone,
        });
    }

    // ═════════════════════════════════════════════════════════════════
    //  HUD & UI
    // ═════════════════════════════════════════════════════════════════

    _updateHUD() {
        const zoneName = document.getElementById('hud-zone-name');
        const username = document.getElementById('hud-username');
        const badge    = document.getElementById('hud-rol-badge');
        if (zoneName) zoneName.textContent = MapScene.ZONES[this.currentZone]?.name || this.currentZone;
        if (username) username.textContent = this.playerState.name;
        if (badge) {
            const rol = this.playerData.rol || 'alumno';
            badge.textContent = rol.charAt(0).toUpperCase() + rol.slice(1);
            badge.className   = `rol-badge ${rol}`;
        }
    }

    _updateOnlineCount() {
        const el = document.getElementById('hud-online-count');
        if (el) el.textContent = Object.keys(this.otherPlayers).length + 1;
    }

    _addChatMessage({ name, color, message, timestamp }) {
        const panel = document.getElementById('chat-panel');
        const msgs  = document.getElementById('chat-messages');
        if (!msgs) return;

        const div  = document.createElement('div');
        div.className = 'chat-msg';
        div.style.borderLeftColor = color || '#4a90d9';
        div.innerHTML = `
            <span class="chat-name" style="color:${color || '#7bb3e8'}">${name}</span>
            ${message}
            ${timestamp ? `<span class="chat-time">${timestamp}</span>` : ''}`;
        msgs.appendChild(div);
        msgs.scrollTop = msgs.scrollHeight;

        if (panel?.classList.contains('hidden')) {
            panel.classList.remove('hidden');
        }
    }

    // ═════════════════════════════════════════════════════════════════
    //  UTILIDADES DE COLOR
    // ═════════════════════════════════════════════════════════════════

    _lightenColor(hex, amount) {
        const num = parseInt(hex.replace('#', ''), 16);
        const r = Math.min(255, (num >> 16) + amount);
        const g = Math.min(255, ((num >> 8) & 0xff) + amount);
        const b = Math.min(255, (num & 0xff) + amount);
        return `#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}`;
    }

    _darkenColor(hex, amount) {
        const num = parseInt(hex.replace('#', ''), 16);
        const r = Math.max(0, (num >> 16) - amount);
        const g = Math.max(0, ((num >> 8) & 0xff) - amount);
        const b = Math.max(0, (num & 0xff) - amount);
        return `#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}`;
    }
}
