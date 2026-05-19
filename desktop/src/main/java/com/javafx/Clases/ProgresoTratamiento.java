package com.javafx.Clases;

import java.time.Instant;
import java.util.List;

/** Resumen de progreso de un paciente en un tratamiento (espejo de ProgresoTratamientoResponse del API). */
public record ProgresoTratamiento(
    String codTrat,
    String tratamientoNombre,
    String parteCuerpo,
    String metricaNombre,
    Double baselineValor,
    Instant baselineFecha,
    Double currentValor,
    Instant currentFecha,
    Double deltaPorcentaje,
    List<ProgresoEntrada> entradas
) {}
