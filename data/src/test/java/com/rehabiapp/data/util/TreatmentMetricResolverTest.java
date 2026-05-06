package com.rehabiapp.data.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios del resolver de metricas. Comprueba el mapeo por defecto.
 */
class TreatmentMetricResolverTest {

    private final TreatmentMetricResolver resolver = new TreatmentMetricResolver();

    @Test
    void prefijoRomDevuelveRangeOfMotion() {
        assertThat(resolver.resolverCampo("ROM-HOMBRO-01")).isEqualTo("rangeOfMotionDegrees");
        assertThat(resolver.resolverLabel("ROM-HOMBRO-01")).isEqualTo("Rango de movimiento (grados)");
    }

    @Test
    void prefijoVelDevuelveAverageSpeed() {
        assertThat(resolver.resolverCampo("VEL-CODO-02")).isEqualTo("averageSpeed");
        assertThat(resolver.resolverLabel("VEL-CODO-02")).isEqualTo("Velocidad media (m/s)");
    }

    @Test
    void prefijoFuerzaDevuelveMaxSpeed() {
        assertThat(resolver.resolverCampo("FUERZA-RODILLA-01")).isEqualTo("maxSpeed");
        assertThat(resolver.resolverLabel("FUERZA-RODILLA-01")).isEqualTo("Velocidad maxima (m/s)");
    }

    @Test
    void prefijoDesconocidoUsaFallback() {
        assertThat(resolver.resolverCampo("XX-OTRO")).isEqualTo("rangeOfMotionDegrees");
        assertThat(resolver.resolverLabel("XX-OTRO")).isEqualTo("Metrica");
    }

    @Test
    void codigoNuloUsaFallbackSeguro() {
        assertThat(resolver.resolverCampo(null)).isEqualTo("rangeOfMotionDegrees");
        assertThat(resolver.resolverLabel(null)).isEqualTo("Metrica");
    }
}
