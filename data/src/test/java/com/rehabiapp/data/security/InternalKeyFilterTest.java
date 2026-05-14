package com.rehabiapp.data.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para {@link InternalKeyFilter}.
 *
 * <p>Spring Boot 4 elimino los test slices web; los tests instancian el filtro
 * directamente con mocks de la API Servlet para mantener velocidad y aislamiento.</p>
 */
class InternalKeyFilterTest {

    private static final String CLAVE = "clave-de-prueba-32-chars-1234567";

    @Test
    void rutaProtegidaConClaveValidaPasaLaCadena() throws Exception {
        InternalKeyFilter filter = new InternalKeyFilter(CLAVE);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getRequestURI()).thenReturn("/ingest/game-session");
        when(req.getHeader("X-Internal-Key")).thenReturn(CLAVE);

        filter.doFilter(req, res, chain);

        verify(chain, times(1)).doFilter(req, res);
        verify(res, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void rutaProtegidaSinCabeceraRetorna401() throws Exception {
        InternalKeyFilter filter = new InternalKeyFilter(CLAVE);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        StringWriter writer = new StringWriter();

        when(req.getRequestURI()).thenReturn("/internal/patient/12345678Z/check-new-data");
        when(req.getHeader("X-Internal-Key")).thenReturn(null);
        when(res.getWriter()).thenReturn(new PrintWriter(writer));

        filter.doFilter(req, res, chain);

        verify(res).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(chain, never()).doFilter(any(), any());
        assertThat(writer.toString()).contains("X-Internal-Key");
    }

    @Test
    void rutaProtegidaConClaveIncorrectaRetorna401() throws Exception {
        InternalKeyFilter filter = new InternalKeyFilter(CLAVE);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(res.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        when(req.getRequestURI()).thenReturn("/ingest/game-session");
        when(req.getHeader("X-Internal-Key")).thenReturn("clave-distinta");

        filter.doFilter(req, res, chain);

        verify(res).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void rutaPublicaActuatorSaltaElFiltro() throws Exception {
        InternalKeyFilter filter = new InternalKeyFilter(CLAVE);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getRequestURI()).thenReturn("/actuator/health/readiness");

        filter.doFilter(req, res, chain);

        // Salta el filtro: la cadena se invoca aunque no haya cabecera.
        verify(chain, times(1)).doFilter(req, res);
        verify(res, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void rutaAnalyticsPublicaSaltaElFiltro() throws Exception {
        InternalKeyFilter filter = new InternalKeyFilter(CLAVE);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getRequestURI()).thenReturn("/analytics/patient/12345678Z/treatment-progress");

        filter.doFilter(req, res, chain);

        // /analytics/** no esta en el listado protegido — la cadena pasa.
        verify(chain, times(1)).doFilter(req, res);
    }
}
