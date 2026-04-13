package com.javafx.Interface;

import com.javafx.Clases.Discapacidad;
import com.javafx.Clases.SesionUsuario;
import com.javafx.Clases.VentanaUtil;
import com.javafx.Clases.VentanaUtil.TipoMensaje;
import com.javafx.dto.DiscapacidadRequest;
import com.javafx.excepcion.ConexionException;
import com.javafx.excepcion.DuplicadoException;
import com.javafx.excepcion.RehabiAppException;
import com.javafx.service.CatalogoService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

/**
 * Controlador para la ventana de gestion de discapacidades del catalogo clinico.
 * Solo accesible para SPECIALIST (enfermeros no ven esta pestana).
 */
public class controladorVentanaDiscapacidades {

    @FXML
    private Button btnAnadirDiscapacidad;

    @FXML
    private Button btnBuscarDiscapacidades;

    @FXML
    private Button btnEditarDiscapacidad;

    @FXML
    private Button btnEliminarDiscapacidad;

    @FXML
    private Button btnSeleccionarTodoDiscapacidades;

    @FXML
    private TableColumn<Discapacidad, String> colCodigo;

    @FXML
    private TableColumn<Discapacidad, String> colDescripcion;

    @FXML
    private TableColumn<Discapacidad, String> colNombre;

    @FXML
    private TableColumn<Discapacidad, Boolean> colProtesis;

    @FXML
    private Label lblBuscarDiscapacidades;

    @FXML
    private Label lblTituloPestaniaDiscapacidades;

    @FXML
    private TableView<Discapacidad> tblDiscapacidades;

    @FXML
    private TextField txfBuscarDiscapacidades;

    @FXML
    private VBox vboxContenedorPrinDiscapacidades;

    // Lista observable de discapacidades
    private ObservableList<Discapacidad> listaDiscapacidades;

    // Servicio del catalogo
    private CatalogoService catalogoService;

    // Cache de todas las discapacidades
    private List<Discapacidad> todasDiscapacidades;

    /**
     * Metodo initialize se ejecuta automaticamente al cargar el FXML
     */
    @FXML
    public void initialize() {
        catalogoService = new CatalogoService();
        listaDiscapacidades = FXCollections.observableArrayList();

        configurarTabla();

        // Permitir seleccion multiple
        tblDiscapacidades.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Doble clic para editar
        tblDiscapacidades.setOnMouseClicked(this::manejarDobleClicTabla);

        cargarDiscapacidades();
    }

