package com.javafx.Interface;

import com.javafx.Clases.InformeService;
import com.javafx.Clases.Paciente;
import com.javafx.Clases.SesionUsuario;
import com.javafx.Clases.VentanaUtil;
import com.javafx.Clases.VentanaUtil.TipoMensaje;
import com.javafx.DAO.PacienteDAO;
import com.javafx.util.PaginacionUtil;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

/**
 * Controlador para la ventana principal de gestion de pacientes
 * Muestra la lista de pacientes y permite realizar operaciones CRUD
 */
public class controladorVentanaPacientes {

    @FXML
    private Button btnAbrirPaciente;

    @FXML
    private Button btnAnadirPaciente;

    @FXML
    private Button btnBuscarPacientes;

    @FXML
    private Button btnEliminarPaciente;

    @FXML
    private Button btnFiltrarPacientes;

    @FXML
    private Button btnGenerarPDFPaciente;

    @FXML
    private Button btnSeleccionarTodoPacientes;

    @FXML
    private TableColumn<Paciente, String> colApellidos;

    @FXML
    private TableColumn<Paciente, String> colDNI;

    @FXML
    private TableColumn<Paciente, String> colDiscapacidad;

    @FXML
    private TableColumn<Paciente, Integer> colEdad;

    @FXML
    private TableColumn<Paciente, String> colNombre;

    @FXML
    private TableColumn<Paciente, Integer> colProtesis;

    @FXML
    private Label lblBuscarPacientes;

    @FXML
    private Label lblTituloPestaniaPacientes;

    @FXML
    private TableView<Paciente> tblPacientes;

    @FXML
    private TextField txfBuscarPacientes;

    @FXML
    private VBox vboxContenedorPrinPacientes;

    //Lista observable de pacientes
    private ObservableList<Paciente> listaPacientes;

    //DAO para operaciones de base de datos
    private PacienteDAO pacienteDAO;

    //DNI del sanitario logueado
    private String dniSanitarioActual;

    // OPTIMIZACION: Paginacion para mejorar rendimiento
    private PaginacionUtil<Paciente> paginacion;
    private static final int REGISTROS_POR_PAGINA = 50; // Mostrar 50 pacientes por pagina
    private List<Paciente> todosPacientes; // Cache de todos los pacientes

    /**
     * Metodo initialize se ejecuta automaticamente al cargar el FXML
     */
    @FXML
    public void initialize() {
        //Inicializar DAO
        pacienteDAO = new PacienteDAO();

        //OPTIMIZACION: Inicializar paginacion
        paginacion = new PaginacionUtil<>(REGISTROS_POR_PAGINA);
        listaPacientes = paginacion.getDatosPaginados();

        //Configurar la tabla
        configurarTabla();

        //Permitir seleccion multiple
        tblPacientes.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        //Configurar doble clic para abrir ficha del paciente
        tblPacientes.setOnMouseClicked(this::manejarDobleClicTabla);

        //OPTIMIZACION: Agregar controles de paginacion
        HBox controlesPaginacion = paginacion.crearControlesPaginacion();
        vboxContenedorPrinPacientes.getChildren().add(controlesPaginacion);

        //Cargar pacientes automaticamente
        cargarPacientes();
    }

