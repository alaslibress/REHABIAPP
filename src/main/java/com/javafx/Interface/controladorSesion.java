package com.javafx.Interface;

import com.javafx.Clases.AnimacionUtil;
import com.javafx.Clases.ConexionBD;
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
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
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

    // Indicador de conexión a BD
    @FXML
    private HBox hboxIndicadorConexion;

    @FXML
    private Circle circuloConexion;

    @FXML
    private Label lblEstadoConexion;

    // DAO para autenticacion
    private SanitarioDAO sanitarioDAO;

    // Timeline para el efecto de brillo (guardar referencia para poder detenerlo)
    private Timeline timelineBrillo;

    /**
     * Metodo initialize se ejecuta automaticamente al cargar el FXML
     */
    @FXML
    public void initialize() {
        try {
            sanitarioDAO = new SanitarioDAO();

            // Ocultar mensaje de error inicialmente
            lblMensajeError.setVisible(false);

            // Verificar estado de conexión a la base de datos
            verificarConexionBD();

            // Verificar y crear usuario admin si no existe (solo si hay conexión)
            verificarUsuarioAdmin();

            // Configurar botón Entrar como botón por defecto (se activa con Enter)
            // Esto permite presionar Enter desde cualquier campo del formulario
            btnEntrar.setDefaultButton(true);

            // ========== ANIMACIONES ==========

            // 1. Logo aparece poco a poco (FadeTransition)
            AnimacionUtil.fadeIn(imgLogo, 1500);

            // 2. Efecto brillo suave en campo de contraseña (Timeline con KeyFrames)
            timelineBrillo = AnimacionUtil.brilloCampoTexto(pssInsertarContrasenia);

            // 3. Efecto hover en botones
            AnimacionUtil.brilloHover(btnEntrar, Color.rgb(93, 173, 226, 0.8));
            AnimacionUtil.brilloHover(btnCancelar, Color.rgb(231, 76, 60, 0.8));

        } catch (Exception e) {
            System.err.println("Error en inicialización del login: " + e.getMessage());
            e.printStackTrace();
            // La aplicación sigue funcionando, solo mostrar el error en consola
            // El usuario verá el indicador de "Sin conexión"
        }
    }

    /**
     * Verifica el estado de la conexión a la base de datos
     * y actualiza el indicador visual en pantalla
     * Nunca lanza excepciones - siempre muestra el estado actual
     */
    private void verificarConexionBD() {
        try {
            boolean conectado = ConexionBD.probarConexion();

            if (conectado) {
                circuloConexion.setFill(Color.web("#27AE60")); // Verde
                lblEstadoConexion.setText("Conectado");
                lblEstadoConexion.setStyle("-fx-text-fill: #27AE60; -fx-font-size: 11px;");
            } else {
                circuloConexion.setFill(Color.web("#E74C3C")); // Rojo
                lblEstadoConexion.setText("Sin conexión");
                lblEstadoConexion.setStyle("-fx-text-fill: #E74C3C; -fx-font-size: 11px;");
            }
        } catch (Exception e) {
            // En caso de cualquier error, mostrar "Sin conexión"
            System.err.println("Error al verificar conexión: " + e.getMessage());
            circuloConexion.setFill(Color.web("#E74C3C")); // Rojo
            lblEstadoConexion.setText("Sin conexión");
            lblEstadoConexion.setStyle("-fx-text-fill: #E74C3C; -fx-font-size: 11px;");
        }
    }

    /**
     * Verifica si existe el usuario admin y lo crea si no existe
     * Solo se ejecuta si hay conexión a la base de datos
     */
    private void verificarUsuarioAdmin() {
        // Solo intentar crear admin si hay conexión
        if (!ConexionBD.probarConexion()) {
            System.out.println("No hay conexión a BD - omitiendo verificación de usuario admin");
            return;
        }

        try {
            boolean creado = sanitarioDAO.crearAdminSiNoExiste();
            if (creado) {
                System.out.println("Sistema listo con usuario admin");
            }
        } catch (Exception e) {
            System.err.println("Error al verificar usuario admin: " + e.getMessage());
            // No propagar la excepción - la app sigue funcionando
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

        try {
            // Verificar primero si hay conexión a la base de datos
            if (!ConexionBD.probarConexion()) {
                mostrarError("No hay conexión con la base de datos");
                verificarConexionBD(); // Actualizar indicador visual
                VentanaUtil.mostrarVentanaInformativa(
                    "No se puede iniciar sesión sin conexión a la base de datos.\n\n" +
                    "Por favor, verifica:\n" +
                    "• Que el servidor PostgreSQL esté ejecutándose\n" +
                    "• La configuración de red y firewall\n" +
                    "• El archivo config.properties",
                    VentanaUtil.TipoMensaje.ERROR
                );
                return;
            }

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

            // Intentar autenticar (protegido con try-catch)
            Sanitario sanitario = null;
            try {
                sanitario = sanitarioDAO.autenticar(dni, contrasena);
            } catch (Exception e) {
                System.err.println("Error en autenticación: " + e.getMessage());
                e.printStackTrace();
                mostrarError("Error al conectar con la base de datos");
                VentanaUtil.mostrarVentanaInformativa(
                    "Se perdió la conexión con la base de datos durante la autenticación.\n\n" +
                    "Por favor, verifica la conexión e inténtalo de nuevo.",
                    VentanaUtil.TipoMensaje.ERROR
                );
                verificarConexionBD(); // Actualizar indicador visual
                return;
            }

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

        } catch (Exception e) {
            // Captura cualquier otra excepción no prevista
            System.err.println("Error inesperado en iniciarSesion: " + e.getMessage());
            e.printStackTrace();
            mostrarError("Error inesperado");
            VentanaUtil.mostrarVentanaInformativa(
                "Ha ocurrido un error inesperado.\n\n" +
                "Por favor, verifica la conexión a la base de datos.",
                VentanaUtil.TipoMensaje.ERROR
            );
            verificarConexionBD(); // Actualizar indicador visual
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
