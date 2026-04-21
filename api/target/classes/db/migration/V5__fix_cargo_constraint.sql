-- V5: Alinear constraint de cargo con el enum Java Rol (SPECIALIST, NURSE).
--
-- La tabla sanitario_agrega_sanitario tenia CHECK con valores en castellano
-- ('medico especialista', 'enfermero') que no coincidian con el enum
-- @Enumerated(EnumType.STRING) de SanitarioRol.java ('SPECIALIST', 'NURSE').
-- Esta migracion actualiza los datos existentes y corrige el constraint.

-- 1. Actualizar datos existentes al nuevo formato del enum Java
UPDATE sanitario_agrega_sanitario
SET cargo = CASE cargo
    WHEN 'medico especialista' THEN 'SPECIALIST'
    WHEN 'enfermero'           THEN 'NURSE'
    ELSE cargo
END;

-- 2. Reemplazar el constraint
ALTER TABLE sanitario_agrega_sanitario
    DROP CONSTRAINT sanitario_agrega_sanitario_cargo_check;

ALTER TABLE sanitario_agrega_sanitario
    ADD CONSTRAINT sanitario_agrega_sanitario_cargo_check
    CHECK (cargo IN ('SPECIALIST', 'NURSE'));
