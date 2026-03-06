package com.javafx.Clases;

import com.javafx.Interface.controladorVentanaOpciones;
import com.javafx.service.CifradoService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;
import java.util.Properties;

/**
 * Clase principal de la aplicacion RehabiAPP
 * Sistema de Gestion de Expedientes para rehabilitacion
 * 
 * MODIFICADO: Aplica CSS externo y animaciones al iniciar la aplicacion
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        //Inicializar servicio de cifrado AES-256 para campos clinicos
        inicializarCifrado();

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

    /**
     * Carga la clave AES-256 desde cifrado.properties e inicializa el servicio de cifrado.
     * Si el archivo no existe o la clave no esta configurada, la app funciona sin cifrado (fallback).
     */
    private void inicializarCifrado() {
        try {
            InputStream input = getClass().getClassLoader().getResourceAsStream("config/cifrado.properties");
            if (input == null) {
                System.out.println("cifrado.properties no encontrado: campos clinicos sin cifrar (modo fallback)");
                return;
            }

            Properties props = new Properties();
            props.load(input);
            input.close();

            String clave = props.getProperty("aes.clave");
            CifradoService.inicializar(clave);

        } catch (Exception e) {
            System.err.println("Error al inicializar cifrado: " + e.getMessage());
            //La app continua sin cifrado (fallback seguro)
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
