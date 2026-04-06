package com.javafx.Clases;

import com.javafx.DAO.CitaDAO;
import com.javafx.DAO.PacienteDAO;
import com.javafx.DAO.SanitarioDAO;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;

import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Servicio para la generacion de informes PDF usando JasperReports.
 * Utiliza JRBeanCollectionDataSource — no requiere conexion JDBC.
 * Los datos se obtienen via la API REST antes de generar el informe.
 */
public class InformeService {

    private static final String DIRECTORIO_INFORMES = "informes";

    private static final PacienteDAO pacienteDAO = new PacienteDAO();
    private static final SanitarioDAO sanitarioDAO = new SanitarioDAO();
    private static final CitaDAO citaDAO = new CitaDAO();

    /**
     * Genera un informe PDF a partir de un archivo .jrxml y una coleccion de beans.
     *
     * @param nombreArchivoJrxml Nombre del archivo .jrxml en resources/reports/
     * @param parametros         Parametros adicionales del informe
     * @param datos              Coleccion de POJOs que alimenta el informe
     * @param nombreSalida       Nombre base del archivo de salida (sin extension)
     * @return true si el informe se genero correctamente
     */
    public static boolean generarInformePDF(String nombreArchivoJrxml,
                                            Map<String, Object> parametros,
                                            Collection<?> datos,
                                            String nombreSalida) {
        try {
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(datos);
            JasperReport jasperReport = compilarInforme(nombreArchivoJrxml);

            if (jasperReport == null) {
                System.err.println("Error: No se pudo compilar el informe");
                return false;
            }

            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parametros, dataSource);

            File dirInformes = new File(DIRECTORIO_INFORMES);
            if (!dirInformes.exists()) {
                dirInformes.mkdirs();
            }

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String rutaSalida = DIRECTORIO_INFORMES + File.separator + nombreSalida + "_" + timestamp + ".pdf";

            JasperExportManager.exportReportToPdfFile(jasperPrint, rutaSalida);
            System.out.println("Informe PDF generado: " + rutaSalida);

            // Exportar HTML como secundario (si falla no afecta al PDF)
            try {
                String rutaHTML = rutaSalida.replace(".pdf", ".html");
                JasperExportManager.exportReportToHtmlFile(jasperPrint, rutaHTML);
            } catch (Exception e) {
                System.err.println("Advertencia: no se genero el HTML: " + e.getMessage());
            }

            abrirPDF(rutaSalida);
            return true;

        } catch (JRException e) {
            System.err.println("Error de JasperReports: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            System.err.println("Error inesperado al generar informe: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Genera un informe PDF en una ruta personalizada.
     *
     * @param nombreArchivoJrxml Nombre del archivo .jrxml
     * @param parametros         Parametros del informe
     * @param datos              Coleccion de POJOs
     * @param rutaDestino        Ruta completa de salida (con nombre de archivo)
     * @param abrirPDF           true para abrir el PDF automaticamente
     * @return true si se genero correctamente
     */
    public static boolean generarInformePDFEnRuta(String nombreArchivoJrxml,
                                                   Map<String, Object> parametros,
                                                   Collection<?> datos,
                                                   String rutaDestino,
                                                   boolean abrirPDF) {
        try {
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(datos);
            JasperReport jasperReport = compilarInforme(nombreArchivoJrxml);

            if (jasperReport == null) return false;

            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parametros, dataSource);
            JasperExportManager.exportReportToPdfFile(jasperPrint, rutaDestino);
            System.out.println("Informe generado en: " + rutaDestino);

            if (abrirPDF) abrirPDF(rutaDestino);
            return true;

        } catch (JRException e) {
            System.err.println("Error de JasperReports: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Error inesperado: " + e.getMessage());
            return false;
        }
    }

    /**
     * Genera el informe de ficha de un paciente individual.
     * Carga los datos del paciente via la API REST.
     */
    public static boolean generarInformePaciente(String dniPaciente) {
        try {
            Paciente paciente = pacienteDAO.buscarPorDni(dniPaciente);
            if (paciente == null) return false;

            Map<String, Object> parametros = new java.util.HashMap<>();
            parametros.put("dni_paciente_param", dniPaciente);

            return generarInformePDF(
                    "InformePacienteRehabiapp.jrxml",
                    parametros,
                    List.of(paciente),
                    "Informe_Paciente_" + dniPaciente
            );
        } catch (Exception e) {
            System.err.println("Error al generar informe de paciente: " + e.getMessage());
            return false;
        }
    }

    /**
     * Genera el informe de paciente en una ruta personalizada.
     * Mantiene la firma anterior para compatibilidad con controladores.
     */
    public static boolean generarInformePacienteEnRuta(String dniPaciente, String rutaDestino, boolean abrirPDF) {
        try {
            Paciente paciente = pacienteDAO.buscarPorDni(dniPaciente);
            if (paciente == null) return false;

            Map<String, Object> parametros = new java.util.HashMap<>();
            parametros.put("dni_paciente_param", dniPaciente);

            return generarInformePDFEnRuta(
                    "InformePacienteRehabiapp.jrxml",
                    parametros,
                    List.of(paciente),
                    rutaDestino,
                    abrirPDF
            );
        } catch (Exception e) {
            System.err.println("Error al generar informe de paciente: " + e.getMessage());
            return false;
        }
    }

    /**
     * Genera informe de paciente y permite guardar copia personalizada.
     */
    public static boolean generarInformePacienteConCopiaPersonalizada(String dniPaciente) {
        boolean exitoPrincipal = generarInformePaciente(dniPaciente);

        if (!exitoPrincipal) return false;

        javafx.stage.DirectoryChooser directoryChooser = new javafx.stage.DirectoryChooser();
        directoryChooser.setTitle("Seleccione carpeta para guardar copia del informe");

        File dirInicial = new File(System.getProperty("user.home"));
        if (dirInicial.exists()) directoryChooser.setInitialDirectory(dirInicial);

        File dirSeleccionado = directoryChooser.showDialog(null);

        if (dirSeleccionado != null) {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String nombreArchivo = "Informe_Paciente_" + dniPaciente + "_" + timestamp + ".pdf";
            String rutaCompleta = dirSeleccionado.getAbsolutePath() + File.separator + nombreArchivo;

            boolean exitoCopia = generarInformePacienteEnRuta(dniPaciente, rutaCompleta, false);

            if (exitoCopia) {
                VentanaUtil.mostrarVentanaInformativa(
                        "Informe guardado correctamente en:\n" + rutaCompleta,
                        VentanaUtil.TipoMensaje.EXITO
                );
            } else {
                VentanaUtil.mostrarVentanaInformativa(
                        "El informe principal se genero correctamente, pero hubo un error al guardar la copia.",
                        VentanaUtil.TipoMensaje.ADVERTENCIA
                );
            }
        }

        return true;
    }

    /**
     * Genera informe de listado de sanitarios.
     * Carga todos los sanitarios via la API REST.
     */
    public static boolean generarInformeSanitarios() {
        try {
            List<Sanitario> sanitarios = sanitarioDAO.listarTodos();
            Map<String, Object> parametros = new java.util.HashMap<>();

            return generarInformePDF(
                    "ListadoSanitarios.jrxml",
                    parametros,
                    sanitarios,
                    "Listado_Sanitarios"
            );
        } catch (Exception e) {
            System.err.println("Error al generar informe de sanitarios: " + e.getMessage());
            return false;
        }
    }

    /**
     * Muestra la agenda de citas de un sanitario en una ventana modal con WebView.
     * Carga las citas via la API REST.
     */
    public static void mostrarAgendaSanitario(String dniSanitario) {
        File archivoHTMLTemp = null;

        try {
            List<Cita> citas = citaDAO.listarPorSanitario(dniSanitario);

            Map<String, Object> parametros = new java.util.HashMap<>();
            parametros.put("dni_sanitario_param", dniSanitario);

            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(citas);
            JasperReport jasperReport = compilarInforme("InformeCitasSanitario.jrxml");

            if (jasperReport == null) {
                VentanaUtil.mostrarVentanaInformativa(
                        "Error: No se pudo compilar el informe de agenda.",
                        VentanaUtil.TipoMensaje.ERROR
                );
                return;
            }

            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parametros, dataSource);

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            archivoHTMLTemp = File.createTempFile("AgendaSanitario_" + timestamp, ".html");
            archivoHTMLTemp.deleteOnExit();

            JasperExportManager.exportReportToHtmlFile(jasperPrint, archivoHTMLTemp.getAbsolutePath());

            mostrarInformeEnWebView(archivoHTMLTemp.getAbsolutePath(),
                    "Agenda de Citas - Sanitario: " + dniSanitario);

        } catch (JRException e) {
            System.err.println("Error de JasperReports: " + e.getMessage());
            e.printStackTrace();
            VentanaUtil.mostrarVentanaInformativa(
                    "Error al generar el informe:\n" + e.getMessage(),
                    VentanaUtil.TipoMensaje.ERROR
            );
        } catch (java.io.IOException e) {
            System.err.println("Error de I/O al crear archivo temporal: " + e.getMessage());
            VentanaUtil.mostrarVentanaInformativa(
                    "Error al crear archivo temporal:\n" + e.getMessage(),
                    VentanaUtil.TipoMensaje.ERROR
            );
        } catch (Exception e) {
            System.err.println("Error inesperado: " + e.getMessage());
            VentanaUtil.mostrarVentanaInformativa(
                    "Error inesperado al mostrar la agenda:\n" + e.getMessage(),
                    VentanaUtil.TipoMensaje.ERROR
            );
        }
    }

