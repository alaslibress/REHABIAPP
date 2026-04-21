-- ============================================================
-- V3__envers_auditoria.sql
-- Tablas de auditoría gestionadas por Hibernate Envers.
-- Creadas explícitamente vía Flyway para mantener control total
-- del esquema en producción (Envers NO auto-genera tablas).
-- Registran el historial completo de cambios sobre entidades
-- auditables: sanitario, paciente, cita y asignaciones clínicas.
-- ============================================================

-- --------------------------------------------------------
-- TABLA: revinfo
-- Metadatos de cada revisión Envers.
-- Almacena timestamp, usuario que realizó el cambio e IP.
-- Envers la utiliza como tabla de revisión personalizada.
-- --------------------------------------------------------
CREATE TABLE revinfo (
    rev       SERIAL      PRIMARY KEY,
    revtstmp  BIGINT      NOT NULL,
    usuario   VARCHAR(20),
    ip_origen VARCHAR(45)
);

-- --------------------------------------------------------
-- TABLA: sanitario_audit
-- Historial de cambios sobre la entidad sanitario.
-- rev_type: 0=INSERT, 1=UPDATE, 2=DELETE (convención Envers).
-- --------------------------------------------------------
CREATE TABLE sanitario_audit (
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
-- Los campos clínicos (alergias, antecedentes, medicacion_actual)
-- se almacenan cifrados tal como llegan desde la capa de aplicación.
-- rev_type: 0=INSERT, 1=UPDATE, 2=DELETE (convención Envers).
-- --------------------------------------------------------
CREATE TABLE paciente_audit (
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
    -- Campos clínicos cifrados (AES-256-GCM)
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
-- La clave primaria incluye todos los campos de la PK de cita
-- más la revisión de Envers.
-- --------------------------------------------------------
CREATE TABLE cita_audit (
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
-- Historial de cambios en las asignaciones paciente-discapacidad,
-- incluyendo variaciones del nivel de progresión clínica.
-- --------------------------------------------------------
CREATE TABLE paciente_discapacidad_audit (
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
-- Historial de cambios en la visibilidad de tratamientos por paciente.
-- --------------------------------------------------------
CREATE TABLE paciente_tratamiento_audit (
    dni_pac  VARCHAR(20) NOT NULL,
    cod_trat VARCHAR(20) NOT NULL,
    rev      INTEGER     NOT NULL REFERENCES revinfo(rev),
    rev_type SMALLINT,
    visible  BOOLEAN,
    PRIMARY KEY (dni_pac, cod_trat, rev)
);
