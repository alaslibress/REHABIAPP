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
 * MODIFICADO: Aplica CSS externo y animaciones al iniciar la aplicacion
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
        primaryStage.setTitle("RehabiAPP - Inicio de Sesión");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);

        // Intentar cargar el icono de la aplicación (RehabiAPPLogoNoLetras)
        try {
            // Probar diferentes nombres posibles del icono
            java.io.InputStream iconStream = null;
            String[] nombresIcono = {
                "/RehabiAPPLogoNoLetras.png",
                "/RehabiAPPLogoNoLetras.jpg",
                "/rehabiapplogonoletras.png",
                "/icono.png"
            };
            
            for (String nombre : nombresIcono) {
                iconStream = getClass().getResourceAsStream(nombre);
                if (iconStream != null) {
                    Image icono = new Image(iconStream);
                    primaryStage.getIcons().add(icono);
                    System.out.println("Icono de aplicación cargado: " + nombre);
                    break;
                }
            }
            
            if (iconStream == null) {
                System.out.println("No se encontró el icono de la aplicación");
            }
        } catch (Exception e) {
            System.out.println("No se pudo cargar el icono de la aplicación: " + e.getMessage());
        }

        // Mostrar la ventana con animación (scale + fade)
        primaryStage.show();
        AnimacionUtil.animarVentana(primaryStage, 500);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
