package com.rehabiapp.data.application.service.dto;

public record RomTimeSeriesPoint(String date, Double romAvg, Double romMax, Double romMin, Long sampleSize) {}
