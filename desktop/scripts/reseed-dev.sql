-- Reseed de datos de desarrollo para el desktop SGE.
-- SOLO para desarrollo local. NO ejecutar en produccion.
--
-- Genera:
--   - 3 sanitarios: admin (SPECIALIST), Carlos Garcia (SPECIALIST), Lucia Martinez (NURSE)
--   - 1 paciente de ejemplo: Pedro Sanchez, asignado al medico Carlos Garcia
--
-- Credenciales documentadas en desktop/scripts/README.md.
--
-- Aplicar con:
--   docker exec -i rehabiapp-db psql -U admin -d rehabiapp < desktop/scripts/reseed-dev.sql

BEGIN;

-- -------------------------------------------------------------------------
-- 1. Limpiar datos previos en orden FK-safe
-- -------------------------------------------------------------------------
DELETE FROM cita;
DELETE FROM paciente_tratamiento;
DELETE FROM paciente_discapacidad;
DELETE FROM telefono_paciente;
DELETE FROM paciente;
DELETE FROM sanitario_agrega_sanitario;
DELETE FROM telefono_sanitario;
DELETE FROM sanitario;

-- -------------------------------------------------------------------------
-- 2. Asegurar localidad y CP de prueba (idempotente)
-- -------------------------------------------------------------------------
INSERT INTO localidad (nombre_localidad, provincia)
    VALUES ('Madrid', 'Madrid')
    ON CONFLICT (nombre_localidad) DO NOTHING;

INSERT INTO cp (cp, nombre_localidad)
    VALUES ('28001', 'Madrid')
    ON CONFLICT (cp) DO NOTHING;

-- -------------------------------------------------------------------------
-- 3. Direccion de prueba compartida para todos los usuarios de seed
-- -------------------------------------------------------------------------
INSERT INTO direccion (calle, numero, piso, cp)
    VALUES ('Calle de la Prueba', '1', '1A', '28001');

-- -------------------------------------------------------------------------
-- 4. Sanitarios con contrasenas BCrypt cost 12
-- Hashes generados el 2026-04-07 con:
--   htpasswd -nbBC 12 "" <pwd> | tr -d ':\n' | sed 's/$2y$/$2a$/'
-- -------------------------------------------------------------------------
INSERT INTO sanitario (
    dni_san, nombre_san, apellido1_san, apellido2_san,
    email_san, num_de_pacientes, contrasena_san, activo
) VALUES
    (
        'ADMIN0000',
        'Admin', 'Sistema', NULL,
        'admin@rehabiapp.local',
        0,
        '$2a$12$2roYx2xkdFiLTuzby1YQAe5K/WQur0I0rhFSv1jLpgSMl1bA7Kaee',
        TRUE
    ),
    (
        '00000001R',
        'Carlos', 'Garcia', 'Lopez',
        'carlos.garcia@rehabiapp.local',
        1,
        '$2a$12$gAHLIGx8cdZgiCDgo/VHAulGjBC3rWDUh6yGFbB.7dwDP6yd9OGUu',
        TRUE
    ),
    (
        '00000002W',
        'Lucia', 'Martinez', 'Ruiz',
        'lucia.martinez@rehabiapp.local',
        0,
        '$2a$12$lgz13jTXZqNhWK6Ex.iMMeUxJQSVWVd1/5H9RZwqrLaIv1KT8tpje',
        TRUE
    );

-- -------------------------------------------------------------------------
-- 5. Roles de los sanitarios
-- -------------------------------------------------------------------------
INSERT INTO sanitario_agrega_sanitario (dni_san, cargo) VALUES
    ('ADMIN0000',  'SPECIALIST'),
    ('00000001R',  'SPECIALIST'),
    ('00000002W',  'NURSE');

-- -------------------------------------------------------------------------
-- 6. Telefonos de los sanitarios (datos de prueba)
-- -------------------------------------------------------------------------
INSERT INTO telefono_sanitario (dni_san, telefono) VALUES
    ('ADMIN0000',  '600000000'),
    ('00000001R',  '600111111'),
    ('00000002W',  '600222222');

-- -------------------------------------------------------------------------
-- 7. Paciente de prueba asignado al medico especialista (00000001R)
--
-- NOTA: alergias, antecedentes, medicacion se insertan NULL
-- porque la app los espera cifrados con AES-256-GCM. Si se insertan
-- en texto plano, el ApiClient lanzara excepcion al deserializar.
-- Para rellenar campos clinicos, usar el API (POST /api/pacientes).
--
-- sexo usa CHECK (sexo IN ('M', 'F', 'O'))
-- protesis es INTEGER (0 = sin protesis)
-- -------------------------------------------------------------------------
INSERT INTO paciente (
    dni_pac, dni_san,
    nombre_pac, apellido1_pac, apellido2_pac,
    edad_pac, email_pac, num_ss,
    id_direccion,
    protesis,
    fecha_nacimiento, sexo,
    alergias, antecedentes, medicacion_actual,
    consentimiento_rgpd, fecha_consentimiento,
    activo
) VALUES (
    '00000003A', '00000001R',
    'Pedro', 'Sanchez', 'Gomez',
    45, 'pedro.sanchez@example.local', '281234567890',
    (SELECT MAX(id_direccion) FROM direccion),
    0,
    '1980-05-15', 'MASCULINO',
    NULL, NULL, NULL,
    TRUE, '2026-04-07',
    TRUE
);

-- -------------------------------------------------------------------------
-- 8. Telefono del paciente (dato de prueba)
-- -------------------------------------------------------------------------
INSERT INTO telefono_paciente (dni_pac, telefono)
    VALUES ('00000003A', '611000001');

COMMIT;

-- -------------------------------------------------------------------------
-- Verificacion final (solo lectura, no modifica nada)
-- -------------------------------------------------------------------------
SELECT 'sanitarios' AS tabla, COUNT(*) AS total FROM sanitario
UNION ALL
SELECT 'pacientes',           COUNT(*)            FROM paciente
UNION ALL
SELECT 'roles asignados',     COUNT(*)            FROM sanitario_agrega_sanitario;

SELECT s.dni_san, s.nombre_san, sas.cargo, s.activo
FROM sanitario s
LEFT JOIN sanitario_agrega_sanitario sas USING (dni_san)
ORDER BY s.dni_san;
