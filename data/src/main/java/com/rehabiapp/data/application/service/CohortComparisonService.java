package com.rehabiapp.data.application.service;

import com.rehabiapp.data.application.pipeline.CohortComparisonPipeline;
import com.rehabiapp.data.application.pipeline.CohortComparisonPipeline.CohortAggregate;
import com.rehabiapp.data.application.pipeline.CohortComparisonPipeline.PatientAggregate;
import com.rehabiapp.data.application.service.dto.CohortComparisonResponse;
import com.rehabiapp.data.application.service.dto.CohortMetrics;
import com.rehabiapp.data.application.service.dto.MetricsDelta;
import com.rehabiapp.data.application.service.dto.MetricsPercentile;
import com.rehabiapp.data.application.service.dto.PatientMetrics;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CohortComparisonService {

    private final CohortComparisonPipeline pipeline;

    public CohortComparisonService(CohortComparisonPipeline pipeline) {
        this.pipeline = pipeline;
    }

    public CohortComparisonResponse compute(String dni, String disability, int level,
                                             String fromStr, String toStr) {
        Instant now = Instant.now();
        Instant from = fromStr != null
                ? LocalDate.parse(fromStr).atStartOfDay(ZoneOffset.UTC).toInstant()
                : now.minus(180, ChronoUnit.DAYS);
        Instant to = toStr != null
                ? LocalDate.parse(toStr).atTime(23, 59, 59).toInstant(ZoneOffset.UTC)
                : now;

        PatientAggregate pat = pipeline.runTarget(dni, disability, level, from, to);
        if (pat == null) throw new NotFoundException("no sessions for " + dni);

        PatientMetrics patientMetrics = new PatientMetrics(
                pat.totalSessions(), pat.avgScore(), pat.avgDuration(),
                pat.completionRate(), pat.avgRom());

        CohortAggregate coh = pipeline.runCohort(dni, disability, level, from, to);
        if (coh == null) {
            return new CohortComparisonResponse(dni, disability, level,
                    patientMetrics, null, null, null, "empty_cohort");
        }

        CohortMetrics cohortMetrics = new CohortMetrics(
                coh.cohortSize(), coh.totalSessions(), coh.avgScore(),
                coh.avgDuration(), coh.completionRate(), coh.avgRom());

        MetricsDelta delta = new MetricsDelta(
                pat.avgScore() - coh.avgScore(),
                pat.avgDuration() - coh.avgDuration(),
                pat.completionRate() - coh.completionRate(),
                pat.avgRom() - coh.avgRom());

        MetricsPercentile percentile = new MetricsPercentile(
                computePercentile(pat.avgScore(), coh.allScores()),
                computePercentile(pat.completionRate(), coh.allCompletions()),
                computePercentile(pat.avgRom(), coh.allRoms()));

        return new CohortComparisonResponse(dni, disability, level,
                patientMetrics, cohortMetrics, delta, percentile, null);
    }

    /** Retorna fraccion de miembros del cohorte por debajo del valor del paciente. */
    private static double computePercentile(double value, List<Double> sorted) {
        if (sorted.isEmpty()) return 0.0;
        long below = sorted.stream().filter(v -> v < value).count();
        return (double) below / sorted.size();
    }
}
