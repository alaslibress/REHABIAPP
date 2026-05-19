package com.rehabiapp.data.scheduler;

import com.rehabiapp.data.analytics.service.MarkdownService;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Regenera periodicamente los markdowns cuyos pacientes tienen sesiones nuevas
 * desde la ultima generacion. Limita a 50 pacientes por ejecucion para no
 * saturar el pipeline.
 *
 * El cron se ejecuta cada 15 minutos en UTC (alineado con TTL de cache).
 * Usa una agregacion server-side con $lookup contra patient_markdown.
 */
@Component
public class MarkdownRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(MarkdownRefreshScheduler.class);
    private static final int LIMITE_PACIENTES = 50;

    private final MarkdownService markdownService;
    private final MongoTemplate mongoTemplate;

    public MarkdownRefreshScheduler(MarkdownService markdownService, MongoTemplate mongoTemplate) {
        this.markdownService = markdownService;
        this.mongoTemplate = mongoTemplate;
    }

    @Scheduled(cron = "0 */15 * * * *", zone = "UTC")
    public void refresh() {
        log.info("Iniciando refresh masivo de markdowns");
        long inicio = System.currentTimeMillis();

        try {
            List<PendienteDto> pendientes = buscarPendientes();
            for (PendienteDto p : pendientes) {
                try {
                    markdownService.regenerar(p.patientDni());
                } catch (Exception e) {
                    log.error("Fallo regenerando MD para token={}", p.patientToken(), e);
                }
            }
            log.info("Refresh markdowns completado: {} regenerados en {}ms",
                    pendientes.size(), System.currentTimeMillis() - inicio);
        } catch (Exception e) {
            log.error("Error global en refresh de markdowns", e);
        }
    }

    /**
     * Calcula los pacientes con sesiones posteriores a la ultima generacion del MD.
     * Pipeline:
     *   1. $group game_sessions por (patientDni, patientToken) -> max(receivedAt).
     *   2. $lookup contra patient_markdown.
     *   3. $match maxReceived > markdown.updatedAt o markdown inexistente.
     *   4. $limit 50.
     */
    private List<PendienteDto> buscarPendientes() {
        AggregationOperation group = ctx -> new Document("$group", new Document()
                .append("_id", new Document()
                        .append("patientDni", "$patientDni")
                        .append("patientToken", "$patientToken"))
                .append("maxReceived", new Document("$max", "$receivedAt"))
        );

        AggregationOperation lookup = ctx -> new Document("$lookup", new Document()
                .append("from", "patient_markdown")
                .append("localField", "_id.patientDni")
                .append("foreignField", "patientDni")
                .append("as", "md")
        );

        AggregationOperation matchStale = ctx -> new Document("$match", new Document()
                .append("$expr", new Document("$or", List.of(
                        new Document("$eq", List.of(new Document("$size", "$md"), 0)),
                        new Document("$gt", List.of(
                                "$maxReceived",
                                new Document("$ifNull", List.of(
                                        new Document("$arrayElemAt", List.of("$md.updatedAt", 0)),
                                        java.util.Date.from(java.time.Instant.EPOCH)))))
                )))
        );

        AggregationOperation project = ctx -> new Document("$project", new Document()
                .append("_id", 0)
                .append("patientDni", "$_id.patientDni")
                .append("patientToken", "$_id.patientToken")
                .append("maxReceived", 1)
        );

        var pipeline = Aggregation.newAggregation(
                group,
                lookup,
                matchStale,
                project,
                Aggregation.limit(LIMITE_PACIENTES)
        );

        return mongoTemplate.aggregate(pipeline, "game_sessions", PendienteDto.class)
                .getMappedResults();
    }

    /**
     * DTO interno para mapear el resultado de la agregacion del scheduler.
     */
    public record PendienteDto(String patientDni, String patientToken) {}
}
