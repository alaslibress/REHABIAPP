package com.rehabiapp.data.domain.model;

import java.time.Instant;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Snapshot pre-agregado de progreso por paciente, juego y periodo.
 * Escrito por el job programado de Fase 3; leido por los endpoints de analitica.
 * Coleccion: patient_progress.
 */
@Document(collection = "patient_progress")
@CompoundIndexes({
    @CompoundIndex(
        name = "uk_patient_game_period",
        def = "{'patientDni': 1, 'gameId': 1, 'progressionLevel': 1, 'period': 1, 'disabilityCode': 1}",
        unique = true
    )
})
public class PatientProgress {

    @Id
    private String id;

    @Indexed(name = "idx_pp_patient_dni")
    private String patientDni;

    private String gameId;
    private Integer progressionLevel;

    /** Cubo temporal: ISO week "YYYY-Www" o mes "YYYY-MM". */
    private String period;

    /** null para filas semanales por juego; codigo discapacidad para filas mensuales cross-game. */
    @Indexed(name = "idx_pp_disability_code")
    private String disabilityCode;

    private Integer totalSessions;
    private Double averageScore;
    private Double averageDuration;
    private Double completionRate;
    private List<Double> rangeOfMotionTrend;
    private Instant lastUpdated;

    public PatientProgress() { }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDisabilityCode() { return disabilityCode; }
    public void setDisabilityCode(String disabilityCode) { this.disabilityCode = disabilityCode; }
    public String getPatientDni() { return patientDni; }
    public void setPatientDni(String patientDni) { this.patientDni = patientDni; }
    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }
    public Integer getProgressionLevel() { return progressionLevel; }
    public void setProgressionLevel(Integer progressionLevel) { this.progressionLevel = progressionLevel; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public Integer getTotalSessions() { return totalSessions; }
    public void setTotalSessions(Integer totalSessions) { this.totalSessions = totalSessions; }
    public Double getAverageScore() { return averageScore; }
    public void setAverageScore(Double averageScore) { this.averageScore = averageScore; }
    public Double getAverageDuration() { return averageDuration; }
    public void setAverageDuration(Double averageDuration) { this.averageDuration = averageDuration; }
    public Double getCompletionRate() { return completionRate; }
    public void setCompletionRate(Double completionRate) { this.completionRate = completionRate; }
    public List<Double> getRangeOfMotionTrend() { return rangeOfMotionTrend; }
    public void setRangeOfMotionTrend(List<Double> rangeOfMotionTrend) { this.rangeOfMotionTrend = rangeOfMotionTrend; }
    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
}
