package com.javafx.Clases;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.util.JRLoader;

import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.io.InputStream;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Servicio para la generación de informes PDF usando JasperReports
 * Compila, rellena y exporta informes a partir de archivos .jrxml
 */
public class InformeService {

    // Directorio base para guardar los informes generados
    private static final String DIRECTORIO_INFORMES = "informes";

    /**
     * Genera un informe PDF a partir de un archivo .jrxml
     *
     * @param nombreArchivoJrxml Nombre del archivo .jrxml (ej: "InformePacienteRehabiapp.jrxml")
     * @param parametros         Map con los parámetros a pasar al informe
     * @param nombreSalida       Nombre base del archivo de salida (sin extensión)
     * @return true si el informe se generó correctamente, false en caso contrario
     */
    public static boolean generarInformePDF(String nombreArchivoJrxml, Map<String, Object> parametros, String nombreSalida) {
        Connection conexion = null;

        try {
            // Obtener la conexión a la base de datos
            conexion = ConexionBD.getConexion();

            if (conexion == null) {
                System.err.println("Error: No se pudo obtener la conexión a la base de datos");
                return false;
            }

            // Compilar el informe
            JasperReport jasperReport = compilarInforme(nombreArchivoJrxml);

            if (jasperReport == null) {
                System.err.println("Error: No se pudo compilar el informe");
                return false;
            }

            // Rellenar el informe con datos
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parametros, conexion);

            // Crear el directorio de informes si no existe
            File directorioInformes = new File(DIRECTORIO_INFORMES);
            if (!directorioInformes.exists()) {
                boolean creado = directorioInformes.mkdirs();
                if (!creado) {
                    System.err.println("Error: No se pudo crear el directorio de informes");
                    return false;
                }
            }

            // Generar nombre de archivo con timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String nombreArchivo = nombreSalida + "_" + timestamp + ".pdf";
            String rutaCompletaPDF = DIRECTORIO_INFORMES + File.separator + nombreArchivo;

            // Exportar a PDF
            JasperExportManager.exportReportToPdfFile(jasperPrint, rutaCompletaPDF);

            System.out.println("Informe PDF generado correctamente: " + rutaCompletaPDF);

            // TAMBIÉN exportar a HTML (si falla, no afecta al PDF)
            try {
                String rutaCompletaHTML = rutaCompletaPDF.replace(".pdf", ".html");
                JasperExportManager.exportReportToHtmlFile(jasperPrint, rutaCompletaHTML);
                System.out.println("Informe HTML generado correctamente: " + rutaCompletaHTML);
            } catch (Exception e) {
                System.err.println("Advertencia: No se pudo generar el HTML (el PDF sí se generó): " + e.getMessage());
            }

            // Abrir el PDF automáticamente
            abrirPDF(rutaCompletaPDF);

            return true;

        } catch (JRException e) {
            System.err.println("Error de JasperReports al generar el informe: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            System.err.println("Error inesperado al generar el informe: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        // NO cerramos la conexión porque es singleton y se reutiliza
    }

    /**
     * Compila un archivo .jrxml a JasperReport
     * Si ya existe el .jasper compilado, lo carga directamente
     *
     * @param nombreArchivoJrxml Nombre del archivo .jrxml
     * @return JasperReport compilado o null si hay error
     */
    private static JasperReport compilarInforme(String nombreArchivoJrxml) {
        try {
            // Ruta al archivo .jrxml en resources/reports
            String rutaJrxml = "/reports/" + nombreArchivoJrxml;
            InputStream inputStreamJrxml = InformeService.class.getResourceAsStream(rutaJrxml);

            if (inputStreamJrxml == null) {
                System.err.println("Error: No se encontró el archivo " + rutaJrxml);
                return null;
            }

            // Intentar cargar el .jasper compilado primero
            String nombreJasper = nombreArchivoJrxml.replace(".jrxml", ".jasper");
            String rutaJasper = "/reports/" + nombreJasper;
            InputStream inputStreamJasper = InformeService.class.getResourceAsStream(rutaJasper);

            if (inputStreamJasper != null) {
                // Si existe el .jasper, cargarlo directamente
                System.out.println("Cargando informe compilado: " + rutaJasper);
                return (JasperReport) JRLoader.loadObject(inputStreamJasper);
            } else {
                // Si no existe, compilar el .jrxml
                System.out.println("Compilando informe: " + rutaJrxml);
                return JasperCompileManager.compileReport(inputStreamJrxml);
            }

        } catch (JRException e) {
            System.err.println("Error al compilar/cargar el informe: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Abre el archivo PDF generado con la aplicación predeterminada del sistema
     *
     * @param rutaPDF Ruta completa al archivo PDF
     */
    private static void abrirPDF(String rutaPDF) {
        try {
            // Verificar que Desktop es soportado en esta plataforma
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                File archivoPDF = new File(rutaPDF);

                if (archivoPDF.exists()) {
                    desktop.open(archivoPDF);
                    System.out.println("PDF abierto correctamente");
                } else {
                    System.err.println("Error: El archivo PDF no existe: " + rutaPDF);
                }
            } else {
                System.err.println("Desktop no soportado en esta plataforma");
            }
        } catch (Exception e) {
            System.err.println("Error al intentar abrir el PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Genera un informe de ficha de paciente individual
     *
     * @param dniPaciente DNI del paciente
     * @return true si se generó correctamente, false en caso contrario
     */
    public static boolean generarInformePaciente(String dniPaciente) {
        // Crear mapa de parámetros
        Map<String, Object> parametros = new java.util.HashMap<>();
        parametros.put("dni_paciente_param", dniPaciente);

        // Generar el informe
        String nombreSalida = "Informe_Paciente_" + dniPaciente;
        return generarInformePDF("InformePacienteRehabiapp.jrxml", parametros, nombreSalida);
    }

    /**
     * Genera un informe de listado de sanitarios
     *
     * @return true si se generó correctamente, false en caso contrario
     */
    public static boolean generarInformeSanitarios() {
        // Sin parámetros (lista todos)
        Map<String, Object> parametros = new java.util.HashMap<>();

        // Generar el informe
        String nombreSalida = "Listado_Sanitarios";
        return generarInformePDF("ListadoSanitarios.jrxml", parametros, nombreSalida);
    }

    /**
     * Muestra la agenda de citas de un sanitario en una ventana modal integrada (WebView)
     * El informe se exporta a HTML y se visualiza en un navegador embebido
     *
     * @param dniSanitario DNI del sanitario cuya agenda se desea visualizar
     * @throws RuntimeException si ocurre un error durante la generación o visualización
     */
    public static void mostrarAgendaSanitario(String dniSanitario) {
        Connection conexion = null;
        File archivoHTMLTemp = null;

        try {
            // 1. Obtener la conexión JDBC activa
            conexion = ConexionBD.getConexion();

            if (conexion == null) {
                VentanaUtil.mostrarVentanaInformativa(
                    "Error: No se pudo establecer conexión con la base de datos.",
                    VentanaUtil.TipoMensaje.ERROR
                );
                return;
            }

            // 2. Compilar el informe
            System.out.println("Compilando InformeCitasSanitario.jrxml...");
            JasperReport jasperReport = compilarInforme("InformeCitasSanitario.jrxml");

            if (jasperReport == null) {
                VentanaUtil.mostrarVentanaInformativa(
                    "Error: No se pudo compilar el informe.\nVerifique que el archivo JRXML existe.",
                    VentanaUtil.TipoMensaje.ERROR
                );
                return;
            }

            // 3. Crear parámetros con el DNI del sanitario
            Map<String, Object> parametros = new java.util.HashMap<>();
            parametros.put("dni_sanitario_param", dniSanitario);

            // 4. Rellenar el informe con datos de la BD
            System.out.println("Rellenando informe para sanitario: " + dniSanitario);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parametros, conexion);

            // 5. Crear archivo HTML temporal
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            archivoHTMLTemp = File.createTempFile("AgendaSanitario_" + timestamp, ".html");
            archivoHTMLTemp.deleteOnExit(); // Se elimina automáticamente al cerrar la aplicación

            String rutaHTML = archivoHTMLTemp.getAbsolutePath();

            // 6. Exportar a HTML
            System.out.println("Exportando informe a HTML: " + rutaHTML);
            JasperExportManager.exportReportToHtmlFile(jasperPrint, rutaHTML);

            // 7. Crear ventana modal con WebView
            mostrarInformeEnWebView(rutaHTML, "Agenda de Citas - Sanitario: " + dniSanitario);

        } catch (JRException e) {
            System.err.println("Error de JasperReports: " + e.getMessage());
            e.printStackTrace();
            VentanaUtil.mostrarVentanaInformativa(
                "Error al generar el informe JasperReports:\n" + e.getMessage(),
                VentanaUtil.TipoMensaje.ERROR
            );
        } catch (java.io.IOException e) {
            System.err.println("Error de E/S al crear archivo temporal: " + e.getMessage());
            e.printStackTrace();
            VentanaUtil.mostrarVentanaInformativa(
                "Error al crear archivo temporal HTML:\n" + e.getMessage(),
                VentanaUtil.TipoMensaje.ERROR
            );
        } catch (Exception e) {
            System.err.println("Error inesperado: " + e.getMessage());
            e.printStackTrace();
            VentanaUtil.mostrarVentanaInformativa(
                "Error inesperado al mostrar la agenda:\n" + e.getMessage(),
                VentanaUtil.TipoMensaje.ERROR
            );
        }
        // NO cerramos la conexión porque es singleton y se reutiliza
    }

    /**
     * Muestra un archivo HTML en una ventana modal usando WebView de JavaFX
     *
     * @param rutaHTML Ruta absoluta al archivo HTML
     * @param titulo Título de la ventana
     */
    private static void mostrarInformeEnWebView(String rutaHTML, String titulo) {
        try {
            // Crear WebView para visualizar el HTML
            WebView webView = new WebView();

            // Cargar el archivo HTML
            File archivoHTML = new File(rutaHTML);
            String urlHTML = archivoHTML.toURI().toURL().toString();
            webView.getEngine().load(urlHTML);

            // Crear Stage modal
            Stage stage = new Stage();
            stage.setTitle(titulo);
            stage.initModality(Modality.APPLICATION_MODAL);

            // Establecer el icono de la ventana
            VentanaUtil.establecerIconoInfo(stage);

            // Crear escena con el WebView
            Scene scene = new Scene(webView, 900, 700);

            // Aplicar CSS de configuración de tema (opcional)
            try {
                com.javafx.Interface.controladorVentanaOpciones.aplicarConfiguracionAScene(scene);
            } catch (Exception e) {
                System.out.println("No se pudo aplicar CSS (no crítico): " + e.getMessage());
            }

            stage.setScene(scene);
            stage.setResizable(true);

            System.out.println("Mostrando informe en WebView: " + urlHTML);
            stage.showAndWait();

        } catch (Exception e) {
            System.err.println("Error al mostrar WebView: " + e.getMessage());
            e.printStackTrace();
            VentanaUtil.mostrarVentanaInformativa(
                "Error al abrir la ventana de visualización:\n" + e.getMessage(),
                VentanaUtil.TipoMensaje.ERROR
            );
        }
    }
}