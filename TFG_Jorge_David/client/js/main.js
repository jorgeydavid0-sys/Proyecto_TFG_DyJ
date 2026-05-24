/**
 * ═══════════════════════════════════════════════════════════════════════
 *   Main.js — Punto de Entrada y Control de Autenticación
 *   Escuela Interactiva 2D | TFG Jorge González — DAM 2º
 *
 *   Gestiona el flujo de login/registro, partículas estéticas,
 *   auto-login con JWT, persistencia de sesión y arranque de Phaser.
 * ═══════════════════════════════════════════════════════════════════════
 */

(function () {
    'use strict';

    const network = new Network();
    let selectedColor = '#4A90D9';
    let phaserGame = null;

    // ─── DOM Elements ────────────────────────────────────────────
    const loginScreen  = document.getElementById('login-screen');
    const gameScreen   = document.getElementById('game-screen');
    const tabLogin     = document.getElementById('tab-login');
    const tabRegister  = document.getElementById('tab-register');
    const formLogin    = document.getElementById('form-login');
    const formRegister = document.getElementById('form-register');
    const colorPicker  = document.getElementById('color-picker');

    // ─── Partículas del Menú de Login ─────────────────────────────
    function initLoginParticles() {
        const particlesEl = document.getElementById('particles');
        if (!particlesEl) return;
        particlesEl.innerHTML = '';
        for (let i = 0; i < 40; i++) {
            const p = document.createElement('div');
            p.style.cssText = `
                position: absolute;
                width: ${2 + Math.random() * 4}px;
                height: ${2 + Math.random() * 4}px;
                background: rgba(74, 144, 217, ${0.1 + Math.random() * 0.2});
                border-radius: 50%;
                left: ${Math.random() * 100}%;
                top: ${Math.random() * 100}%;
                animation: floatUp ${5 + Math.random() * 10}s linear infinite;
            `;
            particlesEl.appendChild(p);
        }
    }

    // Inyectar CSS de partículas si no existe
    if (!document.getElementById('particles-style')) {
        const style = document.createElement('style');
        style.id = 'particles-style';
        style.textContent = `
            @keyframes floatUp {
                0% { transform: translateY(0) translateX(0); opacity: 0; }
                10% { opacity: 1; }
                90% { opacity: 1; }
                100% { transform: translateY(-100vh) translateX(${Math.random() > 0.5 ? '40px' : '-40px'}); opacity: 0; }
            }
        `;
        document.head.appendChild(style);
    }

    // ─── Pestañas (Iniciar Sesión / Registrarse) ───────────────────
    window.switchTab = function (tab) {
        if (tab === 'login') {
            tabLogin.classList.add('active');
            tabRegister.classList.remove('active');
            formLogin.classList.remove('hidden');
            formRegister.classList.add('hidden');
        } else {
            tabRegister.classList.add('active');
            tabLogin.classList.remove('active');
            formRegister.classList.remove('hidden');
            formLogin.classList.add('hidden');
        }
    };

    // Color picker interactivo
    if (colorPicker) {
        const buttons = colorPicker.querySelectorAll('.color-btn');
        buttons.forEach(btn => {
            btn.addEventListener('click', () => {
                buttons.forEach(b => b.classList.remove('selected'));
                btn.classList.add('selected');
                selectedColor = btn.dataset.color;
            });
        });
    }

    // Mostrar notificaciones Toast
    function showToast(message, type = 'info') {
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        toast.textContent = message;
        document.body.appendChild(toast);
        setTimeout(() => toast.classList.add('show'), 10);
        setTimeout(() => {
            toast.classList.remove('show');
            setTimeout(() => toast.remove(), 300);
        }, 3000);
    }

    // ─── Arranque de Phaser.js ─────────────────────────────────────
    function startPhaserGame(user, token) {
        // Guardar en variable global para que MapScene la lea en init()
        window._playerData = {
            username: user.username,
            nombre: user.nombre,
            rol: user.rol,
            color_avatar: user.color_avatar || selectedColor,
            token: token,
            zone: 'entrada'
        };

        const config = {
            type: Phaser.AUTO,
            width: 960,
            height: 640,
            parent: 'phaser-container',
            scale: {
                mode: Phaser.Scale.FIT,
                autoCenter: Phaser.Scale.CENTER_BOTH
            },
            physics: {
                default: 'arcade',
                arcade: {
                    gravity: { y: 0 },
                    debug: false
                }
            },
            scene: [BootScene, MapScene]
        };

        if (phaserGame) {
            phaserGame.destroy(true);
        }
        phaserGame = new Phaser.Game(config);
    }

    // ─── Flujo de Entrada al Campus ───────────────────────────────
    function enterCampus(token, user) {
        // Guardar token localmente
        localStorage.setItem('token', token);

        // Cambiar pantallas
        loginScreen.classList.remove('active');
        gameScreen.classList.add('active');

        // Arrancar juego
        startPhaserGame(user, token);
        showToast(`¡Bienvenido al campus, ${user.nombre}!`, 'success');
    }

    // ─── Manejador de Login ────────────────────────────────────────
    window.handleLogin = async function (e) {
        e.preventDefault();
        const userVal = document.getElementById('login-username').value.trim();
        const passVal = document.getElementById('login-password').value;
        const errEl   = document.getElementById('login-error');
        const btn     = document.getElementById('btn-login');

        errEl.classList.add('hidden');
        btn.disabled = true;
        btn.innerHTML = '<span>⏳ Conectando...</span>';

        try {
            const data = await network.login(userVal, passVal);
            enterCampus(data.token, data.usuario);
        } catch (err) {
            errEl.textContent = err.message || 'Error al iniciar sesión.';
            errEl.classList.remove('hidden');
            btn.disabled = false;
            btn.innerHTML = '<span class="btn-icon">🎮</span><span>Entrar al Campus</span>';
        }
    };

    // ─── Manejador de Registro ─────────────────────────────────────
    window.handleRegister = async function (e) {
        e.preventDefault();
        const nombre    = document.getElementById('reg-nombre').value.trim();
        const apellidos = document.getElementById('reg-apellidos').value.trim();
        const username  = document.getElementById('reg-username').value.trim();
        const password  = document.getElementById('reg-password').value;
        const errEl     = document.getElementById('register-error');
        const btn       = document.getElementById('btn-register');

        errEl.classList.add('hidden');
        btn.disabled = true;
        btn.innerHTML = '<span>✨ Creando cuenta...</span>';

        try {
            const data = await network.register(username, password, nombre, apellidos, selectedColor);
            enterCampus(data.token, data.usuario);
        } catch (err) {
            errEl.textContent = err.message || 'Error al registrar usuario.';
            errEl.classList.remove('hidden');
            btn.disabled = false;
            btn.innerHTML = '<span class="btn-icon">✨</span><span>Crear Cuenta</span>';
        }
    };

    // ─── Auto-Login al recargar ────────────────────────────────────
    async function checkAutoLogin() {
        const token = localStorage.getItem('token');
        if (!token) return;

        try {
            const user = await network.getMe();
            enterCampus(token, user);
        } catch {
            // Token inválido o expirado
            localStorage.removeItem('token');
            showToast('Sesión caducada. Por favor, inicia sesión de nuevo.', 'warning');
        }
    }

    // ─── Cerrar Sesión y Controles HUD ─────────────────────────────
    document.addEventListener('DOMContentLoaded', () => {
        initLoginParticles();
        checkAutoLogin();

        // Botón Pantalla Completa
        const fsBtn = document.getElementById('btn-fullscreen');
        if (fsBtn) {
            fsBtn.addEventListener('click', () => {
                if (!document.fullscreenElement) {
                    document.documentElement.requestFullscreen().catch(() => {});
                } else {
                    document.exitFullscreen();
                }
            });
        }

        // Botón Salir / Desconectar
        const logoutBtn = document.getElementById('btn-logout');
        if (logoutBtn) {
            logoutBtn.addEventListener('click', () => {
                if (confirm('¿Deseas salir del campus y cerrar sesión?')) {
                    localStorage.removeItem('token');
                    window.location.reload(); // Recargar limpia el estado de socket y Phaser
                }
            });
        }
    });

})();
