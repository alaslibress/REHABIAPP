package com.rehabiapp.data.presentation;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test unitario para HealthController.
 *
 * Instancia el controlador directamente sin contexto de Spring ni MockMvc
 * para mantener el test rapido y sin dependencias de infraestructura.
 *
 * Spring Boot 4.0.0 ha eliminado los test slices de web (WebMvcTest),
 * por lo que se utiliza instanciacion directa del controlador.
 */
class HealthControllerTest {

    private final HealthController healthController = new HealthController();

    @Test
    void healthEndpointReturns200() {
        // Verifica que GET /health devuelve 200 OK con cuerpo "OK"
        ResponseEntity<String> response = healthController.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("OK");
    }
}
