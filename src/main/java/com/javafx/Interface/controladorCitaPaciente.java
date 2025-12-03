package com.javafx.Interface;

import com.javafx.Clases.Cita;
import com.javafx.Clases.Paciente;
import com.javafx.Clases.VentanaUtil;
import com.javafx.Clases.VentanaUtil.TipoMensaje;
import com.javafx.DAO.CitaDAO;
import com.javafx.DAO.PacienteDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Controlador para la ventana de detalle de una cita
 * Muestra los datos del paciente y permite modificar o eliminar la cita
 */
public class controladorCitaPaciente {

    @FXML
    private Button btnAceptar;

    @FXML
    private Button btnBorrar;

    @FXML
    private Button btnCambiar;

    @FXML
    private Button btnDescargarInforme;

    @FXML
    private DatePicker dpFechaCita;

    @FXML
    private ImageView imgFotoPaciente;

    @FXML
    private Label lblDNIValor;

    @FXML
    private Label lblEdadValor;

    @FXML
    private Label lblNSSValor;

    @FXML
    private Label lblTituloVentana;

    @FXML
    private Spinner<Integer> spnHora;

    @FXML
    private Spinner<Integer> spnMinuto;

    @FXML
    private TextField txtNombrePaciente;

    //DAOs
    private PacienteDAO pacienteDAO;
    private CitaDAO citaDAO;

    //Datos de la cita actual
    private String dniPacienteActual;
    private String dniSanitarioActual;
    private LocalDate fechaOriginal;
    private LocalTime horaOriginal;

    //Indica si hubo cambios
    private boolean cambiosRealizados = false;

    /**
     * Metodo initialize se ejecuta automaticamente al cargar el FXML
     */
    @FXML
    public void initialize() {
        pacienteDAO = new PacienteDAO();
        citaDAO = new CitaDAO();

        //Configurar spinner de hora (8-20)
        SpinnerValueFactory<Integer> factoryHora =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(8, 20, 9);
        spnHora.setValueFactory(factoryHora);

        //Configurar spinner de minutos (0, 15, 30, 45)
        SpinnerValueFactory<Integer> factoryMinuto =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 45, 0, 15);
        spnMinuto.setValueFactory(factoryMinuto);

