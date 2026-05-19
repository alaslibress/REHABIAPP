package com.javafx.service;

import com.javafx.Clases.CheckProgreso;
import com.javafx.DAO.ProgresoDAO;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Servicio de polling de nuevas sesiones de juego.
 * Ejecuta GET /api/pacientes/{dni}/progreso/check cada 30s mientras la ventana este activa.
 * Hilo daemon — se destruye con la JVM si la ventana no llama a detener().
 */
public class SyncProgresoService {

    private static final Logger LOG = LoggerFactory.getLogger(SyncProgresoService.class);
    private static final long INTERVALO_S = 30L;

    private final String dniPac;
    private final Consumer<Boolean> callback;
    private final ProgresoDAO dao = new ProgresoDAO();
    private ScheduledExecutorService executor;
    private Instant ultimoCheck = Instant.EPOCH;

    public SyncProgresoService(String dniPac, Consumer<Boolean> callback) {
        this.dniPac = dniPac;
        this.callback = callback;
    }

    /** Inicia el polling. Primer check inmediato, luego cada 30s. */
    public void iniciar() {
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sync-progreso-" + dniPac);
            t.setDaemon(true);
            return t;
        });
        executor.scheduleAtFixedRate(this::comprobar, 0, INTERVALO_S, TimeUnit.SECONDS);
    }

    private void comprobar() {
        try {
            CheckProgreso res = dao.comprobarNuevosDatos(dniPac, ultimoCheck);
            ultimoCheck = Instant.now();
            if (res != null && res.hasNewData()) {
                Platform.runLater(() -> callback.accept(true));
            }
        } catch (Exception e) {
            LOG.debug("SyncProgresoService check fallo (silencioso): {}", e.getMessage());
        }
    }

    /** Detiene el polling. Llamar siempre al cerrar la ventana. */
    public void detener() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }
}
