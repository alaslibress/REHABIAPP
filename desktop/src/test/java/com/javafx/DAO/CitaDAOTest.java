package com.javafx.DAO;

import com.fasterxml.jackson.core.type.TypeReference;
import com.javafx.Clases.ApiClient;
import com.javafx.Clases.Cita;
import com.javafx.DAO.CitaDAO;
import com.javafx.dto.CitaResponse;
import com.javafx.dto.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para CitaDAO.
 * Usa el constructor package-private para inyectar un ApiClient mock.
 */
@ExtendWith(MockitoExtension.class)
class CitaDAOTest {

    @Mock
    private ApiClient apiClientMock;

    private CitaDAO dao;

    @BeforeEach
    void setUp() {
        dao = new CitaDAO(apiClientMock);
    }

    private CitaResponse response(String dniPac, String dniSan, LocalDate fecha, LocalTime hora) {
        return new CitaResponse(dniPac, dniSan, fecha, hora, "Juan Garcia", "Dra. Maria");
    }

    private PageResponse<CitaResponse> pagina(CitaResponse... citas) {
        List<CitaResponse> lista = List.of(citas);
        return new PageResponse<>(lista, 0, lista.size(), lista.size(), 1, true);
    }

    // ==================== listarPorFecha ====================

    @Test
    void listarPorFecha_rutaContieneFecha() {
        LocalDate fecha = LocalDate.of(2026, 4, 10);
        when(apiClientMock.get(contains("fecha=" + fecha), any(TypeReference.class)))
            .thenReturn(pagina(response("P01", "S01", fecha, LocalTime.of(9, 0))));

        List<Cita> resultado = dao.listarPorFecha(fecha);

        assertEquals(1, resultado.size());
        assertEquals("P01", resultado.get(0).getDniPaciente());
        assertEquals(fecha, resultado.get(0).getFecha());
    }

    // ==================== listarPorSanitario ====================

    @Test
    void listarPorSanitario_rutaContieneDniSanitario() {
        LocalDate fecha = LocalDate.of(2026, 4, 10);
        when(apiClientMock.get(contains("/api/citas/sanitario/S01"), any(TypeReference.class)))
            .thenReturn(pagina(
                response("P01", "S01", fecha, LocalTime.of(9, 0)),
                response("P02", "S01", fecha, LocalTime.of(10, 0))
            ));

        List<Cita> resultado = dao.listarPorSanitario("S01");

        assertEquals(2, resultado.size());
    }

    // ==================== obtenerCitasPorSanitarioYFecha ====================

    @Test
    void obtenerCitasPorSanitarioYFecha_soloRetornaLasDeEsaFecha() {
        LocalDate fechaBuscada = LocalDate.of(2026, 4, 10);
        LocalDate otraFecha = LocalDate.of(2026, 4, 11);

        when(apiClientMock.get(contains("/api/citas/sanitario/S01"), any(TypeReference.class)))
            .thenReturn(pagina(
                response("P01", "S01", fechaBuscada, LocalTime.of(9, 0)),
                response("P02", "S01", otraFecha, LocalTime.of(10, 0))
            ));

        List<Cita> resultado = dao.obtenerCitasPorSanitarioYFecha("S01", fechaBuscada);

        assertEquals(1, resultado.size());
        assertEquals("P01", resultado.get(0).getDniPaciente());
    }

    // ==================== existeCitaEnHorario ====================

    @Test
    void existeCitaEnHorario_conflictoExiste_retornaTrue() {
        LocalDate fecha = LocalDate.of(2026, 4, 10);
        LocalTime hora = LocalTime.of(9, 0);

        when(apiClientMock.get(contains("/api/citas/sanitario/S01"), any(TypeReference.class)))
            .thenReturn(pagina(response("P01", "S01", fecha, hora)));

        assertTrue(dao.existeCitaEnHorario("S01", fecha, hora));
    }

    @Test
    void existeCitaEnHorario_sinConflicto_retornaFalse() {
        LocalDate fecha = LocalDate.of(2026, 4, 10);

        when(apiClientMock.get(contains("/api/citas/sanitario/S01"), any(TypeReference.class)))
            .thenReturn(pagina(response("P01", "S01", fecha, LocalTime.of(9, 0))));

        assertFalse(dao.existeCitaEnHorario("S01", fecha, LocalTime.of(10, 0)));
    }

    // ==================== insertar ====================

    @Test
    void insertar_llamaPostConRutaCorrecta() {
        when(apiClientMock.post(eq("/api/citas"), any(), eq(CitaResponse.class)))
            .thenReturn(response("P01", "S01", LocalDate.of(2026, 4, 10), LocalTime.of(9, 0)));

        dao.insertar(new Cita("P01", "S01", LocalDate.of(2026, 4, 10), LocalTime.of(9, 0)));

        verify(apiClientMock).post(eq("/api/citas"), any(), eq(CitaResponse.class));
    }

    // ==================== eliminar ====================

    @Test
    void eliminar_rutaContieneLosCuatroParametros() {
        LocalDate fecha = LocalDate.of(2026, 4, 10);
        LocalTime hora = LocalTime.of(9, 0);

        dao.eliminar("P01", "S01", fecha, hora);

        verify(apiClientMock).delete(argThat(ruta ->
            ruta.contains("dniPac=P01")
            && ruta.contains("dniSan=S01")
            && ruta.contains("fecha=" + fecha)
            && ruta.contains("hora=" + hora)
        ));
    }

    // ==================== actualizar ====================

    @Test
    void actualizar_eliminaYReinserta() {
        LocalDate fecha = LocalDate.of(2026, 4, 10);
        LocalTime horaVieja = LocalTime.of(9, 0);
        LocalTime horaNueva = LocalTime.of(11, 0);

        when(apiClientMock.post(eq("/api/citas"), any(), eq(CitaResponse.class)))
            .thenReturn(response("P01", "S01", fecha, horaNueva));

        dao.actualizar("P01", "S01", fecha, horaVieja, new Cita("P01", "S01", fecha, horaNueva));

        verify(apiClientMock).delete(argThat(r -> r.contains("hora=" + horaVieja)));
        verify(apiClientMock).post(eq("/api/citas"), any(), eq(CitaResponse.class));
    }

    // ==================== Conversion nombres desde CitaResponse ====================

    @Test
    void listarPorFecha_responseConNombres_nombresPresentes() {
        LocalDate fecha = LocalDate.of(2026, 4, 10);
        when(apiClientMock.get(anyString(), any(TypeReference.class)))
            .thenReturn(pagina(response("P01", "S01", fecha, LocalTime.of(9, 0))));

        List<Cita> resultado = dao.listarPorFecha(fecha);

        assertEquals("Juan Garcia", resultado.get(0).getNombrePaciente());
        assertEquals("Dra. Maria", resultado.get(0).getNombreSanitario());
    }

    @Test
    void listarPorFecha_responseConNombresNull_nombreVacio() {
        LocalDate fecha = LocalDate.of(2026, 4, 10);
        CitaResponse responseNullNombres = new CitaResponse("P01", "S01", fecha, LocalTime.of(9, 0), null, null);
        when(apiClientMock.get(anyString(), any(TypeReference.class)))
            .thenReturn(pagina(responseNullNombres));

        List<Cita> resultado = dao.listarPorFecha(fecha);

        assertEquals("", resultado.get(0).getNombrePaciente());
        assertEquals("", resultado.get(0).getNombreSanitario());
    }
}
