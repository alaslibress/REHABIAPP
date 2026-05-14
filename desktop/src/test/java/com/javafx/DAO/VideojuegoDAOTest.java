package com.javafx.DAO;

import com.fasterxml.jackson.core.type.TypeReference;
import com.javafx.Clases.ApiClient;
import com.javafx.Clases.Videojuego;
import com.javafx.Clases.VideojuegoRequest;
import com.javafx.dto.PageResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideojuegoDAOTest {

    private ApiClient apiMock;
    private MockedStatic<ApiClient> apiClientStatic;
    private VideojuegoDAO dao;

    @BeforeEach
    void setUp() {
        apiMock = Mockito.mock(ApiClient.class);
        apiClientStatic = Mockito.mockStatic(ApiClient.class);
        apiClientStatic.when(ApiClient::getInstancia).thenReturn(apiMock);
        dao = new VideojuegoDAO();
    }

    @AfterEach
    void tearDown() {
        apiClientStatic.close();
    }

    @Test
    @SuppressWarnings("unchecked")
    void listarPorDiscapacidad_devuelveLista() {
        Videojuego v = new Videojuego(1L, "VJ01", "Juego rehabilitacion", "desc",
                "DIS01", "Hombro", "Brazo", "https://unity.test", true);
        when(apiMock.get(eq("/api/videojuegos/discapacidad/DIS01"), any(TypeReference.class)))
                .thenReturn(List.of(v));

        List<Videojuego> resultado = dao.listarPorDiscapacidad("DIS01");

        assertEquals(1, resultado.size());
        assertEquals("VJ01", resultado.get(0).codigo());
    }

    @Test
    @SuppressWarnings("unchecked")
    void listarTodos_deserializaPageResponseYDevuelveContenido() {
        Videojuego v1 = new Videojuego(1L, "VJ01", "Juego 1", "d1", "DIS01", "Dis 1", "Brazo", "u1", true);
        Videojuego v2 = new Videojuego(2L, "VJ02", "Juego 2", "d2", "DIS02", "Dis 2", "Pierna", "u2", true);
        PageResponse<Videojuego> pagina = new PageResponse<>(List.of(v1, v2), 0, 200, 2, 1, true);
        when(apiMock.get(eq("/api/videojuegos?size=200"), any(TypeReference.class))).thenReturn(pagina);

        List<Videojuego> resultado = dao.listarTodos();

        assertEquals(2, resultado.size());
        assertEquals("VJ01", resultado.get(0).codigo());
    }

    @Test
    void listarTodos_conPageResponseNull_devuelveListaVacia() {
        when(apiMock.get(eq("/api/videojuegos?size=200"), any(TypeReference.class))).thenReturn(null);

        List<Videojuego> resultado = dao.listarTodos();

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    @Test
    void obtenerPorId_llamaAlPathCorrecto() {
        Videojuego esperado = new Videojuego(42L, "VJ42", "Test", null, "DIS01", "Dis", "Brazo", null, true);
        when(apiMock.get(eq("/api/videojuegos/42"), eq(Videojuego.class))).thenReturn(esperado);

        Videojuego resultado = dao.obtenerPorId(42L);

        assertEquals(42L, resultado.idVideojuego());
        verify(apiMock).get("/api/videojuegos/42", Videojuego.class);
    }

    @Test
    void crear_postEnviaRequestYDeserializaVideojuego() {
        VideojuegoRequest req = new VideojuegoRequest("VJ-NEW", "Nuevo", "desc", "DIS01", "Mano", null);
        Videojuego esperado = new Videojuego(5L, "VJ-NEW", "Nuevo", "desc", "DIS01", "Dis 1", "Mano", null, true);
        when(apiMock.post(eq("/api/videojuegos"), eq(req), eq(Videojuego.class))).thenReturn(esperado);

        Videojuego resultado = dao.crear(req);

        assertEquals("VJ-NEW", resultado.codigo());
        verify(apiMock).post("/api/videojuegos", req, Videojuego.class);
    }

    @Test
    void actualizar_putAlPathCorrecto() {
        VideojuegoRequest req = new VideojuegoRequest(null, "Nombre actualizado", null, "REAL", "Mano", null);
        Videojuego esperado = new Videojuego(7L, "VJ-07", "Nombre actualizado", null, "REAL", "Real Dis", "Mano", null, true);
        when(apiMock.put(eq("/api/videojuegos/7"), eq(req), eq(Videojuego.class))).thenReturn(esperado);

        Videojuego resultado = dao.actualizar(7L, req);

        assertEquals("REAL", resultado.codDis());
        verify(apiMock).put("/api/videojuegos/7", req, Videojuego.class);
    }

    @Test
    void eliminar_deleteAlPathCorrecto() {
        doNothing().when(apiMock).delete(anyString());

        dao.eliminar(99L);

        verify(apiMock).delete("/api/videojuegos/99");
    }
}
