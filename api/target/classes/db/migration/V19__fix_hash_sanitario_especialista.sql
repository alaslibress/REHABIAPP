-- ============================================================
-- V19__fix_hash_sanitario_especialista.sql
--
-- Corrige el hash BCrypt del sanitario de pruebas 87654321B.
-- En V14 el hash almacenado no correspondia a la contrasena 'especialista'
-- (probable typo o regeneracion), impidiendo el login del sanitario.
--
-- Idempotente: el UPDATE solo se aplica si el hash actual sigue siendo el
-- valor original de V14 — no pisa cambios manuales posteriores.
-- ============================================================

UPDATE sanitario
   SET contrasena_san = '$2b$12$VSmS3mo00W.8tx6KE1T1AuYr2QZMKiPc8P6KLz5akKXctp4ag6s2G'
 WHERE dni_san = '87654321B'
   AND contrasena_san = '$2a$12$YoS0BYVB5LdCy6oVDHUjXeVYaqTB62jJ5S1M2Gk6GG6rdHPd7zui';
