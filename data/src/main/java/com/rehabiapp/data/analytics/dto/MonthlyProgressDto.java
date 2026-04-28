package com.rehabiapp.data.analytics.dto;

public record MonthlyProgressDto(
        String patientToken,
        String disabilityId,
        Integer progressionLevel,
        String month,            // "YYYY-MM"
        Long totalSessions,
        Double averageScore,
        Double completionRate,
        Double averageRangeOfMotion
) {}
