package com.javafx.Interface;

import com.javafx.Clases.ProgresoTratamiento;
import com.javafx.service.ProgresoService;
import com.javafx.service.SyncProgresoService;
import com.javafx.util.GraficoUtil;
import com.javafx.Clases.VentanaUtil;
import com.javafx.Clases.VentanaUtil.TipoMensaje;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controlador de la ventana de progreso clinico de un paciente.
 * Carga los datos de forma asincrona y actualiza los graficos via polling cada 30s.
 */
public class controladorVentanaProgresoPaciente {

    @FXML private Label lblTituloProgreso;
    @FXML private Label lblUltimaActualizacion;
    @FXML private Label lblEstado;
    @FXML private VBox  contenedorGraficos;
    @FXML private ProgressIndicator indicadorCarga;
    @FXML private Button btnRecargar;
    @FXML private Button btnCerrar;
    @FXML private ScrollPane scrollGraficos;

    private String dniPaciente;
    private final ProgresoService progresoService = new ProgresoService();
    private SyncProgresoService syncService;

    /**
     * Inicializa la ventana con el DNI del paciente y arranca el polling.
     * Llamar inmediatamente despues de cargar el FXML, antes de showAndWait().
     */
    public void inicializarConDni(String dni) {
        this.dniPaciente = dni;
        lblTituloProgreso.setText("Progreso del paciente — " + dni);
        cargarProgresoAsync();
        syncService = new SyncProgresoService(dni, this::onCheckResultado);
        syncService.iniciar();
        // Detener polling si el usuario cierra con la X
        Platform.runLater(() -> {
            Stage s = (Stage) btnCerrar.getScene().getWindow();
            if (s != null) {
                s.setOnCloseRequest(ev -> {
                    if (syncService != null) syncService.detener();
                });
            }
        });
    }

    private void cargarProgresoAsync() {
        indicadorCarga.setVisible(true);
        lblEstado.setText("Cargando...");
        Task<List<ProgresoTratamiento>> task = new Task<>() {
            @Override
            protected List<ProgresoTratamiento> call() {
                return progresoService.obtenerProgreso(dniPaciente);
            }
        };
        task.setOnSucceeded(e -> {
            indicadorCarga.setVisible(false);
            renderizarGraficos(task.getValue());
        });
        task.setOnFailed(e -> {
            indicadorCarga.setVisible(false);
            lblEstado.setText("Sin conexion — datos en cache");
            Throwable ex = task.getException();
            if (ex != null) {
                VentanaUtil.mostrarVentanaInformativa(
                    "Error al cargar progreso: " + ex.getMessage(), TipoMensaje.ERROR);
            }
        });
        Thread t = new Thread(task, "carga-progreso-" + dniPaciente);
        t.setDaemon(true);
        t.start();
    }

    private void renderizarGraficos(List<ProgresoTratamiento> datos) {
        contenedorGraficos.getChildren().clear();
        if (datos == null || datos.isEmpty()) {
            lblEstado.setText("No hay datos de progreso para este paciente.");
            return;
        }
        for (ProgresoTratamiento p : datos) {
            contenedorGraficos.getChildren().add(GraficoUtil.crearLineChart(p));
        }
        lblUltimaActualizacion.setText(
            "Ultima actualizacion: " +
            LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        lblEstado.setText("");
    }

    /** Callback invocado por SyncProgresoService cuando hay datos nuevos. */
    private void onCheckResultado(boolean hasNewData) {
        if (hasNewData) {
            Platform.runLater(() -> {
                progresoService.invalidarCache(dniPaciente);
                cargarProgresoAsync();
                lblEstado.setText("Datos actualizados");
            });
        }
    }

    @FXML
    private void recargar() {
        progresoService.invalidarCache(dniPaciente);
        cargarProgresoAsync();
    }

    @FXML
    private void cerrar() {
        if (syncService != null) syncService.detener();
        ((Stage) btnCerrar.getScene().getWindow()).close();
    }
}
