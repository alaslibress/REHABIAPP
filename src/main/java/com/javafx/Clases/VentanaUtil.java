package com.javafx.Clases;

import com.javafx.Interface.controladorVentanaInformativa;
import com.javafx.Interface.controladorVentanaOpciones;
import com.javafx.Interface.controladorVentanaPregunta;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Clase de utilidad para mostrar ventanas emergentes
 * Ventanas informativas, de error, advertencia y preguntas
 */
public class VentanaUtil {

    /**
     * Enumerado con los tipos de mensaje disponibles
     */
    public enum TipoMensaje {
        INFORMACION("/iconos/info.png"),
        ADVERTENCIA("/iconos/advertencia.png"),
        ERROR("/iconos/error.png"),
        EXITO("/iconos/exito.png");

        private final String icono;

        TipoMensaje(String icono) {
            this.icono = icono;
        }

        public String getIcono() {
            return icono;
        }
    }

    /**
     * Muestra una ventana informativa con un mensaje y tipo especificado
     * @param mensaje Texto a mostrar
     * @param tipo Tipo de mensaje (determina el icono)
     */
    public static void mostrarVentanaInformativa(String mensaje, TipoMensaje tipo) {
        try {
            FXMLLoader loader = new FXMLLoader(VentanaUtil.class.getResource("/VentanaInformativa.fxml"));
            Parent root = loader.load();

            //Configurar el controlador con el mensaje y tipo
            controladorVentanaInformativa controlador = loader.getController();
            controlador.configurarVentana(mensaje, tipo);

            //Crear escena y aplicar CSS
            Scene scene = new Scene(root);
            controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

            //Crear y mostrar la ventana modal
            Stage stage = new Stage();
            stage.setTitle(obtenerTituloVentana(tipo));
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

        } catch (Exception e) {
            System.err.println("Error al mostrar ventana informativa: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Muestra una ventana de pregunta con opciones Si/No
     * @param mensaje Pregunta a mostrar
     * @return true si el usuario confirmo, false si cancelo
     */
    public static boolean mostrarVentanaPregunta(String mensaje) {
        try {
            FXMLLoader loader = new FXMLLoader(VentanaUtil.class.getResource("/VentanaPregunta.fxml"));
            Parent root = loader.load();

            //Configurar el controlador con la pregunta
            controladorVentanaPregunta controlador = loader.getController();
            controlador.configurarPregunta(mensaje);

            //Crear escena y aplicar CSS
            Scene scene = new Scene(root);
            controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

            //Crear y mostrar la ventana modal
            Stage stage = new Stage();
            stage.setTitle("Confirmar");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

            //Devolver la respuesta del usuario
            return controlador.isConfirmado();

        } catch (Exception e) {
            System.err.println("Error al mostrar ventana de pregunta: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Obtiene el titulo de la ventana segun el tipo de mensaje
     * @param tipo Tipo de mensaje
     * @return Titulo correspondiente
     */
    private static String obtenerTituloVentana(TipoMensaje tipo) {
        switch (tipo) {
            case INFORMACION:
                return "Informacion";
            case ADVERTENCIA:
                return "Advertencia";
            case ERROR:
                return "Error";
            case EXITO:
                return "Exito";
            default:
                return "Mensaje";
        }
    }
}