package com.rehabiapp.api.presentation.handler;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test unitario del manejador global de excepciones.
 *
 * <p>Centra la cobertura en la propagacion de errores upstream
 * ({@code HttpClientErrorException} / {@code HttpServerErrorException}) que
 * antes degeneraban en 500 generico (F1).</p>
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void clientErrorUpstreamPreservaStatusYExtraeProblemDetail() {
        String body = "{\"title\":\"Validation failed\",\"status\":422,"
                + "\"detail\":\"clave obligatoria ausente: fingerSpeed\","
                + "\"type\":\"about:blank\"}";
        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Unprocessable Entity",
                org.springframework.http.HttpHeaders.EMPTY,
                body.getBytes(),
                null);

        ResponseEntity<Map<String, String>> resp = handler.manejarClientErrorUpstream(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        Map<String, String> payload = resp.getBody();
        assertThat(payload).isNotNull();
        assertThat(payload.get("error")).isEqualTo("Validation failed");
        assertThat(payload.get("detalle")).isEqualTo("clave obligatoria ausente: fingerSpeed");
    }

    @Test
    void clientErrorUpstreamConBodyNoJsonDevuelveTextoCrudo() {
        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                org.springframework.http.HttpHeaders.EMPTY,
                "texto plano no JSON".getBytes(),
                null);

        ResponseEntity<Map<String, String>> resp = handler.manejarClientErrorUpstream(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().get("detalle")).isEqualTo("texto plano no JSON");
    }

    @Test
    void serverErrorUpstreamPreservaStatus() {
        HttpServerErrorException ex = HttpServerErrorException.create(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service Unavailable",
                org.springframework.http.HttpHeaders.EMPTY,
                new byte[0],
                null);

        ResponseEntity<Map<String, String>> resp = handler.manejarServerErrorUpstream(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().get("error")).isEqualTo("Error en servicio upstream");
    }
}
