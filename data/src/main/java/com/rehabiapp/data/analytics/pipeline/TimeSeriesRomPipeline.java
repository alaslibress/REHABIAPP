package com.rehabiapp.data.analytics.pipeline;

import com.rehabiapp.data.analytics.dto.TimeSeriesRomDto;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Pipeline: serie temporal de rango de movimiento (ROM) a lo largo del periodo de rehabilitacion.
 * Permite visualizar la mejora progresiva del paciente sesion a sesion.
 */
@Component
public class TimeSeriesRomPipeline {

    private final MongoTemplate mongoTemplate;

    public TimeSeriesRomPipeline(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public List<TimeSeriesRomDto> execute(String patientDni) {
        var pipeline = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("patientDni").is(patientDni)
                        .and("movementMetrics.rangeOfMotionDegrees").ne(null)),
                Aggregation.project("patientToken", "gameId", "sessionStart")
                        .and("movementMetrics.rangeOfMotionDegrees").as("rangeOfMotionDegrees"),
                Aggregation.sort(Sort.by(Sort.Direction.ASC, "sessionStart"))
        );

        return mongoTemplate.aggregate(pipeline, "game_sessions", TimeSeriesRomDto.class)
                .getMappedResults();
    }
}
