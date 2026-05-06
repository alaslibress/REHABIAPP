-- V13: Videojuegos terapeuticos, asociacion con tratamientos,
-- almacenamiento de PDF en tratamiento y cache de progreso MD en paciente.
--
-- Cambios:
--   - Tabla nueva: videojuego (catalogo de minijuegos terapeuticos)
--   - Tabla nueva: tratamiento_videojuego (asociacion N:M)
--   - Columnas nuevas en tratamiento: archivo_pdf, nombre_archivo_pdf, tamano_pdf_bytes
--   - Columnas nuevas en paciente: archivo_progreso_md, progreso_md_actualizado_en
--   - Tablas Envers: videojuego_audit, tratamiento_videojuego_audit

-- ============================================================
-- TABLA: videojuego
-- Catalogo maestro de minijuegos terapeuticos hospedados en AWS S3.
-- ============================================================
CREATE TABLE videojuego (
    id_videojuego   BIGSERIAL PRIMARY KEY,
    codigo          VARCHAR(50)  NOT NULL UNIQUE,
    nombre          VARCHAR(200) NOT NULL,
    descripcion     TEXT,
    cod_dis         VARCHAR(20)  NOT NULL,
    parte_cuerpo    VARCHAR(100) NOT NULL,
    url_unity       VARCHAR(500),
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_videojuego_dis
        FOREIGN KEY (cod_dis) REFERENCES discapacidad(cod_dis) ON DELETE RESTRICT
);
CREATE INDEX idx_videojuego_cod_dis ON videojuego(cod_dis);
CREATE INDEX idx_videojuego_activo  ON videojuego(activo);

-- ============================================================
-- TABLA: tratamiento_videojuego
-- Asociacion N:M entre tratamientos y videojuegos.
-- ============================================================
CREATE TABLE tratamiento_videojuego (
    cod_trat        VARCHAR(20) NOT NULL,
    id_videojuego   BIGINT      NOT NULL,
    fecha_vinculo   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (cod_trat, id_videojuego),
    CONSTRAINT fk_tv_trat FOREIGN KEY (cod_trat)      REFERENCES tratamiento(cod_trat)    ON DELETE CASCADE,
    CONSTRAINT fk_tv_jue  FOREIGN KEY (id_videojuego) REFERENCES videojuego(id_videojuego) ON DELETE CASCADE
);

-- ============================================================
-- ALTER tratamiento — soporte para PDF (max 10 MB)
-- ============================================================
ALTER TABLE tratamiento
    ADD COLUMN archivo_pdf BYTEA,
    ADD COLUMN nombre_archivo_pdf VARCHAR(255),
    ADD COLUMN tamano_pdf_bytes BIGINT,
    ADD CONSTRAINT chk_tamano_pdf CHECK (tamano_pdf_bytes IS NULL OR tamano_pdf_bytes <= 10485760);

-- ============================================================
-- ALTER paciente — cache de progreso markdown
-- ============================================================
ALTER TABLE paciente
    ADD COLUMN archivo_progreso_md TEXT,
    ADD COLUMN progreso_md_actualizado_en TIMESTAMP;

-- ============================================================
-- TABLA: videojuego_audit
-- Historial de cambios sobre la entidad videojuego (Hibernate Envers).
-- rev_type: 0=INSERT, 1=UPDATE, 2=DELETE.
-- ============================================================
CREATE TABLE videojuego_audit (
    id_videojuego   BIGINT       NOT NULL,
    rev             INTEGER      NOT NULL REFERENCES revinfo(rev),
    rev_type        SMALLINT,
    codigo          VARCHAR(50),
    nombre          VARCHAR(200),
    descripcion     TEXT,
    cod_dis         VARCHAR(20),
    parte_cuerpo    VARCHAR(100),
    url_unity       VARCHAR(500),
    activo          BOOLEAN,
    fecha_creacion  TIMESTAMP,
    PRIMARY KEY (id_videojuego, rev)
);

-- ============================================================
-- TABLA: tratamiento_videojuego_audit
-- Historial de cambios sobre la asociacion tratamiento-videojuego.
-- ============================================================
CREATE TABLE tratamiento_videojuego_audit (
    cod_trat        VARCHAR(20) NOT NULL,
    id_videojuego   BIGINT      NOT NULL,
    rev             INTEGER     NOT NULL REFERENCES revinfo(rev),
    rev_type        SMALLINT,
    fecha_vinculo   TIMESTAMP,
    PRIMARY KEY (cod_trat, id_videojuego, rev)
);
