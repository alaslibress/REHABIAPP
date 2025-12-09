package com.javafx.Interface;

import com.javafx.Clases.Sanitario;
import com.javafx.Clases.SesionUsuario;
import com.javafx.Clases.VentanaUtil;
import com.javafx.Clases.VentanaUtil.TipoMensaje;
import com.javafx.DAO.SanitarioDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controlador para la ventana de edicion de perfil del usuario logueado
 * Permite modificar los datos personales del sanitario
 */
public class controladorPerfilEditar {

    @FXML
    private Button btnCancelarSanitarioCambiar;

    @FXML
    private Button btnCrearSanitarioCambiar;

    @FXML
    private CheckBox cbxEspecialistaCambiar;

    @FXML
    private Label lblTituloVentana;

    @FXML
    private PasswordField pssContraseniaCambiarSanitario;

    @FXML
    private TextField txfApellidosSanitarioCambiar;

    @FXML
    private TextField txfDNISanitarioCambiar;

    @FXML
    private TextField txfEmailSanitarioCambiar;

    @FXML
    private TextField txfNombreSanitarioCambiar;

    @FXML
    private TextField txfTelDosSanitarioCambiar;

    @FXML
    private TextField txfTelUnoSanitarioCambiar;

    //DAO para operaciones con la base de datos
    private SanitarioDAO sanitarioDAO;

    //Sanitario que se esta editando
    private Sanitario sanitarioActual;

    //Indica si se guardaron cambios
    private boolean cambiosGuardados = false;

    /**
     * Metodo initialize se ejecuta automaticamente al cargar el FXML
     */
    @FXML
    public void initialize() {
        sanitarioDAO = new SanitarioDAO();

        //Cargar datos del usuario logueado
        cargarDatosUsuarioLogueado();
    }

    /**
     * Carga los datos del usuario actualmente logueado
     */
    private void cargarDatosUsuarioLogueado() {
        SesionUsuario sesion = SesionUsuario.getInstancia();

        if (!sesion.haySesionActiva()) {
            VentanaUtil.mostrarVentanaInformativa(
                    "No hay sesion activa.",
                    TipoMensaje.ERROR
            );
            return;
        }

        //Obtener datos completos del sanitario desde la BD
        sanitarioActual = sanitarioDAO.buscarPorDni(sesion.getDniUsuario());

        if (sanitarioActual != null) {
            mostrarDatosEnCampos();
        } else {
            VentanaUtil.mostrarVentanaInformativa(
                    "No se pudieron cargar los datos del usuario.",
                    TipoMensaje.ERROR
            );
        }
    }

    /**
     * Muestra los datos del sanitario en los campos del formulario
     */
    private void mostrarDatosEnCampos() {
        //Titulo con nombre del usuario
        lblTituloVentana.setText("Editar perfil - " + sanitarioActual.getNombre());

        //Datos personales
        txfNombreSanitarioCambiar.setText(sanitarioActual.getNombre());

        //Concatenar apellidos
        String apellidos = sanitarioActual.getApellido1();
        if (sanitarioActual.getApellido2() != null && !sanitarioActual.getApellido2().isEmpty()) {
            apellidos += " " + sanitarioActual.getApellido2();
        }
        txfApellidosSanitarioCambiar.setText(apellidos);

        //DNI (no editable, es la clave primaria)
        //El campo ya esta deshabilitado en FXML con disable="true"
        //El estilo lo maneja el CSS con .text-field:disabled
        txfDNISanitarioCambiar.setText(sanitarioActual.getDni());

        //Email
        txfEmailSanitarioCambiar.setText(sanitarioActual.getEmail());

        //Telefonos (usar getTelefono1 y getTelefono2)
        String tel1 = sanitarioActual.getTelefono1();
        String tel2 = sanitarioActual.getTelefono2();

        if (tel1 != null && !tel1.isEmpty()) {
            txfTelUnoSanitarioCambiar.setText(tel1);
        }
        if (tel2 != null && !tel2.isEmpty()) {
            txfTelDosSanitarioCambiar.setText(tel2);
        }

        //Cargo (checkbox)
        String cargo = sanitarioActual.getCargo();
        if (cargo != null && cargo.toLowerCase().contains("especialista")) {
            cbxEspecialistaCambiar.setSelected(true);
        } else {
            cbxEspecialistaCambiar.setSelected(false);
        }

        //Contraseña vacia (no se muestra la actual por seguridad)
        pssContraseniaCambiarSanitario.setText("");
        pssContraseniaCambiarSanitario.setPromptText("Dejar vacio para no cambiar");
    }

