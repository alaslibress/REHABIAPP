package com.javafx.Interface;

import com.javafx.Clases.AnimacionUtil;
import com.javafx.Clases.Cita;
import com.javafx.Clases.Paciente;
import com.javafx.Clases.SesionUsuario;
import com.javafx.Clases.VentanaUtil;
import com.javafx.Clases.VentanaUtil.TipoMensaje;
import com.javafx.DAO.CitaDAO;
import com.javafx.DAO.PacienteDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Controlador para la ventana de gestión de citas
 * Incluye lista de citas y formulario para crear nuevas citas
 */
public class controladorVentanaCitas {

    // ========== COMPONENTES COLUMNA IZQUIERDA (Lista de citas) ==========
    @FXML
    private VBox vboxContenedorPrincipal;

    @FXML
    private Label lblTituloPestania;

    @FXML
    private DatePicker dpFechaSeleccionada;

    @FXML
    private Button btnHoy;

    @FXML
    private Button btnVerTodas;

    @FXML
    private TableView<Cita> tblCitas;

    @FXML
    private TableColumn<Cita, String> colFecha;

    @FXML
    private TableColumn<Cita, String> colHora;

    @FXML
    private TableColumn<Cita, String> colPaciente;

    @FXML
    private TableColumn<Cita, String> colDNIPaciente;

    @FXML
    private TableColumn<Cita, String> colSanitario;

    @FXML
    private Button btnEliminarCita;

    @FXML
    private Button btnVerCita;

    // ========== COMPONENTES COLUMNA DERECHA (Nueva cita) ==========
    @FXML
    private TextField txtDNIPaciente;

    @FXML
    private Button btnBuscarPaciente;

    @FXML
    private Label lblNombrePaciente;

    @FXML
    private DatePicker dpFechaNuevaCita;

    @FXML
    private Spinner<Integer> spnHora;

    @FXML
    private Spinner<Integer> spnMinuto;

    @FXML
    private Button btnRestablecer;

    @FXML
    private Button btnAniadirCita;

    // ========== DAOs y variables ==========
    private CitaDAO citaDAO;
    private PacienteDAO pacienteDAO;
    private String dniSanitarioActual;
    private Paciente pacienteSeleccionado;

