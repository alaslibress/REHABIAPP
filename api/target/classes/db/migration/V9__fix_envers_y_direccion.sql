-- ============================================================
-- V9__fix_envers_y_direccion.sql
-- Crea tablas Envers (revinfo + audit) que V3 nunca ejecuto
-- por el baseline de Flyway en V8.
-- Corrige direccion.numero de INTEGER a VARCHAR(20).
-- ============================================================

-- --------------------------------------------------------
-- TABLA: revinfo
-- Metadatos de cada revision Envers.
-- Almacena timestamp, usuario que realizo el cambio e IP.
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS revinfo (
    rev       SERIAL      PRIMARY KEY,
    revtstmp  BIGINT      NOT NULL,
    usuario   VARCHAR(20),
    ip_origen VARCHAR(45)
);

-- --------------------------------------------------------
-- TABLA: sanitario_audit
-- Historial de cambios sobre la entidad sanitario.
-- rev_type: 0=INSERT, 1=UPDATE, 2=DELETE (convencion Envers).
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS sanitario_audit (
    dni_san          VARCHAR(20)  NOT NULL,
    rev              INTEGER      NOT NULL REFERENCES revinfo(rev),
    rev_type         SMALLINT,
    nombre_san       VARCHAR(100),
    apellido1_san    VARCHAR(100),
    apellido2_san    VARCHAR(100),
    email_san        VARCHAR(200),
    num_de_pacientes INTEGER,
    activo           BOOLEAN,
    fecha_baja       TIMESTAMP,
    PRIMARY KEY (dni_san, rev)
);

-- --------------------------------------------------------
-- TABLA: paciente_audit
-- Historial de cambios sobre la entidad paciente.
-- Campos clinicos se almacenan cifrados (AES-256-GCM).
-- rev_type: 0=INSERT, 1=UPDATE, 2=DELETE (convencion Envers).
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS paciente_audit (
    dni_pac              VARCHAR(20)  NOT NULL,
    rev                  INTEGER      NOT NULL REFERENCES revinfo(rev),
    rev_type             SMALLINT,
    nombre_pac           VARCHAR(100),
    apellido1_pac        VARCHAR(100),
    apellido2_pac        VARCHAR(100),
    edad_pac             INTEGER,
    email_pac            VARCHAR(200),
    num_ss               VARCHAR(20),
    protesis             BOOLEAN,
    fecha_nacimiento     DATE,
    sexo                 VARCHAR(20),
    alergias             TEXT,
    antecedentes         TEXT,
    medicacion_actual    TEXT,
    consentimiento_rgpd  BOOLEAN,
    fecha_consentimiento TIMESTAMP,
    activo               BOOLEAN,
    fecha_baja           TIMESTAMP,
    PRIMARY KEY (dni_pac, rev)
);

-- --------------------------------------------------------
-- TABLA: cita_audit
-- Historial de cambios sobre la entidad cita.
-- NOTA: columna "hora" (no "hora_cita") — coincide con tabla cita real.
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS cita_audit (
    dni_pac    VARCHAR(20) NOT NULL,
    dni_san    VARCHAR(20) NOT NULL,
    fecha_cita DATE        NOT NULL,
    hora       TIME        NOT NULL,
    rev        INTEGER     NOT NULL REFERENCES revinfo(rev),
    rev_type   SMALLINT,
    PRIMARY KEY (dni_pac, dni_san, fecha_cita, hora, rev)
);

-- --------------------------------------------------------
-- TABLA: paciente_discapacidad_audit
-- Historial de cambios en asignaciones paciente-discapacidad.
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS paciente_discapacidad_audit (
    dni_pac         VARCHAR(20) NOT NULL,
    cod_dis         VARCHAR(20) NOT NULL,
    rev             INTEGER     NOT NULL REFERENCES revinfo(rev),
    rev_type        SMALLINT,
    id_nivel_actual INTEGER,
    notas           TEXT,
    PRIMARY KEY (dni_pac, cod_dis, rev)
);

-- --------------------------------------------------------
-- TABLA: paciente_tratamiento_audit
-- Historial de cambios en visibilidad de tratamientos por paciente.
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS paciente_tratamiento_audit (
    dni_pac  VARCHAR(20) NOT NULL,
    cod_trat VARCHAR(20) NOT NULL,
    rev      INTEGER     NOT NULL REFERENCES revinfo(rev),
    rev_type SMALLINT,
    visible  BOOLEAN,
    PRIMARY KEY (dni_pac, cod_trat, rev)
);

-- --------------------------------------------------------
-- FIX: direccion.numero INTEGER -> VARCHAR(20)
-- Direcciones espanolas admiten "S/N", "12B", "3 bis".
-- La entidad JPA Direccion.java ya mapea numero como String.
-- --------------------------------------------------------
ALTER TABLE direccion ALTER COLUMN numero TYPE VARCHAR(20) USING numero::VARCHAR;
