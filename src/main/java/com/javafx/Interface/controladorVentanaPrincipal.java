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
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Controlador para la ventana principal de la aplicacion
 * Gestiona la navegacion entre pestañas y los permisos de usuario
 */
public class controladorVentanaPrincipal {

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
    private ImageView imgAjustes;

    @FXML
    private ImageView imgLogoStage;

    @FXML
    private ImageView imgSalir;

    @FXML
    private Label lblBuscarRapido;

    @FXML
    private Label lblCargo;

    @FXML
    private Label lblCargoTemporal;

    @FXML
    private Label lblNombreTemporal;

    @FXML
    private Label lblNombreUsuario;

    @FXML
    private Label lblSlogan;

    @FXML
    private Label lblTemporalPestaniaPrincipal;

    @FXML
    private TextField txfBusquedaRapida;

    @FXML
    private VBox vboxContenidoPrincipal;

    //Referencia a la sesion del usuario
    private SesionUsuario sesion;

    //Controlador actual de la pestania cargada
    private Object controladorActual;

    /**
     * Metodo initialize se ejecuta automaticamente al cargar el FXML
     */
    @FXML
    public void initialize() {
        //Obtener la sesion del usuario
        sesion = SesionUsuario.getInstancia();

        //Configurar eventos de los botones de pestañas
        btnPestaniaSanitarios.setOnAction(e -> cargarPestaniaSanitarios());
        btnPestaniaPacientes.setOnAction(e -> cargarPestaniaPacientes());
        btnPestaniaCitas.setOnAction(e -> cargarPestaniaCitas());
        btnPestaniaAyuda.setOnAction(e -> cargarPestania("/VentanaAyuda.fxml"));
        btnAjustes.setOnAction(e -> abrirVentanaOpciones());
        btnPerfil.setOnAction(e -> abrirVentanaPerfil());
        btnSalir.setOnAction(e -> cerrarSesion());
        btnBuscar.setOnAction(e -> realizarBusquedaRapida());

        //Configurar evento Enter en campo de busqueda rapida
        txfBusquedaRapida.setOnAction(e -> realizarBusquedaRapida());

        //Cambiar texto del label de busqueda
        if (lblBuscarRapido != null) {
            lblBuscarRapido.setText("Buscar cita:");
        }

        //Cambiar placeholder del campo de busqueda
        txfBusquedaRapida.setPromptText("dd/mm/aaaa");

        //Inicializar informacion del usuario si hay sesion activa
        if (sesion.haySesionActiva()) {
            actualizarInfoUsuario();
        }
    }

