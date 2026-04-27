package com.rehabiapp.data.application.pipeline;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

import com.rehabiapp.data.domain.model.GameSession;
import com.rehabiapp.data.domain.model.PatientProgress;
import java.time.Instant;
import java.util.List;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

/**
 * Agrega GameSession por paciente, juego, nivel de progresion y semana ISO.
 * Devuelve documentos listos para upsert en patient_progress.
 */
@Component
public class WeeklyGamePipeline {

    private final MongoTemplate mongoTemplate;

    public WeeklyGamePipeline(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public List<PatientProgress> run(Instant since) {

        Aggregation agg = newAggregation(
            match(Criteria.where("sessionStart").gte(since)),
            // Ordena por sessionStart para que $push preserve el orden temporal del ROM trend.
            sort(Sort.Direction.ASC, "sessionStart"),
            project()
                .andInclude("patientDni", "gameId", "progressionLevel",
                            "score", "durationSeconds", "completed", "sessionStart")
                .and("movementMetrics.rangeOfMotionDegrees").as("rom")
                .and(ctx -> new Document("$isoWeekYear", "$sessionStart")).as("isoYear")
                .and(ctx -> new Document("$isoWeek", "$sessionStart")).as("isoWeek"),
            group("patientDni", "gameId", "progressionLevel", "isoYear", "isoWeek")
                .count().as("totalSessions")
                .avg("score").as("averageScore")
                .avg("durationSeconds").as("averageDuration")
                .avg(ConditionalOperators.when(Criteria.where("completed").is(true))
                        .then(1).otherwise(0))
                .as("completionRate")
                .push("rom").as("rangeOfMotionTrend"),
            project("totalSessions", "averageScore", "averageDuration",
                    "completionRate", "rangeOfMotionTrend")
                .and("_id.patientDni").as("patientDni")
                .and("_id.gameId").as("gameId")
                .and("_id.progressionLevel").as("progressionLevel")
                .and("_id.isoYear").as("isoYear")
                .and("_id.isoWeek").as("isoWeek")
        );

        AggregationResults<Document> res = mongoTemplate.aggregate(
                agg, GameSession.class, Document.class);

        Instant now = Instant.now();
        return res.getMappedResults().stream().map(d -> {
            PatientProgress p = new PatientProgress();
            p.setPatientDni(d.getString("patientDni"));
            p.setGameId(d.getString("gameId"));
            p.setProgressionLevel(intVal(d.get("progressionLevel")));
            int y = intVal(d.get("isoYear"));
            int w = intVal(d.get("isoWeek"));
            p.setPeriod(String.format("%04d-W%02d", y, w));
            p.setTotalSessions(intVal(d.get("totalSessions")));
            p.setAverageScore(numberAsDouble(d.get("averageScore")));
            p.setAverageDuration(numberAsDouble(d.get("averageDuration")));
            p.setCompletionRate(numberAsDouble(d.get("completionRate")));
            @SuppressWarnings("unchecked")
            List<Double> trend = (List<Double>) d.get("rangeOfMotionTrend");
            p.setRangeOfMotionTrend(trend);
            p.setLastUpdated(now);
            return p;
        }).toList();
    }

    private static Double numberAsDouble(Object o) { return o == null ? null : ((Number) o).doubleValue(); }
    private static Integer intVal(Object o) { return o == null ? null : ((Number) o).intValue(); }
}
