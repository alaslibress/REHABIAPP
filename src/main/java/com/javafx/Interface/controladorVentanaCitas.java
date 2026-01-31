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
import com.javafx.util.ConstantesApp;

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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
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
    @FXML private ComboBox<Paciente> cmbPacientes;
    @FXML private Label lblInfoPaciente;
    @FXML private javafx.scene.control.DatePicker dtpFechaCita;
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
    private String textoBusquedaPendiente = null;

    // ========== CACHÉ Y ASYNC ==========
    private List<Cita> cacheCitas = new ArrayList<>();
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    // ========== AUTOCOMPLETADO ==========
    private ObservableList<Paciente> todosLosPacientes = FXCollections.observableArrayList();
    private ObservableList<Paciente> pacientesFiltrados = FXCollections.observableArrayList();

    // ========== LISTA DE CITAS ==========
    private ObservableList<Cita> listaCitas = FXCollections.observableArrayList();

    // ========== FORMATO (usando ConstantesApp) ==========
    // REFACTORIZADO: Usar constante centralizada en lugar de duplicar el patrón

    @FXML
    public void initialize() {
        citaDAO = new CitaDAO();
        pacienteDAO = new PacienteDAO();

        configurarTabla();
        configurarSpinners();
        configurarCalendario();
        configurarDatePicker();
        configurarAutocompletadoPacientes();

        //Configurar doble clic para abrir detalles de la cita
        tblCitas.setOnMouseClicked(this::manejarDobleClicTabla);

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
                dtpFechaCita.setValue(newDate); // Sincronizar con DatePicker
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

            // Si hay una búsqueda pendiente, aplicarla
            if (textoBusquedaPendiente != null && !textoBusquedaPendiente.isEmpty()) {
                filtrarPorTexto(textoBusquedaPendiente);
                textoBusquedaPendiente = null; // Limpiar después de aplicar
            } else {
                filtrarCitasPorFecha(fechaSeleccionada);
            }
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

        listaCitas.setAll(citasFiltradas);
    }

    /**
     * Invalida caché y recarga datos
     */
    private void recargarDatos() {
        cargarCitasAsync();
    }

    private void actualizarLabelFecha() {
        if (fechaSeleccionada != null) {
            lblFechaSeleccionada.setText(fechaSeleccionada.format(ConstantesApp.FORMATO_FECHA));
        } else {
            lblFechaSeleccionada.setText("Seleccione en el calendario");
        }
    }

    /**
     * Maneja el evento de clic en la tabla de citas
     * Si es doble clic, abre los detalles de la cita
     */
    private void manejarDobleClicTabla(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
            Cita citaSeleccionada = tblCitas.getSelectionModel().getSelectedItem();
            if (citaSeleccionada != null) {
                verCitaSeleccionada(null);
            }
        }
    }

    private void configurarTabla() {
        tblCitas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaFormateada"));
        colHora.setCellValueFactory(new PropertyValueFactory<>("horaFormateada"));
        colPaciente.setCellValueFactory(new PropertyValueFactory<>("nombrePaciente"));
        colDNIPaciente.setCellValueFactory(new PropertyValueFactory<>("dniPaciente"));
        colSanitario.setCellValueFactory(new PropertyValueFactory<>("nombreSanitario"));

        //IMPORTANTE: Vincular la tabla con la lista observable
        tblCitas.setItems(listaCitas);
    }

    private void configurarSpinners() {
        spnHora.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(8, 20, 9));
        spnMinuto.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 45, 0, 15));
    }

    /**
     * Configura el DatePicker para selección de fecha
     * - Establece la fecha actual por defecto
     * - Bloquea fechas pasadas
     * - Sincroniza con el calendario
     */
    private void configurarDatePicker() {
        // Establecer fecha actual
        dtpFechaCita.setValue(LocalDate.now());

        // Bloquear fechas pasadas
        dtpFechaCita.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);

                // Deshabilitar fechas anteriores a hoy
                if (date != null && date.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #ffc0cb;"); // Rosa claro para fechas deshabilitadas
                }
            }
        });

        // Sincronizar con el calendario y la variable fechaSeleccionada
        dtpFechaCita.valueProperty().addListener((obs, oldDate, newDate) -> {
            if (newDate != null) {
                fechaSeleccionada = newDate;
                monthView.setDate(newDate);
                actualizarLabelFecha();
                filtrarCitasPorFecha(newDate);
            }
        });
    }

    /**
     * Configura el sistema de autocompletado para buscar pacientes
     */
    private void configurarAutocompletadoPacientes() {
        // Cargar todos los pacientes en segundo plano
        Task<List<Paciente>> taskCargarPacientes = new Task<>() {
            @Override
            protected List<Paciente> call() {
                return pacienteDAO.listarTodos();
            }
        };

        taskCargarPacientes.setOnSucceeded(e -> {
            List<Paciente> pacientes = taskCargarPacientes.getValue();
            if (pacientes != null) {
                todosLosPacientes.setAll(pacientes);
                pacientesFiltrados.setAll(pacientes);
                cmbPacientes.setItems(pacientesFiltrados);
            }
        });

        executor.submit(taskCargarPacientes);

        // Configurar StringConverter para mostrar pacientes de forma legible
        cmbPacientes.setConverter(new javafx.util.StringConverter<Paciente>() {
            @Override
            public String toString(Paciente paciente) {
                if (paciente == null) {
                    return "";
                }
                StringBuilder sb = new StringBuilder();
                sb.append(paciente.getDni()).append(" - ");
                sb.append(paciente.getNombre()).append(" ");
                sb.append(paciente.getApellido1());
                if (paciente.getApellido2() != null && !paciente.getApellido2().isEmpty()) {
                    sb.append(" ").append(paciente.getApellido2());
                }
                return sb.toString();
            }

            @Override
            public Paciente fromString(String string) {
                // Mantener el paciente seleccionado aunque el texto cambie
                return pacienteSeleccionado;
            }
        });

        // Bandera para controlar si estamos procesando una selección
        final boolean[] procesandoSeleccion = {false};

        // Implementar filtrado mientras el usuario escribe
        cmbPacientes.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            // No procesar si estamos en medio de una selección
            if (procesandoSeleccion[0]) {
                return;
            }

            // Si hay un paciente ya seleccionado del ComboBox, no filtrar
            Paciente seleccionado = cmbPacientes.getSelectionModel().getSelectedItem();
            if (seleccionado != null) {
                // Verificar si el texto coincide con el paciente seleccionado
                String textoEsperado = cmbPacientes.getConverter().toString(seleccionado);
                if (newValue != null && newValue.equals(textoEsperado)) {
                    return;
                }
            }

            if (newValue == null || newValue.isEmpty()) {
                pacientesFiltrados.setAll(todosLosPacientes);
                lblInfoPaciente.setText("Escribe para ver sugerencias");
                lblInfoPaciente.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");
                cmbPacientes.hide();
                pacienteSeleccionado = null;
                return;
            }

            String textoLower = newValue.toLowerCase();
            List<Paciente> resultados = todosLosPacientes.stream()
                .filter(p -> {
                    String dni = p.getDni() != null ? p.getDni().toLowerCase() : "";
                    String nombre = p.getNombre() != null ? p.getNombre().toLowerCase() : "";
                    String apellido1 = p.getApellido1() != null ? p.getApellido1().toLowerCase() : "";
                    String apellido2 = p.getApellido2() != null ? p.getApellido2().toLowerCase() : "";

                    return dni.contains(textoLower) ||
                           nombre.contains(textoLower) ||
                           apellido1.contains(textoLower) ||
                           apellido2.contains(textoLower);
                })
                .collect(Collectors.toList());

            pacientesFiltrados.setAll(resultados);

            if (!resultados.isEmpty()) {
                lblInfoPaciente.setText(resultados.size() + " paciente(s) encontrado(s)");
                if (!cmbPacientes.isShowing()) {
                    cmbPacientes.show();
                }
            } else {
                lblInfoPaciente.setText("No se encontraron pacientes");
                cmbPacientes.hide();
            }
        });

        // Cuando se selecciona un paciente de la lista
        cmbPacientes.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                procesandoSeleccion[0] = true;
                pacienteSeleccionado = newVal;

                // Actualizar el texto del editor
                cmbPacientes.getEditor().setText(cmbPacientes.getConverter().toString(newVal));

                String info = String.format("✓ Seleccionado: %s %s (NSS: %s)",
                    newVal.getNombre(),
                    newVal.getApellido1(),
                    newVal.getNumSS() != null ? newVal.getNumSS() : "N/A");
                lblInfoPaciente.setText(info);
                lblInfoPaciente.setStyle("-fx-text-fill: #27AE60; -fx-font-size: 11px;");

                // Ocultar el dropdown
                cmbPacientes.hide();

                procesandoSeleccion[0] = false;
            } else {
                // Se limpió la selección
                if (pacienteSeleccionado != null) {
                    pacienteSeleccionado = null;
                    lblInfoPaciente.setText("Escribe para ver sugerencias");
                    lblInfoPaciente.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");
                }
            }
        });
    }

    // ========== MÉTODOS PÚBLICOS ==========

    public void setDniSanitario(String dni) {
        this.dniSanitarioActual = dni;
        if (dni != null) {
            recargarDatos();
        }
    }

    /**
     * Establece un texto de búsqueda que se aplicará cuando las citas se carguen
     * @param texto El texto a buscar
     */
    public void setTextoBusquedaPendiente(String texto) {
        this.textoBusquedaPendiente = texto;
    }

    /**
     * Filtra las citas por texto (fecha, nombre de paciente o DNI)
     * @param texto El texto a buscar
     * @return El número de resultados encontrados
     */
    public int filtrarPorTexto(String texto) {
        if (texto == null || texto.isEmpty()) return 0;

        // Intentar parsear como fecha
        try {
            LocalDate fecha = LocalDate.parse(texto, ConstantesApp.FORMATO_FECHA);
            fechaSeleccionada = fecha;
            monthView.setDate(fecha);
            actualizarLabelFecha();
            filtrarCitasPorFecha(fecha);
            int numResultados = listaCitas.size();

            if (numResultados == 0) {
                VentanaUtil.mostrarVentanaInformativa(
                    "Cita no encontrada.\n\nNo se encontraron citas para la fecha: " + texto,
                    TipoMensaje.INFORMACION
                );
            }

            return numResultados;
        } catch (Exception ignored) {}

        // Buscar por nombre de paciente o DNI de paciente
        String busqueda = texto.toLowerCase();
        List<Cita> filtradas = cacheCitas.stream()
            .filter(c -> {
                // Buscar por nombre de paciente
                boolean coincideNombre = c.getNombrePaciente() != null &&
                                        c.getNombrePaciente().toLowerCase().contains(busqueda);

                // Buscar por DNI de paciente
                boolean coincideDNI = c.getDniPaciente() != null &&
                                     c.getDniPaciente().toLowerCase().contains(busqueda);

                return coincideNombre || coincideDNI;
            })
            .collect(Collectors.toList());

        listaCitas.setAll(filtradas);

        // Actualizar label para mostrar que se está filtrando
        if (!filtradas.isEmpty()) {
            lblFechaSeleccionada.setText("Resultados de búsqueda: " + filtradas.size() + " cita(s)");
        } else {
            lblFechaSeleccionada.setText("Sin resultados");
            VentanaUtil.mostrarVentanaInformativa(
                "Cita no encontrada.\n\nNo se encontraron citas con el criterio de búsqueda: " + texto,
                TipoMensaje.INFORMACION
            );
        }

        return filtradas.size();
    }

    // ========== ACCIONES FXML ==========

    @FXML
    void irAHoy(ActionEvent event) {
        fechaSeleccionada = LocalDate.now();
        dtpFechaCita.setValue(LocalDate.now()); // Actualizar DatePicker
        monthView.setDate(LocalDate.now());
        actualizarLabelFecha();
        filtrarCitasPorFecha(LocalDate.now());
    }

    @FXML
    void verTodasLasCitas(ActionEvent event) {
        listaCitas.setAll(cacheCitas);
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
    void restablecerFormulario(ActionEvent event) {
        cmbPacientes.getSelectionModel().clearSelection();
        cmbPacientes.getEditor().clear();
        lblInfoPaciente.setText("Escribe para ver sugerencias");
        lblInfoPaciente.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");
        spnHora.getValueFactory().setValue(9);
        spnMinuto.getValueFactory().setValue(0);
        pacienteSeleccionado = null;

        fechaSeleccionada = LocalDate.now();
        dtpFechaCita.setValue(LocalDate.now()); // Actualizar DatePicker
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
        final String fechaStr = fechaSeleccionada.format(ConstantesApp.FORMATO_FECHA);

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
                cmbPacientes.getSelectionModel().clearSelection();
                cmbPacientes.getEditor().clear();
                lblInfoPaciente.setText("Escribe para ver sugerencias");
                lblInfoPaciente.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");
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
