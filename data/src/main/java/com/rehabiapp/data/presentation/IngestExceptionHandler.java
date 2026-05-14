package com.rehabiapp.data.presentation;

import com.rehabiapp.data.ingestion.schema.InvalidMetricException;
import com.rehabiapp.data.ingestion.schema.UnknownGameException;
import com.rehabiapp.data.ingestion.service.DuplicateSessionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manejador de excepciones del pipeline de ingesta.
 *
 * Devuelve application/problem+json (RFC 7807) para facilitar la depuracion
 * en clientes Unity y en el API Core.
 */
@RestControllerAdvice
public class IngestExceptionHandler {

    // Bean Validation fallida — claves faltantes, rangos, formato
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> manejarValidacion(MethodArgumentNotValidException ex) {
        var problema = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problema.setType(URI.create("urn:rehabiapp:error:validacion"));
        problema.setTitle("Datos de ingesta invalidos");

        Map<String, String> errores = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        e -> e.getDefaultMessage() != null ? e.getDefaultMessage() : "invalido",
                        (a, b) -> a
                ));
        problema.setProperty("errores", errores);
        return ResponseEntity.badRequest().body(problema);
    }

    // Clave rawMetrics no registrada en el schema del gameId
    @ExceptionHandler(InvalidMetricException.class)
    public ResponseEntity<ProblemDetail> manejarMetricaInvalida(InvalidMetricException ex) {
        var problema = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        problema.setType(URI.create("urn:rehabiapp:error:metrica_invalida"));
        problema.setTitle("Metrica no cumple el schema del juego");
        problema.setDetail(ex.getMessage());
        return ResponseEntity.unprocessableEntity().body(problema);
    }

    // gameId sin schema registrado
    @ExceptionHandler(UnknownGameException.class)
    public ResponseEntity<ProblemDetail> manejarJuegoDesconocido(UnknownGameException ex) {
        var problema = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        problema.setType(URI.create("urn:rehabiapp:error:juego_desconocido"));
        problema.setTitle("gameId sin schema registrado");
        problema.setDetail(ex.getMessage());
        return ResponseEntity.unprocessableEntity().body(problema);
    }

    // Sesion duplicada — devolver 200 con doc existente para idempotencia en reintentos
    @ExceptionHandler(DuplicateSessionException.class)
    public ResponseEntity<Map<String, String>> manejarDuplicado(DuplicateSessionException ex) {
        return ResponseEntity.ok(Map.of(
                "status", "already_accepted",
                "message", ex.getMessage()
        ));
    }
}
