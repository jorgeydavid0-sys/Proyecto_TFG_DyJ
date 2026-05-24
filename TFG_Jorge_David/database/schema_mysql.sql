-- ═══════════════════════════════════════════════════════════════════
-- Escuela Interactiva 2D — Esquema de Base de Datos MySQL
-- TFG Jorge González — DAM 2º
-- Ejecutar: mysql -u root -p < database/schema_mysql.sql
-- ═══════════════════════════════════════════════════════════════════

CREATE DATABASE IF NOT EXISTS escuela_interactiva CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE escuela_interactiva;

-- ─── Tabla de Usuarios ───────────────────────────────────────────────
-- Gestiona las credenciales globales del sistema con roles RBAC
CREATE TABLE IF NOT EXISTS usuarios (
    id_usuario    INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nombre        VARCHAR(50) NOT NULL,
    apellidos     VARCHAR(100) NOT NULL,
    rol           ENUM('admin', 'profesor', 'alumno') NOT NULL DEFAULT 'alumno',
    color_avatar  VARCHAR(7) DEFAULT '#4A90D9',
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ─── Tabla de Alumnos ────────────────────────────────────────────────
-- Entidad dependiente que extiende los datos específicos de estudiantes
CREATE TABLE IF NOT EXISTS alumnos (
    id_alumno     INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario    INT NOT NULL,
    curso         VARCHAR(20) NOT NULL,
    sprite_avatar VARCHAR(50) DEFAULT 'default_avatar.png',
    posicion_x    FLOAT DEFAULT 480.0,
    posicion_y    FLOAT DEFAULT 550.0,
    zona_actual   VARCHAR(50) DEFAULT 'entrada',
    CONSTRAINT fk_alumno_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuarios(id_usuario) ON DELETE CASCADE
);

-- ─── Tabla de Asignaturas ────────────────────────────────────────────
-- Listado de materias impartidas en el centro
CREATE TABLE IF NOT EXISTS asignaturas (
    id_asignatura     INT AUTO_INCREMENT PRIMARY KEY,
    nombre_asignatura VARCHAR(100) NOT NULL,
    id_profesor       INT,
    CONSTRAINT fk_asig_profesor FOREIGN KEY (id_profesor)
        REFERENCES usuarios(id_usuario) ON DELETE SET NULL
);

-- ─── Tabla de Calificaciones ─────────────────────────────────────────
-- Persistencia del expediente escolar y notas de las entregas
CREATE TABLE IF NOT EXISTS calificaciones (
    id_calificacion INT AUTO_INCREMENT PRIMARY KEY,
    id_alumno       INT NOT NULL,
    id_asignatura   INT NOT NULL,
    evaluacion      VARCHAR(20) NOT NULL,
    nota            DECIMAL(4,2) NOT NULL CHECK (nota >= 0.00 AND nota <= 10.00),
    comentario      TEXT,
    tipo            VARCHAR(30) DEFAULT 'Examen',
    fecha_registro  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cal_alumno   FOREIGN KEY (id_alumno)    REFERENCES alumnos(id_alumno)     ON DELETE CASCADE,
    CONSTRAINT fk_cal_asig     FOREIGN KEY (id_asignatura) REFERENCES asignaturas(id_asignatura) ON DELETE CASCADE
);

-- ─── Tabla de Puntos de Interés ──────────────────────────────────────
-- Coordenadas del mapa 2D asociadas a eventos lógicos
CREATE TABLE IF NOT EXISTS puntos_interes (
    id_punto       INT AUTO_INCREMENT PRIMARY KEY,
    nombre_zona    VARCHAR(50) NOT NULL,
    coordenada_x   FLOAT NOT NULL,
    coordenada_y   FLOAT NOT NULL,
    modulo_destino VARCHAR(50) NOT NULL,
    etiqueta       VARCHAR(100),
    ancho          INT DEFAULT 120,
    alto           INT DEFAULT 80
);

-- ─── Tabla de Anuncios (Tablón) ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS anuncios (
    id_anuncio  INT AUTO_INCREMENT PRIMARY KEY,
    titulo      VARCHAR(200) NOT NULL,
    contenido   TEXT NOT NULL,
    autor       VARCHAR(100) NOT NULL,
    importante  BOOLEAN DEFAULT FALSE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ─── Tabla de Horario ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS horario (
    id_horario    INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario    INT NOT NULL,
    dia           ENUM('Lunes','Martes','Miércoles','Jueves','Viernes') NOT NULL,
    hora_inicio   TIME NOT NULL,
    hora_fin      TIME NOT NULL,
    asignatura    VARCHAR(100) NOT NULL,
    aula          VARCHAR(50),
    CONSTRAINT fk_horario_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuarios(id_usuario) ON DELETE CASCADE
);

-- ─── Tabla de Menú del Comedor ───────────────────────────────────────
CREATE TABLE IF NOT EXISTS menu_comedor (
    id_menu       INT AUTO_INCREMENT PRIMARY KEY,
    fecha         DATE NOT NULL UNIQUE,
    primer_plato  VARCHAR(200) NOT NULL,
    segundo_plato VARCHAR(200) NOT NULL,
    postre        VARCHAR(100) NOT NULL,
    alergenos     VARCHAR(200) DEFAULT 'Consultar'
);

-- ═══════════════════════════════════════════════════════════════════
-- DATOS INICIALES (SEED DATA)
-- ═══════════════════════════════════════════════════════════════════

-- Usuarios (contraseñas hasheadas con bcrypt: 'admin123', 'profesor123', 'alumno123')
INSERT INTO usuarios (username, password_hash, nombre, apellidos, rol, color_avatar) VALUES
('admin',    '$2a$10$rqJ8xfMpEJ0fHY3vqBpsPuHnFz4F/bFzjJiNrEn7.sAQUH.1mVkuq', 'Administrador', 'Sistema',        'admin',    '#FF6B6B'),
('prof_garcia', '$2a$10$rqJ8xfMpEJ0fHY3vqBpsPuHnFz4F/bFzjJiNrEn7.sAQUH.1mVkuq', 'María', 'García López', 'profesor', '#4ECDC4'),
('jorge_dam2', '$2a$10$8K1p/a0dR1lVXFdT8rCNFuYU5LzwYPb1sGQ6wJp8jHhEPuHwX7iCO', 'Jorge', 'González',     'alumno',   '#4A90D9')
ON DUPLICATE KEY UPDATE username=username;

-- Alumno vinculado a jorge_dam2
INSERT INTO alumnos (id_usuario, curso, posicion_x, posicion_y) 
SELECT id_usuario, '2º DAM', 480.0, 550.0 FROM usuarios WHERE username = 'jorge_dam2'
ON DUPLICATE KEY UPDATE id_alumno=id_alumno;

-- Asignaturas vinculadas al profesor
INSERT INTO asignaturas (nombre_asignatura, id_profesor) VALUES
('Programación',            (SELECT id_usuario FROM usuarios WHERE username='prof_garcia')),
('Base de Datos',           (SELECT id_usuario FROM usuarios WHERE username='prof_garcia')),
('Sistemas Informáticos',   (SELECT id_usuario FROM usuarios WHERE username='prof_garcia')),
('Entornos de Desarrollo',  (SELECT id_usuario FROM usuarios WHERE username='prof_garcia')),
('Desarrollo Web',          (SELECT id_usuario FROM usuarios WHERE username='prof_garcia'))
ON DUPLICATE KEY UPDATE nombre_asignatura=nombre_asignatura;

-- Calificaciones del alumno jorge_dam2
INSERT INTO calificaciones (id_alumno, id_asignatura, evaluacion, nota, comentario, tipo, fecha_registro)
SELECT a.id_alumno, as2.id_asignatura, 'Primera Evaluación', 8.50, 'Buen trabajo en POO', 'Examen', '2026-04-15'
FROM alumnos a, asignaturas as2, usuarios u
WHERE u.username = 'jorge_dam2' AND a.id_usuario = u.id_usuario AND as2.nombre_asignatura = 'Programación';

INSERT INTO calificaciones (id_alumno, id_asignatura, evaluacion, nota, comentario, tipo, fecha_registro)
SELECT a.id_alumno, as2.id_asignatura, 'Primera Evaluación', 9.00, 'Excelente diseño de esquema', 'Examen', '2026-04-10'
FROM alumnos a, asignaturas as2, usuarios u
WHERE u.username = 'jorge_dam2' AND a.id_usuario = u.id_usuario AND as2.nombre_asignatura = 'Base de Datos';

INSERT INTO calificaciones (id_alumno, id_asignatura, evaluacion, nota, comentario, tipo, fecha_registro)
SELECT a.id_alumno, as2.id_asignatura, 'Primera Evaluación', 7.25, 'Mejorar documentación', 'Práctica', '2026-04-05'
FROM alumnos a, asignaturas as2, usuarios u
WHERE u.username = 'jorge_dam2' AND a.id_usuario = u.id_usuario AND as2.nombre_asignatura = 'Sistemas Informáticos';

INSERT INTO calificaciones (id_alumno, id_asignatura, evaluacion, nota, comentario, tipo, fecha_registro)
SELECT a.id_alumno, as2.id_asignatura, 'Primera Evaluación', 8.75, 'Proyecto TFG sobresaliente', 'Proyecto', '2026-03-28'
FROM alumnos a, asignaturas as2, usuarios u
WHERE u.username = 'jorge_dam2' AND a.id_usuario = u.id_usuario AND as2.nombre_asignatura = 'Entornos de Desarrollo';

INSERT INTO calificaciones (id_alumno, id_asignatura, evaluacion, nota, comentario, tipo, fecha_registro)
SELECT a.id_alumno, as2.id_asignatura, 'Primera Evaluación', 9.50, 'Dominio de frontend y backend', 'Examen', '2026-03-20'
FROM alumnos a, asignaturas as2, usuarios u
WHERE u.username = 'jorge_dam2' AND a.id_usuario = u.id_usuario AND as2.nombre_asignatura = 'Desarrollo Web';

-- Puntos de interés del mapa (zona recepción)
INSERT INTO puntos_interes (nombre_zona, coordenada_x, coordenada_y, modulo_destino, etiqueta, ancho, alto) VALUES
('recepcion', 200, 60, 'tablon',      '📋 Tablón de Anuncios',    120, 80),
('recepcion', 640, 60, 'horario',     '📅 Horario / Calendario',  120, 80),
('conserjeria', 200, 60, 'menu',      '🍽️ Menú del Comedor',      120, 80),
('conserjeria', 640, 60, 'notas',     '📊 Notas / Trabajos',      120, 80)
ON DUPLICATE KEY UPDATE nombre_zona=nombre_zona;

-- Anuncios del tablón
INSERT INTO anuncios (titulo, contenido, autor, importante) VALUES
('📢 Bienvenidos al nuevo curso', 'Bienvenidos al curso 2025-2026 de DAM. Revisad los horarios actualizados en recepción.', 'Dirección', TRUE),
('🏆 Concurso de Programación', 'Inscripción abierta para el concurso anual de programación. Fecha límite: 15 de mayo.', 'Dpto. Informática', FALSE),
('📅 Entrega TFG', 'La fecha límite para la entrega del TFG es el 30 de junio. Consultad con vuestros tutores.', 'Secretaría', TRUE),
('🎮 Torneo de eSports', 'El club de gaming organiza un torneo de League of Legends. Inscripciones hasta el viernes.', 'Club Gaming', FALSE),
('📚 Biblioteca - Nuevos horarios', 'La biblioteca amplía su horario: de 8:00 a 20:00 de lunes a viernes.', 'Biblioteca', FALSE)
ON DUPLICATE KEY UPDATE titulo=titulo;

-- Horario del alumno jorge_dam2
INSERT INTO horario (id_usuario, dia, hora_inicio, hora_fin, asignatura, aula)
SELECT u.id_usuario, 'Lunes', '08:30', '10:15', 'Programación', 'Aula 201'
FROM usuarios u WHERE u.username = 'jorge_dam2';
INSERT INTO horario (id_usuario, dia, hora_inicio, hora_fin, asignatura, aula)
SELECT u.id_usuario, 'Lunes', '10:30', '12:15', 'Base de Datos', 'Aula 202'
FROM usuarios u WHERE u.username = 'jorge_dam2';
INSERT INTO horario (id_usuario, dia, hora_inicio, hora_fin, asignatura, aula)
SELECT u.id_usuario, 'Martes', '08:30', '10:15', 'Entornos de Desarrollo', 'Lab 1'
FROM usuarios u WHERE u.username = 'jorge_dam2';
INSERT INTO horario (id_usuario, dia, hora_inicio, hora_fin, asignatura, aula)
SELECT u.id_usuario, 'Miércoles', '08:30', '10:15', 'Sistemas Informáticos', 'Aula 105'
FROM usuarios u WHERE u.username = 'jorge_dam2';
INSERT INTO horario (id_usuario, dia, hora_inicio, hora_fin, asignatura, aula)
SELECT u.id_usuario, 'Jueves', '10:30', '12:15', 'Desarrollo Web', 'Lab 2'
FROM usuarios u WHERE u.username = 'jorge_dam2';
INSERT INTO horario (id_usuario, dia, hora_inicio, hora_fin, asignatura, aula)
SELECT u.id_usuario, 'Viernes', '08:30', '10:15', 'Programación', 'Aula 201'
FROM usuarios u WHERE u.username = 'jorge_dam2';

-- Menú del comedor (semana actual)
INSERT INTO menu_comedor (fecha, primer_plato, segundo_plato, postre, alergenos) VALUES
(CURDATE(),     'Crema de calabaza',     'Pollo al horno con patatas',    'Fruta de temporada', 'Lácteos'),
(CURDATE()+1,   'Ensalada mixta',        'Merluza a la plancha',          'Yogur natural',      'Pescado, Lácteos'),
(CURDATE()+2,   'Lentejas estofadas',    'Tortilla española',             'Helado',             'Huevo, Lácteos'),
(CURDATE()+3,   'Sopa de pollo',         'Lomo a la plancha con ensalada','Natillas',           'Lácteos'),
(CURDATE()+4,   'Macarrones boloñesa',   'Filete de ternera',             'Tarta de manzana',   'Gluten, Lácteos')
ON DUPLICATE KEY UPDATE primer_plato=VALUES(primer_plato);
