package com.rehabiapp.api.infrastructure.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro JWT — extrae y valida el token Bearer en cada petición HTTP.
 *
 * <p>Si el token es válido, establece la autenticación en el SecurityContext
 * para que Spring Security aplique las reglas de autorización RBAC.</p>
 *
 * <p>Si el token es inválido o no está presente, el filtro continúa la cadena
 * sin autenticación — Spring Security devolverá 401 en rutas protegidas.</p>
 *
 * <p>Excluye los endpoints de autenticación y los probes de K8s (/actuator)
 * para que no requieran token.</p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            // No hay token — continuar sin autenticación
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        try {
            var claims = jwtService.validarToken(token);
            String dni = claims.getSubject();
            String rol = claims.get("rol", String.class);

            // Construir autenticación con el rol como autoridad Spring Security (ROLE_SPECIALIST, ROLE_NURSE)
            var auth = new UsernamePasswordAuthenticationToken(
                    dni,
                    null,
                    rol != null ? List.of(new SimpleGrantedAuthority("ROLE_" + rol)) : List.of()
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (JwtException e) {
            // Token inválido, manipulado o expirado — continuar sin autenticación
            // Spring Security devolverá 401 en rutas protegidas
        }

        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // Excluir probes K8s y endpoints de autenticación del filtro JWT
        return path.startsWith("/actuator") || path.startsWith("/api/auth");
    }
}
