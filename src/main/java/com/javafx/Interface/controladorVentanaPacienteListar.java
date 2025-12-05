package com.javafx.Interface;

import com.javafx.Clases.Paciente;
import com.javafx.Clases.VentanaUtil;
import com.javafx.Clases.VentanaUtil.TipoMensaje;
import com.javafx.DAO.PacienteDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;

/**
 * Controlador para la ventana de ficha/informacion de un paciente
 * Muestra todos los datos del paciente y permite acceder a la edicion
 */
public class controladorVentanaPacienteListar {

    @FXML
    private Button btnAceptar;

    @FXML
    private Button btnDescargarInforme;

    @FXML
    private Button btnEditar;

    @FXML
    private ToggleGroup grupoProtesis;

    @FXML
    private ImageView imgFotoPaciente;

    @FXML
    private Label lblApellidosValor;

    @FXML
    private Label lblDNIValor;

    @FXML
    private Label lblDireccionValor;

    @FXML
    private Label lblDiscapacidadValor;

    @FXML
    private Label lblEdadValor;

    @FXML
    private Label lblEmailValor;

    @FXML
    private Label lblNSSValor;

    @FXML
    private Label lblNombreValor;

    @FXML
    private Label lblTelefono1Valor;

    @FXML
    private Label lblTelefono2Valor;

    @FXML
    private Label lblTituloVentana;

    @FXML
    private RadioButton radioProtesisNo;

    @FXML
    private RadioButton radioProtesisSi;

    @FXML
    private TextArea txtAreaEstado;

    @FXML
    private TextArea txtAreaTratamiento;

    //DAO para operaciones de base de datos
    private PacienteDAO pacienteDAO;

    //DNI del paciente actual
    private String dniPacienteActual;

    //Paciente cargado actualmente
    private Paciente pacienteActual;

    //Indica si hubo cambios durante la edicion
    private boolean cambiosRealizados = false;

    //Modo solo lectura (para usuarios sin permisos de edicion)
    private boolean modoSoloLectura = false;

    /**
     * Metodo initialize se ejecuta automaticamente al cargar el FXML
     */
    @FXML
    public void initialize() {
        pacienteDAO = new PacienteDAO();
    }

    /**
     * Establece el modo solo lectura
     * Deshabilita los botones de edicion
     * @param soloLectura true para activar modo solo lectura
     */
    public void setModoSoloLectura(boolean soloLectura) {
        this.modoSoloLectura = soloLectura;

        if (soloLectura) {
            //Deshabilitar boton de editar
            btnEditar.setDisable(true);
            btnEditar.setOpacity(0.5);
            btnEditar.setTooltip(new javafx.scene.control.Tooltip(
                    "No tienes permisos para editar pacientes"));

            System.out.println("Modo solo lectura activado en ficha de paciente");
        }
    }

    /**
     * Carga los datos de un paciente por su DNI
     * @param dni DNI del paciente a mostrar
     */
    public void cargarDatosPaciente(String dni) {
        this.dniPacienteActual = dni;

        //Obtener paciente completo desde la base de datos
        pacienteActual = pacienteDAO.obtenerPorDNI(dni);

        if (pacienteActual != null) {
            mostrarDatosEnLabels();
            cargarFotoPaciente();
        } else {
            VentanaUtil.mostrarVentanaInformativa(
                    "No se encontro el paciente con DNI: " + dni,
                    TipoMensaje.ERROR
            );
        }
    }

    /**
     * Muestra los datos del paciente en los labels correspondientes
     */
    private void mostrarDatosEnLabels() {
        //Datos personales
        lblNombreValor.setText(valorOGuion(pacienteActual.getNombre()));
        lblApellidosValor.setText(valorOGuion(pacienteActual.getApellidos()));
        lblEdadValor.setText(pacienteActual.getEdad() > 0 ? String.valueOf(pacienteActual.getEdad()) + " años" : "-");
        lblDNIValor.setText(valorOGuion(pacienteActual.getDni()));
        lblEmailValor.setText(valorOGuion(pacienteActual.getEmail()));
        lblNSSValor.setText(valorOGuion(pacienteActual.getNumSS()));

        //Telefonos
        lblTelefono1Valor.setText(valorOGuion(pacienteActual.getTelefono1()));
        lblTelefono2Valor.setText(valorOGuion(pacienteActual.getTelefono2()));

        //Direccion (construir a partir de campos separados)
        String direccionCompleta = construirDireccionCompleta();
        lblDireccionValor.setText(direccionCompleta.isEmpty() ? "-" : direccionCompleta);

        //Discapacidad
        lblDiscapacidadValor.setText(valorOGuion(pacienteActual.getDiscapacidad()));

        //Estado del paciente
        txtAreaEstado.setText(valorOGuion(pacienteActual.getEstadoTratamiento()));

        //Tratamiento
        txtAreaTratamiento.setText(valorOGuion(pacienteActual.getTratamiento()));

        //Protesis
        if (pacienteActual.tieneProtesis()) {
            radioProtesisSi.setSelected(true);
        } else {
            radioProtesisNo.setSelected(true);
        }
    }

    /**
     * Devuelve el valor o un guion si esta vacio
     */
    private String valorOGuion(String valor) {
        return (valor != null && !valor.trim().isEmpty()) ? valor : "-";
    }

