package com.javafx.Interface;

import com.javafx.Clases.Discapacidad;
import com.javafx.Clases.InformeService;
import com.javafx.Clases.NivelProgresion;
import com.javafx.Clases.Paciente;
import com.javafx.Clases.PacienteDiscapacidad;
import com.javafx.Clases.PacienteTratamiento;
import com.javafx.Clases.Tratamiento;
import com.javafx.Clases.VentanaUtil;
import com.javafx.Clases.VentanaUtil.TipoMensaje;
import com.javafx.DAO.PacienteDAO;
import com.javafx.excepcion.ConexionException;
import com.javafx.excepcion.ValidacionException;
import com.javafx.service.AuditService;
import com.javafx.service.CatalogoService;
import com.javafx.service.PacienteClinicoService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controlador para la ventana de ficha/informacion de un paciente.
 * Muestra todos los datos del paciente, su gestion clinica
 * (discapacidades, tratamientos, niveles de progresion) y permite acceder a la edicion.
 */
public class controladorVentanaPacienteListar {

    // ==================== CAMPOS FXML - DATOS PERSONALES ====================

    @FXML private Button btnAceptar;
    @FXML private Button btnDescargarInforme;
    @FXML private Button btnEditar;
    @FXML private ImageView imgFotoPaciente;
    @FXML private Label lblApellidosValor;
    @FXML private Label lblConsentimientoValor;
    @FXML private Label lblDNIValor;
    @FXML private Label lblDireccionValor;
    @FXML private Label lblEdadValor;
    @FXML private Label lblEmailValor;
    @FXML private Label lblFechaNacimientoValor;
    @FXML private Label lblNSSValor;
    @FXML private Label lblNombreValor;
    @FXML private Label lblSexoValor;
    @FXML private Label lblTelefono1Valor;
    @FXML private Label lblTelefono2Valor;
    @FXML private Label lblTituloVentana;
    @FXML private TextArea txtAreaAlergias;
    @FXML private TextArea txtAreaAntecedentes;
    @FXML private TextArea txtAreaEstado;
    @FXML private TextArea txtAreaMedicacion;

    // ==================== CAMPOS FXML - DISCAPACIDADES ====================

    @FXML private TableView<PacienteDiscapacidad> tblDiscapacidadesPaciente;
    @FXML private TableColumn<PacienteDiscapacidad, String> colDisCodigo;
    @FXML private TableColumn<PacienteDiscapacidad, String> colDisNombre;
    @FXML private TableColumn<PacienteDiscapacidad, String> colDisNivel;
    @FXML private TableColumn<PacienteDiscapacidad, String> colDisNotas;
    @FXML private HBox hboxBotonesDiscapacidad;
    @FXML private Button btnAsignarDiscapacidad;
    @FXML private Button btnDesasignarDiscapacidad;
    @FXML private Button btnSubirNivel;
    @FXML private Button btnBajarNivel;
    @FXML private Button btnEditarNotas;

    // ==================== CAMPOS FXML - TRATAMIENTOS ====================

    @FXML private TableView<PacienteTratamiento> tblTratamientosPaciente;
    @FXML private TableColumn<PacienteTratamiento, String> colTratNombre;
    @FXML private TableColumn<PacienteTratamiento, String> colTratDefinicion;
    @FXML private TableColumn<PacienteTratamiento, Boolean> colTratVisible;
    @FXML private HBox hboxBotonesTratamiento;
    @FXML private Button btnAsignarTratamiento;
    @FXML private Button btnDesasignarTratamiento;
    @FXML private Button btnToggleVisibilidad;

    // ==================== SERVICIOS Y ESTADO ====================

    private PacienteDAO pacienteDAO;
    private final PacienteClinicoService pacienteClinicoService = new PacienteClinicoService();
    private final CatalogoService catalogoService = new CatalogoService();

    private String dniPacienteActual;
    private Paciente pacienteActual;
    private boolean cambiosRealizados = false;
    private boolean modoSoloLectura = false;

    private static final int NIVEL_MINIMO = 1;
    private static final int NIVEL_MAXIMO = 4;

    //Mapa de niveles para tooltips informativos
    private Map<String, NivelProgresion> mapaNiveles;

    @FXML
    public void initialize() {
        pacienteDAO = new PacienteDAO();
        cargarMapaNiveles();
        configurarColumnasDiscapacidades();
        configurarColumnasTratamientos();
        configurarSeleccionDiscapacidad();
    }

