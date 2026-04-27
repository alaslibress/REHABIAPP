package com.rehabiapp.data.application.pipeline;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

import com.rehabiapp.data.domain.model.GameSession;
import java.time.Instant;
import java.util.List;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

/**
 * Agrega metricas de un paciente objetivo y de su cohorte (misma discapacidad+nivel, sin el paciente).
 * Usa dos aggregations separadas para evitar sesgos por frecuencia de sesiones en el cohorte.
 */
@Component
public class CohortComparisonPipeline {

    private final MongoTemplate mongoTemplate;

    public CohortComparisonPipeline(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public record PatientAggregate(long totalSessions, double avgScore, double avgDuration,
                                   double completionRate, double avgRom) {}

    public record CohortAggregate(long cohortSize, long totalSessions,
                                  double avgScore, double avgDuration,
                                  double completionRate, double avgRom,
                                  List<Double> allScores, List<Double> allCompletions,
                                  List<Double> allRoms) {}

    public PatientAggregate runTarget(String dni, String disability, int level,
                                      Instant from, Instant to) {
        Aggregation agg = newAggregation(
            match(Criteria.where("patientDni").is(dni)
                    .and("disabilityCode").is(disability)
                    .and("progressionLevel").is(level)
                    .and("sessionStart").gte(from).lte(to)),
            group()
                .count().as("totalSessions")
                .avg("score").as("avgScore")
                .avg("durationSeconds").as("avgDuration")
                .avg(ConditionalOperators.when(Criteria.where("completed").is(true))
                        .then(1).otherwise(0)).as("completionRate")
                .avg("movementMetrics.rangeOfMotionDegrees").as("avgRom")
        );

        AggregationResults<Document> res = mongoTemplate.aggregate(agg, GameSession.class, Document.class);
        if (res.getMappedResults().isEmpty()) return null;

        Document d = res.getMappedResults().get(0);
        return new PatientAggregate(
                lng(d.get("totalSessions")),
                dbl(d.get("avgScore")),
                dbl(d.get("avgDuration")),
                dbl(d.get("completionRate")),
                dbl(d.get("avgRom")));
    }

    @SuppressWarnings("unchecked")
    public CohortAggregate runCohort(String excludeDni, String disability, int level,
                                     Instant from, Instant to) {
        // Fase 1: agrega por paciente para evitar sesgo por volumen
        Aggregation perPatient = newAggregation(
            match(Criteria.where("disabilityCode").is(disability)
                    .and("progressionLevel").is(level)
                    .and("sessionStart").gte(from).lte(to)
                    .and("patientDni").ne(excludeDni)),
            group("patientDni")
                .count().as("sessions")
                .avg("score").as("score")
                .avg("durationSeconds").as("duration")
                .avg(ConditionalOperators.when(Criteria.where("completed").is(true))
                        .then(1).otherwise(0)).as("completion")
                .avg("movementMetrics.rangeOfMotionDegrees").as("rom")
        );

        List<Document> perPatientRows = mongoTemplate.aggregate(
                perPatient, GameSession.class, Document.class).getMappedResults();

        if (perPatientRows.isEmpty()) return null;

        // Fase 2: reduce en Java para obtener medias de medias + arrays para percentil
        long cohortSize = perPatientRows.size();
        long totalSessions = perPatientRows.stream()
                .mapToLong(d -> lng(d.get("sessions"))).sum();
        double avgScore = perPatientRows.stream()
                .mapToDouble(d -> dbl(d.get("score"))).average().orElse(0);
        double avgDuration = perPatientRows.stream()
                .mapToDouble(d -> dbl(d.get("duration"))).average().orElse(0);
        double avgCompletion = perPatientRows.stream()
                .mapToDouble(d -> dbl(d.get("completion"))).average().orElse(0);
        double avgRom = perPatientRows.stream()
                .mapToDouble(d -> dbl(d.get("rom"))).average().orElse(0);

        List<Double> allScores = perPatientRows.stream().map(d -> dbl(d.get("score"))).sorted().toList();
        List<Double> allCompletions = perPatientRows.stream().map(d -> dbl(d.get("completion"))).sorted().toList();
        List<Double> allRoms = perPatientRows.stream().map(d -> dbl(d.get("rom"))).sorted().toList();

        return new CohortAggregate(cohortSize, totalSessions,
                avgScore, avgDuration, avgCompletion, avgRom,
                allScores, allCompletions, allRoms);
    }

    private static double dbl(Object o) { return o == null ? 0.0 : ((Number) o).doubleValue(); }
    private static long lng(Object o) { return o == null ? 0L : ((Number) o).longValue(); }
}
