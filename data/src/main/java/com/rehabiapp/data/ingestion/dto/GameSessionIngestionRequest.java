package com.rehabiapp.data.ingestion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.Instant;

/**
 * DTO de ingesta de sesion de juego. Solo aceptado desde el API Core interno.
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

        @Valid @NotNull
        MovementMetricsRequest movementMetrics,

        @NotNull
        Boolean completed

) {
    public record MovementMetricsRequest(
            @DecimalMin("0.0") Double rangeOfMotionDegrees,
            @DecimalMin("0.0") Double averageSpeed,
            @DecimalMin("0.0") Double maxSpeed
    ) {}
}