    /**
     * Método initialize se ejecuta automáticamente al cargar el FXML
     */
    @FXML
    public void initialize() {
        citaDAO = new CitaDAO();
        pacienteDAO = new PacienteDAO();

        // Configurar columnas de la tabla
        configurarTabla();

        // Configurar spinners de hora
        configurarSpinners();

        // Establecer fecha actual
        dpFechaSeleccionada.setValue(LocalDate.now());
        dpFechaNuevaCita.setValue(LocalDate.now());

        // Configurar DatePicker de nueva cita para no permitir fechas pasadas
        dpFechaNuevaCita.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });

        // Listener para cargar citas al cambiar la fecha
        dpFechaSeleccionada.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                cargarCitasPorFecha(newVal);
            }
        });

        // Obtener DNI del sanitario logueado
        SesionUsuario sesion = SesionUsuario.getInstancia();
        if (sesion.haySesionActiva()) {
            dniSanitarioActual = sesion.getDniUsuario();
            cargarCitasPorFecha(LocalDate.now());
        }
    }

    /**
     * Permite establecer el DNI del sanitario desde fuera
     */
    public void setDniSanitario(String dni) {
        this.dniSanitarioActual = dni;
        if (dni != null) {
            cargarCitasPorFecha(dpFechaSeleccionada.getValue() != null ? 
                    dpFechaSeleccionada.getValue() : LocalDate.now());
        }
    }

    /**
     * Filtra las citas por texto de búsqueda
     * Puede ser una fecha (dd/MM/yyyy) o un nombre de paciente
     * @param textoBusqueda Texto a buscar
     */
    public void filtrarPorTexto(String textoBusqueda) {
        if (textoBusqueda == null || textoBusqueda.isEmpty()) {
            return;
        }

        // Intentar parsear como fecha (dd/MM/yyyy)
        try {
            java.time.format.DateTimeFormatter formatter = 
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate fechaBuscada = LocalDate.parse(textoBusqueda, formatter);
            
            // Establecer la fecha en el DatePicker y cargar citas
            dpFechaSeleccionada.setValue(fechaBuscada);
            cargarCitasPorFecha(fechaBuscada);
            return;
        } catch (Exception e) {
            // No es una fecha, buscar por nombre de paciente
        }

        // Buscar por nombre de paciente en todas las citas
        if (dniSanitarioActual != null) {
            List<Cita> todasLasCitas = citaDAO.listarPorSanitario(dniSanitarioActual);
            List<Cita> citasFiltradas = todasLasCitas.stream()
                .filter(cita -> cita.getNombrePaciente() != null && 
                        cita.getNombrePaciente().toLowerCase().contains(textoBusqueda.toLowerCase()))
                .collect(java.util.stream.Collectors.toList());

            tblCitas.getItems().clear();
            tblCitas.getItems().addAll(citasFiltradas);
            dpFechaSeleccionada.setValue(null); // Limpiar filtro de fecha

            System.out.println("Citas filtradas por '" + textoBusqueda + "': " + citasFiltradas.size());
        }
    }

    /**
     * Configura las columnas de la tabla de citas
     */
    private void configurarTabla() {
        // Configurar política de redimensionamiento para evitar columna extra vacía
        tblCitas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaFormateada"));
        colHora.setCellValueFactory(new PropertyValueFactory<>("horaFormateada"));
        colPaciente.setCellValueFactory(new PropertyValueFactory<>("nombrePaciente"));
        colDNIPaciente.setCellValueFactory(new PropertyValueFactory<>("dniPaciente"));
        colSanitario.setCellValueFactory(new PropertyValueFactory<>("nombreSanitario"));
    }

    /**
     * Configura los spinners de hora y minuto
     */
    private void configurarSpinners() {
        // Spinner de hora (8-20)
        SpinnerValueFactory<Integer> factoryHora =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(8, 20, 9);
        spnHora.setValueFactory(factoryHora);

        // Spinner de minutos (0, 15, 30, 45)
        SpinnerValueFactory<Integer> factoryMinuto =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 45, 0, 15);
        spnMinuto.setValueFactory(factoryMinuto);
    }

    /**
     * Carga las citas del sanitario para una fecha determinada
     */
    private void cargarCitasPorFecha(LocalDate fecha) {
        if (dniSanitarioActual == null) {
            return;
        }

        List<Cita> citas = citaDAO.obtenerCitasPorSanitarioYFecha(dniSanitarioActual, fecha);
        tblCitas.getItems().clear();
        tblCitas.getItems().addAll(citas);

        System.out.println("Citas cargadas para " + fecha + ": " + citas.size());
    }

    /**
     * Carga todas las citas del sanitario (sin filtro de fecha)
     */
    private void cargarTodasLasCitas() {
        if (dniSanitarioActual == null) {
            return;
        }

        List<Cita> citas = citaDAO.listarPorSanitario(dniSanitarioActual);
        tblCitas.getItems().clear();
        tblCitas.getItems().addAll(citas);

        System.out.println("Todas las citas cargadas: " + citas.size());
    }

    // ========== MÉTODOS DE LA COLUMNA IZQUIERDA ==========

    /**
     * Acción del DatePicker al seleccionar fecha
     */
    @FXML
    void seleccionarFecha(ActionEvent event) {
        LocalDate fecha = dpFechaSeleccionada.getValue();
        if (fecha != null) {
            cargarCitasPorFecha(fecha);
        }
    }

    /**
     * Botón "Hoy" - establece la fecha actual
     */
    @FXML
    void irAHoy(ActionEvent event) {
        dpFechaSeleccionada.setValue(LocalDate.now());
        cargarCitasPorFecha(LocalDate.now());
    }

    /**
     * Botón "Ver todas" - muestra todas las citas sin filtro de fecha
     */
    @FXML
    void verTodasLasCitas(ActionEvent event) {
        dpFechaSeleccionada.setValue(null);
        cargarTodasLasCitas();
    }

    /**
     * Elimina la cita seleccionada en la tabla
     */
    @FXML
    void eliminarCitaSeleccionada(ActionEvent event) {
        Cita citaSeleccionada = tblCitas.getSelectionModel().getSelectedItem();

        if (citaSeleccionada == null) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Debe seleccionar una cita para eliminar.",
                    TipoMensaje.ADVERTENCIA
            );
            return;
        }

        boolean confirmado = VentanaUtil.mostrarVentanaPregunta(
                "¿Está seguro de que desea eliminar esta cita?\n\n" +
                        "Paciente: " + citaSeleccionada.getNombrePaciente() + "\n" +
                        "Fecha: " + citaSeleccionada.getFechaFormateada() + "\n" +
                        "Hora: " + citaSeleccionada.getHoraFormateada()
        );

        if (confirmado) {
            boolean eliminada = citaDAO.eliminar(
                    citaSeleccionada.getDniPaciente(),
                    dniSanitarioActual,
                    citaSeleccionada.getFecha(),
                    citaSeleccionada.getHora()
            );

            if (eliminada) {
                VentanaUtil.mostrarVentanaInformativa(
                        "Cita eliminada correctamente.",
                        TipoMensaje.EXITO
                );
                // Recargar citas
                if (dpFechaSeleccionada.getValue() != null) {
                    cargarCitasPorFecha(dpFechaSeleccionada.getValue());
                } else {
                    cargarTodasLasCitas();
                }
            } else {
                VentanaUtil.mostrarVentanaInformativa(
                        "No se pudo eliminar la cita.",
                        TipoMensaje.ERROR
                );
            }
        }
    }

    /**
     * Abre la ventana de detalle de la cita seleccionada
     */
    @FXML
    void verCitaSeleccionada(ActionEvent event) {
        Cita citaSeleccionada = tblCitas.getSelectionModel().getSelectedItem();

        if (citaSeleccionada == null) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Debe seleccionar una cita para ver.",
                    TipoMensaje.ADVERTENCIA
            );
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaCitaPaciente.fxml"));
            Parent root = loader.load();

            // Obtener controlador y cargar datos
            controladorCitaPaciente controlador = loader.getController();
            controlador.cargarDatosCita(
                    citaSeleccionada.getDniPaciente(),
                    dniSanitarioActual,
                    citaSeleccionada.getFecha(),
                    citaSeleccionada.getHora()
            );

            // Crear escena y aplicar CSS
            Scene scene = new Scene(root);
            controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

            Stage stage = new Stage();
            stage.setTitle("Detalle de Cita");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            
            // Establecer icono
            VentanaUtil.establecerIconoVentana(stage);
            
            // Mostrar con animación
            stage.show();
            AnimacionUtil.animarVentanaModal(stage);
            stage.showAndWait();

            // Recargar si hubo cambios
            if (controlador.hayCambiosRealizados()) {
                if (dpFechaSeleccionada.getValue() != null) {
                    cargarCitasPorFecha(dpFechaSeleccionada.getValue());
                } else {
                    cargarTodasLasCitas();
                }
            }

        } catch (Exception e) {
            System.err.println("Error al abrir detalle de cita: " + e.getMessage());
            e.printStackTrace();
            VentanaUtil.mostrarVentanaInformativa(
                    "Error al abrir el detalle de la cita.",
                    TipoMensaje.ERROR
            );
        }
    }

    // ========== MÉTODOS DE LA COLUMNA DERECHA (Nueva cita) ==========

    /**
     * Busca el paciente por DNI
     */
    @FXML
    void buscarPaciente(ActionEvent event) {
        String dni = txtDNIPaciente.getText().trim();

        if (dni.isEmpty()) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Introduce el DNI del paciente.",
                    TipoMensaje.ADVERTENCIA
            );
            return;
        }

        pacienteSeleccionado = pacienteDAO.buscarPorDni(dni);

        if (pacienteSeleccionado != null) {
            String nombreCompleto = pacienteSeleccionado.getNombre() + " " + 
                    pacienteSeleccionado.getApellido1();
            if (pacienteSeleccionado.getApellido2() != null && 
                    !pacienteSeleccionado.getApellido2().isEmpty()) {
                nombreCompleto += " " + pacienteSeleccionado.getApellido2();
            }
            lblNombrePaciente.setText(nombreCompleto);
            lblNombrePaciente.setStyle("-fx-text-fill: #27AE60;"); // Verde
        } else {
            lblNombrePaciente.setText("Paciente no encontrado");
            lblNombrePaciente.setStyle("-fx-text-fill: #E74C3C;"); // Rojo
        }
    }

    /**
     * Limpia el formulario de nueva cita
     */
    @FXML
    void restablecerFormulario(ActionEvent event) {
        txtDNIPaciente.clear();
        lblNombrePaciente.setText("");
        dpFechaNuevaCita.setValue(LocalDate.now());
        spnHora.getValueFactory().setValue(9);
        spnMinuto.getValueFactory().setValue(0);
        pacienteSeleccionado = null;
    }

    /**
     * Crea una nueva cita con los datos del formulario
     */
    @FXML
    void aniadirCita(ActionEvent event) {
        // Validar que hay paciente seleccionado
        if (pacienteSeleccionado == null) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Debe buscar y seleccionar un paciente primero.",
                    TipoMensaje.ADVERTENCIA
            );
            return;
        }

        // Validar fecha
        LocalDate fecha = dpFechaNuevaCita.getValue();
        if (fecha == null) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Debe seleccionar una fecha para la cita.",
                    TipoMensaje.ADVERTENCIA
            );
            return;
        }

        if (fecha.isBefore(LocalDate.now())) {
            VentanaUtil.mostrarVentanaInformativa(
                    "No se pueden crear citas en fechas pasadas.",
                    TipoMensaje.ADVERTENCIA
            );
            return;
        }

        // Obtener hora
        int hora = spnHora.getValue();
        int minuto = spnMinuto.getValue();
        LocalTime horaCita = LocalTime.of(hora, minuto);

        // Verificar que no exista ya una cita en ese horario
        if (citaDAO.existeCitaEnHorario(dniSanitarioActual, fecha, horaCita)) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Ya existe una cita programada para esa fecha y hora.",
                    TipoMensaje.ADVERTENCIA
            );
            return;
        }

        // Crear objeto Cita
        Cita nuevaCita = new Cita(
                pacienteSeleccionado.getDni(),
                dniSanitarioActual,
                fecha,
                horaCita,
                null,
                null
        );

        // Insertar la cita
        boolean creada = citaDAO.insertar(nuevaCita);

        if (creada) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Cita creada correctamente.\n\n" +
                            "Paciente: " + pacienteSeleccionado.getNombre() + "\n" +
                            "Fecha: " + fecha + "\n" +
                            "Hora: " + horaCita,
                    TipoMensaje.EXITO
            );

            // Limpiar formulario
            restablecerFormulario(null);

            // Recargar tabla de citas
            dpFechaSeleccionada.setValue(fecha);
            cargarCitasPorFecha(fecha);

        } else {
            VentanaUtil.mostrarVentanaInformativa(
                    "No se pudo crear la cita.",
                    TipoMensaje.ERROR
            );
        }
    }
}
