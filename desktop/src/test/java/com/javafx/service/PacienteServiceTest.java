package com.javafx.service;

import com.javafx.Clases.Paciente;
import com.javafx.DAO.PacienteDAO;
import com.javafx.excepcion.ValidacionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para PacienteService.
 * Verifica que el servicio delega correctamente al DAO y que
 * la logica de lectura de bytes de foto funciona como se espera.
 */
@ExtendWith(MockitoExtension.class)
class PacienteServiceTest {

    @Mock
    private PacienteDAO pacienteDAOMock;

    private PacienteService servicio;

    @BeforeEach
    void setUp() {
        servicio = new PacienteService(pacienteDAOMock);
    }

    private Paciente pacienteMinimo() {
        return new Paciente("12345678A", "Juan", "Garcia", "Lopez",
            30, "juan@test.com", "SS001", false, "S01");
    }

    // ==================== listarTodos ====================

    @Test
    void listarTodos_delegaAlDAO() {
        List<Paciente> lista = List.of(pacienteMinimo());
        when(pacienteDAOMock.listarTodos()).thenReturn(lista);

        List<Paciente> resultado = servicio.listarTodos();

        assertEquals(1, resultado.size());
        verify(pacienteDAOMock).listarTodos();
    }

    // ==================== insertar sin foto ====================

    @Test
    void insertar_sinFoto_llamaDAOConFotoBytesNull() {
        Paciente p = pacienteMinimo();
        servicio.insertar(p, "600111222", "600333444");

        verify(pacienteDAOMock).insertar(p, null);
        assertEquals("600111222", p.getTelefono1());
        assertEquals("600333444", p.getTelefono2());
    }

    @Test
    void insertar_telefonosNull_losConvierteAVacio() {
        Paciente p = pacienteMinimo();
        servicio.insertar(p, null, null);

        verify(pacienteDAOMock).insertar(p, null);
        assertEquals("", p.getTelefono1());
        assertEquals("", p.getTelefono2());
    }

    // ==================== insertar con foto ====================

    @Test
    void insertar_conArchivoFoto_pasaBytesAlDAO(@TempDir Path tmpDir) throws IOException {
        byte[] contenido = new byte[]{1, 2, 3, 4, 5};
        Path archivoPath = tmpDir.resolve("foto.png");
        Files.write(archivoPath, contenido);
        File archivo = archivoPath.toFile();

        Paciente p = pacienteMinimo();
        servicio.insertar(p, "600111222", "", archivo);

        verify(pacienteDAOMock).insertar(eq(p), eq(contenido));
    }

    @Test
    void insertar_archivoFotoNull_pasaNullAlDAO() {
        Paciente p = pacienteMinimo();
        servicio.insertar(p, "600111222", "", (File) null);

        verify(pacienteDAOMock).insertar(p, null);
    }

    @Test
    void insertar_archivoInexistente_lanzaValidacionException(@TempDir Path tmpDir) {
        File archivoInexistente = tmpDir.resolve("noexiste.png").toFile();
        Paciente p = pacienteMinimo();

        assertThrows(ValidacionException.class,
            () -> servicio.insertar(p, "600111222", "", archivoInexistente));
        verify(pacienteDAOMock, never()).insertar(any(), any());
    }

    // ==================== actualizar sin foto ====================

    @Test
    void actualizar_sinFoto_llamaDAOConFotoBytesNull() {
        Paciente p = pacienteMinimo();
        servicio.actualizar(p, "12345678A", "611222333", "");

        verify(pacienteDAOMock).actualizar(p, "12345678A", null);
        assertEquals("611222333", p.getTelefono1());
    }

    // ==================== actualizar con foto ====================

    @Test
    void actualizar_conArchivoFoto_pasaBytesAlDAO(@TempDir Path tmpDir) throws IOException {
        byte[] contenido = new byte[]{10, 20, 30};
        Path archivoPath = tmpDir.resolve("nueva.png");
        Files.write(archivoPath, contenido);

        Paciente p = pacienteMinimo();
        servicio.actualizar(p, "12345678A", "611222333", "", archivoPath.toFile());

        verify(pacienteDAOMock).actualizar(eq(p), eq("12345678A"), eq(contenido));
    }

    @Test
    void actualizar_archivoFotoNull_mantieneFotoExistente() {
        Paciente p = pacienteMinimo();
        servicio.actualizar(p, "12345678A", "611222333", "", (File) null);

        verify(pacienteDAOMock).actualizar(p, "12345678A", null);
    }

    // ==================== eliminar ====================

    @Test
    void eliminar_delegaAlDAO() {
        servicio.eliminar("12345678A");
        verify(pacienteDAOMock).eliminar("12345678A");
    }

    // ==================== existeDni ====================

    @Test
    void existeDni_delegaAlDAO() {
        when(pacienteDAOMock.existeDni("12345678A")).thenReturn(true);
        assertTrue(servicio.existeDni("12345678A"));
    }
}
