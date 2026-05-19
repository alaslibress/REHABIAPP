package com.rehabiapp.data.analytics.dto;

public record WeeklyProgressDto(
        String patientToken,
        String gameId,
        Integer progressionLevel,
        String week,             // "YYYY-Www"
        Long totalSessions,
        Double averageScore,
        Double completionRate,   // 0.0 - 1.0
        Double averageRangeOfMotion
) {}
