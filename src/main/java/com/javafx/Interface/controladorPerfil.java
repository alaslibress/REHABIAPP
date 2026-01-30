package com.javafx.Interface;

import com.javafx.Clases.Sanitario;
import com.javafx.Clases.SesionUsuario;
import com.javafx.Clases.VentanaUtil;
import com.javafx.Clases.VentanaUtil.TipoMensaje;
import com.javafx.DAO.SanitarioDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Controlador para la ventana de perfil del usuario logueado
 * Permite ver los datos del usuario y cambiar la contraseña
 */
public class controladorPerfil {

    @FXML
    private Button btnCambiarContrasena;

    @FXML
    private Button btnCerrar;

    @FXML
    private Button btnEditarPerfil;

    @FXML
    private ImageView imgFotoUsuario;

    @FXML
    private Label lblApellidosValor;

    @FXML
    private Label lblCargoValor;

    @FXML
    private Label lblDNIValor;

    @FXML
    private Label lblEmailValor;

    @FXML
    private Label lblNombreValor;

    @FXML
    private Label lblNumPacientesValor;

    @FXML
    private Label lblTitulo;

    //DAO
    private SanitarioDAO sanitarioDAO;

    //Datos del usuario actual
    private Sanitario usuarioActual;

    /**
     * Metodo initialize se ejecuta automaticamente al cargar el FXML
     */
    @FXML
    public void initialize() {
        sanitarioDAO = new SanitarioDAO();
    }

    /**
     * Carga los datos del usuario logueado desde la sesion
     */
    public void cargarDatosUsuario() {
        SesionUsuario sesion = SesionUsuario.getInstancia();

        if (!sesion.haySesionActiva()) {
            System.err.println("No hay sesion activa");
            return;
        }

        //Obtener datos completos del sanitario
        usuarioActual = sanitarioDAO.buscarPorDni(sesion.getDniUsuario());

        if (usuarioActual != null) {
            mostrarDatosEnLabels();
        }
    }

    /**
     * Muestra los datos del usuario en los labels
     */
    private void mostrarDatosEnLabels() {
        if (lblDNIValor != null) {
            lblDNIValor.setText(usuarioActual.getDni());
        }

        if (lblNombreValor != null) {
            lblNombreValor.setText(usuarioActual.getNombre());
        }

        if (lblApellidosValor != null) {
            String apellidos = usuarioActual.getApellido1();
            if (usuarioActual.getApellido2() != null && !usuarioActual.getApellido2().isEmpty()) {
                apellidos += " " + usuarioActual.getApellido2();
            }
            lblApellidosValor.setText(apellidos);
        }

        if (lblEmailValor != null) {
            lblEmailValor.setText(usuarioActual.getEmail() != null ? usuarioActual.getEmail() : "-");
        }

        if (lblCargoValor != null) {
            lblCargoValor.setText(usuarioActual.getCargo() != null ? usuarioActual.getCargo() : "-");
        }

        if (lblNumPacientesValor != null) {
            lblNumPacientesValor.setText(String.valueOf(usuarioActual.getNumPacientes()));
        }

        //Cargar imagen por defecto
        cargarImagenPorDefecto();
    }

    /**
     * Carga una imagen por defecto para el usuario
     */
    private void cargarImagenPorDefecto() {
        if (imgFotoUsuario != null) {
            try {
                Image imagenDefault = new Image(getClass().getResourceAsStream("/usuario_default.png"));
                imgFotoUsuario.setImage(imagenDefault);
            } catch (Exception e) {
                imgFotoUsuario.setImage(null);
            }
        }
    }

    /**
     * Abre la ventana para editar el perfil
     */
    @FXML
    void editarPerfil(ActionEvent event) {
        //Solo especialistas pueden editar su perfil completo
        SesionUsuario sesion = SesionUsuario.getInstancia();

        if (!sesion.esEspecialista()) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Los enfermeros solo pueden cambiar su contraseña.\n" +
                            "Contacta con un especialista para modificar otros datos.",
                    TipoMensaje.INFORMACION
            );
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaPerfilEditar.fxml"));
            Parent root = loader.load();

            //Obtener controlador
            controladorPerfilEditar controlador = loader.getController();

            //Crear escena y aplicar CSS
            Scene scene = new Scene(root);
            controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

            Stage stage = new Stage();
            stage.setTitle("Editar Mi Perfil");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            VentanaUtil.establecerIconoVentana(stage);
            stage.showAndWait();

