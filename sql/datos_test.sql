-- ============================================================
--  Escuela Interactiva 2D – Datos de test completos
--  Ejecutar en pgAdmin sobre la BD escuela_interactiva
-- ============================================================

\c escuela_interactiva

-- ── 1. Ampliar tabla usuarios ─────────────────────────────────
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS curso VARCHAR(20) DEFAULT '';
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS clase VARCHAR(5)  DEFAULT '';

-- ── 2. Rediseñar horarios: por clase, no por alumno ───────────
DROP TABLE IF EXISTS horarios;
CREATE TABLE horarios (
    id          SERIAL PRIMARY KEY,
    curso       VARCHAR(20) NOT NULL,
    clase       VARCHAR(5)  NOT NULL,
    dia         VARCHAR(15) NOT NULL
                CHECK (dia IN ('Lunes','Martes','Miercoles','Jueves','Viernes')),
    hora_inicio VARCHAR(5)  NOT NULL,
    hora_fin    VARCHAR(5)  NOT NULL,
    asignatura  VARCHAR(100) NOT NULL,
    aula        VARCHAR(50)  DEFAULT ''
);

-- ── 3. Añadir autor_id a anuncios ─────────────────────────────
ALTER TABLE anuncios ADD COLUMN IF NOT EXISTS autor_id INTEGER REFERENCES usuarios(id) ON DELETE SET NULL;

-- ── 4. Actualizar usuarios existentes ─────────────────────────
UPDATE usuarios SET curso='2DAM', clase='A' WHERE nombre='David';
UPDATE usuarios SET curso='2DAM', clase='A' WHERE nombre='Laura';
-- Marcos y Elena no tienen curso/clase (profesor y admin)

-- ── 5. Nuevos alumnos ─────────────────────────────────────────
INSERT INTO usuarios (nombre, password, color, rol, curso, clase) VALUES
('Ana',    'dab65f851bdd04ff8b45d8ed45fef2283f04eaf0cbc6f700ca0ac4e869b1b5cf', '#9B59B6', 'alumno', '1DAM',  'A'),
('Carlos', '59e6b5a7bb3621efbce78257e0c184b45416bdf1779bdd119b001207ba517e13', '#F39C12', 'alumno', '1DAM',  'A'),
('Lucia',  '0336e150e7ed159a8a1b97ba69bfa2080b97a4213273bed3d168ea8fc7b1893d', '#1ABC9C', 'alumno', '2DAM',  'B'),
('Pedro',  '15dea785b62225736f53aa4faeb387d7470ff44248d2d67ac7ddc75d7b39b0e4', '#E74C3C', 'alumno', '3ESO',  'B'),
('Sofia',  '2653682975c0730d4bfacebc144fd46556f16183c8d190db554fcc6ed0f616e0', '#2ECC71', 'alumno', '4ESO',  'A'),
('Javier', '1b9fbab6466384653ceec819ea0249dd88054df309063a6629da3b3ccd1e1ffe', '#4A90D9', 'alumno', '1ASIR', 'A');

-- ── 6. Horarios por clase ─────────────────────────────────────

-- 2DAM-A (David, Laura)
INSERT INTO horarios (curso,clase,dia,hora_inicio,hora_fin,asignatura,aula) VALUES
('2DAM','A','Lunes',   '08:30','10:15','Programacion Mult.',    'Lab 201'),
('2DAM','A','Lunes',   '10:30','12:15','Base de Datos',         'Aula 203'),
('2DAM','A','Lunes',   '12:30','14:15','Entornos de Desarrollo','Lab 1'),
('2DAM','A','Martes',  '08:30','10:15','Desarrollo Web',        'Lab 2'),
('2DAM','A','Martes',  '10:30','12:15','Sistemas Informaticos', 'Aula 105'),
('2DAM','A','Martes',  '12:30','14:15','Despliegue Aplic.',     'Aula 301'),
('2DAM','A','Miercoles','08:30','10:15','Programacion Mult.',   'Lab 201'),
('2DAM','A','Miercoles','10:30','12:15','Base de Datos',        'Aula 203'),
('2DAM','A','Miercoles','12:30','14:15','Empresa e Iniciativa', 'Aula 102'),
('2DAM','A','Jueves',  '08:30','10:15','Desarrollo Web',        'Lab 2'),
('2DAM','A','Jueves',  '10:30','12:15','Entornos de Desarrollo','Lab 1'),
('2DAM','A','Viernes', '08:30','10:15','Sistemas Informaticos', 'Aula 105'),
('2DAM','A','Viernes', '10:30','12:15','Despliegue Aplic.',     'Aula 301');

