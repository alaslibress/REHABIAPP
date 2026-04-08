-- ============================================================
-- V2__tablas_nuevas.sql
-- Tablas de relación paciente-discapacidad y paciente-tratamiento,
-- junto con el catálogo de niveles de progresión clínica.
-- Extienden el esquema core (V1) con funcionalidad de seguimiento
-- terapéutico por niveles de progresión.
-- ============================================================

-- --------------------------------------------------------
-- TABLA: nivel_progresion
-- Catalogo de niveles de progresion clinica de los tratamientos.
-- El campo "orden" define la secuencia terapeutica ascendente.
-- Los campos nombre_corto, estado_pac y tipos_ejercicio
-- amplian la descripcion clinica para el desktop.
-- --------------------------------------------------------
CREATE TABLE nivel_progresion (
    id_nivel        SERIAL       PRIMARY KEY,
    nombre          VARCHAR(100) NOT NULL UNIQUE,
    nombre_corto    VARCHAR(50)  NOT NULL,
    descripcion     TEXT,
    estado_pac      TEXT,
    tipos_ejercicio TEXT,
    orden           INTEGER      NOT NULL UNIQUE
);

-- --------------------------------------------------------
-- TABLA: paciente_discapacidad
-- Asignación de discapacidades a pacientes con seguimiento
-- del nivel de progresión clínica actual.
-- --------------------------------------------------------
CREATE TABLE paciente_discapacidad (
    dni_pac          VARCHAR(9)  NOT NULL REFERENCES paciente(dni_pac) ON DELETE CASCADE,
    cod_dis          VARCHAR(10) NOT NULL REFERENCES discapacidad(cod_dis),
    id_nivel_actual  INTEGER     NOT NULL DEFAULT 1 REFERENCES nivel_progresion(id_nivel),
    fecha_asignacion TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    notas            TEXT,
    PRIMARY KEY (dni_pac, cod_dis)
);
CREATE INDEX idx_pac_dis_paciente     ON paciente_discapacidad (dni_pac);
CREATE INDEX idx_pac_dis_discapacidad ON paciente_discapacidad (cod_dis);

-- --------------------------------------------------------
-- TABLA: paciente_tratamiento
-- Visibilidad de tratamientos por paciente.
-- Permite al sanitario ocultar temporalmente tratamientos
-- sin eliminar la asignación clínica.
-- --------------------------------------------------------
CREATE TABLE paciente_tratamiento (
    dni_pac          VARCHAR(9)  NOT NULL REFERENCES paciente(dni_pac) ON DELETE CASCADE,
    cod_trat         VARCHAR(10) NOT NULL REFERENCES tratamiento(cod_trat),
    visible          BOOLEAN     NOT NULL DEFAULT TRUE,
    fecha_asignacion TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (dni_pac, cod_trat)
);
CREATE INDEX idx_pac_trat_paciente ON paciente_tratamiento (dni_pac);
CREATE INDEX idx_pac_trat_visible  ON paciente_tratamiento (dni_pac) WHERE visible = TRUE;
