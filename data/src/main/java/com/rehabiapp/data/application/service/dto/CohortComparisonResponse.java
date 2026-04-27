package com.rehabiapp.data.application.service.dto;

public record CohortComparisonResponse(
        String patientDni,
        String disabilityCode,
        Integer progressionLevel,
        PatientMetrics patient,
        CohortMetrics cohort,
        MetricsDelta delta,
        MetricsPercentile percentile,
        String note
) {}
