/**
 * Game — Main game loop, canvas rendering, input handling
 * Escuela Interactiva 2D
 */
class Game {
    constructor(canvas, network, ui) {
        this.canvas = canvas;
        this.ctx = canvas.getContext('2d');
        this.net = network;
        this.ui = ui;
        this.map = new GameMap();
        this.player = null;
        this.otherPlayers = {};
        this.keys = {};
        this.running = false;
        this.lastSend = 0;
        this.sendRate = 50; // ms between position updates
        this.nearInteraction = null;
        this.transitioning = false;
        this.zoneW = 960;
        this.zoneH = 640;
        this._resizeCanvas();
        this._bindInput();
        this._bindNetwork();
        window.addEventListener('resize', () => this._resizeCanvas());
    }

    _resizeCanvas() {
        const maxW = window.innerWidth - 32;
        const maxH = window.innerHeight - 100;
        const ratio = this.zoneW / this.zoneH;
        let w = maxW;
        let h = w / ratio;
        if (h > maxH) { h = maxH; w = h * ratio; }
        this.canvas.width = this.zoneW;
        this.canvas.height = this.zoneH;
        this.canvas.style.width = Math.floor(w) + 'px';
        this.canvas.style.height = Math.floor(h) + 'px';
    }

    _bindInput() {
        window.addEventListener('keydown', (e) => {
            if (this.ui.panelOpen && e.code === 'Escape') { this.ui.closePanel(); return; }
            if (this.ui.chatOpen) {
                if (e.code === 'Escape') this.ui.closeChat();
                return;
            }
            this.keys[e.code] = true;
            if (e.code === 'KeyE') this._tryInteract();
            if (e.code === 'Enter') this.ui.toggleChat();
        });
        window.addEventListener('keyup', (e) => { this.keys[e.code] = false; });
        // Lose focus = stop movement
        window.addEventListener('blur', () => { this.keys = {}; });
    }

    _bindNetwork() {
        this.net.on('joined', (data) => {
            this.map.setZones(data.zones);
            this.map.currentZone = data.zone;
            this.player = new Player(data.name, data.x, data.y, data.color, true);
            this.ui.setZoneName(data.zones[data.zone].name);
            this.net.requestPlayers();
            this.start();
        });

        this.net.on('player_moved', (data) => {
            const key = data.name;
            if (this.otherPlayers[key]) {
                this.otherPlayers[key].targetX = data.x;
                this.otherPlayers[key].targetY = data.y;
                this.otherPlayers[key].direction = data.direction;
                this.otherPlayers[key].moving = true;
            } else {
                const p = new Player(data.name, data.x, data.y, data.color, false);
                this.otherPlayers[key] = p;
            }
        });

        this.net.on('player_joined', (data) => {
            this.net.requestPlayers();
            this.ui.addChatMessage('Sistema', `${data.name} ha entrado a la zona`, '#60E1C8');
        });

        this.net.on('player_left', (data) => {
            delete this.otherPlayers[data.name];
            this.ui.addChatMessage('Sistema', `${data.name} ha salido de la zona`, '#E74C3C');
            this._updatePlayerCount();
        });

        this.net.on('players_update', (data) => {
            this.otherPlayers = {};
            if (data.players) {
                data.players.forEach(p => {
                    this.otherPlayers[p.name] = new Player(p.name, p.x, p.y, p.color, false);
                });
            }
            this._updatePlayerCount();
        });

        this.net.on('zone_changed', (data) => {
            this.map.currentZone = data.zone;
            this.player.x = data.x;
            this.player.y = data.y;
            this.ui.setZoneName(data.zone_config.name);
            this.otherPlayers = {};
            if (data.players) {
                data.players.forEach(p => {
                    this.otherPlayers[p.name] = new Player(p.name, p.x, p.y, p.color, false);
                });
            }
            this._updatePlayerCount();
            this.transitioning = false;
        });

        this.net.on('interaction_data', (data) => {
            this.ui.showPanel(data.type, data.data);
        });

        this.net.on('chat_broadcast', (data) => {
            this.ui.addChatMessage(data.name, data.message, data.color);
        });
    }

    start() {
        if (this.running) return;
        this.running = true;
        this._loop();
    }

    _loop() {
        if (!this.running) return;
        this._update();
        this._draw();
        requestAnimationFrame(() => this._loop());
    }

    _update() {
        if (!this.player || this.transitioning || this.ui.panelOpen) return;

        const moved = this.player.update(this.keys, this.zoneW, this.zoneH);

        // Send position to server at controlled rate
        const now = Date.now();
        if (moved && now - this.lastSend > this.sendRate) {
            this.net.sendMove(this.player.x, this.player.y, this.player.direction);
            this.lastSend = now;
        }

        // Update other players (interpolation)
        Object.values(this.otherPlayers).forEach(p => p.update({}, this.zoneW, this.zoneH));

        // Check transitions
        const transition = this.map.checkTransition(
            this.player.x, this.player.y, this.player.size, this.player.size
        );
        if (transition && !this.transitioning) {
            this.transitioning = true;
            this.ui.playTransition(() => {
                this.net.changeZone(transition.target_zone);
            });
        }

        // Check interactions
        const inter = this.map.checkInteraction(
            this.player.x, this.player.y, this.player.size, this.player.size
        );
        if (inter) {
            this.nearInteraction = inter;
            this.ui.showInteractHint(inter.label);
        } else {
            this.nearInteraction = null;
            this.ui.hideInteractHint();
        }
    }

    _draw() {
        const ctx = this.ctx;
        ctx.clearRect(0, 0, this.zoneW, this.zoneH);

        // Draw map
        this.map.drawZone(ctx, this.zoneW, this.zoneH);

        // Draw other players
        Object.values(this.otherPlayers).forEach(p => p.draw(ctx));

        // Draw local player on top
        if (this.player) this.player.draw(ctx);
    }

    _tryInteract() {
        if (this.nearInteraction && !this.ui.panelOpen) {
            this.net.interact(this.nearInteraction.type);
        }
    }

    _updatePlayerCount() {
        const count = Object.keys(this.otherPlayers).length + 1;
        this.ui.setPlayerCount(count);
    }
}
