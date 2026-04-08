-- ============================================================
-- V8__fix_asignaciones_y_drop_legacy.sql
-- Alinea el esquema real de paciente_discapacidad y
-- paciente_tratamiento con las entidades JPA y elimina los
-- campos legacy discapacidad_pac, tratamiento_pac y
-- estado_tratamiento de paciente y paciente_audit.
--
-- Migracion idempotente: usa bloques DO $$ y IF EXISTS para
-- no fallar si ya se aplico manualmente en entornos previos.
-- ============================================================

-- 1) Renombrar id_nivel -> id_nivel_actual en paciente_discapacidad
--    (solo si la columna antigua existe)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name='paciente_discapacidad' AND column_name='id_nivel') THEN
        ALTER TABLE paciente_discapacidad RENAME COLUMN id_nivel TO id_nivel_actual;
    END IF;
END $$;

-- 2) Simplificar paciente_tratamiento: quitar cod_dis y dni_san_asigna, arreglar PK y visible
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name='paciente_tratamiento' AND column_name='cod_dis') THEN
        ALTER TABLE paciente_tratamiento DROP CONSTRAINT paciente_tratamiento_pkey;
        ALTER TABLE paciente_tratamiento DROP COLUMN cod_dis;
        ALTER TABLE paciente_tratamiento ADD PRIMARY KEY (dni_pac, cod_trat);
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name='paciente_tratamiento' AND column_name='dni_san_asigna') THEN
        ALTER TABLE paciente_tratamiento DROP COLUMN dni_san_asigna;
    END IF;
END $$;

ALTER TABLE paciente_tratamiento ALTER COLUMN visible SET DEFAULT TRUE;

-- 3) Eliminar campos legacy de paciente
ALTER TABLE paciente DROP COLUMN IF EXISTS discapacidad_pac;
ALTER TABLE paciente DROP COLUMN IF EXISTS tratamiento_pac;
ALTER TABLE paciente DROP COLUMN IF EXISTS estado_tratamiento;

-- 4) Eliminar mismos campos del audit Envers
ALTER TABLE paciente_audit DROP COLUMN IF EXISTS discapacidad_pac;
ALTER TABLE paciente_audit DROP COLUMN IF EXISTS tratamiento_pac;
ALTER TABLE paciente_audit DROP COLUMN IF EXISTS estado_tratamiento;