        //Configurar DatePicker para no permitir fechas pasadas
        dpFechaCita.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });
    }

    /**
     * Carga los datos de una cita existente
     * @param dniPaciente DNI del paciente
     * @param dniSanitario DNI del sanitario
     * @param fecha Fecha de la cita
     * @param hora Hora de la cita
     */
    public void cargarDatosCita(String dniPaciente, String dniSanitario, LocalDate fecha, LocalTime hora) {
        this.dniPacienteActual = dniPaciente;
        this.dniSanitarioActual = dniSanitario;
        this.fechaOriginal = fecha;
        this.horaOriginal = hora;

        //Cargar datos del paciente
        Paciente paciente = pacienteDAO.obtenerPorDNI(dniPaciente);

        if (paciente != null) {
            txtNombrePaciente.setText(paciente.getNombreCompleto());
            lblDNIValor.setText(paciente.getDni());
            lblNSSValor.setText(paciente.getNumSS() != null ? paciente.getNumSS() : "-");
            lblEdadValor.setText(paciente.getEdad() > 0 ? paciente.getEdad() + " años" : "-");

            //Cargar foto
            cargarFotoPaciente(dniPaciente);
        } else {
            txtNombrePaciente.setText("Paciente no encontrado");
            lblDNIValor.setText(dniPaciente);
            lblNSSValor.setText("-");
            lblEdadValor.setText("-");
        }

        //Mostrar datos de la cita
        dpFechaCita.setValue(fecha);
        spnHora.getValueFactory().setValue(hora.getHour());
        spnMinuto.getValueFactory().setValue(hora.getMinute());
    }

    /**
     * Carga la foto del paciente desde la base de datos
     */
    private void cargarFotoPaciente(String dniPaciente) {
        try {
            Image imagen = pacienteDAO.obtenerFoto(dniPaciente);
            if (imagen != null) {
                imgFotoPaciente.setImage(imagen);
            } else {
                cargarImagenPorDefecto();
            }
        } catch (Exception e) {
            System.err.println("Error al cargar foto: " + e.getMessage());
            cargarImagenPorDefecto();
        }
    }

    /**
     * Carga una imagen por defecto
     */
    private void cargarImagenPorDefecto() {
        try {
            Image imagenDefault = new Image(getClass().getResourceAsStream("/usuario_default.png"));
            imgFotoPaciente.setImage(imagenDefault);
        } catch (Exception e) {
            imgFotoPaciente.setImage(null);
        }
    }

    /**
     * Borra la cita actual
     */
    @FXML
    void borrarCita(ActionEvent event) {
        boolean confirmado = VentanaUtil.mostrarVentanaPregunta(
                "¿Esta seguro de que desea eliminar esta cita?\n\n" +
                        "Esta accion no se puede deshacer."
        );

        if (confirmado) {
            boolean eliminada = citaDAO.eliminar(dniPacienteActual, dniSanitarioActual, fechaOriginal, horaOriginal);

            if (eliminada) {
                VentanaUtil.mostrarVentanaInformativa(
                        "La cita ha sido eliminada correctamente.",
                        TipoMensaje.EXITO
                );
                cambiosRealizados = true;
                cerrarVentana(event);
            } else {
                VentanaUtil.mostrarVentanaInformativa(
                        "No se pudo eliminar la cita.",
                        TipoMensaje.ERROR
                );
            }
        }
    }

    /**
     * Guarda los cambios en la cita (fecha/hora)
     */
    @FXML
    void cambiarCita(ActionEvent event) {
        //Validar fecha
        if (dpFechaCita.getValue() == null) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Debe seleccionar una fecha para la cita.",
                    TipoMensaje.ADVERTENCIA
            );
            return;
        }

        LocalDate nuevaFecha = dpFechaCita.getValue();
        int nuevaHora = spnHora.getValue();
        int nuevoMinuto = spnMinuto.getValue();
        LocalTime nuevoTiempo = LocalTime.of(nuevaHora, nuevoMinuto);

        //Verificar si hay cambios
        if (nuevaFecha.equals(fechaOriginal) && nuevoTiempo.equals(horaOriginal)) {
            VentanaUtil.mostrarVentanaInformativa(
                    "No se han realizado cambios en la cita.",
                    TipoMensaje.INFORMACION
            );
            return;
        }

        //Verificar si el nuevo horario esta disponible
        if (!nuevaFecha.equals(fechaOriginal) || !nuevoTiempo.equals(horaOriginal)) {
            if (citaDAO.existeCitaEnHorario(dniSanitarioActual, nuevaFecha, nuevoTiempo)) {
                VentanaUtil.mostrarVentanaInformativa(
                        "Ya existe una cita programada para esa fecha y hora.",
                        TipoMensaje.ADVERTENCIA
                );
                return;
            }
        }

        //Actualizar la cita
        boolean actualizada = citaDAO.actualizar(
                dniPacienteActual,
                dniSanitarioActual,
                fechaOriginal,
                horaOriginal,
                nuevaFecha,
                nuevoTiempo
        );

        if (actualizada) {
            VentanaUtil.mostrarVentanaInformativa(
                    "La cita ha sido actualizada correctamente.\n\n" +
                            "Nueva fecha: " + nuevaFecha.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "\n" +
                            "Nueva hora: " + nuevoTiempo.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                    TipoMensaje.EXITO
            );

            //Actualizar valores originales
            fechaOriginal = nuevaFecha;
            horaOriginal = nuevoTiempo;
            cambiosRealizados = true;
        } else {
            VentanaUtil.mostrarVentanaInformativa(
                    "No se pudo actualizar la cita.",
                    TipoMensaje.ERROR
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
     * Descarga el informe del paciente en PDF
     */
    @FXML
    void descargarInformePDF(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar informe del paciente");
        fileChooser.setInitialFileName("Informe_" + dniPacienteActual + ".pdf");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivos PDF", "*.pdf")
        );

        Stage stage = (Stage) btnDescargarInforme.getScene().getWindow();
        File archivoDestino = fileChooser.showSaveDialog(stage);

        if (archivoDestino != null) {
            //TODO: Implementar generacion de PDF con iText o similar
            VentanaUtil.mostrarVentanaInformativa(
                    "Funcionalidad de generacion de PDF pendiente de implementar.",
                    TipoMensaje.INFORMACION
            );
        }
    }

    /**
     * Indica si hubo cambios
     */
    public boolean hayCambiosRealizados() {
        return cambiosRealizados;
    }
}