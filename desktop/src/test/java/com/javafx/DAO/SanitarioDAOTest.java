package com.javafx.DAO;

import com.fasterxml.jackson.core.type.TypeReference;
import com.javafx.Clases.ApiClient;
import com.javafx.Clases.Sanitario;
import com.javafx.DAO.SanitarioDAO;
import com.javafx.dto.LoginResponse;
import com.javafx.dto.PageResponse;
import com.javafx.dto.SanitarioResponse;
import com.javafx.excepcion.AutenticacionException;
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
 * Tests unitarios para SanitarioDAO.
 * Usa el constructor package-private para inyectar un ApiClient mock.
 */
@ExtendWith(MockitoExtension.class)
class SanitarioDAOTest {

    @Mock
    private ApiClient apiClientMock;

    private SanitarioDAO dao;

    @BeforeEach
    void setUp() {
        dao = new SanitarioDAO(apiClientMock);
    }

    private SanitarioResponse responseMinimo(String dni) {
        return new SanitarioResponse(
            dni, "Maria", "Lopez", "Ruiz",
            "maria@clinic.com", 5,
            true, "SPECIALIST",
            List.of()
        );
    }

    // ==================== listarTodos ====================

    @Test
    void listarTodos_apiDevuelvePagina_retornaListaConvertida() {
        PageResponse<SanitarioResponse> pagina = new PageResponse<>(
            List.of(responseMinimo("S01"), responseMinimo("S02")),
            0, 2, 2L, 1, true
        );
        when(apiClientMock.get(contains("/api/sanitarios"), any(TypeReference.class)))
            .thenReturn(pagina);

        List<Sanitario> resultado = dao.listarTodos();

        assertEquals(2, resultado.size());
        assertEquals("S01", resultado.get(0).getDni());
    }

    // ==================== buscarPorDni ====================

    @Test
    void buscarPorDni_encontrado_retornaSanitario() {
        when(apiClientMock.get("/api/sanitarios/S01", SanitarioResponse.class))
            .thenReturn(responseMinimo("S01"));

        Sanitario s = dao.buscarPorDni("S01");

        assertEquals("S01", s.getDni());
        assertEquals("Maria", s.getNombre());
    }

    @Test
    void buscarPorDni_noEncontrado_propagaExcepcion() {
        when(apiClientMock.get("/api/sanitarios/NOEXISTE", SanitarioResponse.class))
            .thenThrow(new ValidacionException("No encontrado", "entidad"));

        assertThrows(ValidacionException.class, () -> dao.buscarPorDni("NOEXISTE"));
    }

    // ==================== existeDni ====================

    @Test
    void existeDni_sanitarioExiste_retornaTrue() {
        when(apiClientMock.get("/api/sanitarios/S01", SanitarioResponse.class))
            .thenReturn(responseMinimo("S01"));

        assertTrue(dao.existeDni("S01"));
    }

    @Test
    void existeDni_sanitarioNoExiste_retornaFalse() {
        when(apiClientMock.get("/api/sanitarios/NOEXISTE", SanitarioResponse.class))
            .thenThrow(new ValidacionException("No encontrado", "entidad"));

        assertFalse(dao.existeDni("NOEXISTE"));
    }

    // ==================== autenticar ====================

    @Test
    void autenticar_credencialesValidas_retornaLoginResponse() {
        LoginResponse loginResponse = new LoginResponse("access123", "refresh456", "SPECIALIST");
        when(apiClientMock.login("S01", "clave123")).thenReturn(loginResponse);

        LoginResponse resultado = dao.autenticar("S01", "clave123");

        assertEquals("access123", resultado.accessToken());
        assertEquals("SPECIALIST", resultado.rol());
    }

    @Test
    void autenticar_credencialesInvalidas_propagaAutenticacionException() {
        when(apiClientMock.login("S01", "mala"))
            .thenThrow(new AutenticacionException("Credenciales invalidas"));

        assertThrows(AutenticacionException.class, () -> dao.autenticar("S01", "mala"));
    }

    // ==================== verificarContrasena ====================

    @Test
    void verificarContrasena_claveCorrecta_retornaTrue() {
        when(apiClientMock.login("S01", "clave"))
            .thenReturn(new LoginResponse("token", "refresh", "SPECIALIST"));

        assertTrue(dao.verificarContrasena("S01", "clave"));
    }

    @Test
    void verificarContrasena_claveIncorrecta_retornaFalse() {
        when(apiClientMock.login("S01", "mala"))
            .thenThrow(new AutenticacionException("Credenciales invalidas"));

        assertFalse(dao.verificarContrasena("S01", "mala"));
    }

    // ==================== insertar ====================

    @Test
    void insertar_llamaPostConRutaCorrecta() {
        when(apiClientMock.post(eq("/api/sanitarios"), any(), eq(SanitarioResponse.class)))
            .thenReturn(responseMinimo("S01"));

        Sanitario s = new Sanitario("S01", "Maria", "Lopez", "Ruiz", "m@c.com", "SPECIALIST");
        dao.insertar(s, "secreto");

        verify(apiClientMock).post(eq("/api/sanitarios"), any(), eq(SanitarioResponse.class));
    }

    // ==================== actualizar ====================

    @Test
    void actualizar_llamaPutConDniOriginalEnRuta() {
        when(apiClientMock.put(eq("/api/sanitarios/S01"), any(), eq(SanitarioResponse.class)))
            .thenReturn(responseMinimo("S01"));

        Sanitario s = new Sanitario("S01", "Maria", "Lopez", "Ruiz", "m@c.com", "SPECIALIST");
        dao.actualizar(s, "S01");

        verify(apiClientMock).put(eq("/api/sanitarios/S01"), any(), eq(SanitarioResponse.class));
    }

    // ==================== eliminar ====================

    @Test
    void eliminar_llamaDeleteConDniEnRuta() {
        dao.eliminar("S01");
        verify(apiClientMock).delete("/api/sanitarios/S01");
    }

    // ==================== existeAdmin ====================

    @Test
    void existeAdmin_adminExiste_retornaTrue() {
        when(apiClientMock.get("/api/sanitarios/ADMIN0000", SanitarioResponse.class))
            .thenReturn(responseMinimo("ADMIN0000"));

        assertTrue(dao.existeAdmin());
    }

    @Test
    void existeAdmin_adminNoExiste_retornaFalse() {
        when(apiClientMock.get("/api/sanitarios/ADMIN0000", SanitarioResponse.class))
            .thenThrow(new ValidacionException("No encontrado", "entidad"));

        assertFalse(dao.existeAdmin());
    }
}
