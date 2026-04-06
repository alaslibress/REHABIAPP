package com.javafx.Interface;

import com.javafx.Clases.Sanitario;
import com.javafx.Clases.VentanaUtil;
import com.javafx.Clases.VentanaUtil.TipoMensaje;
import com.javafx.DAO.SanitarioDAO;
import com.javafx.excepcion.ConexionException;
import com.javafx.excepcion.DuplicadoException;
import com.javafx.excepcion.RehabiAppException;
import com.javafx.excepcion.ValidacionException;
import com.javafx.service.SanitarioService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;
import org.controlsfx.validation.ValidationResult;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;

/**
 * Controlador para la ventana de agregar y modificar sanitario
 * Gestiona tanto la creacion de nuevos sanitarios como la edicion de existentes
 */
public class controladorAgregarSanitario {

    @FXML
    private Button btnCancelarSanitarioNuevo;

    @FXML
    private Button btnCrearSanitarioNuevo;

    @FXML
    private RadioButton rbMedico;

    @FXML
    private RadioButton rbEnfermero;

    @FXML
    private ToggleGroup grupoCargoSanitario;

    @FXML
    private Label lblTituloVentana;

    @FXML
    private PasswordField pssContraseniaNuevoSanitario;

    @FXML
    private TextField txfApellidosSanitario;

    @FXML
    private TextField txfDNISanitario;

    @FXML
    private TextField txfEmailSanitario;

    @FXML
    private TextField txfNombreSanitario;

    @FXML
    private TextField txfTelDosSanitario;

    @FXML
    private TextField txfTelUnoSanitario;

    private SanitarioService sanitarioService;
    private SanitarioDAO sanitarioDAO;

    //Soporte de validacion de ControlsFX
    private ValidationSupport validationSupport;

    //Indica si estamos en modo edicion (true) o creacion (false)
    private boolean modoEdicion = false;

    //DNI original del sanitario cuando estamos en modo edicion
    private String dniOriginal;

    @FXML
    public void initialize() {
        sanitarioService = new SanitarioService();
        sanitarioDAO = new SanitarioDAO();

        validationSupport = new ValidationSupport();

        Platform.runLater(this::configurarValidaciones);

        rbMedico.setSelected(true);
    }

    private void configurarValidaciones() {
        validationSupport.registerValidator(txfNombreSanitario,
                Validator.createEmptyValidator("El nombre es obligatorio"));

        validationSupport.registerValidator(txfApellidosSanitario,
                Validator.createEmptyValidator("Los apellidos son obligatorios"));

        validationSupport.registerValidator(txfDNISanitario,
                Validator.createRegexValidator(
                        "El DNI debe tener 8 digitos y una letra",
                        "^[0-9]{8}[A-Za-z]$",
                        org.controlsfx.validation.Severity.ERROR));

        validationSupport.registerValidator(txfEmailSanitario,
                Validator.createRegexValidator(
                        "El formato del email no es valido",
                        "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$",
                        org.controlsfx.validation.Severity.ERROR));

        validationSupport.registerValidator(pssContraseniaNuevoSanitario,
                Validator.createEmptyValidator("La contrasena es obligatoria"));

        validationSupport.registerValidator(txfTelUnoSanitario,
                Validator.createRegexValidator(
                        "El telefono debe tener 9 digitos",
                        "^[0-9]{9}$",
                        org.controlsfx.validation.Severity.ERROR));

        Validator<String> validadorTelefono2 = (control, valor) -> {
            if (valor == null || valor.trim().isEmpty()) {
                return ValidationResult.fromErrorIf(control, "", false);
            }
            boolean esInvalido = !valor.matches("^[0-9]{9}$");
            return ValidationResult.fromErrorIf(control, "El telefono debe tener 9 digitos", esInvalido);
        };
        validationSupport.registerValidator(txfTelDosSanitario, false, validadorTelefono2);
    }

    /**
     * Carga los datos de un sanitario existente para editar
     */
    public void cargarDatosParaEdicion(Sanitario sanitario) {
        modoEdicion = true;
        dniOriginal = sanitario.getDni();

        lblTituloVentana.setText("Modificar Sanitario");
        btnCrearSanitarioNuevo.setText("Guardar");

        txfNombreSanitario.setText(sanitario.getNombre());
        txfApellidosSanitario.setText(sanitario.getApellidos());
        txfDNISanitario.setText(sanitario.getDni());
        txfEmailSanitario.setText(sanitario.getEmail() != null ? sanitario.getEmail() : "");
        pssContraseniaNuevoSanitario.setText(sanitario.getContrasena() != null ? sanitario.getContrasena() : "");

        txfTelUnoSanitario.setText(sanitario.getTelefono1() != null ? sanitario.getTelefono1() : "");
        txfTelDosSanitario.setText(sanitario.getTelefono2() != null ? sanitario.getTelefono2() : "");

        if (sanitario.esEspecialista()) {
            rbMedico.setSelected(true);
        } else {
            rbEnfermero.setSelected(true);
        }
    }

