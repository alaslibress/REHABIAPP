package com.rehabiapp.data.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro que protege los endpoints internos del pipeline /data.
 *
 * <p>Las rutas {@code /ingest/**} y {@code /internal/**} solo deben ser invocadas
 * por el API Core (servicio interno). Este filtro exige la cabecera
 * {@code X-Internal-Key} con el valor configurado en
 * {@code rehabiapp.api.internal-key} (compartido con /api).</p>
 *
 * <p>El servicio no usa Spring Security; este filtro standalone es la unica
 * proteccion de autorizacion. En produccion la clave se inyecta via variable
 * de entorno {@code RH_INTERNAL_KEY} desde el secret de Kubernetes (o env file
 * en AWS Academy). Si el valor configurado es {@code changeme}, el filtro
 * registra una advertencia en cada llamada para detectar entornos mal configurados.</p>
 */
@Component
public class InternalKeyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(InternalKeyFilter.class);
    private static final String HEADER_NAME = "X-Internal-Key";
    private static final List<String> PROTECTED_PATTERNS = List.of("/ingest/**", "/internal/**");

    private final AntPathMatcher matcher = new AntPathMatcher();
    private final String expectedKey;

    public InternalKeyFilter(@Value("${rehabiapp.api.internal-key:changeme}") String expectedKey) {
        this.expectedKey = expectedKey;
        if ("changeme".equals(expectedKey)) {
            log.warn("rehabiapp.api.internal-key tiene el valor por defecto 'changeme' — INSEGURO. "
                    + "Inyectar RH_INTERNAL_KEY como secreto antes de exponer el servicio.");
        }
    }

    /**
     * Decide si la peticion afecta a un endpoint protegido. Las rutas publicas
     * ({@code /actuator/**}, etc.) saltan el filtro mediante {@link #shouldNotFilter}.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return PROTECTED_PATTERNS.stream().noneMatch(p -> matcher.match(p, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String provided = request.getHeader(HEADER_NAME);
        if (provided == null || !expectedKey.equals(provided)) {
            log.warn("Acceso rechazado a {} — cabecera {} ausente o invalida (origen: {}).",
                    request.getRequestURI(), HEADER_NAME, request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"missing or invalid X-Internal-Key\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
