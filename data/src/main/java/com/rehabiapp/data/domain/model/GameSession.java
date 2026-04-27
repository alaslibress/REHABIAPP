package com.rehabiapp.data.domain.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Telemetria cruda de una sesion de juego terapeutico.
 * Coleccion: game_sessions.
 *
 * Indices:
 *  - sessionId unico (idempotencia de ingesta).
 *  - patientDni + sessionStart (consultas por paciente ordenadas en el tiempo).
 *  - gameId + progressionLevel (agregaciones por juego/nivel).
 */
@Document(collection = "game_sessions")
@CompoundIndexes({
    @CompoundIndex(
        name = "idx_patient_sessionstart",
        def = "{'patientDni': 1, 'sessionStart': -1}"
    ),
    @CompoundIndex(
        name = "idx_game_level",
        def = "{'gameId': 1, 'progressionLevel': 1}"
    ),
    @CompoundIndex(
        name = "idx_patient_disability_start",
        def = "{'patientDni': 1, 'disabilityCode': 1, 'sessionStart': -1}"
    )
})
public class GameSession {

    @Id
    private String id;

    /** Identificador unico de la sesion generado por el cliente (Unity/api). Clave de idempotencia. */
    @Indexed(unique = true, name = "uk_session_id")
    private String sessionId;

    @Indexed(name = "idx_patient_dni")
    private String patientDni;

    @Indexed(name = "idx_game_id")
    private String gameId;

    @Indexed(name = "idx_progression_level")
    private Integer progressionLevel;

    private Instant sessionStart;
    private Instant sessionEnd;
    private Integer durationSeconds;
    private Integer score;
    private Integer repetitionsCompleted;
    private Integer repetitionsTarget;
    private MovementMetrics movementMetrics;
    private Boolean completed;

    /** Codigo de discapacidad CIE-10 o interno. Denormalizado en ingesta para pipelines. */
    @Indexed(name = "idx_disability_code")
    private String disabilityCode;

    /** Timestamp de llegada al servicio. Lo fija el servidor, NO el cliente. */
    private Instant receivedAt;

    public GameSession() { }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getPatientDni() { return patientDni; }
    public void setPatientDni(String patientDni) { this.patientDni = patientDni; }
    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }
    public Integer getProgressionLevel() { return progressionLevel; }
    public void setProgressionLevel(Integer progressionLevel) { this.progressionLevel = progressionLevel; }
    public Instant getSessionStart() { return sessionStart; }
    public void setSessionStart(Instant sessionStart) { this.sessionStart = sessionStart; }
    public Instant getSessionEnd() { return sessionEnd; }
    public void setSessionEnd(Instant sessionEnd) { this.sessionEnd = sessionEnd; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public Integer getRepetitionsCompleted() { return repetitionsCompleted; }
    public void setRepetitionsCompleted(Integer repetitionsCompleted) { this.repetitionsCompleted = repetitionsCompleted; }
    public Integer getRepetitionsTarget() { return repetitionsTarget; }
    public void setRepetitionsTarget(Integer repetitionsTarget) { this.repetitionsTarget = repetitionsTarget; }
    public MovementMetrics getMovementMetrics() { return movementMetrics; }
    public void setMovementMetrics(MovementMetrics movementMetrics) { this.movementMetrics = movementMetrics; }
    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }
    public String getDisabilityCode() { return disabilityCode; }
    public void setDisabilityCode(String disabilityCode) { this.disabilityCode = disabilityCode; }
    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }
}
