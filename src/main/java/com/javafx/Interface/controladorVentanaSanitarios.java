package com.javafx.Interface;

import com.javafx.Clases.Sanitario;
import com.javafx.Clases.SesionUsuario;
import com.javafx.Clases.VentanaUtil;
import com.javafx.Clases.VentanaUtil.TipoMensaje;
import com.javafx.DAO.SanitarioDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

/**
 * Controlador para la ventana de gestion de sanitarios
 * Permite listar, buscar, agregar, abrir y eliminar sanitarios
 * Solo accesible para usuarios con cargo Especialista
 */
public class controladorVentanaSanitarios {

    @FXML
    private Button btnAbrirSanitario;

    @FXML
    private Button btnAnadirSanitario;

    @FXML
    private Button btnBuscarSanitarios;

    @FXML
    private Button btnEliminarSanitario;

    @FXML
    private Button btnFiltrarSanitarios;

    @FXML
    private Button btnSeleccionarTodo;

    @FXML
    private TableColumn<Sanitario, String> colApellidos;

    @FXML
    private TableColumn<Sanitario, String> colCargo;

    @FXML
    private TableColumn<Sanitario, String> colDNI;

    @FXML
    private TableColumn<Sanitario, String> colNombre;

    @FXML
    private Label lblBuscarSanitarios;

    @FXML
    private Label lblTituloPestania;

    @FXML
    private TableView<Sanitario> tblSanitarios;

    @FXML
    private TextField txfBuscarSanitarios;

    @FXML
    private VBox vboxContenedorPrincipal;

    //Lista observable para la tabla
    private ObservableList<Sanitario> listaSanitarios;

    //DAO para operaciones de base de datos
    private SanitarioDAO sanitarioDAO;

    /**
     * Metodo initialize se ejecuta automaticamente al cargar el FXML
     */
    @FXML
    public void initialize() {
        //Inicializar DAO
        sanitarioDAO = new SanitarioDAO();

        //Configurar columnas de la tabla
        configurarTabla();

        //Cargar datos de la base de datos
        cargarSanitarios();
    }

    /**
     * Configura los permisos de la ventana segun el usuario logueado
     * Solo los especialistas tienen acceso completo a esta ventana
     * Los no especialistas no deberian poder acceder aqui
     */
    public void configurarPermisos() {
        SesionUsuario sesion = SesionUsuario.getInstancia();

        if (!sesion.esEspecialista()) {
            //Los no especialistas no tienen acceso a gestion de sanitarios
            //Deshabilitar todos los botones
            btnAnadirSanitario.setDisable(true);
            btnAnadirSanitario.setOpacity(0.5);

            btnEliminarSanitario.setDisable(true);
            btnEliminarSanitario.setOpacity(0.5);

            btnAbrirSanitario.setDisable(true);
            btnAbrirSanitario.setOpacity(0.5);

            VentanaUtil.mostrarVentanaInformativa(
                    "No tienes permisos para gestionar sanitarios.\n" +
                            "Solo los medicos especialistas pueden acceder a esta funcion.",
                    TipoMensaje.ADVERTENCIA
            );

            System.out.println("Acceso denegado a gestion de sanitarios para: " + sesion.getCargo());
        } else {
            System.out.println("Acceso completo a gestion de sanitarios para: " + sesion.getCargo());
        }
    }

    /**
     * Configura las columnas de la tabla vinculandolas con las propiedades del objeto Sanitario
     */
    private void configurarTabla() {
        //Vincular columnas con propiedades del objeto Sanitario
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colApellidos.setCellValueFactory(new PropertyValueFactory<>("apellidos"));
        colDNI.setCellValueFactory(new PropertyValueFactory<>("dni"));
        colCargo.setCellValueFactory(new PropertyValueFactory<>("cargo"));

        //Habilitar seleccion multiple
        tblSanitarios.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
    }

    /**
     * Carga todos los sanitarios de la base de datos en la tabla
     */
    private void cargarSanitarios() {
        //Obtener lista de sanitarios desde la base de datos
        List<Sanitario> sanitarios = sanitarioDAO.listarTodos();

        //Convertir a ObservableList y asignar a la tabla
        listaSanitarios = FXCollections.observableArrayList(sanitarios);
        tblSanitarios.setItems(listaSanitarios);
    }

