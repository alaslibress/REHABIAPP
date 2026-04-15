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
import com.javafx.excepcion.ConexionException;
import com.javafx.excepcion.DuplicadoException;
import com.javafx.excepcion.RehabiAppException;
import com.javafx.util.ConstantesApp;

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
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    // ========== CALENDARIO PERSONALIZADO ==========
    private CalendarioPersonalizado calendario;

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
     * Inicializa el calendario personalizado y lo conecta al contenedor FXML.
     * Suscribe listeners para sincronizar la seleccion con el DatePicker y la tabla.
     */
    private void configurarCalendario() {
        calendario = new CalendarioPersonalizado();
        contenedorCalendario.getChildren().setAll(calendario);
        calendario.prefWidthProperty().bind(contenedorCalendario.widthProperty());
        calendario.prefHeightProperty().bind(contenedorCalendario.heightProperty());

        // Seleccion simple: sincroniza DatePicker y filtra la tabla de citas del dia
        calendario.fechaSeleccionadaProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && !newV.equals(fechaSeleccionada)) {
                fechaSeleccionada = newV;
                dtpFechaCita.setValue(newV);
                actualizarLabelFecha();
                filtrarCitasPorFecha(newV);
            }
        });

        // Doble click sobre un dia: carga en tabla las citas de ese dia (misma logica que click simple)
        calendario.fechaDobleClickProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                fechaSeleccionada = newV;
                dtpFechaCita.setValue(newV);
                actualizarLabelFecha();
                filtrarCitasPorFecha(newV);
            }
        });
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
     * Actualiza el calendario personalizado con los datos del cache de citas.
     * Calcula el conteo por dia y las iniciales de cada paciente para los tooltips.
     */
    private void actualizarEntradasCalendario() {
        // Mapa de fecha -> numero de citas ese dia
        Map<LocalDate, Integer> conteo = cacheCitas.stream()
                .collect(Collectors.groupingBy(Cita::getFecha, Collectors.summingInt(c -> 1)));

        // Mapa de fecha -> lista de iniciales de paciente para el tooltip
        Map<LocalDate, List<String>> iniciales = cacheCitas.stream()
                .collect(Collectors.groupingBy(
                        Cita::getFecha,
                        Collectors.mapping(this::inicialesDePaciente, Collectors.toList())
                ));

        calendario.setCitasPorDia(conteo);
        calendario.setInicialesPorDia(iniciales);
        calendario.refrescar();
    }

    /**
     * Genera las iniciales del nombre del paciente de una cita.
     * Por ejemplo: "Juan Garcia Lopez" -> "J.G."
     *
     * @param cita cita de la que extraer las iniciales del paciente
     * @return cadena con las iniciales separadas por punto
     */
    private String inicialesDePaciente(Cita cita) {
        String nombre = cita.getNombrePaciente() != null ? cita.getNombrePaciente() : "";
        if (nombre.isBlank()) return "??";
        String[] partes = nombre.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(partes.length, 2); i++) {
            if (!partes[i].isEmpty()) {
                sb.append(Character.toUpperCase(partes[i].charAt(0))).append('.');
            }
        }
        return sb.toString();
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
     * Maneja el evento de clic en la tabla de citas.
     * Doble click: navega a la ficha del paciente de la cita seleccionada.
     */
    private void manejarDobleClicTabla(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
            Cita citaSeleccionada = tblCitas.getSelectionModel().getSelectedItem();
            if (citaSeleccionada != null) {
                verFichaPaciente(null);
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
                calendario.setFechaSeleccionada(newDate);
                calendario.setMesActual(YearMonth.from(newDate));
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
            calendario.setFechaSeleccionada(fecha);
            calendario.setMesActual(YearMonth.from(fecha));
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
        dtpFechaCita.setValue(LocalDate.now());
        calendario.setFechaSeleccionada(LocalDate.now());
        calendario.setMesActual(YearMonth.now());
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
        // Verificar si hay seleccion multiple de dias en el calendario
        var diasSeleccionados = calendario.getFechasSeleccionadas();

        if (diasSeleccionados.size() > 1) {
            // Eliminacion en lote: todas las citas de los dias seleccionados
            List<Cita> citasAEliminar = cacheCitas.stream()
                    .filter(c -> diasSeleccionados.contains(c.getFecha()))
                    .collect(Collectors.toList());

            if (citasAEliminar.isEmpty()) {
                VentanaUtil.mostrarVentanaInformativa(
                        "No hay citas en los dias seleccionados.", TipoMensaje.ADVERTENCIA);
                return;
            }

            boolean confirmado = VentanaUtil.mostrarVentanaPregunta(
                    "¿Eliminar " + citasAEliminar.size() + " citas de los dias seleccionados?\n"
                    + "Esta accion no se puede deshacer."
            );

            if (confirmado) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() {
                        for (Cita c : citasAEliminar) {
                            citaDAO.eliminar(c.getDniPaciente(), dniSanitarioActual,
                                    c.getFecha(), c.getHora());
                        }
                        return null;
                    }
                };

                task.setOnSucceeded(e -> {
                    VentanaUtil.mostrarVentanaInformativa(
                            citasAEliminar.size() + " citas eliminadas.", TipoMensaje.EXITO);
                    recargarDatos();
                });

                task.setOnFailed(e -> {
                    Throwable ex = task.getException();
                    VentanaUtil.mostrarVentanaInformativa(
                            "Error al eliminar: " + ex.getMessage(), TipoMensaje.ERROR);
                });

                executor.submit(task);
            }
        } else {
            // Eliminacion individual: cita seleccionada en la tabla
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
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() {
                        citaDAO.eliminar(cita.getDniPaciente(), dniSanitarioActual,
                                        cita.getFecha(), cita.getHora());
                        return null;
                    }
                };

                task.setOnSucceeded(e -> {
                    VentanaUtil.mostrarVentanaInformativa("Cita eliminada.", TipoMensaje.EXITO);
                    recargarDatos();
                });

                task.setOnFailed(e -> {
                    Throwable ex = task.getException();
                    if (ex instanceof ConexionException) {
                        VentanaUtil.mostrarVentanaInformativa(
                                "Error de conexion con la base de datos.", TipoMensaje.ERROR);
                    } else {
                        VentanaUtil.mostrarVentanaInformativa(
                                "Error: " + ex.getMessage(), TipoMensaje.ERROR);
                    }
                });

                executor.submit(task);
            }
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
        dtpFechaCita.setValue(LocalDate.now());
        calendario.setFechaSeleccionada(LocalDate.now());
        calendario.setMesActual(YearMonth.now());
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

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                if (citaDAO.existeCitaEnHorario(dniSanitarioActual, fechaSeleccionada, hora)) {
                    throw new DuplicadoException("Ya existe una cita en ese horario", "horario");
                }
                citaDAO.insertar(nueva);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
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
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (ex instanceof DuplicadoException) {
                VentanaUtil.mostrarVentanaInformativa(
                        "Ya existe una cita en ese horario.", TipoMensaje.ADVERTENCIA);
            } else if (ex instanceof ConexionException) {
                VentanaUtil.mostrarVentanaInformativa(
                        "Error de conexion con la base de datos.", TipoMensaje.ERROR);
            } else {
                VentanaUtil.mostrarVentanaInformativa(
                        "Error: " + ex.getMessage(), TipoMensaje.ERROR);
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
     * Navega a la pestana de Pacientes mostrando la ficha del paciente
     * de la cita actualmente seleccionada en la tabla.
     * Vinculado al boton "Ver ficha paciente" del FXML.
     */
    @FXML
    void verFichaPaciente(ActionEvent event) {
        Cita cita = tblCitas.getSelectionModel().getSelectedItem();
        if (cita == null) {
            VentanaUtil.mostrarVentanaInformativa("Seleccione una cita.", TipoMensaje.ADVERTENCIA);
            return;
        }
        controladorVentanaPrincipal principal = controladorVentanaPrincipal.getInstanciaActiva();
        if (principal != null) {
            principal.abrirFichaPacienteDesdeCita(cita.getDniPaciente());
        } else {
            // Fallback: la ventana principal no esta disponible en este contexto
            VentanaUtil.mostrarVentanaInformativa(
                    "No se puede navegar a la ficha del paciente en este momento.",
                    TipoMensaje.ADVERTENCIA);
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
