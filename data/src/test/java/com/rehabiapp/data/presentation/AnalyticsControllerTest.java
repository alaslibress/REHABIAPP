package com.rehabiapp.data.presentation;

import com.rehabiapp.data.application.service.CohortComparisonService;
import com.rehabiapp.data.application.service.RomTimeSeriesService;
import com.rehabiapp.data.application.service.dto.PatientAnalyticsResponse;
import com.rehabiapp.data.domain.model.PatientProgress;
import com.rehabiapp.data.domain.repository.PatientProgressRepository;
import com.rehabiapp.data.infrastructure.config.InternalAuthFilter;
import jakarta.servlet.FilterChain;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para AnalyticsController. Instanciacion directa — SB4 elimino @WebMvcTest.
 */
class AnalyticsControllerTest {

    private final PatientProgressRepository repo = mock(PatientProgressRepository.class);
    private final RomTimeSeriesService romService = mock(RomTimeSeriesService.class);
    private final CohortComparisonService cohortService = mock(CohortComparisonService.class);
    private final AnalyticsController controller = new AnalyticsController(repo, romService, cohortService);
    private final InternalAuthFilter filter = new InternalAuthFilter("test-key");

    @Test
    void validDniWithRows_returns200() {
        PatientProgress weekly = new PatientProgress();
        weekly.setGameId("reach");
        PatientProgress monthly = new PatientProgress();
        monthly.setGameId("*");
        when(repo.findByPatientDni("12345678Z")).thenReturn(List.of(weekly, monthly));

        ResponseEntity<PatientAnalyticsResponse> r = controller.patient("12345678Z");

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).isNotNull();
        assertThat(r.getBody().weekly()).hasSize(1);
        assertThat(r.getBody().monthlyByDisability()).hasSize(1);
        assertThat(r.getBody().patientDni()).isEqualTo("12345678Z");
    }

    @Test
    void validDniNoRows_returns404() {
        when(repo.findByPatientDni("99999999X")).thenReturn(List.of());

        ResponseEntity<PatientAnalyticsResponse> r = controller.patient("99999999X");

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void analyticsPathRequiresKey() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/analytics/patient/12345678Z");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(401);
        Mockito.verifyNoInteractions(chain);
    }

    @Test
    void analyticsPathWithValidKey_passesFilter() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/analytics/patient/12345678Z");
        req.addHeader("X-Internal-Key", "test-key");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(200);
        Mockito.verify(chain).doFilter(req, res);
    }
}
