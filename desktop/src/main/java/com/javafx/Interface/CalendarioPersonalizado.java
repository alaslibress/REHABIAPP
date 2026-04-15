package com.javafx.Interface;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Componente de calendario mensual personalizado basado en GridPane.
 * Reemplaza CalendarFX MonthView sin dependencias externas.
 * Estructura: 7 columnas (L-D) x 6 filas de celdas con indicadores de citas por dia.
 */
public class CalendarioPersonalizado extends VBox {

    // ========== CONSTANTES ==========
    private static final String[] DIAS_SEMANA = {"L", "M", "X", "J", "V", "S", "D"};
    private static final DateTimeFormatter FORMATO_MES =
            DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("es", "ES"));

    // ========== ESTADO INTERNO ==========
    private YearMonth mesActual;
    private LocalDate fechaSeleccionadaInterna;
    private LocalDate ultimaFechaClickSimple;
    private final ObservableSet<LocalDate> fechasSeleccionadas =
            FXCollections.observableSet();

    // ========== DATOS ==========
    private Map<LocalDate, Integer> citasPorDia = new HashMap<>();
    private Map<LocalDate, List<String>> inicialesPorDia = new HashMap<>();

    // ========== PROPIEDADES OBSERVABLES ==========
    private final ObjectProperty<LocalDate> fechaSeleccionadaProp =
            new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDate> fechaDobleClickProp =
            new SimpleObjectProperty<>();
    private final ObjectProperty<YearMonth> mesVisibleProp =
            new SimpleObjectProperty<>();

    // ========== COMPONENTES UI ==========
    private HBox cabecera;
    private Button btnMesAnterior;
    private Button btnHoy;
    private Button btnMesSiguiente;
    private Label lblTituloMes;
    private GridPane gridDiasSemana;
    private GridPane gridCeldas;

    // ========== CONSTRUCTOR ==========

    /**
     * Crea el componente de calendario con el mes actual como vista inicial.
     */
    public CalendarioPersonalizado() {
        this.mesActual = YearMonth.now();
        this.fechaSeleccionadaInterna = LocalDate.now();
        this.ultimaFechaClickSimple = LocalDate.now();
        this.fechasSeleccionadas.add(LocalDate.now());

        getStyleClass().add("calendario-grid");
        setSpacing(4.0);
        VBox.setVgrow(this, Priority.ALWAYS);

        construirCabecera();
        construirGridDiasSemana();
        construirGridCeldas();

        getChildren().addAll(cabecera, gridDiasSemana, gridCeldas);
        VBox.setVgrow(gridCeldas, Priority.ALWAYS);

        refrescar();
    }

    // ========== CONSTRUCCION INTERNA ==========

    /**
     * Construye la barra de cabecera con botones de navegacion y titulo del mes.
     */
    private void construirCabecera() {
        btnMesAnterior = new Button("<");
        btnMesAnterior.getStyleClass().add("calendario-nav-button");
        btnMesAnterior.setOnAction(e -> {
            mesActual = mesActual.minusMonths(1);
            mesVisibleProp.set(mesActual);
            refrescar();
        });

        btnMesSiguiente = new Button(">");
        btnMesSiguiente.getStyleClass().add("calendario-nav-button");
        btnMesSiguiente.setOnAction(e -> {
            mesActual = mesActual.plusMonths(1);
            mesVisibleProp.set(mesActual);
            refrescar();
        });

        btnHoy = new Button("Hoy");
        btnHoy.getStyleClass().add("calendario-nav-button");
        btnHoy.setOnAction(e -> {
            mesActual = YearMonth.now();
            fechaSeleccionadaInterna = LocalDate.now();
            ultimaFechaClickSimple = LocalDate.now();
            fechasSeleccionadas.clear();
            fechasSeleccionadas.add(LocalDate.now());
            mesVisibleProp.set(mesActual);
            fechaSeleccionadaProp.set(fechaSeleccionadaInterna);
            refrescar();
        });

        lblTituloMes = new Label();
        lblTituloMes.setMaxWidth(Double.MAX_VALUE);
        lblTituloMes.setAlignment(Pos.CENTER);
        HBox.setHgrow(lblTituloMes, Priority.ALWAYS);
        lblTituloMes.getStyleClass().add("calendario-header-titulo");

        cabecera = new HBox(8, btnMesAnterior, lblTituloMes, btnHoy, btnMesSiguiente);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        cabecera.getStyleClass().add("calendario-header");
    }

    /**
     * Construye la fila de etiquetas de dias de la semana (L M X J V S D).
     */
    private void construirGridDiasSemana() {
        gridDiasSemana = new GridPane();
        gridDiasSemana.setHgap(2);

        for (int i = 0; i < 7; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(100.0 / 7.0);
            col.setHgrow(Priority.ALWAYS);
            gridDiasSemana.getColumnConstraints().add(col);

            Label lbl = new Label(DIAS_SEMANA[i]);
            lbl.setMaxWidth(Double.MAX_VALUE);
            lbl.setAlignment(Pos.CENTER);
            lbl.getStyleClass().add("calendario-dia-semana");
            gridDiasSemana.add(lbl, i, 0);
        }
    }

    /**
     * Construye el GridPane de celdas de dias (7 columnas x 6 filas = 42 celdas).
     */
    private void construirGridCeldas() {
        gridCeldas = new GridPane();
        gridCeldas.setHgap(2);
        gridCeldas.setVgap(2);

        // 7 columnas de igual anchura proporcional
        for (int i = 0; i < 7; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(100.0 / 7.0);
            col.setHgrow(Priority.ALWAYS);
            gridCeldas.getColumnConstraints().add(col);
        }

        // 6 filas de igual altura proporcional
        for (int i = 0; i < 6; i++) {
            RowConstraints row = new RowConstraints();
            row.setPercentHeight(100.0 / 6.0);
            row.setVgrow(Priority.ALWAYS);
            gridCeldas.getRowConstraints().add(row);
        }
    }

    // ========== API PUBLICA ==========

    /**
     * Actualiza el mapa de conteo de citas por dia.
     * Llamar siempre antes de refrescar() para que los cambios se reflejen.
     *
     * @param conteoPorDia mapa de fecha a numero de citas
     */
    public void setCitasPorDia(Map<LocalDate, Integer> conteoPorDia) {
        this.citasPorDia = conteoPorDia != null ? conteoPorDia : new HashMap<>();
    }

    /**
     * Actualiza el mapa de iniciales de pacientes por dia para los tooltips.
     *
     * @param inicialesPorDia mapa de fecha a lista de iniciales (e.g. "J.G.")
     */
    public void setInicialesPorDia(Map<LocalDate, List<String>> inicialesPorDia) {
        this.inicialesPorDia = inicialesPorDia != null ? inicialesPorDia : new HashMap<>();
    }

    /**
     * Devuelve el mes actualmente visible en el calendario.
     */
    public YearMonth getMesActual() {
        return mesActual;
    }

    /**
     * Navega el calendario al mes indicado y redibuja.
     *
     * @param mes mes a mostrar
     */
    public void setMesActual(YearMonth mes) {
        if (mes != null) {
            this.mesActual = mes;
            refrescar();
        }
    }

    /**
     * Devuelve la fecha actualmente seleccionada (seleccion simple).
     */
    public LocalDate getFechaSeleccionada() {
        return fechaSeleccionadaInterna;
    }

    /**
     * Establece la fecha seleccionada externamente (p.ej. desde el DatePicker).
     * Limpia la seleccion multiple y redibuja.
     *
     * @param fecha nueva fecha seleccionada
     */
    public void setFechaSeleccionada(LocalDate fecha) {
        if (fecha != null) {
            fechaSeleccionadaInterna = fecha;
            ultimaFechaClickSimple = fecha;
            fechasSeleccionadas.clear();
            fechasSeleccionadas.add(fecha);
            refrescar();
        }
    }

    /**
     * Devuelve el conjunto observable de fechas seleccionadas (seleccion multiple).
     * Util para operaciones de eliminacion en lote.
     */
    public ObservableSet<LocalDate> getFechasSeleccionadas() {
        return fechasSeleccionadas;
    }

    /**
     * Propiedad observable de la fecha seleccionada por click simple.
     * El controlador padre debe suscribirse a este cambio.
     */
    public ObjectProperty<LocalDate> fechaSeleccionadaProperty() {
        return fechaSeleccionadaProp;
    }

    /**
     * Propiedad observable disparada al hacer doble click sobre un dia.
     */
    public ObjectProperty<LocalDate> fechaDobleClickProperty() {
        return fechaDobleClickProp;
    }

    /**
     * Propiedad observable disparada al cambiar el mes visible (navegacion prev/next/hoy).
     */
    public ObjectProperty<YearMonth> mesVisibleProperty() {
        return mesVisibleProp;
    }

    // ========== GETTERS PARA TESTS (acceso package-private) ==========

    /**
     * Devuelve el GridPane de celdas. Usado en tests unitarios.
     */
    GridPane getGridCeldas() {
        return gridCeldas;
    }

    /**
     * Devuelve el boton de mes siguiente. Usado en tests unitarios.
     */
    Button getBtnMesSiguiente() {
        return btnMesSiguiente;
    }

    // ========== RENDERIZADO ==========

    /**
     * Redibuja el grid de celdas con los datos actuales de citasPorDia e inicialesPorDia.
     * Llamar siempre en el hilo de JavaFX (Application Thread).
     */
    public void refrescar() {
        // Calcular punto de inicio del grid: primer dia del mes alineado al lunes
        LocalDate primerDiaMes = mesActual.atDay(1);
        // offset: 0 = Lunes, ..., 6 = Domingo
        int offset = primerDiaMes.getDayOfWeek().getValue() - 1;
        LocalDate inicioGrid = primerDiaMes.minusDays(offset);

        // Eliminar todas las celdas del ciclo anterior
        gridCeldas.getChildren().clear();

        // Rellenar las 42 posiciones del grid (6 filas x 7 columnas)
        for (int i = 0; i < 42; i++) {
            LocalDate fecha = inicioGrid.plusDays(i);
            int col = i % 7;
            int row = i / 7;

            CeldaDia celda = new CeldaDia(
                    fecha,
                    mesActual,
                    citasPorDia,
                    inicialesPorDia,
                    fechasSeleccionadas,
                    fechaSeleccionadaInterna
            );
            configurarHandlerCelda(celda, fecha);
            gridCeldas.add(celda, col, row);
        }

        // Actualizar titulo del mes con primera letra en mayuscula
        String titulo = mesActual.format(FORMATO_MES);
        lblTituloMes.setText(Character.toUpperCase(titulo.charAt(0)) + titulo.substring(1));
    }

    // ========== HANDLERS DE INTERACCION ==========

    /**
     * Configura el handler de clicks del raton sobre una celda de dia.
     * Gestiona: click simple, Ctrl+Click (toggle), Shift+Click (rango), doble click.
     *
     * @param celda  celda de dia a la que se adjunta el handler
     * @param fecha  fecha logica que representa la celda
     */
    private void configurarHandlerCelda(CeldaDia celda, LocalDate fecha) {
        celda.setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;

            // Si es un mes adyacente, navegar a ese mes primero
            YearMonth mesFecha = YearMonth.from(fecha);
            if (!mesFecha.equals(mesActual)) {
                mesActual = mesFecha;
                mesVisibleProp.set(mesActual);
            }

            if (e.getClickCount() == 2) {
                // Doble click: seleccion simple + evento de doble click para expandir tabla
                fechaSeleccionadaInterna = fecha;
                ultimaFechaClickSimple = fecha;
                fechasSeleccionadas.clear();
                fechasSeleccionadas.add(fecha);
                fechaSeleccionadaProp.set(fecha);
                fechaDobleClickProp.set(fecha);
                refrescar();
            } else if (e.isControlDown()) {
                // Ctrl + click: toggle en la seleccion multiple
                if (fechasSeleccionadas.contains(fecha)) {
                    fechasSeleccionadas.remove(fecha);
                } else {
                    fechasSeleccionadas.add(fecha);
                }
                refrescar();
            } else if (e.isShiftDown() && ultimaFechaClickSimple != null) {
                // Shift + click: rango desde la ultima seleccion simple hasta esta fecha
                LocalDate inicio = ultimaFechaClickSimple.isBefore(fecha)
                        ? ultimaFechaClickSimple : fecha;
                LocalDate fin = ultimaFechaClickSimple.isBefore(fecha)
                        ? fecha : ultimaFechaClickSimple;
                LocalDate cursor = inicio;
                while (!cursor.isAfter(fin)) {
                    fechasSeleccionadas.add(cursor);
                    cursor = cursor.plusDays(1);
                }
                refrescar();
            } else {
                // Click simple: seleccion unica, limpia la seleccion multiple
                fechaSeleccionadaInterna = fecha;
                ultimaFechaClickSimple = fecha;
                fechasSeleccionadas.clear();
                fechasSeleccionadas.add(fecha);
                fechaSeleccionadaProp.set(fecha);
                refrescar();
            }
        });
    }

    // ========== CLASE INTERNA: CELDA DIA ==========

    /**
     * Celda individual del calendario que representa un dia concreto.
     * Muestra el numero del dia y, si hay citas, un badge con el conteo.
     * Aplica estilos CSS condicionales segun el tipo de dia.
     */
    static class CeldaDia extends VBox {

        // Campo accesible para tests unitarios
        private final LocalDate fecha;
        private final Label lblBadgeCitas;

        CeldaDia(LocalDate fecha,
                 YearMonth mesVisible,
                 Map<LocalDate, Integer> citasPorDia,
                 Map<LocalDate, List<String>> inicialesPorDia,
                 ObservableSet<LocalDate> fechasSeleccionadas,
                 LocalDate fechaSeleccionadaPrimaria) {

            this.fecha = fecha;

            getStyleClass().add("calendario-celda");
            setSpacing(2.0);
            setAlignment(Pos.TOP_LEFT);
            setMaxWidth(Double.MAX_VALUE);
            setMaxHeight(Double.MAX_VALUE);

            // Etiqueta con el numero del dia del mes
            Label lblNumeroDia = new Label(String.valueOf(fecha.getDayOfMonth()));
            lblNumeroDia.getStyleClass().add("calendario-dia-numero");

            // Badge de citas: solo visible si conteo > 0
            this.lblBadgeCitas = new Label();
            lblBadgeCitas.getStyleClass().add("calendario-badge-citas");
            lblBadgeCitas.setVisible(false);
            lblBadgeCitas.setManaged(false);

            getChildren().addAll(lblNumeroDia, lblBadgeCitas);

            // Aplicar estilos condicionales y datos
            aplicarEstilosYDatos(fecha, mesVisible, citasPorDia, inicialesPorDia,
                    fechasSeleccionadas, fechaSeleccionadaPrimaria, lblBadgeCitas);
        }

        /** Devuelve la fecha logica que representa esta celda. Usado en tests. */
        LocalDate getFecha() {
            return fecha;
        }

        /** Devuelve el texto del badge de citas. Usado en tests. */
        String getBadgeTexto() {
            return lblBadgeCitas.getText();
        }

        /** Indica si el badge de citas esta visible. Usado en tests. */
        boolean isBadgeVisible() {
            return lblBadgeCitas.isVisible();
        }

        /**
         * Aplica las clases CSS condicionales y configura el badge de citas y el tooltip.
         */
        private void aplicarEstilosYDatos(LocalDate fecha,
                YearMonth mesVisible,
                Map<LocalDate, Integer> citasPorDia,
                Map<LocalDate, List<String>> inicialesPorDia,
                ObservableSet<LocalDate> fechasSeleccionadas,
                LocalDate fechaSeleccionadaPrimaria,
                Label lblBadgeCitas) {

            // Dia fuera del mes visible (mes anterior o siguiente)
            if (!YearMonth.from(fecha).equals(mesVisible)) {
                getStyleClass().add("calendario-celda-otro-mes");
            }

            // Fin de semana: sabado (6) y domingo (7)
            int diaSemana = fecha.getDayOfWeek().getValue();
            if (diaSemana == 6 || diaSemana == 7) {
                getStyleClass().add("calendario-celda-fin-semana");
            }

            // Dia de hoy: tiene prioridad visual maxima
            if (fecha.equals(LocalDate.now())) {
                getStyleClass().add("calendario-celda-hoy");
            }

            // Dia seleccionado (individual o parte de seleccion multiple)
            if (fechasSeleccionadas.contains(fecha) || fecha.equals(fechaSeleccionadaPrimaria)) {
                getStyleClass().add("calendario-celda-seleccionada");
            }

            // Badge de citas y tooltip con iniciales de pacientes
            int conteo = citasPorDia.getOrDefault(fecha, 0);
            if (conteo > 0) {
                lblBadgeCitas.setText(conteo + (conteo == 1 ? " cita" : " citas"));
                lblBadgeCitas.setVisible(true);
                lblBadgeCitas.setManaged(true);

                // Tooltip con las iniciales de los pacientes del dia
                List<String> iniciales = inicialesPorDia.getOrDefault(fecha, List.of());
                if (!iniciales.isEmpty()) {
                    Tooltip tt = new Tooltip(String.join(", ", iniciales));
                    tt.setShowDelay(Duration.millis(300));
                    Tooltip.install(this, tt);
                }
            }
        }
    }
}
