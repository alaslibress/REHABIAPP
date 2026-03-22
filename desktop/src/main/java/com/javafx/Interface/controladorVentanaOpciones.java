package com.javafx.Interface;

import com.javafx.Clases.AnimacionUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Controlador para la ventana de opciones
 * Permite cambiar el tamaño de letra y el tema (claro/oscuro)
 * La configuracion se guarda en un archivo config.properties
 * 
 * MODIFICADO: Ahora carga archivos CSS externos en lugar de estilos inline
 */
public class controladorVentanaOpciones {

    @FXML
    private Button btnAplicar;

    @FXML
    private Button btnCancelar;

    @FXML
    private ComboBox<String> cmbTamanioLetra;

    @FXML
    private RadioButton rbTemaClaro;

    @FXML
    private RadioButton rbTemaOscuro;

    @FXML
    private ToggleGroup grupoTema;

    // Constantes para la configuracion
    private static final String CONFIG_FILE = "config.properties";
    private static final String KEY_TAMANIO_LETRA = "tamanio.letra";
    private static final String KEY_TEMA = "tema";

    // Nombres de los archivos CSS
    private static final String CSS_TEMA_CLARO = "/tema_claro.css";
    private static final String CSS_TEMA_OSCURO = "/tema_oscuro.css";

    // Opciones de tamaño de letra
    private static final ObservableList<String> TAMANIOS_LETRA = FXCollections.observableArrayList(
            "Pequeño (12px)",
            "Mediano (14px)",
            "Grande (16px)",
            "Muy grande (18px)"
    );

    /**
     * Metodo initialize se ejecuta automaticamente al cargar el FXML
     */
    @FXML
    public void initialize() {
        // Configurar opciones del ComboBox
        cmbTamanioLetra.setItems(TAMANIOS_LETRA);

        // Cargar configuracion guardada
        cargarConfiguracion();
    }

    /**
     * Carga la configuracion desde el archivo properties
     */
    private void cargarConfiguracion() {
        Properties props = new Properties();
        File configFile = new File(CONFIG_FILE);

        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);

                // Cargar tamaño de letra
                String tamanio = props.getProperty(KEY_TAMANIO_LETRA, "Mediano (14px)");
                cmbTamanioLetra.setValue(tamanio);

