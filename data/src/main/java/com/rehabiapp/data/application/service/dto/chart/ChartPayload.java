package com.rehabiapp.data.application.service.dto.chart;

import java.util.List;

public record ChartPayload(
        String chartId,
        String title,
        String xAxisLabel,
        String yAxisLabel,
        String xType,
        String yType,
        List<ChartSeries> series
) {}
