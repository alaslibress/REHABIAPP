package com.rehabiapp.data.util;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Resuelve la metrica relevante (campo en MovementMetrics y etiqueta humana)
 * a partir del codigo del tratamiento. Configurable via application.yml en
 * el prefijo {@code rehabiapp.metric-map}.
 */
@Component
@ConfigurationProperties(prefix = "rehabiapp.metric-map")
public class TreatmentMetricResolver {

    // Mapping de prefijo de codTrat -> nombre del campo en MovementMetrics.
    // Se inicializa con valores por defecto y puede sobrescribirse desde YAML.
    private Map<String, String> prefixToField = new HashMap<>(Map.of(
            "ROM-", "rangeOfMotionDegrees",
            "VEL-", "averageSpeed",
            "FUERZA-", "maxSpeed"
    ));

    // Mapping de prefijo de codTrat -> etiqueta legible para el usuario final.
    private Map<String, String> prefixToLabel = new HashMap<>(Map.of(
            "ROM-", "Rango de movimiento (grados)",
            "VEL-", "Velocidad media (m/s)",
            "FUERZA-", "Velocidad maxima (m/s)"
    ));

    public String resolverCampo(String codTrat) {
        if (codTrat == null) {
            return "rangeOfMotionDegrees";
        }
        return prefixToField.entrySet().stream()
                .filter(e -> codTrat.startsWith(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("rangeOfMotionDegrees");
    }

    public String resolverLabel(String codTrat) {
        if (codTrat == null) {
            return "Metrica";
        }
        return prefixToLabel.entrySet().stream()
                .filter(e -> codTrat.startsWith(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("Metrica");
    }

    public Map<String, String> getPrefixToField() {
        return prefixToField;
    }

    public void setPrefixToField(Map<String, String> prefixToField) {
        this.prefixToField = prefixToField;
    }

    public Map<String, String> getPrefixToLabel() {
        return prefixToLabel;
    }

    public void setPrefixToLabel(Map<String, String> prefixToLabel) {
        this.prefixToLabel = prefixToLabel;
    }
}
