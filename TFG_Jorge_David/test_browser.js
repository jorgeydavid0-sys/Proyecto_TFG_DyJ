/**
 * ═══════════════════════════════════════════════════════════════════════
 *   Script de Exploración Visual Automatizada (Puppeteer Browser Test)
 *   Escuela Interactiva 2D | TFG Jorge González — DAM 2º
 *
 *   Simula la navegación de un usuario real en el navegador:
 *     1. Abre el navegador y entra a http://localhost:3000.
 *     2. Captura la pantalla de Login con sus efectos.
 *     3. Rellena los campos y hace clic en "Entrar al Campus".
 *     4. Espera a que cargue el motor de juego Phaser.js v3.
 *     5. Simula pulsación de teclas (movimiento) en el campus escolar.
 *     6. Captura la pantalla del videojuego con los avatares activos.
 * ═══════════════════════════════════════════════════════════════════════
 */

const puppeteer = require('puppeteer');
const path = require('path');

const SCREENSHOT_LOGIN_PATH    = path.join(__dirname, 'client', 'captura_1_login.png');
const SCREENSHOT_GAMEPLAY_PATH = path.join(__dirname, 'client', 'captura_2_juego.png');

async function runBrowserTest() {
    console.log('\n🎮 [1/5] Lanzando navegador Chromium...');
    const browser = await puppeteer.launch({
        headless: true,
        args: ['--no-sandbox', '--disable-setuid-sandbox']
    });

    const page = await browser.newPage();
    await page.setViewport({ width: 1024, height: 768 });

    // Reportar logs de la página para depurar
    page.on('console', msg => console.log('  [Navegador LOG]:', msg.text()));
    page.on('pageerror', err => console.log('  [Navegador ERROR]:', err.toString()));

    console.log('🌐 [2/5] Navegando a http://localhost:3000...');
    await page.goto('http://localhost:3000', { waitUntil: 'networkidle2' });

    console.log('📸 [3/5] Tomando captura de pantalla del Login...');
    await page.waitForSelector('#login-screen', { visible: true });
    // Esperar a que las partículas fluyan un momento
    await new Promise(r => setTimeout(r, 1000));
    await page.screenshot({ path: SCREENSHOT_LOGIN_PATH });
    console.log(`       ✔ Captura del login guardada en: captura_1_login.png`);

    console.log('✍️ [4/5] Rellenando credenciales e iniciando sesión...');
    await page.type('#login-username', 'alumno_jorge');
    await page.type('#login-password', 'password123');
    
    console.log('🖱️ Haciendo clic en "Entrar al Campus"...');
    await page.click('#btn-login');

    console.log('🎮 [5/5] Esperando a que el motor Phaser inicialice el mapa...');
    await page.waitForSelector('#game-screen.active', { timeout: 15000 });
    
    // Esperar un par de segundos para que Phaser inicialice las texturas procedurales y renderice el mapa
    await new Promise(r => setTimeout(r, 3000));

    console.log('🚶 Simulando caminar hacia la entrada (Pulsando Flecha Arriba)...');
    await page.keyboard.down('ArrowUp');
    await new Promise(r => setTimeout(r, 2600)); // Caminar suficiente para cruzar la puerta
    await page.keyboard.up('ArrowUp');

    console.log('🎮 Esperando transición e inicialización de la Recepción...');
    await new Promise(r => setTimeout(r, 1000)); // Esperar carga de nueva zona

    console.log('📸 Tomando captura del gameplay interior en Phaser.js...');
    await page.screenshot({ path: SCREENSHOT_GAMEPLAY_PATH });
    console.log(`       ✔ Captura del gameplay guardada en: captura_2_juego.png`);

    console.log('\n👋 Cerrando navegador...');
    await browser.close();

    console.log('\n======================================================================');
    console.log('🎉 EXPLORACIÓN COMPLETADA Y CAPTURADA CON ÉXITO');
    console.log('Puedes ver las imágenes reales generadas en tu carpeta de cliente:');
    console.log('  1. captura_1_login.png (Pantalla de login premium)');
    console.log('  2. captura_2_juego.png (Mapa de Phaser con avatar en movimiento)');
    console.log('======================================================================\n');
}

runBrowserTest().catch(err => {
    console.error('❌ Error durante la simulación de navegación:', err);
    process.exit(1);
});
