package com.rehabiapp.data.analytics.dto;

import java.util.List;

/**
 * Respuesta agregada del endpoint GET /analytics/patient/{dni}.
 * Todos los campos usan patientToken (anonimizado), nunca patientDni.
 */
public record PatientAnalyticsResponse(
        String patientToken,
        List<WeeklyProgressDto> weeklyProgress,
        List<MonthlyProgressDto> monthlyProgress,
        List<TimeSeriesRomDto> romTimeSeries,
        List<CohortComparisonDto> cohortComparison
) {}
