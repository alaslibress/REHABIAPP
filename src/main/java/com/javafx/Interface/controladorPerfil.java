package com.javafx.Interface;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class controladorPerfil {

    @FXML
    private Button btnCamgiarDatos;

    @FXML
    private Label lblApellidosPerfil;

    @FXML
    private Label lblCargoPerfil;

    @FXML
    private Label lblDNIPerfil;

    @FXML
    private Label lblEmailPerfil;

    @FXML
    private Label lblNombrePerfil;

    @FXML
    private Label lblPacientesAsignPerfil;

    @FXML
    private Label lblTelefonoDosPerfil;

    @FXML
    private Label lblTelefonoUnoPerfil;

    @FXML
    private Label lblTituloVentana;

    @FXML
    void cambiarDatos(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaPerfilEditar.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
