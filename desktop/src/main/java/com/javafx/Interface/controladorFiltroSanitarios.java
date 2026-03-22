package com.javafx.Interface;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;

/**
 * Controlador para la ventana de filtros de sanitarios
 * Permite filtrar y ordenar la lista de sanitarios segun varios criterios
 */
public class controladorFiltroSanitarios {

    @FXML
    private Label lblTituloFiltros;

    //Filtros de cargo
    @FXML
    private CheckBox chkEspecialistas;

    @FXML
    private CheckBox chkEnfermeros;

    //Filtros de pacientes asignados
    @FXML
    private CheckBox chkConPacientes;

    @FXML
    private CheckBox chkSinPacientes;

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
    private FiltrosSanitario filtros;

    /**
     * Metodo initialize se ejecuta automaticamente al cargar el FXML
     */
    @FXML
    public void initialize() {
        //Inicializar ComboBox de ordenacion
        cmbOrdenarPor.getItems().addAll(
                "Nombre",
                "Apellidos",
                "DNI",
                "Cargo",
                "Num. Pacientes"
        );
        cmbOrdenarPor.setValue("Nombre");

        //Inicializar objeto de filtros
        filtros = new FiltrosSanitario();
    }

    /**
     * Aplica los filtros seleccionados y cierra la ventana
     */
    @FXML
    void aplicarFiltros(ActionEvent event) {
        //Recoger filtros de cargo
        filtros.setSoloEspecialistas(chkEspecialistas.isSelected());
        filtros.setSoloEnfermeros(chkEnfermeros.isSelected());

        //Recoger filtros de pacientes
        filtros.setConPacientes(chkConPacientes.isSelected());
        filtros.setSinPacientes(chkSinPacientes.isSelected());

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
        //Restablecer cargo
        chkEspecialistas.setSelected(false);
        chkEnfermeros.setSelected(false);

        //Restablecer pacientes
        chkConPacientes.setSelected(false);
        chkSinPacientes.setSelected(false);

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
     * @return Objeto FiltrosSanitario con los criterios
     */
    public FiltrosSanitario getFiltros() {
        return filtros;
    }

    // ==================== CLASE INTERNA PARA FILTROS ====================

    /**
     * Clase que encapsula los criterios de filtrado de sanitarios
     */
    public static class FiltrosSanitario {
        private boolean soloEspecialistas = false;
        private boolean soloEnfermeros = false;
        private boolean conPacientes = false;
        private boolean sinPacientes = false;
        private String ordenarPor = "Nombre";
        private boolean ordenAscendente = true;

        //Getters y Setters
        public boolean isSoloEspecialistas() {
            return soloEspecialistas;
        }

        public void setSoloEspecialistas(boolean soloEspecialistas) {
            this.soloEspecialistas = soloEspecialistas;
        }

        public boolean isSoloEnfermeros() {
            return soloEnfermeros;
        }

        public void setSoloEnfermeros(boolean soloEnfermeros) {
            this.soloEnfermeros = soloEnfermeros;
        }

        public boolean isConPacientes() {
            return conPacientes;
        }

        public void setConPacientes(boolean conPacientes) {
            this.conPacientes = conPacientes;
        }

        public boolean isSinPacientes() {
            return sinPacientes;
        }

        public void setSinPacientes(boolean sinPacientes) {
            this.sinPacientes = sinPacientes;
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
            return soloEspecialistas || soloEnfermeros || conPacientes || sinPacientes;
        }
    }
}