    /**
     * Carga el mapa de niveles de progresion para tooltips informativos
     */
    private void cargarMapaNiveles() {
        mapaNiveles = new HashMap<>();
        try {
            List<NivelProgresion> niveles = catalogoService.listarNiveles();
            for (NivelProgresion nivel : niveles) {
                mapaNiveles.put(nivel.getNombreCorto(), nivel);
            }
        } catch (ConexionException e) {
            //Si falla la carga, los tooltips simplemente no se mostraran
            System.err.println("No se pudieron cargar los niveles para tooltips: " + e.getMessage());
        }
    }

    // ==================== CONFIGURACION DE TABLAS ====================

    private void configurarColumnasDiscapacidades() {
        colDisCodigo.setCellValueFactory(new PropertyValueFactory<>("codDis"));
        colDisNotas.setCellValueFactory(new PropertyValueFactory<>("notas"));

        //Columna de nombre con indicador de protesis
        colDisNombre.setCellValueFactory(new PropertyValueFactory<>("nombreDiscapacidad"));
        colDisNombre.setCellFactory(columna -> new TableCell<>() {
            @Override
            protected void updateItem(String nombre, boolean vacia) {
                super.updateItem(nombre, vacia);

                if (vacia || nombre == null) {
                    setText(null);
                    setTooltip(null);
                    return;
                }

                PacienteDiscapacidad pd = getTableView().getItems().get(getIndex());
                if (pd.isNecesitaProtesis()) {
                    setText(nombre + " (protesis)");
                    Tooltip tooltip = new Tooltip("Esta discapacidad requiere protesis");
                    tooltip.setShowDelay(Duration.millis(300));
                    setTooltip(tooltip);
                } else {
                    setText(nombre);
                    setTooltip(null);
                }
            }
        });

        //Columna de nivel con tooltip informativo
        colDisNivel.setCellValueFactory(new PropertyValueFactory<>("nombreNivel"));
        colDisNivel.setCellFactory(columna -> new TableCell<>() {
            @Override
            protected void updateItem(String nombreNivel, boolean vacia) {
                super.updateItem(nombreNivel, vacia);

                if (vacia || nombreNivel == null) {
                    setText(null);
                    setTooltip(null);
                    return;
                }

                setText(nombreNivel);

                //Buscar datos completos del nivel para el tooltip
                NivelProgresion nivel = mapaNiveles.get(nombreNivel);
                if (nivel != null) {
                    String textoTooltip = nivel.getNombre() + "\n" +
                            "Descripcion: " + nivel.getDescripcion() + "\n" +
                            "Estado paciente: " + nivel.getEstadoPaciente() + "\n" +
                            "Ejercicios: " + nivel.getTiposEjercicio();

                    Tooltip tooltip = new Tooltip(textoTooltip);
                    tooltip.setShowDelay(Duration.millis(300));
                    tooltip.setShowDuration(Duration.seconds(15));
                    tooltip.setWrapText(true);
                    tooltip.setMaxWidth(400);
                    setTooltip(tooltip);
                }
            }
        });
    }

    private void configurarColumnasTratamientos() {
        colTratNombre.setCellValueFactory(new PropertyValueFactory<>("nombreTratamiento"));
        colTratDefinicion.setCellValueFactory(new PropertyValueFactory<>("definicionTratamiento"));
        colTratVisible.setCellValueFactory(new PropertyValueFactory<>("visible"));
    }

    /**
     * Al seleccionar una discapacidad, carga sus tratamientos asociados
     */
    private void configurarSeleccionDiscapacidad() {
        tblDiscapacidadesPaciente.getSelectionModel().selectedItemProperty().addListener(
                (obs, anterior, seleccionada) -> {
                    if (seleccionada != null) {
                        cargarTratamientosPorDiscapacidad(seleccionada.getCodDis());
                    } else {
                        tblTratamientosPaciente.getItems().clear();
                    }
                }
        );
    }

    // ==================== PERMISOS ====================

