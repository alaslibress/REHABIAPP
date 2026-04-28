package com.rehabiapp.data.analytics.pipeline;

import com.rehabiapp.data.analytics.dto.CohortComparisonDto;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Pipeline: comparacion del progreso individual vs cohorte (misma discapacidad y nivel).
 * Ejecuta dos agregaciones independientes y las combina en servicio para claridad.
 */
@Component
public class CohortComparisonPipeline {

    private final MongoTemplate mongoTemplate;

    public CohortComparisonPipeline(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Metricas agregadas del paciente (una fila por combinacion disabilityId + progressionLevel).
     */
    public List<PatientStats> executePatientStats(String patientDni) {
        AggregationOperation addCompleted = ctx -> new Document("$addFields",
                new Document("completedInt", new Document("$cond", List.of(
                        new Document("$eq", List.of("$completed", true)), 1, 0)))
        );

        AggregationOperation group = ctx -> new Document("$group", new Document()
                .append("_id", new Document()
                        .append("patientToken", "$patientToken")
                        .append("disabilityId", "$disabilityId")
                        .append("progressionLevel", "$progressionLevel"))
                .append("totalSessions", new Document("$sum", 1))
                .append("averageScore", new Document("$avg", "$score"))
                .append("completedCount", new Document("$sum", "$completedInt"))
                .append("averageRom",
                        new Document("$avg", "$movementMetrics.rangeOfMotionDegrees"))
        );

        AggregationOperation project = ctx -> new Document("$project", new Document()
                .append("_id", 0)
                .append("patientToken", "$_id.patientToken")
                .append("disabilityId", "$_id.disabilityId")
                .append("progressionLevel", "$_id.progressionLevel")
                .append("averageScore", 1)
                .append("completionRate", new Document("$divide",
                        List.of("$completedCount", "$totalSessions")))
                .append("averageRom", 1)
        );

        var pipeline = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("patientDni").is(patientDni)),
                addCompleted, group, project
        );

        return mongoTemplate.aggregate(pipeline, "game_sessions", PatientStats.class)
                .getMappedResults();
    }

    /**
     * Metricas de cohorte: todos los pacientes excepto el consultado,
     * con la misma disabilityId y progressionLevel.
     */
    public CohortStats executeCohortStats(String disabilityId, Integer progressionLevel,
                                          String excludePatientDni) {
        AggregationOperation addCompleted = ctx -> new Document("$addFields",
                new Document("completedInt", new Document("$cond", List.of(
                        new Document("$eq", List.of("$completed", true)), 1, 0)))
        );

        AggregationOperation group = ctx -> new Document("$group", new Document()
                .append("_id", null)
                .append("averageScore", new Document("$avg", "$score"))
                .append("completedCount", new Document("$sum", "$completedInt"))
                .append("totalSessions", new Document("$sum", 1))
                .append("averageRom",
                        new Document("$avg", "$movementMetrics.rangeOfMotionDegrees"))
        );

        AggregationOperation project = ctx -> new Document("$project", new Document()
                .append("_id", 0)
                .append("averageScore", 1)
                .append("completionRate", new Document("$divide",
                        List.of("$completedCount", "$totalSessions")))
                .append("averageRom", 1)
        );

        var pipeline = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("disabilityId").is(disabilityId)
                        .and("progressionLevel").is(progressionLevel)
                        .and("patientDni").ne(excludePatientDni)),
                addCompleted, group, project
        );

        var results = mongoTemplate.aggregate(pipeline, "game_sessions", CohortStats.class)
                .getMappedResults();
        return results.isEmpty() ? new CohortStats(null, null, null) : results.getFirst();
    }

    public record PatientStats(
            String patientToken,
            String disabilityId,
            Integer progressionLevel,
            Double averageScore,
            Double completionRate,
            Double averageRom
    ) {}

    public record CohortStats(
            Double averageScore,
            Double completionRate,
            Double averageRom
    ) {}

    public List<CohortComparisonDto> buildComparisons(String patientDni) {
        var patientStatsList = executePatientStats(patientDni);
        return patientStatsList.stream().map(ps -> {
            var cohort = executeCohortStats(ps.disabilityId(), ps.progressionLevel(), patientDni);
            return new CohortComparisonDto(
                    ps.patientToken(),
                    ps.disabilityId(),
                    ps.progressionLevel(),
                    ps.averageScore(),
                    cohort.averageScore(),
                    ps.completionRate(),
                    cohort.completionRate(),
                    ps.averageRom(),
                    cohort.averageRom()
            );
        }).toList();
    }
}
