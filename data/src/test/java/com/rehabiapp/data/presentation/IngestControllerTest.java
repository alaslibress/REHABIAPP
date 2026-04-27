package com.rehabiapp.data.presentation;

import com.rehabiapp.data.application.service.DuplicateSessionException;
import com.rehabiapp.data.application.service.GameSessionIngestService;
import com.rehabiapp.data.application.service.dto.GameSessionIngestRequest;
import com.rehabiapp.data.application.service.dto.GameSessionIngestResponse;
import com.rehabiapp.data.application.service.dto.MovementMetricsDto;
import com.rehabiapp.data.infrastructure.config.InternalAuthFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios del controller y del filtro de autenticacion interna.
 * Instanciacion directa (sin contexto Spring) siguiendo el patron del proyecto.
 */
class IngestControllerTest {

    private final GameSessionIngestService service = mock(GameSessionIngestService.class);
    private final IngestController controller = new IngestController(service);
    private final InternalAuthFilter filter = new InternalAuthFilter("test-key");

    // --- Controller tests ---

    @Test
    void validRequestReturns201() {
        GameSessionIngestResponse resp = new GameSessionIngestResponse(
                "id1", "sess-001", Instant.now(), "CREATED");
        when(service.ingest(any())).thenReturn(resp);

        ResponseEntity<GameSessionIngestResponse> result =
                controller.ingestGameSession(buildValidRequest("sess-001"));

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().sessionId()).isEqualTo("sess-001");
        assertThat(result.getBody().status()).isEqualTo("CREATED");
    }

    @Test
    void duplicateSessionReturns409ViaHandler() {
        when(service.ingest(any())).thenThrow(new DuplicateSessionException("sess-dup"));

        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<?> result =
                handler.handleDuplicate(new DuplicateSessionException("sess-dup"));

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(result.getBody().toString()).contains("duplicate_session");
    }

    // --- Filter tests ---

    @Test
    void missingKeyReturns401() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/ingest/game-session");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        Mockito.verifyNoInteractions(chain);
    }

    @Test
    void correctKeyPassesFilter() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/ingest/game-session");
        req.addHeader("X-Internal-Key", "test-key");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(200);
        Mockito.verify(chain).doFilter(req, res);
    }

    @Test
    void wrongKeyReturns401() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/ingest/game-session");
        req.addHeader("X-Internal-Key", "wrong-key");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        Mockito.verifyNoInteractions(chain);
    }

    @Test
    void healthPathSkipsFilter() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/health");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        // /health no empieza por /ingest => filtro no actua => chain se invoca
        filter.doFilter(req, res, chain);

        Mockito.verify(chain).doFilter(req, res);
    }

    private GameSessionIngestRequest buildValidRequest(String sessionId) {
        return new GameSessionIngestRequest(
                sessionId,
                "12345678Z",
                "reach-and-grab",
                2,
                Instant.parse("2026-04-24T10:00:00Z"),
                Instant.parse("2026-04-24T10:05:00Z"),
                300, 820, 18, 20,
                new MovementMetricsDto(92.5, 0.42, 0.88),
                true,
                "M25.5"
        );
    }
}
