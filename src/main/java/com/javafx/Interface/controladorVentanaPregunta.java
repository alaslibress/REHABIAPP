package com.javafx.Interface;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

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

    @FXML
    void cerrarVentana(ActionEvent event) {
        Stage stage = (Stage) btnCancelarPregunta.getScene().getWindow();
        stage.close();
    }

    @FXML
    void confirmarPregunta(ActionEvent event) {

    }

}