            //Recargar datos si se guardaron cambios
            if (controlador != null && controlador.seGuardaronCambios()) {
                cargarDatosUsuario();
            }

        } catch (Exception e) {
            System.err.println("Error al abrir ventana de edicion: " + e.getMessage());
            e.printStackTrace();
            VentanaUtil.mostrarVentanaInformativa(
                    "Error al abrir la ventana de edicion de perfil.",
                    TipoMensaje.ERROR
            );
        }
    }

    /**
     * Abre el dialogo para cambiar la contraseña
     */
    @FXML
    void cambiarContrasena(ActionEvent event) {
        //Mostrar dialogo simple para cambiar contraseña
        mostrarDialogoCambioContrasena();
    }

    /**
     * Muestra un dialogo simple para cambiar la contraseña
     */
    private void mostrarDialogoCambioContrasena() {
        //Crear una ventana simple con los campos necesarios
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Cambiar Contraseña");
        VentanaUtil.establecerIconoVentana(dialog);

        javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(10);
        vbox.setPadding(new javafx.geometry.Insets(20));
        vbox.setAlignment(javafx.geometry.Pos.CENTER);
        vbox.getStyleClass().add("root"); //Importante para tema oscuro

        Label lblActual = new Label("Contraseña actual:");
        PasswordField txtActual = new PasswordField();
        txtActual.setPrefWidth(200);

        Label lblNueva = new Label("Nueva contraseña:");
        PasswordField txtNueva = new PasswordField();
        txtNueva.setPrefWidth(200);

        Label lblConfirmar = new Label("Confirmar contraseña:");
        PasswordField txtConfirmar = new PasswordField();
        txtConfirmar.setPrefWidth(200);

        Label lblError = new Label();
        lblError.setStyle("-fx-text-fill: red;");
        lblError.setVisible(false);

        Button btnGuardar = new Button("Guardar");
        Button btnCancelar = new Button("Cancelar");

        javafx.scene.layout.HBox hboxBotones = new javafx.scene.layout.HBox(10);
        hboxBotones.setAlignment(javafx.geometry.Pos.CENTER);
        hboxBotones.getChildren().addAll(btnCancelar, btnGuardar);

        btnCancelar.setOnAction(e -> dialog.close());

        btnGuardar.setOnAction(e -> {
            String actual = txtActual.getText();
            String nueva = txtNueva.getText();
            String confirmar = txtConfirmar.getText();

            //Validaciones
            if (actual.isEmpty() || nueva.isEmpty() || confirmar.isEmpty()) {
                lblError.setText("Todos los campos son obligatorios");
                lblError.setVisible(true);
                return;
            }

            if (!nueva.equals(confirmar)) {
                lblError.setText("Las contraseñas no coinciden");
                lblError.setVisible(true);
                return;
            }

            if (nueva.length() < 4) {
                lblError.setText("La contraseña debe tener al menos 4 caracteres");
                lblError.setVisible(true);
                return;
            }

            //Verificar contraseña actual
            SesionUsuario sesion = SesionUsuario.getInstancia();
            if (!sanitarioDAO.verificarContrasena(sesion.getDniUsuario(), actual)) {
                lblError.setText("La contraseña actual es incorrecta");
                lblError.setVisible(true);
                return;
            }

            //Cambiar contraseña
            boolean cambiada = sanitarioDAO.cambiarContrasena(sesion.getDniUsuario(), nueva);

            if (cambiada) {
                dialog.close();
                VentanaUtil.mostrarVentanaInformativa(
                        "Contraseña cambiada correctamente.",
                        TipoMensaje.EXITO
                );
            } else {
                lblError.setText("Error al cambiar la contraseña");
                lblError.setVisible(true);
            }
        });

        vbox.getChildren().addAll(
                lblActual, txtActual,
                lblNueva, txtNueva,
                lblConfirmar, txtConfirmar,
                lblError,
                hboxBotones
        );

        Scene scene = new Scene(vbox, 350, 380);

        //Aplicar CSS al dialogo
        controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

        dialog.setScene(scene);
        dialog.showAndWait();
    }

    /**
     * Cierra la ventana de perfil
     */
    @FXML
    void cerrarVentana(ActionEvent event) {
        Stage stage = (Stage) btnCerrar.getScene().getWindow();
        stage.close();
    }
}