    @FXML
    void crearNuevoSanitario(ActionEvent event) {
        try {
            validarCampos();
        } catch (ValidacionException e) {
            VentanaUtil.mostrarVentanaInformativa(e.getMessage(), TipoMensaje.ADVERTENCIA);
            return;
        }

        String dni = txfDNISanitario.getText().trim().toUpperCase();
        String nombre = txfNombreSanitario.getText().trim();
        String apellidos = txfApellidosSanitario.getText().trim();
        String email = txfEmailSanitario.getText().trim();
        String contrasena = pssContraseniaNuevoSanitario.getText();
        String telefono1 = txfTelUnoSanitario.getText().trim();
        String telefono2 = txfTelDosSanitario.getText().trim();

        String cargo = rbMedico.isSelected() ? "medico especialista" : "enfermero";

        String[] apellidosSeparados = separarApellidos(apellidos);

        Sanitario sanitario = new Sanitario(
                dni,
                nombre,
                apellidosSeparados[0],
                apellidosSeparados[1],
                email,
                cargo
        );
        sanitario.setContrasena(contrasena);
        sanitario.setTelefono1(telefono1);
        sanitario.setTelefono2(telefono2);

        boolean exito;
        if (modoEdicion) {
            exito = actualizarSanitarioExistente(sanitario);
        } else {
            exito = insertarNuevoSanitario(sanitario);
        }

        if (exito) {
            cerrarVentana(event);
        }
    }

    /**
     * Inserta un nuevo sanitario. Valida unicidad y captura excepciones.
     */
    private boolean insertarNuevoSanitario(Sanitario sanitario) {
        //Validar unicidad antes de intentar insertar
        try {
            if (sanitarioDAO.existeDni(sanitario.getDni())) {
                VentanaUtil.mostrarVentanaInformativa(
                        "Ya existe un sanitario registrado con ese DNI.",
                        TipoMensaje.ERROR
                );
                txfDNISanitario.requestFocus();
                return false;
            }

            if (sanitarioDAO.existeEmail(sanitario.getEmail())) {
                VentanaUtil.mostrarVentanaInformativa(
                        "Ya existe un sanitario registrado con ese email.",
                        TipoMensaje.ERROR
                );
                txfEmailSanitario.requestFocus();
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
            sanitarioService.insertar(sanitario, sanitario.getContrasena());

            VentanaUtil.mostrarVentanaInformativa(
                    "El sanitario se ha registrado correctamente.",
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
     * Actualiza un sanitario existente. Valida unicidad y captura excepciones.
     */
    private boolean actualizarSanitarioExistente(Sanitario sanitario) {
        try {
            if (!sanitario.getDni().equals(dniOriginal)) {
                if (sanitarioDAO.existeDni(sanitario.getDni())) {
                    VentanaUtil.mostrarVentanaInformativa(
                            "Ya existe otro sanitario con ese DNI.",
                            TipoMensaje.ERROR
                    );
                    txfDNISanitario.requestFocus();
                    return false;
                }
            }

            if (sanitarioDAO.existeEmail(sanitario.getEmail())) {
                VentanaUtil.mostrarVentanaInformativa(
                        "Ya existe otro sanitario con ese email.",
                        TipoMensaje.ERROR
                );
                txfEmailSanitario.requestFocus();
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
            sanitarioService.actualizar(sanitario, dniOriginal);

            VentanaUtil.mostrarVentanaInformativa(
                    "Los datos del sanitario se han actualizado correctamente.",
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
        if (txfNombreSanitario.getText().trim().isEmpty()) {
            txfNombreSanitario.requestFocus();
            throw new ValidacionException(
                    "El nombre es obligatorio.", "nombre");
        }

        String apellidos = txfApellidosSanitario.getText().trim();
        if (apellidos.isEmpty()) {
            txfApellidosSanitario.requestFocus();
            throw new ValidacionException(
                    "Los apellidos son obligatorios.", "apellidos");
        }

        //La BD exige apellido1_san y apellido2_san NOT NULL con contenido
        String[] apellidosSep = apellidos.split(" ", 2);
        if (apellidosSep.length < 2 || apellidosSep[1].trim().isEmpty()) {
            txfApellidosSanitario.requestFocus();
            throw new ValidacionException(
                    "Debe introducir los dos apellidos separados por un espacio.", "apellidos");
        }

        String dni = txfDNISanitario.getText().trim();
        if (!dni.matches("^[0-9]{8}[A-Za-z]$")) {
            txfDNISanitario.requestFocus();
            throw new ValidacionException(
                    "El DNI debe tener 8 numeros seguidos de una letra.", "dni");
        }

        String email = txfEmailSanitario.getText().trim();
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            txfEmailSanitario.requestFocus();
            throw new ValidacionException(
                    "El formato del email no es valido.", "email");
        }

        if (pssContraseniaNuevoSanitario.getText().isEmpty()) {
            pssContraseniaNuevoSanitario.requestFocus();
            throw new ValidacionException(
                    "La contrasena es obligatoria.", "contrasena");
        }

        String tel1 = txfTelUnoSanitario.getText().trim();
        if (!tel1.matches("^[0-9]{9}$")) {
            txfTelUnoSanitario.requestFocus();
            throw new ValidacionException(
                    "El telefono 1 es obligatorio y debe tener 9 digitos.", "telefono1");
        }

        String tel2 = txfTelDosSanitario.getText().trim();
        if (!tel2.isEmpty() && !tel2.matches("^[0-9]{9}$")) {
            txfTelDosSanitario.requestFocus();
            throw new ValidacionException(
                    "El telefono 2 debe tener 9 digitos.", "telefono2");
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
    void cerrarVentana(ActionEvent event) {
        Stage stage = (Stage) btnCancelarSanitarioNuevo.getScene().getWindow();
        stage.close();
    }
}
