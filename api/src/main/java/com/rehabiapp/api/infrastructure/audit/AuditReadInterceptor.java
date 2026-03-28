package com.rehabiapp.api.infrastructure.audit;

import com.rehabiapp.api.domain.enums.AccionAuditoria;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor de auditoría de lecturas — registra en audit_log los accesos GET
 * a los endpoints de pacientes para cumplir con la Ley 41/2002.
 *
 * <p>La Ley 41/2002 (Art. 15 y 17) exige el registro de todos los accesos a historiales
 * clínicos, no solo las modificaciones. Hibernate Envers solo audita escrituras (INSERT,
 * UPDATE, DELETE). Este interceptor complementa Envers para los accesos de lectura.</p>
 *
 * <p>Solo se auditan peticiones GET a /api/pacientes para evitar registros duplicados
 * con los ya generados por los servicios en las operaciones de escritura.</p>
 *
 * <p>El AuditService extrae automáticamente el DNI del usuario autenticado
 * desde el SecurityContext y la IP de origen del header X-Forwarded-For.</p>
 */
@Component
public class AuditReadInterceptor implements HandlerInterceptor {

    private final AuditService auditService;

    public AuditReadInterceptor(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Registra en audit_log las lecturas de datos clínicos de pacientes.
     * Se ejecuta antes del handler del controlador para garantizar el registro
     * incluso si el controlador lanza una excepción.
     *
     * @param request  Petición HTTP entrante.
     * @param response Respuesta HTTP saliente.
     * @param handler  Handler que procesará la petición.
     * @return true siempre — el interceptor no bloquea peticiones, solo registra.
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Solo auditar GET a endpoints de datos clínicos de pacientes (Ley 41/2002)
        if ("GET".equals(request.getMethod()) && request.getRequestURI().startsWith("/api/pacientes")) {
            auditService.registrar(
                    AccionAuditoria.LEER,
                    "paciente",
                    extraerIdDesdePath(request.getRequestURI()),
                    "Lectura via HTTP: " + request.getRequestURI()
            );
        }
        // Siempre continuar — el interceptor no bloquea peticiones
        return true;
    }

    /**
     * Extrae el DNI del paciente del path de la URI cuando está disponible.
     * Para listados (/api/pacientes) devuelve "lista" como identificador genérico.
     *
     * @param uri URI de la petición HTTP.
     * @return DNI del paciente si está en el path, "lista" si es un listado paginado.
     */
    private String extraerIdDesdePath(String uri) {
        // Formato esperado: /api/pacientes/{dni} o /api/pacientes/{dni}/...
        String[] partes = uri.split("/");
        // partes[0]="" partes[1]="api" partes[2]="pacientes" partes[3]="{dni}"
        return partes.length > 3 ? partes[3] : "lista";
    }
}
