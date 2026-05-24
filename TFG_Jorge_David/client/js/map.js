/**
 * Map — Zone rendering and management using Pokémon-style tiles
 * Escuela Interactiva 2D
 */
class GameMap {
    constructor() {
        this.currentZone = 'entrada';
        this.zones = {};
        this.tileSize = 32;
        this.cols = 30; // 960 / 32
        this.rows = 20; // 640 / 32
        
        // Define tile maps for each zone
        this.tileMaps = {
            entrada: this._buildEntradaMap(),
            recepcion: this._buildRecepcionMap(),
            conserjeria: this._buildConserjeriaMap()
        };
    }

    setZones(zones) { this.zones = zones; }
    getZone() { return this.zones[this.currentZone]; }

    _buildEntradaMap() {
        let map = [];
        for (let r = 0; r < this.rows; r++) {
            let row = [];
            for (let c = 0; c < this.cols; c++) {
                let tile = 0; // Grass
                
                // Building
                if (r >= 1 && r <= 4 && c >= 6 && c <= 23) {
                    tile = 3; // Roof
                } else if (r === 5 && c >= 6 && c <= 23) {
                    if (c % 3 === 0 && c > 6 && c < 23) tile = 4; // Window
                    else tile = 2; // Wall
                } else if (r === 6 && c >= 6 && c <= 23) {
                    if (c >= 14 && c <= 15) tile = 5; // Door
                    else tile = 2; // Wall
                }
                
                // Path
                if (r >= 7 && c >= 13 && c <= 16) {
                    tile = 1; // Path
                }
                
                // Trees
                if ((r === 9 || r === 15) && (c === 4 || c === 25)) {
                    tile = 6; // Tree Top
                } else if ((r === 10 || r === 16) && (c === 4 || c === 25)) {
                    tile = 7; // Tree Bottom
                }
                if ((r === 12 || r === 18) && (c === 8 || c === 21)) {
                    tile = 6;
                } else if ((r === 13 || r === 19) && (c === 8 || c === 21)) {
                    tile = 7;
                }

                row.push(tile);
            }
            map.push(row);
        }
        return map;
    }

    _buildRecepcionMap() {
        let map = [];
        for (let r = 0; r < this.rows; r++) {
            let row = [];
            for (let c = 0; c < this.cols; c++) {
                let tile = 8; // Reception Floor
                
                // Top wall
                if (r === 0 || r === 1) {
                    tile = 9; // Reception Wall
                }
                
                // Board on left wall
                if (r === 1 && c >= 5 && c <= 9) tile = 10;
                
                // Calendar on right wall
                if (r === 1 && c >= 20 && c <= 24) tile = 15;
                
                // Reception Desk in middle
                if (r >= 8 && r <= 10 && c >= 11 && c <= 18) {
                    tile = 11; // Desk
                }
                
                // Left door to conserjeria
                if (r >= 8 && r <= 11 && c === 0) {
                    tile = 5; // Door
                }
                
                // Bottom door (salida)
                if (r === 19 && c >= 13 && c <= 16) {
                    tile = 5; // Door
                }
                
                row.push(tile);
            }
            map.push(row);
        }
        return map;
    }

    _buildConserjeriaMap() {
        let map = [];
        for (let r = 0; r < this.rows; r++) {
            let row = [];
            for (let c = 0; c < this.cols; c++) {
                let tile = 12; // Conserjeria Floor
                
                // Top wall
                if (r === 0 || r === 1) {
                    tile = 13; // Conserjeria Wall
                }
                
                // Menu
                if (r === 1 && c >= 5 && c <= 9) tile = 16;
                
                // Notes
                if (r === 1 && c >= 20 && c <= 24) tile = 17;
                
                // Counter
                if (r >= 9 && r <= 10 && c >= 8 && c <= 21) {
                    tile = 18; // Counter
                }
                
                // File cabinets
                if (r >= 15 && r <= 18 && (c === 2 || c === 4 || c === 6)) {
                    tile = 14;
                }
                
                // Right door to recepcion
                if (r >= 8 && r <= 11 && c === 29) {
                    tile = 5;
                }
                
                row.push(tile);
            }
            map.push(row);
        }
        return map;
    }

