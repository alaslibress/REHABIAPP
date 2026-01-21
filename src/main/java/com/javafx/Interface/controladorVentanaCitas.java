package com.javafx.Interface;

import com.javafx.Clases.AnimacionUtil;
import com.javafx.Clases.Cita;
import com.javafx.Clases.InformeService;
import com.javafx.Clases.Paciente;
import com.javafx.Clases.SesionUsuario;
import com.javafx.Clases.VentanaUtil;
import com.javafx.Clases.VentanaUtil.TipoMensaje;
import com.javafx.DAO.CitaDAO;
import com.javafx.DAO.PacienteDAO;

import com.calendarfx.model.Calendar;
import com.calendarfx.model.CalendarSource;
import com.calendarfx.model.Entry;
import com.calendarfx.view.MonthView;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Controlador para la ventana de gestión de citas
 * Implementación LIMPIA con CalendarFX MonthView
 * 
 * OPTIMIZACIONES:
 * - Caché de citas para evitar consultas BD repetidas
 * - Entradas como full-day (sin hora) para evitar "1 más..."
 * - Listener de fecha con debounce implícito
 * - Tareas asíncronas para operaciones BD
 */
public class controladorVentanaCitas {

    // ========== COMPONENTES FXML ==========
    @FXML private VBox vboxContenedorPrincipal;
    @FXML private Label lblTituloPestania;
    @FXML private StackPane contenedorCalendario;
    @FXML private Button btnHoy;
    @FXML private Button btnVerTodas;
    @FXML private Button btnVerAgenda;
    @FXML private TableView<Cita> tblCitas;
    @FXML private TableColumn<Cita, String> colFecha;
    @FXML private TableColumn<Cita, String> colHora;
    @FXML private TableColumn<Cita, String> colPaciente;
    @FXML private TableColumn<Cita, String> colDNIPaciente;
    @FXML private TableColumn<Cita, String> colSanitario;
    @FXML private Button btnEliminarCita;
    @FXML private Button btnVerCita;
    @FXML private TextField txtDNIPaciente;
    @FXML private Button btnBuscarPaciente;
    @FXML private Label lblNombrePaciente;
    @FXML private Label lblFechaSeleccionada;
    @FXML private Spinner<Integer> spnHora;
    @FXML private Spinner<Integer> spnMinuto;
    @FXML private Button btnRestablecer;
    @FXML private Button btnAniadirCita;

    // ========== CALENDARFX ==========
    private MonthView monthView;
    private Calendar calendarioDiasNormales;
    private Calendar calendarioFinesSemana;
    private Calendar calendarioHoy;
    private CalendarSource calendarSource;

    // ========== DAOs ==========
    private CitaDAO citaDAO;
    private PacienteDAO pacienteDAO;

    // ========== ESTADO ==========
    private String dniSanitarioActual;
    private Paciente pacienteSeleccionado;
    private LocalDate fechaSeleccionada;
    
    // ========== CACHÉ Y ASYNC ==========
    private List<Cita> cacheCitas = new ArrayList<>();
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    
    // ========== FORMATO ==========
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        citaDAO = new CitaDAO();
        pacienteDAO = new PacienteDAO();

        configurarTabla();
        configurarSpinners();
        configurarCalendario();

        fechaSeleccionada = LocalDate.now();
        actualizarLabelFecha();

