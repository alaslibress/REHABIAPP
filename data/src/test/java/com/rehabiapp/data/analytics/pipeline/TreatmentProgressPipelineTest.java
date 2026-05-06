package com.rehabiapp.data.analytics.pipeline;

import com.rehabiapp.data.analytics.dto.TreatmentProgressDto;
import com.rehabiapp.data.util.TreatmentMetricResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test unitario del pipeline de progreso terapeutico.
 *
 * No usamos Testcontainers porque la dependencia no esta presente en el POM.
 * Simulamos la respuesta agregada de MongoDB con un MongoTemplate mockeado y
 * comprobamos que el post-proceso (resolver de etiquetas) se aplica.
 */
class TreatmentProgressPipelineTest {

    private MongoTemplate mongoTemplate;
    private TreatmentMetricResolver resolver;
    private TreatmentProgressPipeline pipeline;

    @BeforeEach
    void setup() {
        mongoTemplate = mock(MongoTemplate.class);
        resolver = new TreatmentMetricResolver();
        pipeline = new TreatmentProgressPipeline(mongoTemplate, resolver);
    }

    @Test
    void postProcesoRellenaMetricaNombreSegunCodTrat() {
        // Resultado simulado del pipeline server-side, con metricaNombre vacia.
        TreatmentProgressDto raw = new TreatmentProgressDto(
                "token-abc",
                "ROM-HOMBRO-01",
                "Rehabilitacion hombro",
                "hombro_derecho",
                null,
                new TreatmentProgressDto.BaselineCurrent(Instant.parse("2026-01-01T00:00:00Z"), 30.0),
                new TreatmentProgressDto.BaselineCurrent(Instant.parse("2026-01-10T00:00:00Z"), 45.0),
                50.0,
                List.of(
                        new TreatmentProgressDto.EntradaDto(Instant.parse("2026-01-01T00:00:00Z"), 30.0),
                        new TreatmentProgressDto.EntradaDto(Instant.parse("2026-01-10T00:00:00Z"), 45.0)
                )
        );

        @SuppressWarnings("unchecked")
        AggregationResults<TreatmentProgressDto> results = mock(AggregationResults.class);
        when(results.getMappedResults()).thenReturn(List.of(raw));
        when(mongoTemplate.aggregate(any(Aggregation.class),
                eq("game_sessions"),
                eq(TreatmentProgressDto.class))).thenReturn(results);

        List<TreatmentProgressDto> resultado = pipeline.execute("12345678A");

        assertThat(resultado).hasSize(1);
        TreatmentProgressDto p = resultado.get(0);
        assertThat(p.metricaNombre()).isEqualTo("Rango de movimiento (grados)");
        assertThat(p.deltaPorcentaje()).isEqualTo(50.0);
        assertThat(p.entradas()).hasSize(2);
        assertThat(p.baseline().valor()).isEqualTo(30.0);
        assertThat(p.current().valor()).isEqualTo(45.0);
    }

    @Test
    void resultadoVacioNoRompe() {
        @SuppressWarnings("unchecked")
        AggregationResults<TreatmentProgressDto> results = mock(AggregationResults.class);
        when(results.getMappedResults()).thenReturn(List.of());
        when(mongoTemplate.aggregate(any(Aggregation.class),
                eq("game_sessions"),
                eq(TreatmentProgressDto.class))).thenReturn(results);

        assertThat(pipeline.execute("00000000Z")).isEmpty();
    }
}
