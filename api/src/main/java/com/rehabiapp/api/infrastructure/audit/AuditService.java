package com.rehabiapp.api.infrastructure.audit;

import com.rehabiapp.api.domain.entity.AuditLog;
import com.rehabiapp.api.domain.enums.AccionAuditoria;
import com.rehabiapp.api.domain.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * Servicio de auditoría — registra todas las operaciones incluyendo lecturas.
 *
 * <p>Complementa a Hibernate Envers (que solo audita escrituras) con el registro
 * de accesos de lectura sobre datos clínicos, cumpliendo la Ley 41/2002
 * (registro obligatorio de accesos a historiales).</p>
 *
 * <p>Obtiene automáticamente el DNI del usuario autenticado desde el SecurityContext
 * y la IP de origen del header X-Forwarded-For (K8s detrás de ALB de AWS).</p>
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Registra una entrada de auditoría en la tabla audit_log.
     *
     * <p>El DNI del usuario y la IP de origen se obtienen automáticamente
     * del contexto de seguridad y de la petición HTTP activa.</p>
     *
     * @param accion    Tipo de operación realizada (CREAR, LEER, ACTUALIZAR, ELIMINAR).
     * @param entidad   Nombre de la entidad afectada (ej: "Paciente", "Sanitario").
     * @param idEntidad Identificador del registro afectado (ej: DNI del paciente).
     * @param detalle   Descripción adicional de la operación.
     */
    public void registrar(AccionAuditoria accion, String entidad, String idEntidad, String detalle) {
        AuditLog log = new AuditLog();
        log.setFechaHora(LocalDateTime.now());
        log.setAccion(accion);
        log.setEntidad(entidad);
        log.setIdEntidad(idEntidad);
        log.setDetalle(detalle);

        // Obtener DNI del usuario autenticado desde el SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            log.setDniUsuario(auth.getName());
        }

        // Obtener IP de origen — en K8s detrás de ALB de AWS viene en X-Forwarded-For
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                log.setIpOrigen(
                        xForwardedFor != null
                                ? xForwardedFor.split(",")[0].trim()
                                : request.getRemoteAddr()
                );
            }
        } catch (Exception e) {
            // Contexto de request no disponible (ej: en tests unitarios o tareas programadas)
        }

        auditLogRepository.save(log);
    }
}
