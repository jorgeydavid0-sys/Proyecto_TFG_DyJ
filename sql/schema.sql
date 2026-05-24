-- Escuela Interactiva 2D — Esquema PostgreSQL
-- Ejecutar: psql -U postgres -f schema.sql

CREATE DATABASE escuela_interactiva;
\c escuela_interactiva

CREATE TABLE IF NOT EXISTS usuarios (
    id        SERIAL PRIMARY KEY,
    nombre    VARCHAR(50)  UNIQUE NOT NULL,
    password  VARCHAR(100) NOT NULL,
    email     VARCHAR(100),
    color     VARCHAR(7)   DEFAULT '#4A90D9',
    rol       VARCHAR(10)  DEFAULT 'alumno'
              CHECK (rol IN ('alumno','profesor','admin')),
    nivel     INTEGER      DEFAULT 1,
    xp        INTEGER      DEFAULT 0
);

CREATE TABLE IF NOT EXISTS anuncios (
    id         SERIAL PRIMARY KEY,
    titulo     VARCHAR(200) NOT NULL,
    contenido  TEXT         NOT NULL,
    autor      VARCHAR(100) DEFAULT 'Direccion',
    fecha      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    importante BOOLEAN      DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS notas (
    id            SERIAL PRIMARY KEY,
    id_alumno     INTEGER REFERENCES usuarios(id) ON DELETE CASCADE,
    materia       VARCHAR(100) NOT NULL,
    calificacion  DECIMAL(4,2) NOT NULL,
    tipo          VARCHAR(50)  DEFAULT 'Examen',
    comentario    TEXT         DEFAULT '',
    fecha         DATE         DEFAULT CURRENT_DATE
);

CREATE TABLE IF NOT EXISTS horarios (
    id          SERIAL PRIMARY KEY,
    id_alumno   INTEGER REFERENCES usuarios(id) ON DELETE CASCADE,
    dia         VARCHAR(15) NOT NULL
                CHECK (dia IN ('Lunes','Martes','Miercoles','Jueves','Viernes')),
    hora_inicio VARCHAR(5)  NOT NULL,
    hora_fin    VARCHAR(5)  NOT NULL,
    asignatura  VARCHAR(100) NOT NULL,
    aula        VARCHAR(50)  DEFAULT ''
);

CREATE TABLE IF NOT EXISTS menu_comedor (
    id             SERIAL PRIMARY KEY,
    fecha          DATE        UNIQUE NOT NULL,
    primer_plato   VARCHAR(200) NOT NULL,
    segundo_plato  VARCHAR(200) NOT NULL,
    postre         VARCHAR(200) NOT NULL,
    alergenos      VARCHAR(200) DEFAULT 'Ninguno'
);

CREATE TABLE IF NOT EXISTS chat_mensajes (
    id         SERIAL PRIMARY KEY,
    emisor_id  INTEGER REFERENCES usuarios(id) ON DELETE SET NULL,
    mensaje    TEXT    NOT NULL,
    zona       VARCHAR(20) NOT NULL,
    timestamp  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);
