package com.rehabiapp.data.application.service;

import com.rehabiapp.data.domain.model.GameSession;
import com.rehabiapp.data.domain.model.MovementMetrics;
import com.rehabiapp.data.domain.model.PatientProgress;
import com.rehabiapp.data.domain.repository.GameSessionRepository;
import com.rehabiapp.data.domain.repository.LevelStatisticsRepository;
import com.rehabiapp.data.domain.repository.PatientProgressRepository;
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
 * Tests de integracion del AnalyticsRefreshJob contra MongoDB real.
 * Verifica upsert y idempotencia del refresh doble.
 */
@SpringBootTest
@ActiveProfiles("local")
class AnalyticsRefreshJobTest {

    @BeforeAll
    static void requiresMongo() {
        boolean ok;
        try (Socket s = new Socket("localhost", 27017)) { ok = true; }
        catch (Exception e) { ok = false; }
        assumeTrue(ok, "MongoDB no disponible — test omitido");
    }

    @Autowired AnalyticsRefreshJob job;
    @Autowired GameSessionRepository gameRepo;
    @Autowired PatientProgressRepository progressRepo;
    @Autowired LevelStatisticsRepository levelRepo;

    @AfterEach
    void clean() {
        gameRepo.deleteAll();
        progressRepo.deleteAll();
        levelRepo.deleteAll();
    }

    @Test
    void refresh_populatesBothCollections() {
        gameRepo.saveAll(List.of(
            session("j1", "12345678Z", "game1", 2, "M25.5",
                    Instant.parse("2026-04-13T10:00:00Z"), 700, true),
            session("j2", "12345678Z", "game2", 2, "M25.5",
                    Instant.parse("2026-04-14T10:00:00Z"), 500, false)
        ));

        job.refresh();

        assertThat(progressRepo.count()).isGreaterThan(0);
        assertThat(levelRepo.count()).isGreaterThan(0);
    }

    @Test
    void doubleRefresh_doesNotDuplicateRows() {
        gameRepo.saveAll(List.of(
            session("d1", "12345678Z", "game1", 1, "M25.5",
                    Instant.parse("2026-04-10T10:00:00Z"), 800, true)
        ));

        job.refresh();
        long countAfterFirst = progressRepo.count();

        job.refresh();
        long countAfterSecond = progressRepo.count();

        assertThat(countAfterSecond).isEqualTo(countAfterFirst);
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
