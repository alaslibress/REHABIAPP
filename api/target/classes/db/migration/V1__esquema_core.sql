-- ============================================================
-- V1__esquema_core.sql
-- Esquema central de la base de datos de RehabiAPP.
-- Contiene todas las tablas principales del sistema en el orden
-- correcto para satisfacer las restricciones de clave foránea.
-- Compatible con el esquema del ERP de escritorio (/desktop).
-- ============================================================

-- --------------------------------------------------------
-- TABLA: localidad
-- Catálogo de localidades de España.
-- --------------------------------------------------------
CREATE TABLE localidad (
    nombre_localidad VARCHAR(100) PRIMARY KEY,
    provincia        VARCHAR(100) NOT NULL
);

-- --------------------------------------------------------
-- TABLA: cp
-- Códigos postales vinculados a su localidad.
-- --------------------------------------------------------
CREATE TABLE cp (
    cp               VARCHAR(10)  PRIMARY KEY,
    nombre_localidad VARCHAR(100) NOT NULL
        REFERENCES localidad(nombre_localidad)
);

-- --------------------------------------------------------
-- TABLA: direccion
-- Direcciones postales referenciadas por pacientes y sanitarios.
-- --------------------------------------------------------
CREATE TABLE direccion (
    id_direccion SERIAL       PRIMARY KEY,
    calle        VARCHAR(200) NOT NULL,
    numero       VARCHAR(20),
    piso         VARCHAR(20),
    cp           VARCHAR(10)  NOT NULL
        REFERENCES cp(cp)
);

-- --------------------------------------------------------
-- TABLA: sanitario
-- Profesionales sanitarios del sistema (especialistas y enfermeros).
-- La contraseña se almacena como hash BCrypt (cost factor 12).
-- --------------------------------------------------------
CREATE TABLE sanitario (
    dni_san          VARCHAR(20)  PRIMARY KEY,
    nombre_san       VARCHAR(100) NOT NULL,
    apellido1_san    VARCHAR(100) NOT NULL,
    apellido2_san    VARCHAR(100),
    email_san        VARCHAR(200) UNIQUE NOT NULL,
    num_de_pacientes INTEGER      DEFAULT 0,
    contrasena_san   TEXT         NOT NULL,
    activo           BOOLEAN      DEFAULT TRUE,
    fecha_baja       TIMESTAMP
);

-- --------------------------------------------------------
-- TABLA: sanitario_agrega_sanitario
-- Rol del sanitario dentro del sistema (SPECIALIST o NURSE).
-- Implementa el control de acceso basado en roles (RBAC).
-- --------------------------------------------------------
CREATE TABLE sanitario_agrega_sanitario (
    dni_san VARCHAR(20) PRIMARY KEY
        REFERENCES sanitario(dni_san) ON DELETE CASCADE,
    cargo   VARCHAR(50) NOT NULL
        CHECK (cargo IN ('SPECIALIST', 'NURSE'))
);

-- --------------------------------------------------------
-- TABLA: telefono_sanitario
-- Números de teléfono de los sanitarios (relación 1:N).
-- --------------------------------------------------------
CREATE TABLE telefono_sanitario (
    id_telefono SERIAL      PRIMARY KEY,
    dni_san     VARCHAR(20) NOT NULL
        REFERENCES sanitario(dni_san) ON DELETE CASCADE,
    telefono    VARCHAR(20) NOT NULL
);

-- --------------------------------------------------------
-- TABLA: discapacidad
-- Catálogo de discapacidades gestionadas en la clínica.
-- --------------------------------------------------------
CREATE TABLE discapacidad (
    cod_dis          VARCHAR(20)  PRIMARY KEY,
    nombre_dis       VARCHAR(200) UNIQUE NOT NULL,
    descripcion_dis  TEXT,
    necesita_protesis BOOLEAN     DEFAULT FALSE
);

-- --------------------------------------------------------
-- TABLA: tratamiento
-- Catálogo de tratamientos disponibles en la clínica.
-- --------------------------------------------------------
CREATE TABLE tratamiento (
    cod_trat      VARCHAR(20)  PRIMARY KEY,
    nombre_trat   VARCHAR(200) UNIQUE NOT NULL,
    definicion_trat TEXT
);

-- --------------------------------------------------------
-- TABLA: discapacidad_tratamiento
-- Relación N:M entre discapacidades y tratamientos.
-- --------------------------------------------------------
CREATE TABLE discapacidad_tratamiento (
    cod_dis  VARCHAR(20) REFERENCES discapacidad(cod_dis),
    cod_trat VARCHAR(20) REFERENCES tratamiento(cod_trat),
    PRIMARY KEY (cod_dis, cod_trat)
);

