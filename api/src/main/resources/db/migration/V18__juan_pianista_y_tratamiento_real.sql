-- ============================================================
-- V18__juan_pianista_y_tratamiento_real.sql
--
-- Crea el paciente de pruebas E2E para el videojuego PIANO WebGL:
--   - Paciente "Juan Perez Garcia" con DNI 11111111H (mod 23 = 18 -> H, valido).
--   - Discapacidad nueva 'M-PIANO' (rehabilitacion movilidad fina manos).
--   - Tratamiento 'REAL' (idempotente — solo crea si no existe).
--   - Videojuego 'PIANO-001' enlazado al bucket S3 publico.
--   - Vinculos discapacidad <-> tratamiento <-> videojuego <-> paciente.
--
-- Idempotente: ON CONFLICT DO NOTHING + guards WHERE NOT EXISTS.
-- Hash BCrypt de la contrasena 'Juan1234!' generado offline (cost factor 12).
-- ============================================================

-- ============================================================
-- 1) DIRECCION para Juan — reusa CP 28001 existente del seed V14.
-- ============================================================

INSERT INTO direccion (calle, numero, cp)
SELECT 'Calle Piano', '7', '28001'
WHERE NOT EXISTS (
    SELECT 1 FROM direccion
    WHERE calle = 'Calle Piano' AND numero = '7' AND cp = '28001'
);

-- ============================================================
-- 2) DISCAPACIDAD nueva M-PIANO (rehabilitacion movilidad fina manos).
--    cod_dis VARCHAR(20) admite el guion.
-- ============================================================

INSERT INTO discapacidad (cod_dis, nombre_dis, descripcion_dis, necesita_protesis)
VALUES (
    'M-PIANO',
    'Rehabilitacion movilidad fina manos',
    'Tratamiento de rehabilitacion de la movilidad fina de dedos y mano mediante minijuegos musicales.',
    FALSE
) ON CONFLICT (cod_dis) DO NOTHING;

-- ============================================================
-- 3) TRATAMIENTO REAL — guard para no crear si ya existe.
-- ============================================================

INSERT INTO tratamiento (cod_trat, nombre_trat, definicion_trat)
SELECT 'REAL', 'Rehabilitacion movilidad dedos (REAL)',
       'Tratamiento de rehabilitacion de movilidad fina mediante minijuego PIANO WebGL.'
WHERE NOT EXISTS (SELECT 1 FROM tratamiento WHERE cod_trat = 'REAL');

-- Vincular tratamiento REAL <-> discapacidad M-PIANO.
INSERT INTO discapacidad_tratamiento (cod_dis, cod_trat)
SELECT 'M-PIANO', 'REAL'
WHERE NOT EXISTS (
    SELECT 1 FROM discapacidad_tratamiento WHERE cod_dis = 'M-PIANO' AND cod_trat = 'REAL'
);

-- ============================================================
-- 4) PACIENTE Juan — DNI 11111111H asignado al sanitario existente 87654321B.
-- ============================================================

INSERT INTO paciente (
    dni_pac, dni_san,
    nombre_pac, apellido1_pac, apellido2_pac,
    edad_pac, email_pac, num_ss,
    id_direccion,
    fecha_nacimiento, sexo,
    protesis, consentimiento_rgpd, activo
)
SELECT
    '11111111H', '87654321B',
    'Juan', 'Perez', 'Garcia',
    45, 'juan@rehabiapp.test', '280000000099',
    d.id_direccion,
    '1980-05-12', 'MASCULINO',
    FALSE, TRUE, TRUE
FROM direccion d
WHERE d.calle = 'Calle Piano' AND d.numero = '7' AND d.cp = '28001'
LIMIT 1
ON CONFLICT (dni_pac) DO NOTHING;

-- Hash BCrypt cost-12 de la contrasena 'Juan1234!' (generado offline).
UPDATE paciente
   SET contrasena_pac = '$2b$12$ozoOIETuKnLRZrZZK1WfnuM7lmYVyWk/.f9C9AXESUnV8zPLHX3/.'
 WHERE dni_pac = '11111111H'
   AND contrasena_pac IS NULL;

INSERT INTO telefono_paciente (dni_pac, telefono)
SELECT '11111111H', '600111111'
WHERE NOT EXISTS (
    SELECT 1 FROM telefono_paciente WHERE dni_pac = '11111111H' AND telefono = '600111111'
);

-- ============================================================
-- 5) Asociar Juan a discapacidad M-PIANO (nivel 1, fase aguda).
-- ============================================================

INSERT INTO paciente_discapacidad (dni_pac, cod_dis, id_nivel_actual, fecha_asignacion, notas)
VALUES ('11111111H', 'M-PIANO', 1, CURRENT_TIMESTAMP,
        'Paciente de prueba E2E para juego PIANO WebGL.')
ON CONFLICT (dni_pac, cod_dis) DO NOTHING;

-- ============================================================
-- 6) Asignar tratamiento REAL al paciente Juan (visible en la app).
-- ============================================================

INSERT INTO paciente_tratamiento (dni_pac, cod_trat, visible, fecha_asignacion)
VALUES ('11111111H', 'REAL', TRUE, CURRENT_TIMESTAMP)
ON CONFLICT (dni_pac, cod_trat) DO NOTHING;

-- ============================================================
-- 7) VIDEOJUEGO PIANO-001 — catalogo + url S3 publica.
-- ============================================================

INSERT INTO videojuego (codigo, nombre, descripcion, cod_dis, parte_cuerpo, url_unity, activo)
VALUES (
    'PIANO-001',
    'Piano Rehabilitacion',
    'Minijuego WebGL para rehabilitacion de movilidad fina de dedos. Build Unity en AWS S3.',
    'M-PIANO',
    'MANO_DERECHA',
    'http://s3-bucket-rehabiapp-piano-640681720314.s3-website-us-east-1.amazonaws.com',
    TRUE
) ON CONFLICT (codigo) DO NOTHING;

-- Vincular videojuego PIANO-001 al tratamiento REAL.
INSERT INTO tratamiento_videojuego (cod_trat, id_videojuego)
SELECT 'REAL', v.id_videojuego
  FROM videojuego v
 WHERE v.codigo = 'PIANO-001'
ON CONFLICT (cod_trat, id_videojuego) DO NOTHING;
