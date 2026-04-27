package com.rehabiapp.data.application.pipeline;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

import com.rehabiapp.data.domain.model.GameSession;
import com.rehabiapp.data.domain.model.LevelStatistics;
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
 * Agrega GameSession globalmente por nivel de progresion (1-4).
 * Sin filtro de tiempo — opera sobre la coleccion completa.
 */
@Component
public class LevelStatisticsPipeline {

    private final MongoTemplate mongoTemplate;

    public LevelStatisticsPipeline(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public List<LevelStatistics> run() {

        Aggregation agg = newAggregation(
            group("progressionLevel")
                .count().as("totalSessions")
                .addToSet("patientDni").as("patients")
                .avg("score").as("averageScore")
                .avg("durationSeconds").as("averageDuration")
                .avg(ConditionalOperators.when(Criteria.where("completed").is(true))
                        .then(1).otherwise(0)).as("globalCompletionRate")
                .avg("movementMetrics.rangeOfMotionDegrees").as("averageRangeOfMotion"),
            project("totalSessions", "averageScore", "averageDuration",
                    "globalCompletionRate", "averageRangeOfMotion")
                .and("_id").as("progressionLevel")
                .and("patients").size().as("totalPatients")
        );

        AggregationResults<Document> res = mongoTemplate.aggregate(
                agg, GameSession.class, Document.class);

        Instant now = Instant.now();
        return res.getMappedResults().stream().map(d -> {
            LevelStatistics s = new LevelStatistics();
            s.setProgressionLevel(d.getInteger("progressionLevel"));
            s.setTotalSessions(longVal(d.get("totalSessions")));
            s.setTotalPatients(longVal(d.get("totalPatients")));
            s.setAverageScore(doubleVal(d.get("averageScore")));
            s.setAverageDuration(doubleVal(d.get("averageDuration")));
            s.setGlobalCompletionRate(doubleVal(d.get("globalCompletionRate")));
            s.setAverageRangeOfMotion(doubleVal(d.get("averageRangeOfMotion")));
            s.setLastUpdated(now);
            return s;
        }).toList();
    }

    private static Long longVal(Object o) { return o == null ? null : ((Number) o).longValue(); }
    private static Double doubleVal(Object o) { return o == null ? null : ((Number) o).doubleValue(); }
}
