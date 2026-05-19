package com.rehabiapp.data.analytics.pipeline;

import com.rehabiapp.data.analytics.dto.GlobalLevelStatsDto;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Pipeline: estadisticas globales por nivel de progresion.
 * Usado en dashboards de fisioterapeutas para comparar cohortes.
 */
@Component
public class GlobalLevelStatsPipeline {

    private final MongoTemplate mongoTemplate;

    public GlobalLevelStatsPipeline(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public List<GlobalLevelStatsDto> execute() {
        AggregationOperation addCompleted = ctx -> new Document("$addFields",
                new Document("completedInt", new Document("$cond", List.of(
                        new Document("$eq", List.of("$completed", true)), 1, 0)))
        );

        AggregationOperation group = ctx -> new Document("$group", new Document()
                .append("_id", "$progressionLevel")
                .append("totalSessions", new Document("$sum", 1))
                .append("uniquePatients", new Document("$addToSet", "$patientToken"))
                .append("averageScore", new Document("$avg", "$score"))
                .append("completedCount", new Document("$sum", "$completedInt"))
                .append("averageRangeOfMotion",
                        new Document("$avg", "$movementMetrics.rangeOfMotionDegrees"))
        );

        AggregationOperation project = ctx -> new Document("$project", new Document()
                .append("_id", 0)
                .append("progressionLevel", "$_id")
                .append("totalSessions", 1)
                .append("uniquePatients", new Document("$size", "$uniquePatients"))
                .append("averageScore", 1)
                .append("averageCompletionRate", new Document("$divide",
                        List.of("$completedCount", "$totalSessions")))
                .append("averageRangeOfMotion", 1)
        );

        var pipeline = Aggregation.newAggregation(
                addCompleted,
                group,
                project,
                Aggregation.sort(Sort.by(Sort.Direction.ASC, "progressionLevel"))
        );

        return mongoTemplate.aggregate(pipeline, "game_sessions", GlobalLevelStatsDto.class)
                .getMappedResults();
    }
}
