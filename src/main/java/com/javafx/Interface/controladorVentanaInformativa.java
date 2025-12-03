package com.javafx.Interface;

import com.javafx.Clases.VentanaUtil.TipoMensaje;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Controlador para la ventana informativa
 * Muestra mensajes al usuario con iconos segun el tipo de mensaje
 */
public class controladorVentanaInformativa {

    @FXML
    private Button btnInformacion;

    @FXML
    private ImageView imgIconoInformacion;

    @FXML
    private Label lblInformacion;

    @FXML
    private VBox vboxVentanaInformativa;

    /**
     * Metodo initialize se ejecuta al cargar el FXML
     */
    @FXML
    public void initialize() {
        //Configurar accion del boton para cerrar la ventana
        btnInformacion.setOnAction(this::cerrarVentana);
    }

    /**
     * Configura la ventana con el mensaje y tipo especificados
     * @param mensaje Texto a mostrar
     * @param tipo Tipo de mensaje que determina el icono a usar
     */
    public void configurarVentana(String mensaje, TipoMensaje tipo) {
        //Establecer el texto del mensaje
        lblInformacion.setText(mensaje);

        //Cargar el icono correspondiente al tipo de mensaje
        try {
            Image icono = new Image(getClass().getResourceAsStream(tipo.getIcono()));
            imgIconoInformacion.setImage(icono);
        } catch (Exception e) {
            System.err.println("Error al cargar icono: " + e.getMessage());
        }
    }

    /**
     * Cierra la ventana informativa
     * @param event Evento del boton
     */
    @FXML
    void cerrarVentana(ActionEvent event) {
        Stage stage = (Stage) btnInformacion.getScene().getWindow();
        stage.close();
    }
}