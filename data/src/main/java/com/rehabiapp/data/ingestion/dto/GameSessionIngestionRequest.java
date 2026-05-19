package com.rehabiapp.data.ingestion.dto;

import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.Map;

/**
 * DTO de ingesta de sesion de juego. Solo aceptado desde el API Core interno.
 *
 * rawMetrics reemplaza el antiguo MovementMetricsRequest fijo.
 * movementMetrics se mantiene como campo deprecated para compatibilidad
 * con clientes Unity que aun no envian rawMetrics — se eliminara en la
 * proxima iteracion tras migrar todos los builds.
 */
public record GameSessionIngestionRequest(

        @NotBlank
        String patientDni,

        @NotBlank
        String gameId,

        String disabilityId,

        @NotNull @Min(1) @Max(4)
        Integer progressionLevel,

        @NotNull
        Instant sessionStart,

        @NotNull
        Instant sessionEnd,

        @Positive
        Long durationSeconds,

        @DecimalMin("0.0")
        Double score,

        @Min(0)
        Integer repetitionsCompleted,

        @Positive
        Integer repetitionsTarget,

        // Metricas libres por juego — reemplaza MovementMetricsRequest
        @NotNull @NotEmpty
        Map<String, Object> rawMetrics,

        // Version del schema enviada por el cliente Unity (ej. "v1")
        @NotBlank
        String schemaVersion,

        @NotNull
        Boolean completed,

        // Enriquecimiento opcional desde la API Core: tratamiento del paciente
        String codTrat,

        // Parte del cuerpo trabajada en la sesion (opcional)
        String parteCuerpo,

        // Nombre legible del tratamiento (opcional)
        String tratamientoNombre,

        // Shim de compatibilidad con clientes Unity previos a rawMetrics
        @Deprecated
        MovementMetricsRequest movementMetrics

) {
    /** Compatibilidad con el payload antiguo de Unity. */
    @Deprecated
    public record MovementMetricsRequest(
            @DecimalMin("0.0") Double rangeOfMotionDegrees,
            @DecimalMin("0.0") Double averageSpeed,
            @DecimalMin("0.0") Double maxSpeed
    ) {}
}
