package com.javafx.util;

import com.javafx.Clases.AnimacionUtil;
import com.javafx.Clases.VentanaUtil;
import com.javafx.Interface.controladorVentanaOpciones;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Helper para apertura de ventanas modales con patron estandarizado
 * Elimina aproximadamente 800 lineas de codigo duplicado en 8+ controladores
 */
public class VentanaHelper {

    // Constructor privado para evitar instanciacion
    private VentanaHelper() {
        throw new UnsupportedOperationException("Clase de utilidad no instanciable");
    }

    /**
     * Abre una ventana modal con configuracion estandar
     * @param fxmlPath Ruta al archivo FXML (ej: "/VentanaAgregarPaciente.fxml")
     * @param titulo Titulo de la ventana
     * @return FXMLLoader para acceder al controlador si es necesario
     * @throws IOException Si no se puede cargar el archivo FXML
     */
    public static FXMLLoader abrirVentanaModal(String fxmlPath, String titulo) throws IOException {
        FXMLLoader loader = new FXMLLoader(VentanaHelper.class.getResource(fxmlPath));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

        Stage stage = new Stage();
        stage.setTitle(titulo);
        stage.setScene(scene);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);

        VentanaUtil.establecerIconoVentana(stage);
        stage.setOnShown(e -> AnimacionUtil.animarVentanaModal(stage));

        stage.showAndWait();

        return loader;
    }

    /**
     * Version simplificada que no requiere acceso al controlador
     * Captura y maneja IOException internamente
     * @param fxmlPath Ruta al archivo FXML
     * @param titulo Titulo de la ventana
     */
    public static void abrirVentanaModalSimple(String fxmlPath, String titulo) {
        try {
            abrirVentanaModal(fxmlPath, titulo);
        } catch (IOException e) {
            System.err.println("Error al abrir ventana " + titulo + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Abre una ventana modal y retorna el controlador
     * Util cuando se necesita acceder al controlador despues de cerrar la ventana
     * @param fxmlPath Ruta al archivo FXML
     * @param titulo Titulo de la ventana
     * @param <T> Tipo del controlador
     * @return Controlador de la ventana o null si ocurre error
     */
    public static <T> T abrirVentanaModalConControlador(String fxmlPath, String titulo) {
        try {
            FXMLLoader loader = abrirVentanaModal(fxmlPath, titulo);
            return loader.getController();
        } catch (IOException e) {
            System.err.println("Error al abrir ventana " + titulo + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
