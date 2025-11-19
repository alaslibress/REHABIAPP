package com.javafx.Interface;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class controladorVentanaPrincipal {

    @FXML
    private BorderPane bdpPrincipal;

    @FXML
    private Button btnAjustes;

    @FXML
    private Button btnBuscar;

    @FXML
    private Button btnPerfil;

    @FXML
    private Button btnPestaniaAyuda;

    @FXML
    private Button btnPestaniaCitas;

    @FXML
    private Button btnPestaniaPacientes;

    @FXML
    private Button btnPestaniaSanitarios;

    @FXML
    private Button btnSalir;

    @FXML
    private ImageView imgAjustes;

    @FXML
    private ImageView imgFotoLogoCompletoStage;

    @FXML
    private ImageView imgLogoStage;

    @FXML
    private ImageView imgSalir;

    @FXML
    private Label lblBuscarRapido;

    @FXML
    private Label lblCargo;

    @FXML
    private Label lblCargoTemporal;

    @FXML
    private Label lblNombreTemporal;

    @FXML
    private Label lblNombreUsuario;

    @FXML
    private Label lblSlogan;

    @FXML
    private TextField txfBusquedaRapida;

    @FXML
    private VBox vboxContenidoPrincipal;

    @FXML
    public void initialize() {
        btnPestaniaSanitarios.setOnAction(e -> cargarPestania("/VentanaSanitarios.fxml"));
        btnPestaniaPacientes.setOnAction(e -> cargarPestania("/VentanaPacientes.fxml"));
        btnPestaniaCitas.setOnAction(e -> cargarPestania("/VentanaCitas.fxml"));
        btnPestaniaAyuda.setOnAction(e -> cargarPestania("/VentanaAyuda.fxml"));
        btnAjustes.setOnAction(e -> abrirVentana("/VentanaOpciones.fxml"));
        btnPerfil.setOnAction(e -> abrirVentana("/VentanaPerfil.fxml"));
        btnSalir.setOnAction(e -> cerrarSesion());
    }

    private void cargarPestania(String archivo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(archivo));
            Parent pestania = loader.load();
            vboxContenidoPrincipal.getChildren().clear();
            vboxContenidoPrincipal.getChildren().add(pestania);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void abrirVentana(String archivo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(archivo));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cerrarSesion() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/SGEInicioSesion.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.show();
            Stage actual = (Stage) btnSalir.getScene().getWindow();
            actual.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void Buscar(ActionEvent event) {

    }

    @FXML
    void Perfil(ActionEvent event) {

    }

}