package com.rehabiapp.data.analytics.dto;

import java.time.Instant;
import java.util.List;

/**
 * Representa el progreso terapeutico de un paciente para un tratamiento concreto
 * sobre una parte del cuerpo. Incluye linea base, valor actual, delta porcentual y
 * la serie diaria de promedios para visualizacion.
 *
 * El campo {@code metricaNombre} se rellena en post-proceso a traves del resolver
 * porque depende del mapeo configurable en application.yml.
 */
public record TreatmentProgressDto(
        String patientToken,
        String codTrat,
        String tratamientoNombre,
        String parteCuerpo,
        String metricaNombre,
        BaselineCurrent baseline,
        BaselineCurrent current,
        Double deltaPorcentaje,
        List<EntradaDto> entradas
) {

    public record BaselineCurrent(Instant fecha, Double valor) {}

    public record EntradaDto(Instant fecha, Double valor) {}

    /**
     * Devuelve una copia del DTO con la etiqueta de metrica resuelta.
     */
    public TreatmentProgressDto withMetricaNombre(String label) {
        return new TreatmentProgressDto(
                patientToken,
                codTrat,
                tratamientoNombre,
                parteCuerpo,
                label,
                baseline,
                current,
                deltaPorcentaje,
                entradas
        );
    }
}
