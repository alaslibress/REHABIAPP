package com.javafx.Clases;

import com.javafx.Interface.controladorVentanaOpciones;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Clase principal de la aplicacion RehabiAPP
 * Sistema de Gestion de Expedientes para rehabilitacion
 * 
 * MODIFICADO: Aplica CSS externo al iniciar la aplicacion
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Cargar la ventana de inicio de sesion
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/SGEInicioSesion.fxml"));
        Parent root = loader.load();

        // Crear la escena
        Scene scene = new Scene(root);

        // Aplicar la configuracion guardada (tema CSS + tamaño de fuente)
        controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

        // Configurar el Stage
        primaryStage.setTitle("RehabiAPP - Inicio de Sesion");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);

        // Intentar cargar el icono de la aplicacion
        try {
            Image icono = new Image(getClass().getResourceAsStream("/icono.png"));
            primaryStage.getIcons().add(icono);
        } catch (Exception e) {
            System.out.println("No se pudo cargar el icono de la aplicacion");
        }

        // Mostrar la ventana
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