    /**
     * Establece el modo solo lectura.
     * Deshabilita los botones de edicion y gestion clinica.
     */
    public void setModoSoloLectura(boolean soloLectura) {
        this.modoSoloLectura = soloLectura;

        if (soloLectura) {
            btnEditar.setDisable(true);
            btnEditar.setOpacity(0.5);
            btnEditar.setTooltip(new Tooltip(
                    "No tienes permisos para editar pacientes"));

            //Ocultar botones de gestion clinica para enfermeros
            hboxBotonesDiscapacidad.setVisible(false);
            hboxBotonesDiscapacidad.setManaged(false);
            hboxBotonesTratamiento.setVisible(false);
            hboxBotonesTratamiento.setManaged(false);
        }
    }

    // ==================== CARGA DE DATOS ====================

    /**
     * Carga los datos de un paciente por su DNI
     */
    public void cargarDatosPaciente(String dni) {
        this.dniPacienteActual = dni;

        pacienteActual = pacienteDAO.obtenerPorDNI(dni);

        if (pacienteActual != null) {
            mostrarDatosEnLabels();
            cargarFotoPaciente();
            cargarDiscapacidadesPaciente();

            //Registrar acceso a datos clinicos sensibles (Ley 41/2002, ENS)
            AuditService.consultaSensible(dni);
        } else {
            VentanaUtil.mostrarVentanaInformativa(
                    "No se encontro el paciente con DNI: " + dni,
                    TipoMensaje.ERROR
            );
        }
    }

    private void mostrarDatosEnLabels() {
        //Datos personales
        lblNombreValor.setText(valorOGuion(pacienteActual.getNombre()));
        lblApellidosValor.setText(valorOGuion(pacienteActual.getApellidos()));
        lblEdadValor.setText(pacienteActual.getEdad() > 0 ? String.valueOf(pacienteActual.getEdad()) + " años" : "-");
        lblDNIValor.setText(valorOGuion(pacienteActual.getDni()));
        lblEmailValor.setText(valorOGuion(pacienteActual.getEmail()));
        lblNSSValor.setText(valorOGuion(pacienteActual.getNumSS()));

        //Telefonos
        lblTelefono1Valor.setText(valorOGuion(pacienteActual.getTelefono1()));
        lblTelefono2Valor.setText(valorOGuion(pacienteActual.getTelefono2()));

        //Direccion
        String direccionCompleta = construirDireccionCompleta();
        lblDireccionValor.setText(direccionCompleta.isEmpty() ? "-" : direccionCompleta);

        //Estado del paciente (campo legacy)
        txtAreaEstado.setText(valorOGuion(pacienteActual.getEstadoTratamiento()));

        //Datos clinicos
        String sexoBD = pacienteActual.getSexo();
        if (sexoBD != null) {
            switch (sexoBD) {
                case "M": lblSexoValor.setText("Masculino"); break;
                case "F": lblSexoValor.setText("Femenino"); break;
                case "O": lblSexoValor.setText("Otro"); break;
                default: lblSexoValor.setText("-"); break;
            }
        } else {
            lblSexoValor.setText("-");
        }

        if (pacienteActual.getFechaNacimiento() != null) {
            DateTimeFormatter formateador = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            lblFechaNacimientoValor.setText(pacienteActual.getFechaNacimiento().format(formateador));
        } else {
            lblFechaNacimientoValor.setText("-");
        }

        txtAreaAlergias.setText(valorOGuion(pacienteActual.getAlergias()));
        txtAreaAntecedentes.setText(valorOGuion(pacienteActual.getAntecedentes()));
        txtAreaMedicacion.setText(valorOGuion(pacienteActual.getMedicacionActual()));

        //Consentimiento RGPD
        if (pacienteActual.isConsentimientoRgpd()) {
            String textoConsentimiento = "Si";
            if (pacienteActual.getFechaConsentimiento() != null) {
                DateTimeFormatter formateadorFechaHora = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                textoConsentimiento += " (" + pacienteActual.getFechaConsentimiento().format(formateadorFechaHora) + ")";
            }
            lblConsentimientoValor.setText(textoConsentimiento);
        } else {
            lblConsentimientoValor.setText("No");
        }
    }

    // ==================== DISCAPACIDADES DEL PACIENTE ====================

    /**
     * Carga las discapacidades asignadas al paciente en la tabla
     */
    private void cargarDiscapacidadesPaciente() {
        try {
            List<PacienteDiscapacidad> discapacidades =
                    pacienteClinicoService.listarDiscapacidadesPaciente(dniPacienteActual);
            tblDiscapacidadesPaciente.setItems(FXCollections.observableArrayList(discapacidades));

            //Limpiar tabla de tratamientos al recargar discapacidades
            tblTratamientosPaciente.getItems().clear();
        } catch (ConexionException e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Error al cargar discapacidades: " + e.getMessage(), TipoMensaje.ERROR);
        }
    }

