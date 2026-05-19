--
-- PostgreSQL database dump
--

\restrict umbkAFFKBmIEBnLppuRxePqqeIO4OMEqp3pQ1BpouoRq4l3PA1PBmdnIl4df5k2

-- Dumped from database version 18.3 (Debian 18.3-1.pgdg13+1)
-- Dumped by pg_dump version 18.3 (Debian 18.3-1.pgdg13+1)

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
-- Name: articulacion; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.articulacion (
    id_articulacion integer NOT NULL,
    codigo character varying(32) NOT NULL,
    nombre character varying(80) NOT NULL
);


--
-- Name: articulacion_id_articulacion_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.articulacion_id_articulacion_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: articulacion_id_articulacion_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.articulacion_id_articulacion_seq OWNED BY public.articulacion.id_articulacion;


--
-- Name: audit_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.audit_log (
    id_audit uuid DEFAULT uuidv7() NOT NULL,
    fecha_hora timestamp without time zone DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'::text) NOT NULL,
    dni_usuario character varying(9) NOT NULL,
    nombre_usuario character varying(60),
    accion character varying(20) NOT NULL,
    entidad character varying(30) NOT NULL,
    id_entidad character varying(50) NOT NULL,
    detalle text,
    ip_origen character varying(45),
    CONSTRAINT audit_log_accion_check CHECK (((accion)::text = ANY ((ARRAY['LOGIN'::character varying, 'LOGOUT'::character varying, 'CREATE'::character varying, 'READ'::character varying, 'UPDATE'::character varying, 'SOFT_DELETE'::character varying, 'DELETE'::character varying, 'EXPORT'::character varying, 'PRINT'::character varying, 'CAMBIO_CONTRASENA'::character varying])::text[])))
);


--
-- Name: TABLE audit_log; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.audit_log IS 'Registro de auditoria INMUTABLE (solo INSERT, nunca UPDATE/DELETE)';


