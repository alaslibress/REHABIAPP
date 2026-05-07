package com.javafx.Clases;

import com.javafx.excepcion.ConexionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApiClientFileTest {

    @Mock
    private HttpClient httpClientMock;

    private ApiClient apiClient;

    @BeforeEach
    void setUp() {
        apiClient = new ApiClient(httpClientMock, "http://localhost:8080", 5000L);
    }

    private HttpResponse<byte[]> mockBytesResponse(int statusCode, byte[] body) {
        HttpResponse<byte[]> r = mock(HttpResponse.class);
        when(r.statusCode()).thenReturn(statusCode);
        when(r.body()).thenReturn(body);
        return r;
    }

    private HttpResponse<String> mockStringResponse(int statusCode, String body) {
        HttpResponse<String> r = mock(HttpResponse.class);
        when(r.statusCode()).thenReturn(statusCode);
        when(r.body()).thenReturn(body);
        return r;
    }

    @Test
    void getBytes_devuelveCuerpoBinario_cuando200() throws Exception {
        byte[] contenido = "contenido binario".getBytes(StandardCharsets.UTF_8);
        doReturn(mockBytesResponse(200, contenido))
            .when(httpClientMock).send(any(HttpRequest.class), any());

        byte[] resultado = apiClient.getBytes("/api/tratamientos/T01/pdf");

        assertArrayEquals(contenido, resultado);
    }

    @Test
    void getBytes_lanzaConexion_cuando500() throws Exception {
        doReturn(mockBytesResponse(500, "error interno".getBytes(StandardCharsets.UTF_8)))
            .when(httpClientMock).send(any(HttpRequest.class), any());

        assertThrows(ConexionException.class, () -> apiClient.getBytes("/api/tratamientos/T01/pdf"));
    }

    @Test
    void uploadFile_envia_multipart_con_unica_parte_file() throws Exception {
        doReturn(mockStringResponse(200, ""))
            .when(httpClientMock).send(any(HttpRequest.class), any());

        byte[] pdf = "%PDF-contenido".getBytes(StandardCharsets.UTF_8);
        Void resultado = apiClient.uploadFile(
            "/api/tratamientos/T01/pdf", "file", pdf, "trat.pdf", "application/pdf", Void.class);

        assertNull(resultado);
        verify(httpClientMock).send(
            argThat(req ->
                req.headers().firstValue("Content-Type")
                    .map(ct -> ct.startsWith("multipart/form-data"))
                    .orElse(false)
            ),
            any()
        );
    }
}