        // Cargar datos si hay sesión activa
        SesionUsuario sesion = SesionUsuario.getInstancia();
        if (sesion.haySesionActiva()) {
            dniSanitarioActual = sesion.getDniUsuario();
            cargarCitasAsync();
        }
    }

    /**
     * Configura CalendarFX MonthView de forma LIMPIA y OPTIMIZADA
     * Usa TRES calendarios para aplicar diferentes colores según el tipo de día
     */
    private void configurarCalendario() {
        // Crear MonthView
        monthView = new MonthView();

        // Crear tres calendarios para diferentes tipos de días
        calendarioDiasNormales = new Calendar("Citas - Días Normales");
        calendarioDiasNormales.setStyle(Calendar.Style.STYLE1); // Azul - definido en CSS
        calendarioDiasNormales.setReadOnly(true);

        calendarioFinesSemana = new Calendar("Citas - Fines de Semana");
        calendarioFinesSemana.setStyle(Calendar.Style.STYLE4); // Rojo - definido en CSS
        calendarioFinesSemana.setReadOnly(true);

        calendarioHoy = new Calendar("Citas - Hoy");
        calendarioHoy.setStyle(Calendar.Style.STYLE2); // Verde/Azul claro - definido en CSS
        calendarioHoy.setReadOnly(true);

        // Crear source y añadir calendarios
        calendarSource = new CalendarSource("RehabiAPP");
        calendarSource.getCalendars().addAll(calendarioDiasNormales, calendarioFinesSemana, calendarioHoy);

        // Configurar MonthView
        monthView.getCalendarSources().add(calendarSource);
        monthView.setDate(LocalDate.now());
        monthView.setShowToday(true);
        monthView.setShowWeekNumbers(false);

        // Desactivar la creación de entradas por click (usamos formulario)
        monthView.setEntryFactory(param -> null);

        // Listener para cambio de fecha seleccionada
        monthView.dateProperty().addListener((obs, oldDate, newDate) -> {
            if (newDate != null && !newDate.equals(fechaSeleccionada)) {
                fechaSeleccionada = newDate;
                actualizarLabelFecha();
                filtrarCitasPorFecha(newDate);
            }
        });

        // Añadir al contenedor y hacer responsive
        contenedorCalendario.getChildren().add(monthView);
        monthView.prefWidthProperty().bind(contenedorCalendario.widthProperty());
        monthView.prefHeightProperty().bind(contenedorCalendario.heightProperty());
    }

    /**
     * Carga las citas en segundo plano (no bloquea UI)
     */
    private void cargarCitasAsync() {
        Task<List<Cita>> task = new Task<>() {
            @Override
            protected List<Cita> call() {
                return citaDAO.listarPorSanitario(dniSanitarioActual);
            }
        };

        task.setOnSucceeded(e -> {
            cacheCitas = new ArrayList<>(task.getValue());
            actualizarEntradasCalendario();
            filtrarCitasPorFecha(fechaSeleccionada);
        });

        task.setOnFailed(e -> {
            System.err.println("Error cargando citas: " + task.getException().getMessage());
        });

        executor.submit(task);
    }

    /**
     * Actualiza las entradas del calendario desde la caché
     * Usa entradas FULL-DAY para evitar el problema "1 más..."
     * Asigna cada entrada al calendario correcto según el tipo de día
     */
    private void actualizarEntradasCalendario() {
        // Limpiar entradas existentes de todos los calendarios
        calendarioDiasNormales.clear();
        calendarioFinesSemana.clear();
        calendarioHoy.clear();

        // Agrupar citas por fecha para mostrar contador
        var citasPorFecha = cacheCitas.stream()
            .collect(Collectors.groupingBy(Cita::getFecha));

        // Crear una entrada por cada día que tiene citas
        for (var entry : citasPorFecha.entrySet()) {
            LocalDate fecha = entry.getKey();
            int numCitas = entry.getValue().size();

            // Crear entrada full-day (sin hora específica)
            Entry<String> calEntry = new Entry<>(numCitas + (numCitas == 1 ? " cita" : " citas"));
            calEntry.setFullDay(true);
            calEntry.setInterval(fecha, fecha);

            // Asignar al calendario correcto según el tipo de día
            Calendar calendarioDestino = seleccionarCalendario(fecha);
            calEntry.setCalendar(calendarioDestino);
        }
    }

    /**
     * Selecciona el calendario apropiado según la fecha
     * @param fecha La fecha a evaluar
     * @return El calendario correcto (hoy, fin de semana, o día normal)
     */
    private Calendar seleccionarCalendario(LocalDate fecha) {
        // Verificar si es hoy (prioridad máxima)
        if (fecha.equals(LocalDate.now())) {
            return calendarioHoy;
        }

        // Verificar si es fin de semana (sábado=6, domingo=7)
        int diaSemana = fecha.getDayOfWeek().getValue();
        if (diaSemana == 6 || diaSemana == 7) {
            return calendarioFinesSemana;
        }

        // Día normal
        return calendarioDiasNormales;
    }

    /**
     * Filtra citas de la caché por fecha (instantáneo, sin BD)
     */
    private void filtrarCitasPorFecha(LocalDate fecha) {
        List<Cita> citasFiltradas = cacheCitas.stream()
            .filter(c -> c.getFecha().equals(fecha))
            .collect(Collectors.toList());
        
        tblCitas.getItems().setAll(citasFiltradas);
    }

    /**
     * Invalida caché y recarga datos
     */
    private void recargarDatos() {
        cargarCitasAsync();
    }

    private void actualizarLabelFecha() {
        if (fechaSeleccionada != null) {
            lblFechaSeleccionada.setText(fechaSeleccionada.format(FORMATO_FECHA));
        } else {
            lblFechaSeleccionada.setText("Seleccione en el calendario");
        }
    }

    private void configurarTabla() {
        tblCitas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaFormateada"));
        colHora.setCellValueFactory(new PropertyValueFactory<>("horaFormateada"));
        colPaciente.setCellValueFactory(new PropertyValueFactory<>("nombrePaciente"));
        colDNIPaciente.setCellValueFactory(new PropertyValueFactory<>("dniPaciente"));
        colSanitario.setCellValueFactory(new PropertyValueFactory<>("nombreSanitario"));
    }

    private void configurarSpinners() {
        spnHora.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(8, 20, 9));
        spnMinuto.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 45, 0, 15));
    }

    // ========== MÉTODOS PÚBLICOS ==========

    public void setDniSanitario(String dni) {
        this.dniSanitarioActual = dni;
        if (dni != null) {
            recargarDatos();
        }
    }

    public void filtrarPorTexto(String texto) {
        if (texto == null || texto.isEmpty()) return;

        // Intentar parsear como fecha
        try {
            LocalDate fecha = LocalDate.parse(texto, FORMATO_FECHA);
            fechaSeleccionada = fecha;
            monthView.setDate(fecha);
            actualizarLabelFecha();
            filtrarCitasPorFecha(fecha);
            return;
        } catch (Exception ignored) {}

        // Buscar por nombre de paciente
        String busqueda = texto.toLowerCase();
        List<Cita> filtradas = cacheCitas.stream()
            .filter(c -> c.getNombrePaciente() != null && 
                        c.getNombrePaciente().toLowerCase().contains(busqueda))
            .collect(Collectors.toList());

        tblCitas.getItems().setAll(filtradas);
    }

    // ========== ACCIONES FXML ==========

    @FXML
    void irAHoy(ActionEvent event) {
        fechaSeleccionada = LocalDate.now();
        monthView.setDate(LocalDate.now());
        actualizarLabelFecha();
        filtrarCitasPorFecha(LocalDate.now());
    }

    @FXML
    void verTodasLasCitas(ActionEvent event) {
        tblCitas.getItems().setAll(cacheCitas);
        lblFechaSeleccionada.setText("Mostrando todas");
    }

    @FXML
    void eliminarCitaSeleccionada(ActionEvent event) {
        Cita cita = tblCitas.getSelectionModel().getSelectedItem();
        if (cita == null) {
            VentanaUtil.mostrarVentanaInformativa("Seleccione una cita.", TipoMensaje.ADVERTENCIA);
            return;
        }

        boolean confirmado = VentanaUtil.mostrarVentanaPregunta(
            "¿Eliminar cita?\n\nPaciente: " + cita.getNombrePaciente() +
            "\nFecha: " + cita.getFechaFormateada() +
            "\nHora: " + cita.getHoraFormateada()
        );

        if (confirmado) {
            Task<Boolean> task = new Task<>() {
                @Override
                protected Boolean call() {
                    return citaDAO.eliminar(cita.getDniPaciente(), dniSanitarioActual, 
                                           cita.getFecha(), cita.getHora());
                }
            };

            task.setOnSucceeded(e -> {
                if (task.getValue()) {
                    VentanaUtil.mostrarVentanaInformativa("Cita eliminada.", TipoMensaje.EXITO);
                    recargarDatos();
                } else {
                    VentanaUtil.mostrarVentanaInformativa("Error al eliminar.", TipoMensaje.ERROR);
                }
            });

            executor.submit(task);
        }
    }

    @FXML
    void verCitaSeleccionada(ActionEvent event) {
        Cita cita = tblCitas.getSelectionModel().getSelectedItem();
        if (cita == null) {
            VentanaUtil.mostrarVentanaInformativa("Seleccione una cita.", TipoMensaje.ADVERTENCIA);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaCitaPaciente.fxml"));
            Parent root = loader.load();

            controladorCitaPaciente ctrl = loader.getController();
            ctrl.cargarDatosCita(cita.getDniPaciente(), dniSanitarioActual, 
                                cita.getFecha(), cita.getHora());

            Scene scene = new Scene(root);
            controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

            Stage stage = new Stage();
            stage.setTitle("Detalle de Cita");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            VentanaUtil.establecerIconoVentana(stage);
            
            stage.setOnShown(e -> AnimacionUtil.animarVentanaModal(stage));
            stage.showAndWait();

            if (ctrl.hayCambiosRealizados()) {
                recargarDatos();
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            VentanaUtil.mostrarVentanaInformativa("Error al abrir detalle.", TipoMensaje.ERROR);
        }
    }

    @FXML
    void buscarPaciente(ActionEvent event) {
        String dni = txtDNIPaciente.getText().trim();
        if (dni.isEmpty()) {
            VentanaUtil.mostrarVentanaInformativa("Introduce el DNI.", TipoMensaje.ADVERTENCIA);
            return;
        }

        Task<Paciente> task = new Task<>() {
            @Override
            protected Paciente call() {
                return pacienteDAO.buscarPorDni(dni);
            }
        };

        task.setOnSucceeded(e -> {
            pacienteSeleccionado = task.getValue();
            if (pacienteSeleccionado != null) {
                String nombre = pacienteSeleccionado.getNombre() + " " + 
                               pacienteSeleccionado.getApellido1();
                if (pacienteSeleccionado.getApellido2() != null) {
                    nombre += " " + pacienteSeleccionado.getApellido2();
                }
                lblNombrePaciente.setText(nombre);
                lblNombrePaciente.setStyle("-fx-text-fill: #27AE60;");
            } else {
                lblNombrePaciente.setText("No encontrado");
                lblNombrePaciente.setStyle("-fx-text-fill: #E74C3C;");
            }
        });

        executor.submit(task);
    }

    @FXML
    void restablecerFormulario(ActionEvent event) {
        txtDNIPaciente.clear();
        lblNombrePaciente.setText("");
        spnHora.getValueFactory().setValue(9);
        spnMinuto.getValueFactory().setValue(0);
        pacienteSeleccionado = null;
        
        fechaSeleccionada = LocalDate.now();
        monthView.setDate(LocalDate.now());
        actualizarLabelFecha();
        filtrarCitasPorFecha(LocalDate.now());
    }

    @FXML
    void aniadirCita(ActionEvent event) {
        // Validaciones
        if (pacienteSeleccionado == null) {
            VentanaUtil.mostrarVentanaInformativa("Busque un paciente primero.", TipoMensaje.ADVERTENCIA);
            return;
        }
        if (fechaSeleccionada == null) {
            VentanaUtil.mostrarVentanaInformativa("Seleccione una fecha.", TipoMensaje.ADVERTENCIA);
            return;
        }
        if (fechaSeleccionada.isBefore(LocalDate.now())) {
            VentanaUtil.mostrarVentanaInformativa("No puede ser fecha pasada.", TipoMensaje.ADVERTENCIA);
            return;
        }

        LocalTime hora = LocalTime.of(spnHora.getValue(), spnMinuto.getValue());

        // Verificar en caché (rápido)
        boolean existe = cacheCitas.stream()
            .anyMatch(c -> c.getFecha().equals(fechaSeleccionada) && c.getHora().equals(hora));

        if (existe) {
            VentanaUtil.mostrarVentanaInformativa("Ya existe cita a esa hora.", TipoMensaje.ADVERTENCIA);
            return;
        }

        Cita nueva = new Cita(pacienteSeleccionado.getDni(), dniSanitarioActual,
                             fechaSeleccionada, hora, null, null);

        final String nombrePac = pacienteSeleccionado.getNombre();
        final String fechaStr = fechaSeleccionada.format(FORMATO_FECHA);

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                if (citaDAO.existeCitaEnHorario(dniSanitarioActual, fechaSeleccionada, hora)) {
                    return false;
                }
                return citaDAO.insertar(nueva);
            }
        };

        task.setOnSucceeded(e -> {
            if (task.getValue()) {
                VentanaUtil.mostrarVentanaInformativa(
                    "Cita creada.\n\nPaciente: " + nombrePac + 
                    "\nFecha: " + fechaStr + "\nHora: " + hora,
                    TipoMensaje.EXITO
                );
                
                // Limpiar formulario
                txtDNIPaciente.clear();
                lblNombrePaciente.setText("");
                pacienteSeleccionado = null;
                spnHora.getValueFactory().setValue(9);
                spnMinuto.getValueFactory().setValue(0);
                
                recargarDatos();
            } else {
                VentanaUtil.mostrarVentanaInformativa("No se pudo crear.", TipoMensaje.ERROR);
            }
        });

        executor.submit(task);
    }

    /**
     * MÉTODO PARA VER AGENDA DE CITAS DEL SANITARIO ACTUAL
     * Muestra el informe de citas en una ventana modal integrada (WebView)
     *
     * Para usar este método desde el FXML, añade un botón con:
     * <Button text="Ver Mi Agenda" onAction="#verAgendaSanitario" />
     *
     * @param event Evento de acción del botón
     */
    @FXML
    void verAgendaSanitario(ActionEvent event) {
        // Validación: verificar que hay un sanitario activo en sesión
        if (dniSanitarioActual == null || dniSanitarioActual.isEmpty()) {
            VentanaUtil.mostrarVentanaInformativa(
                "Error: No se pudo identificar al sanitario.\n" +
                "Asegúrese de haber iniciado sesión correctamente.",
                TipoMensaje.ERROR
            );
            return;
        }

        // Mostrar mensaje de carga (opcional pero recomendado)
        System.out.println("Generando agenda para sanitario: " + dniSanitarioActual);

        // Llamar al servicio de informes para mostrar la agenda
        try {
            InformeService.mostrarAgendaSanitario(dniSanitarioActual);
        } catch (Exception e) {
            System.err.println("Error al mostrar la agenda: " + e.getMessage());
            e.printStackTrace();
            VentanaUtil.mostrarVentanaInformativa(
                "Error inesperado al mostrar la agenda.\n" +
                "Por favor, revise la consola para más detalles.",
                TipoMensaje.ERROR
            );
        }
    }

    /**
     * Limpieza de recursos
     */
    public void cleanup() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
