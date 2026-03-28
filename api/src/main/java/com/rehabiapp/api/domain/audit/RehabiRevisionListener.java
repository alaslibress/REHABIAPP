package com.rehabiapp.api.domain.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Listener de revisión Envers personalizado para RehabiAPP.
 *
 * <p>Se invoca automáticamente cada vez que Envers crea una nueva revisión.
 * Extrae el DNI del usuario autenticado desde el SecurityContext de Spring Security
 * y la IP de origen desde el HttpServletRequest, considerando la presencia de
 * proxies inversos (K8s + AWS ALB) a través del header X-Forwarded-For.</p>
 *
 * <p>Cumplimiento: ENS Alto (trazabilidad de operaciones), RGPD Art. 30.</p>
 */
public class RehabiRevisionListener implements RevisionListener {

    /**
     * Header estándar que los proxies y balanceadores (AWS ALB, Nginx) utilizan
     * para pasar la IP real del cliente en entornos Kubernetes.
     */
    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";

    /**
     * Rellena los campos personalizados de la revisión: usuario e IP de origen.
     *
     * @param revisionEntity la entidad de revisión recién creada por Envers
     */
    @Override
    public void newRevision(Object revisionEntity) {
        RehabiRevisionEntity revision = (RehabiRevisionEntity) revisionEntity;

        // Extraer el DNI del usuario autenticado desde el SecurityContext
        String dniUsuario = extraerDniUsuario();
        revision.setUsuario(dniUsuario);

        // Extraer la IP de origen, considerando proxies K8s/ALB
        String ipOrigen = extraerIpOrigen();
        revision.setIpOrigen(ipOrigen);
    }

    /**
     * Extrae el nombre de usuario (DNI del sanitario) del SecurityContext.
     * Si no hay sesión autenticada, devuelve "sistema" para operaciones internas.
     *
     * @return DNI del sanitario autenticado o "sistema"
     */
    private String extraerDniUsuario() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion != null && autenticacion.isAuthenticated()
                && autenticacion.getPrincipal() != null
                && !"anonymousUser".equals(autenticacion.getPrincipal())) {
            return autenticacion.getName();
        }
        return "sistema";
    }

    /**
     * Extrae la IP de origen de la petición HTTP actual.
     *
     * <p>En producción (Kubernetes + AWS ALB), la IP real del cliente viene en el
     * header X-Forwarded-For, que puede contener múltiples IPs separadas por coma
     * (proxy chain). Se toma la primera (más cercana al cliente original).</p>
     *
     * <p>Si no hay petición HTTP activa (operaciones batch/internas), devuelve null.</p>
     *
     * @return IP de origen o null si no hay contexto HTTP
     */
    private String extraerIpOrigen() {
        ServletRequestAttributes atributos =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (atributos == null) {
            return null;
        }

        HttpServletRequest request = atributos.getRequest();

        // Comprobar primero el header X-Forwarded-For (K8s detrás de AWS ALB)
        String ipForwarded = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (ipForwarded != null && !ipForwarded.isBlank()) {
            // Puede venir como "ip1, ip2, ip3" — tomar la primera (cliente real)
            return ipForwarded.split(",")[0].trim();
        }

        // Si no hay proxy, usar la IP directa de la conexión
        return request.getRemoteAddr();
    }
}