-- --------------------------------------------------------
-- TABLA: paciente
-- Datos completos del paciente.
-- IMPORTANTE: Los campos clínicos sensibles (alergias, antecedentes,
-- medicacion_actual) se cifran a nivel de aplicación con AES-256-GCM
-- antes de persistirse en esta columna TEXT.
-- El paciente NUNCA se borra físicamente (soft delete: activo=FALSE).
-- Retención mínima 5 años tras la baja (Ley 41/2002).
-- --------------------------------------------------------
CREATE TABLE paciente (
    dni_pac              VARCHAR(20)  PRIMARY KEY,
    dni_san              VARCHAR(20)  NOT NULL
        REFERENCES sanitario(dni_san) ON DELETE RESTRICT,
    nombre_pac           VARCHAR(100) NOT NULL,
    apellido1_pac        VARCHAR(100) NOT NULL,
    apellido2_pac        VARCHAR(100),
    edad_pac             INTEGER,
    email_pac            VARCHAR(200) UNIQUE,
    num_ss               VARCHAR(20)  UNIQUE,
    id_direccion         INTEGER
        REFERENCES direccion(id_direccion),
    discapacidad_pac     VARCHAR(200),
    tratamiento_pac      VARCHAR(200),
    estado_tratamiento   VARCHAR(100),
    protesis             BOOLEAN      DEFAULT FALSE,
    foto                 BYTEA,
    fecha_nacimiento     DATE,
    sexo                 VARCHAR(20)
        CHECK (sexo IN ('MASCULINO', 'FEMENINO', 'OTRO')),
    -- Campos clínicos cifrados (AES-256-GCM) — RGPD Art. 9
    alergias             TEXT,
    antecedentes         TEXT,
    medicacion_actual    TEXT,
    -- Consentimiento informado RGPD
    consentimiento_rgpd  BOOLEAN      DEFAULT FALSE,
    fecha_consentimiento TIMESTAMP,
    -- Soft delete — jamás se borra físicamente un paciente
    activo               BOOLEAN      DEFAULT TRUE,
    fecha_baja           TIMESTAMP
);

-- --------------------------------------------------------
-- TABLA: telefono_paciente
-- Números de teléfono de los pacientes (relación 1:N).
-- --------------------------------------------------------
CREATE TABLE telefono_paciente (
    id_telefono SERIAL      PRIMARY KEY,
    dni_pac     VARCHAR(20) NOT NULL
        REFERENCES paciente(dni_pac) ON DELETE CASCADE,
    telefono    VARCHAR(20) NOT NULL
);

-- --------------------------------------------------------
-- TABLA: cita
-- Citas entre paciente y sanitario.
-- Clave primaria compuesta para garantizar unicidad de la cita.
-- --------------------------------------------------------
CREATE TABLE cita (
    dni_pac    VARCHAR(20) REFERENCES paciente(dni_pac)  ON DELETE CASCADE,
    dni_san    VARCHAR(20) REFERENCES sanitario(dni_san) ON DELETE CASCADE,
    fecha_cita DATE        NOT NULL,
    hora_cita  TIME        NOT NULL,
    PRIMARY KEY (dni_pac, dni_san, fecha_cita, hora_cita)
);

-- --------------------------------------------------------
-- TABLA: audit_log
-- Registro inmutable de todas las operaciones sobre datos
-- clínicos y de personal, incluyendo accesos de lectura.
-- Complementa a Hibernate Envers (que sólo registra escrituras).
-- Cumple: RGPD Art. 30, Ley 41/2002, ENS Alto.
-- --------------------------------------------------------
CREATE TABLE audit_log (
    id_audit      BIGSERIAL    PRIMARY KEY,
    fecha_hora    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dni_usuario   VARCHAR(20),
    nombre_usuario VARCHAR(200),
    accion        VARCHAR(20)  NOT NULL
        CHECK (accion IN ('CREAR', 'LEER', 'ACTUALIZAR', 'ELIMINAR')),
    entidad       VARCHAR(100),
    id_entidad    VARCHAR(200),
    detalle       TEXT,
    ip_origen     VARCHAR(45)
);

-- ============================================================
-- ÍNDICES sobre claves foráneas principales
-- Mejoran el rendimiento de consultas frecuentes y JOINs.
-- ============================================================

-- Índice: búsqueda de pacientes por sanitario asignado
CREATE INDEX idx_paciente_dni_san    ON paciente(dni_san);

-- Índices: consultas de citas por paciente y por sanitario
CREATE INDEX idx_cita_dni_pac        ON cita(dni_pac);
CREATE INDEX idx_cita_dni_san        ON cita(dni_san);

-- Índice: teléfonos de sanitarios
CREATE INDEX idx_telefono_san_dni    ON telefono_sanitario(dni_san);

-- Índice: teléfonos de pacientes
CREATE INDEX idx_telefono_pac_dni    ON telefono_paciente(dni_pac);

-- Índices: consultas del registro de auditoría por fecha y usuario
CREATE INDEX idx_audit_log_fecha       ON audit_log(fecha_hora);
CREATE INDEX idx_audit_log_dni_usuario ON audit_log(dni_usuario);
