package com.javafx.Interface;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class controladorPerfilEditar {

    @FXML
    private Button btnCancelarSanitarioCambiar;

    @FXML
    private Button btnCrearSanitarioCambiar;

    @FXML
    private CheckBox cbxEspecialistaCambiar;

    @FXML
    private Label lblTituloVentana;

    @FXML
    private PasswordField pssContraseniaCambiarSanitario;

    @FXML
    private TextField txfApellidosSanitarioCambiar;

    @FXML
    private TextField txfDNISanitarioCambiar;

    @FXML
    private TextField txfEmailSanitarioCambiar;

    @FXML
    private TextField txfNombreSanitarioCambiar;

    @FXML
    private TextField txfTelDosSanitarioCambiar;

    @FXML
    private TextField txfTelUnoSanitarioCambiar;

    @FXML
    void cambiarSanitario(ActionEvent event) {

    }

    @FXML
    void cerrarVentana(ActionEvent event) {
        Stage stage = (Stage) btnCancelarSanitarioCambiar.getScene().getWindow();
        stage.close();
    }

}

