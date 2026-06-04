--
-- PostgreSQL database dump
--

\restrict TqGNZeLnSnYXfwAT84dsnZMBhvwPuedgBfbZKddeNt4YcghSEeo0OuaMYlZ3ZYt

-- Dumped from database version 18.1
-- Dumped by pg_dump version 18.1

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: anuncios; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.anuncios (
    id integer NOT NULL,
    titulo character varying(200) NOT NULL,
    contenido text NOT NULL,
    autor character varying(100) DEFAULT 'Direccion'::character varying,
    fecha timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    importante boolean DEFAULT false,
    autor_id integer
);


ALTER TABLE public.anuncios OWNER TO postgres;

--
-- Name: anuncios_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.anuncios_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.anuncios_id_seq OWNER TO postgres;

--
-- Name: anuncios_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.anuncios_id_seq OWNED BY public.anuncios.id;


--
-- Name: chat_mensajes; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.chat_mensajes (
    id integer NOT NULL,
    emisor_id integer,
    mensaje text NOT NULL,
    zona character varying(20) NOT NULL,
    "timestamp" timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.chat_mensajes OWNER TO postgres;

--
-- Name: chat_mensajes_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.chat_mensajes_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.chat_mensajes_id_seq OWNER TO postgres;

--
-- Name: chat_mensajes_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.chat_mensajes_id_seq OWNED BY public.chat_mensajes.id;


--
-- Name: horarios; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.horarios (
    id integer NOT NULL,
    curso character varying(20) NOT NULL,
    clase character varying(5) NOT NULL,
    dia character varying(15) NOT NULL,
    hora_inicio character varying(5) NOT NULL,
    hora_fin character varying(5) NOT NULL,
    asignatura character varying(100) NOT NULL,
    aula character varying(50) DEFAULT ''::character varying,
    CONSTRAINT horarios_dia_check CHECK (((dia)::text = ANY ((ARRAY['Lunes'::character varying, 'Martes'::character varying, 'Miercoles'::character varying, 'Jueves'::character varying, 'Viernes'::character varying])::text[])))
);


ALTER TABLE public.horarios OWNER TO postgres;

--
-- Name: horarios_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.horarios_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.horarios_id_seq OWNER TO postgres;

--
-- Name: horarios_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.horarios_id_seq OWNED BY public.horarios.id;


--
-- Name: menu_comedor; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.menu_comedor (
    id integer NOT NULL,
    fecha date NOT NULL,
    primer_plato character varying(200) NOT NULL,
    segundo_plato character varying(200) NOT NULL,
    postre character varying(200) NOT NULL,
    alergenos character varying(200) DEFAULT 'Ninguno'::character varying
);


ALTER TABLE public.menu_comedor OWNER TO postgres;

--
-- Name: menu_comedor_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.menu_comedor_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.menu_comedor_id_seq OWNER TO postgres;

--
-- Name: menu_comedor_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.menu_comedor_id_seq OWNED BY public.menu_comedor.id;


--
-- Name: notas; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.notas (
    id integer NOT NULL,
    id_alumno integer,
    materia character varying(100) NOT NULL,
    calificacion numeric(4,2) NOT NULL,
    tipo character varying(50) DEFAULT 'Examen'::character varying,
    comentario text DEFAULT ''::text,
    fecha date DEFAULT CURRENT_DATE
);


ALTER TABLE public.notas OWNER TO postgres;

--
-- Name: notas_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.notas_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.notas_id_seq OWNER TO postgres;

--
-- Name: notas_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.notas_id_seq OWNED BY public.notas.id;


--
-- Name: usuarios; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.usuarios (
    id integer NOT NULL,
    nombre character varying(50) NOT NULL,
    password character varying(100) NOT NULL,
    email character varying(100),
    color character varying(7) DEFAULT '#4A90D9'::character varying,
    rol character varying(10) DEFAULT 'alumno'::character varying,
    nivel integer DEFAULT 1,
    xp integer DEFAULT 0,
    curso character varying(20) DEFAULT ''::character varying,
    clase character varying(5) DEFAULT ''::character varying,
    CONSTRAINT usuarios_rol_check CHECK (((rol)::text = ANY ((ARRAY['alumno'::character varying, 'profesor'::character varying, 'admin'::character varying])::text[])))
);


ALTER TABLE public.usuarios OWNER TO postgres;

--
-- Name: usuarios_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.usuarios_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.usuarios_id_seq OWNER TO postgres;

--
-- Name: usuarios_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.usuarios_id_seq OWNED BY public.usuarios.id;


--
-- Name: anuncios id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.anuncios ALTER COLUMN id SET DEFAULT nextval('public.anuncios_id_seq'::regclass);


--
-- Name: chat_mensajes id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.chat_mensajes ALTER COLUMN id SET DEFAULT nextval('public.chat_mensajes_id_seq'::regclass);


--
-- Name: horarios id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.horarios ALTER COLUMN id SET DEFAULT nextval('public.horarios_id_seq'::regclass);


--
-- Name: menu_comedor id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.menu_comedor ALTER COLUMN id SET DEFAULT nextval('public.menu_comedor_id_seq'::regclass);


--
-- Name: notas id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notas ALTER COLUMN id SET DEFAULT nextval('public.notas_id_seq'::regclass);


--
-- Name: usuarios id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.usuarios ALTER COLUMN id SET DEFAULT nextval('public.usuarios_id_seq'::regclass);


--
-- Data for Name: anuncios; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.anuncios (id, titulo, contenido, autor, fecha, importante, autor_id) FROM stdin;
1	Bienvenidos al curso 2025-2026	Bienvenidos a un nuevo año escolar. Recordad que las normas de convivencia estan publicadas en la web del centro y en el tablón de la entrada.	Direccion	2026-05-20 00:00:00	t	\N
2	Entrega del TFG — PLAZO FINAL	La fecha limite para la entrega del Trabajo de Fin de Grado es el 30 de junio a las 23:59. Entregad a través del Moodle del centro. No se admitiran entregas fuera de plazo.	Coordinacion DAM	2026-05-18 00:00:00	t	\N
3	Concurso de Programacion "CodeLagomar"	El departamento de informatica organiza el II Concurso de Programacion. Inscripciones abiertas hasta el 10 de junio. Premio: tablet para los 3 primeros clasificados.	Dpto. Informatica	2026-05-16 00:00:00	f	\N
4	Excursion a la empresa tecnologica TechHub	El proximo 5 de junio visitaremos las instalaciones de TechHub. Autorizaciones disponibles en secretaria. Precio: 8 euros. Plazas limitadas.	Jefatura de Estudios	2026-05-14 00:00:00	f	\N
5	Actualizacion del menu del comedor — junio	El menu del comedor ha sido actualizado para el mes de junio. Podeis consultarlo en el tablón de la conserjeria o en la app.	Comedor Escolar	2026-05-12 00:00:00	f	\N
6	Simulacro de evacuacion — jueves 29 de mayo	El jueves 29 de mayo a las 11:00h se realizara un simulacro de evacuacion de emergencia. Seguid las instrucciones del profesorado.	Coordinador de Seguridad	2026-05-10 00:00:00	t	\N
\.


--
-- Data for Name: chat_mensajes; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.chat_mensajes (id, emisor_id, mensaje, zona, "timestamp") FROM stdin;
\.


--
-- Data for Name: horarios; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.horarios (id, curso, clase, dia, hora_inicio, hora_fin, asignatura, aula) FROM stdin;
1	2DAM	A	Lunes	08:30	10:15	Programacion Mult.	Lab 201
2	2DAM	A	Lunes	10:30	12:15	Base de Datos	Aula 203
3	2DAM	A	Lunes	12:30	14:15	Entornos de Desarrollo	Lab 1
4	2DAM	A	Martes	08:30	10:15	Desarrollo Web	Lab 2
5	2DAM	A	Martes	10:30	12:15	Sistemas Informaticos	Aula 105
6	2DAM	A	Martes	12:30	14:15	Despliegue Aplic.	Aula 301
7	2DAM	A	Miercoles	08:30	10:15	Programacion Mult.	Lab 201
8	2DAM	A	Miercoles	10:30	12:15	Base de Datos	Aula 203
9	2DAM	A	Miercoles	12:30	14:15	Empresa e Iniciativa	Aula 102
10	2DAM	A	Jueves	08:30	10:15	Desarrollo Web	Lab 2
11	2DAM	A	Jueves	10:30	12:15	Entornos de Desarrollo	Lab 1
12	2DAM	A	Viernes	08:30	10:15	Sistemas Informaticos	Aula 105
13	2DAM	A	Viernes	10:30	12:15	Despliegue Aplic.	Aula 301
14	1DAM	A	Lunes	08:30	10:15	Programacion	Lab 201
15	1DAM	A	Lunes	10:30	12:15	Sistemas Informaticos	Aula 105
16	1DAM	A	Lunes	12:30	14:15	Lenguajes de Marcas	Aula 103
17	1DAM	A	Martes	08:30	10:15	Bases de Datos	Aula 203
18	1DAM	A	Martes	10:30	12:15	Entornos de Desarrollo	Lab 1
19	1DAM	A	Martes	12:30	14:15	FOL	Aula 102
20	1DAM	A	Miercoles	08:30	10:15	Programacion	Lab 201
21	1DAM	A	Miercoles	10:30	12:15	Sistemas Informaticos	Aula 105
22	1DAM	A	Jueves	08:30	10:15	Bases de Datos	Aula 203
23	1DAM	A	Jueves	10:30	12:15	Lenguajes de Marcas	Aula 103
24	1DAM	A	Viernes	08:30	10:15	Programacion	Lab 201
25	1DAM	A	Viernes	10:30	12:15	Entornos de Desarrollo	Lab 1
26	3ESO	B	Lunes	08:00	09:00	Matematicas	Aula 12
27	3ESO	B	Lunes	09:00	10:00	Lengua Castellana	Aula 12
28	3ESO	B	Lunes	10:00	11:00	Fisica y Quimica	Lab Ciencias
29	3ESO	B	Lunes	11:30	12:30	Ingles	Aula 15
30	3ESO	B	Lunes	12:30	13:30	Historia	Aula 12
31	3ESO	B	Martes	08:00	09:00	Matematicas	Aula 12
32	3ESO	B	Martes	09:00	10:00	Tecnologia	Taller
33	3ESO	B	Martes	10:00	11:00	Ed. Plastica	Aula Arte
34	3ESO	B	Martes	11:30	12:30	Biologia	Lab Ciencias
35	3ESO	B	Miercoles	08:00	09:00	Lengua Castellana	Aula 12
36	3ESO	B	Miercoles	09:00	10:00	Ingles	Aula 15
37	3ESO	B	Miercoles	10:00	11:00	Ed. Fisica	Gimnasio
38	3ESO	B	Jueves	08:00	09:00	Matematicas	Aula 12
39	3ESO	B	Jueves	09:00	10:00	Historia	Aula 12
40	3ESO	B	Jueves	11:30	12:30	Fisica y Quimica	Lab Ciencias
41	3ESO	B	Viernes	08:00	09:00	Biologia	Lab Ciencias
42	3ESO	B	Viernes	09:00	10:00	Tecnologia	Taller
43	3ESO	B	Viernes	10:00	11:00	Matematicas	Aula 12
44	4ESO	A	Lunes	08:00	09:00	Matematicas Acad.	Aula 22
45	4ESO	A	Lunes	09:00	10:00	Lengua Castellana	Aula 22
46	4ESO	A	Lunes	10:00	11:00	Fisica y Quimica	Lab Ciencias
47	4ESO	A	Lunes	11:30	12:30	Ingles	Aula 25
48	4ESO	A	Martes	08:00	09:00	Historia Contemp.	Aula 22
49	4ESO	A	Martes	09:00	10:00	Economia	Aula 23
50	4ESO	A	Martes	10:00	11:00	Ed. Fisica	Gimnasio
51	4ESO	A	Miercoles	08:00	09:00	Matematicas Acad.	Aula 22
52	4ESO	A	Miercoles	09:00	10:00	Biologia	Lab Ciencias
53	4ESO	A	Miercoles	11:30	12:30	Lengua Castellana	Aula 22
54	4ESO	A	Jueves	08:00	09:00	Fisica y Quimica	Lab Ciencias
55	4ESO	A	Jueves	09:00	10:00	Ingles	Aula 25
56	4ESO	A	Viernes	08:00	09:00	Historia Contemp.	Aula 22
57	4ESO	A	Viernes	09:00	10:00	Matematicas Acad.	Aula 22
58	1ASIR	A	Lunes	08:30	10:15	Implantacion SO	Lab 301
59	1ASIR	A	Lunes	10:30	12:15	Planif. y Admin. Redes	Aula 305
60	1ASIR	A	Lunes	12:30	14:15	Gestion BD	Aula 203
61	1ASIR	A	Martes	08:30	10:15	Implantacion SO	Lab 301
62	1ASIR	A	Martes	10:30	12:15	Fundamentos Hardware	Lab HW
63	1ASIR	A	Martes	12:30	14:15	FOL	Aula 102
64	1ASIR	A	Miercoles	08:30	10:15	Planif. y Admin. Redes	Aula 305
65	1ASIR	A	Miercoles	10:30	12:15	Gestion BD	Aula 203
66	1ASIR	A	Jueves	08:30	10:15	Implantacion SO	Lab 301
67	1ASIR	A	Jueves	10:30	12:15	Fundamentos Hardware	Lab HW
68	1ASIR	A	Viernes	08:30	10:15	Planif. y Admin. Redes	Aula 305
69	1ASIR	A	Viernes	10:30	12:15	Gestion BD	Aula 203
70	2DAM	A	Lunes	13:00	14:00	Ingles	Aula 999
\.


--
-- Data for Name: menu_comedor; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.menu_comedor (id, fecha, primer_plato, segundo_plato, postre, alergenos) FROM stdin;
1	2026-05-26	Crema de calabaza con croutones	Pollo al horno con patatas	Fruta de temporada	Lacteos, Gluten
2	2026-05-27	Ensalada de pasta con atun	Merluza a la plancha con pure	Yogur natural	Pescado, Lacteos, Gluten
3	2026-05-28	Lentejas estofadas con chorizo	Tortilla espanola con ensalada	Helado de vainilla	Huevo, Lacteos
4	2026-05-29	Sopa de fideos casera	Lomo a la plancha con verduras	Natillas con canela	Lacteos, Gluten, Huevo
5	2026-05-30	Gazpacho andaluz	Croquetas de jamon con ensalada	Melocoton en almibar	Lacteos, Gluten, Huevo
6	2026-06-02	Pure de verduras	Salmon al horno con limon	Flan casero	Pescado, Lacteos, Huevo
7	2026-06-03	Macarrones con tomate y carne	Filete de ternera a la plancha	Fruta del tiempo	Gluten
\.


--
-- Data for Name: notas; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.notas (id, id_alumno, materia, calificacion, tipo, comentario, fecha) FROM stdin;
2	1	Base de Datos	9.00	Examen	Excelente diseno de esquema relacional	2026-05-10
3	1	Sistemas Informaticos	7.25	Practica	Mejorar la documentacion del informe	2026-05-05
4	1	Entornos de Desarrollo	8.75	Proyecto	Proyecto TFG sobresaliente	2026-05-01
5	1	Desarrollo Web	9.50	Examen	Dominio completo de frontend y backend	2026-04-20
6	\N	Programacion Mult.	7.00	Examen	Revisar patrones de diseno	2026-05-15
7	\N	Base de Datos	8.50	Examen	Buenas consultas SQL avanzadas	2026-05-10
8	\N	Sistemas Informaticos	6.50	Practica	Necesita mejorar configuracion de redes	2026-05-05
9	\N	Desarrollo Web	9.00	Proyecto	Excelente interfaz responsive	2026-04-28
10	\N	Despliegue Aplic.	7.75	Examen	Buen uso de contenedores Docker	2026-04-15
11	5	Programacion	7.50	Examen	Buena logica, revisar recursividad	2026-05-14
12	5	Bases de Datos	8.00	Practica	Buen diseno de tablas	2026-05-08
13	5	Sistemas Informaticos	6.75	Examen	Mejorar conceptos de virtualizacion	2026-04-30
14	5	Lenguajes de Marcas	9.00	Proyecto	HTML/CSS impecable	2026-04-22
15	6	Programacion	9.00	Examen	Excelente comprension de algoritmos	2026-05-14
16	6	Bases de Datos	7.50	Practica	Corregir normalizacion en 3FN	2026-05-08
17	6	Sistemas Informaticos	8.25	Examen	Muy buen manejo de Linux	2026-04-30
18	6	Entornos de Desarrollo	7.00	Practica	Usar mas atajos del IDE	2026-04-25
19	8	Matematicas	6.50	Examen	Revisar ecuaciones de segundo grado	2026-05-13
20	8	Fisica y Quimica	7.00	Practica	Buena metodologia experimental	2026-05-07
21	8	Ingles	5.50	Examen	Trabajar la expresion escrita	2026-04-29
22	8	Historia	8.00	Examen	Excelente analisis historico	2026-04-20
23	9	Matematicas Acad.	9.50	Examen	Preparada para la EBAU	2026-05-12
24	9	Fisica y Quimica	8.75	Practica	Excelente trabajo experimental	2026-05-06
25	9	Ingles	8.00	Examen	Buen nivel, mejorar listening	2026-04-28
26	9	Economia	9.00	Proyecto	Analisis macroeconomico sobresaliente	2026-04-18
27	10	Implantacion SO	7.50	Practica	Buen manejo de PowerShell y Bash	2026-05-12
28	10	Planif. y Admin. Redes	8.00	Examen	Entiende bien subnetting	2026-05-05
29	10	Gestion BD	6.75	Practica	Revisar transacciones y bloqueos	2026-04-28
30	10	Fundamentos Hardware	8.50	Examen	Excelente diagnostico de averias	2026-04-15
1	1	Programacion Mult.	9.50	Examen	Buen dominio de POO y colecciones	2026-05-15
31	2	Ingles	10.00	Examen		2026-05-26
\.


--
-- Data for Name: usuarios; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.usuarios (id, nombre, password, email, color, rol, nivel, xp, curso, clase) FROM stdin;
3	Marcos	5b1628f887e1f48980426af3f4e582af2d097f1dcefedbfbd95a2b56ea078c2b	\N	#9B59B6	profesor	1	0		
4	Elena	4a008ef315a10dcd4919902c0a580a57ca9484259cf5adfc4026836b4c39f4d5	\N	#2ECC71	admin	1	0		
2	Lauraaaa	4f1b38229d1e9ac964c59cc69eaeea98303f3d1c5fb4fd5b6919fe70d7e8f479	\N	#E74C3C	alumno	1	0		
1	David	a7c55cb647d6ebbc725f587dbaf2f4d75936a0887ee96eaf08e8ec388df655ff	\N	#4A90D9	alumno	1	0	2DAM	A
5	Ana	dab65f851bdd04ff8b45d8ed45fef2283f04eaf0cbc6f700ca0ac4e869b1b5cf	\N	#9B59B6	alumno	1	0	1DAM	A
6	Carlos	59e6b5a7bb3621efbce78257e0c184b45416bdf1779bdd119b001207ba517e13	\N	#F39C12	alumno	1	0	1DAM	A
7	Lucia	0336e150e7ed159a8a1b97ba69bfa2080b97a4213273bed3d168ea8fc7b1893d	\N	#1ABC9C	alumno	1	0	2DAM	B
8	Pedro	15dea785b62225736f53aa4faeb387d7470ff44248d2d67ac7ddc75d7b39b0e4	\N	#E74C3C	alumno	1	0	3ESO	B
9	Sofia	2653682975c0730d4bfacebc144fd46556f16183c8d190db554fcc6ed0f616e0	\N	#2ECC71	alumno	1	0	4ESO	A
10	Javier	1b9fbab6466384653ceec819ea0249dd88054df309063a6629da3b3ccd1e1ffe	\N	#4A90D9	alumno	1	0	1ASIR	A
\.


--
-- Name: anuncios_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.anuncios_id_seq', 6, true);


--
-- Name: chat_mensajes_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.chat_mensajes_id_seq', 1, false);


--
-- Name: horarios_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.horarios_id_seq', 70, true);


--
-- Name: menu_comedor_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.menu_comedor_id_seq', 7, true);


--
-- Name: notas_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.notas_id_seq', 31, true);


--
-- Name: usuarios_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.usuarios_id_seq', 10, true);


--
-- Name: anuncios anuncios_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.anuncios
    ADD CONSTRAINT anuncios_pkey PRIMARY KEY (id);


--
-- Name: chat_mensajes chat_mensajes_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.chat_mensajes
    ADD CONSTRAINT chat_mensajes_pkey PRIMARY KEY (id);


--
-- Name: horarios horarios_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.horarios
    ADD CONSTRAINT horarios_pkey PRIMARY KEY (id);


--
-- Name: menu_comedor menu_comedor_fecha_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.menu_comedor
    ADD CONSTRAINT menu_comedor_fecha_key UNIQUE (fecha);


--
-- Name: menu_comedor menu_comedor_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.menu_comedor
    ADD CONSTRAINT menu_comedor_pkey PRIMARY KEY (id);


--
-- Name: notas notas_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notas
    ADD CONSTRAINT notas_pkey PRIMARY KEY (id);


--
-- Name: usuarios usuarios_nombre_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_nombre_key UNIQUE (nombre);


--
-- Name: usuarios usuarios_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_pkey PRIMARY KEY (id);


--
-- Name: anuncios anuncios_autor_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.anuncios
    ADD CONSTRAINT anuncios_autor_id_fkey FOREIGN KEY (autor_id) REFERENCES public.usuarios(id) ON DELETE SET NULL;


--
-- Name: chat_mensajes chat_mensajes_emisor_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.chat_mensajes
    ADD CONSTRAINT chat_mensajes_emisor_id_fkey FOREIGN KEY (emisor_id) REFERENCES public.usuarios(id) ON DELETE SET NULL;


--
-- Name: notas notas_id_alumno_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notas
    ADD CONSTRAINT notas_id_alumno_fkey FOREIGN KEY (id_alumno) REFERENCES public.usuarios(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

\unrestrict TqGNZeLnSnYXfwAT84dsnZMBhvwPuedgBfbZKddeNt4YcghSEeo0OuaMYlZ3ZYt

