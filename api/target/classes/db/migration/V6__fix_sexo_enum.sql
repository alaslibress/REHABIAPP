-- Correccion: ampliar columna sexo y actualizar constraint para que coincida
-- con el enum Sexo de la entidad JPA (MASCULINO, FEMENINO, OTRO).
-- V1 creaba VARCHAR(1) CHECK IN ('M','F','O') pero la entidad usa EnumType.STRING
-- con valores completos, por lo que la lectura fallaba con InvalidDataAccessApiUsageException.

ALTER TABLE paciente ALTER COLUMN sexo TYPE VARCHAR(20);

ALTER TABLE paciente DROP CONSTRAINT IF EXISTS paciente_sexo_check;

ALTER TABLE paciente ADD CONSTRAINT paciente_sexo_check
    CHECK (sexo IN ('MASCULINO', 'FEMENINO', 'OTRO'));
