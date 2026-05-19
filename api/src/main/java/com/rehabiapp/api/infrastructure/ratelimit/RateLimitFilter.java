package com.rehabiapp.api.infrastructure.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import com.rehabiapp.api.infrastructure.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filtro de rate limiting basado en Bucket4j.
 *
 * <p>Limites:</p>
 * <ul>
 *   <li>{@code /api/auth/login}: 10 peticiones/minuto por IP.</li>
 *   <li>{@code /api/telemetria/**}: 60 peticiones/minuto por subject JWT.</li>
 *   <li>Resto: 300 peticiones/minuto por subject JWT.</li>
 * </ul>
 *
 * <p>Cuando se agota el bucket se devuelve HTTP 429 con la cabecera
 * {@code Retry-After} indicando los segundos hasta la siguiente recarga.</p>
 */
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String TELEMETRIA_PREFIX = "/api/telemetria";

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final JwtService jwtService;

    public RateLimitFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = resolverPath(request);
        String key;
        Bandwidth limit;

        if (LOGIN_PATH.equals(path)) {
            key = "login:" + clientIp(request);
            limit = Bandwidth.builder()
                    .capacity(10).refillIntervally(10, Duration.ofMinutes(1)).build();
        } else if (path.startsWith(TELEMETRIA_PREFIX)) {
            key = "telemetria:" + subject(request);
            limit = Bandwidth.builder()
                    .capacity(60).refillIntervally(60, Duration.ofMinutes(1)).build();
        } else {
            key = "general:" + subject(request);
            limit = Bandwidth.builder()
                    .capacity(300).refillIntervally(300, Duration.ofMinutes(1)).build();
        }

        Bucket bucket = buckets.computeIfAbsent(key,
                k -> Bucket.builder().addLimit(limit).build());

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
            return;
        }

        long waitNanos = bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill();
        long retryAfterSec = Math.max(1, waitNanos / 1_000_000_000L);
        response.setStatus(429);
        response.setHeader("Retry-After", Long.toString(retryAfterSec));
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"error\":\"Demasiadas peticiones\",\"retryAfterSeconds\":"
                        + retryAfterSec + "}");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String p = resolverPath(request);
        return p.startsWith("/actuator")
                || p.startsWith("/v3/api-docs")
                || p.startsWith("/swagger-ui");
    }

    /** Resuelve la ruta de la peticion compatible con MockMvc y servlet real. */
    private String resolverPath(HttpServletRequest request) {
        String p = request.getServletPath();
        if (p == null || p.isEmpty()) {
            p = request.getRequestURI();
            String ctx = request.getContextPath();
            if (ctx != null && !ctx.isEmpty() && p.startsWith(ctx)) {
                p = p.substring(ctx.length());
            }
        }
        return p != null ? p : "";
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String subject(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims c = jwtService.validarToken(header.substring(7));
                if (c.getSubject() != null) {
                    return "jwt:" + c.getSubject();
                }
            } catch (JwtException ignored) {
                // Token invalido — caemos a IP
            }
        }
        return "ip:" + clientIp(request);
    }
}
