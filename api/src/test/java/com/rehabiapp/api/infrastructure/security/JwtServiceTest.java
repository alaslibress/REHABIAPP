package com.rehabiapp.api.infrastructure.security;

import com.rehabiapp.api.domain.enums.Rol;
import com.rehabiapp.api.infrastructure.config.SecurityProperties;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para JwtService.
 *
 * <p>Verifica la generación, extracción y validación de tokens JWT
 * sin necesidad de contexto de Spring ni base de datos.</p>
 */
@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    // Clave con 64+ caracteres para HMAC-SHA512 (requisito de jjwt 0.12.x)
    private static final String CLAVE_JWT_PRUEBA =
            "clave-de-prueba-para-jwt-muy-larga-debe-tener-64-caracteres-minimo!";
    private static final String CLAVE_AES_PRUEBA = "clave-aes-prueba-32-caracteres!!";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        // Instanciar JwtService directamente con propiedades de prueba
        SecurityProperties props = new SecurityProperties(
                CLAVE_JWT_PRUEBA,
                900000L,
                604800000L,
                CLAVE_AES_PRUEBA
        );
        jwtService = new JwtService(props);
    }

    @Test
    void generarAccessToken_retornaTokenValido() {
        // Verificar que el token generado no es nulo ni vacío
        String token = jwtService.generarAccessToken("12345678A", Rol.SPECIALIST);
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void extraerDni_devuelveElDniCorrecto() {
        // El subject del token debe coincidir con el DNI proporcionado
        String token = jwtService.generarAccessToken("12345678A", Rol.SPECIALIST);
        assertEquals("12345678A", jwtService.extraerDni(token));
    }

    @Test
    void extraerRol_devuelveElRolCorrecto() {
        // El claim 'rol' debe coincidir con el rol proporcionado al generar el token
        String token = jwtService.generarAccessToken("12345678A", Rol.NURSE);
        assertEquals(Rol.NURSE, jwtService.extraerRol(token));
    }

    @Test
    void generarRefreshToken_noContieneRol() {
        // El refresh token no incluye claim 'rol' para minimizar información expuesta
        String token = jwtService.generarRefreshToken("12345678A");
        assertNull(jwtService.extraerRol(token));
    }

    @Test
    void validarToken_conTokenExpirado_lanzaExcepcion() throws InterruptedException {
        // Crear servicio con expiración de 1ms para forzar la caducidad del token
        SecurityProperties propsExpiracionCorta = new SecurityProperties(
                CLAVE_JWT_PRUEBA,
                1L,
                1L,
                CLAVE_AES_PRUEBA
        );
        JwtService servicioConExpiracionCorta = new JwtService(propsExpiracionCorta);

        String token = servicioConExpiracionCorta.generarAccessToken("12345678A", Rol.SPECIALIST);

        // Esperar a que el token expire (10ms es suficiente margen sobre 1ms)
        Thread.sleep(10);

        assertThrows(JwtException.class, () -> servicioConExpiracionCorta.validarToken(token));
    }

    @Test
    void validarToken_conTokenFirmadoConOtraClave_lanzaExcepcion() {
        // Un token firmado con otra clave debe ser rechazado por el servicio actual
        SecurityProperties otrasCreds = new SecurityProperties(
                "otra-clave-completamente-diferente-para-jwt-64-caracteres-exactos!!",
                900000L,
                604800000L,
                "otra-clave-aes-32-caracteres!!"
        );
        JwtService otroServicio = new JwtService(otrasCreds);
        String tokenAjeno = otroServicio.generarAccessToken("12345678A", Rol.SPECIALIST);

        assertThrows(JwtException.class, () -> jwtService.validarToken(tokenAjeno));
    }
}