    /**
     * Construye la direccion completa a partir de los campos separados
     * Formato: Calle Numero, Piso - CP Localidad (Provincia)
     */
    private String construirDireccionCompleta() {
        if (pacienteActual == null) {
            return "";
        }

        StringBuilder direccion = new StringBuilder();

        //Calle y numero
        String calle = pacienteActual.getCalle();
        String numero = pacienteActual.getNumero();
        if (calle != null && !calle.trim().isEmpty()) {
            direccion.append(calle.trim());
            if (numero != null && !numero.trim().isEmpty() && !numero.equals("0")) {
                direccion.append(" ").append(numero.trim());
            }
        }

        //Piso
        String piso = pacienteActual.getPiso();
        if (piso != null && !piso.trim().isEmpty()) {
            if (direccion.length() > 0) {
                direccion.append(", ");
            }
            direccion.append(piso.trim());
        }

        //Codigo postal y localidad
        String cp = pacienteActual.getCodigoPostal();
        String localidad = pacienteActual.getLocalidad();
        if ((cp != null && !cp.trim().isEmpty()) || (localidad != null && !localidad.trim().isEmpty())) {
            if (direccion.length() > 0) {
                direccion.append(" - ");
            }
            if (cp != null && !cp.trim().isEmpty()) {
                direccion.append(cp.trim()).append(" ");
            }
            if (localidad != null && !localidad.trim().isEmpty()) {
                direccion.append(localidad.trim());
            }
        }

        //Provincia
        String provincia = pacienteActual.getProvincia();
        if (provincia != null && !provincia.trim().isEmpty()) {
            direccion.append(" (").append(provincia.trim()).append(")");
        }

        return direccion.toString().trim();
    }

    /**
     * Carga la foto del paciente desde la base de datos
     */
    private void cargarFotoPaciente() {
        try {
            Image imagen = pacienteDAO.obtenerFoto(dniPacienteActual);

            if (imagen != null) {
                imgFotoPaciente.setImage(imagen);
            } else {
                //Cargar imagen por defecto si no hay foto
                cargarImagenPorDefecto();
            }
        } catch (Exception e) {
            System.err.println("Error al cargar foto del paciente: " + e.getMessage());
            cargarImagenPorDefecto();
        }
    }

    /**
     * Carga una imagen por defecto cuando no hay foto del paciente
     */
    private void cargarImagenPorDefecto() {
        try {
            //Intentar cargar imagen por defecto del proyecto
            Image imagenDefault = new Image(getClass().getResourceAsStream("/com/javafx/Interface/usuario_default.png"));
            imgFotoPaciente.setImage(imagenDefault);
        } catch (Exception e) {
            //Si no existe, dejar el ImageView vacio
            imgFotoPaciente.setImage(null);
        }
    }

    /**
     * Abre la ventana de edicion del paciente
     */
    @FXML
    void abrirVentanaEditar(ActionEvent event) {
        try {
            //Ruta absoluta desde resources con / al principio
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaAgregarPaciente.fxml"));
            Parent root = loader.load();

            //Obtener el controlador y pasar los datos del paciente
            controladorAgregarPaciente controlador = loader.getController();
            controlador.cargarDatosParaEdicion(pacienteActual);

            //Crear escena y aplicar CSS
            Scene scene = new Scene(root);
            controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

            //Crear y mostrar la ventana modal
            Stage stage = new Stage();
            stage.setTitle("Modificar Paciente");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

            //Recargar los datos del paciente despues de cerrar la ventana de edicion
            //El DNI pudo haber cambiado, intentamos recargar con el DNI actual
            Paciente pacienteRecargado = pacienteDAO.buscarPorDni(dniPacienteActual);

            if (pacienteRecargado != null) {
                //El paciente sigue existiendo con el mismo DNI
                cargarDatosPaciente(dniPacienteActual);
                cambiosRealizados = true;
            } else {
                //El DNI cambio o el paciente fue eliminado, cerrar esta ventana
                VentanaUtil.mostrarVentanaInformativa(
                        "El paciente ha sido modificado. Se cerrara esta ventana.",
                        TipoMensaje.INFORMACION
                );
                cambiosRealizados = true;
                cerrarVentana(event);
            }

        } catch (Exception e) {
            System.err.println("Error al abrir ventana de edicion: " + e.getMessage());
            e.printStackTrace();
            VentanaUtil.mostrarVentanaInformativa(
                    "Error al abrir la ventana de edicion.",
                    TipoMensaje.ERROR
            );
        }
    }

    /**
     * Descarga el informe PDF del paciente
     */
    @FXML
    void descargarInformePDF(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar informe del paciente");
        fileChooser.setInitialFileName("Informe_" + pacienteActual.getDni() + ".pdf");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivos PDF", "*.pdf")
        );

        Stage stage = (Stage) btnDescargarInforme.getScene().getWindow();
        File archivoDestino = fileChooser.showSaveDialog(stage);

        if (archivoDestino != null) {
            //TODO: Implementar generacion de PDF con los datos del paciente
            VentanaUtil.mostrarVentanaInformativa(
                    "Funcionalidad de generacion de PDF pendiente de implementar.",
                    TipoMensaje.INFORMACION
            );
        }
    }

    /**
     * Cierra la ventana
     */
    @FXML
    void cerrarVentana(ActionEvent event) {
        Stage stage = (Stage) btnAceptar.getScene().getWindow();
        stage.close();
    }

    /**
     * Indica si hubo cambios durante la visualizacion/edicion
     * @return true si hubo cambios
     */
    public boolean hayCambiosRealizados() {
        return cambiosRealizados;
    }
}
