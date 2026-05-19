-- ============================================================
-- V20__audit_log_id_uuid.sql
-- Alinear audit_log.id_audit con la entidad JPA AuditLog (UUID).
--
-- V1 creo audit_log.id_audit como BIGSERIAL pero la entidad JPA
-- usa @GeneratedValue(strategy = GenerationType.UUID). En fresh DB
-- esto provoca "column id_audit is of type bigint but expression is of type uuid".
--
-- audit_log es append-only y aun no tiene datos en fresh deploy AWS Academy.
-- Drop + recreate manteniendo constraints + indices + check de V12.
-- ============================================================

DROP TABLE IF EXISTS audit_log CASCADE;

CREATE TABLE audit_log (
    id_audit       UUID         PRIMARY KEY,
    fecha_hora     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dni_usuario    VARCHAR(20),
    nombre_usuario VARCHAR(200),
    accion         VARCHAR(20)  NOT NULL,
    entidad        VARCHAR(100),
    id_entidad     VARCHAR(200),
    detalle        TEXT,
    ip_origen      VARCHAR(45),
    CONSTRAINT audit_log_accion_check CHECK (accion IN (
        'LOGIN', 'LOGOUT', 'CREATE', 'READ', 'UPDATE',
        'SOFT_DELETE', 'DELETE', 'EXPORT', 'PRINT', 'CAMBIO_CONTRASENA'
    ))
);

CREATE INDEX idx_audit_log_fecha       ON audit_log(fecha_hora);
CREATE INDEX idx_audit_log_dni_usuario ON audit_log(dni_usuario);