-- 1DAM-A (Ana, Carlos)
INSERT INTO horarios (curso,clase,dia,hora_inicio,hora_fin,asignatura,aula) VALUES
('1DAM','A','Lunes',    '08:30','10:15','Programacion',          'Lab 201'),
('1DAM','A','Lunes',    '10:30','12:15','Sistemas Informaticos', 'Aula 105'),
('1DAM','A','Lunes',    '12:30','14:15','Lenguajes de Marcas',   'Aula 103'),
('1DAM','A','Martes',   '08:30','10:15','Bases de Datos',        'Aula 203'),
('1DAM','A','Martes',   '10:30','12:15','Entornos de Desarrollo','Lab 1'),
('1DAM','A','Martes',   '12:30','14:15','FOL',                   'Aula 102'),
('1DAM','A','Miercoles','08:30','10:15','Programacion',          'Lab 201'),
('1DAM','A','Miercoles','10:30','12:15','Sistemas Informaticos', 'Aula 105'),
('1DAM','A','Jueves',   '08:30','10:15','Bases de Datos',        'Aula 203'),
('1DAM','A','Jueves',   '10:30','12:15','Lenguajes de Marcas',   'Aula 103'),
('1DAM','A','Viernes',  '08:30','10:15','Programacion',          'Lab 201'),
('1DAM','A','Viernes',  '10:30','12:15','Entornos de Desarrollo','Lab 1');

-- 3ESO-B (Pedro)
INSERT INTO horarios (curso,clase,dia,hora_inicio,hora_fin,asignatura,aula) VALUES
('3ESO','B','Lunes',    '08:00','09:00','Matematicas',           'Aula 12'),
('3ESO','B','Lunes',    '09:00','10:00','Lengua Castellana',     'Aula 12'),
('3ESO','B','Lunes',    '10:00','11:00','Fisica y Quimica',      'Lab Ciencias'),
('3ESO','B','Lunes',    '11:30','12:30','Ingles',                'Aula 15'),
('3ESO','B','Lunes',    '12:30','13:30','Historia',              'Aula 12'),
('3ESO','B','Martes',   '08:00','09:00','Matematicas',           'Aula 12'),
('3ESO','B','Martes',   '09:00','10:00','Tecnologia',            'Taller'),
('3ESO','B','Martes',   '10:00','11:00','Ed. Plastica',          'Aula Arte'),
('3ESO','B','Martes',   '11:30','12:30','Biologia',              'Lab Ciencias'),
('3ESO','B','Miercoles','08:00','09:00','Lengua Castellana',     'Aula 12'),
('3ESO','B','Miercoles','09:00','10:00','Ingles',                'Aula 15'),
('3ESO','B','Miercoles','10:00','11:00','Ed. Fisica',            'Gimnasio'),
('3ESO','B','Jueves',   '08:00','09:00','Matematicas',           'Aula 12'),
('3ESO','B','Jueves',   '09:00','10:00','Historia',              'Aula 12'),
('3ESO','B','Jueves',   '11:30','12:30','Fisica y Quimica',      'Lab Ciencias'),
('3ESO','B','Viernes',  '08:00','09:00','Biologia',              'Lab Ciencias'),
('3ESO','B','Viernes',  '09:00','10:00','Tecnologia',            'Taller'),
('3ESO','B','Viernes',  '10:00','11:00','Matematicas',           'Aula 12');

