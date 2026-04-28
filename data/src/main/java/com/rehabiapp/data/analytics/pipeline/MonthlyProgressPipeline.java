package com.rehabiapp.data.analytics.pipeline;

import com.rehabiapp.data.analytics.dto.MonthlyProgressDto;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Pipeline: progreso mensual por discapacidad (cross-game).
 * Util para visualizar evolucion clinica a lo largo de un mes completo.
 */
@Component
public class MonthlyProgressPipeline {

    private final MongoTemplate mongoTemplate;

    public MonthlyProgressPipeline(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public List<MonthlyProgressDto> execute(String patientDni) {
        // Formato "YYYY-MM" con padding de mes a dos digitos
        AggregationOperation addMonth = ctx -> new Document("$addFields", new Document()
                .append("month", new Document("$concat", List.of(
                        new Document("$toString", new Document("$year", "$sessionStart")),
                        "-",
                        new Document("$cond", List.of(
                                new Document("$lt", List.of(
                                        new Document("$month", "$sessionStart"), 10)),
                                new Document("$concat", List.of("0",
                                        new Document("$toString",
                                                new Document("$month", "$sessionStart")))),
                                new Document("$toString",
                                        new Document("$month", "$sessionStart"))
                        ))
                )))
                .append("completedInt", new Document("$cond", List.of(
                        new Document("$eq", List.of("$completed", true)), 1, 0)))
        );

        AggregationOperation group = ctx -> new Document("$group", new Document()
                .append("_id", new Document()
                        .append("patientToken", "$patientToken")
                        .append("disabilityId", "$disabilityId")
                        .append("progressionLevel", "$progressionLevel")
                        .append("month", "$month"))
                .append("totalSessions", new Document("$sum", 1))
                .append("averageScore", new Document("$avg", "$score"))
                .append("completedCount", new Document("$sum", "$completedInt"))
                .append("averageRangeOfMotion",
                        new Document("$avg", "$movementMetrics.rangeOfMotionDegrees"))
        );

        AggregationOperation project = ctx -> new Document("$project", new Document()
                .append("_id", 0)
                .append("patientToken", "$_id.patientToken")
                .append("disabilityId", "$_id.disabilityId")
                .append("progressionLevel", "$_id.progressionLevel")
                .append("month", "$_id.month")
                .append("totalSessions", 1)
                .append("averageScore", 1)
                .append("completionRate", new Document("$divide",
                        List.of("$completedCount", "$totalSessions")))
                .append("averageRangeOfMotion", 1)
        );

        var pipeline = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("patientDni").is(patientDni)),
                addMonth,
                group,
                project,
                Aggregation.sort(Sort.by(Sort.Direction.ASC, "month"))
        );

        return mongoTemplate.aggregate(pipeline, "game_sessions", MonthlyProgressDto.class)
                .getMappedResults();
    }
}