    // ─────────────────────────── privado ───────────────────────────

    private static JasperReport compilarInforme(String nombreArchivoJrxml) {
        try {
            String rutaJrxml = "/reports/" + nombreArchivoJrxml;
            InputStream isJrxml = InformeService.class.getResourceAsStream(rutaJrxml);

            if (isJrxml == null) {
                System.err.println("Archivo de informe no encontrado: " + rutaJrxml);
                return null;
            }

            String nombreJasper = nombreArchivoJrxml.replace(".jrxml", ".jasper");
            InputStream isJasper = InformeService.class.getResourceAsStream("/reports/" + nombreJasper);

            if (isJasper != null) {
                return (JasperReport) JRLoader.loadObject(isJasper);
            } else {
                return JasperCompileManager.compileReport(isJrxml);
            }

        } catch (JRException e) {
            System.err.println("Error al compilar informe: " + e.getMessage());
            return null;
        }
    }

    private static void abrirPDF(String rutaPDF) {
        try {
            if (Desktop.isDesktopSupported()) {
                File archivoPDF = new File(rutaPDF);
                if (archivoPDF.exists()) {
                    Desktop.getDesktop().open(archivoPDF);
                }
            }
        } catch (Exception e) {
            System.err.println("Error al abrir el PDF: " + e.getMessage());
        }
    }

    private static void mostrarInformeEnWebView(String rutaHTML, String titulo) {
        try {
            WebView webView = new WebView();
            File archivoHTML = new File(rutaHTML);
            webView.getEngine().load(archivoHTML.toURI().toURL().toString());

            Stage stage = new Stage();
            stage.setTitle(titulo);
            stage.initModality(Modality.APPLICATION_MODAL);
            VentanaUtil.establecerIconoInfo(stage);

            Scene scene = new Scene(webView, 900, 700);
            try {
                com.javafx.Interface.controladorVentanaOpciones.aplicarConfiguracionAScene(scene);
            } catch (Exception e) {
                System.out.println("No se pudo aplicar CSS: " + e.getMessage());
            }

            stage.setScene(scene);
            stage.setResizable(true);
            stage.showAndWait();

        } catch (Exception e) {
            System.err.println("Error al mostrar WebView: " + e.getMessage());
            VentanaUtil.mostrarVentanaInformativa(
                    "Error al abrir la ventana de visualizacion:\n" + e.getMessage(),
                    VentanaUtil.TipoMensaje.ERROR
            );
        }
    }
}
