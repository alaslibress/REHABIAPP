package com.rehabiapp.api.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.Map;

/**
 * Payload de telemetria de una sesion de juego enviado por Unity (WebGL).
 *
 * <p>Si `disabilityId` es null el API lo infiere a partir de la asignacion
 * vigente del paciente.</p>
 */
@Schema(description = "Telemetria de sesion de juego enviada por Unity")
public record TelemetriaSesionRequest(
        @Schema(description = "DNI del paciente que jugo la sesion", example = "12345678Z")
        @NotBlank String dniPaciente,

        @Schema(description = "Codigo del videojuego que se ha jugado")
        @NotBlank String videojuegoCodigo,

        @Schema(description = "Codigo de la discapacidad asociada (opcional)")
        String disabilityId,

        @Schema(description = "Codigo del tratamiento al que pertenece la sesion (opcional)")
        String codTratamiento,

        @Schema(description = "Parte del cuerpo ejercitada (opcional)")
        String parteCuerpo,

        @Schema(description = "Marca de tiempo de inicio de la sesion")
        @NotNull Instant inicio,

        @Schema(description = "Marca de tiempo de fin de la sesion")
        @NotNull Instant fin,

        @Schema(description = "Duracion de la sesion en milisegundos")
        @Positive long duracionMs,

        @Schema(description = "Puntuacion obtenida")
        Integer puntuacion,

        @Schema(description = "Metricas adicionales (movimiento, precision, etc.)")
        Map<String, Object> metricas
) {}
