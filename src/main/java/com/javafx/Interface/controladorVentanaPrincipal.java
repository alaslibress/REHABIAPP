package com.javafx.Interface;

import com.javafx.Clases.AnimacionUtil;
import com.javafx.Clases.ConexionBD;
import com.javafx.Clases.SesionUsuario;
import com.javafx.Clases.VentanaUtil;
import com.javafx.Clases.VentanaUtil.TipoMensaje;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Controlador para la ventana principal de la aplicacion
 * Gestiona la navegacion entre pestañas y las acciones del usuario
 * 
 * ACTUALIZADO: Indicador de conexión a BD + Animaciones de pestañas
 */
public class controladorVentanaPrincipal {

    @FXML
    private BorderPane bdpPrincipal;

    @FXML
    private Button btnAjustes;

    @FXML
    private Button btnBuscar;

    @FXML
    private Button btnPerfil;

    @FXML
    private Button btnPestaniaAyuda;

    @FXML
    private Button btnPestaniaCitas;

    @FXML
    private Button btnPestaniaPacientes;

    @FXML
    private Button btnPestaniaSanitarios;

    @FXML
    private Button btnSalir;

    @FXML
    private ImageView imgLogo;

    @FXML
    private Label lblCargoTemporal;

    @FXML
    private Label lblNombreTemporal;

    @FXML
    private Label lblSlogan;

    @FXML
    private TextField txfBusquedaRapida;

    @FXML
    private VBox vboxContenidoPrincipal;

    @FXML
    private VBox vboxMenuLateral;
    
    // Indicador de conexión a BD
    @FXML
    private HBox hboxIndicadorConexion;
    
    @FXML
    private Circle circuloConexion;
    
    @FXML
    private Label lblEstadoConexion;

    // Pestaña actualmente cargada
    private String pestaniaActual = "";

    // Menu contextual para acceso rapido
    private ContextMenu menuContextual;

    // Botón de pestaña actualmente seleccionado
    private Button botonPestaniaActual = null;

    // Timeline para verificación periódica de conexión BD
    private Timeline verificadorConexion;

