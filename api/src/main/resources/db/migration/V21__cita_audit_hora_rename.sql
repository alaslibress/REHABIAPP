-- ============================================================
-- V21__cita_audit_hora_rename.sql
--
-- Alinear cita_audit.hora con la columna real de cita (hora_cita).
--
-- Envers genera cita_audit a partir del mismo @Column de CitaId. La
-- entidad usa @Column(name = "hora_cita"), pero V3/V9 crearon
-- cita_audit con columna "hora" — esto provoca al insertar/actualizar
-- una cita: "column hora_cita of relation cita_audit does not exist".
--
-- Renombramos la columna; los registros existentes (si los hay)
-- conservan sus valores.
-- ============================================================

ALTER TABLE cita_audit
    RENAME COLUMN hora TO hora_cita;
