package com.rehabiapp.api.infrastructure.security;

import com.rehabiapp.api.domain.enums.Rol;
import com.rehabiapp.api.infrastructure.config.SecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Servicio de gestión de tokens JWT — stateless para escalado horizontal en K8s.
 *
 * <p>Genera y valida tokens firmados con HMAC-SHA usando la clave configurada
 * vía secreto Kubernetes. Los tokens son de corta duración (15 min) para
 * minimizar el impacto de una filtración (ENS Alto, RGPD Art. 32).</p>
 *
 * <p>Access token: contiene DNI y rol — caducidad 15 minutos.</p>
 * <p>Refresh token: contiene solo DNI — caducidad 7 días.</p>
 */
@Service
public class JwtService {

    private final SecretKey claveFirma;
    private final long expirationMs;
    private final long refreshExpirationMs;

    public JwtService(SecurityProperties props) {
        this.claveFirma = Keys.hmacShaKeyFor(props.jwtSigningKey().getBytes(StandardCharsets.UTF_8));
        this.expirationMs = props.jwtExpirationMs();
        this.refreshExpirationMs = props.jwtRefreshExpirationMs();
    }

    /**
     * Genera un access token JWT con el DNI y el rol del sanitario.
     * Caducidad: 15 minutos por política de seguridad del proyecto.
     *
     * @param dni DNI del sanitario autenticado.
     * @param rol Rol asignado (SPECIALIST o NURSE).
     * @return Token JWT firmado en formato compacto Base64url.
     */
    public String generarAccessToken(String dni, Rol rol) {
        return Jwts.builder()
                .subject(dni)
                .claim("rol", rol.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(claveFirma)
                .compact();
    }

    /**
     * Genera un refresh token JWT con solo el DNI del sanitario.
     * Caducidad: 7 días. No incluye rol para minimizar información expuesta.
     *
     * @param dni DNI del sanitario autenticado.
     * @return Refresh token JWT firmado en formato compacto Base64url.
     */
    public String generarRefreshToken(String dni) {
        return Jwts.builder()
                .subject(dni)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                .signWith(claveFirma)
                .compact();
    }

    /**
     * Valida la firma y la caducidad del token.
     * Lanza JwtException si el token es inválido, manipulado o ha expirado.
     *
     * @param token Token JWT en formato compacto.
     * @return Claims extraídos del payload del token.
     */
    public Claims validarToken(String token) {
        return Jwts.parser()
                .verifyWith(claveFirma)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extrae el DNI (subject) del token.
     *
     * @param token Token JWT válido.
     * @return DNI del sanitario.
     */
    public String extraerDni(String token) {
        return validarToken(token).getSubject();
    }

    /**
     * Extrae el rol del token. Devuelve null si el token no contiene claim "rol"
     * (por ejemplo, un refresh token).
     *
     * @param token Token JWT válido.
     * @return Rol del sanitario o null.
     */
    public Rol extraerRol(String token) {
        String rolStr = validarToken(token).get("rol", String.class);
        return rolStr != null ? Rol.valueOf(rolStr) : null;
    }
}
