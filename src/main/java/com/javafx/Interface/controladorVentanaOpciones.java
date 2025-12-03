package com.javafx.Interface;

import com.javafx.Clases.VentanaUtil;
import com.javafx.Clases.VentanaUtil.TipoMensaje;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Controlador para la ventana de opciones/configuracion
 * Permite cambiar el tamaño de letra y el tema de la aplicacion
 */
public class controladorVentanaOpciones {

    @FXML
    private Button btnCancelarOpciones;

    @FXML
    private Button btnGuardarOpciones;

    @FXML
    private ComboBox<String> cbxTamanioLetra;

    @FXML
    private ToggleGroup grupoTema;

    @FXML
    private Label lblTituloOpciones;

    @FXML
    private RadioButton rdbClaro;

    @FXML
    private RadioButton rdbOscuro;

    //Ruta del archivo de configuracion
    private static final String CONFIG_FILE = "config.properties";

    //Claves de configuracion
    private static final String KEY_TAMANIO_LETRA = "tamanio.letra";
    private static final String KEY_TEMA = "tema";

    //Valores por defecto
    private static final String TEMA_CLARO = "claro";
    private static final String TEMA_OSCURO = "oscuro";
    private static final String TAMANIO_MEDIANO = "Mediano (14px)";

    /**
     * Metodo initialize se ejecuta automaticamente al cargar el FXML
     */
    @FXML
    public void initialize() {
        //Inicializar ComboBox de tamaño de letra
        inicializarComboTamanio();

        //Cargar configuracion guardada
        cargarConfiguracion();
    }

    /**
     * Inicializa el ComboBox con las opciones de tamaño de letra
     */
    private void inicializarComboTamanio() {
        ObservableList<String> tamanios = FXCollections.observableArrayList(
                "Pequeño (12px)",
                "Mediano (14px)",
                "Grande (16px)",
                "Muy grande (18px)"
        );

        cbxTamanioLetra.setItems(tamanios);
        cbxTamanioLetra.setValue(TAMANIO_MEDIANO);
    }

    /**
     * Carga la configuracion desde el archivo de propiedades
     */
    private void cargarConfiguracion() {
        Properties props = new Properties();
        File configFile = new File(CONFIG_FILE);

        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);

                //Cargar tamaño de letra
                String tamanio = props.getProperty(KEY_TAMANIO_LETRA, TAMANIO_MEDIANO);
                cbxTamanioLetra.setValue(tamanio);

                //Cargar tema
                String tema = props.getProperty(KEY_TEMA, TEMA_CLARO);
                if (TEMA_OSCURO.equals(tema)) {
                    rdbOscuro.setSelected(true);
                } else {
                    rdbClaro.setSelected(true);
                }

