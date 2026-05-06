package com.rehabiapp.data.analytics.dto;

import java.time.Instant;

/**
 * Resumen anonimizado de la ultima sesion del paciente.
 * Consumido por el DashboardService del API Core.
 */
public record LastSessionDto(
        String patientToken,
        String gameId,
        String codTrat,
        Instant sessionStart,
        Double score
) {}
