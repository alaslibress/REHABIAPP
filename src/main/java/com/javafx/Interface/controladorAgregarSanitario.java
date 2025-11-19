package com.javafx.Interface;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class controladorAgregarSanitario {

    @FXML
    private Button btnCancelarSanitarioNuevo;

    @FXML
    private Button btnCrearSanitarioNuevo;

    @FXML
    private CheckBox cbxEspecialista;

    @FXML
    private Label lblTituloVentana;

    @FXML
    private PasswordField pssContraseniaNuevoSanitario;

    @FXML
    private TextField txfApellidosSanitario;

    @FXML
    private TextField txfDNISanitario;

    @FXML
    private TextField txfEmailSanitario;

    @FXML
    private TextField txfNombreSanitario;

    @FXML
    private TextField txfTelDosSanitario;

    @FXML
    private TextField txfTelUnoSanitario;

    @FXML
    void cerrarVentana(ActionEvent event) {
        Stage stage = (Stage) btnCancelarSanitarioNuevo.getScene().getWindow();
        stage.close();
    }

    @FXML
    void crearNuevoSanitario(ActionEvent event) {

    }

}
