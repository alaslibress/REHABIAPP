package com.rehabiapp.data.analytics.pipeline;

import com.rehabiapp.data.analytics.dto.WeeklyProgressDto;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Pipeline: progreso semanal por paciente y juego.
 * Agrupa por semana ISO, calcula score promedio, tasa de completado y ROM media.
 * Todos los resultados usan patientToken (anonimizado).
 */
@Component
public class WeeklyProgressPipeline {

    private final MongoTemplate mongoTemplate;

    public WeeklyProgressPipeline(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public List<WeeklyProgressDto> execute(String patientDni) {
        // Agrega semana ISO y flag entero de sesion completada
        AggregationOperation addFields = ctx -> new Document("$addFields", new Document()
                .append("week", new Document("$concat", List.of(
                        new Document("$toString", new Document("$isoWeekYear", "$sessionStart")),
                        "-W",
                        new Document("$cond", List.of(
                                new Document("$lt", List.of(
                                        new Document("$strLenBytes",
                                                new Document("$toString",
                                                        new Document("$isoWeek", "$sessionStart"))),
                                        2)),
                                new Document("$concat", List.of("0",
                                        new Document("$toString",
                                                new Document("$isoWeek", "$sessionStart")))),
                                new Document("$toString",
                                        new Document("$isoWeek", "$sessionStart"))
                        ))
                )))
                .append("completedInt", new Document("$cond", List.of(
                        new Document("$eq", List.of("$completed", true)), 1, 0)))
        );

        AggregationOperation group = ctx -> new Document("$group", new Document()
                .append("_id", new Document()
                        .append("patientToken", "$patientToken")
                        .append("gameId", "$gameId")
                        .append("progressionLevel", "$progressionLevel")
                        .append("week", "$week"))
                .append("totalSessions", new Document("$sum", 1))
                .append("averageScore", new Document("$avg", "$score"))
                .append("completedCount", new Document("$sum", "$completedInt"))
                .append("averageRangeOfMotion",
                        new Document("$avg", "$movementMetrics.rangeOfMotionDegrees"))
        );

        AggregationOperation project = ctx -> new Document("$project", new Document()
                .append("_id", 0)
                .append("patientToken", "$_id.patientToken")
                .append("gameId", "$_id.gameId")
                .append("progressionLevel", "$_id.progressionLevel")
                .append("week", "$_id.week")
                .append("totalSessions", 1)
                .append("averageScore", 1)
                .append("completionRate", new Document("$divide",
                        List.of("$completedCount", "$totalSessions")))
                .append("averageRangeOfMotion", 1)
        );

        var pipeline = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("patientDni").is(patientDni)),
                addFields,
                group,
                project,
                Aggregation.sort(Sort.by(Sort.Direction.ASC, "week"))
        );

        return mongoTemplate.aggregate(pipeline, "game_sessions", WeeklyProgressDto.class)
                .getMappedResults();
    }
}