-- 4ESO-A (Sofia)
INSERT INTO horarios (curso,clase,dia,hora_inicio,hora_fin,asignatura,aula) VALUES
('4ESO','A','Lunes',    '08:00','09:00','Matematicas Acad.',     'Aula 22'),
('4ESO','A','Lunes',    '09:00','10:00','Lengua Castellana',     'Aula 22'),
('4ESO','A','Lunes',    '10:00','11:00','Fisica y Quimica',      'Lab Ciencias'),
('4ESO','A','Lunes',    '11:30','12:30','Ingles',                'Aula 25'),
('4ESO','A','Martes',   '08:00','09:00','Historia Contemp.',     'Aula 22'),
('4ESO','A','Martes',   '09:00','10:00','Economia',              'Aula 23'),
('4ESO','A','Martes',   '10:00','11:00','Ed. Fisica',            'Gimnasio'),
('4ESO','A','Miercoles','08:00','09:00','Matematicas Acad.',     'Aula 22'),
('4ESO','A','Miercoles','09:00','10:00','Biologia',              'Lab Ciencias'),
('4ESO','A','Miercoles','11:30','12:30','Lengua Castellana',     'Aula 22'),
('4ESO','A','Jueves',   '08:00','09:00','Fisica y Quimica',      'Lab Ciencias'),
('4ESO','A','Jueves',   '09:00','10:00','Ingles',                'Aula 25'),
('4ESO','A','Viernes',  '08:00','09:00','Historia Contemp.',     'Aula 22'),
('4ESO','A','Viernes',  '09:00','10:00','Matematicas Acad.',     'Aula 22');

-- 1ASIR-A (Javier)
INSERT INTO horarios (curso,clase,dia,hora_inicio,hora_fin,asignatura,aula) VALUES
('1ASIR','A','Lunes',    '08:30','10:15','Implantacion SO',       'Lab 301'),
('1ASIR','A','Lunes',    '10:30','12:15','Planif. y Admin. Redes','Aula 305'),
('1ASIR','A','Lunes',    '12:30','14:15','Gestion BD',            'Aula 203'),
('1ASIR','A','Martes',   '08:30','10:15','Implantacion SO',       'Lab 301'),
('1ASIR','A','Martes',   '10:30','12:15','Fundamentos Hardware',  'Lab HW'),
('1ASIR','A','Martes',   '12:30','14:15','FOL',                   'Aula 102'),
('1ASIR','A','Miercoles','08:30','10:15','Planif. y Admin. Redes','Aula 305'),
('1ASIR','A','Miercoles','10:30','12:15','Gestion BD',            'Aula 203'),
('1ASIR','A','Jueves',   '08:30','10:15','Implantacion SO',       'Lab 301'),
('1ASIR','A','Jueves',   '10:30','12:15','Fundamentos Hardware',  'Lab HW'),
('1ASIR','A','Viernes',  '08:30','10:15','Planif. y Admin. Redes','Aula 305'),
('1ASIR','A','Viernes',  '10:30','12:15','Gestion BD',            'Aula 203');

-- ── 7. Notas por alumno ───────────────────────────────────────

-- David (id dinámico — usar subconsulta)
INSERT INTO notas (id_alumno, materia, calificacion, tipo, comentario, fecha) VALUES
((SELECT id FROM usuarios WHERE nombre='David'), 'Programacion Mult.',     8.5,  'Examen',  'Buen dominio de POO y colecciones',       '2026-05-15'),
((SELECT id FROM usuarios WHERE nombre='David'), 'Base de Datos',          9.0,  'Examen',  'Excelente diseno de esquema relacional',   '2026-05-10'),
((SELECT id FROM usuarios WHERE nombre='David'), 'Sistemas Informaticos',  7.25, 'Practica','Mejorar la documentacion del informe',     '2026-05-05'),
((SELECT id FROM usuarios WHERE nombre='David'), 'Entornos de Desarrollo', 8.75, 'Proyecto','Proyecto TFG sobresaliente',               '2026-05-01'),
((SELECT id FROM usuarios WHERE nombre='David'), 'Desarrollo Web',         9.5,  'Examen',  'Dominio completo de frontend y backend',   '2026-04-20');

