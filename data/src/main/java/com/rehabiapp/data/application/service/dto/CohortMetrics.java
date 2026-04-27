package com.rehabiapp.data.application.service.dto;

public record CohortMetrics(
        Long cohortSize,
        Long totalSessions,
        Double averageScore,
        Double averageDuration,
        Double completionRate,
        Double averageRangeOfMotion
) {}