    drawZone(ctx, w, h) {
        const mapData = this.tileMaps[this.currentZone] || this.tileMaps['entrada'];
        for (let r = 0; r < this.rows; r++) {
            for (let c = 0; c < this.cols; c++) {
                const tile = mapData[r][c];
                this._drawTile(ctx, c * this.tileSize, r * this.tileSize, tile);
            }
        }

        this._drawTransitionZones(ctx, w, h);
        this._drawInteractionZones(ctx, w, h);
    }

    _drawTile(ctx, x, y, tile) {
        const s = this.tileSize; // 32
        switch(tile) {
            case 0: // Grass
                ctx.fillStyle = '#6bcf63';
                ctx.fillRect(x, y, s, s);
                ctx.fillStyle = '#55b54d';
                ctx.fillRect(x + 4, y + 4, 4, 4);
                ctx.fillRect(x + 20, y + 16, 4, 4);
                ctx.fillRect(x + 8, y + 24, 4, 4);
                break;
            case 1: // Path
                ctx.fillStyle = '#e6c88e';
                ctx.fillRect(x, y, s, s);
                ctx.fillStyle = '#d1b277';
                ctx.fillRect(x + 2, y + 2, 4, 4);
                ctx.fillRect(x + 24, y + 12, 6, 4);
                ctx.fillRect(x + 10, y + 20, 4, 4);
                break;
            case 2: // Wall (Building)
                ctx.fillStyle = '#b87c51';
                ctx.fillRect(x, y, s, s);
                ctx.strokeStyle = '#945e38';
                ctx.lineWidth = 1;
                ctx.strokeRect(x, y, s, s);
                ctx.beginPath();
                ctx.moveTo(x, y + s/2); ctx.lineTo(x + s, y + s/2);
                ctx.moveTo(x + s/2, y); ctx.lineTo(x + s/2, y + s/2);
                ctx.moveTo(x + s/4, y + s/2); ctx.lineTo(x + s/4, y + s);
                ctx.moveTo(x + 3*s/4, y + s/2); ctx.lineTo(x + 3*s/4, y + s);
                ctx.stroke();
                break;
            case 3: // Roof
                ctx.fillStyle = '#a63a3a';
                ctx.fillRect(x, y, s, s);
                ctx.fillStyle = '#8a2b2b';
                ctx.fillRect(x, y + s - 4, s, 4);
                ctx.strokeStyle = '#6e1e1e';
                ctx.strokeRect(x, y, s, s);
                break;
            case 4: // Window
                ctx.fillStyle = '#b87c51';
                ctx.fillRect(x, y, s, s);
                ctx.fillStyle = '#7ac5e6';
                ctx.fillRect(x + 4, y + 4, s - 8, s - 8);
                ctx.fillStyle = '#fff';
                ctx.fillRect(x + 8, y + 8, 8, 4);
                ctx.strokeStyle = '#5a3e26';
                ctx.lineWidth = 2;
                ctx.strokeRect(x + 4, y + 4, s - 8, s - 8);
                ctx.beginPath();
                ctx.moveTo(x + s/2, y + 4); ctx.lineTo(x + s/2, y + s - 4);
                ctx.moveTo(x + 4, y + s/2); ctx.lineTo(x + s - 4, y + s/2);
                ctx.stroke();
                break;
            case 5: // Door
                ctx.fillStyle = '#5c3a21';
                ctx.fillRect(x, y, s, s);
                ctx.strokeStyle = '#3d2514';
                ctx.lineWidth = 2;
                ctx.strokeRect(x+2, y+2, s-4, s-4);
                ctx.fillStyle = '#d1a13a';
                ctx.beginPath(); ctx.arc(x + s - 8, y + s/2, 3, 0, Math.PI*2); ctx.fill();
                break;
            case 6: // Tree Top
                ctx.fillStyle = '#6bcf63';
                ctx.fillRect(x, y, s, s);
                ctx.fillStyle = '#2d8a2d';
                ctx.beginPath(); ctx.arc(x + s/2, y + s/2 + 8, s/2 + 4, 0, Math.PI*2); ctx.fill();
                ctx.fillStyle = '#3eb53e';
                ctx.beginPath(); ctx.arc(x + s/2, y + s/2 + 4, s/2, 0, Math.PI*2); ctx.fill();
                break;
            case 7: // Tree Bottom
                ctx.fillStyle = '#6bcf63';
                ctx.fillRect(x, y, s, s);
                ctx.fillStyle = '#664229';
                ctx.fillRect(x + s/2 - 4, y, 8, s - 4);
                ctx.fillStyle = '#2d8a2d';
                ctx.beginPath(); ctx.arc(x + s/2, y, s/2 + 4, 0, Math.PI, true); ctx.fill();
                break;
            case 8: // Reception Floor
                ctx.fillStyle = ((x/s+y/s)%2 === 0) ? '#e0e0e0' : '#f5f5f5';
                ctx.fillRect(x, y, s, s);
                break;
            case 9: // Reception Wall
                ctx.fillStyle = '#4a6b8c';
                ctx.fillRect(x, y, s, s);
                ctx.fillStyle = '#3a546e';
                ctx.fillRect(x, y + s - 6, s, 6);
                break;
            case 10: // Board (Tablon)
                ctx.fillStyle = '#4a6b8c';
                ctx.fillRect(x, y, s, s);
                ctx.fillStyle = '#8a623b';
                ctx.fillRect(x + 2, y + 4, s - 4, s - 8);
                ctx.fillStyle = '#fff';
                ctx.fillRect(x + 6, y + 8, 6, 8);
                ctx.fillStyle = '#f0d890';
                ctx.fillRect(x + 16, y + 10, 8, 6);
                ctx.fillStyle = '#ff6b6b';
                ctx.fillRect(x + 8, y + 6, 2, 2);
                ctx.fillRect(x + 20, y + 8, 2, 2);
                break;
            case 11: // Desk
                ctx.fillStyle = ((x/s+y/s)%2 === 0) ? '#e0e0e0' : '#f5f5f5';
                ctx.fillRect(x, y, s, s);
                ctx.fillStyle = '#734d26';
                ctx.fillRect(x + 2, y + 4, s - 4, s - 8);
                ctx.fillStyle = '#5c3a1a';
                ctx.fillRect(x + 2, y + 4, s - 4, 4);
                break;
            case 12: // Conserjeria Floor
                ctx.fillStyle = '#8c6239';
                ctx.fillRect(x, y, s, s);
                ctx.strokeStyle = '#6e4a29';
                ctx.lineWidth = 1;
                ctx.beginPath();
                ctx.moveTo(x, y + 8); ctx.lineTo(x + s, y + 8);
                ctx.moveTo(x, y + 16); ctx.lineTo(x + s, y + 16);
                ctx.moveTo(x, y + 24); ctx.lineTo(x + s, y + 24);
                ctx.moveTo(x + 10, y); ctx.lineTo(x + 10, y + 8);
                ctx.moveTo(x + 20, y + 8); ctx.lineTo(x + 20, y + 16);
                ctx.moveTo(x + 15, y + 16); ctx.lineTo(x + 15, y + 24);
                ctx.moveTo(x + 25, y + 24); ctx.lineTo(x + 25, y + s);
                ctx.stroke();
                break;
            case 13: // Conserjeria Wall
                ctx.fillStyle = '#5c4530';
                ctx.fillRect(x, y, s, s);
                ctx.fillStyle = '#453221';
                ctx.fillRect(x, y + s - 6, s, 6);
                break;
            case 14: // File Cabinet
                ctx.fillStyle = '#8c6239';
                ctx.fillRect(x, y, s, s);
                ctx.strokeStyle = '#6e4a29'; ctx.lineWidth = 1;
                ctx.beginPath(); ctx.moveTo(x, y + 8); ctx.lineTo(x + s, y + 8); ctx.stroke();
                ctx.fillStyle = '#7a7a7a';
                ctx.fillRect(x + 4, y + 2, s - 8, s - 4);
                ctx.fillStyle = '#949494';
                ctx.fillRect(x + 6, y + 4, s - 12, s/2 - 6);
                ctx.fillRect(x + 6, y + s/2 + 2, s - 12, s/2 - 6);
                ctx.fillStyle = '#333';
                ctx.fillRect(x + 12, y + 6, 8, 2);
                ctx.fillRect(x + 12, y + s/2 + 4, 8, 2);
                break;
            case 15: // Calendar
                ctx.fillStyle = '#4a6b8c';
                ctx.fillRect(x, y, s, s);
                ctx.fillStyle = '#fff';
                ctx.fillRect(x + 4, y + 4, s - 8, s - 8);
                ctx.fillStyle = '#e64545';
                ctx.fillRect(x + 4, y + 4, s - 8, 6);
                ctx.fillStyle = '#ccc';
                for(let i=12; i<s-4; i+=4) {
                    ctx.fillRect(x + 6, y + i, s - 12, 1);
                }
                break;
            case 16: // Menu
                ctx.fillStyle = '#5c4530';
                ctx.fillRect(x, y, s, s);
                ctx.fillStyle = '#2a5a2a';
                ctx.fillRect(x + 2, y + 4, s - 4, s - 8);
                ctx.fillStyle = '#fff';
                ctx.fillRect(x + 6, y + 8, s - 12, 2);
                ctx.fillRect(x + 6, y + 14, s - 16, 2);
                ctx.fillRect(x + 6, y + 20, s - 14, 2);
                break;
            case 17: // Notes Conserjeria
                ctx.fillStyle = '#5c4530';
                ctx.fillRect(x, y, s, s);
                ctx.fillStyle = '#5a2a2a';
                ctx.fillRect(x + 2, y + 4, s - 4, s - 8);
                ctx.fillStyle = '#f0d890';
                ctx.fillRect(x + 6, y + 8, 8, 8);
                ctx.fillStyle = '#fff';
                ctx.fillRect(x + 16, y + 12, 8, 10);
                ctx.fillStyle = '#ff6b6b';
                ctx.fillRect(x + 10, y + 6, 2, 2);
                ctx.fillRect(x + 20, y + 10, 2, 2);
                break;
            case 18: // Counter Conserjeria
                ctx.fillStyle = '#8c6239';
                ctx.fillRect(x, y, s, s);
                ctx.fillStyle = '#4a3320';
                ctx.fillRect(x + 2, y + 4, s - 4, s - 8);
                ctx.fillStyle = '#382516';
                ctx.fillRect(x + 2, y + 4, s - 4, 4);
                break;
        }
    }

