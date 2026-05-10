-- ============================================================
-- V14__datos_prueba_desarrollo.sql
-- Datos de prueba para desarrollo local y testing de integracion.
--
-- Usuario de prueba: Admin RehabiAPP (DNI 12345678Z).
-- Coincide con las credenciales mock del BFF movil (credenciales.md).
-- Idempotente: todos los INSERT usan ON CONFLICT DO NOTHING.
--
-- IMPORTANTE: No incluir en produccion. Solo para entornos dev/staging.
-- ============================================================

-- ============================================================
-- SANITARIO DE PRUEBA (referenciado por el paciente de prueba)
-- DNI 87654321B — valido: 87654321 mod 23 = 19 -> letra B
-- Contrasena: 'especialista' — hash BCrypt cost 12
-- ============================================================

INSERT INTO sanitario (
    dni_san, nombre_san, apellido1_san, apellido2_san,
    email_san, num_de_pacientes, contrasena_san, activo
) VALUES (
    '87654321B', 'Doctora', 'Prueba', 'Rehabilitacion',
    'especialista@rehabiapp.com', 1,
    '$2a$12$YoS0BYVB5LdCy6oVDHUjXeVYaqTB62jJ5S1M2Gk6GG6rdHPd7zui',
    TRUE
) ON CONFLICT (dni_san) DO NOTHING;

INSERT INTO sanitario_agrega_sanitario (dni_san, cargo)
VALUES ('87654321B', 'SPECIALIST')
ON CONFLICT (dni_san) DO NOTHING;

-- ============================================================
-- DISCAPACIDADES DE PRUEBA
-- M16 — Coxartrosis (artrosis de cadera)
-- M54 — Lumbalgia cronica (dolor lumbar cronico)
-- ============================================================

INSERT INTO discapacidad (cod_dis, nombre_dis, descripcion_dis, necesita_protesis)
VALUES
    ('M16', 'Coxartrosis',
     'Artrosis de la articulacion coxofemoral. Degeneracion progresiva del cartilago de la cadera.',
     FALSE),
    ('M54', 'Lumbalgia cronica',
     'Dolor cronico en la region lumbar de la columna vertebral. Duracion superior a 12 semanas.',
     FALSE)
ON CONFLICT (cod_dis) DO NOTHING;

-- ============================================================
-- TRATAMIENTOS DE PRUEBA
-- TRT001, TRT003 para M16 (cadera), TRT002 para M54 (lumbar)
-- TRT004 (hidroterapia) vinculado a M16 pero oculto al paciente
-- ============================================================

INSERT INTO tratamiento (cod_trat, nombre_trat, definicion_trat)
VALUES
    ('TRT001', 'Ejercicios de movilidad de cadera',
     'Serie de ejercicios terapeuticos para recuperar el rango articular de la cadera operada o con artrosis. Incluye flexion, extension, rotacion interna y externa.'),
    ('TRT002', 'Electroterapia de baja frecuencia',
     'Aplicacion de corriente electrica de baja frecuencia (TENS) para alivio del dolor lumbar cronico. Electrodos en zona paravertebral L4-L5.'),
    ('TRT003', 'Ejercicios de fortalecimiento lumbar',
     'Plan progresivo de ejercicios isometricos e isotonicos para fortalecer la musculatura paravertebral lumbar y el core abdominal.'),
    ('TRT004', 'Hidroterapia terapeutica',
     'Sesiones terapeuticas en piscina de rehabilitacion. Aprovecha la flotabilidad para ejercicios de bajo impacto articular.')
ON CONFLICT (cod_trat) DO NOTHING;

-- ============================================================
-- NIVELES PROGRESION en tratamientos
-- TRT001 -> nivel 2 (Fase Subaguda), TRT002 -> nivel 1 (Fase Aguda)
-- TRT003 -> nivel 1 (Fase Aguda), TRT004 -> nivel 0 (sin nivel)
-- ============================================================

UPDATE tratamiento SET id_nivel = 2 WHERE cod_trat = 'TRT001';
UPDATE tratamiento SET id_nivel = 1 WHERE cod_trat = 'TRT002';
UPDATE tratamiento SET id_nivel = 1 WHERE cod_trat = 'TRT003';

-- ============================================================
-- VINCULOS DISCAPACIDAD <-> TRATAMIENTO
-- ============================================================

INSERT INTO discapacidad_tratamiento (cod_dis, cod_trat)
VALUES
    ('M16', 'TRT001'),
    ('M16', 'TRT004'),
    ('M54', 'TRT002'),
    ('M54', 'TRT003')
