package com.rehabiapp.data.application.service.dto;

public record MetricsPercentile(
        Double averageScore,
        Double completionRate,
        Double averageRangeOfMotion
) {}
