package com.rehabiapp.data.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Metricas Micrometer del pipeline de datos. Se exponen via /actuator/prometheus
 * para que Prometheus las recolecte.
 *
 *   - rehabiapp.data.ingest.sessions: contador de sesiones ingestadas correctamente.
 *   - rehabiapp.data.markdown.regenerations: contador de regeneraciones de Markdown.
 */
@Component
public class DataMetrics {

    private final Counter ingestCounter;
    private final Counter markdownCounter;

    public DataMetrics(MeterRegistry registry) {
        this.ingestCounter = Counter.builder("rehabiapp.data.ingest.sessions")
                .description("Sesiones de juego ingestadas correctamente")
                .register(registry);
        this.markdownCounter = Counter.builder("rehabiapp.data.markdown.regenerations")
                .description("Markdowns de pacientes regenerados")
                .register(registry);
    }

    public void incIngest() {
        ingestCounter.increment();
    }

    public void incMd() {
        markdownCounter.increment();
    }
}
