package com.javafx.DAO;

import com.fasterxml.jackson.core.type.TypeReference;
import com.javafx.Clases.ApiClient;
import com.javafx.Clases.Videojuego;
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
    void listarTodos_devuelveListaCompleta() {
        Videojuego v1 = new Videojuego(1L, "VJ01", "Juego 1", "d1", "DIS01", "Dis 1", "Brazo", "u1", true);
        Videojuego v2 = new Videojuego(2L, "VJ02", "Juego 2", "d2", "DIS02", "Dis 2", "Pierna", "u2", true);
        when(apiMock.get(eq("/api/videojuegos"), any(TypeReference.class)))
            .thenReturn(List.of(v1, v2));

        List<Videojuego> resultado = dao.listarTodos();

        assertEquals(2, resultado.size());
    }
}
