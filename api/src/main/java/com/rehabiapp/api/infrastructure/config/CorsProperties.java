package com.rehabiapp.api.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Propiedades CORS — origenes permitidos cargados desde configuracion externa.
 *
 * <p>La lista de origenes se externaliza para permitir distintos valores por perfil
 * (local, aws, aws-academy) sin recompilar. En produccion los origenes se inyectan
 * via variables de entorno o ConfigMap de Kubernetes.</p>
 *
 * @param allowedOrigins Lista de patrones de origen permitidos (p.ej. el bucket S3 del juego WebGL).
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        // Defensa: lista nula se sustituye por lista vacia para evitar NPE en SecurityConfig.
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }
}
