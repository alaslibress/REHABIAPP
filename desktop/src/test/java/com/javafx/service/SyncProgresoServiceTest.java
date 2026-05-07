package com.javafx.service;

import com.javafx.Clases.CheckProgreso;
import com.javafx.DAO.ProgresoDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests de SyncProgresoService.
 * Usa CountDownLatch para esperar el callback en background sin Awaitility.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SyncProgresoServiceTest {

    @Mock
    private ProgresoDAO daoMock;

    private SyncProgresoService servicio;

    @BeforeEach
    void setUp() throws Exception {
        servicio = new SyncProgresoService("12345678A", b -> {});
        var campo = SyncProgresoService.class.getDeclaredField("dao");
        campo.setAccessible(true);
        campo.set(servicio, daoMock);
    }

    @AfterEach
    void tearDown() {
        servicio.detener();
    }

    @Test
    void iniciar_invocaCallback_cuando_hasNewData_true() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean callbackRecibido = new AtomicBoolean(false);

        CheckProgreso check = new CheckProgreso(true, Instant.now(), 1);
        when(daoMock.comprobarNuevosDatos(anyString(), any())).thenReturn(check);

        SyncProgresoService svc = new SyncProgresoService("12345678A", hasNew -> {
            callbackRecibido.set(hasNew);
            latch.countDown();
        });
        var campo = SyncProgresoService.class.getDeclaredField("dao");
        campo.setAccessible(true);
        campo.set(svc, daoMock);

        svc.iniciar();
        boolean terminoEnTiempo = latch.await(5, TimeUnit.SECONDS);
        svc.detener();

        assertTrue(terminoEnTiempo, "El callback no fue invocado en 5 segundos");
        assertTrue(callbackRecibido.get());
    }

    @Test
    void detener_paraExecutor() throws Exception {
        // Acceder al campo executor via reflexion
        when(daoMock.comprobarNuevosDatos(anyString(), any()))
            .thenReturn(new CheckProgreso(false, Instant.now(), 0));

        servicio.iniciar();
        Thread.sleep(100); // dar tiempo a arrancar
        servicio.detener();

        var campo = SyncProgresoService.class.getDeclaredField("executor");
        campo.setAccessible(true);
        Object executor = campo.get(servicio);
        // Despues de detener(), el campo debe ser null
        assertNull(executor);
    }
}
