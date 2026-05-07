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
class CatalogoDAOAsociacionTest {

    private ApiClient apiMock;
    private MockedStatic<ApiClient> apiClientStatic;
    private CatalogoDAO dao;

    @BeforeEach
    void setUp() {
        apiMock = Mockito.mock(ApiClient.class);
        apiClientStatic = Mockito.mockStatic(ApiClient.class);
        apiClientStatic.when(ApiClient::getInstancia).thenReturn(apiMock);
        dao = new CatalogoDAO();
    }

    @AfterEach
    void tearDown() {
        apiClientStatic.close();
    }

    @Test
    @SuppressWarnings("unchecked")
    void listarJuegosDeTratamiento_devuelveLista() {
        Videojuego v = new Videojuego(1L, "VJ01", "Juego 1", "desc", "DIS01",
            "Discapacidad 1", "Brazo", "https://unity.test", true);
        when(apiMock.get(eq("/api/catalogo/tratamientos/T01/videojuegos"), any(TypeReference.class)))
            .thenReturn(List.of(v));

        List<Videojuego> resultado = dao.listarJuegosDeTratamiento("T01");

        assertEquals(1, resultado.size());
        assertEquals("VJ01", resultado.get(0).codigo());
    }

    @Test
    void vincularJuego_invocaPostConIdsCorrectos() {
        when(apiMock.post(anyString(), any(), any())).thenReturn(null);

        dao.vincularJuego("T01", 5L);

        verify(apiMock).post(
            eq("/api/catalogo/tratamientos/T01/videojuegos/5"),
            isNull(),
            eq(Void.class)
        );
    }

    @Test
    void desvincularJuego_invocaDelete() {
        doNothing().when(apiMock).delete(anyString());

        dao.desvincularJuego("T01", 5L);

        verify(apiMock).delete("/api/catalogo/tratamientos/T01/videojuegos/5");
    }
}