-- Laura
INSERT INTO notas (id_alumno, materia, calificacion, tipo, comentario, fecha) VALUES
((SELECT id FROM usuarios WHERE nombre='Laura'), 'Programacion Mult.',     7.0,  'Examen',  'Revisar patrones de diseno',               '2026-05-15'),
((SELECT id FROM usuarios WHERE nombre='Laura'), 'Base de Datos',          8.5,  'Examen',  'Buenas consultas SQL avanzadas',           '2026-05-10'),
((SELECT id FROM usuarios WHERE nombre='Laura'), 'Sistemas Informaticos',  6.5,  'Practica','Necesita mejorar configuracion de redes',  '2026-05-05'),
((SELECT id FROM usuarios WHERE nombre='Laura'), 'Desarrollo Web',         9.0,  'Proyecto','Excelente interfaz responsive',            '2026-04-28'),
((SELECT id FROM usuarios WHERE nombre='Laura'), 'Despliegue Aplic.',      7.75, 'Examen',  'Buen uso de contenedores Docker',          '2026-04-15');

-- Ana
INSERT INTO notas (id_alumno, materia, calificacion, tipo, comentario, fecha) VALUES
((SELECT id FROM usuarios WHERE nombre='Ana'), 'Programacion',           7.5,  'Examen',  'Buena logica, revisar recursividad',       '2026-05-14'),
((SELECT id FROM usuarios WHERE nombre='Ana'), 'Bases de Datos',         8.0,  'Practica','Buen diseno de tablas',                    '2026-05-08'),
((SELECT id FROM usuarios WHERE nombre='Ana'), 'Sistemas Informaticos',  6.75, 'Examen',  'Mejorar conceptos de virtualizacion',      '2026-04-30'),
((SELECT id FROM usuarios WHERE nombre='Ana'), 'Lenguajes de Marcas',    9.0,  'Proyecto','HTML/CSS impecable',                       '2026-04-22');

-- Carlos
INSERT INTO notas (id_alumno, materia, calificacion, tipo, comentario, fecha) VALUES
((SELECT id FROM usuarios WHERE nombre='Carlos'), 'Programacion',           9.0,  'Examen',  'Excelente comprension de algoritmos',   '2026-05-14'),
((SELECT id FROM usuarios WHERE nombre='Carlos'), 'Bases de Datos',         7.5,  'Practica','Corregir normalizacion en 3FN',         '2026-05-08'),
((SELECT id FROM usuarios WHERE nombre='Carlos'), 'Sistemas Informaticos',  8.25, 'Examen',  'Muy buen manejo de Linux',              '2026-04-30'),
((SELECT id FROM usuarios WHERE nombre='Carlos'), 'Entornos de Desarrollo', 7.0,  'Practica','Usar mas atajos del IDE',               '2026-04-25');

-- Pedro
INSERT INTO notas (id_alumno, materia, calificacion, tipo, comentario, fecha) VALUES
((SELECT id FROM usuarios WHERE nombre='Pedro'), 'Matematicas',    6.5,  'Examen',  'Revisar ecuaciones de segundo grado',     '2026-05-13'),
((SELECT id FROM usuarios WHERE nombre='Pedro'), 'Fisica y Quimica',7.0, 'Practica','Buena metodologia experimental',         '2026-05-07'),
((SELECT id FROM usuarios WHERE nombre='Pedro'), 'Ingles',          5.5,  'Examen',  'Trabajar la expresion escrita',          '2026-04-29'),
((SELECT id FROM usuarios WHERE nombre='Pedro'), 'Historia',        8.0,  'Examen',  'Excelente analisis historico',           '2026-04-20');

-- Sofia
INSERT INTO notas (id_alumno, materia, calificacion, tipo, comentario, fecha) VALUES
((SELECT id FROM usuarios WHERE nombre='Sofia'), 'Matematicas Acad.', 9.5,  'Examen',  'Preparada para la EBAU',                 '2026-05-12'),
((SELECT id FROM usuarios WHERE nombre='Sofia'), 'Fisica y Quimica',  8.75, 'Practica','Excelente trabajo experimental',        '2026-05-06'),
((SELECT id FROM usuarios WHERE nombre='Sofia'), 'Ingles',            8.0,  'Examen',  'Buen nivel, mejorar listening',          '2026-04-28'),
((SELECT id FROM usuarios WHERE nombre='Sofia'), 'Economia',          9.0,  'Proyecto','Analisis macroeconomico sobresaliente',  '2026-04-18');

