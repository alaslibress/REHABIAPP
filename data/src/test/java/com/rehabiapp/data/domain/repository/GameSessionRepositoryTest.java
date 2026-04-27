package com.rehabiapp.data.domain.repository;

import com.rehabiapp.data.domain.model.GameSession;
import com.rehabiapp.data.domain.model.MovementMetrics;
import java.net.Socket;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests de integracion contra MongoDB real en localhost:27017.
 * Se saltan automaticamente si MongoDB no esta disponible (entorno CI sin DB).
 * Para ejecutar: docker run -d --rm -p 27017:27017 mongo:7.0 && mvn test
 */
@SpringBootTest
@ActiveProfiles("local")
class GameSessionRepositoryTest {

    @BeforeAll
    static void requiresMongo() {
        boolean mongoAvailable;
        try (Socket s = new Socket("localhost", 27017)) {
            mongoAvailable = true;
        } catch (Exception e) {
            mongoAvailable = false;
        }
        assumeTrue(mongoAvailable, "MongoDB no disponible en localhost:27017 — test omitido");
    }

    @Autowired
    GameSessionRepository repository;

    @AfterEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void saveAndFindBySessionId() {
        GameSession gs = buildSession("sess-001");
        repository.save(gs);

        Optional<GameSession> found = repository.findBySessionId("sess-001");

        assertThat(found).isPresent();
        assertThat(found.get().getPatientDni()).isEqualTo("12345678Z");
    }

    @Test
    void duplicateSessionIdThrows() {
        repository.save(buildSession("sess-dup"));

        assertThatThrownBy(() -> repository.save(buildSession("sess-dup")))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
    }

    @Test
    void existsBySessionIdTrueAfterSave() {
        repository.save(buildSession("sess-exists"));

        assertThat(repository.existsBySessionId("sess-exists")).isTrue();
        assertThat(repository.existsBySessionId("sess-no-such")).isFalse();
    }

    private GameSession buildSession(String sessionId) {
        GameSession g = new GameSession();
        g.setSessionId(sessionId);
        g.setPatientDni("12345678Z");
        g.setGameId("reach-and-grab");
        g.setProgressionLevel(1);
        g.setSessionStart(Instant.parse("2026-04-24T10:00:00Z"));
        g.setSessionEnd(Instant.parse("2026-04-24T10:05:00Z"));
        g.setDurationSeconds(300);
        g.setScore(750);
        g.setRepetitionsCompleted(10);
        g.setRepetitionsTarget(10);
        g.setMovementMetrics(new MovementMetrics(90.0, 0.4, 0.8));
        g.setCompleted(true);
        g.setReceivedAt(Instant.now());
        return g;
    }
}
