package com.rehabiapp.data.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Verifica la cabecera X-Internal-Key en las rutas de ingesta.
 * Rutas /health, /actuator/** y cualquier otra quedan fuera del filtro.
 */
@Component
public class InternalAuthFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Internal-Key";

    private final String expectedKey;

    public InternalAuthFilter(@Value("${rehabiapp.internal.shared-key:}") String expectedKey) {
        this.expectedKey = expectedKey;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String p = request.getRequestURI();
        return !(p.startsWith("/ingest") || p.startsWith("/internal") || p.startsWith("/analytics"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        if (expectedKey == null || expectedKey.isBlank()
                || provided == null || !constantTimeEquals(expectedKey, provided)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"unauthorized\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) diff |= a.charAt(i) ^ b.charAt(i);
        return diff == 0;
    }
}
