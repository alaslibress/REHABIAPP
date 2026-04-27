package com.rehabiapp.data.application.service.dto;

import com.rehabiapp.data.application.service.TrendCalculator.Trend;
import java.util.List;

public record RomTimeSeriesResponse(
        String patientDni,
        String bucket,
        String from,
        String to,
        List<RomTimeSeriesPoint> points,
        Trend trend
) {}