    /**
     * Realiza una busqueda rapida de citas por fecha
     * Navega a la pestania de citas y selecciona la fecha indicada
     */
    private void realizarBusquedaRapida() {
        String textoBusqueda = txfBusquedaRapida.getText().trim();

        if (textoBusqueda.isEmpty()) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Introduce una fecha para buscar citas.\nFormato: dd/mm/aaaa",
                    TipoMensaje.INFORMACION
            );
            return;
        }

        //Intentar parsear la fecha
        java.time.LocalDate fechaBuscada = parsearFecha(textoBusqueda);

        if (fechaBuscada == null) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Formato de fecha no valido.\nUsa el formato: dd/mm/aaaa\nEjemplo: 15/03/2025",
                    TipoMensaje.ADVERTENCIA
            );
            return;
        }

        //Cargar pestania de citas con la fecha seleccionada
        cargarPestaniaCitasConFecha(fechaBuscada);
    }

    /**
     * Parsea una fecha en varios formatos posibles
     * @param texto Texto con la fecha
     * @return LocalDate o null si no se puede parsear
     */
    private java.time.LocalDate parsearFecha(String texto) {
        //Formatos aceptados
        String[] formatos = {
                "dd/MM/yyyy",
                "d/M/yyyy",
                "dd-MM-yyyy",
                "d-M-yyyy",
                "yyyy-MM-dd",
                "dd/MM/yy",
                "d/M/yy"
        };

        for (String formato : formatos) {
            try {
                java.time.format.DateTimeFormatter formatter =
                        java.time.format.DateTimeFormatter.ofPattern(formato);
                return java.time.LocalDate.parse(texto, formatter);
            } catch (Exception e) {
                //Intentar siguiente formato
            }
        }

        return null;
    }

    /**
     * Carga la pestania de citas y selecciona una fecha especifica
     * @param fecha Fecha a seleccionar en el calendario
     */
    private void cargarPestaniaCitasConFecha(java.time.LocalDate fecha) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaCitas.fxml"));
            Parent pestania = loader.load();

            controladorVentanaCitas controlador = loader.getController();
            if (controlador != null) {
                controlador.setDniSanitario(sesion.getDniUsuario());
                controlador.seleccionarFecha(fecha);
            }

            controladorActual = controlador;
            vboxContenidoPrincipal.getChildren().clear();
            vboxContenidoPrincipal.getChildren().add(pestania);

            System.out.println("Citas cargadas para fecha: " + fecha);

        } catch (Exception e) {
            System.err.println("Error al cargar pestania de citas: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Inicializa la sesion y actualiza la interfaz
     * Se llama desde el controlador de login
     */
    public void inicializarSesion() {
        sesion = SesionUsuario.getInstancia();
        actualizarInfoUsuario();
        aplicarPermisos();

        //Cargar pestaña de citas por defecto
        cargarPestaniaCitas();
    }

    /**
     * Actualiza los labels con la informacion del usuario logueado
     */
    private void actualizarInfoUsuario() {
        if (sesion.haySesionActiva()) {
            //Actualizar nombre del usuario
            if (lblNombreTemporal != null) {
                lblNombreTemporal.setText(sesion.getNombreCompleto());
            }

            //Actualizar cargo
            if (lblCargoTemporal != null) {
                lblCargoTemporal.setText(sesion.getCargo());
            }
        }
    }

    /**
     * Aplica restricciones de permisos segun el cargo del usuario
     * Enfermeros: solo pueden ver datos y gestionar citas
     * Especialistas: acceso completo
     */
    private void aplicarPermisos() {
        if (!sesion.esEspecialista()) {
            //Los no especialistas (enfermeros) tienen acceso limitado

            //No pueden acceder a la gestion de sanitarios
            btnPestaniaSanitarios.setDisable(true);
            btnPestaniaSanitarios.setOpacity(0.5);
            btnPestaniaSanitarios.setTooltip(new javafx.scene.control.Tooltip(
                    "No tienes permisos para gestionar sanitarios"));

            System.out.println("Permisos restringidos aplicados para: " + sesion.getCargo());
        } else {
            System.out.println("Permisos completos aplicados para: " + sesion.getCargo());
        }
    }

    /**
     * Carga la pestaña de sanitarios verificando permisos
     */
    private void cargarPestaniaSanitarios() {
        if (!sesion.puedeInsertarUsuarios()) {
            VentanaUtil.mostrarVentanaInformativa(
                    "No tienes permisos para acceder a la gestion de sanitarios.",
                    TipoMensaje.ADVERTENCIA
            );
            return;
        }
        cargarPestaniaSanitariosConReferencia();
    }

    /**
     * Carga la pestaña de pacientes
     * Los enfermeros pueden ver pero no editar
     */
    private void cargarPestaniaPacientes() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaPacientes.fxml"));
            Parent pestania = loader.load();

            //Obtener el controlador y configurar permisos
            controladorVentanaPacientes controlador = loader.getController();
            if (controlador != null) {
                controlador.configurarPermisos();
            }

            //Guardar referencia al controlador actual
            controladorActual = controlador;

            vboxContenidoPrincipal.getChildren().clear();
            vboxContenidoPrincipal.getChildren().add(pestania);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Carga la pestania de citas
     */
    private void cargarPestaniaCitas() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaCitas.fxml"));
            Parent pestania = loader.load();

            //Obtener el controlador
            controladorVentanaCitas controlador = loader.getController();
            if (controlador != null) {
                controlador.setDniSanitario(sesion.getDniUsuario());
            }

            //Guardar referencia al controlador actual
            controladorActual = controlador;

            vboxContenidoPrincipal.getChildren().clear();
            vboxContenidoPrincipal.getChildren().add(pestania);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Abre la ventana de opciones
     */
    private void abrirVentanaOpciones() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaOpciones.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Opciones");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);

            //Establecer owner para que la configuracion se aplique
            Stage stagePrincipal = (Stage) vboxContenidoPrincipal.getScene().getWindow();
            stage.initOwner(stagePrincipal);

            stage.setResizable(false);
            stage.showAndWait();

        } catch (Exception e) {
            System.err.println("Error al abrir ventana de opciones: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Carga una pestaña en el contenedor principal
     */
    private void cargarPestania(String archivo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(archivo));
            Parent pestania = loader.load();

            //Configurar permisos en controladores que lo soporten
            Object controlador = loader.getController();
            configurarPermisosControlador(controlador);

            //Guardar referencia al controlador actual
            controladorActual = controlador;

            vboxContenidoPrincipal.getChildren().clear();
            vboxContenidoPrincipal.getChildren().add(pestania);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Carga la pestania de sanitarios guardando referencia
     */
    private void cargarPestaniaSanitariosConReferencia() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaSanitarios.fxml"));
            Parent pestania = loader.load();

            controladorVentanaSanitarios controlador = loader.getController();
            if (controlador != null) {
                controlador.configurarPermisos();
            }

            controladorActual = controlador;

            vboxContenidoPrincipal.getChildren().clear();
            vboxContenidoPrincipal.getChildren().add(pestania);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Configura los permisos en el controlador si tiene el metodo correspondiente
     */
    private void configurarPermisosControlador(Object controlador) {
        if (controlador instanceof controladorVentanaPacientes) {
            ((controladorVentanaPacientes) controlador).configurarPermisos();
        } else if (controlador instanceof controladorVentanaSanitarios) {
            ((controladorVentanaSanitarios) controlador).configurarPermisos();
        } else if (controlador instanceof controladorVentanaCitas) {
            ((controladorVentanaCitas) controlador).setDniSanitario(sesion.getDniUsuario());
        }
    }

    /**
     * Abre la ventana de perfil del usuario
     */
    private void abrirVentanaPerfil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaPerfil.fxml"));
            Parent root = loader.load();

            //Pasar datos al controlador de perfil
            controladorPerfil controlador = loader.getController();
            if (controlador != null) {
                controlador.cargarDatosUsuario();
            }

            Stage stage = new Stage();
            stage.setTitle("Mi Perfil");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

            //Actualizar info del usuario por si cambio algo
            actualizarInfoUsuario();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Abre una ventana modal
     */
    private void abrirVentana(String archivo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(archivo));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Cierra la sesion actual y vuelve a la pantalla de login
     */
    private void cerrarSesion() {
        //Confirmar cierre de sesion
        boolean confirmar = VentanaUtil.mostrarVentanaPregunta(
                "¿Deseas cerrar sesion?"
        );

        if (!confirmar) {
            return;
        }

        try {
            //Cerrar sesion
            SesionUsuario.getInstancia().cerrarSesion();

            //Abrir ventana de login
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/SGEInicioSesion.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("RehabiAPP - Inicio de Sesion");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.show();

            //Cerrar ventana actual
            Stage actual = (Stage) btnSalir.getScene().getWindow();
            actual.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Realiza una busqueda rapida de pacientes
     */
    @FXML
    void Buscar(ActionEvent event) {
        String textoBusqueda = txfBusquedaRapida.getText().trim();

        if (textoBusqueda.isEmpty()) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Introduce un termino de busqueda (DNI, nombre o NSS).",
                    TipoMensaje.INFORMACION
            );
            return;
        }

        //Cargar pestaña de pacientes con el filtro aplicado
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaPacientes.fxml"));
            Parent pestania = loader.load();

            controladorVentanaPacientes controlador = loader.getController();
            if (controlador != null) {
                controlador.configurarPermisos();
                controlador.ejecutarBusqueda(textoBusqueda);
            }

            vboxContenidoPrincipal.getChildren().clear();
            vboxContenidoPrincipal.getChildren().add(pestania);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void Perfil(ActionEvent event) {
        abrirVentanaPerfil();
    }
}