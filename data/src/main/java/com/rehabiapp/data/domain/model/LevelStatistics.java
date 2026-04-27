package com.rehabiapp.data.domain.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Estadisticas globales agregadas por nivel de progresion (1-4).
 * Coleccion: level_statistics. Refrescada por el job programado.
 */
@Document(collection = "level_statistics")
public class LevelStatistics {

    @Id
    private String id;

    @Indexed(unique = true, name = "uk_progression_level")
    private Integer progressionLevel;

    private Long totalSessions;
    private Long totalPatients;
    private Double averageScore;
    private Double averageDuration;
    private Double globalCompletionRate;
    private Double averageRangeOfMotion;
    private Instant lastUpdated;

    public LevelStatistics() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Integer getProgressionLevel() { return progressionLevel; }
    public void setProgressionLevel(Integer v) { this.progressionLevel = v; }
    public Long getTotalSessions() { return totalSessions; }
    public void setTotalSessions(Long v) { this.totalSessions = v; }
    public Long getTotalPatients() { return totalPatients; }
    public void setTotalPatients(Long v) { this.totalPatients = v; }
    public Double getAverageScore() { return averageScore; }
    public void setAverageScore(Double v) { this.averageScore = v; }
    public Double getAverageDuration() { return averageDuration; }
    public void setAverageDuration(Double v) { this.averageDuration = v; }
    public Double getGlobalCompletionRate() { return globalCompletionRate; }
    public void setGlobalCompletionRate(Double v) { this.globalCompletionRate = v; }
    public Double getAverageRangeOfMotion() { return averageRangeOfMotion; }
    public void setAverageRangeOfMotion(Double v) { this.averageRangeOfMotion = v; }
    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant v) { this.lastUpdated = v; }
}