--
-- Name: cita; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cita (
    dni_pac character varying(9) NOT NULL,
    dni_san character varying(9) NOT NULL,
    fecha_cita date NOT NULL,
    hora time without time zone NOT NULL,
    informe bytea,
    fecha_creacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE cita; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.cita IS 'Relacion N:M entre sanitarios y pacientes con fecha e informe';


--
-- Name: cita_audit; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cita_audit (
    dni_pac character varying(20) NOT NULL,
    dni_san character varying(20) NOT NULL,
    fecha_cita date NOT NULL,
    hora time without time zone NOT NULL,
    rev integer NOT NULL,
    rev_type smallint
);


--
-- Name: cp; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cp (
    cp character varying(5) NOT NULL,
    nombre_localidad character varying(100) NOT NULL
);


--
-- Name: TABLE cp; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.cp IS 'Codigos postales vinculados a localidades';


--
-- Name: direccion; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.direccion (
    id_direccion integer NOT NULL,
    calle character varying(150) NOT NULL,
    numero character varying(20) NOT NULL,
    piso character varying(10),
    cp character varying(5) NOT NULL
);


--
-- Name: TABLE direccion; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.direccion IS 'Direcciones normalizadas de los pacientes';


--
-- Name: direccion_id_direccion_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.direccion_id_direccion_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: direccion_id_direccion_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.direccion_id_direccion_seq OWNED BY public.direccion.id_direccion;


--
-- Name: discapacidad; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.discapacidad (
    cod_dis character varying(10) NOT NULL,
    nombre_dis character varying(100) NOT NULL,
    descripcion_dis text,
    necesita_protesis boolean DEFAULT false,
    id_articulacion integer
);


--
-- Name: TABLE discapacidad; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.discapacidad IS 'Catalogo de discapacidades tratables';


--
-- Name: discapacidad_tratamiento; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.discapacidad_tratamiento (
    cod_dis character varying(10) NOT NULL,
    cod_trat character varying(10) NOT NULL
);


--
-- Name: TABLE discapacidad_tratamiento; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.discapacidad_tratamiento IS 'Relacion N:M entre discapacidades y tratamientos';


--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


--
-- Name: juego; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.juego (
    cod_juego character varying(32) NOT NULL,
    nombre character varying(120) NOT NULL,
    descripcion text,
    url_juego character varying(400) NOT NULL,
    id_articulacion integer NOT NULL,
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: localidad; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.localidad (
    nombre_localidad character varying(100) NOT NULL,
    provincia character varying(100) NOT NULL
);


--
-- Name: TABLE localidad; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.localidad IS 'Catalogo de localidades y su provincia';


--
-- Name: nivel_progresion; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.nivel_progresion (
    id_nivel integer NOT NULL,
    nombre character varying(100) NOT NULL,
    nombre_corto character varying(50) NOT NULL,
    descripcion text,
    estado_pac text,
    tipos_ejercicio text,
    orden integer NOT NULL
);


--
-- Name: TABLE nivel_progresion; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.nivel_progresion IS 'Catalogo fijo de 4 niveles clinicos de rehabilitacion';


--
-- Name: paciente; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.paciente (
    dni_pac character varying(9) NOT NULL,
    dni_san character varying(9) NOT NULL,
    nombre_pac character varying(50) NOT NULL,
    apellido1_pac character varying(50) NOT NULL,
    apellido2_pac character varying(50),
    edad_pac integer NOT NULL,
    email_pac character varying(100) NOT NULL,
    num_ss character varying(12) NOT NULL,
    id_direccion integer NOT NULL,
    sexo character varying(20) NOT NULL,
    fecha_nacimiento date NOT NULL,
    foto bytea,
    alergias text DEFAULT ''::text,
    antecedentes text DEFAULT ''::text,
    medicacion_actual text DEFAULT ''::text,
    consentimiento_rgpd boolean DEFAULT false,
    fecha_consentimiento timestamp without time zone,
    protesis boolean DEFAULT false,
    activo boolean DEFAULT true,
    fecha_alta timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    fecha_baja timestamp without time zone,
    archivo_progreso_md text,
    progreso_md_actualizado_en timestamp without time zone,
    contrasena_pac text,
    CONSTRAINT paciente_sexo_check CHECK (((sexo)::text = ANY ((ARRAY['MASCULINO'::character varying, 'FEMENINO'::character varying, 'OTRO'::character varying])::text[])))
);


--
-- Name: TABLE paciente; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.paciente IS 'Pacientes en rehabilitacion (campos clinicos cifrados con AES-256-GCM)';


--
-- Name: paciente_audit; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.paciente_audit (
    dni_pac character varying(20) NOT NULL,
    rev integer NOT NULL,
    rev_type smallint,
    nombre_pac character varying(100),
    apellido1_pac character varying(100),
    apellido2_pac character varying(100),
    edad_pac integer,
    email_pac character varying(200),
    num_ss character varying(20),
    protesis boolean,
    fecha_nacimiento date,
    sexo character varying(20),
    alergias text,
    antecedentes text,
    medicacion_actual text,
    consentimiento_rgpd boolean,
    fecha_consentimiento timestamp without time zone,
    activo boolean,
    fecha_baja timestamp without time zone,
    dni_san character varying(20),
    id_direccion integer,
    foto bytea
);


--
-- Name: paciente_discapacidad; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.paciente_discapacidad (
    dni_pac character varying(9) NOT NULL,
    cod_dis character varying(10) NOT NULL,
    id_nivel_actual integer DEFAULT 1 NOT NULL,
    fecha_asignacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    notas text
);


--
-- Name: TABLE paciente_discapacidad; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.paciente_discapacidad IS 'Discapacidades del paciente con nivel de progresion actual';


--
-- Name: paciente_discapacidad_audit; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.paciente_discapacidad_audit (
    dni_pac character varying(20) NOT NULL,
    cod_dis character varying(20) NOT NULL,
    rev integer NOT NULL,
    rev_type smallint,
    id_nivel_actual integer,
    notas text,
    fecha_asignacion timestamp without time zone
);


--
-- Name: paciente_tratamiento; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.paciente_tratamiento (
    dni_pac character varying(9) NOT NULL,
    cod_trat character varying(10) NOT NULL,
    visible boolean DEFAULT true NOT NULL,
    fecha_asignacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE paciente_tratamiento; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.paciente_tratamiento IS 'Tratamientos asignados con visibilidad controlada por sanitario';


--
-- Name: paciente_tratamiento_audit; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.paciente_tratamiento_audit (
    dni_pac character varying(20) NOT NULL,
    cod_trat character varying(20) NOT NULL,
    rev integer NOT NULL,
    rev_type smallint,
    visible boolean,
    fecha_asignacion timestamp without time zone
);


--
-- Name: revinfo; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.revinfo (
    rev integer NOT NULL,
    revtstmp bigint NOT NULL,
    usuario character varying(20),
    ip_origen character varying(45)
);


--
-- Name: revinfo_rev_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.revinfo_rev_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: revinfo_rev_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.revinfo_rev_seq OWNED BY public.revinfo.rev;


--
-- Name: sanitario; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sanitario (
    dni_san character varying(9) NOT NULL,
    nombre_san character varying(50) NOT NULL,
    apellido1_san character varying(50) NOT NULL,
    apellido2_san character varying(50),
    email_san character varying(100) NOT NULL,
    contrasena_san character varying(255) NOT NULL,
    num_de_pacientes integer DEFAULT 0,
    activo boolean DEFAULT true,
    fecha_alta timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    fecha_baja timestamp without time zone
);


--
-- Name: TABLE sanitario; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sanitario IS 'Personal medico que atiende pacientes';


--
-- Name: sanitario_agrega_sanitario; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sanitario_agrega_sanitario (
    dni_san character varying(9) NOT NULL,
    cargo character varying(25) NOT NULL,
    CONSTRAINT sanitario_agrega_sanitario_cargo_check CHECK (((cargo)::text = ANY ((ARRAY['SPECIALIST'::character varying, 'NURSE'::character varying])::text[])))
);


--
-- Name: TABLE sanitario_agrega_sanitario; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sanitario_agrega_sanitario IS 'Cargo del sanitario (relacion recursiva 1:1)';


--
-- Name: sanitario_audit; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sanitario_audit (
    dni_san character varying(20) NOT NULL,
    rev integer NOT NULL,
    rev_type smallint,
    nombre_san character varying(100),
    apellido1_san character varying(100),
    apellido2_san character varying(100),
    email_san character varying(200),
    num_de_pacientes integer,
    activo boolean,
    fecha_baja timestamp without time zone
);


--
-- Name: session_reports; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.session_reports (
    id bigint NOT NULL,
    mongo_id character varying(48) NOT NULL,
    paciente_dni character varying(20) NOT NULL,
    cod_juego character varying(64) NOT NULL,
    cod_trat character varying(32),
    fecha_sesion timestamp with time zone NOT NULL,
    duracion_seg integer NOT NULL,
    contenido_md text NOT NULL,
    md_hash character varying(64) NOT NULL,
    fecha_creacion timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: TABLE session_reports; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.session_reports IS 'Resumen Markdown de cada sesion de juego. Cache derivado de game_sessions en MongoDB.';


--
-- Name: COLUMN session_reports.mongo_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.session_reports.mongo_id IS 'ID del documento GameSession en MongoDB. Clave de idempotencia.';


--
-- Name: COLUMN session_reports.contenido_md; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.session_reports.contenido_md IS 'Contenido Markdown generado por el pipeline /data. TEXT permite tsvector futuro.';


--
-- Name: COLUMN session_reports.md_hash; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.session_reports.md_hash IS 'SHA-256 del contenido Markdown. Permite detectar cambios en regeneraciones.';


--
-- Name: session_reports_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.session_reports_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: session_reports_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.session_reports_id_seq OWNED BY public.session_reports.id;


--
-- Name: telefono_paciente; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.telefono_paciente (
    id_telefono integer NOT NULL,
    dni_pac character varying(9) NOT NULL,
    telefono character varying(15) NOT NULL
);


--
-- Name: TABLE telefono_paciente; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.telefono_paciente IS 'Telefonos de contacto del paciente (max 2)';


--
-- Name: telefono_paciente_id_telefono_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.telefono_paciente_id_telefono_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: telefono_paciente_id_telefono_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.telefono_paciente_id_telefono_seq OWNED BY public.telefono_paciente.id_telefono;


--
-- Name: telefono_sanitario; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.telefono_sanitario (
    id_telefono integer NOT NULL,
    dni_san character varying(9) NOT NULL,
    telefono character varying(15) NOT NULL
);


--
-- Name: TABLE telefono_sanitario; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.telefono_sanitario IS 'Telefonos de contacto del sanitario (max 2)';


--
-- Name: telefono_sanitario_id_telefono_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.telefono_sanitario_id_telefono_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: telefono_sanitario_id_telefono_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.telefono_sanitario_id_telefono_seq OWNED BY public.telefono_sanitario.id_telefono;


--
-- Name: tratamiento; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tratamiento (
    cod_trat character varying(10) NOT NULL,
    nombre_trat character varying(100) NOT NULL,
    definicion_trat text,
    id_nivel integer,
    cod_juego character varying(32),
    archivo_pdf bytea,
    nombre_archivo_pdf character varying(255),
    tamano_pdf_bytes bigint,
    CONSTRAINT chk_tamano_pdf CHECK (((tamano_pdf_bytes IS NULL) OR (tamano_pdf_bytes <= 10485760)))
);


--
-- Name: TABLE tratamiento; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.tratamiento IS 'Catalogo de tratamientos vinculados a nivel de progresion';


--
-- Name: tratamiento_videojuego; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tratamiento_videojuego (
    cod_trat character varying(20) NOT NULL,
    id_videojuego bigint NOT NULL,
    fecha_vinculo timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: tratamiento_videojuego_audit; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tratamiento_videojuego_audit (
    cod_trat character varying(20) NOT NULL,
    id_videojuego bigint NOT NULL,
    rev integer NOT NULL,
    rev_type smallint,
    fecha_vinculo timestamp without time zone
);


--
-- Name: videojuego; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.videojuego (
    id_videojuego bigint NOT NULL,
    codigo character varying(50) NOT NULL,
    nombre character varying(200) NOT NULL,
    descripcion text,
    cod_dis character varying(20) NOT NULL,
    parte_cuerpo character varying(100) NOT NULL,
    url_unity character varying(500),
    activo boolean DEFAULT true NOT NULL,
    fecha_creacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: videojuego_audit; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.videojuego_audit (
    id_videojuego bigint NOT NULL,
    rev integer NOT NULL,
    rev_type smallint,
    codigo character varying(50),
    nombre character varying(200),
    descripcion text,
    cod_dis character varying(20),
    parte_cuerpo character varying(100),
    url_unity character varying(500),
    activo boolean,
    fecha_creacion timestamp without time zone
);


--
-- Name: videojuego_id_videojuego_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.videojuego_id_videojuego_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: videojuego_id_videojuego_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.videojuego_id_videojuego_seq OWNED BY public.videojuego.id_videojuego;


--
-- Name: articulacion id_articulacion; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.articulacion ALTER COLUMN id_articulacion SET DEFAULT nextval('public.articulacion_id_articulacion_seq'::regclass);


--
-- Name: direccion id_direccion; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.direccion ALTER COLUMN id_direccion SET DEFAULT nextval('public.direccion_id_direccion_seq'::regclass);


--
-- Name: revinfo rev; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.revinfo ALTER COLUMN rev SET DEFAULT nextval('public.revinfo_rev_seq'::regclass);


--
-- Name: session_reports id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.session_reports ALTER COLUMN id SET DEFAULT nextval('public.session_reports_id_seq'::regclass);


--
-- Name: telefono_paciente id_telefono; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.telefono_paciente ALTER COLUMN id_telefono SET DEFAULT nextval('public.telefono_paciente_id_telefono_seq'::regclass);


--
-- Name: telefono_sanitario id_telefono; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.telefono_sanitario ALTER COLUMN id_telefono SET DEFAULT nextval('public.telefono_sanitario_id_telefono_seq'::regclass);


--
-- Name: videojuego id_videojuego; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.videojuego ALTER COLUMN id_videojuego SET DEFAULT nextval('public.videojuego_id_videojuego_seq'::regclass);


--
-- Data for Name: articulacion; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.articulacion (id_articulacion, codigo, nombre) FROM stdin;
1	HEAD	Cabeza
2	NECK	Cuello
3	TORSO	Torso
4	LEFT_SHOULDER	Hombro izquierdo
5	RIGHT_SHOULDER	Hombro derecho
6	LEFT_ARM	Brazo izquierdo
7	RIGHT_ARM	Brazo derecho
8	LEFT_HAND	Mano izquierda
9	RIGHT_HAND	Mano derecha
10	LEFT_HIP	Cadera izquierda
11	RIGHT_HIP	Cadera derecha
12	LEFT_LEG	Pierna izquierda
13	RIGHT_LEG	Pierna derecha
14	LEFT_FOOT	Pie izquierdo
15	RIGHT_FOOT	Pie derecho
\.


--
-- Data for Name: audit_log; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.audit_log (id_audit, fecha_hora, dni_usuario, nombre_usuario, accion, entidad, id_entidad, detalle, ip_origen) FROM stdin;
6692ce50-2127-4d7a-a128-6358673c084c	2026-04-07 16:04:31.063862	admin0000	Admin Sistema	LOGIN	sanitario	admin0000	Login exitoso	0:0:0:0:0:0:0:1
97d50a6b-5d75-4fc2-9c8a-4772f6f2027e	2026-04-07 16:04:31.374486	00000001R	Carlos Garcia	LOGIN	sanitario	00000001R	Login exitoso	0:0:0:0:0:0:0:1
158034d5-301d-405a-a11c-8566e4f1c60a	2026-04-07 16:04:31.630104	00000002W	Lucia Martinez	LOGIN	sanitario	00000002W	Login exitoso	0:0:0:0:0:0:0:1
1fb1ddbe-84a6-4a31-985d-dd16aafa9eb9	2026-04-08 00:11:14.161217	admin0000	Admin Sistema	LOGIN	sanitario	admin0000	Login exitoso	0:0:0:0:0:0:0:1
67e12943-28ae-42f4-ad37-cb97f9c12d81	2026-04-08 00:11:14.423604	00000001R	Carlos Garcia	LOGIN	sanitario	00000001R	Login exitoso	0:0:0:0:0:0:0:1
b9f94f45-1c25-4dbb-a26e-06f2a9407e4a	2026-04-08 00:11:14.668349	00000002W	Lucia Martinez	LOGIN	sanitario	00000002W	Login exitoso	0:0:0:0:0:0:0:1
b490edc5-277d-480e-be01-d2992239489a	2026-04-08 00:20:58.909839	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
dc2f6706-8a50-4d94-8368-23d632077725	2026-04-08 00:22:27.33087	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	127.0.0.1
5a1c36d8-1b09-493c-aa1b-b044b9ba06ed	2026-04-08 00:22:27.533864	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
0451d988-7be1-4670-831b-f342b0959635	2026-04-08 00:22:30.399709	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
5a294aa5-0a80-43f3-a9c9-56b72d380005	2026-04-08 00:22:31.600765	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
9c6cf79e-0963-4bb2-b2e5-f5af959e9e1a	2026-04-08 00:22:32.88389	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
76df3171-76a6-4871-aa13-709897d53103	2026-04-08 00:22:34.108445	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
9d3b3517-00d6-4d32-97f4-aa2e04d70e0a	2026-04-08 00:22:34.321778	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
ed9f4bc7-eb35-408e-8fa0-debc9d9b1792	2026-04-08 00:22:56.61095	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
3a68416e-b2c8-4153-a699-90465c5c4247	2026-04-08 00:24:21.426692	00000002W	Lucia Martinez	LOGIN	sanitario	00000002W	Login exitoso	127.0.0.1
5fe7dbe7-6b8c-4b62-914e-1344f1124d68	2026-04-08 00:24:21.485411	00000002W	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
46495f9c-5b55-48eb-bb22-699e7dfc4c98	2026-04-08 00:24:25.227524	00000002W	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
6336940a-7b44-4aab-89bd-7849c6948ab5	2026-04-08 00:24:26.477135	00000002W	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
e3e63485-7fdb-4133-9bd4-a52a42a6a35a	2026-04-08 00:24:27.896204	00000002W	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
4665ba29-c260-4992-ab3d-b908b45ac1e1	2026-04-08 00:24:29.15031	00000002W	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
b27118ec-f90d-4a02-b012-88694039809d	2026-04-08 00:24:30.2477	00000002W	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
fbffa2c3-0a2f-44b5-b0c2-1c417352620d	2026-04-08 00:24:31.496907	00000002W	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
9e4b6859-df1c-40c3-874c-5350f0371064	2026-04-08 00:24:32.531907	00000002W	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
e228012a-4752-49ba-9750-a0f5eec8b2f1	2026-04-08 00:24:33.596995	00000002W	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
9c8be738-2b55-417f-bb71-24fab04f773f	2026-04-08 00:24:35.346683	00000002W	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
f999fb8a-715d-466d-af1c-2f0637a8e6f8	2026-04-08 00:24:36.128873	00000002W	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
950d9310-8dff-44d3-bb40-9afcd3d1661b	2026-04-08 00:24:36.868871	00000002W	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
0e8331f2-ea99-4b21-a11c-3f4ce9b35441	2026-04-08 00:33:11.096179	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
0a9b746e-5b1a-4d04-9732-3aa9ef960048	2026-04-08 00:33:11.161453	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	0:0:0:0:0:0:0:1
0a033053-d4cc-4227-8cf9-35df6bd3e38b	2026-04-08 00:34:15.469358	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
d7d0bdea-78c2-4fe0-bfb7-8a5ff15f855a	2026-04-08 00:34:15.490556	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	0:0:0:0:0:0:0:1
59b56671-6002-46da-b208-e67c17906ce2	2026-04-08 00:35:12.118907	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
50fc0391-80c9-493e-b9fb-6686b21dd46e	2026-04-08 00:35:12.165867	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	0:0:0:0:0:0:0:1
4eb88f6c-0c35-4fa1-80f7-c9ddeb5ea744	2026-04-08 00:36:03.987345	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
76189783-6bf1-4311-8397-d0bcad8b9085	2026-04-08 00:36:04.073952	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	0:0:0:0:0:0:0:1
bece7963-63ca-4176-8a23-2c1c9aa9654f	2026-04-08 00:40:08.179677	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
4f41ea53-1b9d-462a-b4e5-cc7b4321a15d	2026-04-08 00:40:08.232028	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	0:0:0:0:0:0:0:1
c91f33ae-6bc8-443b-bab0-3da59070e343	2026-04-08 00:54:31.422635	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	127.0.0.1
41077cf5-fa5a-4440-8afe-03674594bd73	2026-04-08 00:54:31.584794	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
f35ee6ba-bbdc-4694-a0c9-60e7fcb02271	2026-04-08 00:57:02.711374	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
7c25b84d-8225-4538-b787-8d8af35dc31c	2026-04-08 00:57:22.865649	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
162a1cda-5275-4a44-8fdc-7a4ad29baa6c	2026-04-08 12:43:17.047465	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
d0dbd880-cfb1-47fe-9530-73ecf37961f2	2026-04-08 12:43:24.440033	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
69f4233f-79fc-4868-9638-7c19a2cc92b1	2026-04-08 12:45:27.588524	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
459d5460-184b-41e7-a8eb-468a97c11239	2026-04-09 16:13:21.315618	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	127.0.0.1
fde6bfca-de70-4c1b-ba85-40e0ff107cb6	2026-04-09 16:13:21.747872	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
b3f84f2f-cae1-4658-8f01-2ce6837ca76b	2026-04-09 16:13:25.050355	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
3d2042a0-491b-4527-bdcd-38b69455c63f	2026-04-09 18:22:59.365875	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	127.0.0.1
f8182623-ff71-4e15-8176-2b3bdb0d89d6	2026-04-09 18:22:59.666398	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
a0b98c7e-2aff-4c85-be76-3a3563a31a7a	2026-04-09 18:23:01.777473	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
0c576172-a445-409b-b422-caa9e4d44b66	2026-04-12 16:57:14.150269	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	127.0.0.1
2ae0c55e-0a71-449f-aefb-401809bb7fbd	2026-04-12 16:57:14.416835	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
f45229a3-030e-4ac3-85ff-f278a6b4d9b8	2026-04-12 16:57:19.965104	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
dfa951bb-8ef3-4bb1-bc3e-10c9934350ff	2026-04-12 16:57:37.99713	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
0745e5b3-ed74-4d68-a9f2-723c9cc4882d	2026-04-13 22:26:04.946412	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	127.0.0.1
1ada78f5-51b8-4584-b6b5-bf1aec0cb751	2026-04-13 22:26:05.41051	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
402fcbd6-3e24-4202-abbc-c2645f6ba9cf	2026-04-14 16:33:26.281599	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	127.0.0.1
a7bb3bff-a55b-4f40-afd3-91cfa2a8b5cb	2026-04-14 16:33:26.576465	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
1c84c67d-9d32-4419-bc4a-aa07c628680d	2026-04-14 16:33:42.138584	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
3a4c8c97-6d09-4d5f-9c60-36e2066387a8	2026-04-14 16:33:49.830669	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
4727e2c5-ba29-4db7-9d2f-bba881a5f0b9	2026-04-14 22:53:56.115129	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	127.0.0.1
f1400045-6492-4a7a-89bb-617ee6209992	2026-04-14 22:53:56.405693	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
8541f762-5e28-4793-9a94-022f3d96d50d	2026-04-14 22:54:03.956186	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
fef3515e-b7d3-445c-abb4-b57f7cd20d92	2026-04-15 13:25:09.572353	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	127.0.0.1
2c6d3df3-ec9a-4740-9fee-add6d3b15eae	2026-04-15 13:25:09.804731	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
2e74734f-c186-4ca4-8918-5cebf77d8d32	2026-04-15 13:25:14.207451	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
1fb54654-4bbc-414b-a165-53ab0184d73d	2026-04-20 09:00:15.243574	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	127.0.0.1
f11d19df-d737-4a2e-8ed2-1d53840b18a4	2026-04-20 09:00:15.503803	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
d07347df-d4df-4113-9c7d-9c5dd5c7d6b1	2026-04-20 09:00:20.750773	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
10babe70-4ae8-4f1c-95eb-51efb6c13f86	2026-04-20 09:01:16.217231	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
aadb61a0-899a-4061-a86c-cc55ced608ef	2026-04-20 09:01:36.629592	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A	127.0.0.1
e2c69a04-482c-42b4-b4ff-0173d48e1fab	2026-04-20 09:01:36.651993	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/foto	127.0.0.1
b5fe2869-313b-41d0-b298-a92e2794a682	2026-04-20 09:01:36.665599	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/discapacidades	127.0.0.1
081044ec-eb45-4099-b2f6-b07e1c5e7bd6	2026-04-20 09:02:05.373898	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/foto	127.0.0.1
9f5ce3c7-444b-453a-baa9-f58848b3d25a	2026-04-20 09:02:05.416358	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/discapacidades	127.0.0.1
20037566-50e9-4dd1-b8db-4dd629bf7fc0	2026-04-20 09:02:30.928163	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A	127.0.0.1
8db1deef-29b5-4aae-b9b0-c025f244b16d	2026-04-20 09:02:30.956564	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A	127.0.0.1
7877c023-ee37-412e-ab8b-d6eef87f6ff9	2026-04-20 09:02:30.973064	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/foto	127.0.0.1
53c95f60-873d-4d33-b8a8-b9862f3f61e9	2026-04-20 09:02:30.984941	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/discapacidades	127.0.0.1
cd2eadac-c8f0-4dc1-9588-26c75461c0b3	2026-04-20 09:02:32.645165	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
2322c052-9eed-4204-977d-6fbfc4b56c7d	2026-04-20 09:19:19.45864	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	127.0.0.1
bb2d2422-d7e1-4a91-af85-e0bf33b89f41	2026-04-20 09:19:19.6878	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
a6958f9b-ca42-45e1-8f9a-890987382396	2026-04-20 09:19:23.133411	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
9429b8e1-0462-4a28-80bc-1afaeee8572b	2026-04-20 09:19:39.924982	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A	127.0.0.1
851aaff5-7bcc-4220-9f43-2337ff89c752	2026-04-20 09:19:39.949595	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/foto	127.0.0.1
24f5cd23-bc13-4e21-9f6d-dd8664d0d7ae	2026-04-20 09:19:39.968072	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/discapacidades	127.0.0.1
1d20ba25-8140-49a7-8618-8e23f6b4fb0a	2026-04-20 09:19:43.136718	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/foto	127.0.0.1
dedc53db-1b3f-4caa-ba4b-0cd59e6e386a	2026-04-20 09:19:43.150514	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/discapacidades	127.0.0.1
3c0fcd22-b3dc-429e-a39f-ad92c9cb3386	2026-04-20 09:19:54.186496	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A	127.0.0.1
11a0f1b8-900a-4ecd-937b-08b48a88c382	2026-04-20 09:19:54.245312	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A	127.0.0.1
bab2c618-f53d-42af-b0af-946f61a3c504	2026-04-20 09:19:54.263084	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/foto	127.0.0.1
1076ea17-f917-4849-b634-932e2d94a301	2026-04-20 09:19:54.274598	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/discapacidades	127.0.0.1
374622ab-b889-4d65-be0b-4dd7cb6034f6	2026-04-20 09:19:55.432049	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
b4f93d81-e971-419a-a4bf-35dfe9fc8ad8	2026-04-20 09:21:54.417932	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	127.0.0.1
f2f8bdb6-fe16-49da-bb37-f05a08fa7884	2026-04-20 09:21:54.620403	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
a215980c-72f0-4a63-b08d-4000da422e53	2026-04-20 09:22:13.981924	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
8aa7d154-5f18-4205-ac17-02dee2b3d0fb	2026-04-20 09:35:27.121452	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	127.0.0.1
3c9772b1-d6e6-4109-b162-b069e56fc96d	2026-04-20 09:35:27.358687	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
ada5a7ec-c56a-46f8-9e3f-bcda5c503e87	2026-04-20 09:35:36.373824	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
2b86f33b-841d-42b0-bf4a-aeb92ae36dc7	2026-04-20 09:35:55.826624	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A	127.0.0.1
f89b9c63-490c-418c-803e-a926cb88a1fb	2026-04-20 09:35:55.846791	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/foto	127.0.0.1
860e07aa-0c1d-4b69-920b-b9668d2e7965	2026-04-20 09:35:55.863134	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/discapacidades	127.0.0.1
4fa7f458-d4a1-44a0-8038-d368363d2418	2026-04-20 09:36:00.425721	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/foto	127.0.0.1
991f2e15-343a-4aad-8df7-8d0a991b7e35	2026-04-20 09:36:00.438606	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/discapacidades	127.0.0.1
a90e1710-4aec-46e0-9333-74d6408fb7b6	2026-04-20 09:36:06.672026	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A	127.0.0.1
5202aacc-4e82-4f09-93f6-13111854f19f	2026-04-20 09:36:06.704841	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A	127.0.0.1
b75d94a8-2051-4c27-8993-57d2c78e7d46	2026-04-20 09:36:06.722324	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/foto	127.0.0.1
613916a5-61e6-4acb-b575-eab3122e8a81	2026-04-20 09:36:06.735089	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/discapacidades	127.0.0.1
1ff82664-83e8-4ad0-9c65-48b1fd708d3d	2026-04-20 09:36:07.665449	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
009e2937-2ac2-41b6-aaa1-52d44061199d	2026-04-20 09:38:49.121293	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A	127.0.0.1
1342d0c9-866e-44d0-872c-efe70d1497e1	2026-04-20 09:38:49.14724	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/foto	127.0.0.1
7b96a03b-a050-4162-b78f-c37c81391d04	2026-04-20 09:38:49.162196	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/discapacidades	127.0.0.1
1ea6c9cd-decb-4c28-9cd3-6b8874a4ee2a	2026-04-20 09:38:51.858043	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/foto	127.0.0.1
e794b63b-13fc-48f1-a2a1-b5b369a2c302	2026-04-20 09:38:51.873093	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/discapacidades	127.0.0.1
9bbf9c68-4c15-4424-b2a7-27589a04d8f2	2026-04-20 09:38:56.988233	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A	127.0.0.1
5d89aa49-a715-4a11-8ad9-11be737f3979	2026-04-20 09:38:57.02379	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A	127.0.0.1
dca8caf0-4175-448b-b8ec-61f9740a21a2	2026-04-20 09:38:57.045935	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/foto	127.0.0.1
c62a6e27-659b-4e82-95e2-3557f495b18d	2026-04-20 09:38:57.059871	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/discapacidades	127.0.0.1
49ac02b3-fed7-4405-9dc8-4db842452f4f	2026-04-20 09:38:57.713711	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
d5735dcf-6d2f-4d94-bf1f-e767e2cd3df7	2026-04-20 09:57:56.83346	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	127.0.0.1
291b0a57-438b-4de9-a5ee-4e8abf855913	2026-04-20 09:57:57.040429	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
6447ef4a-a295-4846-b60f-9ae9f0bf1cb4	2026-04-20 09:57:59.4342	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
99db427c-cbc7-4a32-8aa4-0b937186ec5d	2026-04-20 10:12:56.289019	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	127.0.0.1
b5ab460d-4aa1-4b23-afeb-3ca643cc1370	2026-04-20 10:12:56.693663	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
01fb73bf-e752-4761-9ce3-0c5679fdb3a7	2026-04-20 10:12:59.324589	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
42125cff-d14f-4381-b0b4-92426c911205	2026-04-20 10:13:05.0566	ADMIN0000	\N	CREATE	cita	00000003A-ADMIN0000-2026-04-21-09:00	Cita creada	127.0.0.1
3b812335-5826-4465-ac93-5eb3ce40ccbd	2026-04-20 10:13:26.32143	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A	127.0.0.1
bd01645b-636d-4c76-ad8e-2f513411ec0a	2026-04-20 10:13:26.35932	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/foto	127.0.0.1
e7becf34-74a7-4b12-b8a9-2a3c4fad42a2	2026-04-20 10:13:26.383215	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/discapacidades	127.0.0.1
459eb9ea-f831-43fa-bd65-169a56f1a63b	2026-04-20 10:13:29.13921	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/foto	127.0.0.1
e67b4ab3-e89b-4e05-aab3-a536efaee0eb	2026-04-20 10:13:29.158243	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/discapacidades	127.0.0.1
d8cce78c-2c4f-441c-ad4b-7a608f221aaa	2026-04-20 10:13:35.465976	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A	127.0.0.1
3037ae18-dfd4-4b79-8e1b-0b3bfe8f1590	2026-04-20 10:13:35.514396	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A	127.0.0.1
2ff44cc6-a1d6-44a3-b116-7e1c71400196	2026-04-20 10:13:35.542983	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/foto	127.0.0.1
3cdca6e6-8f65-4c75-a993-b3f04cfcdad2	2026-04-20 10:13:35.563395	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/discapacidades	127.0.0.1
e1c4f698-3e31-40fe-97b8-956a978063bc	2026-04-20 10:13:36.338187	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
d0c908ae-2723-46b6-bf3f-d2039e03a5a7	2026-04-20 13:36:30.389837	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	127.0.0.1
6db1da19-bb11-41cf-a2c3-7907262befbc	2026-04-20 13:36:30.582524	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
b31a41c5-1e7d-4a92-96c8-15a758274f76	2026-04-20 13:36:33.511294	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
cc2abf46-2e38-44d2-aac7-998989b20cba	2026-04-20 13:36:36.926404	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A	127.0.0.1
adb416e2-b073-4ddf-b5aa-33468077740f	2026-04-20 13:36:36.97955	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/foto	127.0.0.1
bec4e7b4-a9ed-497b-85d0-d4c5bef652ab	2026-04-20 13:36:37.006048	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/discapacidades	127.0.0.1
5defa22b-6d96-4dbc-a470-7676eca7b3aa	2026-04-20 13:36:41.585959	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/foto	127.0.0.1
7ebe4f40-cc6c-4c65-88a7-316987f0064c	2026-04-20 13:36:41.604153	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/discapacidades	127.0.0.1
cae1e832-c172-43d5-a08b-20e8ef0c89e8	2026-04-20 13:36:50.383315	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A	127.0.0.1
e1e42b59-63da-4ae8-8101-017c35c0f3cb	2026-04-20 13:36:50.44125	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A	127.0.0.1
805dfca8-bd81-406a-baca-510e58db296b	2026-04-20 13:36:50.460613	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/foto	127.0.0.1
362d0386-c7e8-4351-a518-6c7cc79c81c7	2026-04-20 13:36:50.472897	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/discapacidades	127.0.0.1
a15daa02-ff44-4b52-a84a-4f896437609c	2026-04-20 13:36:52.536559	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
7877486a-6185-41ee-9d8c-8ff016deb5ce	2026-04-20 13:59:22.180313	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	127.0.0.1
45c63dd1-96c2-46ad-b80c-4e609ef6bf5c	2026-04-20 13:59:22.383603	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
d6b52207-a25f-493c-9e4a-2fc72c4a832f	2026-04-20 14:01:23.213834	ADMIN0000	\N	CREATE	sanitario	12345678T	Sanitario creado	127.0.0.1
00f326e9-a840-4130-ac57-54535c03d22d	2026-04-20 14:02:04.142454	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A	127.0.0.1
ce311a44-44fb-45c9-be21-b750c879c5e4	2026-04-20 14:02:04.164862	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/foto	127.0.0.1
280599cb-6e0f-408b-b25b-314b22593d2c	2026-04-20 14:02:04.179513	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/discapacidades	127.0.0.1
76ab2e9e-01f9-4632-b45c-6be1c2a57076	2026-04-20 14:02:07.17368	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/foto	127.0.0.1
cc2289f2-826b-4ad0-8087-39b6afc47923	2026-04-20 14:02:07.21469	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/discapacidades	127.0.0.1
39a90fd6-bd90-4d9b-a2ba-a7afc29887b4	2026-04-20 14:02:16.044823	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A	127.0.0.1
800f91c2-2e16-44a3-a745-5d001158435b	2026-04-20 14:02:16.100114	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A	127.0.0.1
1ac9e49b-0d2d-4e0e-9f14-7f3755a59c08	2026-04-20 14:02:16.11664	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/foto	127.0.0.1
6befd9ff-6194-4ffe-ae16-6409932122f5	2026-04-20 14:02:16.127773	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/discapacidades	127.0.0.1
70cd23e9-fa3f-4af5-9140-bf5ed634c76a	2026-04-20 14:02:18.041001	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
653acbcf-ac69-47da-b268-4b488b38e5f1	2026-04-20 14:02:21.628881	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
285da984-3b27-46a7-a7d8-62878bb97ec4	2026-04-20 14:03:46.665204	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A	127.0.0.1
839536b1-0acb-4547-8193-cf846a571013	2026-04-20 14:03:46.711066	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/foto	127.0.0.1
7f997b66-edaf-4d5a-8da2-0ced81424145	2026-04-20 14:03:46.722967	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/discapacidades	127.0.0.1
26d69910-4a1b-47c5-b37f-2c73c3e45727	2026-04-20 14:04:01.244001	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/foto	127.0.0.1
1ca3b032-2330-4a79-ae4b-b360931143b2	2026-04-20 14:04:01.258513	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/discapacidades	127.0.0.1
215505b9-b797-438e-817f-7ed204d0a88b	2026-04-20 14:05:32.405284	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A	127.0.0.1
9ae43875-1171-4576-aae0-abcb82dd5701	2026-04-20 14:05:32.432762	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A	127.0.0.1
52a8d0ec-5c9c-447d-adf5-62246096320c	2026-04-20 14:05:32.447353	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/foto	127.0.0.1
a719d935-7b5b-409f-bf3f-dbd0113c9e22	2026-04-20 14:05:32.457358	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/discapacidades	127.0.0.1
3d56de5a-298c-4de7-b05b-466bf8d86b52	2026-04-20 14:05:33.911961	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
9026d8dc-6afd-426c-9dcd-cb2ef0621b54	2026-04-20 19:19:39.686828	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
0ce442ee-9a70-484c-9c3a-e86c4dee0b47	2026-04-20 19:19:57.860358	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
db7e9c05-f12e-4b55-91b3-2d4b6788126d	2026-04-20 19:19:57.899076	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	0:0:0:0:0:0:0:1
940ee85d-d0ce-4bb7-bdba-877702b6445f	2026-04-20 19:21:14.710082	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
38666d26-89af-4af7-a043-a90541ac2cd5	2026-04-20 19:21:14.773518	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	0:0:0:0:0:0:0:1
53e22dd5-13f7-4e03-8cd6-55846f7e00e9	2026-04-20 19:23:39.970168	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
be69bdd8-5a39-481c-8b2a-320cb4a3500e	2026-04-20 19:23:40.002459	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	0:0:0:0:0:0:0:1
65f5e724-3ea5-448a-ba1c-99e3346298ef	2026-04-20 19:27:49.508751	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
7c4959c1-a102-4bef-a3cd-ef2ca6b56da3	2026-04-20 19:27:49.571883	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	0:0:0:0:0:0:0:1
6917edba-cfbb-43fe-892e-562de2412778	2026-04-20 19:27:49.617138	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A	0:0:0:0:0:0:0:1
79cd549c-39ae-4342-b283-29d4ee24c378	2026-04-20 19:28:08.219065	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
ed1251e5-95ec-419a-be67-ffcd5f939cf5	2026-04-20 19:28:08.25121	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A	0:0:0:0:0:0:0:1
41e230c4-82c2-4d73-9906-8bc0f6581729	2026-04-20 19:28:48.450409	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
d2ea11f1-bc12-484b-8b20-2388fd9f2e70	2026-04-20 19:32:17.921997	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
988a3794-65bd-4f19-a5b3-c11d8fa2347b	2026-04-20 19:32:45.23188	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
27ddf6d4-0148-4c23-8a1e-d01c158ea843	2026-04-20 19:33:56.161458	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
70eb389c-571c-4a47-a610-9bafe0701fbe	2026-04-20 19:35:22.283292	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
9fd5f6f6-e195-4b2f-82c2-f85717893c9e	2026-04-20 19:38:02.905144	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
b4e6b8a6-a1bd-48a3-8498-13f21b788644	2026-04-20 19:38:02.987845	ADMIN0000	\N	UPDATE	paciente	00000003A	Paciente actualizado	0:0:0:0:0:0:0:1
6185c989-8477-4077-be78-c2fa7852a594	2026-04-20 19:38:29.011771	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
6a958207-cc84-4981-9ad0-133cb29230e4	2026-04-20 19:38:29.060695	ADMIN0000	\N	UPDATE	paciente	00000003A	Paciente actualizado	0:0:0:0:0:0:0:1
46ab9e19-7e59-4754-a73e-ba706ba482b9	2026-04-20 19:40:07.245225	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
a6719025-1ada-4a96-ad7a-79a8722e4b95	2026-04-20 19:40:36.170154	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
49c9ee02-7d4d-4edd-9555-55e6852be974	2026-04-20 19:40:36.72156	ADMIN0000	\N	CREATE	sanitario	TEST0001X	Sanitario creado	0:0:0:0:0:0:0:1
ab186277-aa5a-4665-849e-518adde6dc3f	2026-04-20 19:41:57.78984	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
0ac947f5-cc9c-4b6f-ac33-30569688affd	2026-04-20 19:44:11.422668	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
e6c4b1dd-5939-4202-a7a9-38768ca1cdaa	2026-04-20 19:44:26.595	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
53c71ee8-fcb9-403a-bff5-556430283bb2	2026-04-20 19:44:45.772487	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
76f5c845-f3b1-4b1a-b83e-e2301d5668cc	2026-04-20 19:44:45.863602	ADMIN0000	\N	CREATE	paciente	88888888X	Paciente creado	0:0:0:0:0:0:0:1
d23f18bd-1c48-4f50-b5fc-ed7925541dd1	2026-04-20 19:45:24.616267	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
0f3614ab-e73d-4fba-893e-66899d41e68f	2026-04-20 19:45:24.681314	ADMIN0000	\N	SOFT_DELETE	paciente	88888888X	Paciente dado de baja	0:0:0:0:0:0:0:1
9d1d426f-3fc3-4387-9c71-aa5e2ceaa3b0	2026-04-20 19:45:24.715552	ADMIN0000	\N	SOFT_DELETE	sanitario	TEST0001X	Sanitario dado de baja	0:0:0:0:0:0:0:1
4aba2213-338e-45e1-897f-efb3b34af16d	2026-04-20 19:45:55.123873	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
b6cd321e-1f25-48ac-acd6-cef5e787c62b	2026-04-20 19:45:55.16086	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/discapacidades	0:0:0:0:0:0:0:1
71afb314-ba1f-42b3-a88a-98a2781c5391	2026-04-20 19:45:55.207595	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/tratamientos	0:0:0:0:0:0:0:1
7d19f50e-5664-4a3e-9b03-adb57fc29619	2026-04-20 19:46:04.047158	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
7b05086a-b3d7-4e9b-a8cd-5421c25e341f	2026-04-20 19:46:21.901016	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
8ccad983-4909-48a1-9a86-0c4f794c3947	2026-04-20 19:46:39.848145	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
ef766dcc-fab6-479c-8e6f-42fcc726188b	2026-04-20 19:46:39.878559	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/discapacidades	0:0:0:0:0:0:0:1
e9e0be65-eb33-4738-8316-f0513adb5793	2026-04-20 19:46:39.925765	ADMIN0000	\N	CREATE	paciente_discapacidad	00000003A-DIS003	Discapacidad asignada al paciente	0:0:0:0:0:0:0:1
6579e869-7a10-4c14-8bf9-615713d71532	2026-04-20 19:46:51.419278	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
590c2626-c041-4e9d-a5da-dfc968c3f81d	2026-04-20 19:46:51.460489	ADMIN0000	\N	UPDATE	paciente_discapacidad	00000003A-DIS003	Nivel de progresión actualizado a: 2	0:0:0:0:0:0:0:1
4dd8766d-79d3-4677-9faa-84b31e13b017	2026-04-20 19:47:00.959441	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
07f5e539-ca2b-48b0-82e5-90c47681429d	2026-04-20 19:47:01.006276	ADMIN0000	\N	CREATE	paciente_tratamiento	00000003A-TRT003	Tratamiento asignado al paciente	0:0:0:0:0:0:0:1
9575f5f8-2654-47e3-b692-0acdb29a0e68	2026-04-20 19:47:01.034116	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/tratamientos	0:0:0:0:0:0:0:1
75243ba5-5a62-451f-a9e7-24309bfc1958	2026-04-20 19:47:08.935912	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
20ea7aa3-c23e-4033-b4ca-420650c7adcf	2026-04-20 19:47:08.975497	ADMIN0000	\N	UPDATE	paciente_tratamiento	00000003A-TRT003	Visibilidad de tratamiento cambiada a: false	0:0:0:0:0:0:0:1
cd57ec9b-ff55-43a3-bb15-66fe00302a85	2026-04-20 19:49:14.676827	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
4b81184c-d8a8-4982-a074-88aa7ca957d2	2026-04-20 19:49:14.856149	ADMIN0000	\N	CREATE	paciente_discapacidad	00000003A-DIS003	Discapacidad asignada al paciente	0:0:0:0:0:0:0:1
152e9178-06e5-46d4-bea4-5bbf952e1afd	2026-04-20 19:49:14.933162	ADMIN0000	\N	CREATE	paciente_tratamiento	00000003A-TRT003	Tratamiento asignado al paciente	0:0:0:0:0:0:0:1
64548bb9-9582-49e5-98c2-32e6c28d6341	2026-04-20 19:49:14.988113	ADMIN0000	\N	DELETE	paciente_tratamiento	00000003A-TRT003	Tratamiento desasignado del paciente	0:0:0:0:0:0:0:1
4d60c308-fe71-48d8-9b9a-befc634f75ae	2026-04-20 19:49:15.029831	ADMIN0000	\N	DELETE	paciente_discapacidad	00000003A-DIS003	Discapacidad desasignada del paciente	0:0:0:0:0:0:0:1
660c8df3-6e3a-4cb1-8cc8-6ba7d34cb754	2026-04-24 00:04:30.212077	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	127.0.0.1
d1f4b2b4-124e-4719-97a5-454931d464eb	2026-04-24 00:04:30.456309	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
1aa15617-14a0-4ccd-b815-2151c003a84f	2026-04-24 00:04:38.570955	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
b1edefd5-82ec-4d33-9eb8-44b885de7ff1	2026-04-24 00:05:00.720776	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A	127.0.0.1
bbf9f7ea-2331-40fc-9111-d47f1d6614ad	2026-04-24 00:05:00.744015	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/foto	127.0.0.1
25266faf-e548-4155-9c2b-bc0dc553d1a2	2026-04-24 02:17:04.773675	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	127.0.0.1
4c1cd59a-d015-4f23-b1a3-c4a9243ca871	2026-04-24 02:17:04.993596	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
843047a5-5e05-4ab3-a16b-6337a57826e2	2026-04-24 02:17:07.172019	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
9ed46f36-d370-48e2-b9b3-5583f51b0038	2026-04-24 02:17:46.147198	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
34800364-d98d-407f-8040-eae3471181e1	2026-04-27 15:06:43.981437	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	127.0.0.1
c95c2f96-f559-4759-941f-757dbae4f09e	2026-04-27 15:06:44.206053	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
7b6dae22-c352-4726-bbef-49799ba49ee1	2026-04-27 15:11:14.482443	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
691bce30-43a7-4f1f-913a-440582edb6ff	2026-04-27 15:16:37.197002	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A	127.0.0.1
9c8ae59d-62c2-4c48-9fb3-fd5151a951f3	2026-04-27 15:16:37.218605	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/foto	127.0.0.1
3aca1303-82cc-4e9d-aba0-743e1239fa12	2026-04-27 15:16:37.233802	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/discapacidades	127.0.0.1
7c77edd7-0b3d-41f5-9a17-32035af25b02	2026-04-27 15:17:00.134533	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A	127.0.0.1
94678a54-1f25-43a2-a9a1-61dd809a6ce6	2026-04-27 15:17:00.15274	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/foto	127.0.0.1
2f616bd2-9615-476c-9ffb-60cd7c9c9c72	2026-04-27 15:17:00.16505	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/discapacidades	127.0.0.1
106f871a-1a15-4cb8-b001-101a44064487	2026-05-07 12:53:07.487251	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	127.0.0.1
b95191be-d0e6-4786-84fc-dab4cd988eb3	2026-05-07 12:53:07.692509	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
e49f79a3-aeb7-408a-96d4-aa7c7ee90c05	2026-05-07 12:53:09.932707	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
a55705bc-7862-4aaa-b22d-0a20d067bdce	2026-05-07 12:53:10.777151	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
15f3098f-646e-4b57-820d-41da56e7bbda	2026-05-07 12:53:11.524079	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
f65441fc-7c47-458a-8787-3f8ea30a300e	2026-05-07 12:53:11.815278	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
5ae09443-5f83-4cb2-bc59-0e16b9771b17	2026-05-07 12:53:19.235134	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
c6ac52a3-abba-426b-ab81-c3d2eee20a97	2026-05-07 12:54:02.136402	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
0ea3faa7-0c06-4965-9a8d-b2765c230c65	2026-05-07 13:30:14.656709	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	127.0.0.1
a86528d5-1233-4000-8230-af5c706c29b9	2026-05-07 13:30:14.8353	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
22abee94-15e5-41d4-938c-70ae3136090c	2026-05-07 13:30:23.430478	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
3820ebd7-6b68-463c-9ac6-aa7776db01cd	2026-05-07 13:34:33.234831	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	127.0.0.1
89b7fa60-8653-4a7a-b7e9-04069ec0b861	2026-05-07 13:34:33.423585	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
4251cd44-4682-49ee-8a89-ff85937e9e69	2026-05-07 13:38:57.89164	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
845c88a8-9e5b-469a-a2f2-a7f2c7ceeec8	2026-05-07 13:38:57.915171	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	0:0:0:0:0:0:0:1
2811e597-c63b-452d-805b-c85299f0e3de	2026-05-07 13:39:25.233533	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
294e26ff-3ce1-4463-9754-2a40c990e570	2026-05-07 13:39:25.256821	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	0:0:0:0:0:0:0:1
f75676f1-0064-4edc-afe5-33249d22fca2	2026-05-07 13:40:02.259681	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
af324f7c-a9de-4892-ad68-cfbbf265e0aa	2026-05-07 13:40:02.283834	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	0:0:0:0:0:0:0:1
9f06f566-e469-4e4f-84a6-af49df3e1e14	2026-05-07 13:43:16.04277	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
d96012cc-717e-4e8e-91a2-1aec7d980cc1	2026-05-07 13:43:16.066802	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z	0:0:0:0:0:0:0:1
e9f114ab-9770-4eb5-8db9-9464570357ae	2026-05-07 13:43:23.033515	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
d035c62b-98a5-43f9-bfe2-ba21f9871724	2026-05-07 13:44:01.047365	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
626c5703-61e6-4e4c-9b93-972d041a9f88	2026-05-07 13:44:01.070884	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	0:0:0:0:0:0:0:1
dd595fb3-8f36-41f5-978e-56ce94c1da80	2026-05-07 13:44:59.988381	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
fb26a4db-70f9-48a4-a2a5-4ad481e7c869	2026-05-07 13:45:00.012483	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	0:0:0:0:0:0:0:1
2a832187-1d33-4cbb-acee-278b788db03e	2026-05-07 13:45:00.034889	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	0:0:0:0:0:0:0:1
4a4792f4-6e10-4b7b-9d97-4b8df458b32a	2026-05-07 13:45:11.626376	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
c307f67e-d85f-4ddb-9804-f5b3c9d93b9a	2026-05-07 13:45:11.64978	ADMIN0000	\N	READ	paciente	buscar	Lectura via HTTP: /api/pacientes/buscar	0:0:0:0:0:0:0:1
7b6852dd-5cf5-4fa6-b685-eaeaca9a5152	2026-05-07 13:45:23.717225	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
2939fbf7-803a-4c78-84bc-02a077005d82	2026-05-07 13:45:40.393764	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
a7325c68-dbf7-424c-af6f-74b3c4dc0290	2026-05-07 13:45:40.418291	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	0:0:0:0:0:0:0:1
297855de-088d-4ad6-8c3d-e9c409a5cd00	2026-05-07 13:59:13.944705	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	127.0.0.1
45d1e6e2-8904-4107-9020-66f900fd7fac	2026-05-07 13:59:14.152741	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
d549e7a9-5304-4807-9579-9e8635f657d0	2026-05-07 13:59:16.962904	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
9713316b-5031-4a2f-b892-aeb1feb5cb25	2026-05-07 18:39:44.765325	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
1fae3f95-6e7a-411a-9e70-4c9ed1c7829d	2026-05-07 18:39:44.837859	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	0:0:0:0:0:0:0:1
0d5a9d3a-084e-480c-a362-20363116f975	2026-05-07 18:48:01.559562	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
255a2296-d746-4e1b-aa99-22340a524c23	2026-05-07 18:48:01.658083	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	0:0:0:0:0:0:0:1
ef14444b-315d-4603-95e7-bf9df557ca29	2026-05-07 18:51:25.409179	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	127.0.0.1
25609ac0-b991-4135-b629-01b4be056ec1	2026-05-07 18:51:25.609488	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
1ed97054-87f5-4ee5-bf85-41efc9e0f0ea	2026-05-07 18:51:27.824585	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
b170b7d8-1331-4681-b3d1-6ea9dc3d4dd2	2026-05-07 20:07:31.250104	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
4e534064-1896-480c-ba7a-776327b3881d	2026-05-07 20:07:31.276798	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z	0:0:0:0:0:0:0:1
4f7cbb44-2db4-4ac6-95db-13f8b4e35dc6	2026-05-07 20:07:31.307632	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/discapacidades	0:0:0:0:0:0:0:1
d8369f09-7b44-4f1e-afaa-98d07a9e37ed	2026-05-07 20:07:31.333852	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/foto	0:0:0:0:0:0:0:1
082ba44a-c991-4cf4-ad13-2d9f19d3abb0	2026-05-07 20:08:15.101705	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
b0f3b0d2-dc88-48e7-9895-a33a4fcb59b4	2026-05-07 20:08:15.458088	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
0cf16a96-71f1-4d3a-99fb-a2059ad1d5d4	2026-05-07 20:11:02.117246	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
0a09d05f-da5f-42d2-8c96-153aa2b99724	2026-05-07 20:11:02.484743	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
27ab4aae-9491-4d7d-ae9c-1b1a2db0d14e	2026-05-07 20:11:02.537341	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z	0:0:0:0:0:0:0:1
a3a8b45f-c1d7-4be3-8b40-5e979a2168fb	2026-05-07 20:11:02.555271	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/discapacidades	0:0:0:0:0:0:0:1
19cd4ddf-8434-4249-a653-7d6d309e6ec2	2026-05-07 20:11:02.569758	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/tratamientos	0:0:0:0:0:0:0:1
bf954507-3561-4ef2-8249-edce499b6b38	2026-05-07 20:11:02.600666	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/foto	0:0:0:0:0:0:0:1
d558850c-22c3-4820-ae21-e6bd0ef56bd6	2026-05-07 20:11:33.627965	00000002W	Lucia Martinez	LOGIN	sanitario	00000002W	Login exitoso	0:0:0:0:0:0:0:1
606d6c19-1fa1-4982-b609-ab5277b644ba	2026-05-07 20:11:33.695153	00000002W	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z	0:0:0:0:0:0:0:1
3b7f5909-436f-4b13-a73e-5b994d4d8f9d	2026-05-07 20:11:33.716357	00000002W	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/discapacidades	0:0:0:0:0:0:0:1
d437cec7-20fd-4567-b183-f2c9691bfe2a	2026-05-07 20:11:33.73505	00000002W	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/tratamientos	0:0:0:0:0:0:0:1
d782fc5c-8efc-46d8-a4c5-fdc03d4f8f84	2026-05-07 20:11:33.752906	00000002W	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/foto	0:0:0:0:0:0:0:1
dca04159-7dc6-47dd-88a6-36bc47f23689	2026-05-07 20:49:49.492814	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	127.0.0.1
7109140e-fb81-4c1a-8899-0c693a036bd0	2026-05-07 20:49:49.657969	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
20a60277-9034-489f-a897-628d5cef39fd	2026-05-07 20:49:59.778127	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A	127.0.0.1
2ba2ef54-8fe2-4036-a66e-a6275e78dc49	2026-05-07 20:49:59.794796	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/foto	127.0.0.1
11b76dc2-f443-4a04-8c1d-09bc13f1b990	2026-05-07 20:49:59.807113	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/discapacidades	127.0.0.1
e7932cd5-2e1b-4977-a8f3-74dad5710a79	2026-05-07 20:50:07.535317	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/progreso/check	127.0.0.1
3f836be4-4df0-4e0a-9b5e-5db60883e9f2	2026-05-07 20:50:07.535318	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/progreso	127.0.0.1
5d1d1cbc-d1ac-48ba-b44a-367bf763e2c9	2026-05-07 22:44:21.152377	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
359456b3-8f47-47b2-af11-3ba794876cb1	2026-05-07 22:44:21.176031	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/progreso	0:0:0:0:0:0:0:1
ff7b1b89-9eba-4375-8637-8eb2a26f28f2	2026-05-07 22:44:21.198384	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/progreso/check	0:0:0:0:0:0:0:1
dbdf74e0-f58d-49db-bc28-969c86e47c2a	2026-05-07 22:57:13.290849	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
59cfc5a2-6847-4240-a03f-38ff1509cc23	2026-05-07 22:57:13.369574	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/progreso	0:0:0:0:0:0:0:1
ae36d4f3-87c0-4612-90cf-11b3ef17d20d	2026-05-07 22:57:13.51494	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/progreso/check	0:0:0:0:0:0:0:1
9aeade99-f090-4b1e-971c-4840c0fda20f	2026-05-07 23:01:14.797786	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	127.0.0.1
089b26ff-7549-4556-8e53-f840baef6299	2026-05-07 23:01:15.00777	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
a510bd9c-a183-4c84-b801-34f2272b29dd	2026-05-07 23:01:17.938647	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A	127.0.0.1
e2b978e6-409c-41d7-99ce-d52bf678e916	2026-05-07 23:01:17.966045	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/foto	127.0.0.1
686ce01f-2c7d-4dbc-9541-2f9727b7cf4c	2026-05-07 23:01:17.983622	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/discapacidades	127.0.0.1
1621abf3-1d97-4c9c-8d86-3788a0c752b5	2026-05-07 23:01:21.813292	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/progreso	127.0.0.1
fef88d2f-907b-458b-ba57-92e87636d40d	2026-05-07 23:01:21.814419	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/progreso/check	127.0.0.1
71598058-fa8b-4f1a-9145-73b4ceb0c2ab	2026-05-07 23:01:29.003741	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/progreso	127.0.0.1
c2867c02-7a69-411e-805a-a17bb1475816	2026-05-07 23:02:56.003558	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z	127.0.0.1
d00b7fea-fa1a-4685-956b-418d442b914f	2026-05-07 23:02:56.025168	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/foto	127.0.0.1
f0f1f064-8ce8-4c98-b2b2-f41cb577b066	2026-05-07 23:02:56.039231	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/discapacidades	127.0.0.1
344d1df8-aeb0-4727-9dfa-9663763c912a	2026-05-07 23:03:01.912498	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/progreso/check	127.0.0.1
4205cdbd-2e88-4a78-9294-1abe19d1405b	2026-05-07 23:03:01.912836	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/progreso	127.0.0.1
736b1d83-64b2-4e2c-a07e-b4b8436659be	2026-05-07 23:06:18.865097	ADMIN0000	\N	CREATE	paciente	55667788Z	Paciente creado	0:0:0:0:0:0:0:1
ca969e4f-4301-4f6e-be57-95a59499f956	2026-05-07 23:10:57.331349	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso	0:0:0:0:0:0:0:1
3fb6760d-8942-4d61-967d-d15b7157d1cc	2026-05-07 23:03:03.482961	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/progreso	127.0.0.1
8f9c0735-75b0-4075-a945-17ad031f0087	2026-05-07 23:03:54.742789	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
916663b2-da5c-4b52-8ee2-60ca4d9ff98b	2026-05-07 23:04:11.063591	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
82d93539-1b6b-4714-a327-4868322bcb20	2026-05-07 23:05:22.930921	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
586b1abe-01f4-4cea-951e-8e467513b685	2026-05-07 23:05:33.071795	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
7e309ec9-c834-41bc-9327-77281ae08724	2026-05-07 23:06:18.759434	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
2357104e-9bf4-4bf1-851a-407dbaa82de5	2026-05-07 23:06:38.285812	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
ea911701-d8ae-40e8-8af6-bb20a5d69f31	2026-05-07 23:06:38.320322	ADMIN0000	\N	CREATE	paciente_discapacidad	55667788Z-DIS006	Discapacidad asignada al paciente	0:0:0:0:0:0:0:1
95d1f1a9-e72d-49f0-96af-e8cded7f2d14	2026-05-07 23:06:38.359689	ADMIN0000	\N	CREATE	paciente_tratamiento	55667788Z-TRT001	Tratamiento asignado al paciente	0:0:0:0:0:0:0:1
81098a89-2e64-4005-b164-dca66b422b58	2026-05-07 23:06:38.386242	ADMIN0000	\N	CREATE	paciente_tratamiento	55667788Z-TRT002	Tratamiento asignado al paciente	0:0:0:0:0:0:0:1
6f33a917-cdd9-444d-b58b-50b96239ac0c	2026-05-07 23:06:38.410337	ADMIN0000	\N	CREATE	paciente_tratamiento	55667788Z-TRT003	Tratamiento asignado al paciente	0:0:0:0:0:0:0:1
ee91c386-0446-4845-9345-9061078fde19	2026-05-07 23:10:48.948666	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
d0e43502-65b5-4d0a-bbd9-a9f861fd9be3	2026-05-07 23:10:57.307687	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	0:0:0:0:0:0:0:1
0ae7b47c-9760-4cf0-8f63-05d012a9a12e	2026-05-07 23:28:30.608073	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	127.0.0.1
6a2e7d90-6248-429e-ad30-829e1a871b84	2026-05-07 23:28:30.824714	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
af05b33f-65c6-4f30-95a3-b1250ff1976c	2026-05-07 23:29:08.287128	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
c2da892a-86cc-4b61-8eab-f1d9d00e7bfb	2026-05-07 23:30:08.833044	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z	127.0.0.1
76512690-5485-4c0c-8daa-8049049e11c7	2026-05-07 23:30:08.877578	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/foto	127.0.0.1
60e7056b-9ef6-4f00-aa70-cf37b07f079d	2026-05-07 23:30:08.913366	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/discapacidades	127.0.0.1
0d2d4b73-64f7-430e-9b50-6699f9f2c65c	2026-05-07 23:30:25.799406	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/progreso	127.0.0.1
b9769fe2-f17a-4a06-8379-ac33beb42e92	2026-05-07 23:30:25.805939	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/progreso/check	127.0.0.1
74683363-4ab9-4253-b192-d1a316ac23fc	2026-05-07 23:30:30.759032	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/progreso	127.0.0.1
063c10c5-b8b6-4d4b-bb47-437d1dbaf70c	2026-05-07 23:30:34.728642	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z	127.0.0.1
2933ecc8-faed-4f96-8b04-9553ac16c4f1	2026-05-07 23:30:34.750183	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/foto	127.0.0.1
e64b7777-17ac-44ef-9224-113e0c32c617	2026-05-07 23:30:34.763525	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/discapacidades	127.0.0.1
6f576bf4-f7c0-4daa-9d86-1a8f8283e847	2026-05-07 23:30:38.37311	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso/check	127.0.0.1
bbdefa72-f74b-4fc0-8b8f-b610e68dd584	2026-05-07 23:30:38.373218	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso	127.0.0.1
b0a6fb78-9f93-4ec6-8378-e9db874fac72	2026-05-07 23:30:40.833897	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso	127.0.0.1
3c8acb9c-b9c9-47e2-b7a3-ba02a04b3aaa	2026-05-07 23:30:41.369441	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso	127.0.0.1
c5974e7c-04c3-4810-b656-cc7265cbe840	2026-05-07 23:30:41.770392	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso	127.0.0.1
c703faba-3ca8-4942-a3e1-855348646fdb	2026-05-07 23:30:42.18576	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso	127.0.0.1
ba7bb60e-28de-49b6-9e54-a27e0855b97d	2026-05-07 23:30:42.609011	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso	127.0.0.1
8e4d8d24-75d4-44f2-8755-128a9ca2e44c	2026-05-07 23:30:43.019221	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso	127.0.0.1
0dd212c6-d0cb-43ac-98d6-febe66800d7f	2026-05-07 23:30:43.425121	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso	127.0.0.1
05ad15ca-5c32-4645-a62d-30884ad4ed9d	2026-05-07 23:30:43.783442	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso	127.0.0.1
3aec8927-2569-44af-b979-2be35fec140c	2026-05-07 23:30:44.100971	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso	127.0.0.1
622cc1ec-597c-409c-825e-38ef9c4f01e1	2026-05-07 23:30:44.440917	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso	127.0.0.1
c0bdb3d5-d9b4-49f3-9e16-539f673afb51	2026-05-07 23:30:44.774643	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso	127.0.0.1
0a1c403c-ec39-451b-8309-9d75dc78be34	2026-05-07 23:30:45.184169	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso	127.0.0.1
e92248e5-02fb-43c9-bfc0-b01d457a077e	2026-05-07 23:30:45.644077	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso	127.0.0.1
6ce36691-022e-495f-9828-96659ae18f0b	2026-05-07 23:30:46.229927	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso	127.0.0.1
2ef212d7-efed-4e65-8272-3996895f9807	2026-05-07 23:30:49.667274	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A	127.0.0.1
75a06751-41f1-429b-8b25-3d062bb3f39f	2026-05-07 23:30:49.681415	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/foto	127.0.0.1
348b2fb1-d8e6-47fc-ae17-8352ba44c67a	2026-05-07 23:30:49.69361	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/discapacidades	127.0.0.1
f0ec5adf-836d-4393-bf70-11d425824d02	2026-05-07 23:30:52.512295	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/progreso/check	127.0.0.1
a5959ee2-c0b3-435b-a054-ea68722dcb1e	2026-05-07 23:30:52.512294	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/progreso	127.0.0.1
89783c75-06af-48c4-8123-4db94ecd87c5	2026-05-07 23:30:53.444277	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/progreso	127.0.0.1
b4beb94d-1d9a-46a1-91aa-75886a5e18fc	2026-05-07 23:30:53.61964	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/progreso	127.0.0.1
b0002ce1-0773-4116-819d-00eada1003cf	2026-05-07 23:30:53.798146	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/progreso	127.0.0.1
c1489746-2721-4fe7-8092-4ba78f06f6f7	2026-05-07 23:30:53.979893	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/progreso	127.0.0.1
a579afcc-6211-4506-9e45-1a926b9b011d	2026-05-07 23:30:54.146886	ADMIN0000	\N	READ	paciente	00000003A	Lectura via HTTP: /api/pacientes/00000003A/progreso	127.0.0.1
30f32fae-bc82-4ff9-8ca2-dede45fe840a	2026-05-08 00:01:10.358672	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	127.0.0.1
7bd66b6d-94cb-4b32-bd94-ded2404d7461	2026-05-08 00:01:10.601964	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
11ce4193-5960-4ba9-a77a-38cb08fcc0d0	2026-05-08 00:01:12.198144	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z	127.0.0.1
c9108cfd-295e-4b25-b64c-85ca2354cc23	2026-05-08 00:01:12.224061	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/foto	127.0.0.1
c466e49b-17bd-4440-ac6c-54a8f36fd006	2026-05-08 00:01:12.242699	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/discapacidades	127.0.0.1
606dbc44-e235-4a11-9efc-f41189b318b1	2026-05-08 00:01:14.833363	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso	127.0.0.1
6601ba0d-5291-4bd1-b2f4-e9dd30f727c8	2026-05-08 00:01:14.83387	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso/check	127.0.0.1
a80c0fb6-1e2c-4933-889c-300898549d07	2026-05-08 00:01:16.440697	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso	127.0.0.1
31b983bb-ac44-46d1-bc3c-16a7576b54f7	2026-05-08 00:02:50.418518	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z	127.0.0.1
59aa77d4-ea90-46a8-b4ee-34c4a500adca	2026-05-08 00:02:50.436368	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/foto	127.0.0.1
73cec74c-6989-4dec-b779-7e396fc43c26	2026-05-08 00:02:50.446907	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/discapacidades	127.0.0.1
3ffb04a9-4695-437d-9c34-f65e1a3a1ac3	2026-05-08 00:02:52.64369	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/progreso	127.0.0.1
466ed3f9-8597-4bd7-be7b-30efeec21b61	2026-05-08 00:02:52.644116	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/progreso/check	127.0.0.1
e766a99b-94f2-4d28-a79c-9077743bc1a9	2026-05-08 00:03:00.118225	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/progreso	127.0.0.1
5a732e05-a6d6-4b71-8e65-0e78d063dbdf	2026-05-08 00:03:01.857816	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/progreso	127.0.0.1
375ea761-413a-42d6-827c-f691ae66c6aa	2026-05-08 00:03:02.17205	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/progreso	127.0.0.1
496a95c6-8941-478e-99f8-ff83659c67b1	2026-05-08 00:03:02.361073	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/progreso	127.0.0.1
3f614641-e536-4530-ada9-bd45b6ad7b76	2026-05-08 00:03:02.556411	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/progreso	127.0.0.1
1534bb64-9a61-4c80-9614-232286fb7cdc	2026-05-08 00:03:02.749417	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/progreso	127.0.0.1
5ce6e793-576e-40a0-8ba4-87c8a7925dc9	2026-05-08 00:03:02.972447	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/progreso	127.0.0.1
673d6818-2fc2-47cb-93d0-909c55bdc4d6	2026-05-08 00:08:47.837945	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z	127.0.0.1
ff56d8a2-a88e-40e8-898c-775b64137326	2026-05-08 00:08:47.870376	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/foto	127.0.0.1
2a993762-dd4d-474a-a15e-afa19c73e2a4	2026-05-08 00:08:47.883267	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/discapacidades	127.0.0.1
190a5ea1-ddd7-497c-a92e-a22b36e96f1b	2026-05-08 00:08:52.081575	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso/check	127.0.0.1
5cc97d3e-5bba-41d7-91e3-bb39e089f73d	2026-05-08 00:08:52.08159	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso	127.0.0.1
7ec244ca-a686-4fe9-9931-787a3f174cf0	2026-05-08 00:08:54.618234	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso	127.0.0.1
6472288f-e45e-49e1-a510-edd9bf9fbcc9	2026-05-08 00:08:54.839129	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso	127.0.0.1
98f8acc8-c369-4b9d-a4f8-879a263f79b7	2026-05-08 00:08:55.078101	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso	127.0.0.1
7ec97c89-f8e7-4109-95f1-b1e5b5bb69b4	2026-05-08 00:08:55.29565	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso	127.0.0.1
0d4452cc-4717-4158-b5f8-5e03dfa55294	2026-05-08 00:08:55.468346	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso	127.0.0.1
f15b9db5-5734-4f09-8f82-6969b4c7c2eb	2026-05-08 00:09:45.626689	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z	127.0.0.1
859ef3b0-8588-4d86-a21f-ae1448733b27	2026-05-08 00:09:45.641297	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/foto	127.0.0.1
8c017f87-d1f9-4775-b123-6738c963852f	2026-05-08 00:09:45.650253	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/discapacidades	127.0.0.1
ca78e58f-b699-49bc-868e-86159e76c0d8	2026-05-08 00:12:49.428972	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
68d97de0-128a-4324-9e26-d7c3bc0bd4a8	2026-05-08 00:26:10.506567	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	127.0.0.1
265af466-b1e9-4909-9f2e-4a96d74d3891	2026-05-08 00:26:10.728039	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
02af3ca5-81fa-4d52-8d13-1da7cfec02bf	2026-05-08 00:27:48.440035	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z	127.0.0.1
e0d87e3f-7a28-4b83-9843-cc801e3ca8c2	2026-05-08 00:27:48.469161	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/foto	127.0.0.1
675d8984-b102-4ff4-ae23-46c9c24d15b7	2026-05-08 00:27:48.485176	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/discapacidades	127.0.0.1
6aaaf0be-1e23-48bb-816e-7b314d5664e6	2026-05-08 00:27:54.154376	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso	127.0.0.1
116c5890-d380-458d-a3f6-4c10f9317bc7	2026-05-08 00:27:54.157695	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso/check	127.0.0.1
034f0d6a-8f9c-4524-8d43-95d7124fb84f	2026-05-08 00:28:01.288037	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso	127.0.0.1
20fea3b2-624b-41c8-a8bf-342514893bff	2026-05-08 00:28:02.495875	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso	127.0.0.1
f700b6ea-89a4-4d15-bf1d-2fe0f6762799	2026-05-08 00:28:02.676836	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso	127.0.0.1
c1ab512c-c616-4156-97c5-b4aa61912da2	2026-05-08 00:29:31.049181	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z	127.0.0.1
e28274f5-6e21-4887-9f8e-ef1e9ecc520e	2026-05-08 00:29:31.067574	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/foto	127.0.0.1
81b79e4d-649a-4bb2-a3ec-adc751f546eb	2026-05-08 00:29:31.078847	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/discapacidades	127.0.0.1
b12a345b-2fe8-4fa5-8ca2-ce5483ab55e5	2026-05-08 00:29:34.79238	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso	127.0.0.1
7e2a1778-5b90-4924-835c-204453db29b1	2026-05-08 00:29:34.792381	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso/check	127.0.0.1
ad68ce41-c999-44f7-8a37-970044a2ac6c	2026-05-12 19:35:32.925925	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	127.0.0.1
475facbe-8530-42b6-88a4-d89e4d46a221	2026-05-12 19:35:33.214887	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
3cd30d78-20db-4cb2-b03a-74ec7acfc540	2026-05-12 19:35:36.186165	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
42d0d9cc-b619-411f-99d3-e96ace5919a1	2026-05-13 01:10:59.433492	55667788Z	Juan Benito	LOGIN	paciente	55667788Z	Login movil exitoso	0:0:0:0:0:0:0:1
032cf8db-a351-415d-bfaa-36fd96ca5449	2026-05-13 01:21:00.068619	55667788Z	Juan Benito	LOGIN	paciente	55667788Z	Login movil exitoso	0:0:0:0:0:0:0:1
c313e312-f39a-486c-b9e0-ae86db42a598	2026-05-13 01:21:54.910559	55667788Z	Juan Benito	LOGIN	paciente	55667788Z	Login movil exitoso	0:0:0:0:0:0:0:1
5a8b69a1-3c9a-4422-845f-8a46f90cd340	2026-05-13 01:23:28.337366	55667788Z	Juan Benito	LOGIN	paciente	55667788Z	Login movil exitoso	0:0:0:0:0:0:0:1
c4cf224e-6996-4e1a-985b-88048cbba26a	2026-05-13 01:33:37.093775	55667788Z	Juan Benito	LOGIN	paciente	55667788Z	Login movil exitoso	0:0:0:0:0:0:0:1
d196b5bb-92ff-4144-bc37-1ca52733d4e2	2026-05-13 01:35:38.077268	55667788Z	Juan Benito	LOGIN	paciente	55667788Z	Login movil exitoso	0:0:0:0:0:0:0:1
3421a9b0-73cd-4786-8e42-c2d5c1f681a5	2026-05-13 01:36:36.894141	55667788Z	Juan Benito	LOGIN	paciente	55667788Z	Login movil exitoso	0:0:0:0:0:0:0:1
18ea2f66-b2be-458d-9b60-4cd49cbc4d3a	2026-05-13 01:36:39.270692	55667788Z	\N	CREATE	TelemetriaSesion	55667788Z	Ingesta de sesion de juego: PIANO-001	0:0:0:0:0:0:0:1
bd5c44ad-d157-4c4d-b4b2-2495c232da90	2026-05-13 01:37:28.256783	55667788Z	Juan Benito	LOGIN	paciente	55667788Z	Login movil exitoso	0:0:0:0:0:0:0:1
22322fb7-7ab5-441f-895e-9e52aa583021	2026-05-13 01:37:30.75299	55667788Z	\N	CREATE	TelemetriaSesion	55667788Z	Ingesta de sesion de juego: PIANO-001	0:0:0:0:0:0:0:1
38b66513-c96e-49dd-a8db-c0bb424f4301	2026-05-13 01:42:16.605608	55667788Z	Juan Benito	LOGIN	paciente	55667788Z	Login movil exitoso	0:0:0:0:0:0:0:1
32591765-6f90-4742-9052-f3fd5abd8480	2026-05-13 01:42:16.605408	55667788Z	Juan Benito	LOGIN	paciente	55667788Z	Login movil exitoso	0:0:0:0:0:0:0:1
4f2c2117-cc81-4f3f-bd7a-15bb0b02bfda	2026-05-13 01:42:16.90798	55667788Z	\N	CREATE	TelemetriaSesion	55667788Z	Ingesta de sesion de juego: PIANO-001	0:0:0:0:0:0:0:1
47be9312-3b32-43ba-98cc-da0f8c6a8627	2026-05-13 01:42:16.90798	55667788Z	\N	CREATE	TelemetriaSesion	55667788Z	Ingesta de sesion de juego: PIANO-001	0:0:0:0:0:0:0:1
75d8bfad-50b0-4625-ab7c-133db42b24b9	2026-05-13 01:59:22.118324	55667788Z	Juan Benito	LOGIN	paciente	55667788Z	Login movil exitoso	0:0:0:0:0:0:0:1
003ee22a-2ac8-40a0-84d7-148ed4891030	2026-05-13 02:00:14.290543	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	127.0.0.1
a5a3f7f1-3678-4649-86de-e4497ff1a2e5	2026-05-13 02:00:14.506671	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
716ba67e-2313-4d2d-bcda-f725002e5a5b	2026-05-13 02:00:17.73854	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z	127.0.0.1
d5c44fc2-b1db-425c-a142-a1fefe55e5a9	2026-05-13 02:00:17.757772	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/foto	127.0.0.1
042b89dd-9f51-4ed7-9bd2-2e56648372f5	2026-05-13 02:00:17.775409	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/discapacidades	127.0.0.1
e3f5470c-28d4-4f8e-88b0-768b6fe62cda	2026-05-13 02:00:21.035739	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso	127.0.0.1
4b7958c1-439e-42ca-9aff-075970df5b67	2026-05-13 02:00:21.039028	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso/check	127.0.0.1
a6f21950-34ef-46d1-a047-90e611357d43	2026-05-13 02:00:21.128734	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso	127.0.0.1
d6f6a9b7-b7a2-4822-a588-290bafece8ca	2026-05-13 02:00:28.399695	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso	127.0.0.1
6a948fb1-99f4-4e42-95b4-91c9c58972b7	2026-05-13 02:00:28.902499	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso	127.0.0.1
411064da-9a36-4e89-9c4a-240fcbfaa65a	2026-05-13 02:00:29.257245	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso	127.0.0.1
5a5d5177-f9c7-486f-bd6a-9ea3619cc4e5	2026-05-13 02:00:29.867019	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/progreso	127.0.0.1
491281ed-6529-4c17-a2b4-0ca17e9ddc8e	2026-05-13 02:00:40.308937	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z	127.0.0.1
f9cca955-81c2-49f2-8152-9ddcaeb4f539	2026-05-13 02:00:40.326919	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/foto	127.0.0.1
ae87ffc5-a22e-4e2e-970c-d13ebd2f443c	2026-05-13 02:00:40.338013	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/discapacidades	127.0.0.1
a0bc282f-4408-490f-9ad5-2e84f13aa659	2026-05-13 02:00:43.243894	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/progreso/check	127.0.0.1
01b54d03-cf02-4f29-8fde-65e5fa06d50e	2026-05-13 02:00:43.243926	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/progreso	127.0.0.1
350be005-a1da-48c3-9eaf-3537ccdf2bf0	2026-05-13 02:00:45.265422	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/progreso	127.0.0.1
d7b429f7-1d25-419c-855d-03dd8f2c8007	2026-05-13 02:00:45.719823	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/progreso	127.0.0.1
4098d9b4-64d5-43dc-a32c-dcf21662b3a4	2026-05-13 02:00:45.938662	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/progreso	127.0.0.1
069e92d6-619c-44a4-9d68-bd5880addae9	2026-05-13 02:00:46.192826	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/progreso	127.0.0.1
f1287f7f-0b92-48ac-a210-c28c233e2d2b	2026-05-13 02:00:46.4677	ADMIN0000	\N	READ	paciente	12345678Z	Lectura via HTTP: /api/pacientes/12345678Z/progreso	127.0.0.1
06cfdf4d-3bcc-401a-af18-0ef3ffce0f06	2026-05-13 02:01:34.158194	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	127.0.0.1
aedd4631-ffcf-4f07-b932-8856fdb39591	2026-05-13 02:01:34.32085	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
1fa26757-19a3-4124-9189-199836971c98	2026-05-13 02:01:37.667634	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z	127.0.0.1
cd9efd83-e7ee-46d2-bcc5-e9b500c3b667	2026-05-13 02:01:37.686138	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/foto	127.0.0.1
e1b03972-01ee-4c09-8acc-81efa9e981ef	2026-05-13 02:01:37.698626	ADMIN0000	\N	READ	paciente	55667788Z	Lectura via HTTP: /api/pacientes/55667788Z/discapacidades	127.0.0.1
50ae3d7c-e169-4511-a261-d1efeac60e77	2026-05-13 02:30:03.949026	ADMIN0000	Admin Sistema	LOGIN	sanitario	ADMIN0000	Login exitoso	127.0.0.1
05d0dc47-8e60-4945-b7c6-ee9421320590	2026-05-13 02:30:04.102474	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
8a3242c7-f139-4903-9143-373660c94e5e	2026-05-13 02:30:15.249219	ADMIN0000	\N	READ	paciente	lista	Lectura via HTTP: /api/pacientes	127.0.0.1
\.


--
-- Data for Name: cita; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.cita (dni_pac, dni_san, fecha_cita, hora, informe, fecha_creacion) FROM stdin;
00000003A	ADMIN0000	2026-04-21	09:00:00	\N	2026-04-20 10:13:05.030207
12345678Z	87654321B	2026-04-10	10:00:00	\N	2026-05-07 12:32:51.506713
12345678Z	87654321B	2026-04-17	11:30:00	\N	2026-05-07 12:32:51.506713
12345678Z	87654321B	2026-04-24	09:00:00	\N	2026-05-07 12:32:51.506713
\.


--
-- Data for Name: cita_audit; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.cita_audit (dni_pac, dni_san, fecha_cita, hora, rev, rev_type) FROM stdin;
00000003A	ADMIN0000	2026-04-21	09:00:00	2	0
\.


--
-- Data for Name: cp; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.cp (cp, nombre_localidad) FROM stdin;
23001	Jaen
28001	Madrid
\.


--
-- Data for Name: direccion; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.direccion (id_direccion, calle, numero, piso, cp) FROM stdin;
1	Calle Real	15	2A	23001
4	Calle de la Prueba	1	1A	28001
5	Calle Test	1	A	28001
8	Calle Rehabilitacion	1	\N	28001
9	Calle Mayor	42	3B	28001
\.


--
-- Data for Name: discapacidad; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.discapacidad (cod_dis, nombre_dis, descripcion_dis, necesita_protesis, id_articulacion) FROM stdin;
DIS001	Amputacion miembro inferior	Perdida total o parcial de pierna o pie	t	\N
DIS002	Amputacion miembro superior	Perdida total o parcial de brazo o mano	t	\N
DIS003	Lesion de rodilla	Rotura de ligamentos o menisco	f	\N
DIS004	Fractura de cadera	Fractura del hueso de la cadera	f	\N
DIS005	Lesion medular	Lesion de medula con deficit de movilidad	f	\N
M16	Coxartrosis	Artrosis de la articulacion coxofemoral. Degeneracion progresiva del cartilago de la cadera.	f	\N
M54	Lumbalgia cronica	Dolor cronico en la region lumbar de la columna vertebral. Duracion superior a 12 semanas.	f	\N
DIS006	Lesion de hombro	Patologia del manguito rotador y articulacion glenohumeral. Incluye tendinitis, bursitis y roturas parciales.	f	\N
REAL	Articulacion de dedos	Problemas derivados de la movilidad de los dedos,	f	\N
\.


--
-- Data for Name: discapacidad_tratamiento; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.discapacidad_tratamiento (cod_dis, cod_trat) FROM stdin;
DIS001	TRT001
DIS001	TRT002
DIS001	TRT009
DIS001	TRT003
DIS001	TRT004
DIS001	TRT005
DIS001	TRT006
DIS001	TRT007
DIS001	TRT008
DIS002	TRT001
DIS002	TRT002
DIS002	TRT010
DIS002	TRT003
DIS002	TRT004
DIS002	TRT005
DIS002	TRT006
DIS002	TRT007
DIS002	TRT008
DIS003	TRT001
DIS003	TRT002
DIS003	TRT003
DIS003	TRT004
DIS003	TRT005
DIS003	TRT006
DIS003	TRT007
DIS003	TRT008
DIS004	TRT001
DIS004	TRT002
DIS004	TRT003
DIS004	TRT004
DIS004	TRT005
DIS004	TRT006
DIS004	TRT007
DIS004	TRT008
DIS005	TRT001
DIS005	TRT002
DIS005	TRT003
DIS005	TRT004
DIS005	TRT005
DIS005	TRT006
DIS005	TRT007
DIS005	TRT008
M16	TRT001
M16	TRT004
M54	TRT002
M54	TRT003
DIS006	TRT001
DIS006	TRT002
DIS006	TRT003
DIS006	TRT004
DIS006	TRT005
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	8	<< Flyway Baseline >>	BASELINE	<< Flyway Baseline >>	\N	admin	2026-04-09 15:31:20.895007	0	t
2	9	fix envers y direccion	SQL	V9__fix_envers_y_direccion.sql	373817746	admin	2026-04-20 09:57:44.446338	24	t
3	10	fix audit columns	SQL	V10__fix_audit_columns.sql	773425159	admin	2026-04-20 13:36:16.829675	6	t
4	11	fix protesis boolean	SQL	V11__fix_protesis_boolean.sql	-948483014	admin	2026-04-20 19:18:45.554904	25	t
5	12	add delete accion audit	SQL	V12__add_delete_accion_audit.sql	33659921	admin	2026-04-20 19:48:55.460105	11	t
6	13	videojuego pdf md	SQL	V13__juego_articulacion.sql	1886437982	admin	2026-04-24 02:16:18.699762	28	t
7	14	datos prueba desarrollo	SQL	V14__datos_prueba_desarrollo.sql	-2082821860	admin	2026-05-07 12:32:51.494144	15	t
8	15	paciente contrasena	SQL	V15__paciente_contrasena.sql	1720841064	admin	2026-05-07 12:32:51.528208	1	t
9	16	fix campos clinicos vacios	SQL	V16__fix_campos_clinicos_vacios.sql	1786029226	admin	2026-05-07 18:46:29.510558	4	t
10	17	session reports	SQL	V17__session_reports.sql	533535695	admin	2026-05-12 00:25:40.155553	28	t
\.


--
-- Data for Name: juego; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.juego (cod_juego, nombre, descripcion, url_juego, id_articulacion, activo) FROM stdin;
\.


--
-- Data for Name: localidad; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.localidad (nombre_localidad, provincia) FROM stdin;
Jaen	Jaen
Madrid	Madrid
\.


--
-- Data for Name: nivel_progresion; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.nivel_progresion (id_nivel, nombre, nombre_corto, descripcion, estado_pac, tipos_ejercicio, orden) FROM stdin;
1	Fase Aguda o de Maxima Proteccion	Fase Aguda	Comienza inmediatamente despues de la lesion o cirugia. Tejido inflamado, fragil, dolor agudo. Objetivo: evitar atrofia severa y controlar inflamacion.	Movilidad muy reducida, dolor intenso ante el esfuerzo, posible inmovilizacion parcial.	Movilizacion pasiva, ejercicios isometricos submaximos.	1
2	Fase Subaguda o de Movilidad Controlada	Fase Subaguda	El dolor agudo y la inflamacion han bajado. El paciente necesita volver a mover la extremidad en todo su recorrido natural.	Menos dolor en reposo, debilidad notable.	Movilidad activo-asistida (poleas, baston, hidroterapia), movilidad activa libre.	2
3	Fase de Fortalecimiento y Remodelacion	Fortalecimiento	El paciente tiene recorrido articular casi completo y sin dolor. Objetivo: reconstruir masa muscular y resistencia.	Articulacion funcional, pero el musculo se fatiga rapido.	Ejercicios isotonicos (bandas, mancuernas, maquinas), cadena cinetica cerrada (sentadillas, flexiones pared).	3
4	Fase Funcional y Propioceptiva	Funcional	Fuerza recuperada, rango de movimiento completo, sin dolor. Objetivo: reeducar sistema nervioso para reacciones automaticas.	Musculo fuerte, rango completo, necesita recuperar confianza.	Propiocepcion y equilibrio (Bosu, superficies inestables), ejercicios funcionales (vida diaria, pliometria).	4
\.


--
-- Data for Name: paciente; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.paciente (dni_pac, dni_san, nombre_pac, apellido1_pac, apellido2_pac, edad_pac, email_pac, num_ss, id_direccion, sexo, fecha_nacimiento, foto, alergias, antecedentes, medicacion_actual, consentimiento_rgpd, fecha_consentimiento, protesis, activo, fecha_alta, fecha_baja, archivo_progreso_md, progreso_md_actualizado_en, contrasena_pac) FROM stdin;
00000003A	00000001R	Pedro	Sanchez	Gomez	45	pedro.sanchez@example.local	281234567890	4	MASCULINO	1980-05-15	\N	\N	\N	\N	t	2026-04-07 00:00:00	f	t	2026-04-07 13:12:20.983971	\N	\N	\N	\N
88888888X	ADMIN0000	TestPac2	Prueba	Test	40	testpac2@test.com	888888888888	5	FEMENINO	1985-06-15	\N	\N	\N	\N	f	\N	t	f	2026-04-20 19:44:45.824769	2026-04-20 19:45:24.68089	\N	\N	\N
12345678Z	87654321B	Admin	RehabiAPP	\N	36	admin@rehabiapp.com	280000000001	8	MASCULINO	1990-01-01	\N	\N	\N	\N	t	\N	f	t	2026-05-07 12:32:51.506713	\N	\N	\N	$2a$12$SaIPLAYHVw6aqi5h1Jjwtec6JjvEdK54L8jw209hMVPwJdxKgZ3J2
55667788Z	ADMIN0000	Juan	Benito	Gomez	34	juan.benito@rehabiapp.test	280034556677	9	MASCULINO	1991-03-15	\N	\N	\N	\N	t	2026-05-07 23:06:18.830022	f	t	2026-05-07 23:06:18.828281	\N	\N	\N	$2b$12$TKeQb.nooezArydBwkg7LO8pxqm.xj39zvu7243KskLYcAm/Xe5ni
\.


--
-- Data for Name: paciente_audit; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.paciente_audit (dni_pac, rev, rev_type, nombre_pac, apellido1_pac, apellido2_pac, edad_pac, email_pac, num_ss, protesis, fecha_nacimiento, sexo, alergias, antecedentes, medicacion_actual, consentimiento_rgpd, fecha_consentimiento, activo, fecha_baja, dni_san, id_direccion, foto) FROM stdin;
00000003A	4	1	Pedro	Sanchez	Gomez	45	pedro.sanchez@example.local	281234567890	t	1980-05-15	MASCULINO	\N	\N	\N	t	2026-04-07 00:00:00	t	\N	00000001R	4	\N
00000003A	5	1	Pedro	Sanchez	Gomez	45	pedro.sanchez@example.local	281234567890	f	1980-05-15	MASCULINO	\N	\N	\N	t	2026-04-07 00:00:00	t	\N	00000001R	4	\N
88888888X	7	0	TestPac2	Prueba	Test	40	testpac2@test.com	888888888888	t	1985-06-15	FEMENINO	\N	\N	\N	f	\N	t	\N	ADMIN0000	5	\N
88888888X	8	1	TestPac2	Prueba	Test	40	testpac2@test.com	888888888888	t	1985-06-15	FEMENINO	\N	\N	\N	f	\N	f	2026-04-20 19:45:24.68089	ADMIN0000	5	\N
55667788Z	18	0	Juan	Benito	Gomez	34	juan.benito@rehabiapp.test	280034556677	f	1991-03-15	MASCULINO	\N	\N	\N	t	2026-05-07 23:06:18.830022	t	\N	ADMIN0000	9	\N
\.


--
-- Data for Name: paciente_discapacidad; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.paciente_discapacidad (dni_pac, cod_dis, id_nivel_actual, fecha_asignacion, notas) FROM stdin;
12345678Z	M16	2	2024-03-10 09:00:00	Cadera derecha afectada principalmente. Post-operatorio de artroplastia total.
12345678Z	M54	1	2024-06-01 10:30:00	\N
55667788Z	DIS006	1	2026-05-07 23:06:38.316643	Rotura parcial del supraespinoso derecho. Dolor severo en abduccion. Post-cirugia artroscopica hace 3 semanas.
\.


--
-- Data for Name: paciente_discapacidad_audit; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.paciente_discapacidad_audit (dni_pac, cod_dis, rev, rev_type, id_nivel_actual, notas, fecha_asignacion) FROM stdin;
00000003A	DIS003	10	0	1	\N	2026-04-20 19:46:39.922103
00000003A	DIS003	11	1	2	\N	2026-04-20 19:46:39.922103
00000003A	DIS003	14	1	1	\N	2026-04-20 19:49:14.844947
00000003A	DIS003	17	2	1	\N	2026-04-20 19:49:14.844947
55667788Z	DIS006	19	0	1	Rotura parcial del supraespinoso derecho. Dolor severo en abduccion. Post-cirugia artroscopica hace 3 semanas.	2026-05-07 23:06:38.316643
\.


--
-- Data for Name: paciente_tratamiento; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.paciente_tratamiento (dni_pac, cod_trat, visible, fecha_asignacion) FROM stdin;
12345678Z	TRT001	t	2024-03-15 09:00:00
12345678Z	TRT002	t	2024-03-15 09:00:00
12345678Z	TRT003	t	2024-06-05 11:00:00
12345678Z	TRT004	f	2024-06-05 11:00:00
55667788Z	TRT001	t	2026-05-07 23:06:38.357328
55667788Z	TRT002	t	2026-05-07 23:06:38.385047
55667788Z	TRT003	t	2026-05-07 23:06:38.409252
\.


--
-- Data for Name: paciente_tratamiento_audit; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.paciente_tratamiento_audit (dni_pac, cod_trat, rev, rev_type, visible, fecha_asignacion) FROM stdin;
00000003A	TRT003	12	0	t	2026-04-20 19:47:01.0026
00000003A	TRT003	13	1	f	2026-04-20 19:47:01.0026
00000003A	TRT003	15	1	t	2026-04-20 19:49:14.929705
00000003A	TRT003	16	2	t	2026-04-20 19:49:14.929705
55667788Z	TRT001	20	0	t	2026-05-07 23:06:38.357328
55667788Z	TRT002	21	0	t	2026-05-07 23:06:38.385047
55667788Z	TRT003	22	0	t	2026-05-07 23:06:38.409252
\.


--
-- Data for Name: revinfo; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.revinfo (rev, revtstmp, usuario, ip_origen) FROM stdin;
2	1776672785067	ADMIN0000	127.0.0.1
3	1776686483219	ADMIN0000	127.0.0.1
4	1776706683024	ADMIN0000	0:0:0:0:0:0:0:1
5	1776706709074	ADMIN0000	0:0:0:0:0:0:0:1
6	1776706836733	ADMIN0000	0:0:0:0:0:0:0:1
7	1776707085873	ADMIN0000	0:0:0:0:0:0:0:1
8	1776707124686	ADMIN0000	0:0:0:0:0:0:0:1
9	1776707124722	ADMIN0000	0:0:0:0:0:0:0:1
10	1776707199931	ADMIN0000	0:0:0:0:0:0:0:1
11	1776707211465	ADMIN0000	0:0:0:0:0:0:0:1
12	1776707221012	ADMIN0000	0:0:0:0:0:0:0:1
13	1776707228980	ADMIN0000	0:0:0:0:0:0:0:1
14	1776707354877	ADMIN0000	0:0:0:0:0:0:0:1
15	1776707354937	ADMIN0000	0:0:0:0:0:0:0:1
16	1776707354995	ADMIN0000	0:0:0:0:0:0:0:1
17	1776707355035	ADMIN0000	0:0:0:0:0:0:0:1
18	1778187978869	ADMIN0000	0:0:0:0:0:0:0:1
19	1778187998323	ADMIN0000	0:0:0:0:0:0:0:1
20	1778187998362	ADMIN0000	0:0:0:0:0:0:0:1
21	1778187998388	ADMIN0000	0:0:0:0:0:0:0:1
22	1778187998412	ADMIN0000	0:0:0:0:0:0:0:1
\.


--
-- Data for Name: sanitario; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sanitario (dni_san, nombre_san, apellido1_san, apellido2_san, email_san, contrasena_san, num_de_pacientes, activo, fecha_alta, fecha_baja) FROM stdin;
00000001R	Carlos	Garcia	Lopez	carlos.garcia@rehabiapp.local	$2a$12$gAHLIGx8cdZgiCDgo/VHAulGjBC3rWDUh6yGFbB.7dwDP6yd9OGUu	1	t	2026-04-07 13:12:20.983971	\N
00000002W	Lucia	Martinez	Ruiz	lucia.martinez@rehabiapp.local	$2a$12$lgz13jTXZqNhWK6Ex.iMMeUxJQSVWVd1/5H9RZwqrLaIv1KT8tpje	0	t	2026-04-07 13:12:20.983971	\N
ADMIN0000	Admin	Sistema	\N	admin@rehabiapp.local	$2a$12$2roYx2xkdFiLTuzby1YQAe5K/WQur0I0rhFSv1jLpgSMl1bA7Kaee	0	t	2026-04-07 13:12:20.983971	\N
12345678T	pepe	pozo	perez	pepe@gmail.com	$2a$12$In4akU7EcOD4JCrQ2oOIr.8fokRv34ni3KrA1DSwaXgDPr8l4oxaW	0	t	2026-04-20 14:01:23.190229	\N
TEST0001X	Test	A	B	test9999@test.com	$2a$12$aTg4fQ7fZJCk7QVU/JzLVut.ig46he.e9ERXI9YJC/DsE5Lwo9H4e	0	f	2026-04-20 19:40:36.712802	2026-04-20 19:45:24.715026
87654321B	Doctora	Prueba	Rehabilitacion	especialista@rehabiapp.com	$2a$12$YoS0BYVB5LdCy6oVDHUjXeVYaqTB62jJ5S1M2Gk6GG6rdHPd7zui	1	t	2026-05-07 12:32:51.506713	\N
\.


--
-- Data for Name: sanitario_agrega_sanitario; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sanitario_agrega_sanitario (dni_san, cargo) FROM stdin;
00000001R	SPECIALIST
00000002W	NURSE
ADMIN0000	SPECIALIST
12345678T	SPECIALIST
TEST0001X	SPECIALIST
87654321B	SPECIALIST
\.


--
-- Data for Name: sanitario_audit; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sanitario_audit (dni_san, rev, rev_type, nombre_san, apellido1_san, apellido2_san, email_san, num_de_pacientes, activo, fecha_baja) FROM stdin;
12345678T	3	0	pepe	pozo	perez	pepe@gmail.com	0	t	\N
TEST0001X	6	0	Test	A	B	test9999@test.com	0	t	\N
TEST0001X	9	1	Test	A	B	test9999@test.com	0	f	2026-04-20 19:45:24.715026
\.


--
-- Data for Name: session_reports; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.session_reports (id, mongo_id, paciente_dni, cod_juego, cod_trat, fecha_sesion, duracion_seg, contenido_md, md_hash, fecha_creacion) FROM stdin;
2	6a03bad8a2fa39130e7cb36f	55667788Z	PIANO-001	TRT003	2026-05-13 02:00:00+00	60	# Informe de sesion de juego terapeutico\n\n> Documento generado automaticamente. Uso interno — analisis por agente IA.\n\n- **ID sesion (MongoDB):** `6a03bad8a2fa39130e7cb36f`\n- **Paciente (token):** `c8b18f0a683cb6be618b96f90eb4b3dbd30ec5f42a221980cae93b37dde8dbdf`\n- **Juego:** `PIANO-001`\n- **Tratamiento:** Movilidad activo-asistida (`TRT003`)\n- **Parte del cuerpo:** Mano Derecha\n- **Discapacidad:** `DIS006`\n\n## Datos de la sesion\n\n| Campo | Valor |\n|---|---|\n| Inicio | 2026-05-13T02:00:00Z |\n| Fin | 2026-05-13T02:01:00Z |\n| Duracion | 60 s |\n| Nivel de progresion | 1 |\n| Completada | Si |\n\n## Datos brutos por dedo\n\n_Observaciones por dedo previas a cualquier calculo de media o score._\n\n| Dedo | Aciertos | Fallos | Total notas | Precision dedo (%) |\n|---|---|---|---|---|\n| Dedo 0 | 10 | 2 | 12 | 83.3 |\n| Dedo 1 | 10 | 2 | 12 | 83.3 |\n| Dedo 2 | 10 | 3 | 13 | 76.9 |\n| Dedo 3 | 9 | 3 | 12 | 75.0 |\n| Dedo 4 | 9 | 2 | 11 | 81.8 |\n\n## Metricas de sesion brutas\n\n_Valores de sesion completa sin calculo de scores derivados._\n\n| Metrica | Valor | Descripcion |\n|---|---|---|\n| avgReactionMs | 850.0000 | Tiempo medio de reaccion (ms) |\n| fingerSpeed | 0.8500 | Velocidad media (notas/segundo) |\n| notesHit | 48 | Total notas acertadas en la sesion |\n| notesMissed | 12 | Total notas falladas en la sesion |\n\n## Scores computados\n\n_Derivados de los datos brutos. No usar como fuente primaria de analisis._\n\n| Score | Valor |\n|---|---|\n| Score global sesion | 108.0000 |\n| accuracyPct | 80.0000 |\n| maxStreak | 15 |\n| irsScore | 115.0000 |\n\n## Contexto para analisis automatico\n\n- **Schema version:** v1\n- **Recibido en pipeline:** 2026-05-12T23:42:16.731709612Z\n- **Metrics hash (idempotencia):** `07f93c8c2482f33952d456f5cb5f88f5036d8b1e00af61ddfad413f3a2c696f2`\n\n### Instrucciones para el agente IA\n\n1. Priorizar la seccion **Datos brutos por dedo** para evaluar asimetrias laterales.\n2. Comparar `avgReactionMs` con el umbral clinico de referencia (1500 ms).\n3. Los valores de la seccion **Scores computados** son referencias secundarias.\n4. No inferir diagnostico clinico — solo identificar patrones y anomalias.\n5. Si `completada = No`, ajustar interpretacion por sesion incompleta.\n	b2c54774ce0a65ea52d75a0383d796d4e75611d41c408b514371515fba54e85a	2026-05-12 23:42:16.770236+00
3	6a03bad8a2fa39130e7cb36e	55667788Z	PIANO-001	TRT003	2026-05-13 02:30:00+00	60	# Informe de sesion de juego terapeutico\n\n> Documento generado automaticamente. Uso interno — analisis por agente IA.\n\n- **ID sesion (MongoDB):** `6a03bad8a2fa39130e7cb36e`\n- **Paciente (token):** `c8b18f0a683cb6be618b96f90eb4b3dbd30ec5f42a221980cae93b37dde8dbdf`\n- **Juego:** `PIANO-001`\n- **Tratamiento:** Movilidad activo-asistida (`TRT003`)\n- **Parte del cuerpo:** Mano Derecha\n- **Discapacidad:** `DIS006`\n\n## Datos de la sesion\n\n| Campo | Valor |\n|---|---|\n| Inicio | 2026-05-13T02:30:00Z |\n| Fin | 2026-05-13T02:31:00Z |\n| Duracion | 60 s |\n| Nivel de progresion | 1 |\n| Completada | Si |\n\n## Datos brutos por dedo\n\n_Observaciones por dedo previas a cualquier calculo de media o score._\n\n| Dedo | Aciertos | Fallos | Total notas | Precision dedo (%) |\n|---|---|---|---|---|\n| Dedo 0 | 10 | 2 | 12 | 83.3 |\n| Dedo 1 | 10 | 2 | 12 | 83.3 |\n| Dedo 2 | 10 | 3 | 13 | 76.9 |\n| Dedo 3 | 9 | 3 | 12 | 75.0 |\n| Dedo 4 | 9 | 2 | 11 | 81.8 |\n\n## Metricas de sesion brutas\n\n_Valores de sesion completa sin calculo de scores derivados._\n\n| Metrica | Valor | Descripcion |\n|---|---|---|\n| avgReactionMs | 850.0000 | Tiempo medio de reaccion (ms) |\n| fingerSpeed | 0.8500 | Velocidad media (notas/segundo) |\n| notesHit | 48 | Total notas acertadas en la sesion |\n| notesMissed | 12 | Total notas falladas en la sesion |\n\n## Scores computados\n\n_Derivados de los datos brutos. No usar como fuente primaria de analisis._\n\n| Score | Valor |\n|---|---|\n| Score global sesion | 108.0000 |\n| accuracyPct | 80.0000 |\n| maxStreak | 15 |\n| irsScore | 115.0000 |\n\n## Contexto para analisis automatico\n\n- **Schema version:** v1\n- **Recibido en pipeline:** 2026-05-12T23:42:16.731507823Z\n- **Metrics hash (idempotencia):** `07f93c8c2482f33952d456f5cb5f88f5036d8b1e00af61ddfad413f3a2c696f2`\n\n### Instrucciones para el agente IA\n\n1. Priorizar la seccion **Datos brutos por dedo** para evaluar asimetrias laterales.\n2. Comparar `avgReactionMs` con el umbral clinico de referencia (1500 ms).\n3. Los valores de la seccion **Scores computados** son referencias secundarias.\n4. No inferir diagnostico clinico — solo identificar patrones y anomalias.\n5. Si `completada = No`, ajustar interpretacion por sesion incompleta.\n	bbd37b36d6b2165bdb5da91a4def3882bb973ab4c841aa203b0fdd0b255d45c2	2026-05-12 23:42:16.770235+00
4	6a03b9851dd51e52bfdf4b92	55667788Z	PIANO-001	TRT003	2026-05-13 01:40:00+00	60	# Informe de sesion de juego terapeutico\n\n> Documento generado automaticamente. Uso interno — analisis por agente IA.\n\n- **ID sesion (MongoDB):** `6a03b9851dd51e52bfdf4b92`\n- **Paciente (token):** `c8b18f0a683cb6be618b96f90eb4b3dbd30ec5f42a221980cae93b37dde8dbdf`\n- **Juego:** `PIANO-001`\n- **Tratamiento:** Movilidad activo-asistida (`TRT003`)\n- **Parte del cuerpo:** Mano Derecha\n- **Discapacidad:** `DIS006`\n\n## Datos de la sesion\n\n| Campo | Valor |\n|---|---|\n| Inicio | 2026-05-13T01:40:00Z |\n| Fin | 2026-05-13T01:41:00Z |\n| Duracion | 60 s |\n| Nivel de progresion | 1 |\n| Completada | Si |\n\n## Datos brutos por dedo\n\n_Observaciones por dedo previas a cualquier calculo de media o score._\n\n| Dedo | Aciertos | Fallos | Total notas | Precision dedo (%) |\n|---|---|---|---|---|\n| Dedo 0 | 8 | 4 | 12 | 66.7 |\n| Dedo 1 | 9 | 3 | 12 | 75.0 |\n| Dedo 2 | 8 | 4 | 12 | 66.7 |\n| Dedo 3 | 9 | 4 | 13 | 69.2 |\n| Dedo 4 | 8 | 3 | 11 | 72.7 |\n\n## Metricas de sesion brutas\n\n_Valores de sesion completa sin calculo de scores derivados._\n\n| Metrica | Valor | Descripcion |\n|---|---|---|\n| avgReactionMs | 1200.0000 | Tiempo medio de reaccion (ms) |\n| fingerSpeed | 0.7000 | Velocidad media (notas/segundo) |\n| notesHit | 42 | Total notas acertadas en la sesion |\n| notesMissed | 18 | Total notas falladas en la sesion |\n\n## Scores computados\n\n_Derivados de los datos brutos. No usar como fuente primaria de analisis._\n\n| Score | Valor |\n|---|---|\n| Score global sesion | 100.0000 |\n| accuracyPct | 70.0000 |\n| maxStreak | 8 |\n| irsScore | 100.0000 |\n\n## Contexto para analisis automatico\n\n- **Schema version:** v1\n- **Recibido en pipeline:** 2026-05-12T23:36:37.055Z\n- **Metrics hash (idempotencia):** `4b4155dee8b8eb7465f6ac8062b4b555ab2403a641daae89edf351c8ab9cf83e`\n\n### Instrucciones para el agente IA\n\n1. Priorizar la seccion **Datos brutos por dedo** para evaluar asimetrias laterales.\n2. Comparar `avgReactionMs` con el umbral clinico de referencia (1500 ms).\n3. Los valores de la seccion **Scores computados** son referencias secundarias.\n4. No inferir diagnostico clinico — solo identificar patrones y anomalias.\n5. Si `completada = No`, ajustar interpretacion por sesion incompleta.\n	d3a0ae497f1f966d4a9cbef4dae4a0f02970ad94fdbd90a77af10a62fa1d86a4	2026-05-12 23:45:00.010411+00
5	6a03b9b8a2fa39130e7cb36d	55667788Z	PIANO-001	TRT003	2026-05-13 01:50:00+00	60	# Informe de sesion de juego terapeutico\n\n> Documento generado automaticamente. Uso interno — analisis por agente IA.\n\n- **ID sesion (MongoDB):** `6a03b9b8a2fa39130e7cb36d`\n- **Paciente (token):** `c8b18f0a683cb6be618b96f90eb4b3dbd30ec5f42a221980cae93b37dde8dbdf`\n- **Juego:** `PIANO-001`\n- **Tratamiento:** Movilidad activo-asistida (`TRT003`)\n- **Parte del cuerpo:** Mano Derecha\n- **Discapacidad:** `DIS006`\n\n## Datos de la sesion\n\n| Campo | Valor |\n|---|---|\n| Inicio | 2026-05-13T01:50:00Z |\n| Fin | 2026-05-13T01:51:00Z |\n| Duracion | 60 s |\n| Nivel de progresion | 1 |\n| Completada | Si |\n\n## Datos brutos por dedo\n\n_Observaciones por dedo previas a cualquier calculo de media o score._\n\n| Dedo | Aciertos | Fallos | Total notas | Precision dedo (%) |\n|---|---|---|---|---|\n| Dedo 0 | 9 | 3 | 12 | 75.0 |\n| Dedo 1 | 9 | 3 | 12 | 75.0 |\n| Dedo 2 | 9 | 3 | 12 | 75.0 |\n| Dedo 3 | 9 | 3 | 12 | 75.0 |\n| Dedo 4 | 9 | 3 | 12 | 75.0 |\n\n## Metricas de sesion brutas\n\n_Valores de sesion completa sin calculo de scores derivados._\n\n| Metrica | Valor | Descripcion |\n|---|---|---|\n| avgReactionMs | 900.0000 | Tiempo medio de reaccion (ms) |\n| fingerSpeed | 0.8000 | Velocidad media (notas/segundo) |\n| notesHit | 45 | Total notas acertadas en la sesion |\n| notesMissed | 15 | Total notas falladas en la sesion |\n\n## Scores computados\n\n_Derivados de los datos brutos. No usar como fuente primaria de analisis._\n\n| Score | Valor |\n|---|---|\n| Score global sesion | 105.0000 |\n| accuracyPct | 75.0000 |\n| maxStreak | 12 |\n| irsScore | 110.0000 |\n\n## Contexto para analisis automatico\n\n- **Schema version:** v1\n- **Recibido en pipeline:** 2026-05-12T23:37:28.434Z\n- **Metrics hash (idempotencia):** `ddbc906d5a5d7e84f7556818e9a88f9dceb2d9c60a2bf748b02784b9e57202eb`\n\n### Instrucciones para el agente IA\n\n1. Priorizar la seccion **Datos brutos por dedo** para evaluar asimetrias laterales.\n2. Comparar `avgReactionMs` con el umbral clinico de referencia (1500 ms).\n3. Los valores de la seccion **Scores computados** son referencias secundarias.\n4. No inferir diagnostico clinico — solo identificar patrones y anomalias.\n5. Si `completada = No`, ajustar interpretacion por sesion incompleta.\n	89fa8f2516ee863b7b784538c4acc5d74e08a4e911373b9f3144f1ab8e9857f4	2026-05-12 23:45:00.136694+00
\.


--
-- Data for Name: telefono_paciente; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.telefono_paciente (id_telefono, dni_pac, telefono) FROM stdin;
3	00000003A	611000001
6	12345678Z	600000000
7	55667788Z	612345678
\.


--
-- Data for Name: telefono_sanitario; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.telefono_sanitario (id_telefono, dni_san, telefono) FROM stdin;
8	00000001R	600111111
9	00000002W	600222222
10	ADMIN0000	600000000
11	12345678T	622222222
\.


--
-- Data for Name: tratamiento; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tratamiento (cod_trat, nombre_trat, definicion_trat, id_nivel, cod_juego, archivo_pdf, nombre_archivo_pdf, tamano_pdf_bytes) FROM stdin;
TRT004	Movilidad activa libre	Paciente mueve la extremidad por si mismo contra gravedad	2	\N	\N	\N	\N
TRT005	Ejercicios isotonicos	Uso de bandas elasticas, mancuernas ligeras o maquinas	3	\N	\N	\N	\N
TRT006	Cadena cinetica cerrada	Sentadillas, flexiones contra pared, extremo fijo	3	\N	\N	\N	\N
TRT007	Propiocepcion y equilibrio	Ejercicios sobre superficies inestables (Bosu, cojines)	4	\N	\N	\N	\N
TRT008	Ejercicios funcionales	Simulacion de vida diaria, pliometria, cambios de direccion	4	\N	\N	\N	\N
TRT009	Adaptacion protesis pierna	Programa de adaptacion a protesis de miembro inferior	2	\N	\N	\N	\N
TRT010	Adaptacion protesis brazo	Programa de adaptacion a protesis de miembro superior	2	\N	\N	\N	\N
TRT001	Movilizacion pasiva articular	Fisioterapeuta mueve la articulacion sin esfuerzo del paciente	2	\N	\N	\N	\N
TRT002	Isometricos submaximos	Contracciones musculares estaticas sin mover la articulacion	1	\N	\N	\N	\N
TRT003	Movilidad activo-asistida	Paciente se mueve con ayuda de poleas, baston o hidroterapia	1	\N	\N	\N	\N
\.


--
-- Data for Name: tratamiento_videojuego; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tratamiento_videojuego (cod_trat, id_videojuego, fecha_vinculo) FROM stdin;
TRT001	2	2026-05-07 12:32:51.506713
\.


--
-- Data for Name: tratamiento_videojuego_audit; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tratamiento_videojuego_audit (cod_trat, id_videojuego, rev, rev_type, fecha_vinculo) FROM stdin;
\.


--
-- Data for Name: videojuego; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.videojuego (id_videojuego, codigo, nombre, descripcion, cod_dis, parte_cuerpo, url_unity, activo, fecha_creacion) FROM stdin;
2	GAME-HIP-01	Mover la cadera	Minijuego de rehabilitacion de cadera mediante movimientos controlados de flexion y extension.	M16	CADERA	https://games.rehabiapp.com/hip-01	t	2026-05-07 12:32:51.506713
\.


--
-- Data for Name: videojuego_audit; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.videojuego_audit (id_videojuego, rev, rev_type, codigo, nombre, descripcion, cod_dis, parte_cuerpo, url_unity, activo, fecha_creacion) FROM stdin;
\.


--
-- Name: articulacion_id_articulacion_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.articulacion_id_articulacion_seq', 15, true);


--
-- Name: direccion_id_direccion_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.direccion_id_direccion_seq', 9, true);


--
-- Name: revinfo_rev_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.revinfo_rev_seq', 22, true);


--
-- Name: session_reports_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.session_reports_id_seq', 5, true);


--
-- Name: telefono_paciente_id_telefono_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.telefono_paciente_id_telefono_seq', 7, true);


--
-- Name: telefono_sanitario_id_telefono_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.telefono_sanitario_id_telefono_seq', 11, true);


--
-- Name: videojuego_id_videojuego_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.videojuego_id_videojuego_seq', 2, true);


--
-- Name: 25160; Type: BLOB METADATA; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('25160');

--
-- Data for Name: 25160; Type: BLOBS; Schema: -; Owner: -
--

BEGIN;

SELECT pg_catalog.lo_open('25160', 131072);
SELECT pg_catalog.lowrite(0, '\x2320496e666f726d6520646520736573696f6e206465206a7565676f20746572617065757469636f0a0a3e20446f63756d656e746f2067656e657261646f206175746f6d61746963616d656e74652e2055736f20696e7465726e6f20e2809420616e616c6973697320706f72206167656e74652049412e0a0a2d202a2a494420736573696f6e20284d6f6e676f4442293a2a2a2060366130336239623861326661333931333065376362333664600a2d202a2a50616369656e74652028746f6b656e293a2a2a206063386231386630613638336362366265363138623936663930656234623364626433306563356634326132323139383063616539336233376464653864626466600a2d202a2a4a7565676f3a2a2a20605049414e4f2d303031600a2d202a2a54726174616d69656e746f3a2a2a204d6f76696c696461642061637469766f2d617369737469646120286054525430303360290a2d202a2a50617274652064656c2063756572706f3a2a2a204d616e6f20446572656368610a2d202a2a4469736361706163696461643a2a2a2060444953303036600a0a2323204461746f73206465206c6120736573696f6e0a0a7c2043616d706f207c2056616c6f72207c0a7c2d2d2d7c2d2d2d7c0a7c20496e6963696f207c20323032362d30352d31335430313a35303a30305a207c0a7c2046696e207c20323032362d30352d31335430313a35313a30305a207c0a7c204475726163696f6e207c2036302073207c0a7c204e6976656c2064652070726f67726573696f6e207c2031207c0a7c20436f6d706c6574616461207c205369207c0a0a2323204461746f7320627275746f7320706f72206465646f0a0a5f4f62736572766163696f6e657320706f72206465646f20707265766961732061206375616c71756965722063616c63756c6f206465206d65646961206f2073636f72652e5f0a0a7c204465646f207c204163696572746f73207c2046616c6c6f73207c20546f74616c206e6f746173207c20507265636973696f6e206465646f20282529207c0a7c2d2d2d7c2d2d2d7c2d2d2d7c2d2d2d7c2d2d2d7c0a7c204465646f2030207c2039207c2033207c203132207c2037352e30207c0a7c204465646f2031207c2039207c2033207c203132207c2037352e30207c0a7c204465646f2032207c2039207c2033207c203132207c2037352e30207c0a7c204465646f2033207c2039207c2033207c203132207c2037352e30207c0a7c204465646f2034207c2039207c2033207c203132207c2037352e30207c0a0a2323204d6574726963617320646520736573696f6e206272757461730a0a5f56616c6f72657320646520736573696f6e20636f6d706c6574612073696e2063616c63756c6f2064652073636f72657320646572697661646f732e5f0a0a7c204d657472696361207c2056616c6f72207c204465736372697063696f6e207c0a7c2d2d2d7c2d2d2d7c2d2d2d7c0a7c206176675265616374696f6e4d73207c203930302e30303030207c205469656d706f206d6564696f206465207265616363696f6e20286d7329207c0a7c2066696e6765725370656564207c20302e38303030207c2056656c6f6369646164206d6564696120286e6f7461732f736567756e646f29207c0a7c206e6f746573486974207c203435207c20546f74616c206e6f7461732061636572746164617320656e206c6120736573696f6e207c0a7c206e6f7465734d6973736564207c203135207c20546f74616c206e6f7461732066616c6c6164617320656e206c6120736573696f6e207c0a0a23232053636f72657320636f6d70757461646f730a0a5f446572697661646f73206465206c6f73206461746f7320627275746f732e204e6f207573617220636f6d6f206675656e7465207072696d6172696120646520616e616c697369732e5f0a0a7c2053636f7265207c2056616c6f72207c0a7c2d2d2d7c2d2d2d7c0a7c2053636f726520676c6f62616c20736573696f6e207c203130352e30303030207c0a7c206163637572616379506374207c2037352e30303030207c0a7c206d617853747265616b207c203132207c0a7c2069727353636f7265207c203131302e30303030207c0a0a232320436f6e746578746f207061726120616e616c69736973206175746f6d617469636f0a0a2d202a2a536368656d612076657273696f6e3a2a2a2076310a2d202a2a526563696269646f20656e20706970656c696e653a2a2a20323032362d30352d31325432333a33373a32382e3433343332383530315a0a2d202a2a4d657472696373206861736820286964656d706f74656e636961293a2a2a206064646263393036643561356437653834663735353638313865396138386639646365623264396336306132626637343862303237383462396535373230326562600a0a23232320496e737472756363696f6e6573207061726120656c206167656e74652049410a0a312e205072696f72697a6172206c612073656363696f6e202a2a4461746f7320627275746f7320706f72206465646f2a2a2070617261206576616c756172206173696d657472696173206c61746572616c65732e0a322e20436f6d706172617220606176675265616374696f6e4d736020636f6e20656c20756d6272616c20636c696e69636f206465207265666572656e636961202831353030206d73292e0a332e204c6f732076616c6f726573206465206c612073656363696f6e202a2a53636f72657320636f6d70757461646f732a2a20736f6e207265666572656e6369617320736563756e6461726961732e0a342e204e6f20696e666572697220646961676e6f737469636f20636c696e69636f20e2809420736f6c6f206964656e7469666963617220706174726f6e6573207920616e6f6d616c6961732e0a352e2053692060636f6d706c6574616461203d204e6f602c20616a757374617220696e746572707265746163696f6e20706f7220736573696f6e20696e636f6d706c6574612e0a');
SELECT pg_catalog.lo_close(0);

COMMIT;

--
-- Name: articulacion articulacion_codigo_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.articulacion
    ADD CONSTRAINT articulacion_codigo_key UNIQUE (codigo);


--
-- Name: articulacion articulacion_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.articulacion
    ADD CONSTRAINT articulacion_pkey PRIMARY KEY (id_articulacion);


--
-- Name: audit_log audit_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_log
    ADD CONSTRAINT audit_log_pkey PRIMARY KEY (id_audit);


--
-- Name: cita_audit cita_audit_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cita_audit
    ADD CONSTRAINT cita_audit_pkey PRIMARY KEY (dni_pac, dni_san, fecha_cita, hora, rev);


--
-- Name: cita cita_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cita
    ADD CONSTRAINT cita_pkey PRIMARY KEY (dni_pac, dni_san, fecha_cita, hora);


--
-- Name: cp cp_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cp
    ADD CONSTRAINT cp_pkey PRIMARY KEY (cp);


--
-- Name: direccion direccion_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.direccion
    ADD CONSTRAINT direccion_pkey PRIMARY KEY (id_direccion);


--
-- Name: discapacidad discapacidad_nombre_dis_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.discapacidad
    ADD CONSTRAINT discapacidad_nombre_dis_key UNIQUE (nombre_dis);


--
-- Name: discapacidad discapacidad_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.discapacidad
    ADD CONSTRAINT discapacidad_pkey PRIMARY KEY (cod_dis);


--
-- Name: discapacidad_tratamiento discapacidad_tratamiento_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.discapacidad_tratamiento
    ADD CONSTRAINT discapacidad_tratamiento_pkey PRIMARY KEY (cod_dis, cod_trat);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: juego juego_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.juego
    ADD CONSTRAINT juego_pkey PRIMARY KEY (cod_juego);


--
-- Name: localidad localidad_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.localidad
    ADD CONSTRAINT localidad_pkey PRIMARY KEY (nombre_localidad);


--
-- Name: nivel_progresion nivel_progresion_orden_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nivel_progresion
    ADD CONSTRAINT nivel_progresion_orden_unique UNIQUE (orden);


--
-- Name: nivel_progresion nivel_progresion_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nivel_progresion
    ADD CONSTRAINT nivel_progresion_pkey PRIMARY KEY (id_nivel);


--
-- Name: paciente_audit paciente_audit_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.paciente_audit
    ADD CONSTRAINT paciente_audit_pkey PRIMARY KEY (dni_pac, rev);


--
-- Name: paciente_discapacidad_audit paciente_discapacidad_audit_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.paciente_discapacidad_audit
    ADD CONSTRAINT paciente_discapacidad_audit_pkey PRIMARY KEY (dni_pac, cod_dis, rev);


--
-- Name: paciente_discapacidad paciente_discapacidad_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.paciente_discapacidad
    ADD CONSTRAINT paciente_discapacidad_pkey PRIMARY KEY (dni_pac, cod_dis);


--
-- Name: paciente paciente_email_pac_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.paciente
    ADD CONSTRAINT paciente_email_pac_key UNIQUE (email_pac);


--
-- Name: paciente paciente_num_ss_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.paciente
    ADD CONSTRAINT paciente_num_ss_key UNIQUE (num_ss);


--
-- Name: paciente paciente_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.paciente
    ADD CONSTRAINT paciente_pkey PRIMARY KEY (dni_pac);


--
-- Name: paciente_tratamiento_audit paciente_tratamiento_audit_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.paciente_tratamiento_audit
    ADD CONSTRAINT paciente_tratamiento_audit_pkey PRIMARY KEY (dni_pac, cod_trat, rev);


--
-- Name: paciente_tratamiento paciente_tratamiento_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.paciente_tratamiento
    ADD CONSTRAINT paciente_tratamiento_pkey PRIMARY KEY (dni_pac, cod_trat);


--
-- Name: revinfo revinfo_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.revinfo
    ADD CONSTRAINT revinfo_pkey PRIMARY KEY (rev);


--
-- Name: sanitario_agrega_sanitario sanitario_agrega_sanitario_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sanitario_agrega_sanitario
    ADD CONSTRAINT sanitario_agrega_sanitario_pkey PRIMARY KEY (dni_san);


--
-- Name: sanitario_audit sanitario_audit_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sanitario_audit
    ADD CONSTRAINT sanitario_audit_pkey PRIMARY KEY (dni_san, rev);


--
-- Name: sanitario sanitario_email_san_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sanitario
    ADD CONSTRAINT sanitario_email_san_key UNIQUE (email_san);


--
-- Name: sanitario sanitario_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sanitario
    ADD CONSTRAINT sanitario_pkey PRIMARY KEY (dni_san);


--
-- Name: session_reports session_reports_mongo_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.session_reports
    ADD CONSTRAINT session_reports_mongo_id_key UNIQUE (mongo_id);


--
-- Name: session_reports session_reports_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.session_reports
    ADD CONSTRAINT session_reports_pkey PRIMARY KEY (id);


--
-- Name: telefono_paciente telefono_paciente_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.telefono_paciente
    ADD CONSTRAINT telefono_paciente_pkey PRIMARY KEY (id_telefono);


--
-- Name: telefono_sanitario telefono_sanitario_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.telefono_sanitario
    ADD CONSTRAINT telefono_sanitario_pkey PRIMARY KEY (id_telefono);


--
-- Name: tratamiento tratamiento_nombre_trat_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tratamiento
    ADD CONSTRAINT tratamiento_nombre_trat_key UNIQUE (nombre_trat);


--
-- Name: tratamiento tratamiento_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tratamiento
    ADD CONSTRAINT tratamiento_pkey PRIMARY KEY (cod_trat);


--
-- Name: tratamiento_videojuego_audit tratamiento_videojuego_audit_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tratamiento_videojuego_audit
    ADD CONSTRAINT tratamiento_videojuego_audit_pkey PRIMARY KEY (cod_trat, id_videojuego, rev);


--
-- Name: tratamiento_videojuego tratamiento_videojuego_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tratamiento_videojuego
    ADD CONSTRAINT tratamiento_videojuego_pkey PRIMARY KEY (cod_trat, id_videojuego);


--
-- Name: videojuego_audit videojuego_audit_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.videojuego_audit
    ADD CONSTRAINT videojuego_audit_pkey PRIMARY KEY (id_videojuego, rev);


--
-- Name: videojuego videojuego_codigo_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.videojuego
    ADD CONSTRAINT videojuego_codigo_key UNIQUE (codigo);


--
-- Name: videojuego videojuego_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.videojuego
    ADD CONSTRAINT videojuego_pkey PRIMARY KEY (id_videojuego);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_audit_entidad; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_entidad ON public.audit_log USING btree (entidad, id_entidad);


--
-- Name: idx_audit_fecha; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_fecha ON public.audit_log USING btree (fecha_hora);


--
-- Name: idx_audit_usuario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_usuario ON public.audit_log USING btree (dni_usuario);


--
-- Name: idx_cita_fecha; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cita_fecha ON public.cita USING btree (fecha_cita);


--
-- Name: idx_cita_paciente; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cita_paciente ON public.cita USING btree (dni_pac);


--
-- Name: idx_cita_sanitario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cita_sanitario ON public.cita USING btree (dni_san);


--
-- Name: idx_discapacidad_articulacion; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_discapacidad_articulacion ON public.discapacidad USING btree (id_articulacion);


--
-- Name: idx_discapacidad_nombre; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_discapacidad_nombre ON public.discapacidad USING btree (nombre_dis);


--
-- Name: idx_juego_articulacion; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_juego_articulacion ON public.juego USING btree (id_articulacion);


--
-- Name: idx_pac_dis_discapacidad; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pac_dis_discapacidad ON public.paciente_discapacidad USING btree (cod_dis);


--
-- Name: idx_pac_dis_paciente; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pac_dis_paciente ON public.paciente_discapacidad USING btree (dni_pac);


--
-- Name: idx_pac_trat_paciente; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pac_trat_paciente ON public.paciente_tratamiento USING btree (dni_pac);


--
-- Name: idx_pac_trat_visible; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pac_trat_visible ON public.paciente_tratamiento USING btree (dni_pac) WHERE (visible = true);


--
-- Name: idx_paciente_activo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_paciente_activo ON public.paciente USING btree (dni_pac) WHERE (activo = true);


--
-- Name: idx_paciente_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_paciente_email ON public.paciente USING btree (email_pac);


--
-- Name: idx_paciente_nombre; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_paciente_nombre ON public.paciente USING btree (nombre_pac, apellido1_pac, apellido2_pac);


--
-- Name: idx_paciente_num_ss; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_paciente_num_ss ON public.paciente USING btree (num_ss);


--
-- Name: idx_paciente_sanitario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_paciente_sanitario ON public.paciente USING btree (dni_san);


--
-- Name: idx_sanitario_activo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sanitario_activo ON public.sanitario USING btree (dni_san) WHERE (activo = true);


--
-- Name: idx_sanitario_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sanitario_email ON public.sanitario USING btree (email_san);


--
-- Name: idx_sanitario_nombre; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sanitario_nombre ON public.sanitario USING btree (nombre_san, apellido1_san, apellido2_san);


--
-- Name: idx_session_reports_cod_juego; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_session_reports_cod_juego ON public.session_reports USING btree (cod_juego);


--
-- Name: idx_session_reports_paciente_fecha; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_session_reports_paciente_fecha ON public.session_reports USING btree (paciente_dni, fecha_sesion DESC);


--
-- Name: idx_tratamiento_juego; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tratamiento_juego ON public.tratamiento USING btree (cod_juego);


--
-- Name: idx_tratamiento_nivel; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tratamiento_nivel ON public.tratamiento USING btree (id_nivel);


--
-- Name: idx_tratamiento_nombre; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tratamiento_nombre ON public.tratamiento USING btree (nombre_trat);


--
-- Name: idx_videojuego_activo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_videojuego_activo ON public.videojuego USING btree (activo);


--
-- Name: idx_videojuego_cod_dis; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_videojuego_cod_dis ON public.videojuego USING btree (cod_dis);


--
-- Name: cita_audit cita_audit_rev_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cita_audit
    ADD CONSTRAINT cita_audit_rev_fkey FOREIGN KEY (rev) REFERENCES public.revinfo(rev);


--
-- Name: cita cita_dni_pac_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cita
    ADD CONSTRAINT cita_dni_pac_fkey FOREIGN KEY (dni_pac) REFERENCES public.paciente(dni_pac) ON DELETE CASCADE;


--
-- Name: cita cita_dni_san_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cita
    ADD CONSTRAINT cita_dni_san_fkey FOREIGN KEY (dni_san) REFERENCES public.sanitario(dni_san) ON DELETE CASCADE;


--
-- Name: cp cp_nombre_localidad_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cp
    ADD CONSTRAINT cp_nombre_localidad_fkey FOREIGN KEY (nombre_localidad) REFERENCES public.localidad(nombre_localidad);


--
-- Name: direccion direccion_cp_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.direccion
    ADD CONSTRAINT direccion_cp_fkey FOREIGN KEY (cp) REFERENCES public.cp(cp);


--
-- Name: discapacidad discapacidad_id_articulacion_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.discapacidad
    ADD CONSTRAINT discapacidad_id_articulacion_fkey FOREIGN KEY (id_articulacion) REFERENCES public.articulacion(id_articulacion) ON DELETE SET NULL;


--
-- Name: discapacidad_tratamiento discapacidad_tratamiento_cod_dis_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.discapacidad_tratamiento
    ADD CONSTRAINT discapacidad_tratamiento_cod_dis_fkey FOREIGN KEY (cod_dis) REFERENCES public.discapacidad(cod_dis) ON DELETE CASCADE;


--
-- Name: discapacidad_tratamiento discapacidad_tratamiento_cod_trat_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.discapacidad_tratamiento
    ADD CONSTRAINT discapacidad_tratamiento_cod_trat_fkey FOREIGN KEY (cod_trat) REFERENCES public.tratamiento(cod_trat) ON DELETE CASCADE;


--
-- Name: session_reports fk_session_reports_paciente; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.session_reports
    ADD CONSTRAINT fk_session_reports_paciente FOREIGN KEY (paciente_dni) REFERENCES public.paciente(dni_pac) ON DELETE RESTRICT;


--
-- Name: tratamiento_videojuego fk_tv_jue; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tratamiento_videojuego
    ADD CONSTRAINT fk_tv_jue FOREIGN KEY (id_videojuego) REFERENCES public.videojuego(id_videojuego) ON DELETE CASCADE;


--
-- Name: tratamiento_videojuego fk_tv_trat; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tratamiento_videojuego
    ADD CONSTRAINT fk_tv_trat FOREIGN KEY (cod_trat) REFERENCES public.tratamiento(cod_trat) ON DELETE CASCADE;


--
-- Name: videojuego fk_videojuego_dis; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.videojuego
    ADD CONSTRAINT fk_videojuego_dis FOREIGN KEY (cod_dis) REFERENCES public.discapacidad(cod_dis) ON DELETE RESTRICT;


--
-- Name: juego juego_id_articulacion_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.juego
    ADD CONSTRAINT juego_id_articulacion_fkey FOREIGN KEY (id_articulacion) REFERENCES public.articulacion(id_articulacion) ON DELETE RESTRICT;


--
-- Name: paciente_audit paciente_audit_rev_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.paciente_audit
    ADD CONSTRAINT paciente_audit_rev_fkey FOREIGN KEY (rev) REFERENCES public.revinfo(rev);


--
-- Name: paciente_discapacidad_audit paciente_discapacidad_audit_rev_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.paciente_discapacidad_audit
    ADD CONSTRAINT paciente_discapacidad_audit_rev_fkey FOREIGN KEY (rev) REFERENCES public.revinfo(rev);


--
-- Name: paciente_discapacidad paciente_discapacidad_cod_dis_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.paciente_discapacidad
    ADD CONSTRAINT paciente_discapacidad_cod_dis_fkey FOREIGN KEY (cod_dis) REFERENCES public.discapacidad(cod_dis);


--
-- Name: paciente_discapacidad paciente_discapacidad_dni_pac_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.paciente_discapacidad
    ADD CONSTRAINT paciente_discapacidad_dni_pac_fkey FOREIGN KEY (dni_pac) REFERENCES public.paciente(dni_pac) ON DELETE CASCADE;


--
-- Name: paciente_discapacidad paciente_discapacidad_id_nivel_actual_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.paciente_discapacidad
    ADD CONSTRAINT paciente_discapacidad_id_nivel_actual_fkey FOREIGN KEY (id_nivel_actual) REFERENCES public.nivel_progresion(id_nivel);


--
-- Name: paciente paciente_dni_san_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.paciente
    ADD CONSTRAINT paciente_dni_san_fkey FOREIGN KEY (dni_san) REFERENCES public.sanitario(dni_san) ON DELETE RESTRICT;


--
-- Name: paciente paciente_id_direccion_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.paciente
    ADD CONSTRAINT paciente_id_direccion_fkey FOREIGN KEY (id_direccion) REFERENCES public.direccion(id_direccion);


--
-- Name: paciente_tratamiento_audit paciente_tratamiento_audit_rev_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.paciente_tratamiento_audit
    ADD CONSTRAINT paciente_tratamiento_audit_rev_fkey FOREIGN KEY (rev) REFERENCES public.revinfo(rev);


--
-- Name: paciente_tratamiento paciente_tratamiento_cod_trat_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.paciente_tratamiento
    ADD CONSTRAINT paciente_tratamiento_cod_trat_fkey FOREIGN KEY (cod_trat) REFERENCES public.tratamiento(cod_trat);


--
-- Name: paciente_tratamiento paciente_tratamiento_dni_pac_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.paciente_tratamiento
    ADD CONSTRAINT paciente_tratamiento_dni_pac_fkey FOREIGN KEY (dni_pac) REFERENCES public.paciente(dni_pac) ON DELETE CASCADE;


--
-- Name: sanitario_agrega_sanitario sanitario_agrega_sanitario_dni_san_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sanitario_agrega_sanitario
    ADD CONSTRAINT sanitario_agrega_sanitario_dni_san_fkey FOREIGN KEY (dni_san) REFERENCES public.sanitario(dni_san) ON DELETE CASCADE;


--
-- Name: sanitario_audit sanitario_audit_rev_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sanitario_audit
    ADD CONSTRAINT sanitario_audit_rev_fkey FOREIGN KEY (rev) REFERENCES public.revinfo(rev);


--
-- Name: telefono_paciente telefono_paciente_dni_pac_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.telefono_paciente
    ADD CONSTRAINT telefono_paciente_dni_pac_fkey FOREIGN KEY (dni_pac) REFERENCES public.paciente(dni_pac) ON DELETE CASCADE;


--
-- Name: telefono_sanitario telefono_sanitario_dni_san_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.telefono_sanitario
    ADD CONSTRAINT telefono_sanitario_dni_san_fkey FOREIGN KEY (dni_san) REFERENCES public.sanitario(dni_san) ON DELETE CASCADE;


--
-- Name: tratamiento tratamiento_cod_juego_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tratamiento
    ADD CONSTRAINT tratamiento_cod_juego_fkey FOREIGN KEY (cod_juego) REFERENCES public.juego(cod_juego) ON DELETE SET NULL;


--
-- Name: tratamiento tratamiento_id_nivel_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tratamiento
    ADD CONSTRAINT tratamiento_id_nivel_fkey FOREIGN KEY (id_nivel) REFERENCES public.nivel_progresion(id_nivel);


--
-- Name: tratamiento_videojuego_audit tratamiento_videojuego_audit_rev_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tratamiento_videojuego_audit
    ADD CONSTRAINT tratamiento_videojuego_audit_rev_fkey FOREIGN KEY (rev) REFERENCES public.revinfo(rev);


--
-- Name: videojuego_audit videojuego_audit_rev_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.videojuego_audit
    ADD CONSTRAINT videojuego_audit_rev_fkey FOREIGN KEY (rev) REFERENCES public.revinfo(rev);


--
-- PostgreSQL database dump complete
--

\unrestrict umbkAFFKBmIEBnLppuRxePqqeIO4OMEqp3pQ1BpouoRq4l3PA1PBmdnIl4df5k2

