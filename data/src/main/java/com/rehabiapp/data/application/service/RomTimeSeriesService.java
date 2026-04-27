package com.rehabiapp.data.application.service;

import com.rehabiapp.data.application.pipeline.RomTimeSeriesPipeline;
import com.rehabiapp.data.application.service.TrendCalculator.Trend;
import com.rehabiapp.data.application.service.dto.RomTimeSeriesPoint;
import com.rehabiapp.data.application.service.dto.RomTimeSeriesResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RomTimeSeriesService {

    private final RomTimeSeriesPipeline pipeline;

    public RomTimeSeriesService(RomTimeSeriesPipeline pipeline) {
        this.pipeline = pipeline;
    }

    public RomTimeSeriesResponse compute(String dni, String bucket,
                                         String fromStr, String toStr, String gameId) {
        Instant now = Instant.now();
        Instant from = fromStr != null
                ? LocalDate.parse(fromStr).atStartOfDay(ZoneOffset.UTC).toInstant()
                : now.minus(180, ChronoUnit.DAYS);
        Instant to = toStr != null
                ? LocalDate.parse(toStr).atTime(23, 59, 59).toInstant(ZoneOffset.UTC)
                : now;

        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }

        List<RomTimeSeriesPoint> points = pipeline.run(dni, bucket, from, to, gameId);
        if (points.isEmpty()) throw new NotFoundException("no sessions for " + dni);

        List<Double> romAvgs = points.stream().map(RomTimeSeriesPoint::romAvg).toList();
        Trend trend = TrendCalculator.compute(romAvgs);

        return new RomTimeSeriesResponse(
                dni, bucket,
                from.toString().substring(0, 10),
                to.toString().substring(0, 10),
                points, trend);
    }
}
