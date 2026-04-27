package com.rehabiapp.data.application.service.dto.chart;

import java.util.List;

public record ChartSeries(String name, List<String> x, List<Double> y) {}
