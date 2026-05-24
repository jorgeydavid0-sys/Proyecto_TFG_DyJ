-- ═══════════════════════════════════════════════════════════════════
-- Escuela Interactiva 2D — Esquema de Base de Datos PostgreSQL
-- TFG Jorge González — DAM 2º
-- ═══════════════════════════════════════════════════════════════════

-- Crear la base de datos (ejecutar manualmente si no existe)
-- CREATE DATABASE escuela_interactiva;

-- ─── Tabla de Usuarios ───────────────────────────────────────────────
-- Almacena la información de cada alumno/jugador conectado.
CREATE TABLE IF NOT EXISTS usuarios (
    id          SERIAL PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL UNIQUE,
    email       VARCHAR(200),
    password    VARCHAR(255) NOT NULL DEFAULT '1234',
    pos_x       INTEGER DEFAULT 480,
    pos_y       INTEGER DEFAULT 550,
    zona        VARCHAR(50) DEFAULT 'entrada',
    color       VARCHAR(7) DEFAULT '#4A90D9',
    rol         VARCHAR(20) DEFAULT 'alumno',
    nivel       INTEGER DEFAULT 1,
    xp          INTEGER DEFAULT 0,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ─── Tabla de Notas / Calificaciones ─────────────────────────────────
-- Guarda las calificaciones de cada alumno por asignatura.
CREATE TABLE IF NOT EXISTS notas (
    id              SERIAL PRIMARY KEY,
    id_alumno       INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    materia         VARCHAR(100) NOT NULL,
    calificacion    DECIMAL(4,2) NOT NULL CHECK (calificacion >= 0 AND calificacion <= 10),
    tipo            VARCHAR(50) DEFAULT 'Examen',
    comentario      TEXT,
    fecha           DATE DEFAULT CURRENT_DATE
);

-- ─── Tabla de Horarios ───────────────────────────────────────────────
-- Define el horario semanal de clases.
CREATE TABLE IF NOT EXISTS horarios (
    id          SERIAL PRIMARY KEY,
    id_alumno   INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    dia         VARCHAR(20) NOT NULL CHECK (dia IN ('Lunes','Martes','Miércoles','Jueves','Viernes')),
    hora_inicio TIME NOT NULL,
    hora_fin    TIME NOT NULL,
    asignatura  VARCHAR(100) NOT NULL,
    aula        VARCHAR(50)
);

-- ─── Tabla de Anuncios del Tablón ────────────────────────────────────
-- Noticias y avisos publicados en el tablón de la entrada.
CREATE TABLE IF NOT EXISTS anuncios (
    id          SERIAL PRIMARY KEY,
    titulo      VARCHAR(200) NOT NULL,
    contenido   TEXT NOT NULL,
    autor       VARCHAR(100) DEFAULT 'Dirección',
    fecha       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    importante  BOOLEAN DEFAULT FALSE
);

-- ─── Tabla de Menú del Comedor ───────────────────────────────────────
-- Menú diario del comedor escolar.
CREATE TABLE IF NOT EXISTS menu_comedor (
    id              SERIAL PRIMARY KEY,
    fecha           DATE NOT NULL UNIQUE,
    primer_plato    VARCHAR(200),
    segundo_plato   VARCHAR(200),
    postre          VARCHAR(200),
    alergenos       TEXT
);

-- ─── Tabla de Mensajes de Chat ───────────────────────────────────────
-- Registro de mensajes entre usuarios en el mapa.
CREATE TABLE IF NOT EXISTS chat_mensajes (
    id              SERIAL PRIMARY KEY,
    emisor_id       INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    mensaje         TEXT NOT NULL,
    zona            VARCHAR(50),
    timestamp       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- ═══════════════════════════════════════════════════════════════════
-- DATOS DE EJEMPLO
-- ═══════════════════════════════════════════════════════════════════

-- Usuarios de prueba
INSERT INTO usuarios (nombre, email, password, color, rol) VALUES
    ('Jorge', 'jorge@escuela.com', '1234', '#4A90D9', 'alumno'),
    ('María', 'maria@escuela.com', '1234', '#E74C3C', 'profesor'),
    ('Carlos', 'carlos@escuela.com', '1234', '#2ECC71', 'admin'),
    ('Ana', 'ana@escuela.com', '1234', '#F39C12', 'alumno')
ON CONFLICT (nombre) DO NOTHING;

-- Notas de ejemplo
INSERT INTO notas (id_alumno, materia, calificacion, tipo, comentario) VALUES
    (1, 'Programación', 8.50, 'Examen', 'Buen trabajo en POO'),
    (1, 'Base de Datos', 9.00, 'Examen', 'Excelente diseño de esquema'),
    (1, 'Sistemas Informáticos', 7.25, 'Práctica', 'Mejorar documentación'),
    (1, 'Entornos de Desarrollo', 8.75, 'Proyecto', 'Proyecto TFG sobresaliente'),
    (1, 'Desarrollo Web', 9.50, 'Examen', 'Dominio de frontend y backend'),
    (2, 'Programación', 7.00, 'Examen', 'Revisar estructuras de datos'),
    (2, 'Base de Datos', 8.25, 'Examen', 'Buenas consultas SQL'),
    (3, 'Programación', 9.25, 'Examen', 'Excelente código limpio'),
    (3, 'Base de Datos', 6.50, 'Práctica', 'Necesita más práctica en JOIN')
ON CONFLICT DO NOTHING;

-- Horarios de ejemplo (para usuario 1 - Jorge)
INSERT INTO horarios (id_alumno, dia, hora_inicio, hora_fin, asignatura, aula) VALUES
    (1, 'Lunes',     '08:30', '10:15', 'Programación',          'Aula 201'),
    (1, 'Lunes',     '10:30', '12:15', 'Base de Datos',         'Aula 203'),
    (1, 'Lunes',     '12:30', '14:15', 'Entornos de Desarrollo','Lab 1'),
    (1, 'Martes',    '08:30', '10:15', 'Desarrollo Web',        'Lab 2'),
    (1, 'Martes',    '10:30', '12:15', 'Sistemas Informáticos', 'Aula 105'),
    (1, 'Miércoles', '08:30', '10:15', 'Programación',          'Aula 201'),
    (1, 'Miércoles', '10:30', '12:15', 'Base de Datos',         'Aula 203'),
    (1, 'Jueves',    '08:30', '10:15', 'Desarrollo Web',        'Lab 2'),
    (1, 'Jueves',    '10:30', '12:15', 'Entornos de Desarrollo','Lab 1'),
    (1, 'Viernes',   '08:30', '10:15', 'Sistemas Informáticos', 'Aula 105'),
    (1, 'Viernes',   '10:30', '12:15', 'Programación',          'Aula 201')
ON CONFLICT DO NOTHING;

-- Anuncios del tablón
INSERT INTO anuncios (titulo, contenido, autor, importante) VALUES
    ('📢 Bienvenidos al nuevo curso', 'Bienvenidos al curso 2025-2026 de DAM. Recordad revisar los horarios actualizados en recepción.', 'Dirección', TRUE),
    ('🏆 Concurso de Programación', 'Se abre la inscripción para el concurso anual de programación. Fecha límite: 15 de mayo.', 'Dpto. Informática', FALSE),
    ('📅 Entrega TFG', 'La fecha límite para la entrega del Trabajo Final de Grado es el 30 de junio. Consultad con vuestros tutores.', 'Secretaría', TRUE),
    ('🎮 Torneo de eSports', 'El club de gaming organiza un torneo de League of Legends. Inscripciones abiertas hasta el viernes.', 'Club Gaming', FALSE),
    ('📚 Biblioteca - Nuevos horarios', 'La biblioteca amplía su horario: de 8:00 a 20:00 de lunes a viernes.', 'Biblioteca', FALSE)
ON CONFLICT DO NOTHING;

-- Menú del comedor (semana ejemplo)
INSERT INTO menu_comedor (fecha, primer_plato, segundo_plato, postre, alergenos) VALUES
    (CURRENT_DATE,     'Crema de calabaza',      'Pollo al horno con patatas',   'Fruta de temporada', 'Lácteos'),
    (CURRENT_DATE + 1, 'Ensalada mixta',         'Merluza a la plancha',         'Yogur natural',      'Pescado, Lácteos'),
    (CURRENT_DATE + 2, 'Lentejas estofadas',     'Tortilla española',            'Helado',             'Huevo, Lácteos'),
    (CURRENT_DATE + 3, 'Sopa de pollo',          'Lomo a la plancha con ensalada','Natillas',           'Lácteos'),
    (CURRENT_DATE + 4, 'Macarrones boloñesa',    'Filete de ternera',            'Tarta de manzana',   'Gluten, Lácteos')
ON CONFLICT (fecha) DO NOTHING;
