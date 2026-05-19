-- V17: Tabla de informes de sesion de juego (resumen Markdown derivado de MongoDB)
-- Cache derivado: el registro principal vive en MongoDB (game_sessions).
-- Este registro facilita reportes SQL y busqueda futura via tsvector.

CREATE TABLE session_reports (
    id              BIGSERIAL    PRIMARY KEY,
    mongo_id        VARCHAR(48)  NOT NULL UNIQUE,
    paciente_dni    VARCHAR(20)  NOT NULL,
    cod_juego       VARCHAR(64)  NOT NULL,
    cod_trat        VARCHAR(32),
    fecha_sesion    TIMESTAMPTZ  NOT NULL,
    duracion_seg    INTEGER      NOT NULL,
    contenido_md    TEXT         NOT NULL,
    md_hash         VARCHAR(64)  NOT NULL,
    fecha_creacion  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_session_reports_paciente
        FOREIGN KEY (paciente_dni) REFERENCES paciente(dni_pac)
        ON DELETE RESTRICT
);

CREATE INDEX idx_session_reports_paciente_fecha
    ON session_reports (paciente_dni, fecha_sesion DESC);

CREATE INDEX idx_session_reports_cod_juego
    ON session_reports (cod_juego);

COMMENT ON TABLE session_reports IS
    'Resumen Markdown de cada sesion de juego. Cache derivado de game_sessions en MongoDB.';
COMMENT ON COLUMN session_reports.mongo_id IS
    'ID del documento GameSession en MongoDB. Clave de idempotencia.';
COMMENT ON COLUMN session_reports.contenido_md IS
    'Contenido Markdown generado por el pipeline /data. TEXT permite tsvector futuro.';
COMMENT ON COLUMN session_reports.md_hash IS
    'SHA-256 del contenido Markdown. Permite detectar cambios en regeneraciones.';
