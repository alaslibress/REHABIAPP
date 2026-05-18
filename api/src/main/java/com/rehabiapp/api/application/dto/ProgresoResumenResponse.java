package com.rehabiapp.api.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Resumen plano de progreso del paciente — shape consumido por el BFF movil
 * (campo {@code myProgressSummary} en GraphQL).
 *
 * <p>El API proxia el calculo al pipeline {@code /data}; aqui se mantiene el
 * shape exacto que espera el frontend (PROGRESS_SUMMARY): totalSessions,
 * averageScore, improvementRate, lastSessionDate.</p>
 */
@Schema(description = "Resumen plano del progreso del paciente para la welcome card movil")
public record ProgresoResumenResponse(
        @Schema(description = "Numero total de sesiones registradas")
        long totalSessions,

        @Schema(description = "Media del score, null si no hay sesiones con score")
        Double averageScore,

        @Schema(description = "Variacion porcentual entre primera y ultima sesion")
        Double improvementRate,

        @Schema(description = "Fecha (UTC) de la sesion mas reciente")
        Instant lastSessionDate
) {}
