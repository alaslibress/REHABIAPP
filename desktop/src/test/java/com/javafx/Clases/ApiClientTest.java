package com.javafx.Clases;

import com.javafx.dto.LoginResponse;
import com.javafx.excepcion.AutenticacionException;
import com.javafx.excepcion.ConexionException;
import com.javafx.excepcion.DuplicadoException;
import com.javafx.excepcion.PermisoException;
import com.javafx.excepcion.ValidacionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para ApiClient.
 * Usa el constructor package-private para inyectar un HttpClient mock.
 * Verifica: gestion de sesion, mapeo de errores HTTP a excepciones del dominio,
 * y presencia del header Authorization en las peticiones.
 *
 * Se usa doReturn en lugar de when().thenReturn() para evitar conflictos de
 * tipos genericos con HttpClient.send() que devuelve HttpResponse<T>.
 */
@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
// LENIENT porque el helper mockResponse stub body() defensivamente; probarConexion() solo usa statusCode()
@MockitoSettings(strictness = Strictness.LENIENT)
class ApiClientTest {

    @Mock
    private HttpClient httpClientMock;

    private ApiClient apiClient;

    @BeforeEach
    void setUp() {
        apiClient = new ApiClient(httpClientMock, "http://localhost:8080", 5000L);
    }

    // Crea un HttpResponse mock que devuelve el statusCode y body indicados
    private HttpResponse<String> mockResponse(int statusCode, String body) {
        HttpResponse<String> r = mock(HttpResponse.class);
        when(r.statusCode()).thenReturn(statusCode);
        when(r.body()).thenReturn(body);
        return r;
    }

    // ==================== Estado de sesion ====================

    @Test
    void haySesionActiva_sinLogin_retornaFalse() {
        assertFalse(apiClient.haySesionActiva());
    }

    @Test
    void getAccessToken_sinLogin_retornaNull() {
        assertNull(apiClient.getAccessToken());
    }

    @Test
    void logout_sinTokenPrevio_noLanzaExcepcion() {
        assertDoesNotThrow(() -> apiClient.logout());
        assertFalse(apiClient.haySesionActiva());
    }

    // ==================== Login exitoso ====================

    @Test
    void login_respuesta200_almacenaTokensYRetornaResponse() throws Exception {
        String loginJson = "{\"accessToken\":\"abc123\",\"refreshToken\":\"ref456\",\"rol\":\"SPECIALIST\"}";
        doReturn(mockResponse(200, loginJson)).when(httpClientMock).send(any(HttpRequest.class), any());

        LoginResponse response = apiClient.login("S01", "clave");

        assertEquals("abc123", response.accessToken());
        assertEquals("ref456", response.refreshToken());
        assertEquals("SPECIALIST", response.rol());
        assertTrue(apiClient.haySesionActiva());
        assertEquals("abc123", apiClient.getAccessToken());
    }

    @Test
    void logout_despuesDeLogin_limpiaTokens() throws Exception {
        String loginJson = "{\"accessToken\":\"abc123\",\"refreshToken\":\"ref456\",\"rol\":\"SPECIALIST\"}";
        doReturn(mockResponse(200, loginJson)).when(httpClientMock).send(any(HttpRequest.class), any());
        apiClient.login("S01", "clave");

        apiClient.logout();

        assertFalse(apiClient.haySesionActiva());
        assertNull(apiClient.getAccessToken());
    }

    // ==================== Mapeo de errores HTTP a excepciones ====================

    @Test
    void login_respuesta401_lanzaAutenticacionException() throws Exception {
        String errorJson = "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Credenciales invalidas\",\"path\":\"/api/auth/login\"}";
        doReturn(mockResponse(401, errorJson)).when(httpClientMock).send(any(HttpRequest.class), any());

        assertThrows(AutenticacionException.class, () -> apiClient.login("S01", "mala"));
    }

