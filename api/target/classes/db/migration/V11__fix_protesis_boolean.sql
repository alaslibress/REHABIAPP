-- V11: Convertir columna protesis de INTEGER a BOOLEAN en tabla paciente.
--
-- Problema: el schema legacy del desktop creo protesis como INTEGER (0/1).
-- La entidad JPA Paciente.java lo mapea como boolean.
-- Hibernate falla en UPDATE con "column protesis is of type integer but
-- expression is of type boolean".
--
-- La columna puede tener DEFAULT 0 (legacy) que impide el cast directo.
-- Solucion: DROP DEFAULT → ALTER TYPE con USING → SET DEFAULT FALSE.
-- Idempotente: solo actua si la columna sigue siendo INTEGER.

DO $$
DECLARE
    col_type TEXT;
BEGIN
    SELECT data_type INTO col_type
    FROM information_schema.columns
    WHERE table_name = 'paciente' AND column_name = 'protesis';

    IF col_type = 'integer' THEN
        ALTER TABLE paciente ALTER COLUMN protesis DROP DEFAULT;
        ALTER TABLE paciente ALTER COLUMN protesis TYPE BOOLEAN
            USING (protesis <> 0);
        ALTER TABLE paciente ALTER COLUMN protesis SET DEFAULT FALSE;
    END IF;
END $$;
