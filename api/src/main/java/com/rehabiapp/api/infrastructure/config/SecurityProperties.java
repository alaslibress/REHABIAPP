package com.rehabiapp.api.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de seguridad inyectadas desde secretos Kubernetes vía variables de entorno.
 *
 * <p>Las claves sensibles (jwtSigningKey, encryptionKey) NUNCA se hardcodean en el
 * código fuente ni en ficheros de configuración del repositorio.
 * En producción se inyectan mediante Kubernetes Secrets montados como variables de entorno.</p>
 *
 * <p>Cumple con ENS Alto: gestión centralizada de secretos criptográficos.</p>
 *
 * @param jwtSigningKey          Clave HMAC-SHA de mínimo 32 bytes para firma de tokens JWT.
 * @param jwtExpirationMs        Tiempo de vida del access token en milisegundos (defecto: 15 min).
 * @param jwtRefreshExpirationMs Tiempo de vida del refresh token en milisegundos (defecto: 7 días).
 * @param encryptionKey          Clave AES-256-GCM de 32 bytes para cifrado de campos clínicos.
 */
@ConfigurationProperties(prefix = "rehabiapp.security")
public record SecurityProperties(
        String jwtSigningKey,
        long jwtExpirationMs,
        long jwtRefreshExpirationMs,
        String encryptionKey
) {}
