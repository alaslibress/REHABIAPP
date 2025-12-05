package com.javafx.Interface;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;

/**
 * Controlador para la ventana de filtros de pacientes
 * Permite filtrar y ordenar la lista de pacientes segun varios criterios
 */
public class controladorFiltroPacientes {

    @FXML
    private Label lblTituloFiltros;

    //Filtros de protesis
    @FXML
    private CheckBox chkConProtesis;

    @FXML
    private CheckBox chkSinProtesis;

    //Filtros de edad
    @FXML
    private CheckBox chkFiltrarEdad;

    @FXML
    private Spinner<Integer> spnEdadMinima;

    @FXML
    private Spinner<Integer> spnEdadMaxima;

    //Ordenacion
    @FXML
    private ComboBox<String> cmbOrdenarPor;

    @FXML
    private RadioButton rdbAscendente;

    @FXML
    private RadioButton rdbDescendente;

    @FXML
    private ToggleGroup grupoOrden;

    //Indica si se aplicaron filtros
    private boolean filtrosAplicados = false;

    //Objeto para devolver los filtros seleccionados
    private FiltrosPaciente filtros;

    /**
     * Metodo initialize se ejecuta automaticamente al cargar el FXML
     */
    @FXML
    public void initialize() {
        //Inicializar spinners de edad
        spnEdadMinima.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 120, 0));
        spnEdadMaxima.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 120, 120));

        //Deshabilitar spinners hasta que se active el filtro de edad
        spnEdadMinima.setDisable(true);
        spnEdadMaxima.setDisable(true);

        //Listener para habilitar/deshabilitar spinners de edad
        chkFiltrarEdad.selectedProperty().addListener((obs, oldVal, newVal) -> {
            spnEdadMinima.setDisable(!newVal);
            spnEdadMaxima.setDisable(!newVal);
        });

        //Inicializar ComboBox de ordenacion
        cmbOrdenarPor.getItems().addAll(
                "Nombre",
                "Apellidos",
                "DNI",
                "Edad",
                "Discapacidad"
        );
        cmbOrdenarPor.setValue("Nombre");

        //Inicializar objeto de filtros
        filtros = new FiltrosPaciente();
    }

    /**
     * Aplica los filtros seleccionados y cierra la ventana
     */
    @FXML
    void aplicarFiltros(ActionEvent event) {
        //Recoger filtros de protesis
        filtros.setConProtesis(chkConProtesis.isSelected());
        filtros.setSinProtesis(chkSinProtesis.isSelected());

        //Recoger filtros de edad
        filtros.setFiltrarEdad(chkFiltrarEdad.isSelected());
        if (chkFiltrarEdad.isSelected()) {
            filtros.setEdadMinima(spnEdadMinima.getValue());
            filtros.setEdadMaxima(spnEdadMaxima.getValue());
        }

        //Recoger ordenacion
        filtros.setOrdenarPor(cmbOrdenarPor.getValue());
        filtros.setOrdenAscendente(rdbAscendente.isSelected());

        filtrosAplicados = true;

        //Cerrar ventana
        cerrarVentana(null);
    }

    /**
     * Restablece todos los filtros a sus valores por defecto
     */
    @FXML
    void restablecerFiltros(ActionEvent event) {
        //Restablecer protesis
        chkConProtesis.setSelected(false);
        chkSinProtesis.setSelected(false);

        //Restablecer edad
        chkFiltrarEdad.setSelected(false);
        spnEdadMinima.getValueFactory().setValue(0);
        spnEdadMaxima.getValueFactory().setValue(120);

        //Restablecer ordenacion
        cmbOrdenarPor.setValue("Nombre");
        rdbAscendente.setSelected(true);
    }

    /**
     * Cierra la ventana sin aplicar filtros
     */
    @FXML
    void cerrarVentana(ActionEvent event) {
        Stage stage = (Stage) lblTituloFiltros.getScene().getWindow();
        stage.close();
    }

    /**
     * Indica si se aplicaron filtros
     * @return true si se pulsó aplicar, false si se canceló
     */
    public boolean seFiltrosAplicados() {
        return filtrosAplicados;
    }

    /**
     * Obtiene los filtros seleccionados
     * @return Objeto FiltrosPaciente con los criterios
     */
    public FiltrosPaciente getFiltros() {
        return filtros;
    }

    // ==================== CLASE INTERNA PARA FILTROS ====================

    /**
     * Clase que encapsula los criterios de filtrado de pacientes
     */
    public static class FiltrosPaciente {
        private boolean conProtesis = false;
        private boolean sinProtesis = false;
        private boolean filtrarEdad = false;
        private int edadMinima = 0;
        private int edadMaxima = 120;
        private String ordenarPor = "Nombre";
        private boolean ordenAscendente = true;

        //Getters y Setters
        public boolean isConProtesis() {
            return conProtesis;
        }

        public void setConProtesis(boolean conProtesis) {
            this.conProtesis = conProtesis;
        }

        public boolean isSinProtesis() {
            return sinProtesis;
        }

        public void setSinProtesis(boolean sinProtesis) {
            this.sinProtesis = sinProtesis;
        }

        public boolean isFiltrarEdad() {
            return filtrarEdad;
        }

        public void setFiltrarEdad(boolean filtrarEdad) {
            this.filtrarEdad = filtrarEdad;
        }

        public int getEdadMinima() {
            return edadMinima;
        }

        public void setEdadMinima(int edadMinima) {
            this.edadMinima = edadMinima;
        }

        public int getEdadMaxima() {
            return edadMaxima;
        }

        public void setEdadMaxima(int edadMaxima) {
            this.edadMaxima = edadMaxima;
        }

        public String getOrdenarPor() {
            return ordenarPor;
        }

        public void setOrdenarPor(String ordenarPor) {
            this.ordenarPor = ordenarPor;
        }

        public boolean isOrdenAscendente() {
            return ordenAscendente;
        }

        public void setOrdenAscendente(boolean ordenAscendente) {
            this.ordenAscendente = ordenAscendente;
        }

        /**
         * Indica si hay algun filtro activo
         * @return true si hay filtros activos
         */
        public boolean hayFiltrosActivos() {
            return conProtesis || sinProtesis || filtrarEdad;
        }
    }
}