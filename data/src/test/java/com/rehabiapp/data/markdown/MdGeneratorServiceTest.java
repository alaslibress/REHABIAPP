package com.rehabiapp.data.markdown;

import com.rehabiapp.data.analytics.dto.TreatmentProgressDto;
import com.rehabiapp.data.analytics.service.TreatmentProgressService;
import com.rehabiapp.data.domain.document.GameSession;
import com.rehabiapp.data.domain.repository.GameSessionRepository;
import com.rehabiapp.data.util.PseudonymUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test del generador de Markdown contra un golden file.
 *
 * La comparacion ignora la linea "**Generado:**" porque depende de
 * Instant.now() y normaliza espacios en blanco.
 */
class MdGeneratorServiceTest {

    @Test
    void generaMarkdownConDosTratamientosYDosSesiones() throws IOException {
        // Mocks de dependencias: pipeline ya pre-procesado y repositorio de sesiones.
        TreatmentProgressService progressService = mock(TreatmentProgressService.class);
        GameSessionRepository sessionRepository = mock(GameSessionRepository.class);
        PseudonymUtil pseudonymUtil = new PseudonymUtil("salt-fijo-test");

        TreatmentProgressDto progreso = new TreatmentProgressDto(
                pseudonymUtil.tokenize("12345678A"),
                "ROM-HOMBRO-01",
                "Rehabilitacion hombro",
                "hombro_derecho",
                "Rango de movimiento (grados)",
                new TreatmentProgressDto.BaselineCurrent(Instant.parse("2026-01-01T00:00:00Z"), 30.0),
                new TreatmentProgressDto.BaselineCurrent(Instant.parse("2026-01-10T00:00:00Z"), 45.0),
                50.0,
                List.of(
                        new TreatmentProgressDto.EntradaDto(Instant.parse("2026-01-01T00:00:00Z"), 30.0),
                        new TreatmentProgressDto.EntradaDto(Instant.parse("2026-01-10T00:00:00Z"), 45.0)
                )
        );

        GameSession ses1 = sesion(Instant.parse("2026-01-10T10:00:00Z"), "game-rom-01", 87.5, true);
        GameSession ses0 = sesion(Instant.parse("2026-01-01T10:00:00Z"), "game-rom-01", 65.0, false);

        when(progressService.getOrCompute("12345678A")).thenReturn(List.of(progreso));
        when(sessionRepository.findTop20ByPatientDniOrderBySessionStartDesc("12345678A"))
                .thenReturn(List.of(ses1, ses0));

        MdGeneratorService generator =
                new MdGeneratorService(progressService, sessionRepository, pseudonymUtil);

        MdGeneratorService.GenerationResult result = generator.generar("12345678A");

        // Cargar golden con token sustituido al token real.
        String token = pseudonymUtil.tokenize("12345678A");
        String golden = Files.readString(
                        Path.of("src/test/resources/golden/markdown-paciente-test.md"),
                        StandardCharsets.UTF_8)
                .replace("__TOKEN__", token);

        String actual = result.content();

        // Compara ignorando la linea de timestamp y normalizando whitespace.
        assertThat(normalizar(actual)).isEqualTo(normalizar(golden));
        assertThat(result.sessionCount()).isEqualTo(2);
        assertThat(result.lastSessionAt()).isEqualTo(Instant.parse("2026-01-10T10:00:00Z"));
    }

    private GameSession sesion(Instant inicio, String gameId, double score, boolean completed) {
        return new GameSession(
                null, "12345678A", gameId, "discap-1", 1,
                inicio, inicio.plusSeconds(600), 600L, score,
                10, 10,
                new GameSession.MovementMetrics(45.0, 0.5, 1.0),
                completed, inicio, "token", "ROM-HOMBRO-01", "hombro_derecho",
                "Rehabilitacion hombro");
    }

    private String normalizar(String texto) {
        // Filtra la linea con timestamp generado, colapsa whitespace y trim.
        return texto.lines()
                .filter(l -> !l.startsWith("**Generado:**"))
                .map(String::stripTrailing)
                .reduce("", (a, b) -> a + "\n" + b)
                .trim()
                .replaceAll("\\s+\n", "\n");
    }
}
