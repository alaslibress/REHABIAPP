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
 * @author Alejandro Pozo Perez
 */
public class Main extends Application {

    //Titulo de la aplicacion
    private static final String APP_TITLE = "RehabiAPP";

    //Version de la aplicacion
    private static final String APP_VERSION = "1.0";

    /**
     * Metodo principal de entrada
     * @param args Argumentos de linea de comandos
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Metodo start de JavaFX
     * Carga la ventana de inicio de sesion
     * @param primaryStage Stage principal de la aplicacion
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            //Cargar la ventana de inicio de sesion
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/SGEInicioSesion.fxml"));
            Parent root = loader.load();

            //Configurar la escena
            Scene scene = new Scene(root);

            //Configurar el stage
            primaryStage.setTitle(APP_TITLE + " - Inicio de Sesion");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);

            //Intentar cargar icono de la aplicacion
            cargarIconoAplicacion(primaryStage);

            //Aplicar configuracion guardada (tema y tamaño de letra)
            controladorVentanaOpciones.cargarYAplicarConfiguracion(primaryStage);

            //Mostrar ventana
            primaryStage.show();

            System.out.println(APP_TITLE + " v" + APP_VERSION + " iniciado correctamente");

        } catch (Exception e) {
            System.err.println("Error al cargar la ventana de inicio de sesion: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Intenta cargar el icono de la aplicacion
     * @param stage Stage al que asignar el icono
     */
    private void cargarIconoAplicacion(Stage stage) {
        try {
            //Intentar cargar icono desde resources
            Image icono = new Image(getClass().getResourceAsStream("/icono.png"));
            stage.getIcons().add(icono);
        } catch (Exception e) {
            //Si no existe el icono, continuar sin el
            System.out.println("Icono de aplicacion no encontrado, usando icono por defecto");
        }
    }

    /**
     * Metodo stop de JavaFX
     * Se ejecuta al cerrar la aplicacion
     */
    @Override
    public void stop() {
        System.out.println(APP_TITLE + " cerrado correctamente");
    }
}