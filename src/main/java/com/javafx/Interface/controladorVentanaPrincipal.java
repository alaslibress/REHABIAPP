package com.javafx.Interface;

import com.javafx.Clases.SesionUsuario;
import com.javafx.Clases.VentanaUtil;
import com.javafx.Clases.VentanaUtil.TipoMensaje;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Controlador para la ventana principal de la aplicacion
 * Gestiona la navegacion entre pestañas y las acciones del usuario
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

    //Pestaña actualmente cargada
    private String pestaniaActual = "";

    /**
     * Metodo initialize se ejecuta automaticamente al cargar el FXML
     */
    @FXML
    public void initialize() {
        //Cargar pestaña de pacientes por defecto
        cargarPestania("Pacientes");
    }

    /**
     * Inicializa la sesion del usuario mostrando sus datos
     */
    public void inicializarSesion() {
        SesionUsuario sesion = SesionUsuario.getInstancia();

        if (sesion.haySesionActiva()) {
            lblNombreTemporal.setText(sesion.getNombreCompleto());
            lblCargoTemporal.setText(sesion.getCargo());

            //Ocultar pestaña de sanitarios si no es especialista
            if (!sesion.esEspecialista()) {
                btnPestaniaSanitarios.setVisible(false);
                btnPestaniaSanitarios.setManaged(false);
            }
        }
    }

    /**
     * Carga una pestaña en el contenido principal
     * @param nombrePestania Nombre de la pestaña a cargar
     */
    private void cargarPestania(String nombrePestania) {
        //Evitar recargar la misma pestaña
        if (nombrePestania.equals(pestaniaActual)) {
            return;
        }

        try {
            String rutaFXML = "/Ventana" + nombrePestania + ".fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(rutaFXML));
            Parent contenido = loader.load();

            //Configurar permisos segun el controlador
            Object controlador = loader.getController();
            if (controlador instanceof controladorVentanaSanitarios) {
                ((controladorVentanaSanitarios) controlador).configurarPermisos();
            } else if (controlador instanceof controladorVentanaPacientes) {
                ((controladorVentanaPacientes) controlador).configurarPermisos();
            }

            //Cargar contenido en el centro del BorderPane
            bdpPrincipal.setCenter(contenido);
            pestaniaActual = nombrePestania;

            System.out.println("Pestaña cargada: " + nombrePestania);

        } catch (Exception e) {
            System.err.println("Error al cargar pestaña " + nombrePestania + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Abre la pestaña de sanitarios
     */
    @FXML
    void abrirPestaniaSanitarios(ActionEvent event) {
        cargarPestania("Sanitarios");
    }

    /**
     * Abre la pestaña de pacientes
     */
    @FXML
    void abrirPestaniaPacientes(ActionEvent event) {
        cargarPestania("Pacientes");
    }

    /**
     * Abre la pestaña de citas
     */
    @FXML
    void abrirPestaniaCitas(ActionEvent event) {
        cargarPestania("Citas");
    }

    /**
     * Abre la pestaña de ayuda
     */
    @FXML
    void abrirPestaniaAyuda(ActionEvent event) {
        cargarPestania("Ayuda");
    }

    /**
     * Realiza una busqueda rapida
     */
    @FXML
    void busquedaRapida(ActionEvent event) {
        String textoBusqueda = txfBusquedaRapida.getText().trim();

        if (textoBusqueda.isEmpty()) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Introduce un texto para buscar.",
                    TipoMensaje.ADVERTENCIA
            );
            return;
        }

        //Buscar en la pestaña actual
        //Obtener el controlador de la pestaña actual
        if (bdpPrincipal.getCenter() != null) {
            //Dependiendo de la pestaña actual, ejecutar la busqueda
            System.out.println("Busqueda rapida: " + textoBusqueda + " en pestaña " + pestaniaActual);
        }
    }

    /**
     * Abre la ventana de opciones/ajustes
     */
    @FXML
    void abrirVentanaOpciones(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaOpciones.fxml"));
            Parent root = loader.load();

            //Crear escena y aplicar CSS
            Scene scene = new Scene(root);
            controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

            Stage stage = new Stage();
            stage.setTitle("Opciones");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

            //Recargar configuracion en la ventana principal despues de cerrar opciones
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

            //Obtener controlador y cargar datos
            controladorPerfil controlador = loader.getController();
            controlador.cargarDatosUsuario();

            //Crear escena y aplicar CSS
            Scene scene = new Scene(root);
            controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

            Stage stage = new Stage();
            stage.setTitle("Mi Perfil");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

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
                "¿Esta seguro de que desea cerrar sesion?"
        );

        if (confirmado) {
            //Cerrar sesion
            SesionUsuario.getInstancia().cerrarSesion();

            try {
                //Cargar ventana de login
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/SGEInicioSesion.fxml"));
                Parent root = loader.load();

                //Crear escena y aplicar CSS
                Scene scene = new Scene(root);
                controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

                Stage stage = new Stage();
                stage.setTitle("RehabiAPP - Inicio de Sesion");
                stage.setScene(scene);
                stage.setResizable(false);
                stage.show();

                //Cerrar ventana actual
                Stage ventanaActual = (Stage) btnSalir.getScene().getWindow();
                ventanaActual.close();

            } catch (Exception e) {
                System.err.println("Error al volver al login: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
