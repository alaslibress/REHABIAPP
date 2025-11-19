package com.javafx.Interface;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;

public class controladorVentanaOpciones {

    @FXML
    private Button btnCancelarOpciones;

    @FXML
    private Button btnGuardarOpciones;

    @FXML
    private ComboBox<?> cbxTamanioLetra;

    @FXML
    private ToggleGroup grupoTema;

    @FXML
    private Label lblTituloOpciones;

    @FXML
    private RadioButton rdbClaro;

    @FXML
    private RadioButton rdbOscuro;

    @FXML
    void cerrarVentanaOpciones(ActionEvent event) {
        Stage stage = (Stage) btnCancelarOpciones.getScene().getWindow();
        stage.close();
    }

    @FXML
    void guardarOpciones(ActionEvent event) {

    }

}
