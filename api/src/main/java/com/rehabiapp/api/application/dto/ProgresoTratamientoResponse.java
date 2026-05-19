package com.rehabiapp.api.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Resumen de progreso de un paciente en un tratamiento concreto.
 *
 * <p>Devuelto por el pipeline `/data` y proxiado por el API.</p>
 */
@Schema(description = "Resumen de progreso de un paciente en un tratamiento")
public record ProgresoTratamientoResponse(
        String codTrat,
        String tratamientoNombre,
        String parteCuerpo,
        String metricaNombre,
        Double baselineValor,
        Instant baselineFecha,
        Double currentValor,
        Instant currentFecha,
        Double deltaPorcentaje,
        List<ProgresoEntradaResponse> entradas
) {}
