package com.javafx.Interface;

import com.javafx.Clases.Sanitario;
import com.javafx.Clases.VentanaUtil;
import com.javafx.Clases.VentanaUtil.TipoMensaje;
import com.javafx.DAO.SanitarioDAO;
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
 * Utiliza un unico FXML (VentanaAgregarSanitario.fxml) para ambas operaciones
 * Este cambio se ha implementado en la segunda versión del proyecto (día 1 del 12 de 2025)
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

    // REFACTORIZADO: Usar capa de servicio
    private SanitarioService sanitarioService;
    private SanitarioDAO sanitarioDAO; // Temporal para validaciones

    //Soporte de validacion de ControlsFX
    private ValidationSupport validationSupport;

    //Indica si estamos en modo edicion (true) o creacion (false)
    private boolean modoEdicion = false;

    //DNI original del sanitario cuando estamos en modo edicion
    private String dniOriginal;

    /**
     * Metodo initialize se ejecuta automaticamente al cargar el FXML
     */
    @FXML
    public void initialize() {
        // REFACTORIZADO: Inicializar servicio
        sanitarioService = new SanitarioService();
        sanitarioDAO = new SanitarioDAO(); // Temporal para validaciones

        //Inicializar soporte de validacion
        validationSupport = new ValidationSupport();

        //Configurar validaciones en un hilo separado para evitar problemas de carga
        Platform.runLater(this::configurarValidaciones);

        //Seleccionar "Médico" por defecto
        rbMedico.setSelected(true);
    }

    /**
     * Configura las validaciones de los campos del formulario usando ControlsFX
     */
    private void configurarValidaciones() {
        //Validacion del nombre: no puede estar vacio
        validationSupport.registerValidator(txfNombreSanitario,
                Validator.createEmptyValidator("El nombre es obligatorio"));

        //Validacion de apellidos: no puede estar vacio
        validationSupport.registerValidator(txfApellidosSanitario,
                Validator.createEmptyValidator("Los apellidos son obligatorios"));

        //Validacion del DNI: formato 8 digitos + 1 letra
        validationSupport.registerValidator(txfDNISanitario,
                Validator.createRegexValidator(
                        "El DNI debe tener 8 digitos y una letra",
                        "^[0-9]{8}[A-Za-z]$",
                        org.controlsfx.validation.Severity.ERROR));

        //Validacion del email: formato valido
        validationSupport.registerValidator(txfEmailSanitario,
                Validator.createRegexValidator(
                        "El formato del email no es valido",
                        "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$",
                        org.controlsfx.validation.Severity.ERROR));

        //Validacion de la contrasena: no puede estar vacia
        validationSupport.registerValidator(pssContraseniaNuevoSanitario,
                Validator.createEmptyValidator("La contrasena es obligatoria"));

        //Validacion del telefono 1: 9 digitos obligatorio
        validationSupport.registerValidator(txfTelUnoSanitario,
                Validator.createRegexValidator(
                        "El telefono debe tener 9 digitos",
                        "^[0-9]{9}$",
                        org.controlsfx.validation.Severity.ERROR));

        //Validacion del telefono 2: opcional, pero si tiene contenido debe ser valido
        Validator<String> validadorTelefono2 = (control, valor) -> {
            //Si esta vacio es valido porque es opcional
            if (valor == null || valor.trim().isEmpty()) {
                return ValidationResult.fromErrorIf(control, "", false);
            }
            //Si tiene contenido debe cumplir el formato
            boolean esInvalido = !valor.matches("^[0-9]{9}$");
            return ValidationResult.fromErrorIf(control, "El telefono debe tener 9 digitos", esInvalido);
        };
        validationSupport.registerValidator(txfTelDosSanitario, false, validadorTelefono2);
    }

    /**
     * Carga los datos de un sanitario existente para editar
     * Cambia el modo del formulario a edicion
     * @param sanitario Sanitario a editar
     */
    public void cargarDatosParaEdicion(Sanitario sanitario) {
        //Activar modo edicion
        modoEdicion = true;
        dniOriginal = sanitario.getDni();

        //Cambiar titulo de la ventana
        lblTituloVentana.setText("Modificar Sanitario");

        //Cambiar texto del boton
        btnCrearSanitarioNuevo.setText("Guardar");

        //Rellenar campos con los datos del sanitario
        txfNombreSanitario.setText(sanitario.getNombre());
        txfApellidosSanitario.setText(sanitario.getApellidos());
        txfDNISanitario.setText(sanitario.getDni());
        txfEmailSanitario.setText(sanitario.getEmail() != null ? sanitario.getEmail() : "");
        pssContraseniaNuevoSanitario.setText(sanitario.getContrasena() != null ? sanitario.getContrasena() : "");

        //Rellenar telefonos
        txfTelUnoSanitario.setText(sanitario.getTelefono1() != null ? sanitario.getTelefono1() : "");
        txfTelDosSanitario.setText(sanitario.getTelefono2() != null ? sanitario.getTelefono2() : "");

        //Seleccionar radiobutton segun el cargo
        if (sanitario.esEspecialista()) {
            rbMedico.setSelected(true);
        } else {
            rbEnfermero.setSelected(true);
        }
    }

    /**
     * Crea un nuevo sanitario o guarda los cambios si estamos en modo edicion
     * @param event Evento del boton
     */
    @FXML
    void crearNuevoSanitario(ActionEvent event) {
        //Validar campos del formulario
        if (!validarCampos()) {
            return;
        }

        //Obtener datos del formulario
        String dni = txfDNISanitario.getText().trim().toUpperCase();
        String nombre = txfNombreSanitario.getText().trim();
        String apellidos = txfApellidosSanitario.getText().trim();
        String email = txfEmailSanitario.getText().trim();
        String contrasena = pssContraseniaNuevoSanitario.getText();
        String telefono1 = txfTelUnoSanitario.getText().trim();
        String telefono2 = txfTelDosSanitario.getText().trim();

        //Determinar cargo segun radiobutton seleccionado
        String cargo = rbMedico.isSelected() ? "medico especialista" : "enfermero";

        //Separar apellidos en primer y segundo apellido
        String[] apellidosSeparados = separarApellidos(apellidos);

        //Crear objeto sanitario con los datos del formulario
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

        //Ejecutar operacion segun el modo
        boolean exito;
        if (modoEdicion) {
            exito = actualizarSanitarioExistente(sanitario);
        } else {
            exito = insertarNuevoSanitario(sanitario);
        }

        //Cerrar ventana si la operacion fue exitosa
        if (exito) {
            cerrarVentana(event);
        }
    }

    /**
     * Inserta un nuevo sanitario en la base de datos
     * @param sanitario Sanitario a insertar
     * @return true si la insercion fue exitosa
     */
    private boolean insertarNuevoSanitario(Sanitario sanitario) {
        //Verificar que el DNI no exista ya en la base de datos
        if (sanitarioDAO.existeDni(sanitario.getDni())) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Ya existe un sanitario registrado con ese DNI.",
                    TipoMensaje.ERROR
            );
            txfDNISanitario.requestFocus();
            return false;
        }

        //Verificar que el email no exista ya en la base de datos
        if (sanitarioDAO.existeEmail(sanitario.getEmail())) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Ya existe un sanitario registrado con ese email.",
                    TipoMensaje.ERROR
            );
            txfEmailSanitario.requestFocus();
            return false;
        }

        // REFACTORIZADO: Usar SanitarioService que maneja insercion + telefonos
        boolean insertado = sanitarioService.insertar(
                sanitario,
                sanitario.getTelefono1(),
                sanitario.getTelefono2()
        );

        if (insertado) {
            VentanaUtil.mostrarVentanaInformativa(
                    "El sanitario se ha registrado correctamente.",
                    TipoMensaje.EXITO
            );
            return true;
        } else {
            VentanaUtil.mostrarVentanaInformativa(
                    "No se pudo registrar el sanitario. Intentalo de nuevo.",
                    TipoMensaje.ERROR
            );
            return false;
        }
    }

    /**
     * Actualiza un sanitario existente en la base de datos
     * @param sanitario Sanitario con los nuevos datos
     * @return true si la actualizacion fue exitosa
     */
    private boolean actualizarSanitarioExistente(Sanitario sanitario) {
        //Verificar si el DNI ha cambiado y si el nuevo DNI ya existe
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

        //Verificar que el email no exista para otro sanitario
        if (sanitarioDAO.existeEmailExcluyendoDni(sanitario.getEmail(), dniOriginal)) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Ya existe otro sanitario con ese email.",
                    TipoMensaje.ERROR
            );
            txfEmailSanitario.requestFocus();
            return false;
        }

        // REFACTORIZADO: Usar SanitarioService que maneja actualizacion + telefonos
        boolean actualizado = sanitarioService.actualizar(
                sanitario,
                dniOriginal,
                sanitario.getTelefono1(),
                sanitario.getTelefono2()
        );

        if (actualizado) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Los datos del sanitario se han actualizado correctamente.",
                    TipoMensaje.EXITO
            );
            return true;
        } else {
            VentanaUtil.mostrarVentanaInformativa(
                    "No se pudieron actualizar los datos. Intentalo de nuevo.",
                    TipoMensaje.ERROR
            );
            return false;
        }
    }

    /**
     * Valida que todos los campos obligatorios esten correctos
     * Muestra mensajes de advertencia si hay errores
     * @return true si todos los campos son validos
     */
    private boolean validarCampos() {
        //Verificar campo nombre
        if (txfNombreSanitario.getText().trim().isEmpty()) {
            VentanaUtil.mostrarVentanaInformativa(
                    "El nombre es obligatorio.",
                    TipoMensaje.ADVERTENCIA
            );
            txfNombreSanitario.requestFocus();
            return false;
        }

        //Verificar campo apellidos
        if (txfApellidosSanitario.getText().trim().isEmpty()) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Los apellidos son obligatorios.",
                    TipoMensaje.ADVERTENCIA
            );
            txfApellidosSanitario.requestFocus();
            return false;
        }

        //Verificar formato DNI (8 numeros + 1 letra)
        String dni = txfDNISanitario.getText().trim();
        if (!dni.matches("^[0-9]{8}[A-Za-z]$")) {
            VentanaUtil.mostrarVentanaInformativa(
                    "El DNI debe tener 8 numeros seguidos de una letra.",
                    TipoMensaje.ADVERTENCIA
            );
            txfDNISanitario.requestFocus();
            return false;
        }

        //Verificar formato email
        String email = txfEmailSanitario.getText().trim();
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            VentanaUtil.mostrarVentanaInformativa(
                    "El formato del email no es valido.",
                    TipoMensaje.ADVERTENCIA
            );
            txfEmailSanitario.requestFocus();
            return false;
        }

        //Verificar contrasena
        if (pssContraseniaNuevoSanitario.getText().isEmpty()) {
            VentanaUtil.mostrarVentanaInformativa(
                    "La contrasena es obligatoria.",
                    TipoMensaje.ADVERTENCIA
            );
            pssContraseniaNuevoSanitario.requestFocus();
            return false;
        }

        //Verificar formato telefono 1 (obligatorio, 9 digitos)
        String tel1 = txfTelUnoSanitario.getText().trim();
        if (!tel1.matches("^[0-9]{9}$")) {
            VentanaUtil.mostrarVentanaInformativa(
                    "El telefono 1 es obligatorio y debe tener 9 digitos.",
                    TipoMensaje.ADVERTENCIA
            );
            txfTelUnoSanitario.requestFocus();
            return false;
        }

        //Verificar formato telefono 2 solo si tiene contenido (opcional)
        String tel2 = txfTelDosSanitario.getText().trim();
        if (!tel2.isEmpty() && !tel2.matches("^[0-9]{9}$")) {
            VentanaUtil.mostrarVentanaInformativa(
                    "El telefono 2 debe tener 9 digitos.",
                    TipoMensaje.ADVERTENCIA
            );
            txfTelDosSanitario.requestFocus();
            return false;
        }

        //Todos los campos son validos
        return true;
    }

    /**
     * Separa los apellidos en primer y segundo apellido
     * @param apellidosCompletos Cadena con ambos apellidos separados por espacio
     * @return Array con [apellido1, apellido2]
     */
    private String[] separarApellidos(String apellidosCompletos) {
        String[] resultado = new String[2];
        String[] partes = apellidosCompletos.split(" ", 2);

        resultado[0] = partes[0];
        resultado[1] = partes.length > 1 ? partes[1] : "";

        return resultado;
    }

    /**
     * Cierra la ventana sin guardar cambios
     * @param event Evento del boton
     */
    @FXML
    void cerrarVentana(ActionEvent event) {
        Stage stage = (Stage) btnCancelarSanitarioNuevo.getScene().getWindow();
        stage.close();
    }
}