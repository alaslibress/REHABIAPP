package com.javafx.Interface;

import com.javafx.Clases.Discapacidad;
import com.javafx.Clases.SesionUsuario;
import com.javafx.Clases.Videojuego;
import com.javafx.Clases.VentanaUtil;
import com.javafx.Clases.VentanaUtil.TipoMensaje;
import com.javafx.DAO.VideojuegoDAO;
import com.javafx.excepcion.ConexionException;
import com.javafx.excepcion.DuplicadoException;
import com.javafx.excepcion.RehabiAppException;
import com.javafx.service.CatalogoService;
import com.javafx.util.PaginacionUtil;
import com.javafx.util.TableUiUtil;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * Controlador para la gestion del catalogo de videojuegos terapeuticos.
 * Permite al SPECIALIST crear, editar y eliminar videojuegos, asignando
 * cada uno a la discapacidad correspondiente (campo cod_dis).
 * Solo accesible para SPECIALIST.
 */
public class controladorVentanaVideojuegos {

    @FXML private Button btnAnadirVideojuego;
    @FXML private Button btnBuscarVideojuegos;
    @FXML private Button btnEditarVideojuego;
    @FXML private Button btnEliminarVideojuego;
    @FXML private ComboBox<Object> cmbFiltroDiscapacidad;
    @FXML private TableColumn<Videojuego, String> colCodigo;
    @FXML private TableColumn<Videojuego, String> colDiscapacidad;
    @FXML private TableColumn<Videojuego, String> colNombre;
    @FXML private TableColumn<Videojuego, String> colParteCuerpo;
    @FXML private TableColumn<Videojuego, Boolean> colActivo;
    @FXML private Label lblTituloPestaniaVideojuegos;
    @FXML private TableView<Videojuego> tblVideojuegos;
    @FXML private TextField txfBuscarVideojuegos;
    @FXML private VBox vboxContenedorPrinVideojuegos;

    private ObservableList<Videojuego> listaVideojuegos;
    private final VideojuegoDAO videojuegoDAO = new VideojuegoDAO();
    private final CatalogoService catalogoService = new CatalogoService();
    private List<Videojuego> todosVideojuegos = new ArrayList<>();

    private PaginacionUtil<Videojuego> paginacion;
    private static final int REGISTROS_POR_PAGINA = 50;
    // Entrada sintetica para "todas las discapacidades" en el filtro
    private static final String TODAS = "Todas";

    @FXML
    public void initialize() {
        paginacion = new PaginacionUtil<>(REGISTROS_POR_PAGINA);
        listaVideojuegos = paginacion.getDatosPaginados();
        configurarTabla();
        configurarComboFiltro();
        tblVideojuegos.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        tblVideojuegos.setOnMouseClicked(this::manejarDobleClicTabla);
        cargarVideojuegos();
        HBox controles = paginacion.crearControlesPaginacion();
        vboxContenedorPrinVideojuegos.getChildren().add(controles);
    }

    public void configurarPermisos() {
        SesionUsuario sesion = SesionUsuario.getInstancia();
        if (!sesion.esEspecialista()) {
            btnAnadirVideojuego.setDisable(true);
            btnAnadirVideojuego.setOpacity(0.5);
            btnAnadirVideojuego.setTooltip(new Tooltip("No tienes permisos para anadir videojuegos"));
            btnEditarVideojuego.setDisable(true);
            btnEditarVideojuego.setOpacity(0.5);
            btnEliminarVideojuego.setDisable(true);
            btnEliminarVideojuego.setOpacity(0.5);
        }
    }

