/**
 * Network — WebSocket communication
 * Escuela Interactiva 2D
 */
class Network {
    constructor() {
        this.socket = null;
        this.connected = false;
        this.callbacks = {};
    }

    connect(serverUrl = window.location.origin) {
        return new Promise((resolve, reject) => {
            this.socket = io(serverUrl, {
                transports: ['websocket', 'polling'],
                reconnection: true,
                reconnectionAttempts: 5,
                reconnectionDelay: 2000,
            });
            this.socket.on('connect', () => { this.connected = true; resolve(); });
            this.socket.on('disconnect', () => { this.connected = false; this._trigger('disconnected'); });
            this.socket.on('connect_error', (err) => reject(err));

            const events = ['joined','player_joined','player_left','player_moved',
                          'players_update','zone_changed','interaction_data','chat_broadcast'];
            events.forEach(e => this.socket.on(e, (d) => this._trigger(e, d)));
        });
    }

    join(name, color, rol = 'alumno', user_id = null) { 
        this.socket.emit('join', { name, color, rol, user_id }); 
    }
    sendMove(x, y, direction) { if (this.connected) this.socket.emit('move', { x, y, direction }); }
    changeZone(targetZone) { this.socket.emit('change_zone', { target_zone: targetZone }); }
    interact(type) { this.socket.emit('interact', { type }); }
    sendChat(message) { if (this.connected && message.trim()) this.socket.emit('chat_message', { message: message.trim() }); }
    requestPlayers() { this.socket.emit('request_players'); }

    // ─── PETICIONES REST API (AUTENTICACIÓN Y CRUD) ──────────────────

    async _request(url, method = 'GET', data = null) {
        const token = localStorage.getItem('token');
        const options = {
            method,
            headers: {
                'Content-Type': 'application/json'
            }
        };
        if (token) {
            options.headers['Authorization'] = `Bearer ${token}`;
        }
        if (data) {
            options.body = JSON.stringify(data);
        }
        const res = await fetch(url, options);
        const json = await res.json();
        if (!res.ok) {
            throw new Error(json.error || 'Error en la petición');
        }
        return json;
    }

    // Autenticación
    async register(username, password, nombre, apellidos, color_avatar) {
        return this._request('/api/auth/register', 'POST', { username, password, nombre, apellidos, color_avatar });
    }

    async login(username, password) {
        return this._request('/api/auth/login', 'POST', { username, password });
    }

    async logout() {
        return this._request('/api/auth/logout', 'POST');
    }

    async getMe() {
        return this._request('/api/auth/me');
    }

    // Calificaciones (Notas)
    async getNotas() {
        return this._request('/api/notas');
    }

    async createNota(id_alumno, materia, calificacion, tipo, comentario) {
        return this._request('/api/notas', 'POST', { id_alumno, materia, calificacion, tipo, comentario });
    }

    async updateNota(id_nota, calificacion, tipo, comentario) {
        return this._request(`/api/notas/${id_nota}`, 'PUT', { calificacion, tipo, comentario });
    }

    async deleteNota(id_nota) {
        return this._request(`/api/notas/${id_nota}`, 'DELETE');
    }

    // Anuncios (Tablón)
    async getAnuncios() {
        return this._request('/api/anuncios');
    }

    async createAnuncio(titulo, contenido, importante) {
        return this._request('/api/anuncios', 'POST', { titulo, contenido, importante });
    }

    async updateAnuncio(id_anuncio, titulo, contenido, importante) {
        return this._request(`/api/anuncios/${id_anuncio}`, 'PUT', { titulo, contenido, importante });
    }

    async deleteAnuncio(id_anuncio) {
        return this._request(`/api/anuncios/${id_anuncio}`, 'DELETE');
    }

    // Usuarios (Admin)
    async getUsuarios() {
        return this._request('/api/usuarios');
    }

    async updateUsuarioRol(user_id, rol) {
        return this._request(`/api/usuarios/${user_id}`, 'PUT', { rol });
    }

    async deleteUsuario(user_id) {
        return this._request(`/api/usuarios/${user_id}`, 'DELETE');
    }

    // Horarios (CRUD)
    async getHorarios(id_alumno = '') {
        const query = id_alumno ? `?id_alumno=${id_alumno}` : '';
        return this._request(`/api/horarios${query}`);
    }

    async getAllHorarios() {
        return this._request('/api/horarios/todos');
    }

    async createHorario(id_alumno, dia, hora_inicio, hora_fin, asignatura, aula) {
        return this._request('/api/horarios', 'POST', { id_alumno, dia, hora_inicio, hora_fin, asignatura, aula });
    }

    async deleteHorario(id_horario) {
        return this._request(`/api/horarios/${id_horario}`, 'DELETE');
    }

    // Menú Comedor (CRUD)
    async getMenuComedor() {
        return this._request('/api/menu_comedor');
    }

    async createOrUpdateMenu(fecha, primer_plato, segundo_plato, postre, alergenos) {
        return this._request('/api/menu_comedor', 'POST', { fecha, primer_plato, segundo_plato, postre, alergenos });
    }

    async deleteMenu(id_menu) {
        return this._request(`/api/menu_comedor/${id_menu}`, 'DELETE');
    }

    // Callbacks
    on(event, callback) {
        if (!this.callbacks[event]) this.callbacks[event] = [];
        this.callbacks[event].push(callback);
    }

    _trigger(event, data) {
        if (this.callbacks[event]) this.callbacks[event].forEach(cb => cb(data));
    }
}
