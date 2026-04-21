-- V12: Añadir valor DELETE al check constraint de audit_log.accion.
-- Los endpoints de desasignacion (paciente_discapacidad, paciente_tratamiento)
-- realizan borrado fisico de la asociacion, no soft-delete. AuditService
-- registra accion='DELETE' pero la constraint original no lo incluia.

ALTER TABLE audit_log DROP CONSTRAINT audit_log_accion_check;

ALTER TABLE audit_log ADD CONSTRAINT audit_log_accion_check
    CHECK (accion IN (
        'LOGIN', 'LOGOUT', 'CREATE', 'READ', 'UPDATE',
        'SOFT_DELETE', 'DELETE', 'EXPORT', 'PRINT', 'CAMBIO_CONTRASENA'
    ));