                // Cargar tema
                String tema = props.getProperty(KEY_TEMA, "claro");
                if ("oscuro".equals(tema)) {
                    rbTemaOscuro.setSelected(true);
                } else {
                    rbTemaClaro.setSelected(true);
                }

            } catch (IOException e) {
                System.err.println("Error al cargar configuracion: " + e.getMessage());
                establecerValoresPorDefecto();
            }
        } else {
            establecerValoresPorDefecto();
        }
    }

    /**
     * Establece los valores por defecto
     */
    private void establecerValoresPorDefecto() {
        cmbTamanioLetra.setValue("Mediano (14px)");
        rbTemaClaro.setSelected(true);
    }

    /**
     * Guarda la configuracion y aplica los cambios
     */
    @FXML
    void guardarOpciones(ActionEvent event) {
        Properties props = new Properties();

        // Guardar tamaño de letra
        String tamanio = cmbTamanioLetra.getValue();
        props.setProperty(KEY_TAMANIO_LETRA, tamanio);

        // Guardar tema
        String tema = rbTemaOscuro.isSelected() ? "oscuro" : "claro";
        props.setProperty(KEY_TEMA, tema);

        // Escribir archivo
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            props.store(fos, "Configuracion de RehabiAPP");
            System.out.println("Configuracion guardada correctamente");
        } catch (IOException e) {
            System.err.println("Error al guardar configuracion: " + e.getMessage());
        }

        // Aplicar la configuracion a todas las ventanas abiertas
        aplicarConfiguracionGlobal(tamanio, tema);

        // Cerrar ventana de opciones
        cerrarVentana(event);
    }

    /**
     * Aplica la configuracion a todas las ventanas abiertas
     * @param tamanio Tamaño de letra seleccionado
     * @param tema Tema seleccionado (claro/oscuro)
     */
    private void aplicarConfiguracionGlobal(String tamanio, String tema) {
        // Obtener el tamaño en pixeles
        int tamanioPx = obtenerTamanioPx(tamanio);

        // Obtener la ruta del CSS correspondiente
        String cssPath = "oscuro".equals(tema) ? CSS_TEMA_OSCURO : CSS_TEMA_CLARO;

        // Aplicar a todas las ventanas abiertas
        for (Window window : Window.getWindows()) {
            if (window instanceof Stage) {
                Stage stage = (Stage) window;
                Scene scene = stage.getScene();
                if (scene != null) {
                    aplicarConfiguracionAEscena(scene, cssPath, tamanioPx);
                }
            }
        }
    }

    /**
     * Aplica la configuracion CSS y tamaño de fuente a una escena
     * @param scene Escena a configurar
     * @param cssPath Ruta del archivo CSS
     * @param tamanioPx Tamaño de fuente en pixeles
     */
    public static void aplicarConfiguracionAEscena(Scene scene, String cssPath, int tamanioPx) {
        if (scene == null) return;

        // Limpiar hojas de estilo anteriores (solo las de tema)
        scene.getStylesheets().removeIf(s -> 
            s.contains("tema_claro.css") || s.contains("tema_oscuro.css")
        );

        // Agregar la nueva hoja de estilo
        try {
            String cssUrl = controladorVentanaOpciones.class.getResource(cssPath).toExternalForm();
            scene.getStylesheets().add(cssUrl);
            System.out.println("CSS aplicado: " + cssPath);
        } catch (Exception e) {
            System.err.println("Error al cargar CSS " + cssPath + ": " + e.getMessage());
        }

        // Aplicar tamaño de fuente al root
        if (scene.getRoot() != null) {
            scene.getRoot().setStyle("-fx-font-size: " + tamanioPx + "px;");
        }
    }

    /**
     * Convierte el nombre del tamaño a pixeles
     * @param tamanio Nombre del tamaño
     * @return Tamaño en pixeles
     */
    private static int obtenerTamanioPx(String tamanio) {
        if (tamanio == null) return 14;

        if (tamanio.contains("12")) return 12;
        if (tamanio.contains("14")) return 14;
        if (tamanio.contains("16")) return 16;
        if (tamanio.contains("18")) return 18;

        return 14; // Por defecto
    }

    /**
     * Cierra la ventana sin guardar
     */
    @FXML
    void cerrarVentana(ActionEvent event) {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }

    // ============================================================
    // METODOS ESTATICOS PARA APLICAR CONFIGURACION DESDE OTRAS CLASES
    // ============================================================

    /**
     * Carga y aplica la configuracion guardada a un Stage
     * Debe llamarse al abrir cada nueva ventana
     * @param stage Stage al que aplicar la configuracion
     */
    public static void cargarYAplicarConfiguracion(Stage stage) {
        if (stage == null || stage.getScene() == null) return;

        Properties props = new Properties();
        File configFile = new File(CONFIG_FILE);

        String tema = "claro";
        String tamanio = "Mediano (14px)";

        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
                tema = props.getProperty(KEY_TEMA, "claro");
                tamanio = props.getProperty(KEY_TAMANIO_LETRA, "Mediano (14px)");
            } catch (IOException e) {
                System.err.println("Error al cargar configuracion: " + e.getMessage());
            }
        }

        // Aplicar configuracion
        String cssPath = "oscuro".equals(tema) ? CSS_TEMA_OSCURO : CSS_TEMA_CLARO;
        int tamanioPx = obtenerTamanioPx(tamanio);

        aplicarConfiguracionAEscena(stage.getScene(), cssPath, tamanioPx);
    }

    /**
     * Aplica la configuracion guardada a una Scene
     * Util cuando se carga contenido dinamico
     * @param scene Scene a configurar
     */
    public static void aplicarConfiguracionAScene(Scene scene) {
        if (scene == null) return;

        Properties props = new Properties();
        File configFile = new File(CONFIG_FILE);

        String tema = "claro";
        String tamanio = "Mediano (14px)";

        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
                tema = props.getProperty(KEY_TEMA, "claro");
                tamanio = props.getProperty(KEY_TAMANIO_LETRA, "Mediano (14px)");
            } catch (IOException e) {
                System.err.println("Error al cargar configuracion: " + e.getMessage());
            }
        }

        String cssPath = "oscuro".equals(tema) ? CSS_TEMA_OSCURO : CSS_TEMA_CLARO;
        int tamanioPx = obtenerTamanioPx(tamanio);

        aplicarConfiguracionAEscena(scene, cssPath, tamanioPx);
    }

    /**
     * Obtiene el tema actual configurado
     * @return "claro" u "oscuro"
     */
    public static String obtenerTemaActual() {
        Properties props = new Properties();
        File configFile = new File(CONFIG_FILE);

        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
                return props.getProperty(KEY_TEMA, "claro");
            } catch (IOException e) {
                System.err.println("Error al leer tema: " + e.getMessage());
            }
        }
        return "claro";
    }

    /**
     * Obtiene el tamaño de letra actual configurado
     * @return Tamaño de letra como String
     */
    public static String obtenerTamanioLetraActual() {
        Properties props = new Properties();
        File configFile = new File(CONFIG_FILE);

        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
                return props.getProperty(KEY_TAMANIO_LETRA, "Mediano (14px)");
            } catch (IOException e) {
                System.err.println("Error al leer tamanio letra: " + e.getMessage());
            }
        }
        return "Mediano (14px)";
    }

    /**
     * Obtiene la ruta del CSS del tema actual
     * @return Ruta del archivo CSS
     */
    public static String obtenerCssActual() {
        String tema = obtenerTemaActual();
        return "oscuro".equals(tema) ? CSS_TEMA_OSCURO : CSS_TEMA_CLARO;
    }

    /**
     * Obtiene el tamaño de fuente actual en pixeles
     * @return Tamaño en pixeles
     */
    public static int obtenerTamanioPxActual() {
        return obtenerTamanioPx(obtenerTamanioLetraActual());
    }

    /**
     * Aplica configuracion CSS y animacion de aparicion a un Stage
     * Debe llamarse DESPUES de stage.show()
     * @param stage Stage a animar
     */
    public static void aplicarConfiguracionYAnimacion(Stage stage) {
        if (stage == null || stage.getScene() == null) return;

        // Aplicar CSS y tamaño de fuente
        aplicarConfiguracionAScene(stage.getScene());

        // Aplicar animacion de aparecer (scale + fade)
        AnimacionUtil.animarVentana(stage, 300);
    }
}
