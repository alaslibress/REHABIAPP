package com.javafx.Interface;

import com.javafx.Clases.Paciente;
import com.javafx.Clases.Sanitario;
import com.javafx.Clases.VentanaUtil;
import com.javafx.Clases.VentanaUtil.TipoMensaje;
import com.javafx.DAO.PacienteDAO;
import com.javafx.DAO.SanitarioDAO;
import com.javafx.excepcion.ConexionException;
import com.javafx.excepcion.DuplicadoException;
import com.javafx.excepcion.RehabiAppException;
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
import java.time.LocalDateTime;
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
    private CheckBox chkConsentimientoRgpd;

    @FXML
    private ComboBox<Sanitario> cmbSanitario;

    @FXML
    private ComboBox<String> cmbSexo;

    @FXML
    private DatePicker dpFechaNacimiento;

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
    private TextArea txtAreaAlergias;

    @FXML
    private TextArea txtAreaAntecedentes;

    @FXML
    private TextArea txtAreaEstado;

    @FXML
    private TextArea txtAreaMedicacion;

    @FXML
    private TextArea txtAreaTratamiento;

    @FXML
    private TextField txtDNI;

    @FXML
    private TextField txtDiscapacidad;

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

    //Variables para la gestion de archivos
    private File archivoFoto;

    //DNI del sanitario que esta creando/editando el paciente
    private String dniSanitarioActual;

    private PacienteService pacienteService;
    private PacienteDAO pacienteDAO;
    private SanitarioDAO sanitarioDAO;

    //Soporte de validacion de ControlsFX
    private ValidationSupport validationSupport;

    //Indica si estamos en modo edicion (true) o creacion (false)
    private boolean modoEdicion = false;

    //DNI original del paciente cuando estamos en modo edicion
    private String dniOriginal;

    //Lista de sanitarios para el ComboBox
    private ObservableList<Sanitario> listaSanitarios;

    @FXML
    public void initialize() {
        pacienteService = new PacienteService();
        pacienteDAO = new PacienteDAO();
        sanitarioDAO = new SanitarioDAO();

        SpinnerValueFactory<Integer> valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 120, 18);
        spinnerEdad.setValueFactory(valueFactory);

        cmbSexo.setItems(FXCollections.observableArrayList("Masculino", "Femenino", "Otro"));

        radioProtesisNo.setSelected(true);

        validationSupport = new ValidationSupport();

        cargarSanitarios();

        Platform.runLater(this::configurarValidaciones);
    }

    private void cargarSanitarios() {
        listaSanitarios = FXCollections.observableArrayList();

        List<Sanitario> sanitarios = sanitarioDAO.listarTodos();
        listaSanitarios.addAll(sanitarios);

        cmbSanitario.setItems(listaSanitarios);

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

        validationSupport.registerValidator(txtCalle,
                Validator.createEmptyValidator("La calle es obligatoria"));

        validationSupport.registerValidator(txtNumero,
                Validator.createRegexValidator(
                        "El numero es obligatorio y debe ser numerico",
                        "^[0-9]+$",
                        org.controlsfx.validation.Severity.ERROR));

        validationSupport.registerValidator(txtCodigoPostal,
                Validator.createRegexValidator(
                        "El codigo postal debe tener 5 digitos",
                        "^[0-9]{5}$",
                        org.controlsfx.validation.Severity.ERROR));

        validationSupport.registerValidator(txtLocalidad,
                Validator.createEmptyValidator("La localidad es obligatoria"));

        validationSupport.registerValidator(txtProvincia,
                Validator.createEmptyValidator("La provincia es obligatoria"));
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

        if (paciente.getDniSanitario() != null) {
            for (Sanitario sanitario : listaSanitarios) {
                if (sanitario.getDni().equals(paciente.getDniSanitario())) {
                    cmbSanitario.getSelectionModel().select(sanitario);
                    break;
                }
            }
        }

        txtNombre.setText(paciente.getNombre());
        txtApellidos.setText(paciente.getApellidos());
        txtDNI.setText(paciente.getDni());
        spinnerEdad.getValueFactory().setValue(paciente.getEdad());
        txtEmail.setText(paciente.getEmail() != null ? paciente.getEmail() : "");
        txtNSS.setText(paciente.getNumSS() != null ? paciente.getNumSS() : "");
        txtDiscapacidad.setText(paciente.getDiscapacidad() != null ? paciente.getDiscapacidad() : "");
        txtAreaTratamiento.setText(paciente.getTratamiento() != null ? paciente.getTratamiento() : "");
        txtAreaEstado.setText(paciente.getEstadoTratamiento() != null ? paciente.getEstadoTratamiento() : "");

        txtTelefono1.setText(paciente.getTelefono1() != null ? paciente.getTelefono1() : "");
        txtTelefono2.setText(paciente.getTelefono2() != null ? paciente.getTelefono2() : "");

        txtCalle.setText(paciente.getCalle() != null ? paciente.getCalle() : "");
        txtNumero.setText(paciente.getNumero() != null ? paciente.getNumero() : "");
        txtPiso.setText(paciente.getPiso() != null ? paciente.getPiso() : "");
        txtCodigoPostal.setText(paciente.getCodigoPostal() != null ? paciente.getCodigoPostal() : "");
        txtLocalidad.setText(paciente.getLocalidad() != null ? paciente.getLocalidad() : "");
        txtProvincia.setText(paciente.getProvincia() != null ? paciente.getProvincia() : "");

        if (paciente.tieneProtesis()) {
            radioProtesisSi.setSelected(true);
        } else {
            radioProtesisNo.setSelected(true);
        }

        String sexoBD = paciente.getSexo();
        if (sexoBD != null) {
            switch (sexoBD) {
                case "M": cmbSexo.setValue("Masculino"); break;
                case "F": cmbSexo.setValue("Femenino"); break;
                case "O": cmbSexo.setValue("Otro"); break;
            }
        }
        dpFechaNacimiento.setValue(paciente.getFechaNacimiento());
        txtAreaAlergias.setText(paciente.getAlergias() != null ? paciente.getAlergias() : "");
        txtAreaAntecedentes.setText(paciente.getAntecedentes() != null ? paciente.getAntecedentes() : "");
        txtAreaMedicacion.setText(paciente.getMedicacionActual() != null ? paciente.getMedicacionActual() : "");
        chkConsentimientoRgpd.setSelected(paciente.isConsentimientoRgpd());

        cargarFotoDesdeBD(paciente.getDni());
    }

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

    @FXML
    void guardarPaciente(ActionEvent event) {
        try {
            validarCampos();
        } catch (ValidacionException e) {
            VentanaUtil.mostrarVentanaInformativa(e.getMessage(), TipoMensaje.ADVERTENCIA);
            return;
        }

        Sanitario sanitarioSeleccionado = cmbSanitario.getSelectionModel().getSelectedItem();
        String dniSanitario = sanitarioSeleccionado.getDni();

        String[] apellidosSeparados = separarApellidos(txtApellidos.getText().trim());
        int numProtesis = radioProtesisSi.isSelected() ? 1 : 0;

        Paciente paciente = new Paciente(
                txtDNI.getText().trim().toUpperCase(),
                txtNombre.getText().trim(),
                apellidosSeparados[0],
                apellidosSeparados[1],
                spinnerEdad.getValue(),
                txtEmail.getText().trim(),
                txtNSS.getText().trim(),
                txtDiscapacidad.getText().trim(),
                txtAreaTratamiento.getText().trim(),
                txtAreaEstado.getText().trim(),
                numProtesis,
                dniSanitario
        );

        paciente.setTelefono1(txtTelefono1.getText().trim());
        paciente.setTelefono2(txtTelefono2.getText().trim());

        paciente.setCalle(txtCalle.getText().trim());
        paciente.setNumero(txtNumero.getText().trim());
        paciente.setPiso(txtPiso.getText().trim());
        paciente.setCodigoPostal(txtCodigoPostal.getText().trim());
        paciente.setLocalidad(txtLocalidad.getText().trim());
        paciente.setProvincia(txtProvincia.getText().trim());

        String sexoSeleccionado = cmbSexo.getValue();
        if (sexoSeleccionado != null) {
            switch (sexoSeleccionado) {
                case "Masculino": paciente.setSexo("M"); break;
                case "Femenino": paciente.setSexo("F"); break;
                case "Otro": paciente.setSexo("O"); break;
            }
        }
        paciente.setFechaNacimiento(dpFechaNacimiento.getValue());
        paciente.setAlergias(txtAreaAlergias.getText().trim());
        paciente.setAntecedentes(txtAreaAntecedentes.getText().trim());
        paciente.setMedicacionActual(txtAreaMedicacion.getText().trim());
        paciente.setConsentimientoRgpd(chkConsentimientoRgpd.isSelected());
        if (chkConsentimientoRgpd.isSelected()) {
            paciente.setFechaConsentimiento(LocalDateTime.now());
        }

        boolean exito;
        if (modoEdicion) {
            exito = actualizarPacienteExistente(paciente);
        } else {
            exito = insertarNuevoPaciente(paciente);
        }

        if (exito) {
            cerrarVentana(event);
        }
    }

    /**
     * Inserta un nuevo paciente. Valida unicidad antes de insertar
     * y captura excepciones especificas para mostrar mensajes claros.
     */
    private boolean insertarNuevoPaciente(Paciente paciente) {
        //Validar unicidad antes de intentar insertar (para mensajes mas claros)
        try {
            if (pacienteDAO.existeDni(paciente.getDni())) {
                VentanaUtil.mostrarVentanaInformativa(
                        "Ya existe un paciente registrado con ese DNI.",
                        TipoMensaje.ERROR
                );
                txtDNI.requestFocus();
                return false;
            }

            if (pacienteDAO.existeEmail(paciente.getEmail())) {
                VentanaUtil.mostrarVentanaInformativa(
                        "Ya existe un paciente registrado con ese email.",
                        TipoMensaje.ERROR
                );
                txtEmail.requestFocus();
                return false;
            }

            if (pacienteDAO.existeNumSS(paciente.getNumSS())) {
                VentanaUtil.mostrarVentanaInformativa(
                        "Ya existe un paciente con ese numero de seguridad social.",
                        TipoMensaje.ERROR
                );
                txtNSS.requestFocus();
                return false;
            }
        } catch (ConexionException e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Error de conexion con la base de datos.",
                    TipoMensaje.ERROR
            );
            return false;
        }

        try {
            //Insertar paciente + telefonos + foto en una sola transaccion atomica
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
            return true;

        } catch (DuplicadoException e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Ya existe un registro con " + e.getCampo() + " duplicado.",
                    TipoMensaje.ERROR
            );
            return false;

        } catch (ConexionException e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Error de conexion con la base de datos.",
                    TipoMensaje.ERROR
            );
            return false;

        } catch (RehabiAppException e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Error: " + e.getMessage(),
                    TipoMensaje.ERROR
            );
            return false;
        }
    }

    /**
     * Actualiza un paciente existente. Valida unicidad y captura excepciones.
     */
    private boolean actualizarPacienteExistente(Paciente paciente) {
        try {
            if (!paciente.getDni().equals(dniOriginal)) {
                if (pacienteDAO.existeDni(paciente.getDni())) {
                    VentanaUtil.mostrarVentanaInformativa(
                            "Ya existe otro paciente con ese DNI.",
                            TipoMensaje.ERROR
                    );
                    txtDNI.requestFocus();
                    return false;
                }
            }

            if (pacienteDAO.existeEmailExcluyendoDni(paciente.getEmail(), dniOriginal)) {
                VentanaUtil.mostrarVentanaInformativa(
                        "Ya existe otro paciente con ese email.",
                        TipoMensaje.ERROR
                );
                txtEmail.requestFocus();
                return false;
            }

            if (pacienteDAO.existeNumSSExcluyendoDni(paciente.getNumSS(), dniOriginal)) {
                VentanaUtil.mostrarVentanaInformativa(
                        "Ya existe otro paciente con ese numero de seguridad social.",
                        TipoMensaje.ERROR
                );
                txtNSS.requestFocus();
                return false;
            }
        } catch (ConexionException e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Error de conexion con la base de datos.",
                    TipoMensaje.ERROR
            );
            return false;
        }

        try {
            //Actualizar paciente + telefonos + foto en una sola transaccion atomica
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
            return true;

        } catch (DuplicadoException e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Ya existe un registro con " + e.getCampo() + " duplicado.",
                    TipoMensaje.ERROR
            );
            return false;

        } catch (ConexionException e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Error de conexion con la base de datos.",
                    TipoMensaje.ERROR
            );
            return false;

        } catch (RehabiAppException e) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Error: " + e.getMessage(),
                    TipoMensaje.ERROR
            );
            return false;
        }
    }

    /**
     * Valida todos los campos obligatorios del formulario.
     * Lanza ValidacionException indicando el campo que fallo.
     */
    private void validarCampos() {
        if (cmbSanitario.getSelectionModel().getSelectedItem() == null) {
            cmbSanitario.requestFocus();
            throw new ValidacionException(
                    "Debe seleccionar un sanitario asignado.", "sanitario");
        }

        if (txtNombre.getText().trim().isEmpty()) {
            txtNombre.requestFocus();
            throw new ValidacionException(
                    "El nombre es obligatorio.", "nombre");
        }

        String apellidos = txtApellidos.getText().trim();
        if (apellidos.isEmpty()) {
            txtApellidos.requestFocus();
            throw new ValidacionException(
                    "Los apellidos son obligatorios.", "apellidos");
        }

        //La BD exige apellido1 y apellido2 NOT NULL con contenido
        String[] apellidosSep = apellidos.split(" ", 2);
        if (apellidosSep.length < 2 || apellidosSep[1].trim().isEmpty()) {
            txtApellidos.requestFocus();
            throw new ValidacionException(
                    "Debe introducir los dos apellidos separados por un espacio.", "apellidos");
        }

        String dni = txtDNI.getText().trim();
        if (!dni.matches("^[0-9]{8}[A-Za-z]$")) {
            txtDNI.requestFocus();
            throw new ValidacionException(
                    "El DNI debe tener 8 numeros seguidos de una letra.", "dni");
        }

        if (spinnerEdad.getValue() < 1 || spinnerEdad.getValue() > 120) {
            spinnerEdad.requestFocus();
            throw new ValidacionException(
                    "La edad debe estar entre 1 y 120 anios.", "edad");
        }

        String email = txtEmail.getText().trim();
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            txtEmail.requestFocus();
            throw new ValidacionException(
                    "El formato del email no es valido.", "email");
        }

        String nss = txtNSS.getText().trim();
        if (!nss.matches("^[0-9]{12}$")) {
            txtNSS.requestFocus();
            throw new ValidacionException(
                    "El numero de seguridad social debe tener 12 digitos.", "numSS");
        }

        String tel1 = txtTelefono1.getText().trim();
        if (!tel1.matches("^[0-9]{9}$")) {
            txtTelefono1.requestFocus();
            throw new ValidacionException(
                    "El telefono 1 es obligatorio y debe tener 9 digitos.", "telefono1");
        }

        String tel2 = txtTelefono2.getText().trim();
        if (!tel2.isEmpty() && !tel2.matches("^[0-9]{9}$")) {
            txtTelefono2.requestFocus();
            throw new ValidacionException(
                    "El telefono 2 debe tener 9 digitos.", "telefono2");
        }

        if (txtCalle.getText().trim().isEmpty()) {
            txtCalle.requestFocus();
            throw new ValidacionException(
                    "La calle es obligatoria.", "calle");
        }

        //La BD exige numero INT NOT NULL en la tabla direccion
        String numero = txtNumero.getText().trim();
        if (numero.isEmpty()) {
            txtNumero.requestFocus();
            throw new ValidacionException(
                    "El numero de la direccion es obligatorio.", "numero");
        }
        if (!numero.matches("^[0-9]+$")) {
            txtNumero.requestFocus();
            throw new ValidacionException(
                    "El numero de la direccion debe ser un valor numerico.", "numero");
        }

        String cp = txtCodigoPostal.getText().trim();
        if (!cp.matches("^[0-9]{5}$")) {
            txtCodigoPostal.requestFocus();
            throw new ValidacionException(
                    "El codigo postal debe tener 5 digitos.", "codigoPostal");
        }

        if (txtLocalidad.getText().trim().isEmpty()) {
            txtLocalidad.requestFocus();
            throw new ValidacionException(
                    "La localidad es obligatoria.", "localidad");
        }

        //La BD exige provincia NOT NULL en la tabla localidad
        if (txtProvincia.getText().trim().isEmpty()) {
            txtProvincia.requestFocus();
            throw new ValidacionException(
                    "La provincia es obligatoria.", "provincia");
        }
    }

    private String[] separarApellidos(String apellidosCompletos) {
        String[] resultado = new String[2];
        String[] partes = apellidosCompletos.split(" ", 2);

        resultado[0] = partes[0];
        resultado[1] = partes.length > 1 ? partes[1] : "";

        return resultado;
    }

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

    @FXML
    void cerrarVentana(ActionEvent event) {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }
}