    @Test
    void get_respuesta403_lanzaPermisoException() throws Exception {
        doReturn(mockResponse(403, "{\"message\":\"Sin permisos\"}"))
            .when(httpClientMock).send(any(HttpRequest.class), any());

        assertThrows(PermisoException.class, () -> apiClient.get("/api/recurso", String.class));
    }

    @Test
    void get_respuesta404_lanzaValidacionException() throws Exception {
        doReturn(mockResponse(404, "{\"message\":\"No encontrado\"}"))
            .when(httpClientMock).send(any(HttpRequest.class), any());

        assertThrows(ValidacionException.class, () -> apiClient.get("/api/pacientes/NOEXISTE", String.class));
    }

    @Test
    void post_respuesta400_lanzaValidacionException() throws Exception {
        doReturn(mockResponse(400, "{\"message\":\"Campo requerido\"}"))
            .when(httpClientMock).send(any(HttpRequest.class), any());

        assertThrows(ValidacionException.class, () -> apiClient.post("/api/pacientes", "{}", String.class));
    }

    @Test
    void post_respuesta409_lanzaDuplicadoException() throws Exception {
        doReturn(mockResponse(409, "{\"message\":\"DNI ya existe\"}"))
            .when(httpClientMock).send(any(HttpRequest.class), any());

        assertThrows(DuplicadoException.class, () -> apiClient.post("/api/pacientes", "{}", String.class));
    }

    @Test
    void get_respuesta500_lanzaConexionException() throws Exception {
        doReturn(mockResponse(500, "Internal Server Error"))
            .when(httpClientMock).send(any(HttpRequest.class), any());

        assertThrows(ConexionException.class, () -> apiClient.get("/api/recurso", String.class));
    }

    // ==================== Header Authorization ====================

    @Test
    void get_conTokenActivo_enviaHeaderAuthorization() throws Exception {
        String loginJson = "{\"accessToken\":\"miToken\",\"refreshToken\":\"ref\",\"rol\":\"SPECIALIST\"}";
        // Crear respuestas antes de la cadena doReturn para evitar conflictos de stubbing anidado
        HttpResponse<String> respLogin = mockResponse(200, loginJson);
        HttpResponse<String> respGet = mockResponse(200, "\"resultado\"");
        doReturn(respLogin).doReturn(respGet)
            .when(httpClientMock).send(any(HttpRequest.class), any());

        apiClient.login("S01", "clave");
        apiClient.get("/api/test", String.class);

        // Verificar que la segunda peticion (GET) lleva header Authorization: Bearer miToken
        verify(httpClientMock, atLeastOnce()).send(
            argThat(req -> req.headers().firstValue("Authorization")
                .orElse("").equals("Bearer miToken")),
            any()
        );
    }

    // ==================== Fallo de red ====================

    @Test
    void get_errorDeRed_lanzaConexionException() throws Exception {
        doThrow(new IOException("Conexion rechazada"))
            .when(httpClientMock).send(any(HttpRequest.class), any());

        assertThrows(ConexionException.class, () -> apiClient.get("/api/test", String.class));
    }

    // ==================== probarConexion ====================

    @Test
    void probarConexion_apiResponde200_retornaTrue() throws Exception {
        doReturn(mockResponse(200, "{\"status\":\"UP\"}"))
            .when(httpClientMock).send(any(HttpRequest.class), any());

        assertTrue(apiClient.probarConexion());
    }

    @Test
    void probarConexion_errorDeRed_retornaFalse() throws Exception {
        doThrow(new IOException("Sin conexion"))
            .when(httpClientMock).send(any(HttpRequest.class), any());

        assertFalse(apiClient.probarConexion());
    }

    @Test
    void probarConexion_apiResponde503_retornaFalse() throws Exception {
        doReturn(mockResponse(503, "Service Unavailable"))
            .when(httpClientMock).send(any(HttpRequest.class), any());

        assertFalse(apiClient.probarConexion());
    }
}