    private void configurarTabla() {
        // Videojuego es un record Java — los accesores son codigo(), nombre(), etc.
        // PropertyValueFactory solo soporta JavaBeans (getX/isX), asi que usamos
        // lambdas para leer directamente del record y devolver Observable*.
        colCodigo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().codigo()));
        colNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().nombre()));
        colDiscapacidad.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().discapacidadNombre()));
        colParteCuerpo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().parteCuerpo()));
        colActivo.setCellValueFactory(c -> new SimpleBooleanProperty(c.getValue().activo()));
        // Codigo del videojuego → tipografia monoespaciada
        colCodigo.setCellFactory(TableUiUtil.monoCell());
        // Activo como badge semantico (true → "Activo" ok, false → "Inactivo" danger)
        colActivo.setCellFactory(TableUiUtil.badgeCell(
                v -> Boolean.TRUE.equals(v) ? "Activo" : "Inactivo",
                v -> Boolean.TRUE.equals(v) ? "ok" : "danger"));
        tblVideojuegos.setItems(listaVideojuegos);
    }

    private void configurarComboFiltro() {
        cmbFiltroDiscapacidad.setCellFactory(lv -> new ListCell<Object>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                if (item instanceof Discapacidad d)
                    setText(d.getNombreDis() + " (" + d.getCodDis() + ")");
                else
                    setText(item.toString());
            }
        });
        cmbFiltroDiscapacidad.setButtonCell(new ListCell<Object>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                if (item instanceof Discapacidad d)
                    setText(d.getNombreDis() + " (" + d.getCodDis() + ")");
                else
                    setText(item.toString());
            }
        });

        cmbFiltroDiscapacidad.getItems().add(TODAS);
        try {
            cmbFiltroDiscapacidad.getItems().addAll(catalogoService.listarDiscapacidades());
        } catch (RehabiAppException e) {
            System.err.println("No se pudo cargar el filtro de discapacidades: " + e.getMessage());
        }
        cmbFiltroDiscapacidad.getSelectionModel().selectFirst();
        cmbFiltroDiscapacidad.valueProperty().addListener((obs, o, n) -> aplicarFiltros());
    }

    private void cargarVideojuegos() {
        try {
            todosVideojuegos = videojuegoDAO.listarTodos();
            if (todosVideojuegos == null) todosVideojuegos = new ArrayList<>();
            aplicarFiltros();
        } catch (ConexionException e) {
            todosVideojuegos = new ArrayList<>();
            paginacion.setDatos(todosVideojuegos);
            VentanaUtil.mostrarVentanaInformativa(
                    "Error de conexion al cargar videojuegos.", TipoMensaje.ERROR);
        } catch (RehabiAppException e) {
            todosVideojuegos = new ArrayList<>();
            paginacion.setDatos(todosVideojuegos);
            VentanaUtil.mostrarVentanaInformativa(
                    "Error al cargar videojuegos: " + e.getMessage(), TipoMensaje.ERROR);
        } catch (Exception e) {
            todosVideojuegos = new ArrayList<>();
            paginacion.setDatos(todosVideojuegos);
            System.err.println("Error inesperado al cargar videojuegos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void aplicarFiltros() {
        String busqueda = txfBuscarVideojuegos != null
                ? txfBuscarVideojuegos.getText().trim().toLowerCase() : "";
        Object filtroSel = cmbFiltroDiscapacidad.getSelectionModel().getSelectedItem();
        String codDisFiltro = (filtroSel instanceof Discapacidad d) ? d.getCodDis() : null;

        List<Videojuego> filtrados = todosVideojuegos.stream()
                .filter(v -> busqueda.isEmpty()
                        || v.codigo().toLowerCase().contains(busqueda)
                        || v.nombre().toLowerCase().contains(busqueda)
                        || (v.discapacidadNombre() != null
                            && v.discapacidadNombre().toLowerCase().contains(busqueda)))
                .filter(v -> codDisFiltro == null
                        || codDisFiltro.equals(v.codDis()))
                .toList();
        paginacion.setDatos(filtrados);
    }

    @FXML
    void buscarVideojuegos(ActionEvent event) {
        aplicarFiltros();
    }

    @FXML
    void abrirFormularioNuevoVideojuego(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaAgregarVideojuego.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            controladorVentanaOpciones.aplicarConfiguracionAScene(scene);
            Stage stage = new Stage();
            stage.setTitle("Nuevo Videojuego");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.setMinWidth(520); // Spec §3.8: footer del modal nunca se corta
            VentanaUtil.establecerIconoVentana(stage);
            stage.showAndWait();
            cargarVideojuegos();
        } catch (Exception e) {
            System.err.println("Error al abrir formulario nuevo videojuego: " + e.getMessage());
            e.printStackTrace();
            VentanaUtil.mostrarVentanaInformativa(
                    "Error al abrir el formulario de nuevo videojuego.", TipoMensaje.ERROR);
        }
    }

    @FXML
    void editarVideojuegoSeleccionado(ActionEvent event) {
        Videojuego seleccionado = tblVideojuegos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Debe seleccionar un videojuego de la lista.", TipoMensaje.ADVERTENCIA);
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaAgregarVideojuego.fxml"));
            Parent root = loader.load();
            controladorAgregarVideojuego ctrl = loader.getController();
            ctrl.cargarDatosParaEdicion(seleccionado);
            Scene scene = new Scene(root);
            controladorVentanaOpciones.aplicarConfiguracionAScene(scene);
            Stage stage = new Stage();
            stage.setTitle("Editar Videojuego");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.setMinWidth(520); // Spec §3.8: footer del modal nunca se corta
            VentanaUtil.establecerIconoVentana(stage);
            stage.showAndWait();
            cargarVideojuegos();
        } catch (Exception e) {
            System.err.println("Error al abrir formulario de edicion: " + e.getMessage());
            e.printStackTrace();
            VentanaUtil.mostrarVentanaInformativa(
                    "Error al abrir la edicion del videojuego.", TipoMensaje.ERROR);
        }
    }

    @FXML
    void eliminarVideojuegoSeleccionado(ActionEvent event) {
        Videojuego seleccionado = tblVideojuegos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Debe seleccionar un videojuego para eliminar.", TipoMensaje.ADVERTENCIA);
            return;
        }
        boolean confirmado = VentanaUtil.mostrarVentanaPregunta(
                "¿Seguro que desea eliminar el videojuego '"
                + seleccionado.nombre() + "' (" + seleccionado.codigo() + ")?\n\n"
                + "El videojuego quedara inactivo y no aparecera en nuevos tratamientos.");
        if (!confirmado) return;
        try {
            videojuegoDAO.eliminar(seleccionado.idVideojuego());
            VentanaUtil.mostrarVentanaInformativa(
                    "Videojuego eliminado correctamente.", TipoMensaje.EXITO);
            cargarVideojuegos();
        } catch (DuplicadoException e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "No se puede eliminar: " + e.getMessage(), TipoMensaje.ADVERTENCIA);
        } catch (ConexionException e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Error de conexion con el servidor.", TipoMensaje.ERROR);
        } catch (RehabiAppException e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Error al eliminar: " + e.getMessage(), TipoMensaje.ERROR);
        }
    }

    private void manejarDobleClicTabla(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
            Videojuego seleccionado = tblVideojuegos.getSelectionModel().getSelectedItem();
            if (seleccionado != null) editarVideojuegoSeleccionado(null);
        }
    }
}
