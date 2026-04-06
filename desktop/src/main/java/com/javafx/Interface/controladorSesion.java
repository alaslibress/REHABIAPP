package com.javafx.Interface;

import com.javafx.Clases.AnimacionUtil;
import com.javafx.Clases.ApiClient;
import com.javafx.Clases.Sanitario;
import com.javafx.Clases.SesionUsuario;
import com.javafx.Clases.VentanaUtil;
import com.javafx.DAO.SanitarioDAO;
import com.javafx.dto.LoginResponse;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
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
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

/**
 * Controlador para la ventana de inicio de sesion.
 * Gestiona la autenticacion via la API REST (JWT) y el acceso al sistema.
 *
 * Todas las llamadas a la API se ejecutan en un hilo de fondo (Task) para
 * no bloquear el JavaFX Application Thread.
 */
public class controladorSesion {

    @FXML private Button btnCancelar;
    @FXML private Button btnEntrar;
    @FXML private ImageView imgLogo;
    @FXML private Label lblInsertarContrasenia;
    @FXML private Label lblInsertarDNI;
    @FXML private Label lblMensajeError;
    @FXML private PasswordField pssInsertarContrasenia;
    @FXML private TextField txtInsertarDNI;
    @FXML private HBox hboxIndicadorConexion;
    @FXML private Circle circuloConexion;
    @FXML private Label lblEstadoConexion;

    private final SanitarioDAO sanitarioDAO = new SanitarioDAO();
    private Timeline timelineBrillo;

    @FXML
    public void initialize() {
        try {
            lblMensajeError.setVisible(false);

            // Verificar conectividad con la API en hilo de fondo
            verificarConexionApi();

            btnEntrar.setDefaultButton(true);

            // Animaciones
            AnimacionUtil.fadeIn(imgLogo, 1500);
            timelineBrillo = AnimacionUtil.brilloCampoTexto(pssInsertarContrasenia);
            AnimacionUtil.brilloHover(btnEntrar, Color.rgb(93, 173, 226, 0.8));
            AnimacionUtil.brilloHover(btnCancelar, Color.rgb(231, 76, 60, 0.8));

        } catch (Exception e) {
            System.err.println("Error en inicializacion del login: " + e.getMessage());
        }
    }

    /**
     * Verifica la conectividad con la API REST en un hilo de fondo.
     */
    private void verificarConexionApi() {
        Task<Boolean> tarea = new Task<>() {
            @Override
            protected Boolean call() {
                return ApiClient.getInstancia().probarConexion();
            }
        };

        tarea.setOnSucceeded(e -> {
            boolean conectado = tarea.getValue();
            if (conectado) {
                circuloConexion.setFill(Color.web("#27AE60"));
                lblEstadoConexion.setText("Conectado");
                lblEstadoConexion.setStyle("-fx-text-fill: #27AE60; -fx-font-size: 11px;");
            } else {
                circuloConexion.setFill(Color.web("#E74C3C"));
                lblEstadoConexion.setText("Sin conexion");
                lblEstadoConexion.setStyle("-fx-text-fill: #E74C3C; -fx-font-size: 11px;");
            }
        });

        tarea.setOnFailed(e -> {
            circuloConexion.setFill(Color.web("#E74C3C"));
            lblEstadoConexion.setText("Sin conexion");
            lblEstadoConexion.setStyle("-fx-text-fill: #E74C3C; -fx-font-size: 11px;");
        });

        Thread hilo = new Thread(tarea);
        hilo.setDaemon(true);
        hilo.start();
    }

    @FXML
    void CerrarAPP(ActionEvent event) {
        if (timelineBrillo != null) {
            timelineBrillo.stop();
        }
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }

    /**
     * Intenta iniciar sesion con las credenciales introducidas.
     * La llamada a la API se ejecuta en un hilo de fondo para no bloquear la UI.
     */
    @FXML
    void iniciarSesion(ActionEvent event) {
        lblMensajeError.setVisible(false);

        String dni = txtInsertarDNI.getText().trim().toUpperCase();
        String contrasena = pssInsertarContrasenia.getText();

        // Validaciones basicas en el hilo UI
        if (dni.isEmpty()) {
            mostrarError("Introduce tu DNI");
            txtInsertarDNI.requestFocus();
            return;
        }
        if (contrasena.isEmpty()) {
            mostrarError("Introduce tu contrasena");
            pssInsertarContrasenia.requestFocus();
            return;
        }

        // Deshabilitar boton durante la operacion
        btnEntrar.setDisable(true);

        // Tarea de autenticacion en hilo de fondo
        Task<Sanitario> tareaLogin = new Task<>() {
            @Override
            protected Sanitario call() {
                // 1. Login via API: obtiene tokens y rol
                LoginResponse loginResponse = ApiClient.getInstancia().login(dni, contrasena);

                // 2. Obtener datos completos del sanitario
                Sanitario sanitario = sanitarioDAO.buscarPorDni(dni);
                sanitario.setContrasena(""); // No almacenar contrasena en memoria

                // Guardar tokens y rol en la sesion
                SesionUsuario.getInstancia().iniciarSesion(
                    sanitario.getDni(),
                    sanitario.getNombre(),
                    sanitario.getApellido1() + " " + sanitario.getApellido2(),
                    sanitario.getEmail(),
                    sanitario.getCargo(),
                    loginResponse.accessToken(),
                    loginResponse.refreshToken(),
                    loginResponse.rol()
                );

                return sanitario;
            }
        };

        tareaLogin.setOnSucceeded(e -> {
            if (timelineBrillo != null) {
                timelineBrillo.stop();
            }
            abrirVentanaPrincipal();
        });

        tareaLogin.setOnFailed(e -> {
            btnEntrar.setDisable(false);
            Throwable error = tareaLogin.getException();
            if (error instanceof com.javafx.excepcion.AutenticacionException) {
                mostrarError("DNI o contrasena incorrectos");
                pssInsertarContrasenia.clear();
                pssInsertarContrasenia.requestFocus();
            } else if (error instanceof com.javafx.excepcion.ConexionException) {
                mostrarError("No hay conexion con la API");
                verificarConexionApi();
                VentanaUtil.mostrarVentanaInformativa(
                    "No se puede conectar con el servidor.\n\n" +
                    "Verifica que la API este en ejecucion en localhost:8080.",
                    VentanaUtil.TipoMensaje.ERROR
                );
            } else {
                mostrarError("Error inesperado");
                System.err.println("Error en login: " + error.getMessage());
            }
        });

        Thread hilo = new Thread(tareaLogin);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void mostrarError(String mensaje) {
        lblMensajeError.setText(mensaje);
        lblMensajeError.setVisible(true);
    }

    private void abrirVentanaPrincipal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaPrincipal.fxml"));
            Parent root = loader.load();

            controladorVentanaPrincipal controlador = loader.getController();
            if (controlador != null) {
                controlador.inicializarSesion();
            }

            Scene scene = new Scene(root);
            controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

            Stage stage = new Stage();
            stage.setTitle("RehabiAPP - " + SesionUsuario.getInstancia().getNombreCompleto());
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.setResizable(true);
            VentanaUtil.establecerIconoVentana(stage);

            stage.setOnCloseRequest(e -> {
                SesionUsuario.getInstancia().cerrarSesion();
            });

            stage.show();
            AnimacionUtil.animarVentana(stage, 400);

            Stage loginStage = (Stage) btnEntrar.getScene().getWindow();
            loginStage.close();

        } catch (Exception e) {
            mostrarError("Error al abrir la aplicacion");
            e.printStackTrace();
        }
    }
}