    /**
     * Guarda los cambios realizados en el perfil
     */
    @FXML
    void cambiarSanitario(ActionEvent event) {
        //Validar campos obligatorios
        if (!validarCampos()) {
            return;
        }

        //Confirmar cambios
        boolean confirmar = VentanaUtil.mostrarVentanaPregunta(
                "¿Desea guardar los cambios en su perfil?"
        );

        if (!confirmar) {
            return;
        }

        //Recoger datos del formulario
        String nombre = txfNombreSanitarioCambiar.getText().trim();
        String apellidosCompletos = txfApellidosSanitarioCambiar.getText().trim();
        String email = txfEmailSanitarioCambiar.getText().trim();
        String telefono1 = txfTelUnoSanitarioCambiar.getText().trim();
        String telefono2 = txfTelDosSanitarioCambiar.getText().trim();
        String contrasena = pssContraseniaCambiarSanitario.getText();
        boolean esEspecialista = cbxEspecialistaCambiar.isSelected();

        //Separar apellidos
        String apellido1 = "";
        String apellido2 = "";
        if (!apellidosCompletos.isEmpty()) {
            String[] partes = apellidosCompletos.split(" ", 2);
            apellido1 = partes[0];
            if (partes.length > 1) {
                apellido2 = partes[1];
            }
        }

        //Actualizar objeto sanitario
        sanitarioActual.setNombre(nombre);
        sanitarioActual.setApellido1(apellido1);
        sanitarioActual.setApellido2(apellido2);
        sanitarioActual.setEmail(email);

        //Cargo
        String cargo = esEspecialista ? "medico especialista" : "enfermero";
        sanitarioActual.setCargo(cargo);

        //Guardar DNI original para la actualizacion
        String dniOriginal = sanitarioActual.getDni();

        //Intentar actualizar en la base de datos
        boolean exito = sanitarioDAO.actualizar(sanitarioActual, dniOriginal);

        if (exito) {
            //Actualizar telefonos
            sanitarioDAO.actualizarTelefonos(dniOriginal, telefono1, telefono2);

            //Actualizar contraseña si se proporciono una nueva
            if (contrasena != null && !contrasena.isEmpty()) {
                sanitarioDAO.cambiarContrasena(sanitarioActual.getDni(), contrasena);
            }

            //Actualizar cargo en la tabla sanitario_agrega_sanitario
            sanitarioDAO.actualizarCargo(sanitarioActual.getDni(), cargo);

            //Actualizar datos de sesion
            SesionUsuario sesion = SesionUsuario.getInstancia();
            sesion.iniciarSesion(
                    sanitarioActual.getDni(),
                    nombre,
                    apellido1 + " " + apellido2,
                    email,
                    cargo
            );

            cambiosGuardados = true;

            VentanaUtil.mostrarVentanaInformativa(
                    "Perfil actualizado correctamente.",
                    TipoMensaje.EXITO
            );

            //Cerrar ventana
            cerrarVentana(null);

        } else {
            VentanaUtil.mostrarVentanaInformativa(
                    "Error al actualizar el perfil.",
                    TipoMensaje.ERROR
            );
        }
    }

    /**
     * Valida que los campos obligatorios esten completos
     * @return true si todos los campos son validos
     */
    private boolean validarCampos() {
        StringBuilder errores = new StringBuilder();

        //Nombre obligatorio
        if (txfNombreSanitarioCambiar.getText().trim().isEmpty()) {
            errores.append("- El nombre es obligatorio\n");
        }

        //Apellidos obligatorios
        if (txfApellidosSanitarioCambiar.getText().trim().isEmpty()) {
            errores.append("- Los apellidos son obligatorios\n");
        }

        //Email obligatorio y formato valido
        String email = txfEmailSanitarioCambiar.getText().trim();
        if (email.isEmpty()) {
            errores.append("- El email es obligatorio\n");
        } else if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            errores.append("- El formato del email no es valido\n");
        }

        //Contraseña minimo 4 caracteres si se proporciona
        String contrasena = pssContraseniaCambiarSanitario.getText();
        if (!contrasena.isEmpty() && contrasena.length() < 4) {
            errores.append("- La contraseña debe tener al menos 4 caracteres\n");
        }

        //Mostrar errores si los hay
        if (errores.length() > 0) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Por favor corrija los siguientes errores:\n\n" + errores.toString(),
                    TipoMensaje.ADVERTENCIA
            );
            return false;
        }

        return true;
    }

    /**
     * Indica si se guardaron cambios
     * @return true si hubo cambios guardados
     */
    public boolean seGuardaronCambios() {
        return cambiosGuardados;
    }

    /**
     * Cierra la ventana sin guardar cambios
     */
    @FXML
    void cerrarVentana(ActionEvent event) {
        Stage stage = (Stage) btnCancelarSanitarioCambiar.getScene().getWindow();
        stage.close();
    }
}