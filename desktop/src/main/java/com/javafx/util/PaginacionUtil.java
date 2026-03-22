package com.javafx.util;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.util.List;

/**
 * Utilidad para implementar paginacion en TableView
 * Reduce el tiempo de carga mostrando solo un subconjunto de datos
 */
public class PaginacionUtil<T> {

    private List<T> todosLosDatos;
    private ObservableList<T> datosPaginados;
    private int paginaActual;
    private int registrosPorPagina;
    private int totalPaginas;

    // Componentes UI para controles de paginacion
    private Button btnPrimera;
    private Button btnAnterior;
    private Button btnSiguiente;
    private Button btnUltima;
    private Label lblInfoPagina;

    /**
     * Constructor
     * @param registrosPorPagina Numero de registros a mostrar por pagina
     */
    public PaginacionUtil(int registrosPorPagina) {
        this.registrosPorPagina = registrosPorPagina;
        this.paginaActual = 0;
        this.datosPaginados = FXCollections.observableArrayList();
    }

    /**
     * Establece los datos completos y calcula paginacion
     * @param datos Lista completa de datos
     */
    public void setDatos(List<T> datos) {
        this.todosLosDatos = datos;
        this.paginaActual = 0;
        calcularTotalPaginas();
        actualizarPaginaActual();
    }

    /**
     * Calcula el numero total de paginas
     */
    private void calcularTotalPaginas() {
        if (todosLosDatos == null || todosLosDatos.isEmpty()) {
            totalPaginas = 0;
        } else {
            totalPaginas = (int) Math.ceil((double) todosLosDatos.size() / registrosPorPagina);
        }
    }

    /**
     * Actualiza los datos de la pagina actual
     */
    private void actualizarPaginaActual() {
        datosPaginados.clear();

        if (todosLosDatos == null || todosLosDatos.isEmpty()) {
            return;
        }

        int inicio = paginaActual * registrosPorPagina;
        int fin = Math.min(inicio + registrosPorPagina, todosLosDatos.size());

        datosPaginados.addAll(todosLosDatos.subList(inicio, fin));

        actualizarControles();
    }

    /**
     * Obtiene la lista observable con los datos de la pagina actual
     * @return ObservableList para vincular a TableView
     */
    public ObservableList<T> getDatosPaginados() {
        return datosPaginados;
    }

    /**
     * Navega a la primera pagina
     */
    public void irPrimeraPagina() {
        if (paginaActual != 0) {
            paginaActual = 0;
            actualizarPaginaActual();
        }
    }

    /**
     * Navega a la pagina anterior
     */
    public void irPaginaAnterior() {
        if (paginaActual > 0) {
            paginaActual--;
            actualizarPaginaActual();
        }
    }

    /**
     * Navega a la pagina siguiente
     */
    public void irPaginaSiguiente() {
        if (paginaActual < totalPaginas - 1) {
            paginaActual++;
            actualizarPaginaActual();
        }
    }

    /**
     * Navega a la ultima pagina
     */
    public void irUltimaPagina() {
        if (paginaActual != totalPaginas - 1 && totalPaginas > 0) {
            paginaActual = totalPaginas - 1;
            actualizarPaginaActual();
        }
    }

    /**
     * Crea y configura los controles de paginacion
     * @return HBox con los controles de navegacion
     */
    public HBox crearControlesPaginacion() {
        HBox controles = new HBox(10);
        controles.setStyle("-fx-alignment: center; -fx-padding: 10;");

        btnPrimera = new Button("<<");
        btnPrimera.setOnAction(e -> irPrimeraPagina());
        btnPrimera.setTooltip(new javafx.scene.control.Tooltip("Primera página"));

        btnAnterior = new Button("<");
        btnAnterior.setOnAction(e -> irPaginaAnterior());
        btnAnterior.setTooltip(new javafx.scene.control.Tooltip("Página anterior"));

        lblInfoPagina = new Label();
        lblInfoPagina.setStyle("-fx-min-width: 150; -fx-alignment: center;");

        btnSiguiente = new Button(">");
        btnSiguiente.setOnAction(e -> irPaginaSiguiente());
        btnSiguiente.setTooltip(new javafx.scene.control.Tooltip("Página siguiente"));

        btnUltima = new Button(">>");
        btnUltima.setOnAction(e -> irUltimaPagina());
        btnUltima.setTooltip(new javafx.scene.control.Tooltip("Última página"));

        controles.getChildren().addAll(btnPrimera, btnAnterior, lblInfoPagina, btnSiguiente, btnUltima);

        actualizarControles();

        return controles;
    }

    /**
     * Actualiza el estado de los controles de navegacion
     */
    private void actualizarControles() {
        if (btnPrimera == null) return;

        // Actualizar label de informacion
        if (todosLosDatos == null || todosLosDatos.isEmpty()) {
            lblInfoPagina.setText("No hay registros");
        } else {
            int inicio = paginaActual * registrosPorPagina + 1;
            int fin = Math.min((paginaActual + 1) * registrosPorPagina, todosLosDatos.size());
            lblInfoPagina.setText(String.format("Página %d de %d (%d-%d de %d)",
                    paginaActual + 1, Math.max(1, totalPaginas), inicio, fin, todosLosDatos.size()));
        }

        // Habilitar/deshabilitar botones segun la pagina actual
        btnPrimera.setDisable(paginaActual == 0);
        btnAnterior.setDisable(paginaActual == 0);
        btnSiguiente.setDisable(paginaActual >= totalPaginas - 1);
        btnUltima.setDisable(paginaActual >= totalPaginas - 1);
    }

    /**
     * Obtiene el numero de la pagina actual (0-indexed)
     * @return Numero de pagina actual
     */
    public int getPaginaActual() {
        return paginaActual;
    }

    /**
     * Obtiene el total de paginas
     * @return Total de paginas
     */
    public int getTotalPaginas() {
        return totalPaginas;
    }

    /**
     * Obtiene el total de registros
     * @return Total de registros
     */
    public int getTotalRegistros() {
        return todosLosDatos != null ? todosLosDatos.size() : 0;
    }

    /**
     * Cambia el numero de registros por pagina
     * @param nuevoTamanio Nuevo numero de registros por pagina
     */
    public void setRegistrosPorPagina(int nuevoTamanio) {
        this.registrosPorPagina = nuevoTamanio;
        calcularTotalPaginas();
        paginaActual = 0;
        actualizarPaginaActual();
    }
}
