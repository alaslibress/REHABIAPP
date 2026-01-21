package com.javafx.Clases;

import com.javafx.Clases.AnimacionUtil;
import com.javafx.Interface.controladorVentanaInformativa;
import com.javafx.Interface.controladorVentanaOpciones;
import com.javafx.Interface.controladorVentanaPregunta;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Clase de utilidad para mostrar ventanas informativas y de pregunta
 * 
 * CORREGIDO: Rutas de iconos actualizadas para que todos esten en /resources
 * Los iconos deben estar en la carpeta resources junto con los FXML
 */
public class VentanaUtil {

    /**
     * Enum que define los tipos de mensaje con sus iconos correspondientes
     * IMPORTANTE: Los iconos deben estar en la carpeta resources
     * Nombres de archivo esperados: exito.png, error.png, advertencia.png, informacion.png
     * Si no existen, se usará pregunta.png como fallback
     */
    public enum TipoMensaje {
        EXITO("exito.png", "confirmacion.png", "check.png", "ok.png"),
        ERROR("error.png", "cruz.png", "x.png"),
        ADVERTENCIA("advertencia.png", "warning.png", "alerta.png"),
        INFORMACION("informacion.png", "info.png", "information.png");

        private final String[] iconosAlternativos;

        TipoMensaje(String... iconos) {
            this.iconosAlternativos = iconos;
        }

        /**
         * Obtiene la lista de nombres de iconos alternativos
         * @return Array de nombres de iconos a intentar
         */
        public String[] getIconosAlternativos() {
            return iconosAlternativos;
        }

        /**
         * Obtiene la ruta del icono principal (con /)
         * @return Ruta del icono (ej: /exito.png)
         */
        public String getIcono() {
            return "/" + iconosAlternativos[0];
        }
        
        /**
         * Obtiene la ruta relativa del icono (sin barra inicial)
         * @return Ruta relativa del icono (ej: exito.png)
         */
        public String getIconoRelativo() {
            return iconosAlternativos[0];
        }
    }

    /**
     * Establece el icono de la aplicación en un Stage
     * @param stage Stage al que añadir el icono
     */
    public static void establecerIconoVentana(Stage stage) {
        if (stage == null) return;
        
        // Lista de posibles nombres del icono de RehabiAPP
        String[] nombresIcono = {
            "/RehabiAPPLogoNoLetras.png",
            "/RehabiAPPLogoNoLetras.jpg",
            "/RehabiAPPLogoNoLetras.jpeg",
            "/rehabiapplogonoletras.png",
            "/logo.png",
            "/icono.png"
        };
        
        for (String nombre : nombresIcono) {
            try {
                java.io.InputStream stream = VentanaUtil.class.getResourceAsStream(nombre);
                if (stream != null) {
                    Image icono = new Image(stream);
                    stage.getIcons().add(icono);
                    System.out.println("Icono de ventana cargado: " + nombre);
                    return;
                }
            } catch (Exception e) {
                // Continuar con el siguiente nombre
            }
        }
        System.out.println("No se encontró icono para la ventana");
    }

    /**
     * Establece el icono info.png en un Stage (para ventanas informativas)
     * @param stage Stage al que añadir el icono
     */
    public static void establecerIconoInfo(Stage stage) {
        if (stage == null) return;

        try {
            java.io.InputStream stream = VentanaUtil.class.getResourceAsStream("/info.png");
            if (stream != null) {
                Image icono = new Image(stream);
                stage.getIcons().add(icono);
                System.out.println("Icono info.png cargado para ventana informativa");
            } else {
                System.out.println("No se encontró el icono info.png");
            }
        } catch (Exception e) {
            System.err.println("Error al cargar icono info.png: " + e.getMessage());
        }
    }

    /**
     * Muestra una ventana informativa con un mensaje y tipo especifico
     * @param mensaje Texto a mostrar
     * @param tipo Tipo de mensaje (determina el icono)
     */
    public static void mostrarVentanaInformativa(String mensaje, TipoMensaje tipo) {
        try {
            FXMLLoader loader = new FXMLLoader(
                VentanaUtil.class.getResource("/VentanaInformativa.fxml")
            );
            Parent root = loader.load();

            // Configurar el controlador
            controladorVentanaInformativa controlador = loader.getController();
            controlador.configurarVentana(mensaje, tipo);

            // Crear escena y aplicar CSS
            Scene scene = new Scene(root);
            controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

            // Crear y configurar stage
            Stage stage = new Stage();
            stage.setTitle(obtenerTitulo(tipo));
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);

            // Establecer icono info.png para las ventanas informativas
            establecerIconoInfo(stage);
            
            // Aplicar animación cuando la ventana se muestre
            stage.setOnShown(e -> AnimacionUtil.animarVentanaModal(stage));
            
            // Mostrar y esperar
            stage.showAndWait();

        } catch (Exception e) {
            System.err.println("Error al mostrar ventana informativa: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Muestra una ventana de pregunta/confirmacion
     * @param mensaje Texto de la pregunta
     * @return true si el usuario confirmo, false si cancelo
     */
    public static boolean mostrarVentanaPregunta(String mensaje) {
        try {
            FXMLLoader loader = new FXMLLoader(
                VentanaUtil.class.getResource("/VentanaPregunta.fxml")
            );
            Parent root = loader.load();

            // Configurar el controlador
            controladorVentanaPregunta controlador = loader.getController();
            controlador.configurarPregunta(mensaje);

            // Crear escena y aplicar CSS
            Scene scene = new Scene(root);
            controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

            // Crear y configurar stage
            Stage stage = new Stage();
            stage.setTitle("Confirmar");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            
            // Establecer icono de la aplicación
            establecerIconoVentana(stage);
            
            // Aplicar animación cuando la ventana se muestre
            stage.setOnShown(e -> AnimacionUtil.animarVentanaModal(stage));
            
            // Mostrar y esperar
            stage.showAndWait();

            return controlador.isConfirmado();

        } catch (Exception e) {
            System.err.println("Error al mostrar ventana de pregunta: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Obtiene el titulo de la ventana segun el tipo de mensaje
     */
    private static String obtenerTitulo(TipoMensaje tipo) {
        switch (tipo) {
            case EXITO:
                return "Exito";
            case ERROR:
                return "Error";
            case ADVERTENCIA:
                return "Advertencia";
            case INFORMACION:
            default:
                return "Informacion";
        }
    }
}
