package com.rehabiapp.data.presentation;

import com.rehabiapp.data.application.service.DuplicateSessionException;
import com.rehabiapp.data.application.service.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getAllErrors().stream()
                .map(e -> e.getDefaultMessage())
                .collect(Collectors.toList());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", 400,
                "error", "validation_failed",
                "details", errors
        ));
    }

    @ExceptionHandler(DuplicateSessionException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateSessionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", 409,
                "error", "duplicate_session",
                "sessionId", ex.getSessionId()
        ));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", 404,
                "error", "not_found",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArg(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", 400,
                "error", "validation_failed",
                "details", List.of(ex.getMessage())
        ));
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraint(
            jakarta.validation.ConstraintViolationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", 400,
                "error", "validation_failed",
                "details", ex.getConstraintViolations().stream()
                        .map(v -> v.getMessage()).toList()
        ));
    }
}
