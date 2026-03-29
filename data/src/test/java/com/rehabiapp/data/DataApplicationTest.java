package com.rehabiapp.data;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Test de arranque del contexto de Spring Boot.
 *
 * Verifica que la configuracion de beans, controladores y componentes
 * se carga correctamente sin errores al iniciar la aplicacion.
 *
 * MongoDB se excluye de la autoconfiguracion mediante el perfil "test"
 * definido en src/test/resources/application-test.yml para evitar
 * dependencia de una instancia MongoDB real durante los tests.
 */
@SpringBootTest
@ActiveProfiles("test")
class DataApplicationTest {

    @Test
    void contextLoads() {
        // Verifica que el contexto de Spring arranca sin errores
    }
}