    /**
     * Abre la ficha de informacion del sanitario seleccionado
     * @param event Evento del boton
     */
    @FXML
    void abrirFichaSanitario(ActionEvent event) {
        //Obtener sanitario seleccionado en la tabla
        Sanitario sanitarioSeleccionado = tblSanitarios.getSelectionModel().getSelectedItem();

        //Verificar que haya un sanitario seleccionado
        if (sanitarioSeleccionado == null) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Selecciona un sanitario de la tabla para ver su informacion.",
                    TipoMensaje.ADVERTENCIA
            );
            return;
        }

        try {
            //Cargar la ventana de ficha del sanitario
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaSanitarioListar.fxml"));
            Parent root = loader.load();

            //Obtener el controlador y pasarle el DNI del sanitario
            controladorVentanaSanitarioListar controlador = loader.getController();
            controlador.cargarDatosSanitario(sanitarioSeleccionado.getDni());

            //Crear y mostrar la ventana modal
            Stage stage = new Stage();
            stage.setTitle("Informacion del sanitario");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

            //Refrescar la tabla por si se modifico algo
            cargarSanitarios();

        } catch (Exception e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "No se pudo abrir la ficha del sanitario.",
                    TipoMensaje.ERROR
            );
            System.err.println("Error al abrir ficha del sanitario: " + e.getMessage());
        }
    }

    /**
     * Abre la ventana de filtros de sanitarios
     * @param event Evento del boton
     */
    @FXML
    void abrirFiltrosSanitarios(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaFiltroSanitarios.fxml"));
            Parent root = loader.load();

            controladorFiltroSanitarios controlador = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Filtrar Sanitarios");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

            //Si se aplicaron filtros, filtrar la lista
            if (controlador.seFiltrosAplicados()) {
                aplicarFiltros(controlador.getFiltros());
            }

        } catch (Exception e) {
            System.err.println("Error al abrir ventana de filtros: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Aplica los filtros seleccionados a la lista de sanitarios
     * @param filtros Objeto con los criterios de filtrado
     */
    private void aplicarFiltros(controladorFiltroSanitarios.FiltrosSanitario filtros) {
        //Obtener todos los sanitarios
        List<Sanitario> todosLosSanitarios = sanitarioDAO.listarTodos();

        //Filtrar segun criterios
        List<Sanitario> sanitariosFiltrados = todosLosSanitarios.stream()
                //Filtro de cargo
                .filter(s -> {
                    if (filtros.isSoloEspecialistas() && !filtros.isSoloEnfermeros()) {
                        return s.getCargo() != null && s.getCargo().toLowerCase().contains("especialista");
                    } else if (filtros.isSoloEnfermeros() && !filtros.isSoloEspecialistas()) {
                        return s.getCargo() != null && s.getCargo().toLowerCase().contains("enfermero");
                    }
                    return true; //Si ambos o ninguno, mostrar todos
                })
                //Filtro de pacientes asignados
                .filter(s -> {
                    if (filtros.isConPacientes() && !filtros.isSinPacientes()) {
                        return s.getNumPacientes() > 0;
                    } else if (filtros.isSinPacientes() && !filtros.isConPacientes()) {
                        return s.getNumPacientes() == 0;
                    }
                    return true;
                })
                .collect(java.util.stream.Collectors.toList());

        //Ordenar segun criterio
        java.util.Comparator<Sanitario> comparador = obtenerComparadorSanitarios(filtros.getOrdenarPor());
        if (!filtros.isOrdenAscendente()) {
            comparador = comparador.reversed();
        }
        sanitariosFiltrados.sort(comparador);

        //Actualizar lista
        listaSanitarios = FXCollections.observableArrayList(sanitariosFiltrados);
        tblSanitarios.setItems(listaSanitarios);

        System.out.println("Filtros aplicados. Sanitarios mostrados: " + sanitariosFiltrados.size());
    }

    /**
     * Obtiene el comparador segun el campo de ordenacion
     * @param campo Campo por el que ordenar
     * @return Comparador correspondiente
     */
    private java.util.Comparator<Sanitario> obtenerComparadorSanitarios(String campo) {
        switch (campo) {
            case "Apellidos":
                return java.util.Comparator.comparing(Sanitario::getApellidos, String.CASE_INSENSITIVE_ORDER);
            case "DNI":
                return java.util.Comparator.comparing(Sanitario::getDni, String.CASE_INSENSITIVE_ORDER);
            case "Cargo":
                return java.util.Comparator.comparing(s -> s.getCargo() != null ? s.getCargo() : "", String.CASE_INSENSITIVE_ORDER);
            case "Num. Pacientes":
                return java.util.Comparator.comparingInt(Sanitario::getNumPacientes);
            case "Nombre":
            default:
                return java.util.Comparator.comparing(Sanitario::getNombre, String.CASE_INSENSITIVE_ORDER);
        }
    }

    /**
     * Busca sanitarios por texto en el campo de busqueda
     * @param event Evento del campo o boton
     */
    private void buscarSanitariosPorTexto() {
        String textoBusqueda = txfBuscarSanitarios.getText().trim();

        if (textoBusqueda.isEmpty()) {
            cargarSanitarios();
        } else {
            List<Sanitario> sanitariosFiltrados = sanitarioDAO.buscarPorTexto(textoBusqueda);
            listaSanitarios = FXCollections.observableArrayList(sanitariosFiltrados);
            tblSanitarios.setItems(listaSanitarios);
        }
    }

    /**
     * Abre el formulario para agregar un nuevo sanitario
     * @param event Evento del boton
     */
    @FXML
    void abrirFormularioNuevoSanitario(ActionEvent event) {
        try {
            //Cargar la ventana de agregar sanitario
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaAgregarSanitario.fxml"));
            Parent root = loader.load();

            //Crear y mostrar la ventana modal
            Stage stage = new Stage();
            stage.setTitle("Nuevo Sanitario");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

            //Refrescar la tabla despues de agregar
            cargarSanitarios();

        } catch (Exception e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "No se pudo abrir el formulario de nuevo sanitario.",
                    TipoMensaje.ERROR
            );
            System.err.println("Error al abrir formulario de nuevo sanitario: " + e.getMessage());
        }
    }

    /**
     * Elimina el sanitario seleccionado previa confirmacion
     * @param event Evento del boton
     */
    @FXML
    void eliminarSanitarioSeleccionado(ActionEvent event) {
        //Obtener sanitario seleccionado en la tabla
        Sanitario sanitarioSeleccionado = tblSanitarios.getSelectionModel().getSelectedItem();

        //Verificar que haya un sanitario seleccionado
        if (sanitarioSeleccionado == null) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Selecciona un sanitario de la tabla para eliminarlo.",
                    TipoMensaje.ADVERTENCIA
            );
            return;
        }

        //Verificar si el sanitario tiene pacientes asignados
        if (sanitarioSeleccionado.getNumPacientes() > 0) {
            VentanaUtil.mostrarVentanaInformativa(
                    "No se puede eliminar el sanitario porque tiene " +
                            sanitarioSeleccionado.getNumPacientes() + " paciente(s) asignado(s).\n\n" +
                            "Primero debe reasignar los pacientes a otro sanitario.",
                    TipoMensaje.ERROR
            );
            return;
        }

        //Mostrar ventana de confirmacion
        String mensajeConfirmacion = "¿Estas seguro de que deseas eliminar al sanitario?\n\n" +
                "DNI: " + sanitarioSeleccionado.getDni() + "\n" +
                "Nombre: " + sanitarioSeleccionado.getNombreCompleto() + "\n\n" +
                "Esta accion no se puede deshacer.";

        boolean confirmado = VentanaUtil.mostrarVentanaPregunta(mensajeConfirmacion);

        if (confirmado) {
            //Proceder con la eliminacion
            boolean eliminado = sanitarioDAO.eliminar(sanitarioSeleccionado.getDni());

            if (eliminado) {
                VentanaUtil.mostrarVentanaInformativa(
                        "El sanitario se ha eliminado correctamente.",
                        TipoMensaje.EXITO
                );

                //Refrescar la tabla
                cargarSanitarios();

            } else {
                VentanaUtil.mostrarVentanaInformativa(
                        "No se pudo eliminar el sanitario. Intentalo de nuevo.",
                        TipoMensaje.ERROR
                );
            }
        }
    }

    /**
     * Selecciona todos los sanitarios de la tabla
     * @param event Evento del boton
     */
    @FXML
    void seleccionarTodosSanitarios(ActionEvent event) {
        tblSanitarios.getSelectionModel().selectAll();
    }

    /**
     * Busca sanitarios al presionar Enter o el boton de buscar
     * @param event Evento del campo de texto o boton
     */
    @FXML
    void buscarSanitarios(ActionEvent event) {
        buscarSanitariosPorTexto();
    }

    /**
     * Realiza una busqueda de sanitarios con el texto especificado
     * Llamado desde la ventana principal para busqueda rapida
     * @param textoBusqueda Texto a buscar
     */
    public void ejecutarBusqueda(String textoBusqueda) {
        if (textoBusqueda != null && !textoBusqueda.isEmpty()) {
            txfBuscarSanitarios.setText(textoBusqueda);
            buscarSanitariosPorTexto();
        }
    }
}