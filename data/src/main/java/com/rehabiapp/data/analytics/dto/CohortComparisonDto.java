package com.rehabiapp.data.analytics.dto;

public record CohortComparisonDto(
        String patientToken,
        String disabilityId,
        Integer progressionLevel,
        Double patientAverageScore,
        Double cohortAverageScore,
        Double patientCompletionRate,
        Double cohortCompletionRate,
        Double patientAverageRom,
        Double cohortAverageRom
) {}
