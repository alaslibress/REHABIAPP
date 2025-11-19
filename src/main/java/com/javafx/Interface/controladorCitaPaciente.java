package com.javafx.Interface;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class controladorCitaPaciente {

    @FXML
    private Button btnAceptar;

    @FXML
    private Button btnBorrar;

    @FXML
    private Button btnCambiar;

    @FXML
    private Button btnDescargarInforme;

    @FXML
    private ImageView imgFotoPaciente;

    @FXML
    private Label lblApellidosValor;

    @FXML
    private Label lblDNIValor;

    @FXML
    private Label lblEdadValor;

    @FXML
    private Label lblFechaValor;

    @FXML
    private Label lblHoraValor;

    @FXML
    private Label lblNSSValor;

    @FXML
    private Label lblNombreValor;

    @FXML
    private Label lblTituloVentana;

    @FXML
    void borrarCita(ActionEvent event) {

    }

    @FXML
    void cambiarCita(ActionEvent event) {

    }

    @FXML
    void cerrarVentana(ActionEvent event) {
        Stage stage = (Stage) btnAceptar.getScene().getWindow();
        stage.close();
    }

    @FXML
    void descargarInformePDF(ActionEvent event) {

    }

}

