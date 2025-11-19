package com.javafx.Clases;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 *
 * @author Alejandro Pozo Pérez
 */
public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            //Cargar la ventana de inicio de sesion
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/SGEInicioSesion.fxml"));
            Parent root = loader.load();

            //Configurar la escena
            Scene scene = new Scene(root);

            //Configurar el stage
            primaryStage.setTitle("RehabiAPP - Inicio de Sesión");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false); //No redimensionable en login
            primaryStage.show();

        } catch (Exception e) {
            System.err.println("Error al cargar la ventana de inicio de sesión");
            e.printStackTrace();
        }
    }
}