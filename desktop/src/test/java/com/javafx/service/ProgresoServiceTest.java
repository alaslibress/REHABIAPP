package com.javafx.service;

import com.javafx.Clases.CheckProgreso;
import com.javafx.Clases.ProgresoTratamiento;
import com.javafx.DAO.ProgresoDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests de ProgresoService con mock de ProgresoDAO.
 * Verifica la logica de cache de 30s y la delegacion al DAO.
 */
@ExtendWith(MockitoExtension.class)
class ProgresoServiceTest {

    @Mock
    private ProgresoDAO daoMock;

    private ProgresoService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new ProgresoService();
        // Sustituir el campo dao con el mock via reflexion
        var campo = ProgresoService.class.getDeclaredField("dao");
        campo.setAccessible(true);
        campo.set(service, daoMock);
    }

    private ProgresoTratamiento crearProgreso(String codTrat) {
        return new ProgresoTratamiento(codTrat, "Nombre", "Hombro", "ROM",
            0.0, Instant.EPOCH, 45.0, Instant.now(), 100.0, List.of());
    }

    @Test
    void obtenerProgreso_cacheaResultado_durante30s() {
        List<ProgresoTratamiento> datos = List.of(crearProgreso("T01"));
        when(daoMock.obtenerProgreso("12345678A")).thenReturn(datos);

        // Primera llamada — debe ir al DAO
        List<ProgresoTratamiento> r1 = service.obtenerProgreso("12345678A");
        // Segunda llamada inmediata — debe usar cache
        List<ProgresoTratamiento> r2 = service.obtenerProgreso("12345678A");

        assertSame(r1, r2);
        // DAO llamado solo una vez por el cache
        verify(daoMock, times(1)).obtenerProgreso("12345678A");
    }

    @Test
    void invalidarCache_fuerza_nuevoFetch() {
        List<ProgresoTratamiento> datos = List.of(crearProgreso("T01"));
        when(daoMock.obtenerProgreso("12345678A")).thenReturn(datos);

        service.obtenerProgreso("12345678A"); // llena cache
        service.invalidarCache("12345678A");
        service.obtenerProgreso("12345678A"); // debe llamar al DAO de nuevo

        verify(daoMock, times(2)).obtenerProgreso("12345678A");
    }

    @Test
    void comprobarNuevosDatos_propagaCheckProgreso() {
        Instant desde = Instant.now();
        CheckProgreso esperado = new CheckProgreso(true, desde, 3);
        when(daoMock.comprobarNuevosDatos("12345678A", desde)).thenReturn(esperado);

        CheckProgreso resultado = service.comprobarNuevosDatos("12345678A", desde);

        assertTrue(resultado.hasNewData());
        assertEquals(3, resultado.count());
        verify(daoMock).comprobarNuevosDatos("12345678A", desde);
    }
}
