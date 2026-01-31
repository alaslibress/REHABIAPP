package com.javafx.Interface;

import com.javafx.Clases.VentanaUtil;
import com.javafx.Clases.VentanaUtil.TipoMensaje;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.awt.Desktop;
import java.net.URI;

/**
 * Controlador para la ventana de ayuda
 * Muestra informacion de uso de la aplicacion y permite contactar con soporte
 */
public class controladorVentanaAyuda {

    @FXML
    private Button btnContactar;

    @FXML
    private Label lblTituloPestania;

    @FXML
    private TextArea txtAreaAyuda;

    //Email de soporte
    private static final String EMAIL_SOPORTE = "soporte@rehabiapp.com";

    //Asunto por defecto del email
    private static final String ASUNTO_EMAIL = "Consulta desde RehabiAPP";

    /**
     * Metodo initialize se ejecuta automaticamente al cargar el FXML
     */
    @FXML
    public void initialize() {
        //Cargar el texto de ayuda
        cargarTextoAyuda();
    }

    /**
     * Carga el texto de ayuda en el TextArea
     */
    private void cargarTextoAyuda() {
        String textoAyuda = """
                BIENVENIDO A REHABIAPP
                ======================
                
                Esta aplicacion permite gestionar sanitarios, pacientes y citas medicas de forma sencilla.
                
                
                FUNCIONES PRINCIPALES
                ---------------------
                
                1. GESTION DE SANITARIOS
                   - Añadir nuevos medicos especialistas y enfermeros
                   - Editar informacion personal y de contacto
                   - Eliminar sanitarios del sistema
                   - Visualizar lista completa de personal sanitario
                
                2. GESTION DE PACIENTES
                   - Registro completo con datos personales
                   - Gestion de discapacidades y tratamientos
                   - Almacenamiento de fotos del paciente
                   - Seguimiento del estado del tratamiento
                   - Generacion de informes en PDF
                
                3. GESTION DE CITAS
                   - Calendario interactivo para programar citas
                   - Visualizacion de citas por fecha
                   - Creacion y eliminacion de citas
                   - Asociacion de pacientes con sanitarios
                
                
                PERMISOS DE USUARIO
                -------------------
                
                - MEDICO ESPECIALISTA: Acceso completo a todas las funciones
                  (crear, editar, eliminar pacientes y sanitarios)
                
                - ENFERMERO: Acceso de solo lectura a pacientes y gestion de citas
                  (no puede crear, editar ni eliminar pacientes o sanitarios)
                
                
                OPCIONES DE CONFIGURACION
                -------------------------
                
                En el menu Opciones puede personalizar:
                - Tamaño de letra de la aplicacion
                - Tema visual (claro u oscuro)
                
                
                ATAJOS DE TECLADO
                -----------------
                
                - Enter en campos de busqueda: Realizar busqueda
                - Doble clic en tabla: Abrir ficha del elemento
                - Esc en ventanas emergentes: Cerrar ventana
                
                
                SOPORTE TECNICO
                ---------------
                
                Para cualquier duda o incidencia, pulse el boton "Contactar"
                o envie un email a: soporte@rehabiapp.com
                
                
                VERSION: 2.0
                DESARROLLADO POR: Alejandro Pozo Perez
                """;

        txtAreaAyuda.setText(textoAyuda);
    }

    /**
     * Abre el cliente de correo para contactar con soporte
     */
    @FXML
    void contactarSoporte(ActionEvent event) {
        try {
            //Verificar si Desktop esta soportado
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();

                //Verificar si se puede abrir el cliente de correo
                if (desktop.isSupported(Desktop.Action.MAIL)) {
                    //Construir URI de mailto
                    String mailto = "mailto:" + EMAIL_SOPORTE +
                            "?subject=" + codificarURL(ASUNTO_EMAIL) +
                            "&body=" + codificarURL(obtenerCuerpoEmail());

                    //Abrir cliente de correo
                    desktop.mail(new URI(mailto));

                    System.out.println("Cliente de correo abierto");

                } else {
                    //Si no se puede abrir el cliente de correo, mostrar email
                    mostrarEmailAlternativo();
                }
            } else {
                //Si Desktop no esta soportado, mostrar email
                mostrarEmailAlternativo();
            }

        } catch (Exception e) {
            System.err.println("Error al abrir cliente de correo: " + e.getMessage());
            mostrarEmailAlternativo();
        }
    }

    /**
     * Muestra el email de soporte de forma alternativa
     */
    private void mostrarEmailAlternativo() {
        VentanaUtil.mostrarVentanaInformativa(
                "Para contactar con soporte, envie un email a:\n\n" +
                        EMAIL_SOPORTE + "\n\n" +
                        "Describa su consulta o problema con el mayor detalle posible.",
                TipoMensaje.INFORMACION
        );
    }

    /**
     * Obtiene el cuerpo del email predeterminado
     * @return Texto del cuerpo del email
     */
    private String obtenerCuerpoEmail() {
        return "Estimado equipo de soporte,\n\n" +
                "Me pongo en contacto con ustedes para:\n\n" +
                "[Describa su consulta o problema aqui]\n\n" +
                "Gracias por su atencion.\n" +
                "Un saludo.";
    }

    /**
     * Codifica un texto para usar en URL
     * @param texto Texto a codificar
     * @return Texto codificado
     */
    private String codificarURL(String texto) {
        try {
            return java.net.URLEncoder.encode(texto, "UTF-8")
                    .replace("+", "%20");
        } catch (Exception e) {
            return texto;
        }
    }
}