-- Javier
INSERT INTO notas (id_alumno, materia, calificacion, tipo, comentario, fecha) VALUES
((SELECT id FROM usuarios WHERE nombre='Javier'), 'Implantacion SO',        7.5,  'Practica','Buen manejo de PowerShell y Bash',     '2026-05-12'),
((SELECT id FROM usuarios WHERE nombre='Javier'), 'Planif. y Admin. Redes', 8.0,  'Examen',  'Entiende bien subnetting',             '2026-05-05'),
((SELECT id FROM usuarios WHERE nombre='Javier'), 'Gestion BD',             6.75, 'Practica','Revisar transacciones y bloqueos',     '2026-04-28'),
((SELECT id FROM usuarios WHERE nombre='Javier'), 'Fundamentos Hardware',   8.5,  'Examen',  'Excelente diagnostico de averias',     '2026-04-15');

-- ── 8. Tablón de anuncios ─────────────────────────────────────
TRUNCATE anuncios RESTART IDENTITY CASCADE;
INSERT INTO anuncios (titulo, contenido, autor, fecha, importante) VALUES
('Bienvenidos al curso 2025-2026',
 'Bienvenidos a un nuevo año escolar. Recordad que las normas de convivencia estan publicadas en la web del centro y en el tablón de la entrada.',
 'Direccion', '2026-05-20', true),
('Entrega del TFG — PLAZO FINAL',
 'La fecha limite para la entrega del Trabajo de Fin de Grado es el 30 de junio a las 23:59. Entregad a través del Moodle del centro. No se admitiran entregas fuera de plazo.',
 'Coordinacion DAM', '2026-05-18', true),
('Concurso de Programacion "CodeLagomar"',
 'El departamento de informatica organiza el II Concurso de Programacion. Inscripciones abiertas hasta el 10 de junio. Premio: tablet para los 3 primeros clasificados.',
 'Dpto. Informatica', '2026-05-16', false),
('Excursion a la empresa tecnologica TechHub',
 'El proximo 5 de junio visitaremos las instalaciones de TechHub. Autorizaciones disponibles en secretaria. Precio: 8 euros. Plazas limitadas.',
 'Jefatura de Estudios', '2026-05-14', false),
('Actualizacion del menu del comedor — junio',
 'El menu del comedor ha sido actualizado para el mes de junio. Podeis consultarlo en el tablón de la conserjeria o en la app.',
 'Comedor Escolar', '2026-05-12', false),
('Simulacro de evacuacion — jueves 29 de mayo',
 'El jueves 29 de mayo a las 11:00h se realizara un simulacro de evacuacion de emergencia. Seguid las instrucciones del profesorado.',
 'Coordinador de Seguridad', '2026-05-10', true);

-- ── 9. Menú del comedor — semana completa ─────────────────────
TRUNCATE menu_comedor RESTART IDENTITY;
INSERT INTO menu_comedor (fecha, primer_plato, segundo_plato, postre, alergenos) VALUES
('2026-05-26', 'Crema de calabaza con croutones',  'Pollo al horno con patatas',    'Fruta de temporada',      'Lacteos, Gluten'),
('2026-05-27', 'Ensalada de pasta con atun',        'Merluza a la plancha con pure', 'Yogur natural',            'Pescado, Lacteos, Gluten'),
('2026-05-28', 'Lentejas estofadas con chorizo',    'Tortilla espanola con ensalada','Helado de vainilla',       'Huevo, Lacteos'),
('2026-05-29', 'Sopa de fideos casera',             'Lomo a la plancha con verduras','Natillas con canela',      'Lacteos, Gluten, Huevo'),
('2026-05-30', 'Gazpacho andaluz',                  'Croquetas de jamon con ensalada','Melocoton en almibar',   'Lacteos, Gluten, Huevo'),
('2026-06-02', 'Pure de verduras',                  'Salmon al horno con limon',     'Flan casero',              'Pescado, Lacteos, Huevo'),
('2026-06-03', 'Macarrones con tomate y carne',     'Filete de ternera a la plancha','Fruta del tiempo',         'Gluten');
