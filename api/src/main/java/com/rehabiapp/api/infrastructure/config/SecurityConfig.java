package com.rehabiapp.api.infrastructure.config;

import com.rehabiapp.api.infrastructure.filter.PayloadSizeFilter;
import com.rehabiapp.api.infrastructure.ratelimit.RateLimitFilter;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración de seguridad de la API REST — stateless con JWT para K8s.
 *
 * <p>Diseño stateless: sin sesiones de servidor, sin cookies de sesión.
 * Escalado horizontal sin afinidad de sesión requerida.</p>
 *
 * <p>CSRF desactivado: las APIs REST stateless con JWT son inmunes a CSRF
 * por diseño (el token no se envía automáticamente por el navegador).</p>
 *
 * <p>CORS habilitado mediante {@link CorsProperties}: los origenes permitidos
 * se inyectan por configuracion externa para permitir que las builds Unity WebGL
 * alojadas en AWS S3/CloudFront hagan POST a esta API desde otro origen.</p>
 *
 * <p>@EnableMethodSecurity permite usar @PreAuthorize y @PostAuthorize
 * en controladores y servicios para control de acceso granular por rol.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final RateLimitFilter rateLimitFilter;
    private final PayloadSizeFilter payloadSizeFilter;
    private final CorsProperties corsProperties;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter,
                          RateLimitFilter rateLimitFilter,
                          PayloadSizeFilter payloadSizeFilter,
                          CorsProperties corsProperties) {
        this.jwtFilter = jwtFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.payloadSizeFilter = payloadSizeFilter;
        this.corsProperties = corsProperties;
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
                // CORS aplicado antes de cualquier otro filtro — necesario para preflights OPTIONS
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                // API stateless — CSRF no aplica con JWT
                .csrf(AbstractHttpConfigurer::disable)
                // Sin sesiones de servidor — escalado horizontal K8s sin afinidad
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Rutas de autenticación — públicas (login, refresh)
                        .requestMatchers("/api/auth/**").permitAll()
                        // Endpoints internos entre servicios — protegidos por X-Internal-Key, no por JWT
                        .requestMatchers("/api/internal/**").permitAll()
                        // Probes de K8s — accesibles sin autenticación desde el plano de control
                        .requestMatchers("/actuator/health/**").permitAll()
                        // Endpoint de métricas Prometheus — accesible desde el stack de observabilidad
                        .requestMatchers("/actuator/prometheus").permitAll()
                        // Documentacion OpenAPI / Swagger UI — publica para desarrolladores
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Todo lo demás requiere JWT válido
                        .anyRequest().authenticated()
                )
                // Cadena de filtros: rate limit → payload size → JWT → autenticacion
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(payloadSizeFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configuracion CORS — origenes inyectados desde {@link CorsProperties}.
     *
     * <p>Se usa {@code setAllowedOriginPatterns} en lugar de {@code setAllowedOrigins}
     * porque {@code allowCredentials=true} prohibe el wildcard "*" en origenes literales.
     * Los patrones soportan comodines y son la forma correcta cuando se necesita
     * compatibilidad con tokens en cabeceras Authorization.</p>
     *
     * @return Fuente de configuracion CORS basada en URL para los endpoints {@code /api/**} y {@code /actuator/**}.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(corsProperties.allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        source.registerCorsConfiguration("/actuator/**", config);
        return source;
    }
}
