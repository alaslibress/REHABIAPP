-- V15: Aniade contrasena BCrypt a la tabla paciente para autenticacion movil.
--
-- El BFF movil llama a POST /api/auth/login-paciente con DNI + contrasena.
-- El API verifica el hash y emite un JWT con rol=PATIENT.
-- El hash se almacena como BCrypt cost 12 (misma politica que sanitarios).
--
-- Migracion idempotente: ADD COLUMN IF NOT EXISTS.
-- Nullable inicialmente para compatibilidad con registros existentes sin contrasena.

ALTER TABLE paciente
    ADD COLUMN IF NOT EXISTS contrasena_pac TEXT;

-- Actualizar hash del paciente de prueba de V14 (contrasena: 'admin', BCrypt cost 12)
UPDATE paciente
SET contrasena_pac = '$2a$12$SaIPLAYHVw6aqi5h1Jjwtec6JjvEdK54L8jw209hMVPwJdxKgZ3J2'
WHERE dni_pac = '12345678Z'
  AND contrasena_pac IS NULL;
