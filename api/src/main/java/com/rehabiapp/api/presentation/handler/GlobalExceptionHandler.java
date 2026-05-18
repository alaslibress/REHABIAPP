package com.rehabiapp.api.presentation.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehabiapp.api.domain.exception.AccesoNoPermitidoException;
import com.rehabiapp.api.domain.exception.RecursoNoEncontradoException;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de excepciones — convierte excepciones a respuestas HTTP estándar.
 *
 * <p>Centraliza el manejo de errores para todos los controladores REST.
 * Nunca expone stack traces ni mensajes internos del sistema en producción.
 * Los mensajes de error son genéricos para evitar filtración de información.</p>
 *
 * <p>Jerarquía de manejo por prioridad:</p>
 * <ul>
 *   <li>404 Not Found — recurso no encontrado en base de datos.</li>
 *   <li>403 Forbidden — acceso denegado por rol o política de seguridad.</li>
 *   <li>400 Bad Request — validación de campos fallida (@Valid).</li>
 *   <li>409 Conflict — violación de restricción de integridad en base de datos.</li>
 *   <li>401 Unauthorized — token JWT inválido, expirado o malformado.</li>
 *   <li>500 Internal Server Error — cualquier excepción no controlada.</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Parser para extraer el body JSON de respuestas upstream (ProblemDetail RFC 7807).
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Maneja errores HTTP propagados desde servicios upstream (tipicamente `/data`).
     *
     * <p>Cuando el API actua como proxy de `/data` y este responde con 4xx
     * (p. ej. 422 Unprocessable Entity por payload invalido), preservamos el
     * status original y el detalle proporcionado por el upstream en lugar de
     * convertirlo en un 500 generico. El body de Spring sigue el formato
     * ProblemDetail RFC 7807 (`{title, status, detail, type}`).</p>
     *
     * @param ex Excepcion lanzada por {@code RestClient}/{@code RestTemplate} con status 4xx.
     * @return Respuesta con el status original y body simplificado.
     */
    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<Map<String, String>> manejarClientErrorUpstream(HttpClientErrorException ex) {
        // El upstream ya logueo el detalle; aqui basta con WARN para trazabilidad.
        log.warn("Upstream respondio {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
        return ResponseEntity.status(ex.getStatusCode())
                .body(extraerCuerpoUpstream(ex.getResponseBodyAsString(), "Error en peticion"));
    }

    /**
     * Maneja errores 5xx propagados desde servicios upstream — mismo patron que 4xx.
     * Se preserva el status del upstream para que el cliente distinga 502/503/504.
     *
     * @param ex Excepcion lanzada por {@code RestClient}/{@code RestTemplate} con status 5xx.
     * @return Respuesta con el status original y body simplificado.
     */
    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<Map<String, String>> manejarServerErrorUpstream(HttpServerErrorException ex) {
        log.warn("Upstream respondio {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
        return ResponseEntity.status(ex.getStatusCode())
                .body(extraerCuerpoUpstream(ex.getResponseBodyAsString(), "Error en servicio upstream"));
    }

    /**
     * Parsea el body JSON del upstream (ProblemDetail) y produce el shape estandar
     * del handler {@code {"error": ..., "detalle": ...}}. Si el body no es JSON
     * valido, se devuelve el texto crudo como detalle.
     */
    private Map<String, String> extraerCuerpoUpstream(String body, String defaultError) {
        if (body == null || body.isBlank()) {
            return Map.of("error", defaultError);
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(body);
            String title = root.path("title").asText(defaultError);
            String detail = root.path("detail").asText(null);
            if (detail == null || detail.isBlank()) {
                // Algunos servicios usan "message" o "error" como clave del detalle.
                detail = root.path("message").asText(root.path("error").asText(body));
            }
            return Map.of("error", title, "detalle", detail);
        } catch (Exception parseEx) {
            // Body no es JSON parseable: devolvemos el texto tal cual.
            return Map.of("error", defaultError, "detalle", body);
        }
    }

    /**
     * Maneja recursos no encontrados en base de datos.
     * Devuelve 404 con el mensaje descriptivo del recurso ausente.
     *
     * @param ex Excepción con el detalle del recurso no encontrado.
     * @return Respuesta 404 con cuerpo JSON estandarizado.
     */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> manejarNoEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "No encontrado", "detalle", ex.getMessage()));
    }

    /**
     * Maneja accesos denegados por restricciones de rol (RBAC).
     * Devuelve 403 sin exponer qué rol hubiera sido necesario.
     *
     * @param ex Excepción con el motivo del acceso denegado.
     * @return Respuesta 403 con cuerpo JSON estandarizado.
     */
    @ExceptionHandler(AccesoNoPermitidoException.class)
    public ResponseEntity<Map<String, String>> manejarAccesoDenegado(AccesoNoPermitidoException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Acceso denegado", "detalle", ex.getMessage()));
    }

    /**
     * Maneja accesos denegados de Spring Security (@PreAuthorize) — devuelve 403.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> manejarAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Acceso denegado",
                        "detalle", "El rol del usuario no permite esta operacion"));
    }

    /**
     * Maneja errores de validación de campos en el cuerpo de la petición (@Valid).
     * Devuelve 400 con un mapa de campo → mensaje de error para cada campo inválido.
     *
     * @param ex Excepción de validación con los errores por campo.
     * @return Respuesta 400 con mapa de campos inválidos y sus mensajes.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacion(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(e -> {
            String campo = ((FieldError) e).getField();
            campos.put(campo, e.getDefaultMessage());
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Validacion fallida", "campos", campos));
    }

    /**
     * Maneja violaciones de restricciones de integridad en base de datos.
     * Típicamente ocurre cuando se intenta crear un recurso con una clave duplicada.
     * El mensaje es genérico para no exponer la estructura de la base de datos.
     *
     * @param ex Excepción de violación de integridad de JPA/Hibernate.
     * @return Respuesta 409 con mensaje genérico de conflicto.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> manejarConflicto(DataIntegrityViolationException ex) {
        // Esta excepcion puede llegar por dos vias:
        //  a) Lanzada manualmente por un Service tras un chequeo previo
        //     (ex.getMessage() contiene un mensaje legible orientado al usuario).
        //  b) Propagada por JPA/Hibernate desde un INSERT/DELETE que rompe la BD
        //     (entonces ex.getMostSpecificCause() trae el mensaje del driver).
        // Damos prioridad al mensaje del service por ser mas claro para el usuario.
        String mensajeService = ex.getMessage();
        String causa = ex.getMostSpecificCause() != null
                ? String.valueOf(ex.getMostSpecificCause().getMessage()).toLowerCase()
                : "";
        boolean esFk = causa.contains("foreign key")
                || causa.contains("clave foránea")
                || causa.contains("clave foranea")
                || causa.contains("viola la restricción")
                || causa.contains("viola la restriccion")
                || causa.contains("violates foreign key");

        String detalle;
        String error;
        if (mensajeService != null && !mensajeService.isBlank()
                && !mensajeService.equals(causa)) {
            // El service ya formateo un mensaje pensado para el usuario final.
            detalle = mensajeService;
            error = "Recurso con dependencias";
        } else if (esFk) {
            detalle = "El recurso esta vinculado a otros datos. Reasigne o elimine primero las dependencias antes de borrarlo.";
            error = "Recurso con dependencias";
        } else {
            detalle = "El recurso ya existe o viola una restriccion de unicidad.";
            error = "Conflicto de datos";
        }

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", error, "detalle", detalle));
    }

    /**
     * Maneja tokens JWT inválidos, expirados o malformados.
     * Devuelve 401 con el motivo del rechazo del token.
     *
     * @param ex Excepción de JWT con el tipo de error del token.
     * @return Respuesta 401 con detalle del fallo del token.
     */
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<Map<String, String>> manejarJwt(JwtException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Token invalido", "detalle", ex.getMessage()));
    }

    /**
     * Manejador de último recurso para excepciones no controladas.
     * Devuelve 500 sin exponer el stack trace ni el mensaje interno.
     * El error real se registra en los logs del servidor para diagnóstico.
     *
     * @param ex Excepción no controlada capturada como último recurso.
     * @return Respuesta 500 con mensaje genérico sin detalles internos.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> manejarGenerico(Exception ex) {
        // Registrar stack trace completo en los logs del servidor para diagnostico
        log.error("Excepcion no controlada: {}", ex.getMessage(), ex);
        // No exponer stack trace ni mensaje interno en producción
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error interno del servidor"));
    }
}
