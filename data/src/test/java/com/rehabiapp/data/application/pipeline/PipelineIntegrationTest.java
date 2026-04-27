package com.rehabiapp.data.application.pipeline;

import com.rehabiapp.data.domain.model.GameSession;
import com.rehabiapp.data.domain.model.LevelStatistics;
import com.rehabiapp.data.domain.model.MovementMetrics;
import com.rehabiapp.data.domain.model.PatientProgress;
import com.rehabiapp.data.domain.repository.GameSessionRepository;
import java.net.Socket;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests de integracion de los tres pipelines contra MongoDB real.
 * Omitidos si MongoDB no esta en localhost:27017.
 */
@SpringBootTest
@ActiveProfiles("local")
class PipelineIntegrationTest {

    @BeforeAll
    static void requiresMongo() {
        boolean ok;
        try (Socket s = new Socket("localhost", 27017)) { ok = true; }
        catch (Exception e) { ok = false; }
        assumeTrue(ok, "MongoDB no disponible — test omitido");
    }

    @Autowired GameSessionRepository gameSessionRepo;
    @Autowired WeeklyGamePipeline weeklyPipeline;
    @Autowired MonthlyDisabilityPipeline monthlyPipeline;
    @Autowired LevelStatisticsPipeline levelsPipeline;

    @AfterEach
    void clean() { gameSessionRepo.deleteAll(); }

    @Test
    void weeklyPipeline_producesTwoRowsForTwoWeeks() {
        // Semana ISO 16 (2026-04-13) y semana ISO 17 (2026-04-20) — mismo paciente+juego+nivel
        gameSessionRepo.saveAll(List.of(
            session("w1", "12345678Z", "game1", 2, "M25.5",
                    Instant.parse("2026-04-13T10:00:00Z"), 800, true),
            session("w2", "12345678Z", "game1", 2, "M25.5",
                    Instant.parse("2026-04-20T10:00:00Z"), 600, false)
        ));

        List<PatientProgress> rows = weeklyPipeline.run(Instant.parse("2026-01-01T00:00:00Z"));

        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(PatientProgress::getPeriod)
                .allSatisfy(p -> assertThat(p).matches("\\d{4}-W\\d{2}"));
    }

    @Test
    void monthlyPipeline_producesCrossGameRow() {
        // Dos juegos distintos en el mismo mes para la misma discapacidad
        gameSessionRepo.saveAll(List.of(
            session("m1", "12345678Z", "game1", 1, "M25.5",
                    Instant.parse("2026-04-10T10:00:00Z"), 700, true),
            session("m2", "12345678Z", "game2", 1, "M25.5",
                    Instant.parse("2026-04-15T10:00:00Z"), 500, true)
        ));

        List<PatientProgress> rows = monthlyPipeline.run(Instant.parse("2026-01-01T00:00:00Z"));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getGameId()).isEqualTo("*");
        assertThat(rows.get(0).getPeriod()).matches("\\d{4}-\\d{2}");
        assertThat(rows.get(0).getTotalSessions()).isEqualTo(2);
    }

    @Test
    void levelsPipeline_producesRowPerLevel() {
        // Tres sesiones con tres niveles distintos
        gameSessionRepo.saveAll(List.of(
            session("l1", "12345678Z", "game1", 1, "M25.5",
                    Instant.parse("2026-04-10T10:00:00Z"), 700, true),
            session("l2", "12345678Z", "game1", 2, "M25.5",
                    Instant.parse("2026-04-11T10:00:00Z"), 600, false),
            session("l3", "12345678Z", "game1", 3, "M25.5",
                    Instant.parse("2026-04-12T10:00:00Z"), 800, true)
        ));

        List<LevelStatistics> rows = levelsPipeline.run();

        assertThat(rows).hasSize(3);
        assertThat(rows).extracting(LevelStatistics::getProgressionLevel)
                .containsExactlyInAnyOrder(1, 2, 3);
    }

    private GameSession session(String sessionId, String dni, String gameId,
                                int level, String disability, Instant start,
                                int score, boolean completed) {
        GameSession g = new GameSession();
        g.setSessionId(sessionId);
        g.setPatientDni(dni);
        g.setGameId(gameId);
        g.setProgressionLevel(level);
        g.setDisabilityCode(disability);
        g.setSessionStart(start);
        g.setSessionEnd(start.plusSeconds(300));
        g.setDurationSeconds(300);
        g.setScore(score);
        g.setRepetitionsCompleted(10);
        g.setRepetitionsTarget(10);
        g.setMovementMetrics(new MovementMetrics(85.0, 0.4, 0.8));
        g.setCompleted(completed);
        g.setReceivedAt(Instant.now());
        return g;
    }
}