    _drawTransitionZones(ctx, w, h) {
        const zone = this.zones[this.currentZone];
        if (!zone || !zone.transitions) return;
        zone.transitions.forEach(t => {
            const r = t.rect;
            ctx.fillStyle = 'rgba(96, 225, 200, 0.12)';
            ctx.fillRect(r.x, r.y, r.w, r.h);
            ctx.strokeStyle = 'rgba(96, 225, 200, 0.4)';
            ctx.lineWidth = 2; ctx.setLineDash([6, 4]);
            ctx.strokeRect(r.x, r.y, r.w, r.h);
            ctx.setLineDash([]);
        });
    }

    _drawInteractionZones(ctx, w, h) {
        const zone = this.zones[this.currentZone];
        if (!zone || !zone.interactions) return;
        zone.interactions.forEach(inter => {
            const r = inter.rect;
            ctx.fillStyle = 'rgba(74, 144, 217, 0.08)';
            ctx.fillRect(r.x, r.y, r.w, r.h);
        });
    }

    checkTransition(px, py, pw, ph) {
        const zone = this.zones[this.currentZone];
        if (!zone || !zone.transitions) return null;
        for (const t of zone.transitions) {
            const r = t.rect;
            if (px + pw > r.x && px < r.x + r.w && py + ph > r.y && py < r.y + r.h) {
                return t;
            }
        }
        return null;
    }

    checkInteraction(px, py, pw, ph) {
        const zone = this.zones[this.currentZone];
        if (!zone || !zone.interactions) return null;
        for (const inter of zone.interactions) {
            const r = inter.rect;
            const margin = 20;
            if (px + pw > r.x - margin && px < r.x + r.w + margin &&
                py + ph > r.y - margin && py < r.y + r.h + margin) {
                return inter;
            }
        }
        return null;
    }
}
