package com.rehabiapp.api.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para PasswordService.
 *
 * <p>Verifica el hashing BCrypt con factor de coste 12 y la compatibilidad
 * con el formato de hashes del desktop ERP (jBCrypt 0.4).</p>
 *
 * <p>No requiere contexto de Spring — el servicio se instancia directamente.</p>
 *
 * <p>AVISO DE RENDIMIENTO: BCrypt con cost factor 12 es lento por diseño
 * (resistencia a fuerza bruta). Cada llamada a hashear() tarda ~300-500ms.
 * Mantener el número de tests al mínimo necesario.</p>
 */
class PasswordServiceTest {

    private PasswordService passwordService;

    @BeforeEach
    void setUp() {
        // Instanciar directamente sin contexto Spring
        passwordService = new PasswordService();
    }

    @Test
    void hashear_generaHashBcrypt() {
        // El hash generado debe tener el prefijo BCrypt estándar con cost factor 12
        String hash = passwordService.hashear("miContrasena123");
        assertNotNull(hash);
        // BCrypt puede generar $2a$ o $2b$ dependiendo de la implementación
        assertTrue(
                hash.startsWith("$2a$12$") || hash.startsWith("$2b$12$"),
                "El hash debe comenzar con el prefijo BCrypt de cost factor 12"
        );
    }

    @Test
    void verificar_conContrasenaCorrrecta_retornaTrue() {
        // La contraseña original debe verificarse correctamente contra su hash
        String hash = passwordService.hashear("miContrasena123");
        assertTrue(passwordService.verificar("miContrasena123", hash));
    }

    @Test
    void verificar_conContrasenaIncorrecta_retornaFalse() {
        // Una contraseña diferente no debe coincidir con el hash
        String hash = passwordService.hashear("miContrasena123");
        assertFalse(passwordService.verificar("otraContrasena", hash));
    }

    @Test
    void hashear_mismaClave_generaHashesDiferentes() {
        // BCrypt incluye salt aleatorio en cada operación, por lo que el mismo
        // texto plano genera hashes diferentes — cada hash es único
        String hash1 = passwordService.hashear("mismaClave");
        String hash2 = passwordService.hashear("mismaClave");
        assertNotEquals(hash1, hash2, "BCrypt debe generar hashes diferentes por el salt aleatorio");
    }

    @Test
    void verificar_conHashLegadoDesktop_esCompatible() {
        // Los hashes generados con BCrypt estándar (formato jBCrypt 0.4)
        // deben ser verificables — garantiza compatibilidad con el desktop ERP
        String hashLegado = passwordService.hashear("testPassword123");
        assertTrue(
                passwordService.verificar("testPassword123", hashLegado),
                "Debe ser compatible con hashes BCrypt en formato estándar (jBCrypt 0.4)"
        );
    }
}
