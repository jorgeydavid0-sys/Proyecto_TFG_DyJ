/**
 * UI — Interaction panels, chat, HUD, and management dashboards
 * Escuela Interactiva 2D
 */
class UI {
    constructor() {
        this.panelOpen = false;
        this.chatOpen = false;
        this.dashboardOpen = false;
        this.network = null;
        this.user = null;
        this.activeTab = '';

        this.els = {
            zoneName:      document.getElementById('zone-name'),
            playerCount:   document.getElementById('player-count'),
            interactHint:  document.getElementById('interact-hint'),
            interactLabel: document.getElementById('interact-label'),
            panel:         document.getElementById('interaction-panel'),
            panelTitle:    document.getElementById('panel-title'),
            panelContent:  document.getElementById('panel-content'),
            chatPanel:     document.getElementById('chat-panel'),
            chatMessages:  document.getElementById('chat-messages'),
            chatInput:     document.getElementById('chat-input'),
            transition:    document.getElementById('transition-overlay'),
            // Elementos nuevos
            toastContainer:document.getElementById('toast-container'),
            dashboard:     document.getElementById('management-dashboard'),
            dbTitleText:   document.getElementById('dashboard-title-text'),
            dbSidebar:     document.getElementById('dashboard-sidebar'),
            dbContent:     document.getElementById('dashboard-tab-content'),
            hudUserIcon:   document.getElementById('hud-user-icon'),
            hudUserName:   document.getElementById('hud-user-name'),
            hudUserRole:   document.getElementById('hud-user-role'),
        };
    }

    setZoneName(name) {
        const icons = { 
            'Entrada Exterior': '🏫', 
            'Recepción': '🏛️', 
            'Conserjería': '🔑',
            'Aula de Informática': '💻',
            'Aula Principal': '📚',
            'Comedor': '🍽️',
            'Biblioteca': '📖'
        };
        this.els.zoneName.textContent = (icons[name] || '📍') + ' ' + name;
    }

    setPlayerCount(n) { 
        this.els.playerCount.textContent = n; 
    }

    showInteractHint(label) {
        this.els.interactLabel.textContent = label;
        this.els.interactHint.classList.remove('hidden');
    }

    hideInteractHint() { 
        this.els.interactHint.classList.add('hidden'); 
    }

    // Modal interactivo (se abre al pulsar E en el mapa)
    showPanel(type, data) {
        this.panelOpen = true;
        const titles = {
            tablon: '📋 Tablón de Anuncios',
            horario: '📅 Horario Semanal',
            menu_comedor: '🍽️ Menú del Comedor',
            notas: '📝 Notas / Calificaciones',
        };
        this.els.panelTitle.textContent = titles[type] || type;

        let html = '';
        switch (type) {
            case 'tablon': html = this._renderTablon(data); break;
            case 'horario': html = this._renderHorario(data); break;
            case 'menu_comedor': html = this._renderMenu(data); break;
            case 'notas': html = this._renderNotas(data); break;
        }
        this.els.panelContent.innerHTML = html;
        this.els.panel.classList.remove('hidden');
    }

    closePanel() {
        this.panelOpen = false;
        this.els.panel.classList.add('hidden');
    }

    toggleChat() {
        this.chatOpen = !this.chatOpen;
        this.els.chatPanel.classList.toggle('hidden', !this.chatOpen);
        if (this.chatOpen) this.els.chatInput.focus();
    }

    closeChat() {
        this.chatOpen = false;
        this.els.chatPanel.classList.add('hidden');
    }

    addChatMessage(name, message, color) {
        const div = document.createElement('div');
        div.className = 'chat-msg';
        div.innerHTML = `<span class="chat-name" style="color:${color || '#4A90D9'}">${this._esc(name)}:</span><span class="chat-text">${this._esc(message)}</span>`;
        this.els.chatMessages.appendChild(div);
        this.els.chatMessages.scrollTop = this.els.chatMessages.scrollHeight;
    }

