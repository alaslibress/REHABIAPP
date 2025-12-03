package com.javafx.Interface;

import com.javafx.Clases.Cita;
import com.javafx.Clases.Paciente;
import com.javafx.Clases.VentanaUtil;
import com.javafx.Clases.VentanaUtil.TipoMensaje;
import com.javafx.DAO.CitaDAO;
import com.javafx.DAO.PacienteDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
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
 * Controlador para la ventana principal de gestion de citas
 * Permite ver, crear y eliminar citas medicas
 */
public class controladorVentanaCitas {

    @FXML
    private VBox vboxContenedorPrincipal;

    @FXML
    private Label lblTituloPestania;

    //Tabla de citas
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

    //Filtros y acciones de la tabla
    @FXML
    private DatePicker dpFechaSeleccionada;

    @FXML
    private Button btnHoy;

    @FXML
    private Button btnVerTodas;

    @FXML
    private Button btnEliminarCita;

    @FXML
    private Button btnVerCita;

    //Formulario nueva cita
    @FXML
    private TextField txtDNIPaciente;

    @FXML
    private Label lblNombrePaciente;

    @FXML
    private DatePicker dpFechaNuevaCita;

    @FXML
    private Spinner<Integer> spnHora;

    @FXML
    private Spinner<Integer> spnMinuto;

    @FXML
    private Button btnBuscarPaciente;

    @FXML
    private Button btnRestablecer;

    @FXML
    private Button btnAniadirCita;

    //DAOs
    private CitaDAO citaDAO;
    private PacienteDAO pacienteDAO;

    //Lista observable de citas
    private ObservableList<Cita> listaCitas;

    //DNI del sanitario logueado (se establecera desde el controlador principal)
    private String dniSanitarioActual;

    //Paciente seleccionado para nueva cita
    private Paciente pacienteSeleccionado;

    /**
     * Metodo initialize se ejecuta automaticamente al cargar el FXML
     */
    @FXML
    public void initialize() {
        //Inicializar DAOs
        citaDAO = new CitaDAO();
        pacienteDAO = new PacienteDAO();

        //Inicializar lista observable
        listaCitas = FXCollections.observableArrayList();

        //Configurar tabla
        configurarTabla();

        //Configurar spinners de hora
        configurarSpinnersHora();

        //Configurar DatePickers
        configurarDatePickers();

        //Cargar citas del dia actual
        dpFechaSeleccionada.setValue(LocalDate.now());
        cargarCitasPorFecha(LocalDate.now());
    }

    /**
     * Configura las columnas de la tabla de citas
     */
    private void configurarTabla() {
        colFecha.setCellValueFactory(cellData -> cellData.getValue().fechaFormateadaProperty());
        colHora.setCellValueFactory(cellData -> cellData.getValue().horaFormateadaProperty());
        colPaciente.setCellValueFactory(new PropertyValueFactory<>("nombrePaciente"));
        colDNIPaciente.setCellValueFactory(new PropertyValueFactory<>("dniPaciente"));
        colSanitario.setCellValueFactory(new PropertyValueFactory<>("nombreSanitario"));

        tblCitas.setItems(listaCitas);
        tblCitas.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }

    /**
     * Configura los spinners de hora y minuto
     */
    private void configurarSpinnersHora() {
        //Spinner de hora (8-20, hora laboral)
        SpinnerValueFactory<Integer> factoryHora =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(8, 20, 9);
        spnHora.setValueFactory(factoryHora);

        //Spinner de minutos (0, 15, 30, 45 - intervalos de 15 min)
        SpinnerValueFactory<Integer> factoryMinuto =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 45, 0, 15);
        spnMinuto.setValueFactory(factoryMinuto);
    }

    /**
     * Configura los DatePickers para no permitir fechas pasadas en nueva cita
     */
    private void configurarDatePickers() {
        //El DatePicker de nueva cita no permite fechas pasadas
        dpFechaNuevaCita.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });

        //Valor por defecto: hoy
        dpFechaNuevaCita.setValue(LocalDate.now());
    }

    /**
     * Establece el DNI del sanitario logueado
     */
    public void setDniSanitario(String dniSanitario) {
        this.dniSanitarioActual = dniSanitario;
    }

    /**
     * Carga las citas de una fecha especifica
     */
    private void cargarCitasPorFecha(LocalDate fecha) {
        listaCitas.clear();

        List<Cita> citas = citaDAO.listarPorFecha(fecha);
        listaCitas.addAll(citas);

        System.out.println("Citas cargadas para " + fecha + ": " + citas.size());
    }

    /**
     * Carga todas las citas
     */
    private void cargarTodasLasCitas() {
        listaCitas.clear();

        List<Cita> citas = citaDAO.listarTodas();
        listaCitas.addAll(citas);

        System.out.println("Total de citas cargadas: " + citas.size());
    }

    // ==================== EVENTOS DE FILTRO ====================

    /**
     * Evento al seleccionar una fecha en el DatePicker
     */
    @FXML
    void seleccionarFecha(ActionEvent event) {
        LocalDate fechaSeleccionada = dpFechaSeleccionada.getValue();

        if (fechaSeleccionada != null) {
            cargarCitasPorFecha(fechaSeleccionada);
        }
    }

    /**
     * Muestra las citas del dia actual
     */
    @FXML
    void irAHoy(ActionEvent event) {
        dpFechaSeleccionada.setValue(LocalDate.now());
        cargarCitasPorFecha(LocalDate.now());
    }

    /**
     * Muestra todas las citas sin filtro de fecha
     */
    @FXML
    void verTodasLasCitas(ActionEvent event) {
        dpFechaSeleccionada.setValue(null);
        cargarTodasLasCitas();
    }

    // ==================== EVENTOS DE ACCIONES EN TABLA ====================

    /**
     * Abre la ventana de detalle de la cita seleccionada
     */
    @FXML
    void verCitaSeleccionada(ActionEvent event) {
        Cita citaSeleccionada = tblCitas.getSelectionModel().getSelectedItem();

        if (citaSeleccionada == null) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Debe seleccionar una cita de la lista.",
                    TipoMensaje.ADVERTENCIA
            );
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaCitaPaciente.fxml"));
            Parent root = loader.load();

            //Obtener el controlador y cargar los datos de la cita
            controladorCitaPaciente controlador = loader.getController();
            controlador.cargarDatosCita(
                    citaSeleccionada.getDniPaciente(),
                    citaSeleccionada.getDniSanitario(),
                    citaSeleccionada.getFecha(),
                    citaSeleccionada.getHora()
            );

            //Mostrar ventana modal
            Stage stage = new Stage();
            stage.setTitle("Detalle de Cita");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

            //Recargar citas si hubo cambios
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

    /**
     * Elimina la cita seleccionada
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

        String mensaje = "¿Esta seguro de que desea eliminar la cita?\n\n" +
                "Paciente: " + citaSeleccionada.getNombrePaciente() + "\n" +
                "Fecha: " + citaSeleccionada.getFechaFormateada() + "\n" +
                "Hora: " + citaSeleccionada.getHoraFormateada();

        boolean confirmado = VentanaUtil.mostrarVentanaPregunta(mensaje);

        if (confirmado) {
            boolean eliminada = citaDAO.eliminar(citaSeleccionada);

            if (eliminada) {
                VentanaUtil.mostrarVentanaInformativa(
                        "La cita ha sido eliminada correctamente.",
                        TipoMensaje.EXITO
                );

                //Recargar lista
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

    // ==================== EVENTOS DE NUEVA CITA ====================

    /**
     * Busca un paciente por DNI para asignar a la nueva cita
     */
    @FXML
    void buscarPaciente(ActionEvent event) {
        String dni = txtDNIPaciente.getText().trim().toUpperCase();

        if (dni.isEmpty()) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Introduce el DNI del paciente.",
                    TipoMensaje.ADVERTENCIA
            );
            return;
        }

        //Validar formato DNI
        if (!dni.matches("^[0-9]{8}[A-Za-z]$")) {
            VentanaUtil.mostrarVentanaInformativa(
                    "El formato del DNI no es valido (8 numeros + letra).",
                    TipoMensaje.ADVERTENCIA
            );
            return;
        }

        //Buscar paciente
        Paciente paciente = pacienteDAO.buscarPorDni(dni);

        if (paciente != null) {
            pacienteSeleccionado = paciente;
            lblNombrePaciente.setText(paciente.getNombreCompleto());
            lblNombrePaciente.setStyle("-fx-text-fill: #008000;"); //Verde
        } else {
            pacienteSeleccionado = null;
            lblNombrePaciente.setText("Paciente no encontrado");
            lblNombrePaciente.setStyle("-fx-text-fill: #FF0000;"); //Rojo
        }
    }

    /**
     * Crea una nueva cita con los datos del formulario
     */
    @FXML
    void aniadirCita(ActionEvent event) {
        //Validar que se haya seleccionado un paciente
        if (pacienteSeleccionado == null) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Debe buscar y seleccionar un paciente primero.",
                    TipoMensaje.ADVERTENCIA
            );
            txtDNIPaciente.requestFocus();
            return;
        }

        //Validar fecha
        LocalDate fecha = dpFechaNuevaCita.getValue();
        if (fecha == null) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Debe seleccionar una fecha para la cita.",
                    TipoMensaje.ADVERTENCIA
            );
            return;
        }

        //Obtener hora
        int hora = spnHora.getValue();
        int minuto = spnMinuto.getValue();
        LocalTime horaCita = LocalTime.of(hora, minuto);

        //Obtener DNI del sanitario (usar el del paciente si no hay uno logueado)
        String dniSanitario = dniSanitarioActual;
        if (dniSanitario == null || dniSanitario.isEmpty()) {
            dniSanitario = pacienteSeleccionado.getDniSanitario();
        }

        //Verificar si ya existe una cita en ese horario
        if (citaDAO.existeCitaEnHorario(dniSanitario, fecha, horaCita)) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Ya existe una cita programada para esa fecha y hora.",
                    TipoMensaje.ADVERTENCIA
            );
            return;
        }

        //Crear la cita
        Cita nuevaCita = new Cita(
                pacienteSeleccionado.getDni(),
                dniSanitario,
                fecha,
                horaCita
        );

        boolean insertada = citaDAO.insertar(nuevaCita);

        if (insertada) {
            VentanaUtil.mostrarVentanaInformativa(
                    "La cita ha sido creada correctamente.\n\n" +
                            "Paciente: " + pacienteSeleccionado.getNombreCompleto() + "\n" +
                            "Fecha: " + fecha.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "\n" +
                            "Hora: " + horaCita.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                    TipoMensaje.EXITO
            );

            //Limpiar formulario
            restablecerFormulario(event);

            //Recargar lista de citas
            if (dpFechaSeleccionada.getValue() != null) {
                cargarCitasPorFecha(dpFechaSeleccionada.getValue());
            } else {
                cargarTodasLasCitas();
            }
        } else {
            VentanaUtil.mostrarVentanaInformativa(
                    "No se pudo crear la cita. Revisa la consola para mas detalles.",
                    TipoMensaje.ERROR
            );
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
     * Selecciona una fecha especifica en el calendario y carga las citas
     * Llamado desde la ventana principal para busqueda rapida
     * @param fecha Fecha a seleccionar
     */
    public void seleccionarFecha(LocalDate fecha) {
        if (fecha != null && dpFechaSeleccionada != null) {
            //Establecer la fecha en el DatePicker
            dpFechaSeleccionada.setValue(fecha);

            //Cargar las citas de esa fecha
            cargarCitasPorFecha(fecha);

            System.out.println("Fecha seleccionada desde busqueda rapida: " + fecha);
        }
    }
}