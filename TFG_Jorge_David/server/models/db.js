/**
 * ═══════════════════════════════════════════════════════════════════════
 *   Escuela Interactiva 2D — Conexión MySQL
 *   TFG Jorge González — DAM 2º
 *
 *   Pool de conexiones a MySQL usando mysql2/promise
 * ═══════════════════════════════════════════════════════════════════════
 */
const mysql = require('mysql2/promise');
require('dotenv').config();

const pool = mysql.createPool({
    host:            process.env.DB_HOST     || 'localhost',
    port:            parseInt(process.env.DB_PORT || '3306'),
    database:        process.env.DB_NAME     || 'escuela_interactiva',
    user:            process.env.DB_USER     || 'root',
    password:        process.env.DB_PASSWORD || '',
    waitForConnections: true,
    connectionLimit:    10,
    queueLimit:         0,
    decimalNumbers:     true,
});

// Verificar conexión al arrancar
(async () => {
    try {
        const conn = await pool.getConnection();
        console.log(`  ✅ Conectado a MySQL (${process.env.DB_NAME || 'escuela_interactiva'})`);
        conn.release();
    } catch (err) {
        console.warn(`  ⚠️  No se pudo conectar a MySQL: ${err.message}`);
        console.warn(`  ℹ️  El servidor funcionará con datos de demostración.`);
    }
})();

module.exports = pool;
