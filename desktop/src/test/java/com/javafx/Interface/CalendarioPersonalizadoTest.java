package com.javafx.Interface;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para CalendarioPersonalizado.
 * Utiliza JFXPanel en @BeforeAll para inicializar el toolkit de JavaFX en modo headless.
 */
class CalendarioPersonalizadoTest {

    /**
     * Inicializa el toolkit de JavaFX antes de ejecutar cualquier test.
     * JFXPanel es el mecanismo mas simple para arrancar el toolkit sin stage visible.
     */
    @BeforeAll
    static void inicializarJavaFX() throws Exception {
        // JFXPanel arranca el toolkit al ser instanciado
        new JFXPanel();
        // Esperar a que el hilo de JavaFX este listo
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(latch::countDown);
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX toolkit no inicio en 5 segundos");
    }

    /**
     * Ejecuta un bloque de codigo en el hilo de JavaFX y espera su finalizacion.
     *
     * @param accion bloque a ejecutar
     */
    private static void enHiloJFX(Runnable accion) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final Throwable[] error = {null};
        Platform.runLater(() -> {
            try {
                accion.run();
            } catch (Throwable t) {
                error[0] = t;
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS), "Timeout esperando hilo JavaFX");
        if (error[0] != null) throw new RuntimeException(error[0]);
    }

    // ============================== TESTS ==============================

    /**
     * Test 1: El mes inicial del calendario debe ser el mes actual.
     */
    @Test
    void testMesInicialEsMesActual() throws Exception {
        enHiloJFX(() -> {
            CalendarioPersonalizado cal = new CalendarioPersonalizado();
            assertEquals(YearMonth.now(), cal.getMesActual(),
                    "El mes inicial debe ser el mes actual");
        });
    }

    /**
     * Test 2: Al establecer un conteo de citas, el badge del dia correspondiente
     * debe ser visible con el texto correcto.
     */
    @Test
    void testConteoCitasActualizaBadge() throws Exception {
        enHiloJFX(() -> {
            CalendarioPersonalizado cal = new CalendarioPersonalizado();
            LocalDate hoy = LocalDate.now();

            cal.setCitasPorDia(Map.of(hoy, 3));
            cal.refrescar();

            // Buscar la celda del dia de hoy en el grid
            boolean badgeEncontrado = cal.getGridCeldas().getChildren().stream()
                    .filter(n -> n instanceof CalendarioPersonalizado.CeldaDia)
                    .map(n -> (CalendarioPersonalizado.CeldaDia) n)
                    .filter(c -> c.getFecha() != null && c.getFecha().equals(hoy))
                    .anyMatch(c -> c.getBadgeTexto() != null
                            && c.getBadgeTexto().equals("3 citas")
                            && c.isBadgeVisible());

            assertTrue(badgeEncontrado,
                    "La celda de hoy debe mostrar badge '3 citas' visible");
        });
    }

    /**
     * Test 3: Un click simple sobre una fecha debe actualizar la propiedad
     * fechaSeleccionadaProperty con esa fecha.
     */
    @Test
    void testClickSimpleActualizaFechaSeleccionada() throws Exception {
        enHiloJFX(() -> {
            CalendarioPersonalizado cal = new CalendarioPersonalizado();
            LocalDate objetivo = LocalDate.now().plusDays(2);

            // Simular la seleccion directa via API publica (equivalente a click simple)
            cal.setFechaSeleccionada(objetivo);

            assertEquals(objetivo, cal.getFechaSeleccionada(),
                    "getFechaSeleccionada() debe devolver la fecha establecida");
            assertTrue(cal.getFechasSeleccionadas().contains(objetivo),
                    "El conjunto de seleccionadas debe contener la fecha");
        });
    }

    /**
     * Test 4: La navegacion al mes siguiente debe incrementar el mes visible en 1.
     */
    @Test
    void testNavegacionMesSiguiente() throws Exception {
        enHiloJFX(() -> {
            CalendarioPersonalizado cal = new CalendarioPersonalizado();
            YearMonth mesInicial = cal.getMesActual();

            cal.getBtnMesSiguiente().fire();

            assertEquals(mesInicial.plusMonths(1), cal.getMesActual(),
                    "Tras pulsar siguiente, el mes debe avanzar 1");
        });
    }

    /**
     * Test 5: La primera celda del grid debe tener la styleClass "calendario-celda-otro-mes"
     * cuando el mes no comienza en lunes.
     */
    @Test
    void testCeldaFueraDeMesTieneStyleClass() throws Exception {
        enHiloJFX(() -> {
            // Usar un mes que sabemos que no empieza en lunes: mayo 2025 empieza en jueves
            CalendarioPersonalizado cal = new CalendarioPersonalizado();
            cal.setMesActual(YearMonth.of(2025, 5));

            // El 1 de mayo 2025 es jueves (offset=3), las 3 primeras celdas son de abril
            CalendarioPersonalizado.CeldaDia primeracelda =
                    cal.getGridCeldas().getChildren().stream()
                            .filter(n -> n instanceof CalendarioPersonalizado.CeldaDia)
                            .map(n -> (CalendarioPersonalizado.CeldaDia) n)
                            .findFirst()
                            .orElse(null);

            assertNotNull(primeracelda, "Debe existir al menos una celda en el grid");
            assertTrue(primeracelda.getStyleClass().contains("calendario-celda-otro-mes"),
                    "La primera celda (mes adyacente) debe tener styleClass 'calendario-celda-otro-mes'");
        });
    }
}