    /**
     * Maneja el evento de doble clic en la tabla.
     * Si es doble clic, abre el formulario de edicion.
     */
    private void manejarDobleClicTabla(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
            Discapacidad seleccionada = tblDiscapacidades.getSelectionModel().getSelectedItem();
            if (seleccionada != null) {
                editarDiscapacidadSeleccionada(null);
            }
        }
    }

    /**
     * Configura los permisos segun el rol del usuario.
     * Esta pestana solo es visible para SPECIALIST, pero se aplica RBAC
     * igualmente como medida de defensa en profundidad.
     */
    public void configurarPermisos() {
        SesionUsuario sesion = SesionUsuario.getInstancia();

        if (!sesion.esEspecialista()) {
            // Deshabilitar boton de anadir
            btnAnadirDiscapacidad.setDisable(true);
            btnAnadirDiscapacidad.setOpacity(0.5);
            btnAnadirDiscapacidad.setTooltip(new Tooltip(
                    "No tienes permisos para anadir discapacidades"));

            // Deshabilitar boton de eliminar
            btnEliminarDiscapacidad.setDisable(true);
            btnEliminarDiscapacidad.setOpacity(0.5);
            btnEliminarDiscapacidad.setTooltip(new Tooltip(
                    "No tienes permisos para eliminar discapacidades"));

            // Deshabilitar boton de editar
            btnEditarDiscapacidad.setDisable(true);
            btnEditarDiscapacidad.setOpacity(0.5);
            btnEditarDiscapacidad.setTooltip(new Tooltip(
                    "No tienes permisos para editar discapacidades"));

            System.out.println("Permisos de solo lectura aplicados para discapacidades: " + sesion.getCargo());
        } else {
            System.out.println("Permisos completos aplicados para discapacidades: " + sesion.getCargo());
        }
    }

    /**
     * Configura las columnas de la tabla
     */
    private void configurarTabla() {
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codDis"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombreDis"));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcionDis"));

        // CellFactory para mostrar "Si"/"No" en vez de true/false
        colProtesis.setCellValueFactory(new PropertyValueFactory<>("necesitaProtesis"));
        colProtesis.setCellFactory(column -> new TableCell<Discapacidad, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item ? "Si" : "No");
                }
            }
        });

        tblDiscapacidades.setItems(listaDiscapacidades);
    }

    /**
     * Carga todas las discapacidades desde el servicio del catalogo
     */
    private void cargarDiscapacidades() {
        try {
            todasDiscapacidades = catalogoService.listarDiscapacidades();
            listaDiscapacidades.clear();
            listaDiscapacidades.addAll(todasDiscapacidades);
            System.out.println("Discapacidades cargadas: " + todasDiscapacidades.size());
        } catch (ConexionException e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Error de conexion al cargar discapacidades.",
                    TipoMensaje.ERROR
            );
        } catch (Exception e) {
            System.err.println("Error al cargar discapacidades: " + e.getMessage());
            e.printStackTrace();
            VentanaUtil.mostrarVentanaInformativa(
                    "Error al cargar las discapacidades: " + e.getMessage(),
                    TipoMensaje.ERROR
            );
        }
    }

    /**
     * Busca discapacidades por texto (codigo o nombre) — filtrado local en memoria
     */
    @FXML
    void buscarDiscapacidades(ActionEvent event) {
        String textoBusqueda = txfBuscarDiscapacidades.getText().trim().toLowerCase();

        listaDiscapacidades.clear();

        if (textoBusqueda.isEmpty()) {
            if (todasDiscapacidades != null) {
                listaDiscapacidades.addAll(todasDiscapacidades);
            } else {
                cargarDiscapacidades();
            }
        } else {
            List<Discapacidad> filtradas = todasDiscapacidades.stream()
                    .filter(d -> d.getCodDis().toLowerCase().contains(textoBusqueda)
                              || d.getNombreDis().toLowerCase().contains(textoBusqueda))
                    .toList();
            listaDiscapacidades.addAll(filtradas);
        }
    }

    /**
     * Abre el formulario para crear una nueva discapacidad
     */
    @FXML
    void abrirFormularioNuevaDiscapacidad(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaAgregarDiscapacidad.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

            Stage stage = new Stage();
            stage.setTitle("Nueva Discapacidad");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            VentanaUtil.establecerIconoVentana(stage);
            stage.showAndWait();

            cargarDiscapacidades();

        } catch (Exception e) {
            System.err.println("Error al abrir formulario de nueva discapacidad: " + e.getMessage());
            e.printStackTrace();
            VentanaUtil.mostrarVentanaInformativa(
                    "Error al abrir el formulario de nueva discapacidad.",
                    TipoMensaje.ERROR
            );
        }
    }

    /**
     * Abre el formulario de edicion para la discapacidad seleccionada
     */
    @FXML
    void editarDiscapacidadSeleccionada(ActionEvent event) {
        Discapacidad seleccionada = tblDiscapacidades.getSelectionModel().getSelectedItem();

        if (seleccionada == null) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Debe seleccionar una discapacidad de la lista.",
                    TipoMensaje.ADVERTENCIA
            );
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaAgregarDiscapacidad.fxml"));
            Parent root = loader.load();

            controladorAgregarDiscapacidad controlador = loader.getController();
            controlador.cargarDatosParaEdicion(seleccionada);

            Scene scene = new Scene(root);
            controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

            Stage stage = new Stage();
            stage.setTitle("Editar Discapacidad");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            VentanaUtil.establecerIconoVentana(stage);
            stage.showAndWait();

            cargarDiscapacidades();

        } catch (Exception e) {
            System.err.println("Error al abrir formulario de edicion: " + e.getMessage());
            e.printStackTrace();
            VentanaUtil.mostrarVentanaInformativa(
                    "Error al abrir la edicion de la discapacidad.",
                    TipoMensaje.ERROR
            );
        }
    }

    /**
     * Elimina la discapacidad seleccionada con confirmacion previa.
     * La API rechaza con 409 si hay pacientes asignados (DuplicadoException).
     */
    @FXML
    void eliminarDiscapacidadSeleccionada(ActionEvent event) {
        Discapacidad seleccionada = tblDiscapacidades.getSelectionModel().getSelectedItem();

        if (seleccionada == null) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Debe seleccionar una discapacidad para eliminar.",
                    TipoMensaje.ADVERTENCIA
            );
            return;
        }

        String mensaje = "¿Esta seguro de que desea eliminar la discapacidad '"
                + seleccionada.getNombreDis() + "' (" + seleccionada.getCodDis() + ")?\n\n"
                + "Se eliminaran tambien sus vinculos con tratamientos.";

        boolean confirmado = VentanaUtil.mostrarVentanaPregunta(mensaje);

        if (confirmado) {
            try {
                catalogoService.eliminarDiscapacidad(seleccionada.getCodDis());
                VentanaUtil.mostrarVentanaInformativa(
                        "La discapacidad ha sido eliminada correctamente.",
                        TipoMensaje.EXITO
                );
                cargarDiscapacidades();

            } catch (DuplicadoException e) {
                // 409: hay pacientes asignados a esta discapacidad
                VentanaUtil.mostrarVentanaInformativa(
                        "No se puede eliminar: " + e.getMessage(),
                        TipoMensaje.ADVERTENCIA
                );
            } catch (ConexionException e) {
                VentanaUtil.mostrarVentanaInformativa(
                        "Error de conexion con el servidor.",
                        TipoMensaje.ERROR
                );
            } catch (RehabiAppException e) {
                VentanaUtil.mostrarVentanaInformativa(
                        "Error: " + e.getMessage(),
                        TipoMensaje.ERROR
                );
            }
        }
    }

    /**
     * Selecciona todas las discapacidades de la tabla
     */
    @FXML
    void seleccionarTodasDiscapacidades(ActionEvent event) {
        tblDiscapacidades.getSelectionModel().selectAll();
    }
}
