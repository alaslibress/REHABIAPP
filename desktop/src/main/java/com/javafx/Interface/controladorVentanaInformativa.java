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
        // Establecer el texto del mensaje
        lblInformacion.setText(mensaje);

        // Intentar cargar el icono correspondiente
        java.io.InputStream stream = null;
        
        // Probar cada alternativa de nombre de icono
        for (String nombreIcono : tipo.getIconosAlternativos()) {
            // Intentar con ruta absoluta
            stream = getClass().getResourceAsStream("/" + nombreIcono);
            if (stream != null) {
                System.out.println("Icono encontrado: /" + nombreIcono);
                break;
            }
            
            // Intentar sin barra
            stream = getClass().getResourceAsStream(nombreIcono);
            if (stream != null) {
                System.out.println("Icono encontrado: " + nombreIcono);
                break;
            }
        }
        
        // Si no se encontró ningún icono alternativo, usar info.png como fallback
        if (stream == null) {
            System.err.println("No se encontró icono para " + tipo + ", usando info.png");
            stream = getClass().getResourceAsStream("/info.png");
        }
        
        // Cargar la imagen
        if (stream != null) {
            try {
                Image icono = new Image(stream);
                imgIconoInformacion.setImage(icono);
            } catch (Exception e) {
                System.err.println("Error al crear imagen: " + e.getMessage());
            }
        } else {
            System.err.println("No se pudo cargar ningún icono");
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