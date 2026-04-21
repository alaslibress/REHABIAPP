-- ============================================================
-- V10__fix_audit_columns.sql
-- Agrega columnas faltantes en tablas de auditoria Envers.
-- Diagnosticadas comparando entidades @Audited contra V9.
--
-- Problema: V9 creo tablas audit incompletas respecto a las
-- entidades JPA actuales. Envers falla con INSERT al crear/editar
-- pacientes, asignaciones de discapacidad y tratamientos.
-- ============================================================

-- paciente_audit: FK sanitario, FK direccion y foto faltantes
-- sanitario: @ManyToOne sin @NotAudited -> FK dni_san auditada
-- direccion: @Audited(NOT_AUDITED) -> FK id_direccion auditada
-- foto: @Column byte[] sin @NotAudited -> campo auditado
ALTER TABLE paciente_audit ADD COLUMN IF NOT EXISTS dni_san      VARCHAR(20);
ALTER TABLE paciente_audit ADD COLUMN IF NOT EXISTS id_direccion INTEGER;
ALTER TABLE paciente_audit ADD COLUMN IF NOT EXISTS foto         BYTEA;

-- paciente_discapacidad_audit: fecha_asignacion faltante
-- @Column fecha_asignacion sin @NotAudited -> campo auditado
ALTER TABLE paciente_discapacidad_audit ADD COLUMN IF NOT EXISTS fecha_asignacion TIMESTAMP;

-- paciente_tratamiento_audit: fecha_asignacion faltante
-- @Column fecha_asignacion sin @NotAudited -> campo auditado
ALTER TABLE paciente_tratamiento_audit ADD COLUMN IF NOT EXISTS fecha_asignacion TIMESTAMP;
