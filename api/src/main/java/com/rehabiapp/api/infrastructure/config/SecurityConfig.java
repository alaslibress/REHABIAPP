package com.rehabiapp.api.infrastructure.config;

import com.rehabiapp.api.infrastructure.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración de seguridad de la API REST — stateless con JWT para K8s.
 *
 * <p>Diseño stateless: sin sesiones de servidor, sin cookies de sesión.
 * Escalado horizontal sin afinidad de sesión requerida.</p>
 *
 * <p>CSRF desactivado: las APIs REST stateless con JWT son inmunes a CSRF
 * por diseño (el token no se envía automáticamente por el navegador).</p>
 *
 * <p>@EnableMethodSecurity permite usar @PreAuthorize y @PostAuthorize
 * en controladores y servicios para control de acceso granular por rol.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    /**
     * Cadena de filtros de seguridad principal.
     *
     * <p>Rutas públicas: endpoints de autenticación y probes de Kubernetes.
     * Todo lo demás requiere JWT válido en el header Authorization: Bearer.</p>
     *
     * @param http Constructor de seguridad HTTP de Spring.
     * @return Cadena de filtros configurada.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // API stateless — CSRF no aplica con JWT
                .csrf(AbstractHttpConfigurer::disable)
                // Sin sesiones de servidor — escalado horizontal K8s sin afinidad
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Rutas de autenticación — públicas (login, refresh)
                        .requestMatchers("/api/auth/**").permitAll()
                        // Probes de K8s — accesibles sin autenticación desde el plano de control
                        .requestMatchers("/actuator/health/**").permitAll()
                        // Endpoint de métricas Prometheus — accesible desde el stack de observabilidad
                        .requestMatchers("/actuator/prometheus").permitAll()
                        // Todo lo demás requiere JWT válido
                        .anyRequest().authenticated()
                )
                // Añadir filtro JWT antes del filtro de autenticación por usuario/contraseña
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
