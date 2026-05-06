package com.rehabiapp.data.ingestion.service;

import com.rehabiapp.data.domain.document.GameSession;
import com.rehabiapp.data.domain.repository.GameSessionRepository;
import com.rehabiapp.data.ingestion.dto.GameSessionIngestionRequest;
import com.rehabiapp.data.observability.DataMetrics;
import com.rehabiapp.data.util.PseudonymUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class IngestService {

    private final GameSessionRepository repository;
    private final PseudonymUtil pseudonymUtil;

    // Metricas opcionales (Phase 7). Inyectadas si el bean existe en el contexto.
    @Autowired(required = false)
    private DataMetrics metrics_;

    public IngestService(GameSessionRepository repository, PseudonymUtil pseudonymUtil) {
        this.repository = repository;
        this.pseudonymUtil = pseudonymUtil;
    }

    public GameSession ingestSession(GameSessionIngestionRequest req) {
        // Deteccion de duplicados: combinacion unica de paciente + juego + inicio
        if (repository.existsByPatientDniAndGameIdAndSessionStart(
                req.patientDni(), req.gameId(), req.sessionStart())) {
            throw new DuplicateSessionException(
                    "Sesion duplicada: paciente=%s juego=%s inicio=%s"
                            .formatted(req.patientDni(), req.gameId(), req.sessionStart()));
        }

        var metrics = new GameSession.MovementMetrics(
                req.movementMetrics().rangeOfMotionDegrees(),
                req.movementMetrics().averageSpeed(),
                req.movementMetrics().maxSpeed()
        );

        var session = new GameSession(
                null,
                req.patientDni(),
                req.gameId(),
                req.disabilityId(),
                req.progressionLevel(),
                req.sessionStart(),
                req.sessionEnd(),
                req.durationSeconds(),
                req.score(),
                req.repetitionsCompleted(),
                req.repetitionsTarget(),
                metrics,
                req.completed(),
                Instant.now(),
                pseudonymUtil.tokenize(req.patientDni()),
                req.codTrat(),
                req.parteCuerpo(),
                req.tratamientoNombre()
        );

        var saved = repository.save(session);
        // Metrica de observabilidad: contador de sesiones ingestadas correctamente
        if (metrics_ != null) {
            metrics_.incIngest();
        }
        return saved;
    }
}
