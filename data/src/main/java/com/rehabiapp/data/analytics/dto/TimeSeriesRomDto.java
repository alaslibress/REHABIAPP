package com.rehabiapp.data.analytics.dto;

import java.time.Instant;

public record TimeSeriesRomDto(
        String patientToken,
        String gameId,
        Instant sessionStart,
        Double rangeOfMotionDegrees
) {}
