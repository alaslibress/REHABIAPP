package com.rehabiapp.data.application.pipeline;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

import com.rehabiapp.data.application.service.dto.RomTimeSeriesPoint;
import com.rehabiapp.data.domain.model.GameSession;
import java.time.Instant;
import java.util.List;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

/**
 * Agrega rangeOfMotionDegrees por cubo temporal (day | week | month) para un paciente.
 */
@Component
public class RomTimeSeriesPipeline {

    private final MongoTemplate mongoTemplate;

    public RomTimeSeriesPipeline(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public List<RomTimeSeriesPoint> run(String dni, String bucket, Instant from, Instant to,
                                        String gameId) {
        Criteria c = Criteria.where("patientDni").is(dni)
                .and("sessionStart").gte(from).lte(to);
        if (gameId != null) c = c.and("gameId").is(gameId);

        Document bucketExpr = bucketKeyExpr(bucket);

        Aggregation agg = newAggregation(
            match(c),
            sort(Sort.Direction.ASC, "sessionStart"),
            project()
                .and("movementMetrics.rangeOfMotionDegrees").as("rom")
                .and(ctx -> bucketExpr).as("bucketKey"),
            group("bucketKey")
                .count().as("sampleSize")
                .avg("rom").as("romAvg")
                .max("rom").as("romMax")
                .min("rom").as("romMin"),
            project("sampleSize", "romAvg", "romMax", "romMin")
                .and("_id").as("date"),
            sort(Sort.Direction.ASC, "date")
        );

        AggregationResults<Document> res = mongoTemplate.aggregate(
                agg, GameSession.class, Document.class);

        return res.getMappedResults().stream().map(d -> new RomTimeSeriesPoint(
                d.getString("date"),
                dbl(d.get("romAvg")),
                dbl(d.get("romMax")),
                dbl(d.get("romMin")),
                lng(d.get("sampleSize"))
        )).toList();
    }

    private static Document bucketKeyExpr(String bucket) {
        return switch (bucket) {
            case "day" -> new Document("$dateToString",
                    new Document("format", "%Y-%m-%d").append("date", "$sessionStart"));
            case "month" -> new Document("$dateToString",
                    new Document("format", "%Y-%m").append("date", "$sessionStart"));
            default -> // week
                    new Document("$concat", List.of(
                        new Document("$toString", new Document("$isoWeekYear", "$sessionStart")),
                        "-W",
                        new Document("$toString", new Document("$isoWeek", "$sessionStart"))
                    ));
        };
    }

    private static Double dbl(Object o) { return o == null ? null : ((Number) o).doubleValue(); }
    private static Long lng(Object o) { return o == null ? null : ((Number) o).longValue(); }
}
