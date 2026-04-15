package com.javafx.Interface;

import com.javafx.Clases.Discapacidad;
import com.javafx.Clases.NivelProgresion;
import com.javafx.excepcion.ConexionException;
import com.javafx.service.CatalogoService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.List;

/**
 * Controlador para la ventana de filtros de tratamientos.
 * Permite filtrar por discapacidad vinculada, nivel de progresion y criterio de orden.
 */
public class controladorFiltroTratamientos {

    @FXML
    private Label lblTituloFiltros;

    // Filtro por discapacidad vinculada
    @FXML
    private ComboBox<Discapacidad> cmbFiltroDiscapacidad;

    // Filtro por nivel de progresion
    @FXML
    private ComboBox<NivelProgresion> cmbFiltroNivel;

    // Criterio de ordenacion
    @FXML
    private ComboBox<String> cmbOrdenarPor;

    // Indica si se aplicaron filtros (true) o se cancelo (false)
    private boolean filtrosAplicados = false;

    // Servicio de catalogo para cargar discapacidades y niveles
    private final CatalogoService catalogoService = new CatalogoService();

    /**
     * Metodo initialize se ejecuta automaticamente al cargar el FXML.
     * Carga los datos de los ComboBox desde la API.
     */
    @FXML
    public void initialize() {
        cargarDiscapacidades();
        cargarNiveles();
        inicializarOrden();
    }

    /**
     * Carga el ComboBox de discapacidades con todos los registros del catalogo.
     * Incluye una opcion nula al inicio que representa "Todas".
     */
    private void cargarDiscapacidades() {
        try {
            List<Discapacidad> discapacidades = catalogoService.listarDiscapacidades();
            cmbFiltroDiscapacidad.getItems().clear();
            cmbFiltroDiscapacidad.getItems().add(null); // opcion "Todas"
            cmbFiltroDiscapacidad.getItems().addAll(discapacidades);

            // Mostrar "Todas" cuando el valor es null
            cmbFiltroDiscapacidad.setConverter(new StringConverter<Discapacidad>() {
                @Override
                public String toString(Discapacidad discapacidad) {
                    return discapacidad == null ? "Todas" : discapacidad.getNombreDis();
                }

                @Override
                public Discapacidad fromString(String string) {
                    return null;
                }
            });

        } catch (ConexionException e) {
            System.err.println("Error de conexion al cargar discapacidades en filtro: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error al cargar discapacidades en filtro: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Carga el ComboBox de niveles de progresion con todos los registros del catalogo.
     * Incluye una opcion nula al inicio que representa "Todos".
     */
    private void cargarNiveles() {
        try {
            List<NivelProgresion> niveles = catalogoService.listarNivelesProgresion();
            cmbFiltroNivel.getItems().clear();
            cmbFiltroNivel.getItems().add(null); // opcion "Todos"
            cmbFiltroNivel.getItems().addAll(niveles);

            // Mostrar "Todos" cuando el valor es null
            cmbFiltroNivel.setConverter(new StringConverter<NivelProgresion>() {
                @Override
                public String toString(NivelProgresion nivel) {
                    return nivel == null ? "Todos" : nivel.getNombre();
                }

                @Override
                public NivelProgresion fromString(String string) {
                    return null;
                }
            });

        } catch (ConexionException e) {
            System.err.println("Error de conexion al cargar niveles en filtro: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error al cargar niveles en filtro: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Inicializa el ComboBox de ordenacion con los criterios disponibles.
     */
    private void inicializarOrden() {
        cmbOrdenarPor.getItems().addAll("Nombre A-Z", "Nombre Z-A", "Codigo");
        cmbOrdenarPor.setValue("Nombre A-Z");
    }

    /**
     * Aplica los filtros seleccionados y cierra la ventana.
     * El controlador padre lee los valores mediante los getters.
     */
    @FXML
    void aplicarFiltros(ActionEvent event) {
        filtrosAplicados = true;
        cerrarVentana(null);
    }

    /**
     * Restablece todos los filtros a sus valores por defecto.
     */
    @FXML
    void restablecerFiltros(ActionEvent event) {
        cmbFiltroDiscapacidad.setValue(null);
        cmbFiltroNivel.setValue(null);
        cmbOrdenarPor.setValue("Nombre A-Z");
        filtrosAplicados = false;
    }

    /**
     * Cierra la ventana sin aplicar filtros.
     */
    @FXML
    void cerrarVentana(ActionEvent event) {
        Stage stage = (Stage) lblTituloFiltros.getScene().getWindow();
        stage.close();
    }

    // ==================== GETTERS PARA EL CONTROLADOR PADRE ====================

    /**
     * Indica si el usuario pulso "Aplicar" o "Cancelar"/"Restablecer".
     * @return true si se pulso Aplicar, false si se cancelo
     */
    public boolean isFiltrosAplicados() {
        return filtrosAplicados;
    }

    /**
     * Devuelve la discapacidad seleccionada, o null si se selecciono "Todas".
     * @return Discapacidad seleccionada o null
     */
    public Discapacidad getDiscapacidadSeleccionada() {
        return cmbFiltroDiscapacidad.getValue();
    }

    /**
     * Devuelve el nivel de progresion seleccionado, o null si se selecciono "Todos".
     * @return NivelProgresion seleccionado o null
     */
    public NivelProgresion getNivelSeleccionado() {
        return cmbFiltroNivel.getValue();
    }

    /**
     * Devuelve el criterio de ordenacion seleccionado.
     * @return Cadena con el criterio: "Nombre A-Z", "Nombre Z-A" o "Codigo"
     */
    public String getOrden() {
        return cmbOrdenarPor.getValue();
    }
}
