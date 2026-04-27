package com.rehabiapp.data.application.service.dto;

public record PatientMetrics(
        Long totalSessions,
        Double averageScore,
        Double averageDuration,
        Double completionRate,
        Double averageRangeOfMotion
) {}
