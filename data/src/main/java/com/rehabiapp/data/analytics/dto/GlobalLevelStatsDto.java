package com.rehabiapp.data.analytics.dto;

public record GlobalLevelStatsDto(
        Integer progressionLevel,
        Long totalSessions,
        Long uniquePatients,
        Double averageScore,
        Double averageCompletionRate,
        Double averageRangeOfMotion
) {}