    /**
     * Metodo initialize se ejecuta automaticamente al cargar el FXML
     */
    @FXML
    public void initialize() {
        // Verificar conexión a BD inicialmente
        verificarConexionBD();

        // Iniciar verificador periódico (cada 5 segundos)
        iniciarVerificadorConexion();

        // Permitir reconexión manual al hacer clic en el indicador
        hboxIndicadorConexion.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                verificarConexionBD();
            }
        });
        hboxIndicadorConexion.setStyle(hboxIndicadorConexion.getStyle() + "; -fx-cursor: hand;");

        // Crear menu contextual
        crearMenuContextual();

        // Permitir busqueda con Enter
        txfBusquedaRapida.setOnAction(e -> busquedaRapida(null));

        // Cargar pestaña de pacientes por defecto
        cargarPestania("Pacientes");

        // Marcar pestaña inicial como seleccionada
        marcarPestaniaSeleccionada(btnPestaniaPacientes);
    }

    /**
     * Inicia un verificador periódico que actualiza el estado de la conexión cada 5 segundos
     */
    private void iniciarVerificadorConexion() {
        verificadorConexion = new Timeline(
            new KeyFrame(Duration.seconds(5), e -> verificarConexionBD())
        );
        verificadorConexion.setCycleCount(Timeline.INDEFINITE);
        verificadorConexion.play();
    }

    /**
     * Verifica la conexión a la base de datos y actualiza el indicador visual
     */
    private void verificarConexionBD() {
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
    }

    /**
     * Marca visualmente la pestaña seleccionada con animación
     */
    private void marcarPestaniaSeleccionada(Button botonSeleccionado) {
        // Quitar estilo de selección del botón anterior
        if (botonPestaniaActual != null) {
            botonPestaniaActual.getStyleClass().remove("pestania-seleccionada");
            // Animación de deselección
            ScaleTransition scaleOut = new ScaleTransition(Duration.millis(150), botonPestaniaActual);
            scaleOut.setToX(1.0);
            scaleOut.setToY(1.0);
            scaleOut.play();
        }
        
        // Aplicar estilo al nuevo botón seleccionado
        if (botonSeleccionado != null) {
            botonSeleccionado.getStyleClass().add("pestania-seleccionada");
            
            // Animación de selección
            ScaleTransition scaleIn = new ScaleTransition(Duration.millis(200), botonSeleccionado);
            scaleIn.setFromX(1.0);
            scaleIn.setFromY(1.0);
            scaleIn.setToX(1.05);
            scaleIn.setToY(1.05);
            scaleIn.play();
            
            botonPestaniaActual = botonSeleccionado;
        }
    }

    /**
     * Crea el menu contextual con accesos rapidos
     */
    private void crearMenuContextual() {
        menuContextual = new ContextMenu();

        // Item: Perfil
        MenuItem itemPerfil = new MenuItem("Mi Perfil");
        itemPerfil.setOnAction(e -> abrirVentanaPerfil(null));

        // Item: Configuracion
        MenuItem itemConfiguracion = new MenuItem("Configuración");
        itemConfiguracion.setOnAction(e -> abrirVentanaOpciones(null));

        // Item: Ayuda
        MenuItem itemAyuda = new MenuItem("Ayuda");
        itemAyuda.setOnAction(e -> {
            cargarPestania("Ayuda");
            marcarPestaniaSeleccionada(btnPestaniaAyuda);
        });

        // Separador
        SeparatorMenuItem separador = new SeparatorMenuItem();

        // Item: Cerrar sesion
        MenuItem itemCerrarSesion = new MenuItem("Cerrar Sesión");
        itemCerrarSesion.setOnAction(e -> cerrarSesion(null));

        // Añadir todos los items al menu
        menuContextual.getItems().addAll(
                itemPerfil,
                itemConfiguracion,
                itemAyuda,
                separador,
                itemCerrarSesion
        );

        // Configurar evento de click derecho en el BorderPane principal
        bdpPrincipal.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                menuContextual.show(bdpPrincipal, event.getScreenX(), event.getScreenY());
            } else {
                // Ocultar menu si se hace click izquierdo
                menuContextual.hide();
            }
        });
    }

    /**
     * Inicializa la sesion del usuario mostrando sus datos
     */
    public void inicializarSesion() {
        SesionUsuario sesion = SesionUsuario.getInstancia();

        if (sesion.haySesionActiva()) {
            lblNombreTemporal.setText(sesion.getNombreCompleto());
            lblCargoTemporal.setText(sesion.getCargo());

            // Ocultar pestaña de sanitarios si no es especialista
            if (!sesion.esEspecialista()) {
                btnPestaniaSanitarios.setVisible(false);
                btnPestaniaSanitarios.setManaged(false);
            }
        }
    }

    /**
     * Carga una pestaña generica en el contenido principal
     * @param nombrePestania Nombre de la pestaña a cargar (Sanitarios, Pacientes, Ayuda)
     */
    private void cargarPestania(String nombrePestania) {
        // Evitar recargar la misma pestaña
        if (nombrePestania.equals(pestaniaActual)) {
            return;
        }

        try {
            String rutaFXML = "/Ventana" + nombrePestania + ".fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(rutaFXML));
            Parent contenido = loader.load();

            // Configurar permisos segun el controlador
            Object controlador = loader.getController();
            if (controlador instanceof controladorVentanaSanitarios) {
                ((controladorVentanaSanitarios) controlador).configurarPermisos();
            } else if (controlador instanceof controladorVentanaPacientes) {
                ((controladorVentanaPacientes) controlador).configurarPermisos();
            }

            // Cargar contenido en el centro del BorderPane
            bdpPrincipal.setCenter(contenido);
            pestaniaActual = nombrePestania;

            System.out.println("Pestaña cargada: " + nombrePestania);

        } catch (Exception e) {
            System.err.println("Error al cargar pestaña " + nombrePestania + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Referencia al controlador de citas para poder filtrar desde búsqueda
    private controladorVentanaCitas controladorCitasActual;

    /**
     * Carga la pestaña de citas con configuración especial
     * La pestaña de citas requiere establecer el DNI del sanitario actual
     */
    private void cargarPestaniaCitas() {
        // Evitar recargar la misma pestaña
        if ("Citas".equals(pestaniaActual)) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaCitas.fxml"));
            Parent contenido = loader.load();

            // Obtener controlador y configurar el DNI del sanitario actual
            controladorCitasActual = loader.getController();
            if (controladorCitasActual != null) {
                SesionUsuario sesion = SesionUsuario.getInstancia();
                if (sesion.haySesionActiva()) {
                    controladorCitasActual.setDniSanitario(sesion.getDniUsuario());
                }
            }

            // Cargar contenido en el centro del BorderPane
            bdpPrincipal.setCenter(contenido);
            pestaniaActual = "Citas";

            System.out.println("Pestaña cargada: Citas");

        } catch (Exception e) {
            System.err.println("Error al cargar pestaña Citas: " + e.getMessage());
            e.printStackTrace();
            VentanaUtil.mostrarVentanaInformativa(
                    "Error al cargar la pestaña de citas.",
                    TipoMensaje.ERROR
            );
        }
    }

    /**
     * Carga la pestaña de citas y aplica un filtro de búsqueda
     * @param textoBusqueda Texto a buscar (fecha o nombre de paciente)
     */
    private void cargarPestaniaCitasConFiltro(String textoBusqueda) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaCitas.fxml"));
            Parent contenido = loader.load();

            // Obtener controlador
            controladorCitasActual = loader.getController();
            if (controladorCitasActual != null) {
                SesionUsuario sesion = SesionUsuario.getInstancia();
                if (sesion.haySesionActiva()) {
                    controladorCitasActual.setDniSanitario(sesion.getDniUsuario());
                }
                // Aplicar el filtro de búsqueda
                controladorCitasActual.filtrarPorTexto(textoBusqueda);
            }

            // Cargar contenido en el centro del BorderPane
            bdpPrincipal.setCenter(contenido);
            pestaniaActual = "Citas";
            
            // Marcar pestaña como seleccionada
            marcarPestaniaSeleccionada(btnPestaniaCitas);

            System.out.println("Pestaña Citas cargada con filtro: " + textoBusqueda);

        } catch (Exception e) {
            System.err.println("Error al cargar pestaña Citas con filtro: " + e.getMessage());
            e.printStackTrace();
            VentanaUtil.mostrarVentanaInformativa(
                    "Error al cargar la pestaña de citas.",
                    TipoMensaje.ERROR
            );
        }
    }

    /**
     * Abre la pestaña de sanitarios
     */
    @FXML
    void abrirPestaniaSanitarios(ActionEvent event) {
        cargarPestania("Sanitarios");
        marcarPestaniaSeleccionada(btnPestaniaSanitarios);
    }

    /**
     * Abre la pestaña de pacientes
     */
    @FXML
    void abrirPestaniaPacientes(ActionEvent event) {
        cargarPestania("Pacientes");
        marcarPestaniaSeleccionada(btnPestaniaPacientes);
    }

    /**
     * Abre la pestaña de citas
     */
    @FXML
    void abrirPestaniaCitas(ActionEvent event) {
        cargarPestaniaCitas();
        marcarPestaniaSeleccionada(btnPestaniaCitas);
    }

    /**
     * Abre la pestaña de ayuda
     */
    @FXML
    void abrirPestaniaAyuda(ActionEvent event) {
        cargarPestania("Ayuda");
        marcarPestaniaSeleccionada(btnPestaniaAyuda);
    }

    /**
     * Realiza una búsqueda rápida de citas
     * Busca por fecha (dd/MM/yyyy) o por nombre de paciente
     * Lleva automáticamente a la pestaña de Citas con el filtro aplicado
     */
    @FXML
    void busquedaRapida(ActionEvent event) {
        String textoBusqueda = txfBusquedaRapida.getText().trim();

        if (textoBusqueda.isEmpty()) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Introduce una fecha (dd/MM/yyyy) o nombre de paciente para buscar citas.",
                    TipoMensaje.ADVERTENCIA
            );
            return;
        }

        // Cargar pestaña de citas con el filtro aplicado
        cargarPestaniaCitasConFiltro(textoBusqueda);
        
        // Limpiar campo de búsqueda
        txfBusquedaRapida.clear();
    }

    /**
     * Abre la ventana de opciones/ajustes
     */
    @FXML
    void abrirVentanaOpciones(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaOpciones.fxml"));
            Parent root = loader.load();

            // Crear escena y aplicar CSS
            Scene scene = new Scene(root);
            controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

            Stage stage = new Stage();
            stage.setTitle("Opciones");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            
            // Establecer icono
            VentanaUtil.establecerIconoVentana(stage);
            
            // Mostrar con animación
            stage.setOnShown(e -> AnimacionUtil.animarVentanaModal(stage));
            stage.showAndWait();

            // Recargar configuracion en la ventana principal despues de cerrar opciones
            controladorVentanaOpciones.aplicarConfiguracionAScene(bdpPrincipal.getScene());

        } catch (Exception e) {
            System.err.println("Error al abrir ventana de opciones: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Abre la ventana de perfil del usuario
     */
    @FXML
    void abrirVentanaPerfil(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaPerfil.fxml"));
            Parent root = loader.load();

            // Obtener controlador y cargar datos
            controladorPerfil controlador = loader.getController();
            controlador.cargarDatosUsuario();

            // Crear escena y aplicar CSS
            Scene scene = new Scene(root);
            controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

            Stage stage = new Stage();
            stage.setTitle("Mi Perfil");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            
            // Establecer icono
            VentanaUtil.establecerIconoVentana(stage);
            
            // Mostrar con animación
            stage.setOnShown(e -> AnimacionUtil.animarVentanaModal(stage));
            stage.showAndWait();

            // Actualizar datos del usuario en caso de cambios
            inicializarSesion();

        } catch (Exception e) {
            System.err.println("Error al abrir ventana de perfil: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Cierra la sesion y vuelve a la pantalla de login
     */
    @FXML
    void cerrarSesion(ActionEvent event) {
        boolean confirmado = VentanaUtil.mostrarVentanaPregunta(
                "¿Está seguro de que desea cerrar sesión?"
        );

        if (confirmado) {
            // Cerrar sesion
            SesionUsuario.getInstancia().cerrarSesion();

            try {
                // Cargar ventana de login
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/SGEInicioSesion.fxml"));
                Parent root = loader.load();

                // Crear escena y aplicar CSS
                Scene scene = new Scene(root);
                controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

                Stage stage = new Stage();
                stage.setTitle("RehabiAPP - Inicio de Sesión");
                stage.setScene(scene);
                stage.setResizable(false);
                
                // Establecer icono
                VentanaUtil.establecerIconoVentana(stage);
                
                // Mostrar con animación
                stage.show();
                AnimacionUtil.animarVentana(stage, 400);

                // Cerrar ventana actual
                Stage ventanaActual = (Stage) btnSalir.getScene().getWindow();
                ventanaActual.close();

            } catch (Exception e) {
                System.err.println("Error al volver al login: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
