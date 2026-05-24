/**
 * Player — Character rendering and movement
 * Escuela Interactiva 2D
 */
class Player {
    constructor(name, x, y, color, isLocal = false) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.color = color || '#4A90D9';
        this.isLocal = isLocal;
        this.size = 32;
        this.speed = 4;
        this.direction = 'down';
        this.frame = 0;
        this.animTimer = 0;
        this.moving = false;
        this.targetX = x;
        this.targetY = y;
    }

    update(keys, zoneW, zoneH) {
        if (!this.isLocal) {
            // Interpolate remote players
            this.x += (this.targetX - this.x) * 0.25;
            this.y += (this.targetY - this.y) * 0.25;
            return false;
        }
        let dx = 0, dy = 0;
        if (keys['KeyW'] || keys['ArrowUp'])    dy = -this.speed;
        if (keys['KeyS'] || keys['ArrowDown'])  dy = this.speed;
        if (keys['KeyA'] || keys['ArrowLeft'])   dx = -this.speed;
        if (keys['KeyD'] || keys['ArrowRight'])  dx = this.speed;

        this.moving = dx !== 0 || dy !== 0;
        if (this.moving) {
            if (dy < 0) this.direction = 'up';
            else if (dy > 0) this.direction = 'down';
            else if (dx < 0) this.direction = 'left';
            else if (dx > 0) this.direction = 'right';
            this.animTimer++;
            if (this.animTimer % 8 === 0) this.frame = (this.frame + 1) % 4;
        } else {
            this.frame = 0;
            this.animTimer = 0;
        }

        this.x = Math.max(0, Math.min(zoneW - this.size, this.x + dx));
        this.y = Math.max(0, Math.min(zoneH - this.size, this.y + dy));
        return this.moving;
    }

    draw(ctx) {
        const x = Math.round(this.x);
        const y = Math.round(this.y);
        const s = this.size;
        const hs = s / 2;

        // Shadow
        ctx.fillStyle = 'rgba(0,0,0,0.25)';
        ctx.beginPath();
        ctx.ellipse(x + hs, y + s - 2, hs * 0.7, 4, 0, 0, Math.PI * 2);
        ctx.fill();

        // Body bounce
        const bounce = this.moving ? Math.sin(this.animTimer * 0.5) * 2 : 0;
        const by = y + bounce;

        // Body
        ctx.fillStyle = this.color;
        ctx.beginPath();
        ctx.roundRect(x + 6, by + 12, s - 12, s - 16, 4);
        ctx.fill();

        // Head
        const headColor = this._lighten(this.color, 30);
        ctx.fillStyle = headColor;
        ctx.beginPath();
        ctx.arc(x + hs, by + 10, 10, 0, Math.PI * 2);
        ctx.fill();

        // Eyes
        ctx.fillStyle = '#fff';
        let ex1, ey1, ex2, ey2;
        switch (this.direction) {
            case 'down':  ex1 = x+13; ey1 = by+9;  ex2 = x+19; ey2 = by+9; break;
            case 'up':    ex1 = x+13; ey1 = by+7;  ex2 = x+19; ey2 = by+7; break;
            case 'left':  ex1 = x+10; ey1 = by+8;  ex2 = x+16; ey2 = by+8; break;
            case 'right': ex1 = x+16; ey1 = by+8;  ex2 = x+22; ey2 = by+8; break;
        }
        ctx.beginPath(); ctx.arc(ex1, ey1, 2.5, 0, Math.PI * 2); ctx.fill();
        ctx.beginPath(); ctx.arc(ex2, ey2, 2.5, 0, Math.PI * 2); ctx.fill();
        // Pupils
        ctx.fillStyle = '#222';
        ctx.beginPath(); ctx.arc(ex1, ey1 + 0.5, 1.2, 0, Math.PI * 2); ctx.fill();
        ctx.beginPath(); ctx.arc(ex2, ey2 + 0.5, 1.2, 0, Math.PI * 2); ctx.fill();

        // Legs (walking animation)
        ctx.fillStyle = this._darken(this.color, 30);
        if (this.moving) {
            const legOff = Math.sin(this.animTimer * 0.6) * 3;
            ctx.fillRect(x + 10, by + s - 8, 5, 8 + legOff);
            ctx.fillRect(x + s - 15, by + s - 8, 5, 8 - legOff);
        } else {
            ctx.fillRect(x + 10, by + s - 8, 5, 8);
            ctx.fillRect(x + s - 15, by + s - 8, 5, 8);
        }

        // Name tag
        ctx.fillStyle = 'rgba(0,0,0,0.6)';
        ctx.font = 'bold 10px Inter';
        ctx.textAlign = 'center';
        const nameW = ctx.measureText(this.name).width + 10;
        ctx.beginPath();
        ctx.roundRect(x + hs - nameW/2, by - 10, nameW, 14, 4);
        ctx.fill();
        ctx.fillStyle = '#fff';
        ctx.fillText(this.name, x + hs, by);
    }

    _lighten(hex, pct) {
        const num = parseInt(hex.replace('#', ''), 16);
        const r = Math.min(255, (num >> 16) + pct);
        const g = Math.min(255, ((num >> 8) & 0xff) + pct);
        const b = Math.min(255, (num & 0xff) + pct);
        return `rgb(${r},${g},${b})`;
    }

    _darken(hex, pct) {
        const num = parseInt(hex.replace('#', ''), 16);
        const r = Math.max(0, (num >> 16) - pct);
        const g = Math.max(0, ((num >> 8) & 0xff) - pct);
        const b = Math.max(0, (num & 0xff) - pct);
        return `rgb(${r},${g},${b})`;
    }
}
