package com.javafx.Interface;

import com.javafx.Clases.Discapacidad;
import com.javafx.Clases.NivelProgresion;
import com.javafx.Clases.SesionUsuario;
import com.javafx.Clases.Tratamiento;
import com.javafx.Clases.VentanaUtil;
import com.javafx.Clases.VentanaUtil.TipoMensaje;
import com.javafx.excepcion.ConexionException;
import com.javafx.excepcion.DuplicadoException;
import com.javafx.excepcion.RehabiAppException;
import com.javafx.service.CatalogoService;
import com.javafx.util.PaginacionUtil;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador para la ventana de gestion de tratamientos del catalogo clinico.
 * Permite listar, buscar, filtrar por discapacidad y nivel de progresion,
 * crear, editar y eliminar tratamientos.
 * Solo accesible para SPECIALIST (enfermeros no ven esta pestana).
 */
public class controladorVentanaTratamientos {

    @FXML
    private Button btnAnadirTratamiento;

    @FXML
    private Button btnBuscarTratamientos;

    @FXML
    private Button btnEditarTratamiento;

    @FXML
    private Button btnEliminarTratamiento;

    @FXML
    private Button btnSeleccionarTodoTratamientos;

    @FXML
    private Button btnFiltrarTratamientos;

    @FXML
    private TableColumn<Tratamiento, String> colCodigo;

    @FXML
    private TableColumn<Tratamiento, String> colNombre;

    @FXML
    private TableColumn<Tratamiento, String> colDefinicion;

    @FXML
    private TableColumn<Tratamiento, String> colDiscapacidad;

    @FXML
    private TableColumn<Tratamiento, String> colNivel;

    @FXML
    private Label lblTituloPestaniaTratamientos;

    @FXML
    private TableView<Tratamiento> tblTratamientos;

    @FXML
    private TextField txfBuscarTratamientos;

    @FXML
    private VBox vboxContenedorPrinTratamientos;

    // Lista observable de tratamientos
    private ObservableList<Tratamiento> listaTratamientos;

    // Servicio del catalogo
    private CatalogoService catalogoService;

    // Cache de todos los tratamientos
    private List<Tratamiento> todosTratamientos;

    // Estado actual de los filtros del popup
    private Discapacidad filtroDiscapacidad = null;
    private NivelProgresion filtroNivel = null;
    private String filtroOrden = "Nombre A-Z";

    // Paginacion
    private PaginacionUtil<Tratamiento> paginacion;
    private static final int REGISTROS_POR_PAGINA = 50;

    /**
     * Metodo initialize se ejecuta automaticamente al cargar el FXML
     */
    @FXML
    public void initialize() {
        catalogoService = new CatalogoService();
        paginacion = new PaginacionUtil<>(REGISTROS_POR_PAGINA);
        listaTratamientos = paginacion.getDatosPaginados();

        configurarTabla();

        // Permitir seleccion multiple
        tblTratamientos.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Doble clic para editar
        tblTratamientos.setOnMouseClicked(this::manejarDobleClicTabla);

        cargarTratamientos();

        // Agregar controles de paginacion al contenedor principal
        HBox controlesPaginacion = paginacion.crearControlesPaginacion();
        vboxContenedorPrinTratamientos.getChildren().add(controlesPaginacion);
    }

