package com.rehabiapp.data.scheduler;

import com.rehabiapp.data.domain.document.PatientProgress;
import com.rehabiapp.data.domain.repository.PatientProgressRepository;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Refresca la coleccion patient_progress cada noche a las 02:00 UTC.
 * Agrega todas las sesiones de juego y calcula metricas por paciente,
 * juego, discapacidad, nivel de progresion y semana ISO.
 * La operacion es idempotente: borra y reconstruye cada registro del periodo actual.
 */
@Component
public class PatientProgressRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(PatientProgressRefreshScheduler.class);

    private final MongoTemplate mongoTemplate;
    private final PatientProgressRepository repository;

    public PatientProgressRefreshScheduler(MongoTemplate mongoTemplate,
                                           PatientProgressRepository repository) {
        this.mongoTemplate = mongoTemplate;
        this.repository = repository;
    }

    @Scheduled(cron = "0 0 2 * * *", zone = "UTC")
    public void refresh() {
        log.info("Iniciando refresh de patient_progress");
        long inicio = System.currentTimeMillis();

        try {
            List<PatientProgress> progresosCalculados = calcularProgresosSemanales();
            repository.deleteAll();
            repository.saveAll(progresosCalculados);
            log.info("Refresh completado: {} registros en {}ms",
                    progresosCalculados.size(), System.currentTimeMillis() - inicio);
        } catch (Exception e) {
            log.error("Error en refresh de patient_progress", e);
        }
    }

    private List<PatientProgress> calcularProgresosSemanales() {
        // Stage: agrega semana ISO y flag de sesion completada
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
                        .append("patientDni", "$patientDni")
                        .append("patientToken", "$patientToken")
                        .append("gameId", "$gameId")
                        .append("disabilityId", "$disabilityId")
                        .append("progressionLevel", "$progressionLevel")
                        .append("period", "$week"))
                .append("totalSessions", new Document("$sum", 1))
                .append("averageScore", new Document("$avg", "$score"))
                .append("averageDuration", new Document("$avg", "$durationSeconds"))
                .append("completedCount", new Document("$sum", "$completedInt"))
                .append("rangeOfMotionTrend",
                        new Document("$push", "$movementMetrics.rangeOfMotionDegrees"))
        );

        AggregationOperation project = ctx -> new Document("$project", new Document()
                .append("_id", 0)
                .append("patientDni", "$_id.patientDni")
                .append("patientToken", "$_id.patientToken")
                .append("gameId", "$_id.gameId")
                .append("disabilityId", "$_id.disabilityId")
                .append("progressionLevel", "$_id.progressionLevel")
                .append("period", "$_id.period")
                .append("totalSessions", 1)
                .append("averageScore", 1)
                .append("averageDuration", 1)
                .append("completionRate", new Document("$divide",
                        List.of("$completedCount", "$totalSessions")))
                .append("rangeOfMotionTrend", 1)
                .append("lastUpdated", new Document("$$NOW", new Document()))
        );

        var pipeline = Aggregation.newAggregation(addFields, group, project);

        return mongoTemplate.aggregate(pipeline, "game_sessions", PatientProgress.class)
                .getMappedResults()
                .stream()
                .map(pp -> new PatientProgress(
                        null,
                        pp.patientDni(),
                        pp.patientToken(),
                        pp.gameId(),
                        pp.disabilityId(),
                        pp.progressionLevel(),
                        pp.period(),
                        pp.totalSessions(),
                        pp.averageScore(),
                        pp.averageDuration(),
                        pp.completionRate(),
                        pp.rangeOfMotionTrend(),
                        Instant.now()
                ))
                .toList();
    }
}
