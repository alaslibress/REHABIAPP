-- Migracion V13: Juegos terapeuticos y articulaciones
-- Permite vincular tratamientos clinicos a juegos Unity hospedados en AWS.
-- La articulacion taxonomiza partes del cuerpo alineadas con el diagrama SVG del frontend movil.

-- Taxonomia de articulaciones / partes del cuerpo
CREATE TABLE articulacion (
    id_articulacion SERIAL PRIMARY KEY,
    codigo          VARCHAR(32)  UNIQUE NOT NULL,
    nombre          VARCHAR(80)  NOT NULL
);

-- Seed alineado con BodyPartId de mobile/frontend/src/types/progress.ts
INSERT INTO articulacion (codigo, nombre) VALUES
  ('HEAD',           'Cabeza'),
  ('NECK',           'Cuello'),
  ('TORSO',          'Torso'),
  ('LEFT_SHOULDER',  'Hombro izquierdo'),
  ('RIGHT_SHOULDER', 'Hombro derecho'),
  ('LEFT_ARM',       'Brazo izquierdo'),
  ('RIGHT_ARM',      'Brazo derecho'),
  ('LEFT_HAND',      'Mano izquierda'),
  ('RIGHT_HAND',     'Mano derecha'),
  ('LEFT_HIP',       'Cadera izquierda'),
  ('RIGHT_HIP',      'Cadera derecha'),
  ('LEFT_LEG',       'Pierna izquierda'),
  ('RIGHT_LEG',      'Pierna derecha'),
  ('LEFT_FOOT',      'Pie izquierdo'),
  ('RIGHT_FOOT',     'Pie derecho');

-- FK nullable en discapacidad: asocia cada discapacidad a una articulacion afectada
ALTER TABLE discapacidad
    ADD COLUMN id_articulacion INTEGER NULL
        REFERENCES articulacion(id_articulacion) ON DELETE SET NULL;

-- Catalogo de juegos Unity disponibles en AWS S3/CloudFront
CREATE TABLE juego (
    cod_juego       VARCHAR(32)  PRIMARY KEY,
    nombre          VARCHAR(120) NOT NULL,
    descripcion     TEXT,
    url_juego       VARCHAR(400) NOT NULL,
    id_articulacion INTEGER      NOT NULL
        REFERENCES articulacion(id_articulacion) ON DELETE RESTRICT,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE
);

-- FK nullable en tratamiento: juego terapeutico asociado (uno a uno, opcional)
ALTER TABLE tratamiento
    ADD COLUMN cod_juego VARCHAR(32) NULL
        REFERENCES juego(cod_juego) ON DELETE SET NULL;

-- Indices de soporte para filtros frecuentes
CREATE INDEX idx_juego_articulacion   ON juego(id_articulacion);
CREATE INDEX idx_tratamiento_juego    ON tratamiento(cod_juego);
CREATE INDEX idx_discapacidad_articulacion ON discapacidad(id_articulacion);