    /**
     * Maneja el evento de clic en la tabla de pacientes
     * Si es doble clic, abre la ficha del paciente
     */
    private void manejarDobleClicTabla(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
            Paciente pacienteSeleccionado = tblPacientes.getSelectionModel().getSelectedItem();
            if (pacienteSeleccionado != null) {
                abrirFichaPaciente(null);
            }
        }
    }

    /**
     * Configura los permisos de la ventana segun el usuario logueado
     * No especialistas (enfermeros): solo pueden ver pacientes (listar)
     * Especialistas: acceso completo (CRUD)
     */
    public void configurarPermisos() {
        SesionUsuario sesion = SesionUsuario.getInstancia();

        if (!sesion.esEspecialista()) {
            //Los no especialistas solo pueden LISTAR (leer)
            //Deshabilitar boton de añadir
            btnAnadirPaciente.setDisable(true);
            btnAnadirPaciente.setOpacity(0.5);
            btnAnadirPaciente.setTooltip(new javafx.scene.control.Tooltip(
                    "No tienes permisos para añadir pacientes"));

            //Deshabilitar boton de eliminar
            btnEliminarPaciente.setDisable(true);
            btnEliminarPaciente.setOpacity(0.5);
            btnEliminarPaciente.setTooltip(new javafx.scene.control.Tooltip(
                    "No tienes permisos para eliminar pacientes"));

            //Deshabilitar boton de abrir/editar (solo ver)
            btnAbrirPaciente.setText("Ver");

            System.out.println("Permisos de solo lectura aplicados para: " + sesion.getCargo());
        } else {
            System.out.println("Permisos completos aplicados para: " + sesion.getCargo());
        }

        //Establecer DNI del sanitario
        dniSanitarioActual = sesion.getDniUsuario();
    }

    /**
     * Realiza una busqueda de pacientes con el texto especificado
     * Llamado desde la ventana principal para busqueda rapida
     * @param textoBusqueda Texto a buscar
     */
    public void ejecutarBusqueda(String textoBusqueda) {
        if (textoBusqueda != null && !textoBusqueda.isEmpty()) {
            txfBuscarPacientes.setText(textoBusqueda);
            realizarBusqueda();
        }
    }

    /**
     * Realiza la busqueda con el texto del campo de busqueda
     */
    private void realizarBusqueda() {
        String texto = txfBuscarPacientes.getText().trim();

        if (texto.isEmpty()) {
            cargarPacientes();
            return;
        }

        listaPacientes.clear();
        List<Paciente> pacientesEncontrados = pacienteDAO.buscarPorTexto(texto);
        listaPacientes.addAll(pacientesEncontrados);
    }

    /**
     * Configura las columnas de la tabla
     */
    private void configurarTabla() {
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colApellidos.setCellValueFactory(new PropertyValueFactory<>("apellidos"));
        colDNI.setCellValueFactory(new PropertyValueFactory<>("dni"));
        colEdad.setCellValueFactory(new PropertyValueFactory<>("edad"));
        colDiscapacidad.setCellValueFactory(new PropertyValueFactory<>("discapacidad"));
        colProtesis.setCellValueFactory(new PropertyValueFactory<>("protesis"));

        //Asignar la lista observable a la tabla
        tblPacientes.setItems(listaPacientes);
    }

    /**
     * Establece el DNI del sanitario logueado y carga los pacientes
     * @param dniSanitario DNI del sanitario
     */
    public void setDniSanitario(String dniSanitario) {
        this.dniSanitarioActual = dniSanitario;
        cargarPacientes();
    }

    /**
     * Carga todos los pacientes desde la base de datos
     */
    private void cargarPacientes() {
        // OPTIMIZACION: Cargar todos los pacientes y usar paginacion
        todosPacientes = pacienteDAO.listarTodos();
        paginacion.setDatos(todosPacientes);

        System.out.println("Pacientes cargados: " + todosPacientes.size() +
                           " (mostrando " + listaPacientes.size() + " por pagina)");
    }

    /**
     * Abre la ficha de informacion del paciente seleccionado
     */
    @FXML
    void abrirFichaPaciente(ActionEvent event) {
        Paciente pacienteSeleccionado = tblPacientes.getSelectionModel().getSelectedItem();

        if (pacienteSeleccionado == null) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Debe seleccionar un paciente de la lista.",
                    TipoMensaje.ADVERTENCIA
            );
            return;
        }

        try {
            //Ruta absoluta desde resources con / al principio
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaListarPaciente.fxml"));
            Parent root = loader.load();

            //Obtener el controlador y cargar los datos del paciente
            controladorVentanaPacienteListar controlador = loader.getController();
            controlador.cargarDatosPaciente(pacienteSeleccionado.getDni());

            //Configurar modo solo lectura para no especialistas
            SesionUsuario sesion = SesionUsuario.getInstancia();
            if (!sesion.esEspecialista()) {
                controlador.setModoSoloLectura(true);
            }

            //Crear escena y aplicar CSS
            Scene scene = new Scene(root);
            controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

            //Crear y mostrar la ventana modal
            Stage stage = new Stage();
            stage.setTitle("Ficha del Paciente");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            VentanaUtil.establecerIconoVentana(stage);
            stage.showAndWait();

            //Recargar la lista si hubo cambios
            if (controlador.hayCambiosRealizados()) {
                cargarPacientes();
            }

        } catch (Exception e) {
            System.err.println("Error al abrir ficha del paciente: " + e.getMessage());
            e.printStackTrace();
            VentanaUtil.mostrarVentanaInformativa(
                    "Error al abrir la ficha del paciente.",
                    TipoMensaje.ERROR
            );
        }
    }

    /**
     * Abre el formulario para crear un nuevo paciente
     */
    @FXML
    void abrirFormularioNuevoPaciente(ActionEvent event) {
        try {
            //Ruta absoluta desde resources con / al principio
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaAgregarPaciente.fxml"));
            Parent root = loader.load();

            //Obtener el controlador y pasar el DNI del sanitario
            controladorAgregarPaciente controlador = loader.getController();
            controlador.setDniSanitario(dniSanitarioActual);

            //Crear escena y aplicar CSS
            Scene scene = new Scene(root);
            controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

            //Crear y mostrar la ventana modal
            Stage stage = new Stage();
            stage.setTitle("Nuevo Paciente");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            VentanaUtil.establecerIconoVentana(stage);
            stage.showAndWait();

            //Recargar la lista de pacientes
            cargarPacientes();

        } catch (Exception e) {
            System.err.println("Error al abrir formulario de nuevo paciente: " + e.getMessage());
            e.printStackTrace();
            VentanaUtil.mostrarVentanaInformativa(
                    "Error al abrir el formulario de nuevo paciente.",
                    TipoMensaje.ERROR
            );
        }
    }

    /**
     * Abre la ventana de filtros avanzados para pacientes
     */
    @FXML
    void abrirFiltrosPacientes(ActionEvent event) {
        try {
            //Cargar la ventana de filtros
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaFiltroPacientes.fxml"));
            Parent root = loader.load();

            //Obtener el controlador de filtros
            controladorFiltroPacientes controlador = loader.getController();

            //Crear escena y aplicar CSS
            Scene scene = new Scene(root);
            controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

            //Crear y mostrar la ventana modal
            Stage stage = new Stage();
            stage.setTitle("Filtrar Pacientes");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            VentanaUtil.establecerIconoVentana(stage);
            stage.showAndWait();

            //Si se aplicaron filtros, filtrar la tabla
            if (controlador.seFiltrosAplicados()) {
                aplicarFiltros(controlador.getFiltros());
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
     * Aplica los filtros seleccionados a la lista de pacientes
     * @param filtros Objeto con los criterios de filtrado
     */
    private void aplicarFiltros(controladorFiltroPacientes.FiltrosPaciente filtros) {
        //Obtener todos los pacientes
        List<Paciente> todosLosPacientes = pacienteDAO.listarTodos();

        //Filtrar segun los criterios
        List<Paciente> pacientesFiltrados = todosLosPacientes.stream()
                .filter(p -> {
                    //Filtro por protesis
                    if (filtros.isConProtesis() && !filtros.isSinProtesis()) {
                        return p.tieneProtesis();
                    }
                    if (filtros.isSinProtesis() && !filtros.isConProtesis()) {
                        return !p.tieneProtesis();
                    }
                    return true;
                })
                .filter(p -> {
                    //Filtro por edad
                    if (filtros.isFiltrarEdad()) {
                        int edad = p.getEdad();
                        return edad >= filtros.getEdadMinima() && edad <= filtros.getEdadMaxima();
                    }
                    return true;
                })
                .sorted((p1, p2) -> {
                    //Ordenacion
                    int resultado = 0;
                    switch (filtros.getOrdenarPor()) {
                        case "Nombre":
                            resultado = p1.getNombre().compareToIgnoreCase(p2.getNombre());
                            break;
                        case "Apellidos":
                            resultado = p1.getApellidos().compareToIgnoreCase(p2.getApellidos());
                            break;
                        case "DNI":
                            resultado = p1.getDni().compareToIgnoreCase(p2.getDni());
                            break;
                        case "Edad":
                            resultado = Integer.compare(p1.getEdad(), p2.getEdad());
                            break;
                        case "Discapacidad":
                            String disc1 = p1.getDiscapacidad() != null ? p1.getDiscapacidad() : "";
                            String disc2 = p2.getDiscapacidad() != null ? p2.getDiscapacidad() : "";
                            resultado = disc1.compareToIgnoreCase(disc2);
                            break;
                    }
                    return filtros.isOrdenAscendente() ? resultado : -resultado;
                })
                .toList();

        //Actualizar la tabla
        listaPacientes.clear();
        listaPacientes.addAll(pacientesFiltrados);
    }

    /**
     * Busca pacientes por texto
     */
    @FXML
    void buscarPacientes(ActionEvent event) {
        String textoBusqueda = txfBuscarPacientes.getText().trim();

        listaPacientes.clear();

        if (textoBusqueda.isEmpty()) {
            //Si no hay texto, mostrar todos
            List<Paciente> pacientes = pacienteDAO.listarTodos();
            listaPacientes.addAll(pacientes);
        } else {
            //Buscar por texto
            List<Paciente> pacientes = pacienteDAO.buscarPorTexto(textoBusqueda);
            listaPacientes.addAll(pacientes);
        }
    }

    /**
     * Elimina el paciente seleccionado
     */
    @FXML
    void eliminarPacienteSeleccionado(ActionEvent event) {
        Paciente pacienteSeleccionado = tblPacientes.getSelectionModel().getSelectedItem();

        if (pacienteSeleccionado == null) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Debe seleccionar un paciente para eliminar.",
                    TipoMensaje.ADVERTENCIA
            );
            return;
        }

        //Mostrar ventana de confirmacion
        String mensaje = "¿Esta seguro de que desea eliminar al paciente " +
                pacienteSeleccionado.getNombreCompleto() + " (" + pacienteSeleccionado.getDni() + ")?\n\n" +
                "Esta accion eliminara tambien sus citas y telefonos asociados.";

        boolean confirmado = VentanaUtil.mostrarVentanaPregunta(mensaje);

        if (confirmado) {
            boolean eliminado = pacienteDAO.eliminar(pacienteSeleccionado.getDni());

            if (eliminado) {
                VentanaUtil.mostrarVentanaInformativa(
                        "El paciente ha sido eliminado correctamente.",
                        TipoMensaje.EXITO
                );
                cargarPacientes();
            } else {
                VentanaUtil.mostrarVentanaInformativa(
                        "No se pudo eliminar el paciente.",
                        TipoMensaje.ERROR
                );
            }
        }
    }

    /**
     * Selecciona todos los pacientes de la tabla
     */
    @FXML
    void seleccionarTodosPacientes(ActionEvent event) {
        tblPacientes.getSelectionModel().selectAll();
    }

    /**
     * Genera un informe PDF del paciente seleccionado
     * El informe se guarda en la carpeta 'informes' y se abre automáticamente
     * Además, se permite al usuario guardar una copia en una ubicación personalizada
     */
    @FXML
    void generarInformePaciente(ActionEvent event) {
        // Obtener el paciente seleccionado
        Paciente pacienteSeleccionado = tblPacientes.getSelectionModel().getSelectedItem();

        // Validar que hay un paciente seleccionado
        if (pacienteSeleccionado == null) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Debe seleccionar un paciente de la lista para generar el informe.",
                    TipoMensaje.ADVERTENCIA
            );
            return;
        }

        try {
            // Obtener el DNI del paciente
            String dniPaciente = pacienteSeleccionado.getDni();

            System.out.println("Generando informe para paciente con DNI: " + dniPaciente);

            // Generar el informe con copia personalizada usando InformeService
            // Esto generará el PDF en 'informes', lo abrirá, y preguntará dónde guardar copia adicional
            boolean exito = InformeService.generarInformePacienteConCopiaPersonalizada(dniPaciente);

            if (!exito) {
                VentanaUtil.mostrarVentanaInformativa(
                        "No se pudo generar el informe PDF.\n" +
                        "Por favor, revise la consola para más detalles.",
                        TipoMensaje.ERROR
                );
            }

        } catch (Exception e) {
            System.err.println("Error al generar el informe: " + e.getMessage());
            e.printStackTrace();
            VentanaUtil.mostrarVentanaInformativa(
                    "Error inesperado al generar el informe.\n" +
                    "Detalles: " + e.getMessage(),
                    TipoMensaje.ERROR
            );
        }
    }
}