    /**
     * Configura las columnas de la tabla de tratamientos
     */
    private void configurarTabla() {
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codTrat"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombreTrat"));
        colDefinicion.setCellValueFactory(new PropertyValueFactory<>("definicionTrat"));
        colDiscapacidad.setCellValueFactory(new PropertyValueFactory<>("discapacidadesAsociadas"));
        colNivel.setCellValueFactory(new PropertyValueFactory<>("nombreNivel"));

        tblTratamientos.setItems(listaTratamientos);
    }

    /**
     * Carga todos los tratamientos desde el servicio del catalogo
     */
    private void cargarTratamientos() {
        try {
            todosTratamientos = catalogoService.listarTratamientos();

            // Enriquecer cada tratamiento con sus discapacidades asociadas
            for (Tratamiento t : todosTratamientos) {
                try {
                    List<Discapacidad> vinculadas = catalogoService.listarDiscapacidadesDeTratamiento(t.getCodTrat());
                    String nombres = vinculadas.stream()
                            .map(Discapacidad::getNombreDis)
                            .collect(Collectors.joining(", "));
                    t.setDiscapacidadesAsociadas(nombres);
                } catch (Exception e) {
                    // Si falla obtener discapacidades de un tratamiento concreto, no bloquear la carga
                    t.setDiscapacidadesAsociadas("-");
                    System.err.println("No se pudieron cargar discapacidades para " + t.getCodTrat() + ": " + e.getMessage());
                }
            }

            paginacion.setDatos(todosTratamientos);
            System.out.println("Tratamientos cargados: " + todosTratamientos.size());
        } catch (ConexionException e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Error de conexion al cargar tratamientos.",
                    TipoMensaje.ERROR
            );
        } catch (Exception e) {
            System.err.println("Error al cargar tratamientos: " + e.getMessage());
            e.printStackTrace();
            VentanaUtil.mostrarVentanaInformativa(
                    "Error al cargar los tratamientos: " + e.getMessage(),
                    TipoMensaje.ERROR
            );
        }
    }

    /**
     * Busca tratamientos aplicando todos los filtros activos (texto, discapacidad, nivel)
     */
    @FXML
    void buscarTratamientos(ActionEvent event) {
        aplicarFiltros();
    }

    /**
     * Aplica los filtros combinados: texto de busqueda, discapacidad y nivel de progresion.
     * El filtrado se realiza localmente sobre la lista completa ya cargada (todosTratamientos),
     * usando el campo discapacidadesAsociadas (cadena de nombres separados por coma) para
     * filtrar por discapacidad sin necesidad de llamar a la API.
     */
    private void aplicarFiltros() {
        try {
            if (todosTratamientos == null) return;

            String textoBusqueda = txfBuscarTratamientos.getText().trim().toLowerCase();

            List<Tratamiento> filtrados = todosTratamientos.stream()
                    .filter(t -> {
                        // Filtro por discapacidad: comparar contra el campo discapacidadesAsociadas
                        if (filtroDiscapacidad != null) {
                            String asociadas = t.getDiscapacidadesAsociadas();
                            if (asociadas == null || asociadas.isBlank() || asociadas.equals("-")) {
                                return false;
                            }
                            String nombreBuscado = filtroDiscapacidad.getNombreDis().toLowerCase();
                            boolean coincide = false;
                            for (String nombre : asociadas.split(",")) {
                                if (nombre.trim().toLowerCase().equals(nombreBuscado)) {
                                    coincide = true;
                                    break;
                                }
                            }
                            if (!coincide) return false;
                        }
                        // Filtro por nivel de progresion
                        if (filtroNivel != null && t.getIdNivel() != filtroNivel.getIdNivel()) {
                            return false;
                        }
                        // Filtro por texto en codigo o nombre
                        if (!textoBusqueda.isEmpty()) {
                            return t.getCodTrat().toLowerCase().contains(textoBusqueda)
                                    || t.getNombreTrat().toLowerCase().contains(textoBusqueda);
                        }
                        return true;
                    })
                    .sorted((t1, t2) -> {
                        // Ordenacion segun criterio del popup
                        return switch (filtroOrden) {
                            case "Nombre Z-A" -> t2.getNombreTrat().compareToIgnoreCase(t1.getNombreTrat());
                            case "Codigo" -> t1.getCodTrat().compareToIgnoreCase(t2.getCodTrat());
                            default -> t1.getNombreTrat().compareToIgnoreCase(t2.getNombreTrat()); // "Nombre A-Z"
                        };
                    })
                    .collect(Collectors.toList());

            paginacion.setDatos(filtrados);
        } catch (Exception e) {
            System.err.println("Error al aplicar filtros: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Abre la ventana popup de filtros avanzados de tratamientos.
     * Al cerrar con "Aplicar", actualiza los filtros de instancia y reaplica.
     */
    @FXML
    void abrirFiltrosTratamientos(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaFiltroTratamientos.fxml"));
            Parent root = loader.load();

            controladorFiltroTratamientos controlador = loader.getController();

            Scene scene = new Scene(root);
            controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

            Stage stage = new Stage();
            stage.setTitle("Filtrar Tratamientos");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            VentanaUtil.establecerIconoVentana(stage);
            stage.showAndWait();

            if (controlador.isFiltrosAplicados()) {
                filtroDiscapacidad = controlador.getDiscapacidadSeleccionada();
                filtroNivel = controlador.getNivelSeleccionado();
                filtroOrden = controlador.getOrden() != null ? controlador.getOrden() : "Nombre A-Z";
                aplicarFiltros();
            }

        } catch (Exception e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "No se pudo abrir la ventana de filtros.",
                    TipoMensaje.ERROR
            );
            System.err.println("Error al abrir ventana de filtros: " + e.getMessage());
            e.printStackTrace();
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
            btnAnadirTratamiento.setDisable(true);
            btnAnadirTratamiento.setOpacity(0.5);
            btnAnadirTratamiento.setTooltip(new Tooltip(
                    "No tienes permisos para anadir tratamientos"));

            // Deshabilitar boton de eliminar
            btnEliminarTratamiento.setDisable(true);
            btnEliminarTratamiento.setOpacity(0.5);
            btnEliminarTratamiento.setTooltip(new Tooltip(
                    "No tienes permisos para eliminar tratamientos"));

            // Deshabilitar boton de editar
            btnEditarTratamiento.setDisable(true);
            btnEditarTratamiento.setOpacity(0.5);
            btnEditarTratamiento.setTooltip(new Tooltip(
                    "No tienes permisos para editar tratamientos"));

            System.out.println("Permisos de solo lectura aplicados para tratamientos: " + sesion.getCargo());
        } else {
            System.out.println("Permisos completos aplicados para tratamientos: " + sesion.getCargo());
        }
    }

    /**
     * Abre el formulario para crear un nuevo tratamiento
     */
    @FXML
    void abrirFormularioNuevoTratamiento(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaAgregarTratamiento.fxml"));
            Parent root = loader.load();

            controladorAgregarTratamiento controlador = loader.getController();

            Scene scene = new Scene(root);
            controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

            Stage stage = new Stage();
            stage.setTitle("Nuevo Tratamiento");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            VentanaUtil.establecerIconoVentana(stage);
            stage.showAndWait();

            // Solo recargar si el usuario guardo algo; evita popup de error al cancelar
            if (controlador.isGuardadoExitoso()) {
                try {
                    cargarTratamientos();
                    aplicarFiltros();
                } catch (Exception e) {
                    System.err.println("Error al recargar datos: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("Error al abrir formulario de nuevo tratamiento: " + e.getMessage());
            e.printStackTrace();
            VentanaUtil.mostrarVentanaInformativa(
                    "Error al abrir el formulario de nuevo tratamiento.",
                    TipoMensaje.ERROR
            );
        }
    }

    /**
     * Abre el formulario de edicion para el tratamiento seleccionado
     */
    @FXML
    void editarTratamientoSeleccionado(ActionEvent event) {
        Tratamiento seleccionado = tblTratamientos.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Debe seleccionar un tratamiento de la lista.",
                    TipoMensaje.ADVERTENCIA
            );
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaAgregarTratamiento.fxml"));
            Parent root = loader.load();

            controladorAgregarTratamiento controlador = loader.getController();
            controlador.cargarDatosParaEdicion(seleccionado);

            Scene scene = new Scene(root);
            controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

            Stage stage = new Stage();
            stage.setTitle("Editar Tratamiento");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            VentanaUtil.establecerIconoVentana(stage);
            stage.showAndWait();

            // Solo recargar si el usuario guardo algo; evita popup de error al cancelar
            if (controlador.isGuardadoExitoso()) {
                try {
                    cargarTratamientos();
                    aplicarFiltros();
                } catch (Exception e) {
                    System.err.println("Error al recargar datos: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("Error al abrir formulario de edicion: " + e.getMessage());
            e.printStackTrace();
            VentanaUtil.mostrarVentanaInformativa(
                    "Error al abrir la edicion del tratamiento.",
                    TipoMensaje.ERROR
            );
        }
    }

    /**
     * Elimina el tratamiento seleccionado con confirmacion previa.
     * La API rechaza con 409 si hay pacientes asignados (DuplicadoException).
     */
    @FXML
    void eliminarTratamientoSeleccionado(ActionEvent event) {
        Tratamiento seleccionado = tblTratamientos.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Debe seleccionar un tratamiento para eliminar.",
                    TipoMensaje.ADVERTENCIA
            );
            return;
        }

        String mensaje = "¿Esta seguro de que desea eliminar el tratamiento '"
                + seleccionado.getNombreTrat() + "' (" + seleccionado.getCodTrat() + ")?\n\n"
                + "Se eliminaran tambien sus vinculos con discapacidades.";

        boolean confirmado = VentanaUtil.mostrarVentanaPregunta(mensaje);

        if (confirmado) {
            try {
                catalogoService.eliminarTratamiento(seleccionado.getCodTrat());
                VentanaUtil.mostrarVentanaInformativa(
                        "El tratamiento ha sido eliminado correctamente.",
                        TipoMensaje.EXITO
                );
                cargarTratamientos();
                aplicarFiltros();

            } catch (DuplicadoException e) {
                // 409: hay pacientes asignados a este tratamiento
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
     * Selecciona todos los tratamientos de la tabla
     */
    @FXML
    void seleccionarTodosTratamientos(ActionEvent event) {
        tblTratamientos.getSelectionModel().selectAll();
    }

    /**
     * Maneja el evento de doble clic en la tabla.
     * Si es doble clic, abre el formulario de edicion.
     */
    private void manejarDobleClicTabla(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
            Tratamiento seleccionado = tblTratamientos.getSelectionModel().getSelectedItem();
            if (seleccionado != null) {
                editarTratamientoSeleccionado(null);
            }
        }
    }
}
