package com.rehabiapp.data.analytics.pipeline;

import com.rehabiapp.data.analytics.dto.TreatmentProgressDto;
import com.rehabiapp.data.util.TreatmentMetricResolver;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Pipeline de progreso terapeutico por (codTrat, parteCuerpo).
 *
 * El pipeline se ejecuta server-side en MongoDB para no traer documentos crudos
 * a memoria. Calcula la metrica relevante segun el prefijo del codTrat:
 * ROM-* -> rangeOfMotionDegrees, VEL-* -> averageSpeed, FUERZA-* -> maxSpeed.
 *
 * Stages:
 *   1. $match: paciente + codTrat presente.
 *   2. $addFields: dia truncado y metricValue segun prefijo.
 *   3. $group por (codTrat, parteCuerpo, dia): promedio de la metrica.
 *   4. $sort por dia ascendente.
 *   5. $group por (codTrat, parteCuerpo): construye entradas, baseline y current.
 *   6. $project final con deltaPorcentaje calculado.
 */
@Component
public class TreatmentProgressPipeline {

    private static final String COLECCION = "game_sessions";
    private static final String NOMBRE_TIMER = "rehabiapp.data.pipeline.duration";

    private final MongoTemplate mongoTemplate;
    private final TreatmentMetricResolver resolver;

    // Registro opcional para no romper tests con contexto minimo.
    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    public TreatmentProgressPipeline(MongoTemplate mongoTemplate, TreatmentMetricResolver resolver) {
        this.mongoTemplate = mongoTemplate;
        this.resolver = resolver;
    }

    public List<TreatmentProgressDto> execute(String patientDni) {
        Timer.Sample sample = meterRegistry != null ? Timer.start(meterRegistry) : null;
        try {
            return ejecutarInterno(patientDni);
        } finally {
            if (sample != null) {
                sample.stop(Timer.builder(NOMBRE_TIMER)
                        .tag("pipeline", "treatment-progress")
                        .register(meterRegistry));
            }
        }
    }

    private List<TreatmentProgressDto> ejecutarInterno(String patientDni) {
        // Stage 1: filtra paciente y descarta sesiones sin codTrat.
        AggregationOperation match = ctx -> new Document("$match",
                new Document("patientDni", patientDni)
                        .append("codTrat", new Document("$ne", null)));

        // Stage 2: dia truncado y metricValue segun prefijo del codTrat.
        AggregationOperation addFields = ctx -> new Document("$addFields", new Document()
                .append("day", new Document("$dateTrunc", new Document()
                        .append("date", "$sessionStart")
                        .append("unit", "day")))
                .append("metricValue", new Document("$switch", new Document()
                        .append("branches", List.of(
                                new Document("case",
                                        new Document("$gt", List.of(
                                                new Document("$indexOfBytes",
                                                        List.of("$codTrat", "ROM-")), -1)))
                                        .append("then", "$movementMetrics.rangeOfMotionDegrees"),
                                new Document("case",
                                        new Document("$gt", List.of(
                                                new Document("$indexOfBytes",
                                                        List.of("$codTrat", "VEL-")), -1)))
                                        .append("then", "$movementMetrics.averageSpeed"),
                                new Document("case",
                                        new Document("$gt", List.of(
                                                new Document("$indexOfBytes",
                                                        List.of("$codTrat", "FUERZA-")), -1)))
                                        .append("then", "$movementMetrics.maxSpeed")
                        ))
                        .append("default", "$movementMetrics.rangeOfMotionDegrees")
                ))
        );

        // Stage 3: promedio diario por (codTrat, parteCuerpo).
        AggregationOperation groupDay = ctx -> new Document("$group", new Document()
                .append("_id", new Document()
                        .append("codTrat", "$codTrat")
                        .append("parteCuerpo", "$parteCuerpo")
                        .append("day", "$day"))
                .append("valorPromedio", new Document("$avg", "$metricValue"))
                .append("patientToken", new Document("$first", "$patientToken"))
                .append("tratamientoNombre", new Document("$first", "$tratamientoNombre"))
        );

        // Stage 4: ordena por dia para que $first/$last sean baseline/current.
        AggregationOperation sortDay = ctx -> new Document("$sort",
                new Document("_id.day", 1));

        // Stage 5: agrupa por tratamiento construyendo serie y limites.
        AggregationOperation groupTrat = ctx -> new Document("$group", new Document()
                .append("_id", new Document()
                        .append("codTrat", "$_id.codTrat")
                        .append("parteCuerpo", "$_id.parteCuerpo"))
                .append("patientToken", new Document("$first", "$patientToken"))
                .append("tratamientoNombre", new Document("$first", "$tratamientoNombre"))
                .append("entradas", new Document("$push", new Document()
                        .append("fecha", "$_id.day")
                        .append("valor", "$valorPromedio")))
                .append("baselineFecha", new Document("$min", "$_id.day"))
                .append("currentFecha", new Document("$max", "$_id.day"))
                .append("baselineValor", new Document("$first", "$valorPromedio"))
                .append("currentValor", new Document("$last", "$valorPromedio"))
        );

        // Stage 6: forma final con deltaPorcentaje seguro frente a baseline cero.
        AggregationOperation project = ctx -> new Document("$project", new Document()
                .append("_id", 0)
                .append("patientToken", 1)
                .append("codTrat", "$_id.codTrat")
                .append("parteCuerpo", "$_id.parteCuerpo")
                .append("tratamientoNombre", 1)
                .append("baseline", new Document()
                        .append("fecha", "$baselineFecha")
                        .append("valor", "$baselineValor"))
                .append("current", new Document()
                        .append("fecha", "$currentFecha")
                        .append("valor", "$currentValor"))
                .append("entradas", 1)
                .append("deltaPorcentaje", new Document("$cond", List.of(
                        new Document("$or", List.of(
                                new Document("$eq", List.of("$baselineValor", 0)),
                                new Document("$eq", List.of("$baselineValor", null)))),
                        null,
                        new Document("$multiply", List.of(
                                new Document("$divide", List.of(
                                        new Document("$subtract",
                                                List.of("$currentValor", "$baselineValor")),
                                        "$baselineValor")),
                                100))
                )))
        );

        var pipeline = Aggregation.newAggregation(match, addFields, groupDay, sortDay, groupTrat, project);
        var results = mongoTemplate.aggregate(pipeline, COLECCION, TreatmentProgressDto.class)
                .getMappedResults();

        // Post-proceso en Java: rellena la etiqueta de la metrica via resolver.
        return results.stream()
                .map(r -> r.withMetricaNombre(resolver.resolverLabel(r.codTrat())))
                .toList();
    }
}