ON CONFLICT (cod_dis, cod_trat) DO NOTHING;

-- ============================================================
-- DIRECCION DE PRUEBA
-- Requerida por la FK NOT NULL id_direccion de la tabla paciente.
-- ============================================================

INSERT INTO direccion (calle, numero, cp)
SELECT 'Calle Rehabilitacion', '1', '28001'
WHERE NOT EXISTS (
    SELECT 1 FROM direccion
    WHERE calle = 'Calle Rehabilitacion' AND numero = '1' AND cp = '28001'
);

-- ============================================================
-- PACIENTE DE PRUEBA
-- DNI 12345678Z — valido: 12345678 mod 23 = 14 -> letra Z
-- Referenciado por las credenciales mock del BFF (MOCK_API=true).
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
    '12345678Z', '87654321B',
    'Admin', 'RehabiAPP', NULL,
    36, 'admin@rehabiapp.com', '280000000001',
    d.id_direccion,
    '1990-01-01', 'MASCULINO',
    FALSE, TRUE, TRUE
FROM direccion d
WHERE d.calle = 'Calle Rehabilitacion' AND d.numero = '1' AND d.cp = '28001'
LIMIT 1
ON CONFLICT (dni_pac) DO NOTHING;

INSERT INTO telefono_paciente (dni_pac, telefono)
SELECT '12345678Z', '600000000'
WHERE NOT EXISTS (
    SELECT 1 FROM telefono_paciente WHERE dni_pac = '12345678Z' AND telefono = '600000000'
);

-- ============================================================
-- DISCAPACIDADES DEL PACIENTE DE PRUEBA
-- M16 a nivel 2 (Fase Subaguda), M54 a nivel 1 (Fase Aguda)
-- ============================================================

INSERT INTO paciente_discapacidad (dni_pac, cod_dis, id_nivel_actual, fecha_asignacion, notas)
VALUES
    ('12345678Z', 'M16', 2, '2024-03-10 09:00:00',
     'Cadera derecha afectada principalmente. Post-operatorio de artroplastia total.'),
    ('12345678Z', 'M54', 1, '2024-06-01 10:30:00',
     NULL)
ON CONFLICT (dni_pac, cod_dis) DO NOTHING;

-- ============================================================
-- TRATAMIENTOS DEL PACIENTE DE PRUEBA
-- TRT001-TRT003 visibles, TRT004 oculto
-- ============================================================

INSERT INTO paciente_tratamiento (dni_pac, cod_trat, visible, fecha_asignacion)
VALUES
    ('12345678Z', 'TRT001', TRUE,  '2024-03-15 09:00:00'),
    ('12345678Z', 'TRT002', TRUE,  '2024-03-15 09:00:00'),
    ('12345678Z', 'TRT003', TRUE,  '2024-06-05 11:00:00'),
    ('12345678Z', 'TRT004', FALSE, '2024-06-05 11:00:00')
ON CONFLICT (dni_pac, cod_trat) DO NOTHING;

-- ============================================================
-- VIDEOJUEGO DE PRUEBA
-- GAME-HIP-01 — vinculado a M16 (cadera) y al tratamiento TRT001
-- ============================================================

INSERT INTO videojuego (codigo, nombre, descripcion, cod_dis, parte_cuerpo, url_unity, activo)
VALUES (
    'GAME-HIP-01',
    'Mover la cadera',
    'Minijuego de rehabilitacion de cadera mediante movimientos controlados de flexion y extension.',
    'M16',
    'CADERA',
    'https://games.rehabiapp.com/hip-01',
    TRUE
) ON CONFLICT (codigo) DO NOTHING;

INSERT INTO tratamiento_videojuego (cod_trat, id_videojuego)
SELECT 'TRT001', id_videojuego
FROM videojuego
WHERE codigo = 'GAME-HIP-01'
ON CONFLICT (cod_trat, id_videojuego) DO NOTHING;

-- ============================================================
-- CITAS DEL PACIENTE DE PRUEBA
-- Tres proximas citas con el especialista de prueba
-- ============================================================

INSERT INTO cita (dni_pac, dni_san, fecha_cita, hora)
VALUES
    ('12345678Z', '87654321B', '2026-04-10', '10:00:00'),
    ('12345678Z', '87654321B', '2026-04-17', '11:30:00'),
    ('12345678Z', '87654321B', '2026-04-24', '09:00:00')
ON CONFLICT (dni_pac, dni_san, fecha_cita, hora) DO NOTHING;
