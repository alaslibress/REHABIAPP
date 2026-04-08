package com.javafx.Interface;

import com.javafx.Clases.Paciente;
import com.javafx.Clases.Sanitario;
import com.javafx.Clases.VentanaUtil;
import com.javafx.Clases.VentanaUtil.TipoMensaje;
import com.javafx.DAO.PacienteDAO;
import com.javafx.DAO.SanitarioDAO;
import com.javafx.excepcion.ConexionException;
import com.javafx.excepcion.DuplicadoException;
import com.javafx.excepcion.ValidacionException;
import com.javafx.service.PacienteService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

/**
 * Controlador para la ventana de agregar y modificar paciente
 * Gestiona tanto la creacion de nuevos pacientes como la edicion de existentes
 */
public class controladorAgregarPaciente {

    @FXML
    private Button btnAgregarFoto;

    @FXML
    private Button btnCancelar;

    @FXML
    private Button btnGuardar;

    @FXML
    private ComboBox<Sanitario> cmbSanitario;

    @FXML
    private ToggleGroup grupoProtesis;

    @FXML
    private ImageView imgFotoPaciente;

    @FXML
    private Label lblTituloVentana;

    @FXML
    private RadioButton radioProtesisNo;

    @FXML
    private RadioButton radioProtesisSi;

    @FXML
    private Spinner<Integer> spinnerEdad;

    @FXML
    private TextField txtApellidos;

    @FXML
    private TextField txtDNI;

    @FXML
    private TextField txtEmail;

    @FXML
    private TextField txtNSS;

    @FXML
    private TextField txtNombre;

    @FXML
    private TextField txtTelefono1;

    @FXML
    private TextField txtTelefono2;

    //Campos de direccion
    @FXML
    private TextField txtCalle;

    @FXML
    private TextField txtNumero;

    @FXML
    private TextField txtPiso;

    @FXML
    private TextField txtCodigoPostal;

    @FXML
    private TextField txtLocalidad;

    @FXML
    private TextField txtProvincia;

    //Campos clinicos v2
    @FXML
    private ComboBox<String> cmbSexo;

    @FXML
    private DatePicker dpFechaNacimiento;

    @FXML
    private TextArea txtAreaAlergias;

    @FXML
    private TextArea txtAreaAntecedentes;

    @FXML
    private TextArea txtAreaMedicacion;

    @FXML
    private CheckBox chkConsentimientoRgpd;

    //Variables para la gestion de archivos
    private File archivoFoto;

    //DNI del sanitario que esta creando/editando el paciente
    private String dniSanitarioActual;

    // REFACTORIZADO: Usar capa de servicio en lugar de DAO directamente
    // (Se mantiene PacienteDAO solo para operaciones de foto que aún no están en el servicio)
    private PacienteService pacienteService;
    private PacienteDAO pacienteDAO; // TODO: Mover operaciones de foto al servicio
    private SanitarioDAO sanitarioDAO;

    //Soporte de validacion de ControlsFX
    private ValidationSupport validationSupport;

    //Indica si estamos en modo edicion (true) o creacion (false)
    private boolean modoEdicion = false;

    //DNI original del paciente cuando estamos en modo edicion
    private String dniOriginal;

    //Lista de sanitarios para el ComboBox
    private ObservableList<Sanitario> listaSanitarios;

    /**
     * Metodo initialize se ejecuta automaticamente al cargar el FXML
     */
    @FXML
    public void initialize() {
        //REFACTORIZADO: Inicializar servicio
        pacienteService = new PacienteService();
        pacienteDAO = new PacienteDAO(); // Temporal para fotos
        sanitarioDAO = new SanitarioDAO();

        //Configurar el spinner de edad (0-120 años)
        SpinnerValueFactory<Integer> valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 120, 18);
        spinnerEdad.setValueFactory(valueFactory);

        //Seleccionar "No" por defecto en protesis
        radioProtesisNo.setSelected(true);

        //Configurar ComboBox de sexo
        cmbSexo.setItems(FXCollections.observableArrayList("M", "F", "O"));

        //Inicializar soporte de validacion
        validationSupport = new ValidationSupport();

        //Cargar lista de sanitarios en el ComboBox
        cargarSanitarios();

