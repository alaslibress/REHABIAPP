package com.rehabiapp.data.application.service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Payload JSON recibido en POST /ingest/game-session.
 * Todas las validaciones son estructurales; reglas de negocio viven en el servicio.
 */
public record GameSessionIngestRequest(

        @NotBlank
        @Size(min = 8, max = 128)
        String sessionId,

        @NotBlank
        @Pattern(regexp = "^[0-9]{8}[A-HJ-NP-TV-Z]$", message = "DNI invalido")
        String patientDni,

        @NotBlank
        @Size(max = 64)
        String gameId,

        @NotNull
        @Min(1) @Max(4)
        Integer progressionLevel,

        @NotNull
        Instant sessionStart,

        @NotNull
        Instant sessionEnd,

        @NotNull @PositiveOrZero
        Integer durationSeconds,

        @NotNull @PositiveOrZero
        Integer score,

        @NotNull @PositiveOrZero
        Integer repetitionsCompleted,

        @NotNull @PositiveOrZero
        Integer repetitionsTarget,

        @NotNull @Valid
        MovementMetricsDto movementMetrics,

        @NotNull
        Boolean completed,

        @NotBlank
        @Size(max = 16)
        String disabilityCode
) {

    /** sessionEnd debe ser posterior o igual a sessionStart. */
    @AssertTrue(message = "sessionEnd debe ser >= sessionStart")
    public boolean isTemporalOrderValid() {
        return sessionStart != null && sessionEnd != null && !sessionEnd.isBefore(sessionStart);
    }

    /** repetitionsCompleted no puede superar el objetivo. */
    @AssertTrue(message = "repetitionsCompleted no puede ser > repetitionsTarget")
    public boolean isRepetitionCountValid() {
        return repetitionsCompleted != null && repetitionsTarget != null
                && repetitionsCompleted <= repetitionsTarget;
    }
}
