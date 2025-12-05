package com.javafx.Interface;

import com.javafx.Clases.Cita;
import com.javafx.Clases.SesionUsuario;
import com.javafx.Clases.VentanaUtil;
import com.javafx.Clases.VentanaUtil.TipoMensaje;
import com.javafx.DAO.CitaDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.List;

/**
 * Controlador para la ventana de gestion de citas
 * Muestra el calendario y las citas programadas
 */
public class controladorVentanaCitas {

    @FXML
    private Button btnAnadirCita;

    @FXML
    private Button btnEliminarCita;

    @FXML
    private Button btnVerCita;

    @FXML
    private TableColumn<Cita, String> colDNIPaciente;

    @FXML
    private TableColumn<Cita, String> colFecha;

    @FXML
    private TableColumn<Cita, String> colHora;

    @FXML
    private TableColumn<Cita, String> colNombrePaciente;

    @FXML
    private DatePicker dpFechaCitas;

    @FXML
    private Label lblTituloPestania;

    @FXML
    private TableView<Cita> tblCitas;

    @FXML
    private VBox vboxContenedorCitas;

    //DAO para operaciones con citas
    private CitaDAO citaDAO;

    //DNI del sanitario logueado
    private String dniSanitarioActual;

    /**
     * Metodo initialize se ejecuta automaticamente al cargar el FXML
     */
    @FXML
    public void initialize() {
        citaDAO = new CitaDAO();

        //Configurar columnas de la tabla
        configurarTabla();

        //Establecer fecha actual en el DatePicker
        dpFechaCitas.setValue(LocalDate.now());

        //Listener para cargar citas al cambiar la fecha
        dpFechaCitas.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                cargarCitasPorFecha(newVal);
            }
        });

        //Obtener DNI del sanitario logueado
        SesionUsuario sesion = SesionUsuario.getInstancia();
        if (sesion.haySesionActiva()) {
            dniSanitarioActual = sesion.getDniUsuario();
            cargarCitasPorFecha(LocalDate.now());
        }
    }

    /**
     * Configura las columnas de la tabla de citas
     */
    private void configurarTabla() {
        colNombrePaciente.setCellValueFactory(new PropertyValueFactory<>("nombrePaciente"));
        colDNIPaciente.setCellValueFactory(new PropertyValueFactory<>("dniPaciente"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaFormateada"));
        colHora.setCellValueFactory(new PropertyValueFactory<>("horaFormateada"));
    }

    /**
     * Carga las citas del sanitario para una fecha determinada
     * @param fecha Fecha para la que cargar las citas
     */
    private void cargarCitasPorFecha(LocalDate fecha) {
        if (dniSanitarioActual == null) {
            return;
        }

        List<Cita> citas = citaDAO.obtenerCitasPorSanitarioYFecha(dniSanitarioActual, fecha);
        tblCitas.getItems().clear();
        tblCitas.getItems().addAll(citas);

        System.out.println("Citas cargadas para " + fecha + ": " + citas.size());
    }

    /**
     * Abre la ventana para añadir una nueva cita
     */
    @FXML
    void anadirCita(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaAgregarCita.fxml"));
            Parent root = loader.load();

            //Crear escena y aplicar CSS
            Scene scene = new Scene(root);
            controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

            Stage stage = new Stage();
            stage.setTitle("Nueva Cita");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

            //Recargar citas
            cargarCitasPorFecha(dpFechaCitas.getValue());

        } catch (Exception e) {
            System.err.println("Error al abrir ventana de nueva cita: " + e.getMessage());
            VentanaUtil.mostrarVentanaInformativa(
                    "Error al abrir la ventana de nueva cita.",
                    TipoMensaje.ERROR
            );
        }
    }

    /**
     * Elimina la cita seleccionada
     */
    @FXML
    void eliminarCita(ActionEvent event) {
        Cita citaSeleccionada = tblCitas.getSelectionModel().getSelectedItem();

        if (citaSeleccionada == null) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Debe seleccionar una cita para eliminar.",
                    TipoMensaje.ADVERTENCIA
            );
            return;
        }

        boolean confirmado = VentanaUtil.mostrarVentanaPregunta(
                "¿Esta seguro de que desea eliminar esta cita?\n\n" +
                        "Paciente: " + citaSeleccionada.getNombrePaciente() + "\n" +
                        "Fecha: " + citaSeleccionada.getFechaFormateada() + "\n" +
                        "Hora: " + citaSeleccionada.getHoraFormateada()
        );

        if (confirmado) {
            boolean eliminada = citaDAO.eliminar(
                    citaSeleccionada.getDniPaciente(),
                    dniSanitarioActual,
                    citaSeleccionada.getFecha(),
                    citaSeleccionada.getHora()
            );

            if (eliminada) {
                VentanaUtil.mostrarVentanaInformativa(
                        "Cita eliminada correctamente.",
                        TipoMensaje.EXITO
                );
                cargarCitasPorFecha(dpFechaCitas.getValue());
            } else {
                VentanaUtil.mostrarVentanaInformativa(
                        "No se pudo eliminar la cita.",
                        TipoMensaje.ERROR
                );
            }
        }
    }

    /**
     * Abre la ventana de detalle de la cita seleccionada
     */
    @FXML
    void verCita(ActionEvent event) {
        Cita citaSeleccionada = tblCitas.getSelectionModel().getSelectedItem();

        if (citaSeleccionada == null) {
            VentanaUtil.mostrarVentanaInformativa(
                    "Debe seleccionar una cita para ver.",
                    TipoMensaje.ADVERTENCIA
            );
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaCitaPaciente.fxml"));
            Parent root = loader.load();

            //Obtener controlador y cargar datos
            controladorCitaPaciente controlador = loader.getController();
            controlador.cargarDatosCita(
                    citaSeleccionada.getDniPaciente(),
                    dniSanitarioActual,
                    citaSeleccionada.getFecha(),
                    citaSeleccionada.getHora()
            );

            //Crear escena y aplicar CSS
            Scene scene = new Scene(root);
            controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

            Stage stage = new Stage();
            stage.setTitle("Detalle de Cita");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

            //Recargar si hubo cambios
            if (controlador.hayCambiosRealizados()) {
                cargarCitasPorFecha(dpFechaCitas.getValue());
            }

        } catch (Exception e) {
            System.err.println("Error al abrir detalle de cita: " + e.getMessage());
            e.printStackTrace();
            VentanaUtil.mostrarVentanaInformativa(
                    "Error al abrir el detalle de la cita.",
                    TipoMensaje.ERROR
            );
        }
    }
}