        //Configurar validaciones en un hilo separado
        Platform.runLater(this::configurarValidaciones);
    }

    /**
     * Carga la lista de sanitarios en el ComboBox
     */
    private void cargarSanitarios() {
        listaSanitarios = FXCollections.observableArrayList();

        List<Sanitario> sanitarios = sanitarioDAO.listarTodos();
        listaSanitarios.addAll(sanitarios);

        cmbSanitario.setItems(listaSanitarios);

        //Configurar como se muestra cada sanitario
        cmbSanitario.setCellFactory(param -> new ListCell<Sanitario>() {
            @Override
            protected void updateItem(Sanitario sanitario, boolean empty) {
                super.updateItem(sanitario, empty);
                if (empty || sanitario == null) {
                    setText(null);
                } else {
                    setText(sanitario.getDni() + " - " + sanitario.getNombreCompleto());
                }
            }
        });

        cmbSanitario.setConverter(new StringConverter<Sanitario>() {
            @Override
            public String toString(Sanitario sanitario) {
                if (sanitario == null) {
                    return null;
                }
                return sanitario.getDni() + " - " + sanitario.getNombreCompleto();
            }

            @Override
            public Sanitario fromString(String string) {
                return null;
            }
        });
    }

    /**
     * Configura las validaciones de los campos del formulario
     */
    private void configurarValidaciones() {
        validationSupport.registerValidator(txtNombre,
                Validator.createEmptyValidator("El nombre es obligatorio"));

        validationSupport.registerValidator(txtApellidos,
                Validator.createEmptyValidator("Los apellidos son obligatorios"));

        validationSupport.registerValidator(txtDNI,
                Validator.createRegexValidator(
                        "El DNI debe tener 8 digitos y una letra",
                        "^[0-9]{8}[A-Za-z]$",
                        org.controlsfx.validation.Severity.ERROR));

        validationSupport.registerValidator(txtEmail,
                Validator.createRegexValidator(
                        "El formato del email no es valido",
                        "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$",
                        org.controlsfx.validation.Severity.ERROR));

        validationSupport.registerValidator(txtNSS,
                Validator.createRegexValidator(
                        "El NSS debe tener 12 digitos",
                        "^[0-9]{12}$",
                        org.controlsfx.validation.Severity.ERROR));

        validationSupport.registerValidator(txtTelefono1,
                Validator.createRegexValidator(
                        "El telefono debe tener 9 digitos",
                        "^[0-9]{9}$",
                        org.controlsfx.validation.Severity.ERROR));

        //Validacion de campos de direccion obligatorios
        validationSupport.registerValidator(txtCalle,
                Validator.createEmptyValidator("La calle es obligatoria"));

        validationSupport.registerValidator(txtCodigoPostal,
                Validator.createRegexValidator(
                        "El codigo postal debe tener 5 digitos",
                        "^[0-9]{5}$",
                        org.controlsfx.validation.Severity.ERROR));

        validationSupport.registerValidator(txtLocalidad,
                Validator.createEmptyValidator("La localidad es obligatoria"));
    }

    /**
     * Establece el DNI del sanitario y lo preselecciona en el ComboBox
     */
    public void setDniSanitario(String dniSanitario) {
        this.dniSanitarioActual = dniSanitario;

        if (dniSanitario != null && !dniSanitario.isEmpty()) {
            for (Sanitario sanitario : listaSanitarios) {
                if (sanitario.getDni().equals(dniSanitario)) {
                    cmbSanitario.getSelectionModel().select(sanitario);
                    break;
                }
            }
        }
    }

    /**
     * Carga los datos de un paciente existente para editar
     */
    public void cargarDatosParaEdicion(Paciente paciente) {
        modoEdicion = true;
        dniOriginal = paciente.getDni();
        dniSanitarioActual = paciente.getDniSanitario();

        lblTituloVentana.setText("Modificar Paciente");
        btnGuardar.setText("Guardar");

        //Seleccionar sanitario
        if (paciente.getDniSanitario() != null) {
            for (Sanitario sanitario : listaSanitarios) {
                if (sanitario.getDni().equals(paciente.getDniSanitario())) {
                    cmbSanitario.getSelectionModel().select(sanitario);
                    break;
                }
            }
        }

        //Datos personales
        txtNombre.setText(paciente.getNombre());
        txtApellidos.setText(paciente.getApellidos());
        txtDNI.setText(paciente.getDni());
        spinnerEdad.getValueFactory().setValue(paciente.getEdad());
        txtEmail.setText(paciente.getEmail() != null ? paciente.getEmail() : "");
        txtNSS.setText(paciente.getNumSS() != null ? paciente.getNumSS() : "");
        // Los campos legacy de discapacidad/tratamiento/estado han sido eliminados del modelo.
        // Se gestionan ahora en la ficha del paciente (controladorVentanaPacienteListar).

        //Telefonos
        txtTelefono1.setText(paciente.getTelefono1() != null ? paciente.getTelefono1() : "");
        txtTelefono2.setText(paciente.getTelefono2() != null ? paciente.getTelefono2() : "");

        //Campos de direccion postal
        txtCalle.setText(paciente.getCalle() != null ? paciente.getCalle() : "");
        txtNumero.setText(paciente.getNumero() != null ? paciente.getNumero() : "");
        txtPiso.setText(paciente.getPiso() != null ? paciente.getPiso() : "");
        txtCodigoPostal.setText(paciente.getCodigoPostal() != null ? paciente.getCodigoPostal() : "");
        txtLocalidad.setText(paciente.getLocalidad() != null ? paciente.getLocalidad() : "");
        txtProvincia.setText(paciente.getProvincia() != null ? paciente.getProvincia() : "");

        //Protesis
        if (paciente.tieneProtesis()) {
            radioProtesisSi.setSelected(true);
        } else {
            radioProtesisNo.setSelected(true);
        }

        //Campos clinicos v2
        if (paciente.getSexo() != null && !paciente.getSexo().isEmpty()) {
            cmbSexo.setValue(paciente.getSexo());
        }
        dpFechaNacimiento.setValue(paciente.getFechaNacimiento());
        txtAreaAlergias.setText(paciente.getAlergias() != null ? paciente.getAlergias() : "");
        txtAreaAntecedentes.setText(paciente.getAntecedentes() != null ? paciente.getAntecedentes() : "");
        txtAreaMedicacion.setText(paciente.getMedicacionActual() != null ? paciente.getMedicacionActual() : "");
        chkConsentimientoRgpd.setSelected(paciente.isConsentimientoRgpd());

        //Cargar foto
        cargarFotoDesdeBD(paciente.getDni());
    }

    /**
     * Carga la foto del paciente desde la base de datos
     */
    private void cargarFotoDesdeBD(String dniPaciente) {
        try {
            Image imagen = pacienteDAO.obtenerFoto(dniPaciente);
            if (imagen != null) {
                imgFotoPaciente.setImage(imagen);
            }
        } catch (Exception e) {
            System.err.println("Error al cargar foto desde BD: " + e.getMessage());
        }
    }

    /**
     * Guarda el paciente (crea nuevo o actualiza existente)
     */
    @FXML
    void guardarPaciente(ActionEvent event) {
        if (!validarCampos()) {
            return;
        }

        Sanitario sanitarioSeleccionado = cmbSanitario.getSelectionModel().getSelectedItem();
        String dniSanitario = sanitarioSeleccionado.getDni();

        String[] apellidosSeparados = separarApellidos(txtApellidos.getText().trim());
        boolean protesis = radioProtesisSi.isSelected();

        //Crear objeto Paciente
        Paciente paciente = new Paciente(
                txtDNI.getText().trim().toUpperCase(),
                txtNombre.getText().trim(),
                apellidosSeparados[0],
                apellidosSeparados[1],
                spinnerEdad.getValue(),
                txtEmail.getText().trim(),
                txtNSS.getText().trim(),
                protesis,
                dniSanitario
        );

        //Configurar telefonos
        paciente.setTelefono1(txtTelefono1.getText().trim());
        paciente.setTelefono2(txtTelefono2.getText().trim());

        //Configurar campos de direccion postal
        paciente.setCalle(txtCalle.getText().trim());
        paciente.setNumero(txtNumero.getText().trim());
        paciente.setPiso(txtPiso.getText().trim());
        paciente.setCodigoPostal(txtCodigoPostal.getText().trim());
        paciente.setLocalidad(txtLocalidad.getText().trim());
        paciente.setProvincia(txtProvincia.getText().trim());

        //Configurar campos clinicos v2
        paciente.setSexo(cmbSexo.getValue() != null ? cmbSexo.getValue() : "");
        paciente.setFechaNacimiento(dpFechaNacimiento.getValue());
        paciente.setAlergias(txtAreaAlergias.getText() != null ? txtAreaAlergias.getText().trim() : "");
        paciente.setAntecedentes(txtAreaAntecedentes.getText() != null ? txtAreaAntecedentes.getText().trim() : "");
        paciente.setMedicacionActual(txtAreaMedicacion.getText() != null ? txtAreaMedicacion.getText().trim() : "");
        paciente.setConsentimientoRgpd(chkConsentimientoRgpd.isSelected());

        try {
            if (modoEdicion) {
                actualizarPacienteExistente(paciente);
            } else {
                insertarNuevoPaciente(paciente);
            }
            cerrarVentana(event);
        } catch (DuplicadoException e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Dato duplicado: " + e.getMessage(),
                    TipoMensaje.ERROR
            );
            enfocarCampoDuplicado(e.getCampo());
        } catch (ValidacionException e) {
            VentanaUtil.mostrarVentanaInformativa(
                    e.getMessage(),
                    TipoMensaje.ERROR
            );
        } catch (ConexionException e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Error de conexion con la base de datos.",
                    TipoMensaje.ERROR
            );
        }
    }

    /**
     * Inserta un nuevo paciente en la base de datos.
     * La operacion es atomica: paciente + telefonos + foto en una sola transaccion.
     * Lanza excepciones tipadas que se capturan en guardarPaciente().
     */
    private void insertarNuevoPaciente(Paciente paciente) {
        pacienteService.insertar(
                paciente,
                paciente.getTelefono1(),
                paciente.getTelefono2(),
                archivoFoto
        );

        VentanaUtil.mostrarVentanaInformativa(
                "El paciente se ha registrado correctamente.",
                TipoMensaje.EXITO
        );
    }

    /**
     * Actualiza un paciente existente en la base de datos.
     * La operacion es atomica: paciente + telefonos + foto en una sola transaccion.
     * Lanza excepciones tipadas que se capturan en guardarPaciente().
     */
    private void actualizarPacienteExistente(Paciente paciente) {
        pacienteService.actualizar(
                paciente,
                dniOriginal,
                paciente.getTelefono1(),
                paciente.getTelefono2(),
                archivoFoto
        );

        VentanaUtil.mostrarVentanaInformativa(
                "Los datos del paciente se han actualizado correctamente.",
                TipoMensaje.EXITO
        );
    }

    /**
     * Enfoca el campo correspondiente al dato duplicado detectado
     */
    private void enfocarCampoDuplicado(String campo) {
        if (campo == null) return;

        switch (campo.toLowerCase()) {
            case "dni" -> txtDNI.requestFocus();
            case "email" -> txtEmail.requestFocus();
            case "nss", "num_ss" -> txtNSS.requestFocus();
        }
    }

    /**
     * Valida que todos los campos obligatorios esten correctos
     */
    private boolean validarCampos() {
        //Sanitario
        if (cmbSanitario.getSelectionModel().getSelectedItem() == null) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Debe seleccionar un sanitario asignado.",
                    TipoMensaje.ADVERTENCIA
            );
            cmbSanitario.requestFocus();
            return false;
        }

        //Nombre
        if (txtNombre.getText().trim().isEmpty()) {
            VentanaUtil.mostrarVentanaInformativa(
                    "El nombre es obligatorio.",
                    TipoMensaje.ADVERTENCIA
            );
            txtNombre.requestFocus();
            return false;
        }

        //Apellidos
        if (txtApellidos.getText().trim().isEmpty()) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Los apellidos son obligatorios.",
                    TipoMensaje.ADVERTENCIA
            );
            txtApellidos.requestFocus();
            return false;
        }

        //DNI
        String dni = txtDNI.getText().trim();
        if (!dni.matches("^[0-9]{8}[A-Za-z]$")) {
            VentanaUtil.mostrarVentanaInformativa(
                    "El DNI debe tener 8 numeros seguidos de una letra.",
                    TipoMensaje.ADVERTENCIA
            );
            txtDNI.requestFocus();
            return false;
        }

        //Edad
        if (spinnerEdad.getValue() < 1 || spinnerEdad.getValue() > 120) {
            VentanaUtil.mostrarVentanaInformativa(
                    "La edad debe estar entre 1 y 120 años.",
                    TipoMensaje.ADVERTENCIA
            );
            spinnerEdad.requestFocus();
            return false;
        }

        //Email
        String email = txtEmail.getText().trim();
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            VentanaUtil.mostrarVentanaInformativa(
                    "El formato del email no es valido.",
                    TipoMensaje.ADVERTENCIA
            );
            txtEmail.requestFocus();
            return false;
        }

        //NSS
        String nss = txtNSS.getText().trim();
        if (!nss.matches("^[0-9]{12}$")) {
            VentanaUtil.mostrarVentanaInformativa(
                    "El numero de seguridad social debe tener 12 digitos.",
                    TipoMensaje.ADVERTENCIA
            );
            txtNSS.requestFocus();
            return false;
        }

        //Telefono 1
        String tel1 = txtTelefono1.getText().trim();
        if (!tel1.matches("^[0-9]{9}$")) {
            VentanaUtil.mostrarVentanaInformativa(
                    "El telefono 1 es obligatorio y debe tener 9 digitos.",
                    TipoMensaje.ADVERTENCIA
            );
            txtTelefono1.requestFocus();
            return false;
        }

        //Telefono 2 (opcional)
        String tel2 = txtTelefono2.getText().trim();
        if (!tel2.isEmpty() && !tel2.matches("^[0-9]{9}$")) {
            VentanaUtil.mostrarVentanaInformativa(
                    "El telefono 2 debe tener 9 digitos.",
                    TipoMensaje.ADVERTENCIA
            );
            txtTelefono2.requestFocus();
            return false;
        }

        //Calle (obligatoria)
        if (txtCalle.getText().trim().isEmpty()) {
            VentanaUtil.mostrarVentanaInformativa(
                    "La calle es obligatoria.",
                    TipoMensaje.ADVERTENCIA
            );
            txtCalle.requestFocus();
            return false;
        }

        //Codigo postal (obligatorio, 5 digitos)
        String cp = txtCodigoPostal.getText().trim();
        if (!cp.matches("^[0-9]{5}$")) {
            VentanaUtil.mostrarVentanaInformativa(
                    "El codigo postal debe tener 5 digitos.",
                    TipoMensaje.ADVERTENCIA
            );
            txtCodigoPostal.requestFocus();
            return false;
        }

        //Localidad (obligatoria)
        if (txtLocalidad.getText().trim().isEmpty()) {
            VentanaUtil.mostrarVentanaInformativa(
                    "La localidad es obligatoria.",
                    TipoMensaje.ADVERTENCIA
            );
            txtLocalidad.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Separa los apellidos en primer y segundo apellido
     */
    private String[] separarApellidos(String apellidosCompletos) {
        String[] resultado = new String[2];
        String[] partes = apellidosCompletos.split(" ", 2);

        resultado[0] = partes[0];
        resultado[1] = partes.length > 1 ? partes[1] : "";

        return resultado;
    }

    /**
     * Abre un dialogo para seleccionar la foto del paciente
     */
    @FXML
    void agregarFotoPaciente(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar foto del paciente");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imagenes", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        Stage stage = (Stage) btnAgregarFoto.getScene().getWindow();
        File archivoSeleccionado = fileChooser.showOpenDialog(stage);

        if (archivoSeleccionado != null) {
            archivoFoto = archivoSeleccionado;
            try {
                Image imagen = new Image(archivoSeleccionado.toURI().toString());
                imgFotoPaciente.setImage(imagen);
            } catch (Exception e) {
                VentanaUtil.mostrarVentanaInformativa(
                        "No se pudo cargar la imagen seleccionada.",
                        TipoMensaje.ERROR
                );
                archivoFoto = null;
            }
        }
    }

    /**
     * Cierra la ventana sin guardar cambios
     */
    @FXML
    void cerrarVentana(ActionEvent event) {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }
}