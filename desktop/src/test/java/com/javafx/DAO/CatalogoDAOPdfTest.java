package com.javafx.DAO;

import com.javafx.Clases.ApiClient;
import com.javafx.Clases.PdfMetadato;
import com.javafx.excepcion.ValidacionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogoDAOPdfTest {

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
    void subirPdfTratamiento_invocaUploadFile_conRutaCorrecta() {
        byte[] pdf = new byte[]{0x25, 0x50, 0x44, 0x46}; // %PDF
        when(apiMock.uploadFile(anyString(), anyString(), any(), anyString(), anyString(), any()))
            .thenReturn(null);

        dao.subirPdfTratamiento("T01", pdf, "documento.pdf");

        verify(apiMock).uploadFile(
            eq("/api/tratamientos/T01/pdf"),
            eq("file"),
            eq(pdf),
            eq("documento.pdf"),
            eq("application/pdf"),
            eq(Void.class)
        );
    }

    @Test
    void consultarMetadatosPdf_devuelveNull_cuando404() {
        when(apiMock.get(anyString(), eq(PdfMetadato.class)))
            .thenThrow(new ValidacionException("No encontrado", "entidad"));

        PdfMetadato resultado = dao.consultarMetadatosPdf("T99");

        assertNull(resultado);
    }

    @Test
    void descargarPdfTratamiento_devuelveBytes() {
        byte[] esperado = new byte[]{1, 2, 3};
        when(apiMock.getBytes("/api/tratamientos/T01/pdf")).thenReturn(esperado);

        byte[] resultado = dao.descargarPdfTratamiento("T01");

        assertArrayEquals(esperado, resultado);
    }
}
