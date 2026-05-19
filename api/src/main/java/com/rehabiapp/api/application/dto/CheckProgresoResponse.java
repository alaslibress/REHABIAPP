package com.rehabiapp.api.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Resultado de la consulta de novedades de progreso para un paciente.
 */
@Schema(description = "Indicador de novedades de progreso para un paciente")
public record CheckProgresoResponse(
        @Schema(description = "True si hay sesiones de juego nuevas posteriores a la marca de tiempo")
        boolean hasNewData,
        @Schema(description = "Marca de tiempo de la ultima sesion conocida")
        Instant lastSessionAt,
        @Schema(description = "Numero de sesiones nuevas")
        int count
) {}
