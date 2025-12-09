package com.javafx.Interface;

import com.javafx.Clases.AnimacionUtil;
import com.javafx.Clases.Sanitario;
import com.javafx.Clases.SesionUsuario;
import com.javafx.Clases.VentanaUtil;
import com.javafx.DAO.SanitarioDAO;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * Controlador para la ventana de inicio de sesion
 * Gestiona la autenticacion de usuarios y el acceso al sistema
 * 
 * ANIMACIONES APLICADAS (segun tema 2-6):
 * - Logo: FadeTransition (aparece poco a poco)
 * - Campo contraseña: Timeline con efecto brillo
 * - Botones: ScaleTransition en hover
 */
public class controladorSesion {

    @FXML
    private Button btnCancelar;

    @FXML
    private Button btnEntrar;

    @FXML
    private ImageView imgLogo;

    @FXML
    private Label lblInsertarContrasenia;

    @FXML
    private Label lblInsertarDNI;

    @FXML
    private Label lblMensajeError;

    @FXML
    private PasswordField pssInsertarContrasenia;

    @FXML
    private TextField txtInsertarDNI;

    // DAO para autenticacion
    private SanitarioDAO sanitarioDAO;

    // Timeline para el efecto de brillo (guardar referencia para poder detenerlo)
    private Timeline timelineBrillo;

    /**
     * Metodo initialize se ejecuta automaticamente al cargar el FXML
     */
    @FXML
    public void initialize() {
        sanitarioDAO = new SanitarioDAO();

        // Ocultar mensaje de error inicialmente
        lblMensajeError.setVisible(false);

        // Verificar y crear usuario admin si no existe
        verificarUsuarioAdmin();

        // Configurar eventos de teclado para login con Enter
        txtInsertarDNI.setOnKeyPressed(this::manejarTeclaEnter);
        pssInsertarContrasenia.setOnKeyPressed(this::manejarTeclaEnter);

        // ========== ANIMACIONES ==========

        // 1. Logo aparece poco a poco (FadeTransition)
        AnimacionUtil.fadeIn(imgLogo, 1500);

        // 2. Efecto brillo suave en campo de contraseña (Timeline con KeyFrames)
        timelineBrillo = AnimacionUtil.brilloCampoTexto(pssInsertarContrasenia);

        // 3. Efecto hover en botones
        AnimacionUtil.brilloHover(btnEntrar, Color.rgb(93, 173, 226, 0.8));
        AnimacionUtil.brilloHover(btnCancelar, Color.rgb(231, 76, 60, 0.8));
    }

    /**
     * Verifica si existe el usuario admin y lo crea si no existe
     */
    private void verificarUsuarioAdmin() {
        try {
            boolean creado = sanitarioDAO.crearAdminSiNoExiste();
            if (creado) {
                System.out.println("Sistema listo con usuario admin");
            }
        } catch (Exception e) {
            System.err.println("Error al verificar usuario admin: " + e.getMessage());
        }
    }

    /**
     * Maneja la tecla Enter para iniciar sesion
     */
    private void manejarTeclaEnter(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            iniciarSesion(null);
        }
    }

    /**
     * Cierra la aplicacion
     */
    @FXML
    void CerrarAPP(ActionEvent event) {
        // Detener animaciones antes de cerrar
        if (timelineBrillo != null) {
            timelineBrillo.stop();
        }

        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }

    /**
     * Intenta iniciar sesion con las credenciales introducidas
     */
    @FXML
    void iniciarSesion(ActionEvent event) {
        // Ocultar mensaje de error previo
        lblMensajeError.setVisible(false);

        // Obtener credenciales
        String dni = txtInsertarDNI.getText().trim().toUpperCase();
        String contrasena = pssInsertarContrasenia.getText();

        // Validar campos vacios
        if (dni.isEmpty()) {
            mostrarError("Introduce tu DNI");
            txtInsertarDNI.requestFocus();
            return;
        }

        if (contrasena.isEmpty()) {
            mostrarError("Introduce tu contraseña");
            pssInsertarContrasenia.requestFocus();
            return;
        }

        // Intentar autenticar
        Sanitario sanitario = sanitarioDAO.autenticar(dni, contrasena);

        if (sanitario != null) {
            // Detener animacion de brillo antes de cambiar de ventana
            if (timelineBrillo != null) {
                timelineBrillo.stop();
            }

            // Autenticacion exitosa - guardar sesion
            SesionUsuario sesion = SesionUsuario.getInstancia();
            sesion.iniciarSesion(
                    sanitario.getDni(),
                    sanitario.getNombre(),
                    sanitario.getApellido1() + " " + sanitario.getApellido2(),
                    sanitario.getEmail(),
                    sanitario.getCargo()
            );

            // Abrir ventana principal
            abrirVentanaPrincipal();

        } else {
            // Autenticacion fallida
            mostrarError("DNI o contraseña incorrectos");
            pssInsertarContrasenia.clear();
            pssInsertarContrasenia.requestFocus();
        }
    }

    /**
     * Muestra un mensaje de error
     */
    private void mostrarError(String mensaje) {
        lblMensajeError.setText(mensaje);
        lblMensajeError.setVisible(true);
    }

    /**
     * Abre la ventana principal y cierra la de login
     */
    private void abrirVentanaPrincipal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaPrincipal.fxml"));
            Parent root = loader.load();

            // Obtener el controlador y pasar datos de sesion si es necesario
            controladorVentanaPrincipal controlador = loader.getController();
            if (controlador != null) {
                controlador.inicializarSesion();
            }

            Scene scene = new Scene(root);
            
            // Aplicar configuracion de tema y tamaño de letra
            controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

            Stage stage = new Stage();
            stage.setTitle("RehabiAPP - " + SesionUsuario.getInstancia().getNombreCompleto());
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.setResizable(true);
            VentanaUtil.establecerIconoVentana(stage);

            // Manejar cierre de ventana para cerrar sesion
            stage.setOnCloseRequest(e -> {
                SesionUsuario.getInstancia().cerrarSesion();
            });

            stage.show();

            // Aplicar animacion de aparecer a la ventana principal
            AnimacionUtil.animarVentana(stage, 400);

            // Cerrar ventana de login
            Stage loginStage = (Stage) btnEntrar.getScene().getWindow();
            loginStage.close();

        } catch (Exception e) {
            mostrarError("Error al abrir la aplicacion");
            e.printStackTrace();
        }
    }
}