                System.out.println("Configuracion cargada correctamente");

            } catch (IOException e) {
                System.err.println("Error al cargar configuracion: " + e.getMessage());
                //Usar valores por defecto si hay error
            }
        }
    }

    /**
     * Guarda las opciones seleccionadas
     */
    @FXML
    void guardarOpciones(ActionEvent event) {
        Properties props = new Properties();

        //Guardar tamaño de letra
        String tamanio = cbxTamanioLetra.getValue();
        props.setProperty(KEY_TAMANIO_LETRA, tamanio);

        //Guardar tema
        String tema = rdbOscuro.isSelected() ? TEMA_OSCURO : TEMA_CLARO;
        props.setProperty(KEY_TEMA, tema);

        //Guardar en archivo
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            props.store(fos, "Configuracion de RehabiAPP");

            //Aplicar cambios inmediatamente
            aplicarConfiguracion(tamanio, tema);

            VentanaUtil.mostrarVentanaInformativa(
                    "Configuracion guardada correctamente.\n" +
                            "Los cambios se han aplicado.",
                    TipoMensaje.EXITO
            );

            //Cerrar ventana
            cerrarVentanaOpciones(null);

        } catch (IOException e) {
            System.err.println("Error al guardar configuracion: " + e.getMessage());
            VentanaUtil.mostrarVentanaInformativa(
                    "Error al guardar la configuracion.",
                    TipoMensaje.ERROR
            );
        }
    }

    /**
     * Aplica la configuracion a la ventana principal
     * @param tamanio Tamaño de letra seleccionado
     * @param tema Tema seleccionado (claro/oscuro)
     */
    private void aplicarConfiguracion(String tamanio, String tema) {
        //Obtener la ventana principal (parent de esta ventana)
        Stage stageActual = (Stage) btnGuardarOpciones.getScene().getWindow();
        Stage stagePrincipal = (Stage) stageActual.getOwner();

        if (stagePrincipal != null && stagePrincipal.getScene() != null) {
            //Aplicar tamaño de letra
            int fontSize = obtenerTamanioPixeles(tamanio);
            String estiloFuente = "-fx-font-size: " + fontSize + "px;";

            //Aplicar tema
            String estiloTema = obtenerEstiloTema(tema);

            //Combinar estilos
            String estiloCompleto = estiloFuente + estiloTema;

            //Aplicar a la escena principal
            stagePrincipal.getScene().getRoot().setStyle(estiloCompleto);

            System.out.println("Configuracion aplicada: " + estiloCompleto);
        }
    }

    /**
     * Convierte el texto del ComboBox a pixeles
     * @param tamanio Texto del tamaño seleccionado
     * @return Tamaño en pixeles
     */
    private int obtenerTamanioPixeles(String tamanio) {
        if (tamanio == null) {
            return 14;
        }

        if (tamanio.contains("12")) {
            return 12;
        } else if (tamanio.contains("14")) {
            return 14;
        } else if (tamanio.contains("16")) {
            return 16;
        } else if (tamanio.contains("18")) {
            return 18;
        }

        return 14; //Por defecto
    }

    /**
     * Obtiene el estilo CSS segun el tema seleccionado
     * @param tema Tema seleccionado
     * @return String con el estilo CSS
     */
    private String obtenerEstiloTema(String tema) {
        if (TEMA_OSCURO.equals(tema)) {
            return "-fx-base: #3c3c3c; " +
                    "-fx-background: #2b2b2b; " +
                    "-fx-control-inner-background: #3c3c3c; " +
                    "-fx-text-fill: #ffffff;";
        } else {
            //Tema claro (por defecto)
            return "-fx-base: #ececec; " +
                    "-fx-background: #ffffff; " +
                    "-fx-control-inner-background: #ffffff;";
        }
    }

    /**
     * Cierra la ventana sin guardar cambios
     */
    @FXML
    void cerrarVentanaOpciones(ActionEvent event) {
        Stage stage = (Stage) btnCancelarOpciones.getScene().getWindow();
        stage.close();
    }

    // ==================== METODOS ESTATICOS PARA CARGAR CONFIG ====================

    /**
     * Carga y aplica la configuracion guardada a una escena
     * Llamar este metodo al iniciar la aplicacion
     * @param stage Stage principal de la aplicacion
     */
    public static void cargarYAplicarConfiguracion(Stage stage) {
        Properties props = new Properties();
        File configFile = new File(CONFIG_FILE);

        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);

                String tamanio = props.getProperty(KEY_TAMANIO_LETRA, TAMANIO_MEDIANO);
                String tema = props.getProperty(KEY_TEMA, TEMA_CLARO);

                //Calcular tamaño en pixeles
                int fontSize = 14;
                if (tamanio.contains("12")) fontSize = 12;
                else if (tamanio.contains("14")) fontSize = 14;
                else if (tamanio.contains("16")) fontSize = 16;
                else if (tamanio.contains("18")) fontSize = 18;

                //Construir estilo
                String estiloFuente = "-fx-font-size: " + fontSize + "px;";
                String estiloTema;

                if (TEMA_OSCURO.equals(tema)) {
                    estiloTema = "-fx-base: #3c3c3c; " +
                            "-fx-background: #2b2b2b; " +
                            "-fx-control-inner-background: #3c3c3c; " +
                            "-fx-text-fill: #ffffff;";
                } else {
                    estiloTema = "-fx-base: #ececec; " +
                            "-fx-background: #ffffff; " +
                            "-fx-control-inner-background: #ffffff;";
                }

                //Aplicar estilo
                if (stage.getScene() != null && stage.getScene().getRoot() != null) {
                    stage.getScene().getRoot().setStyle(estiloFuente + estiloTema);
                }

                System.out.println("Configuracion inicial aplicada");

            } catch (IOException e) {
                System.err.println("Error al cargar configuracion inicial: " + e.getMessage());
            }
        }
    }

    /**
     * Obtiene el tema actual guardado
     * @return "claro" u "oscuro"
     */
    public static String obtenerTemaActual() {
        Properties props = new Properties();
        File configFile = new File(CONFIG_FILE);

        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
                return props.getProperty(KEY_TEMA, TEMA_CLARO);
            } catch (IOException e) {
                System.err.println("Error al leer tema: " + e.getMessage());
            }
        }

        return TEMA_CLARO;
    }

    /**
     * Obtiene el tamaño de letra actual guardado
     * @return Tamaño en pixeles
     */
    public static int obtenerTamanioLetraActual() {
        Properties props = new Properties();
        File configFile = new File(CONFIG_FILE);

        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
                String tamanio = props.getProperty(KEY_TAMANIO_LETRA, TAMANIO_MEDIANO);

                if (tamanio.contains("12")) return 12;
                else if (tamanio.contains("14")) return 14;
                else if (tamanio.contains("16")) return 16;
                else if (tamanio.contains("18")) return 18;

            } catch (IOException e) {
                System.err.println("Error al leer tamaño letra: " + e.getMessage());
            }
        }

        return 14;
    }
}