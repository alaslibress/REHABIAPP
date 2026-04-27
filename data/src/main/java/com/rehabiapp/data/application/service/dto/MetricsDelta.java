package com.rehabiapp.data.application.service.dto;

public record MetricsDelta(
        Double averageScore,
        Double averageDuration,
        Double completionRate,
        Double averageRangeOfMotion
) {}
