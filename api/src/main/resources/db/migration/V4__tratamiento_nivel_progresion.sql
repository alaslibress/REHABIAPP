-- Migración V4: Vincular tratamientos a niveles de progresión clínica
-- Permite filtrar tratamientos por el nivel de progresión del paciente
-- (agudo, subagudo, fortalecimiento, funcional).
--
-- Prioridad BAJA — no bloqueante para la migración del desktop ERP.
-- La columna es nullable para mantener compatibilidad con tratamientos
-- existentes que no tienen nivel asignado.

ALTER TABLE tratamiento
    ADD COLUMN id_nivel INTEGER REFERENCES nivel_progresion(id_nivel);

-- Índice para acelerar el filtrado de tratamientos por nivel de progresión
CREATE INDEX idx_tratamiento_nivel ON tratamiento(id_nivel);
