package com.rehabiapp.data.application.service;

import com.rehabiapp.data.application.service.dto.GameSessionIngestRequest;
import com.rehabiapp.data.application.service.dto.GameSessionIngestResponse;
import com.rehabiapp.data.application.service.dto.MovementMetricsDto;
import com.rehabiapp.data.domain.model.GameSession;
import com.rehabiapp.data.domain.model.MovementMetrics;
import com.rehabiapp.data.domain.repository.GameSessionRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * Orquesta la ingesta de sesiones de juego: mapeo DTO -> entidad, marcado de timestamp
 * de servidor, persistencia y traduccion de errores de idempotencia.
 */
@Service
public class GameSessionIngestService {

    private static final Logger log = LoggerFactory.getLogger(GameSessionIngestService.class);

    private final GameSessionRepository repository;

    public GameSessionIngestService(GameSessionRepository repository) {
        this.repository = repository;
    }

    /**
     * Persiste una sesion recien ingerida. Si el sessionId ya existe, lanza
     * {@link DuplicateSessionException} para que el controller responda 409.
     */
    public GameSessionIngestResponse ingest(GameSessionIngestRequest request) {

        // Pre-check barato antes de tirar excepcion de la BBDD. El indice unico
        // sigue siendo la red de seguridad definitiva contra condiciones de carrera.
        if (repository.existsBySessionId(request.sessionId())) {
            log.info("Sesion duplicada descartada sessionId={}", request.sessionId());
            throw new DuplicateSessionException(request.sessionId());
        }

        GameSession entity = toEntity(request);

        try {
            GameSession saved = repository.save(entity);
            log.info("Sesion ingerida sessionId={} dni={} gameId={} nivel={}",
                    saved.getSessionId(), saved.getPatientDni(),
                    saved.getGameId(), saved.getProgressionLevel());
            return new GameSessionIngestResponse(
                    saved.getId(), saved.getSessionId(), saved.getReceivedAt(), "CREATED");
        } catch (DuplicateKeyException e) {
            // Condicion de carrera: dos peticiones concurrentes con el mismo sessionId.
            log.warn("Colision en indice unico sessionId={}", request.sessionId());
            throw new DuplicateSessionException(request.sessionId());
        }
    }

    private GameSession toEntity(GameSessionIngestRequest r) {
        GameSession g = new GameSession();
        g.setSessionId(r.sessionId());
        g.setPatientDni(r.patientDni());
        g.setGameId(r.gameId());
        g.setProgressionLevel(r.progressionLevel());
        g.setSessionStart(r.sessionStart());
        g.setSessionEnd(r.sessionEnd());
        g.setDurationSeconds(r.durationSeconds());
        g.setScore(r.score());
        g.setRepetitionsCompleted(r.repetitionsCompleted());
        g.setRepetitionsTarget(r.repetitionsTarget());
        MovementMetricsDto m = r.movementMetrics();
        g.setMovementMetrics(new MovementMetrics(
                m.rangeOfMotionDegrees(), m.averageSpeed(), m.maxSpeed()));
        g.setCompleted(r.completed());
        g.setDisabilityCode(r.disabilityCode());
        g.setReceivedAt(Instant.now());
        return g;
    }
}
