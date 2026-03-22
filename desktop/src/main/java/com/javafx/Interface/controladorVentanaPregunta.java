package com.javafx.Interface;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Controlador para la ventana de pregunta/confirmacion
 * Permite al usuario confirmar o cancelar una accion
 */
public class controladorVentanaPregunta {

    @FXML
    private Button btnCancelarPregunta;

    @FXML
    private Button btnConfirmarPregunta;

    @FXML
    private ImageView imgPregunta;

    @FXML
    private Label lblPregunta;

    @FXML
    private VBox vboxVentanaPregunta;

    //Variable para almacenar la respuesta del usuario
    private boolean confirmado = false;

    /**
     * Metodo initialize se ejecuta al cargar el FXML
     */
    @FXML
    public void initialize() {
        //Inicializacion si es necesaria
    }

    /**
     * Configura el texto de la pregunta a mostrar
     * @param mensaje Texto de la pregunta
     */
    public void configurarPregunta(String mensaje) {
        lblPregunta.setText(mensaje);
    }

    /**
     * Devuelve si el usuario confirmo la accion
     * @return true si confirmo, false si cancelo
     */
    public boolean isConfirmado() {
        return confirmado;
    }

    /**
     * Cierra la ventana sin confirmar (cancelar)
     * @param event Evento del boton
     */
    @FXML
    void cerrarVentana(ActionEvent event) {
        confirmado = false;
        Stage stage = (Stage) btnCancelarPregunta.getScene().getWindow();
        stage.close();
    }

    /**
     * Confirma la accion y cierra la ventana
     * @param event Evento del boton
     */
    @FXML
    void confirmarPregunta(ActionEvent event) {
        confirmado = true;
        Stage stage = (Stage) btnConfirmarPregunta.getScene().getWindow();
        stage.close();
    }
}