    /**
     * Carga los tratamientos asignados al paciente para una discapacidad concreta
     */
    private void cargarTratamientosPorDiscapacidad(String codDis) {
        try {
            List<PacienteTratamiento> tratamientos =
                    pacienteClinicoService.listarTratamientosPaciente(dniPacienteActual, codDis);
            tblTratamientosPaciente.setItems(FXCollections.observableArrayList(tratamientos));
        } catch (ConexionException e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Error al cargar tratamientos: " + e.getMessage(), TipoMensaje.ERROR);
        }
    }

    @FXML
    void asignarDiscapacidad(ActionEvent event) {
        try {
            //Obtener discapacidades del catalogo
            List<Discapacidad> todasDiscapacidades = catalogoService.listarDiscapacidades();

            if (todasDiscapacidades.isEmpty()) {
                VentanaUtil.mostrarVentanaInformativa(
                        "No hay discapacidades en el catalogo.", TipoMensaje.ADVERTENCIA);
                return;
            }

            //Mostrar dialogo de seleccion
            ChoiceDialog<Discapacidad> dialogo = new ChoiceDialog<>(todasDiscapacidades.get(0), todasDiscapacidades);
            dialogo.setTitle("Asignar discapacidad");
            dialogo.setHeaderText("Selecciona la discapacidad a asignar al paciente:");
            dialogo.setContentText("Discapacidad:");
            controladorVentanaOpciones.aplicarConfiguracionAScene(dialogo.getDialogPane().getScene());

            Optional<Discapacidad> resultado = dialogo.showAndWait();
            if (resultado.isPresent()) {
                pacienteClinicoService.asignarDiscapacidad(dniPacienteActual, resultado.get().getCodDis());
                VentanaUtil.mostrarVentanaInformativa(
                        "Discapacidad asignada correctamente.", TipoMensaje.EXITO);
                cargarDiscapacidadesPaciente();
                cambiosRealizados = true;
            }

        } catch (ConexionException e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Error al asignar discapacidad: " + e.getMessage(), TipoMensaje.ERROR);
        }
    }

    @FXML
    void desasignarDiscapacidad(ActionEvent event) {
        PacienteDiscapacidad seleccionada = tblDiscapacidadesPaciente.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Selecciona una discapacidad para desasignar.", TipoMensaje.ADVERTENCIA);
            return;
        }

        boolean confirmado = VentanaUtil.mostrarVentanaPregunta(
                "Se desasignara la discapacidad '" + seleccionada.getNombreDiscapacidad() +
                "' y todos sus tratamientos asociados al paciente. ¿Continuar?");