    async playTransition(callback) {
        this.els.transition.classList.remove('hidden');
        this.els.transition.classList.add('active');
        await this._wait(400);
        if (callback) callback();
        await this._wait(300);
        this.els.transition.classList.remove('active');
        await this._wait(400);
        this.els.transition.classList.add('hidden');
    }

    // ─── TOAST NOTIFICATIONS ─────────────────────────────────────────

    showToast(message, type = 'info') {
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        const icons = { success: '✅', error: '❌', warning: '⚠️', info: 'ℹ️' };
        toast.innerHTML = `<span class="toast-icon">${icons[type] || 'ℹ️'}</span><span class="toast-message">${message}</span>`;
        
        this.els.toastContainer.appendChild(toast);
        
        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateX(50px)';
            toast.style.transition = 'all 0.3s ease';
            setTimeout(() => toast.remove(), 300);
        }, 4000);
    }

    // ─── GESTIÓN DE DASHBOARDS (RBAC) ───────────────────────────────

    setupDashboard(user, network) {
        this.user = user;
        this.network = network;

        // Rellenar HUD de perfil
        this.els.hudUserName.textContent = user.nombre;
        this.els.hudUserRole.textContent = user.rol;
        this.els.hudUserRole.className = `role-badge badge-${user.rol}`;
        this.els.hudUserIcon.textContent = user.rol === 'admin' ? '👑' : user.rol === 'profesor' ? '🎓' : '👤';

        // Mostrar u ocultar botón de gestión en el HUD
        const btnToggle = document.getElementById('btn-dashboard-toggle');
        if (user.rol === 'profesor' || user.rol === 'admin') {
            btnToggle.classList.remove('hidden');
        } else {
            btnToggle.classList.add('hidden');
        }
    }

    openDashboard() {
        if (!this.user || (this.user.rol !== 'profesor' && this.user.rol !== 'admin')) return;
        
        this.dashboardOpen = true;
        this.els.dashboard.classList.remove('hidden');
        
        // Crear pestañas según rol
        let tabs = [];
        if (this.user.rol === 'profesor') {
            tabs = [
                { id: 'notas', label: '📝 Calificaciones', icon: '📝' },
                { id: 'anuncios', label: '📢 Tablón de Anuncios', icon: '📢' }
            ];
            this.els.dbTitleText.textContent = 'Panel de Control Docente';
        } else if (this.user.rol === 'admin') {
            tabs = [
                { id: 'usuarios', label: '👥 Control de Usuarios', icon: '👥' },
                { id: 'comedor', label: '🍽️ Menú del Comedor', icon: '🍽' },
                { id: 'horarios', label: '📅 Gestión de Horarios', icon: '📅' },
                { id: 'notas', label: '📝 Calificaciones', icon: '📝' },
                { id: 'anuncios', label: '📢 Tablón de Anuncios', icon: '📢' }
            ];
            this.els.dbTitleText.textContent = 'Panel de Administración Escolar';
        }

        // Renderizar barra lateral
        this.els.dbSidebar.innerHTML = tabs.map(t => `
            <button class="db-tab-btn" data-tab="${t.id}">
                <span>${t.icon}</span>
                <span>${t.label}</span>
            </button>
        `).join('');

        // Añadir escuchadores a las pestañas de la barra lateral
        const tabBtns = this.els.dbSidebar.querySelectorAll('.db-tab-btn');
        tabBtns.forEach(btn => {
            btn.addEventListener('click', () => {
                tabBtns.forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                this.loadDashboardTab(btn.dataset.tab);
            });
        });

        // Activar la primera pestaña por defecto
        if (tabs.length > 0) {
            tabBtns[0].click();
        }
    }

    closeDashboard() {
        this.dashboardOpen = false;
        this.els.dashboard.classList.add('hidden');
    }

    async loadDashboardTab(tabId) {
        this.activeTab = tabId;
        this.els.dbContent.innerHTML = '<div style="color:var(--text-muted)">Cargando datos...</div>';
        
        try {
            switch(tabId) {
                case 'notas':
                    await this._loadNotasTab();
                    break;
                case 'anuncios':
                    await this._loadAnunciosTab();
                    break;
                case 'usuarios':
                    await this._loadUsuariosTab();
                    break;
                case 'horarios':
                    await this._loadHorariosTab();
                    break;
                case 'comedor':
                    await this._loadComedorTab();
                    break;
            }
        } catch(err) {
            this.showToast(err.message, 'error');
            this.els.dbContent.innerHTML = `<div style="color:var(--danger)">Error al cargar pestaña: ${err.message}</div>`;
        }
    }

    // ─── TEMPLATES DE PESTAÑAS (CRUD DOCENTE / ADMIN) ────────────────

    // 1. Pestaña de Calificaciones (Profesor / Admin)
    async _loadNotasTab() {
        const notas = await this.network.getNotas();
        const alumnos = await this.network.getUsuarios(); // Para el selector de añadir nota
        
        const alumnosOptions = alumnos
            .filter(a => a.rol === 'alumno')
            .map(a => `<option value="${a.id}">${a.nombre} (ID: ${a.id})</option>`)
            .join('');

        let html = `
            <div class="content-header-row">
                <h3>Gestión de Calificaciones Académicas</h3>
            </div>
            
            <div class="db-form-box">
                <div class="db-form-title">➕ Registrar Nueva Calificación</div>
                <form id="db-form-nota">
                    <div class="form-grid">
                        <div class="input-group">
                            <label for="nota-alumno">Alumno</label>
                            <select id="nota-alumno" required class="select-role">
                                ${alumnosOptions || '<option value="" disabled>No hay alumnos registrados</option>'}
                            </select>
                        </div>
                        <div class="input-group">
                            <label for="nota-materia">Materia / Asignatura</label>
                            <input type="text" id="nota-materia" placeholder="Ej. Matemáticas" required autocomplete="off">
                        </div>
                        <div class="input-group">
                            <label for="nota-valor">Calificación (0 - 10)</label>
                            <input type="number" id="nota-valor" min="0" max="10" step="0.1" placeholder="Ej. 8.5" required>
                        </div>
                        <div class="input-group">
                            <label for="nota-tipo">Tipo de Evaluación</label>
                            <select id="nota-tipo" class="select-role">
                                <option value="Examen">Examen</option>
                                <option value="Proyecto">Proyecto</option>
                                <option value="Tarea">Tarea</option>
                                <option value="Participación">Participación</option>
                            </select>
                        </div>
                        <div class="input-group form-group-full">
                            <label for="nota-comentario">Comentario / Feedback</label>
                            <input type="text" id="nota-comentario" placeholder="Detalles de la calificación..." autocomplete="off">
                        </div>
                    </div>
                    <div class="form-actions">
                        <button type="submit" class="btn-primary">Registrar Nota</button>
                    </div>
                </form>
            </div>

            <div class="db-table-container">
                <table class="db-table">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Alumno</th>
                            <th>Materia</th>
                            <th>Nota</th>
                            <th>Tipo</th>
                            <th>Comentario</th>
                            <th>Fecha</th>
                            <th>Acciones</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${notas.map(n => `
                            <tr>
                                <td>${n.id}</td>
                                <td><b style="color:white">${this._esc(n.nombre_alumno || ('Alumno ' + n.id_alumno))}</b></td>
                                <td>${this._esc(n.materia)}</td>
                                <td>
                                    <span class="role-badge" style="background:${parseFloat(n.calificacion) >= 5 ? 'rgba(46, 204, 113, 0.2)' : 'rgba(231, 76, 60, 0.2)'}; color:${parseFloat(n.calificacion) >= 5 ? 'var(--success)' : 'var(--danger)'}; border: 1px solid">
                                        ${parseFloat(n.calificacion).toFixed(1)}
                                    </span>
                                </td>
                                <td>${this._esc(n.tipo)}</td>
                                <td>${this._esc(n.comentario || '—')}</td>
                                <td>${this._formatDate(n.fecha)}</td>
                                <td>
                                    <button class="btn-action btn-delete" data-id="${n.id}">Eliminar</button>
                                </td>
                            </tr>
                        `).join('') || '<tr><td colspan="8" style="text-align:center">No hay calificaciones registradas.</td></tr>'}
                    </tbody>
                </table>
            </div>
        `;

        this.els.dbContent.innerHTML = html;

        // Escuchador del formulario
        const form = document.getElementById('db-form-nota');
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            const id_alumno = parseInt(document.getElementById('nota-alumno').value);
            const materia = document.getElementById('nota-materia').value;
            const calificacion = parseFloat(document.getElementById('nota-valor').value);
            const tipo = document.getElementById('nota-tipo').value;
            const comentario = document.getElementById('nota-comentario').value;

            try {
                await this.network.createNota(id_alumno, materia, calificacion, tipo, comentario);
                this.showToast('Calificación registrada con éxito', 'success');
                this.loadDashboardTab('notas');
            } catch(err) {
                this.showToast(err.message, 'error');
            }
        });

        // Escuchadores de eliminación
        this.els.dbContent.querySelectorAll('.btn-delete').forEach(btn => {
            btn.addEventListener('click', async () => {
                const id = btn.dataset.id;
                if (confirm('¿Estás seguro de eliminar esta calificación?')) {
                    try {
                        await this.network.deleteNota(id);
                        this.showToast('Calificación eliminada', 'success');
                        this.loadDashboardTab('notas');
                    } catch(err) {
                        this.showToast(err.message, 'error');
                    }
                }
            });
        });
    }

    // 2. Pestaña de Anuncios (Profesor / Admin)
    async _loadAnunciosTab() {
        const anuncios = await this.network.getAnuncios();
        
        let html = `
            <div class="content-header-row">
                <h3>Tablón de Anuncios Escolares</h3>
            </div>
            
            <div class="db-form-box">
                <div class="db-form-title">➕ Publicar Nuevo Anuncio</div>
                <form id="db-form-anuncio">
                    <div class="form-grid">
                        <div class="input-group">
                            <label for="anuncio-titulo">Título del Anuncio</label>
                            <input type="text" id="anuncio-titulo" placeholder="Ej. Reunión de delegados" required autocomplete="off">
                        </div>
                        <div class="input-group">
                            <label for="anuncio-importante">Importancia</label>
                            <select id="anuncio-importante" class="select-role">
                                <option value="false">Normal</option>
                                <option value="true">⚠️ Importante (Destacado)</option>
                            </select>
                        </div>
                        <div class="input-group form-group-full">
                            <label for="anuncio-contenido">Contenido / Mensaje</label>
                            <textarea id="anuncio-contenido" placeholder="Escribe el cuerpo del anuncio..." required style="width:100%; min-height:80px; background:var(--bg-glass-light); border:1px solid var(--border); border-radius:var(--radius-sm); color:white; padding:12px; font-family:var(--font); outline:none"></textarea>
                        </div>
                    </div>
                    <div class="form-actions">
                        <button type="submit" class="btn-primary">Publicar Anuncio</button>
                    </div>
                </form>
            </div>

            <div class="db-table-container">
                <table class="db-table">
                    <thead>
                        <tr>
                            <th>Título</th>
                            <th>Contenido</th>
                            <th>Autor</th>
                            <th>Tipo</th>
                            <th>Fecha</th>
                            <th>Acciones</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${anuncios.map(a => `
                            <tr>
                                <td><b style="color:white">${this._esc(a.titulo)}</b></td>
                                <td style="max-width:300px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap">${this._esc(a.contenido)}</td>
                                <td>${this._esc(a.autor)}</td>
                                <td>
                                    ${a.importante 
                                        ? '<span class="role-badge" style="background:rgba(231, 76, 60,0.15); color:var(--danger); border:1px solid var(--danger)">Importante</span>' 
                                        : '<span class="role-badge" style="background:rgba(255,255,255,0.05); color:var(--text-muted); border:1px solid var(--border)">Normal</span>'}
                                </td>
                                <td>${this._formatDate(a.fecha)}</td>
                                <td>
                                    <button class="btn-action btn-delete" data-id="${a.id}">Eliminar</button>
                                </td>
                            </tr>
                        `).join('') || '<tr><td colspan="6" style="text-align:center">No hay anuncios publicados.</td></tr>'}
                    </tbody>
                </table>
            </div>
        `;

        this.els.dbContent.innerHTML = html;

        // Envío de formulario
        const form = document.getElementById('db-form-anuncio');
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            const titulo = document.getElementById('anuncio-titulo').value;
            const importante = document.getElementById('anuncio-importante').value === 'true';
            const contenido = document.getElementById('anuncio-contenido').value;

            try {
                await this.network.createAnuncio(titulo, contenido, importante);
                this.showToast('Anuncio publicado', 'success');
                this.loadDashboardTab('anuncios');
            } catch(err) {
                this.showToast(err.message, 'error');
            }
        });

        // Eliminar
        this.els.dbContent.querySelectorAll('.btn-delete').forEach(btn => {
            btn.addEventListener('click', async () => {
                const id = btn.dataset.id;
                if (confirm('¿Deseas eliminar este anuncio?')) {
                    try {
                        await this.network.deleteAnuncio(id);
                        this.showToast('Anuncio eliminado', 'success');
                        this.loadDashboardTab('anuncios');
                    } catch(err) {
                        this.showToast(err.message, 'error');
                    }
                }
            });
        });
    }

    // 3. Pestaña de Usuarios (Solo Admin)
    async _loadUsuariosTab() {
        const usuarios = await this.network.getUsuarios();
        
        let html = `
            <div class="content-header-row">
                <h3>Control de Cuentas y Roles de Usuario</h3>
            </div>
            
            <div class="db-table-container">
                <table class="db-table">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Nombre de Usuario</th>
                            <th>Email</th>
                            <th>Rol Escolar</th>
                            <th>Acciones</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${usuarios.map(u => `
                            <tr>
                                <td>${u.id}</td>
                                <td><b style="color:white">${this._esc(u.nombre)}</b></td>
                                <td>${this._esc(u.email || '—')}</td>
                                <td>
                                    <select class="select-role-edit select-role" data-id="${u.id}" style="padding:6px; font-size:0.75rem; width:130px; height:auto">
                                        <option value="alumno" ${u.rol === 'alumno' ? 'selected' : ''}>Alumno</option>
                                        <option value="profesor" ${u.rol === 'profesor' ? 'selected' : ''}>Profesor</option>
                                        <option value="admin" ${u.rol === 'admin' ? 'selected' : ''}>Administrador</option>
                                    </select>
                                </td>
                                <td>
                                    <button class="btn-action btn-edit btn-save-role" data-id="${u.id}">Guardar Rol</button>
                                    <button class="btn-action btn-delete" data-id="${u.id}" ${u.id === this.user.id ? 'disabled title="No puedes eliminarte a ti mismo"' : ''}>Eliminar</button>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;

        this.els.dbContent.innerHTML = html;

        // Escuchador Guardar Rol
        this.els.dbContent.querySelectorAll('.btn-save-role').forEach(btn => {
            btn.addEventListener('click', async () => {
                const id = btn.dataset.id;
                const select = this.els.dbContent.querySelector(`select[data-id="${id}"]`);
                const nuevo_rol = select.value;
                
                try {
                    await this.network.updateUsuarioRol(id, nuevo_rol);
                    this.showToast('Rol de usuario actualizado', 'success');
                    this.loadDashboardTab('usuarios');
                } catch(err) {
                    this.showToast(err.message, 'error');
                }
            });
        });

        // Eliminar Usuario
        this.els.dbContent.querySelectorAll('.btn-delete').forEach(btn => {
            btn.addEventListener('click', async () => {
                const id = btn.dataset.id;
                if (confirm('¿Estás seguro de eliminar por completo este usuario? Esto borrará también sus notas y horarios asociados.')) {
                    try {
                        await this.network.deleteUsuario(id);
                        this.showToast('Usuario eliminado', 'success');
                        this.loadDashboardTab('usuarios');
                    } catch(err) {
                        this.showToast(err.message, 'error');
                    }
                }
            });
        });
    }

    // 4. Pestaña de Horarios (Solo Admin)
    async _loadHorariosTab() {
        const horarios = await this.network.getAllHorarios();
        const alumnos = await this.network.getUsuarios();
        
        const alumnosOptions = alumnos
            .filter(a => a.rol === 'alumno')
            .map(a => `<option value="${a.id}">${a.nombre} (ID: ${a.id})</option>`)
            .join('');

        let html = `
            <div class="content-header-row">
                <h3>Planificación y Horarios Semanales</h3>
            </div>
            
            <div class="db-form-box">
                <div class="db-form-title">➕ Asignar Nueva Clase / Slot de Horario</div>
                <form id="db-form-horario">
                    <div class="form-grid">
                        <div class="input-group">
                            <label for="horario-alumno">Asignar a Alumno</label>
                            <select id="horario-alumno" required class="select-role">
                                ${alumnosOptions || '<option value="" disabled>No hay alumnos registrados</option>'}
                            </select>
                        </div>
                        <div class="input-group">
                            <label for="horario-dia">Día de la Semana</label>
                            <select id="horario-dia" class="select-role">
                                <option value="Lunes">Lunes</option>
                                <option value="Martes">Martes</option>
                                <option value="Miércoles">Miércoles</option>
                                <option value="Jueves">Jueves</option>
                                <option value="Viernes">Viernes</option>
                            </select>
                        </div>
                        <div class="input-group">
                            <label for="horario-inicio">Hora Inicio</label>
                            <input type="time" id="horario-inicio" required>
                        </div>
                        <div class="input-group">
                            <label for="horario-fin">Hora Fin</label>
                            <input type="time" id="horario-fin" required>
                        </div>
                        <div class="input-group">
                            <label for="horario-materia">Asignatura</label>
                            <input type="text" id="horario-materia" placeholder="Ej. Programación Móvil" required autocomplete="off">
                        </div>
                        <div class="input-group">
                            <label for="horario-aula">Aula / Clase</label>
                            <input type="text" id="horario-aula" placeholder="Ej. Aula 102" autocomplete="off">
                        </div>
                    </div>
                    <div class="form-actions">
                        <button type="submit" class="btn-primary">Asignar Slot</button>
                    </div>
                </form>
            </div>

            <div class="db-table-container">
                <table class="db-table">
                    <thead>
                        <tr>
                            <th>Alumno</th>
                            <th>Día</th>
                            <th>Hora</th>
                            <th>Asignatura</th>
                            <th>Aula</th>
                            <th>Acciones</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${horarios.map(h => `
                            <tr>
                                <td><b style="color:white">${this._esc(h.nombre_alumno || ('Alumno ' + h.id_alumno))}</b></td>
                                <td>${this._esc(h.dia)}</td>
                                <td>${h.hora_inicio.slice(0,5)} - ${h.hora_fin.slice(0,5)}</td>
                                <td>${this._esc(h.asignatura)}</td>
                                <td>${this._esc(h.aula || '—')}</td>
                                <td>
                                    <button class="btn-action btn-delete" data-id="${h.id}">Eliminar</button>
                                </td>
                            </tr>
                        `).join('') || '<tr><td colspan="6" style="text-align:center">No hay clases programadas.</td></tr>'}
                    </tbody>
                </table>
            </div>
        `;

        this.els.dbContent.innerHTML = html;

        // Registrar
        const form = document.getElementById('db-form-horario');
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            const id_alumno = parseInt(document.getElementById('horario-alumno').value);
            const dia = document.getElementById('horario-dia').value;
            const hora_inicio = document.getElementById('horario-inicio').value;
            const hora_fin = document.getElementById('horario-fin').value;
            const asignatura = document.getElementById('horario-materia').value;
            const aula = document.getElementById('horario-aula').value;

            try {
                await this.network.createHorario(id_alumno, dia, hora_inicio, hora_fin, asignatura, aula);
                this.showToast('Clase asignada con éxito', 'success');
                this.loadDashboardTab('horarios');
            } catch(err) {
                this.showToast(err.message, 'error');
            }
        });

        // Eliminar
        this.els.dbContent.querySelectorAll('.btn-delete').forEach(btn => {
            btn.addEventListener('click', async () => {
                const id = btn.dataset.id;
                if (confirm('¿Eliminar este bloque horario?')) {
                    try {
                        await this.network.deleteHorario(id);
                        this.showToast('Bloque eliminado', 'success');
                        this.loadDashboardTab('horarios');
                    } catch(err) {
                        this.showToast(err.message, 'error');
                    }
                }
            });
        });
    }

    // 5. Pestaña de Menú Comedor (Solo Admin)
    async _loadComedorTab() {
        const comedor = await this.network.getMenuComedor();
        
        let html = `
            <div class="content-header-row">
                <h3>Programación del Menú del Comedor</h3>
            </div>
            
            <div class="db-form-box">
                <div class="db-form-title">➕ Publicar o Modificar Menú Escolar</div>
                <form id="db-form-comedor">
                    <div class="form-grid">
                        <div class="input-group">
                            <label for="menu-fecha">Fecha del Menú</label>
                            <input type="date" id="menu-fecha" required>
                        </div>
                        <div class="input-group">
                            <label for="menu-primero">Primer Plato</label>
                            <input type="text" id="menu-primero" placeholder="Ej. Crema de verduras" required autocomplete="off">
                        </div>
                        <div class="input-group">
                            <label for="menu-segundo">Segundo Plato</label>
                            <input type="text" id="menu-segundo" placeholder="Ej. Pechuga de pollo a la plancha" required autocomplete="off">
                        </div>
                        <div class="input-group">
                            <label for="menu-postre">Postre</label>
                            <input type="text" id="menu-postre" placeholder="Ej. Fruta de temporada" required autocomplete="off">
                        </div>
                        <div class="input-group form-group-full">
                            <label for="menu-alergenos">Alérgenos (Opcional)</label>
                            <input type="text" id="menu-alergenos" placeholder="Ej. Glúten, Lactosa, Frutos secos..." autocomplete="off">
                        </div>
                    </div>
                    <div class="form-actions">
                        <button type="submit" class="btn-primary">Registrar Menú</button>
                    </div>
                </form>
            </div>

            <div class="db-table-container">
                <table class="db-table">
                    <thead>
                        <tr>
                            <th>Fecha</th>
                            <th>Primero</th>
                            <th>Segundo</th>
                            <th>Postre</th>
                            <th>Alérgenos</th>
                            <th>Acciones</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${comedor.map(m => `
                            <tr>
                                <td><b style="color:white">${this._formatDate(m.fecha)}</b></td>
                                <td>${this._esc(m.primer_plato)}</td>
                                <td>${this._esc(m.segundo_plato)}</td>
                                <td>${this._esc(m.postre)}</td>
                                <td><span style="color:var(--warning)">${this._esc(m.alergenos || 'Ninguno')}</span></td>
                                <td>
                                    <button class="btn-action btn-delete" data-id="${m.id}">Eliminar</button>
                                </td>
                            </tr>
                        `).join('') || '<tr><td colspan="6" style="text-align:center">No hay menús registrados.</td></tr>'}
                    </tbody>
                </table>
            </div>
        `;

        this.els.dbContent.innerHTML = html;

        // Establecer la fecha de hoy por defecto en el selector
        document.getElementById('menu-fecha').value = new Date().toISOString().split('T')[0];

        // Guardar
        const form = document.getElementById('db-form-comedor');
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            const fecha = document.getElementById('menu-fecha').value;
            const primer_plato = document.getElementById('menu-primero').value;
            const segundo_plato = document.getElementById('menu-segundo').value;
            const postre = document.getElementById('menu-postre').value;
            const alergenos = document.getElementById('menu-alergenos').value;

            try {
                await this.network.createOrUpdateMenu(fecha, primer_plato, segundo_plato, postre, alergenos);
                this.showToast('Menú del comedor registrado', 'success');
                this.loadDashboardTab('comedor');
            } catch(err) {
                this.showToast(err.message, 'error');
            }
        });

        // Eliminar
        this.els.dbContent.querySelectorAll('.btn-delete').forEach(btn => {
            btn.addEventListener('click', async () => {
                const id = btn.dataset.id;
                if (confirm('¿Estás seguro de eliminar este menú?')) {
                    try {
                        await this.network.deleteMenu(id);
                        this.showToast('Menú eliminado con éxito', 'success');
                        this.loadDashboardTab('comedor');
                    } catch(err) {
                        this.showToast(err.message, 'error');
                    }
                }
            });
        });
    }

    // ─── RENDERS TRADICIONALES EN MAPA (Mantiene compatibilidad completa) ───

    _renderTablon(data) {
        if (!data || !data.length) return '<p style="color:var(--text-dim)">No hay anuncios.</p>';
        return data.map(a => `
            <div class="announcement-card ${a.importante ? 'important' : ''}">
                <div class="announcement-title">${this._esc(a.titulo)}</div>
                <div class="announcement-body">${this._esc(a.contenido)}</div>
                <div class="announcement-meta">
                    <span>👤 ${this._esc(a.autor)}</span>
                    <span>📅 ${this._formatDate(a.fecha)}</span>
                </div>
            </div>
        `).join('');
    }

    _renderHorario(data) {
        if (!data || !data.length) return '<p style="color:var(--text-dim)">No hay horario registrado.</p>';
        let html = '<table class="schedule-table"><thead><tr><th>Día</th><th>Hora</th><th>Asignatura</th><th>Aula</th></tr></thead><tbody>';
        let lastDay = '';
        data.forEach(h => {
            const day = h.dia !== lastDay ? `<span class="schedule-day">${this._esc(h.dia)}</span>` : '';
            lastDay = h.dia;
            const start = typeof h.hora_inicio === 'string' ? h.hora_inicio.slice(0,5) : h.hora_inicio;
            const end = typeof h.hora_fin === 'string' ? h.hora_fin.slice(0,5) : h.hora_fin;
            html += `<tr><td>${day}</td><td>${start} - ${end}</td><td>${this._esc(h.asignatura)}</td><td>${this._esc(h.aula || '')}</td></tr>`;
        });
        html += '</tbody></table>';
        return html;
    }

    _renderMenu(data) {
        if (!data || !data.length) return '<p style="color:var(--text-dim)">No hay menú disponible.</p>';
        return data.map(m => `
            <div class="menu-day-card">
                <div class="menu-date">📅 ${this._formatDate(m.fecha)}</div>
                <div class="menu-item"><span class="menu-item-label">🥣 Primero:</span><span>${this._esc(m.primer_plato)}</span></div>
                <div class="menu-item"><span class="menu-item-label">🥩 Segundo:</span><span>${this._esc(m.segundo_plato)}</span></div>
                <div class="menu-item"><span class="menu-item-label">🍰 Postre:</span><span>${this._esc(m.postre)}</span></div>
                ${m.alergenos ? `<div class="menu-allergens">⚠️ Alérgenos: ${this._esc(m.alergenos)}</div>` : ''}
            </div>
        `).join('');
    }

    _renderNotas(data) {
        if (!data || !data.length) return '<p style="color:var(--text-dim)">No hay calificaciones registradas para ti.</p>';
        return data.map(n => {
            const score = parseFloat(n.calificacion);
            const cls = score >= 8.5 ? 'high' : score >= 5 ? 'medium' : 'low';
            return `
                <div class="grade-card">
                    <div class="grade-score ${cls}">${score.toFixed(1)}</div>
                    <div class="grade-info">
                        <div class="grade-subject">${this._esc(n.materia)}</div>
                        <div class="grade-detail">${this._esc(n.tipo)} · ${this._esc(n.comentario || '')} · ${this._formatDate(n.fecha)}</div>
                    </div>
                </div>
            `;
        }).join('');
    }

    _esc(s) { 
        const d = document.createElement('div'); 
        d.textContent = s || ''; 
        return d.innerHTML; 
    }
    
    _formatDate(d) {
        if (!d) return '';
        try { 
            return new Date(d).toLocaleDateString('es-ES', { day: 'numeric', month: 'short', year: 'numeric' }); 
        } catch { 
            return String(d); 
        }
    }
    
    _wait(ms) { 
        return new Promise(r => setTimeout(r, ms)); 
    }
}
