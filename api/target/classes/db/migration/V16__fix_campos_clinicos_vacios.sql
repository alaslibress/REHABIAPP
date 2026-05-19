-- V16__fix_campos_clinicos_vacios.sql
--
-- Las columnas cifradas con AES-256-GCM (alergias, antecedentes, medicacion_actual)
-- deben almacenarse como NULL cuando no tienen valor, nunca como cadena vacia ''.
-- Una cadena vacia no es Base64 valido de un bloque GCM y rompe el descifrado con
-- NegativeArraySizeException (-12) en CampoClinicoConverter.convertToEntityAttribute.
--
-- Causa: insercion directa de datos de prueba en V14 que omitio los campos pero
-- la BD tenia un default '' por un esquema anterior (ya corregido).
-- Este script normaliza cualquier '' existente a NULL para todos los pacientes.

UPDATE paciente
SET alergias = NULL
WHERE alergias = '';

UPDATE paciente
SET antecedentes = NULL
WHERE antecedentes = '';

UPDATE paciente
SET medicacion_actual = NULL
WHERE medicacion_actual = '';
