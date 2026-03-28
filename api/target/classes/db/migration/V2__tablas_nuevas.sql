-- ============================================================
-- V2__tablas_nuevas.sql
-- Tablas de relación paciente-discapacidad y paciente-tratamiento,
-- junto con el catálogo de niveles de progresión clínica.
-- Extienden el esquema core (V1) con funcionalidad de seguimiento
-- terapéutico por niveles de progresión.
-- ============================================================

-- --------------------------------------------------------
-- TABLA: nivel_progresion
-- Catálogo de niveles de progresión clínica de los tratamientos.
-- El campo "orden" define la secuencia terapéutica ascendente.
-- --------------------------------------------------------
CREATE TABLE nivel_progresion (
    id_nivel    SERIAL       PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL UNIQUE,
    orden       INTEGER      NOT NULL UNIQUE,
    descripcion TEXT
);

-- --------------------------------------------------------
-- TABLA: paciente_discapacidad
-- Asignación de discapacidades a pacientes con seguimiento
-- del nivel de progresión clínica actual.
-- --------------------------------------------------------
CREATE TABLE paciente_discapacidad (
    dni_pac          VARCHAR(20) REFERENCES paciente(dni_pac)       ON DELETE CASCADE,
    cod_dis          VARCHAR(20) REFERENCES discapacidad(cod_dis),
    id_nivel         INTEGER     REFERENCES nivel_progresion(id_nivel),
    fecha_asignacion TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notas            TEXT,
    PRIMARY KEY (dni_pac, cod_dis)
);

-- --------------------------------------------------------
-- TABLA: paciente_tratamiento
-- Visibilidad de tratamientos por paciente.
-- Permite al sanitario ocultar temporalmente tratamientos
-- sin eliminar la asignación clínica.
-- --------------------------------------------------------
CREATE TABLE paciente_tratamiento (
    dni_pac          VARCHAR(20) REFERENCES paciente(dni_pac)   ON DELETE CASCADE,
    cod_trat         VARCHAR(20) REFERENCES tratamiento(cod_trat),
    visible          BOOLEAN     NOT NULL DEFAULT TRUE,
    fecha_asignacion TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (dni_pac, cod_trat)
);
