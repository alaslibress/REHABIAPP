package com.rehabiapp.data.ingestion.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Especificacion de un campo en el schema de metricas de un juego.
 *
 * type: "number" | "integer" | "string" | "boolean"
 * min / max: solo aplica para tipos numericos.
 * unit: descripcion de la unidad (informativo, aparece en el Markdown).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FieldSpec(
        String type,
        Double min,
        Double max,
        String unit
) {
    /**
     * Valida el valor contra type y rango.
     * Lanza InvalidMetricException si no cumple.
     */
    public void validate(String key, Object valor) {
        if (valor == null) {
            throw new InvalidMetricException("valor nulo en clave: " + key);
        }
        switch (type) {
            case "number" -> validarNumero(key, valor);
            case "integer" -> validarEntero(key, valor);
            case "boolean" -> {
                if (!(valor instanceof Boolean)) {
                    throw new InvalidMetricException("se esperaba boolean en clave: " + key
                            + ", recibido: " + valor.getClass().getSimpleName());
                }
            }
            // "string" acepta cualquier valor de texto
        }
    }

    private void validarNumero(String key, Object valor) {
        if (!(valor instanceof Number n)) {
            throw new InvalidMetricException("se esperaba numero en clave: " + key
                    + ", recibido: " + valor.getClass().getSimpleName());
        }
        double d = n.doubleValue();
        if (min != null && d < min) {
            throw new InvalidMetricException("valor " + d + " en clave '" + key
                    + "' es menor que el minimo permitido " + min);
        }
        if (max != null && d > max) {
            throw new InvalidMetricException("valor " + d + " en clave '" + key
                    + "' supera el maximo permitido " + max);
        }
    }

    private void validarEntero(String key, Object valor) {
        if (!(valor instanceof Number)) {
            throw new InvalidMetricException("se esperaba entero en clave: " + key
                    + ", recibido: " + valor.getClass().getSimpleName());
        }
        double d = ((Number) valor).doubleValue();
        if (d != Math.floor(d)) {
            throw new InvalidMetricException("se esperaba entero sin decimales en clave: " + key
                    + ", recibido: " + valor);
        }
        if (min != null && d < min) {
            throw new InvalidMetricException("valor " + (long) d + " en clave '" + key
                    + "' es menor que el minimo permitido " + min.longValue());
        }
        if (max != null && d > max) {
            throw new InvalidMetricException("valor " + (long) d + " en clave '" + key
                    + "' supera el maximo permitido " + max.longValue());
        }
    }
}
