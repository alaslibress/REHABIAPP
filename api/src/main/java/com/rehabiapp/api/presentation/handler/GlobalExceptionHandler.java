package com.rehabiapp.api.presentation.handler;

import com.rehabiapp.api.domain.exception.AccesoNoPermitidoException;
import com.rehabiapp.api.domain.exception.RecursoNoEncontradoException;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Conflicto de datos",
                        "detalle", "El recurso ya existe o viola una restriccion de integridad"));
    }

    /**
     * Maneja argumentos invalidos de negocio (ej. articulacion incompatible al asociar juego).
     * Devuelve 400 con el mensaje descriptivo.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> manejarArgumentoInvalido(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Argumento invalido", "detalle", ex.getMessage()));
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