        if (confirmado) {
            try {
                pacienteClinicoService.desasignarDiscapacidad(dniPacienteActual, seleccionada.getCodDis());
                VentanaUtil.mostrarVentanaInformativa(
                        "Discapacidad desasignada correctamente.", TipoMensaje.EXITO);
                cargarDiscapacidadesPaciente();
                cambiosRealizados = true;
            } catch (ConexionException e) {
                VentanaUtil.mostrarVentanaInformativa(
                        "Error al desasignar: " + e.getMessage(), TipoMensaje.ERROR);
            }
        }
    }

    @FXML
    void subirNivel(ActionEvent event) {
        PacienteDiscapacidad seleccionada = tblDiscapacidadesPaciente.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Selecciona una discapacidad para cambiar el nivel.", TipoMensaje.ADVERTENCIA);
            return;
        }

        int nivelActual = seleccionada.getIdNivelActual();
        if (nivelActual >= NIVEL_MAXIMO) {
            VentanaUtil.mostrarVentanaInformativa(
                    "El paciente ya esta en el nivel maximo de progresion.", TipoMensaje.ADVERTENCIA);
            return;
        }

        try {
            pacienteClinicoService.cambiarNivel(dniPacienteActual, seleccionada.getCodDis(), nivelActual + 1);
            VentanaUtil.mostrarVentanaInformativa(
                    "Nivel actualizado a " + (nivelActual + 1) + ".", TipoMensaje.EXITO);
            cargarDiscapacidadesPaciente();
            cambiosRealizados = true;
        } catch (ValidacionException e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Error de validacion: " + e.getMessage(), TipoMensaje.ERROR);
        } catch (ConexionException e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Error al cambiar nivel: " + e.getMessage(), TipoMensaje.ERROR);
        }
    }

    @FXML
    void bajarNivel(ActionEvent event) {
        PacienteDiscapacidad seleccionada = tblDiscapacidadesPaciente.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Selecciona una discapacidad para cambiar el nivel.", TipoMensaje.ADVERTENCIA);
            return;
        }

        int nivelActual = seleccionada.getIdNivelActual();
        if (nivelActual <= NIVEL_MINIMO) {
            VentanaUtil.mostrarVentanaInformativa(
                    "El paciente ya esta en el nivel minimo de progresion.", TipoMensaje.ADVERTENCIA);
            return;
        }

        try {
            pacienteClinicoService.cambiarNivel(dniPacienteActual, seleccionada.getCodDis(), nivelActual - 1);
            VentanaUtil.mostrarVentanaInformativa(
                    "Nivel actualizado a " + (nivelActual - 1) + ".", TipoMensaje.EXITO);
            cargarDiscapacidadesPaciente();
            cambiosRealizados = true;
        } catch (ValidacionException e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Error de validacion: " + e.getMessage(), TipoMensaje.ERROR);
        } catch (ConexionException e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Error al cambiar nivel: " + e.getMessage(), TipoMensaje.ERROR);
        }
    }

    /**
     * Edita las notas de la discapacidad seleccionada del paciente.
     * Abre un TextInputDialog con las notas actuales para modificarlas.
     */
    @FXML
    void editarNotas(ActionEvent event) {
        PacienteDiscapacidad seleccionada = tblDiscapacidadesPaciente.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Selecciona una discapacidad para editar sus notas.", TipoMensaje.ADVERTENCIA);
            return;
        }

        TextInputDialog dialogo = new TextInputDialog(seleccionada.getNotas());
        dialogo.setTitle("Editar notas");
        dialogo.setHeaderText("Notas para '" + seleccionada.getNombreDiscapacidad() + "':");
        dialogo.setContentText("Notas:");
        controladorVentanaOpciones.aplicarConfiguracionAScene(dialogo.getDialogPane().getScene());

        Optional<String> resultado = dialogo.showAndWait();
        if (resultado.isPresent()) {
            try {
                pacienteClinicoService.actualizarNotas(
                        dniPacienteActual, seleccionada.getCodDis(), resultado.get());
                VentanaUtil.mostrarVentanaInformativa(
                        "Notas actualizadas correctamente.", TipoMensaje.EXITO);
                cargarDiscapacidadesPaciente();
                cambiosRealizados = true;
            } catch (ConexionException e) {
                VentanaUtil.mostrarVentanaInformativa(
                        "Error al actualizar notas: " + e.getMessage(), TipoMensaje.ERROR);
            }
        }
    }

    // ==================== TRATAMIENTOS DEL PACIENTE ====================

    @FXML
    void asignarTratamiento(ActionEvent event) {
        PacienteDiscapacidad disSeleccionada = tblDiscapacidadesPaciente.getSelectionModel().getSelectedItem();
        if (disSeleccionada == null) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Selecciona primero una discapacidad para asignar tratamientos.", TipoMensaje.ADVERTENCIA);
            return;
        }

        try {
            //Obtener tratamientos disponibles (filtrados por discapacidad, nivel y no asignados)
            List<Tratamiento> disponibles = pacienteClinicoService.obtenerTratamientosDisponibles(
                    dniPacienteActual, disSeleccionada.getCodDis());

            if (disponibles.isEmpty()) {
                VentanaUtil.mostrarVentanaInformativa(
                        "No hay tratamientos disponibles para esta discapacidad y nivel.", TipoMensaje.ADVERTENCIA);
                return;
            }

            ChoiceDialog<Tratamiento> dialogo = new ChoiceDialog<>(disponibles.get(0), disponibles);
            dialogo.setTitle("Asignar tratamiento");
            dialogo.setHeaderText("Tratamientos disponibles para '" + disSeleccionada.getNombreDiscapacidad() + "':");
            dialogo.setContentText("Tratamiento:");
            controladorVentanaOpciones.aplicarConfiguracionAScene(dialogo.getDialogPane().getScene());

            Optional<Tratamiento> resultado = dialogo.showAndWait();
            if (resultado.isPresent()) {
                pacienteClinicoService.asignarTratamiento(
                        dniPacienteActual, resultado.get().getCodTrat(), disSeleccionada.getCodDis());
                VentanaUtil.mostrarVentanaInformativa(
                        "Tratamiento asignado correctamente.", TipoMensaje.EXITO);
                cargarTratamientosPorDiscapacidad(disSeleccionada.getCodDis());
                cambiosRealizados = true;
            }

        } catch (ConexionException e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Error al asignar tratamiento: " + e.getMessage(), TipoMensaje.ERROR);
        }
    }

    @FXML
    void desasignarTratamiento(ActionEvent event) {
        PacienteDiscapacidad disSeleccionada = tblDiscapacidadesPaciente.getSelectionModel().getSelectedItem();
        PacienteTratamiento tratSeleccionado = tblTratamientosPaciente.getSelectionModel().getSelectedItem();

        if (disSeleccionada == null || tratSeleccionado == null) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Selecciona un tratamiento para desasignar.", TipoMensaje.ADVERTENCIA);
            return;
        }

        boolean confirmado = VentanaUtil.mostrarVentanaPregunta(
                "Se desasignara el tratamiento '" + tratSeleccionado.getNombreTratamiento() +
                "' del paciente. ¿Continuar?");

        if (confirmado) {
            try {
                pacienteClinicoService.desasignarTratamiento(
                        dniPacienteActual, tratSeleccionado.getCodTrat(), disSeleccionada.getCodDis());
                VentanaUtil.mostrarVentanaInformativa(
                        "Tratamiento desasignado correctamente.", TipoMensaje.EXITO);
                cargarTratamientosPorDiscapacidad(disSeleccionada.getCodDis());
                cambiosRealizados = true;
            } catch (ConexionException e) {
                VentanaUtil.mostrarVentanaInformativa(
                        "Error al desasignar tratamiento: " + e.getMessage(), TipoMensaje.ERROR);
            }
        }
    }

    @FXML
    void toggleVisibilidad(ActionEvent event) {
        PacienteDiscapacidad disSeleccionada = tblDiscapacidadesPaciente.getSelectionModel().getSelectedItem();
        PacienteTratamiento tratSeleccionado = tblTratamientosPaciente.getSelectionModel().getSelectedItem();

        if (disSeleccionada == null || tratSeleccionado == null) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Selecciona un tratamiento para cambiar su visibilidad.", TipoMensaje.ADVERTENCIA);
            return;
        }

        try {
            boolean nuevoEstado = !tratSeleccionado.isVisible();
            pacienteClinicoService.cambiarVisibilidad(
                    dniPacienteActual, tratSeleccionado.getCodTrat(),
                    disSeleccionada.getCodDis(), nuevoEstado);

            String mensaje = nuevoEstado ? "Tratamiento ahora visible para el paciente."
                    : "Tratamiento oculto para el paciente.";
            VentanaUtil.mostrarVentanaInformativa(mensaje, TipoMensaje.EXITO);

            cargarTratamientosPorDiscapacidad(disSeleccionada.getCodDis());
            cambiosRealizados = true;
        } catch (ConexionException e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Error al cambiar visibilidad: " + e.getMessage(), TipoMensaje.ERROR);
        }
    }

    // ==================== UTILIDADES ====================

    private String valorOGuion(String valor) {
        return (valor != null && !valor.trim().isEmpty()) ? valor : "-";
    }

    private String construirDireccionCompleta() {
        if (pacienteActual == null) {
            return "";
        }

        StringBuilder direccion = new StringBuilder();

        String calle = pacienteActual.getCalle();
        String numero = pacienteActual.getNumero();
        if (calle != null && !calle.trim().isEmpty()) {
            direccion.append(calle.trim());
            if (numero != null && !numero.trim().isEmpty() && !numero.equals("0")) {
                direccion.append(" ").append(numero.trim());
            }
        }

        String piso = pacienteActual.getPiso();
        if (piso != null && !piso.trim().isEmpty()) {
            if (direccion.length() > 0) {
                direccion.append(", ");
            }
            direccion.append(piso.trim());
        }

        String cp = pacienteActual.getCodigoPostal();
        String localidad = pacienteActual.getLocalidad();
        if ((cp != null && !cp.trim().isEmpty()) || (localidad != null && !localidad.trim().isEmpty())) {
            if (direccion.length() > 0) {
                direccion.append(" - ");
            }
            if (cp != null && !cp.trim().isEmpty()) {
                direccion.append(cp.trim()).append(" ");
            }
            if (localidad != null && !localidad.trim().isEmpty()) {
                direccion.append(localidad.trim());
            }
        }

        String provincia = pacienteActual.getProvincia();
        if (provincia != null && !provincia.trim().isEmpty()) {
            direccion.append(" (").append(provincia.trim()).append(")");
        }

        return direccion.toString().trim();
    }

    // ==================== FOTO ====================

    private void cargarFotoPaciente() {
        try {
            Image imagen = pacienteDAO.obtenerFoto(dniPacienteActual);

            if (imagen != null) {
                imgFotoPaciente.setImage(imagen);
            } else {
                cargarImagenPorDefecto();
            }
        } catch (Exception e) {
            cargarImagenPorDefecto();
        }
    }

    private void cargarImagenPorDefecto() {
        try {
            Image imagenDefault = new Image(getClass().getResourceAsStream("/com/javafx/Interface/usuario_default.png"));
            imgFotoPaciente.setImage(imagenDefault);
        } catch (Exception e) {
            imgFotoPaciente.setImage(null);
        }
    }

    // ==================== EDICION Y ACCIONES ====================

    @FXML
    void abrirVentanaEditar(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaAgregarPaciente.fxml"));
            Parent root = loader.load();

            controladorAgregarPaciente controlador = loader.getController();
            controlador.cargarDatosParaEdicion(pacienteActual);

            Scene scene = new Scene(root);
            controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

            Stage stage = new Stage();
            stage.setTitle("Modificar Paciente");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            VentanaUtil.establecerIconoVentana(stage);
            stage.showAndWait();

            //Recargar datos tras cerrar ventana de edicion
            Paciente pacienteRecargado = pacienteDAO.buscarPorDni(dniPacienteActual);

            if (pacienteRecargado != null) {
                cargarDatosPaciente(dniPacienteActual);
                cambiosRealizados = true;
            } else {
                VentanaUtil.mostrarVentanaInformativa(
                        "El paciente ha sido modificado. Se cerrara esta ventana.",
                        TipoMensaje.INFORMACION
                );
                cambiosRealizados = true;
                cerrarVentana(event);
            }

        } catch (Exception e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Error al abrir la ventana de edicion.",
                    TipoMensaje.ERROR
            );
        }
    }

    @FXML
    void descargarInformePDF(ActionEvent event) {
        if (pacienteActual == null) {
            VentanaUtil.mostrarVentanaInformativa(
                    "No hay datos de paciente cargados.",
                    TipoMensaje.ERROR
            );
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar informe del paciente");
        fileChooser.setInitialFileName("Informe_" + pacienteActual.getDni() + ".pdf");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivos PDF", "*.pdf")
        );

        File directorioInicial = new File(System.getProperty("user.home"));
        if (directorioInicial.exists()) {
            fileChooser.setInitialDirectory(directorioInicial);
        }

        Stage stage = (Stage) btnDescargarInforme.getScene().getWindow();
        File archivoDestino = fileChooser.showSaveDialog(stage);

        if (archivoDestino != null) {
            try {
                String rutaCompleta = archivoDestino.getAbsolutePath();

                if (!rutaCompleta.toLowerCase().endsWith(".pdf")) {
                    rutaCompleta += ".pdf";
                }

                boolean exito = InformeService.generarInformePacienteEnRuta(
                        pacienteActual.getDni(),
                        rutaCompleta,
                        false
                );

                if (exito) {
                    VentanaUtil.mostrarVentanaInformativa(
                            "El informe PDF se ha guardado correctamente en:\n" + rutaCompleta,
                            TipoMensaje.EXITO
                    );
                } else {
                    VentanaUtil.mostrarVentanaInformativa(
                            "No se pudo generar el informe PDF.",
                            TipoMensaje.ERROR
                    );
                }

            } catch (Exception e) {
                VentanaUtil.mostrarVentanaInformativa(
                        "Error inesperado al generar el informe.\nDetalles: " + e.getMessage(),
                        TipoMensaje.ERROR
                );
            }
        }
    }

    @FXML
    void cerrarVentana(ActionEvent event) {
        Stage stage = (Stage) btnAceptar.getScene().getWindow();
        stage.close();
    }

    public boolean hayCambiosRealizados() {
        return cambiosRealizados;
    }
}
