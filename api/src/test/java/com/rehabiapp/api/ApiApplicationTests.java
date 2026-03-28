package com.rehabiapp.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Prueba de arranque del contexto de Spring para la API de RehabiAPP.
 * Verifica que la configuración base es válida y el contexto se carga correctamente.
 */
@SpringBootTest
@ActiveProfiles("test")
class ApiApplicationTests {

    @Test
    void contextLoads() {
        // Verificación: el contexto de Spring arranca sin errores
    }
}
