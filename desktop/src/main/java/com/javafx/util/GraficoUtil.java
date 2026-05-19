package com.javafx.util;

import com.javafx.Clases.ProgresoEntrada;
import com.javafx.Clases.ProgresoTratamiento;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tooltip;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** Factoria de graficos para la ventana de progreso de paciente. */
public final class GraficoUtil {

    private GraficoUtil() {}

    /**
     * Crea un LineChart con eje X = valor de la metrica (numerico) y eje Y = fecha (categoria).
     * El primer punto recibe la clase CSS "punto-baseline" y el ultimo "punto-actual".
     */
    public static LineChart<Number, String> crearLineChart(ProgresoTratamiento p) {
        NumberAxis ejeX = new NumberAxis();
        ejeX.setLabel(p.metricaNombre() != null ? p.metricaNombre() : "Valor");
        CategoryAxis ejeY = new CategoryAxis();
        ejeY.setLabel("Fecha");

        LineChart<Number, String> chart = new LineChart<>(ejeX, ejeY);
        chart.setTitle(p.tratamientoNombre() + " — " + p.parteCuerpo());
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.getStyleClass().add("grafico-progreso");

        XYChart.Series<Number, String> serie = new XYChart.Series<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        if (p.entradas() != null) {
            for (ProgresoEntrada e : p.entradas()) {
                if (e.fecha() != null && e.valor() != null) {
                    String fechaTxt = e.fecha().atZone(ZoneOffset.UTC).format(fmt);
                    serie.getData().add(new XYChart.Data<>(e.valor(), fechaTxt));
                }
            }
        }
        chart.getData().add(serie);

        // Aplicar estilos y tooltips una vez que los nodos esten en escena
        chart.setOnMouseEntered(ev -> aplicarEstilosPuntos(serie));
        // Tambien aplicar cuando la serie se renderice (puede ocurrir antes del hover)
        Platform_runLater(() -> aplicarEstilosPuntos(serie));

        return chart;
    }

    private static void aplicarEstilosPuntos(XYChart.Series<Number, String> serie) {
        if (serie.getData().isEmpty()) return;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 0; i < serie.getData().size(); i++) {
            XYChart.Data<Number, String> punto = serie.getData().get(i);
            if (punto.getNode() != null) {
                Tooltip.install(punto.getNode(),
                    new Tooltip(punto.getYValue() + ": " + punto.getXValue()));
                if (i == 0) punto.getNode().getStyleClass().add("punto-baseline");
                if (i == serie.getData().size() - 1) punto.getNode().getStyleClass().add("punto-actual");
            }
        }
    }

    // Helper para invocar en el hilo JavaFX sin importar Platform directamente en este util
    private static void Platform_runLater(Runnable r) {
        javafx.application.Platform.runLater(r);
    }
}
