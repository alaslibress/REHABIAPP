package com.rehabiapp.api.domain.repository;

import com.rehabiapp.api.domain.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repositorio de dominio para la entidad AuditLog.
 *
 * <p>Solo soporta operaciones de inserción. La tabla audit_log
 * es append-only por política de seguridad y cumplimiento legal.</p>
 *
 * <p>Se extienden únicamente las operaciones de JpaRepository necesarias.
 * Las operaciones de actualización y borrado heredadas no deben invocarse
 * sobre registros de auditoría (ENS Alto, RGPD Art. 30).</p>
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    // Solo se utiliza save() heredado de JpaRepository para insertar entradas de auditoría
}
