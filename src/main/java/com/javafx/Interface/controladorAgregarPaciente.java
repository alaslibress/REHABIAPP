package com.javafx.Interface;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class controladorAgregarPaciente {

    @FXML
    private Button btnAgregarFoto;

    @FXML
    private Button btnCancelar;

    @FXML
    private Button btnGuardar;

    @FXML
    private Button btnImportarInforme;

    @FXML
    private ToggleGroup grupoProtesis;

    @FXML
    private ImageView imgFotoPaciente;

    @FXML
    private Label lblTituloVentana;

    @FXML
    private RadioButton radioProtesisNo;

    @FXML
    private RadioButton radioProtesisSi;

    @FXML
    private Spinner<?> spinnerEdad;

    @FXML
    private TextField txtApellidos;

    @FXML
    private TextArea txtAreaEstado;

    @FXML
    private TextArea txtAreaTratamiento;

    @FXML
    private TextField txtDNI;

    @FXML
    private TextField txtDireccion;

    @FXML
    private TextField txtDiscapacidad;

    @FXML
    private TextField txtEmail;

    @FXML
    private TextField txtNSS;

    @FXML
    private TextField txtNombre;

    @FXML
    private TextField txtTelefono1;

    @FXML
    private TextField txtTelefono2;

    @FXML
    void agregarFotoPaciente(ActionEvent event) {

    }

    @FXML
    void cerrarVentana(ActionEvent event) {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }

    @FXML
    void guardarPaciente(ActionEvent event) {

    }

    @FXML
    void importarInformePDF(ActionEvent event) {

    }

}
