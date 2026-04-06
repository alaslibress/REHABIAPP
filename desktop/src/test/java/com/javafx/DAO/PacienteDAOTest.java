package com.javafx.DAO;

import com.fasterxml.jackson.core.type.TypeReference;
import com.javafx.Clases.ApiClient;
import com.javafx.Clases.Paciente;
import com.javafx.DAO.PacienteDAO;
import com.javafx.dto.PageResponse;
import com.javafx.dto.PacienteResponse;
import com.javafx.excepcion.ValidacionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para PacienteDAO.
 * Usa el constructor package-private para inyectar un ApiClient mock.
 */
@ExtendWith(MockitoExtension.class)
class PacienteDAOTest {

    @Mock
    private ApiClient apiClientMock;

    private PacienteDAO dao;

    @BeforeEach
    void setUp() {
        dao = new PacienteDAO(apiClientMock);
    }

    private PacienteResponse responseMinimo(String dni) {
        return new PacienteResponse(
            dni, "S01", "Juan", "Garcia", "Lopez",
            30, "juan@test.com", "SS001",
            "M", null, false, true,
            null, null, null, false, List.of()
        );
    }

    // ==================== listarTodos ====================

    @Test
    void listarTodos_apiDevuelvePagina_retornaListaConvertida() {
        PageResponse<PacienteResponse> pagina = new PageResponse<>(
            List.of(responseMinimo("12345678A"), responseMinimo("87654321B")),
            0, 2, 2L, 1, true
        );
        when(apiClientMock.get(contains("/api/pacientes"), any(TypeReference.class)))
            .thenReturn(pagina);

        List<Paciente> resultado = dao.listarTodos();

        assertEquals(2, resultado.size());
        assertEquals("12345678A", resultado.get(0).getDni());
        assertEquals("87654321B", resultado.get(1).getDni());
    }

    @Test
    void listarTodos_paginaVacia_retornaListaVacia() {
        PageResponse<PacienteResponse> pagina = new PageResponse<>(
            List.of(), 0, 0, 0L, 0, true
        );
        when(apiClientMock.get(anyString(), any(TypeReference.class))).thenReturn(pagina);

        assertTrue(dao.listarTodos().isEmpty());
    }

    // ==================== buscarPorDni ====================

    @Test
    void buscarPorDni_encontrado_retornaPaciente() {
        when(apiClientMock.get("/api/pacientes/12345678A", PacienteResponse.class))
            .thenReturn(responseMinimo("12345678A"));

        Paciente p = dao.buscarPorDni("12345678A");

        assertEquals("12345678A", p.getDni());
        assertEquals("Juan", p.getNombre());
    }

    @Test
    void buscarPorDni_noEncontrado_propagaExcepcion() {
        when(apiClientMock.get("/api/pacientes/NOEXISTE", PacienteResponse.class))
            .thenThrow(new ValidacionException("No encontrado", "entidad"));

        assertThrows(ValidacionException.class, () -> dao.buscarPorDni("NOEXISTE"));
    }

    // ==================== obtenerPorDNI (alias) ====================

    @Test
    void obtenerPorDNI_delegaABuscarPorDni() {
        when(apiClientMock.get("/api/pacientes/12345678A", PacienteResponse.class))
            .thenReturn(responseMinimo("12345678A"));

        Paciente p = dao.obtenerPorDNI("12345678A");
        assertEquals("12345678A", p.getDni());
    }

    // ==================== existeDni ====================

    @Test
    void existeDni_pacienteExiste_retornaTrue() {
        when(apiClientMock.get("/api/pacientes/12345678A", PacienteResponse.class))
            .thenReturn(responseMinimo("12345678A"));

        assertTrue(dao.existeDni("12345678A"));
    }

    @Test
    void existeDni_pacienteNoExiste_retornaFalse() {
        when(apiClientMock.get("/api/pacientes/NOEXISTE", PacienteResponse.class))
            .thenThrow(new ValidacionException("No encontrado", "entidad"));

        assertFalse(dao.existeDni("NOEXISTE"));
    }

    // ==================== insertar ====================

    @Test
    void insertar_llamaPostConRutaCorrecta() {
        when(apiClientMock.post(eq("/api/pacientes"), any(), eq(PacienteResponse.class)))
            .thenReturn(responseMinimo("12345678A"));

        Paciente p = new Paciente("12345678A", "Juan", "Garcia", "Lopez",
            30, "juan@test.com", "SS001", false, "S01");
        dao.insertar(p);

        verify(apiClientMock).post(eq("/api/pacientes"), any(), eq(PacienteResponse.class));
    }

    // ==================== actualizar ====================

    @Test
    void actualizar_llamaPutConDniOriginalEnRuta() {
        when(apiClientMock.put(eq("/api/pacientes/12345678A"), any(), eq(PacienteResponse.class)))
            .thenReturn(responseMinimo("12345678A"));

        Paciente p = new Paciente("12345678A", "Juan", "Garcia", "Lopez",
            30, "juan@test.com", "SS001", false, "S01");
        dao.actualizar(p, "12345678A");

        verify(apiClientMock).put(eq("/api/pacientes/12345678A"), any(), eq(PacienteResponse.class));
    }

    @Test
    void actualizar_dniCambiado_rutaUsaDniOriginal() {
        when(apiClientMock.put(eq("/api/pacientes/ORIGINAL"), any(), eq(PacienteResponse.class)))
            .thenReturn(responseMinimo("NUEVODNI"));

        Paciente p = new Paciente("NUEVODNI", "Juan", "Garcia", "Lopez",
            30, "juan@test.com", "SS001", false, "S01");
        dao.actualizar(p, "ORIGINAL");

        verify(apiClientMock).put(eq("/api/pacientes/ORIGINAL"), any(), eq(PacienteResponse.class));
    }

    // ==================== eliminar ====================

    @Test
    void eliminar_llamaDeleteConDniEnRuta() {
        dao.eliminar("12345678A");
        verify(apiClientMock).delete("/api/pacientes/12345678A");
    }

    // ==================== buscarPorTexto ====================

    @Test
    void buscarPorTexto_rutaContieneBuscarYTexto() {
        PageResponse<PacienteResponse> pagina = new PageResponse<>(
            List.of(), 0, 0, 0L, 0, true
        );
        when(apiClientMock.get(argThat(r -> r.contains("buscar") && r.contains("Juan")),
            any(TypeReference.class))).thenReturn(pagina);

        dao.buscarPorTexto("Juan");

        verify(apiClientMock).get(
            argThat(r -> r.contains("buscar") && r.contains("Juan")),
            any(TypeReference.class)
        );
    }
}
