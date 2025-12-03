package com.javafx.Clases;

import com.javafx.Interface.controladorVentanaInformativa;
import com.javafx.Interface.controladorVentanaPregunta;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Clase de utilidades para gestionar ventanas emergentes del sistema
 * Proporciona metodos estaticos para mostrar mensajes informativos y de confirmacion
 */
public class VentanaUtil {

    /**
     * Enumerado con los tipos de mensaje disponibles
     * Cada tipo tiene asociado un titulo y un icono
     */
    public enum TipoMensaje {
        EXITO("Operacion exitosa", "/correcto.png"),
        ERROR("Error", "/error.png"),
        ADVERTENCIA("Advertencia", "/advertencia.png"),
        INFORMACION("Informacion", "/correcto.png");

        private final String titulo;
        private final String icono;

        TipoMensaje(String titulo, String icono) {
            this.titulo = titulo;
            this.icono = icono;
        }

        public String getTitulo() {
            return titulo;
        }

        public String getIcono() {
            return icono;
        }
    }

    /**
     * Muestra una ventana informativa con un mensaje y tipo especificado
     * @param mensaje Texto a mostrar en la ventana
     * @param tipo Tipo de mensaje (EXITO, ERROR, ADVERTENCIA, INFORMACION)
     */
    public static void mostrarVentanaInformativa(String mensaje, TipoMensaje tipo) {
        try {
            FXMLLoader loader = new FXMLLoader(VentanaUtil.class.getResource("/VentanaInformativa.fxml"));
            Parent root = loader.load();

            //Obtener el controlador y configurar el mensaje
            controladorVentanaInformativa controlador = loader.getController();
            controlador.configurarVentana(mensaje, tipo);

            //Crear y mostrar la ventana modal
            Stage stage = new Stage();
            stage.setTitle(tipo.getTitulo());
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

        } catch (Exception e) {
            System.err.println("Error al mostrar ventana informativa: " + e.getMessage());
        }
    }

    /**
     * Muestra una ventana de confirmacion con una pregunta
     * @param mensaje Pregunta a mostrar al usuario
     * @return true si el usuario confirma, false si cancela
     */
    public static boolean mostrarVentanaPregunta(String mensaje) {
        try {
            FXMLLoader loader = new FXMLLoader(VentanaUtil.class.getResource("/VentanaPregunta.fxml"));
            Parent root = loader.load();

            //Obtener el controlador y configurar la pregunta
            controladorVentanaPregunta controlador = loader.getController();
            controlador.configurarPregunta(mensaje);

            //Crear y mostrar la ventana modal
            Stage stage = new Stage();
            stage.setTitle("Confirmar accion");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

            //Devolver la respuesta del usuario
            return controlador.isConfirmado();

        } catch (Exception e) {
            System.err.println("Error al mostrar ventana de pregunta: " + e.getMessage());
            return false;
        }
    }
}