package com.rehabiapp.data.application.service;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rehabiapp.data.domain.model.GameSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class ExportService {

    private static final String[] CSV_HEADERS = {
        "sessionId", "patientDni", "gameId", "disabilityCode", "progressionLevel",
        "sessionStart", "sessionEnd", "durationSeconds", "score",
        "repetitionsCompleted", "repetitionsTarget",
        "rangeOfMotionDegrees", "averageSpeed", "maxSpeed",
        "completed", "receivedAt"
    };

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final MongoTemplate mongoTemplate;

    public ExportService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void streamCsv(String dni, String from, String to, PrintWriter out) throws IOException {
        Query q = buildQuery(dni, from, to);
        final boolean[] any = {false};
        try (CSVPrinter printer = new CSVPrinter(out,
                CSVFormat.DEFAULT.builder().setHeader(CSV_HEADERS).build())) {
            mongoTemplate.stream(q, GameSession.class).forEach(g -> {
                any[0] = true;
                try {
                    printer.printRecord(
                            g.getSessionId(), g.getPatientDni(), g.getGameId(),
                            g.getDisabilityCode(), g.getProgressionLevel(),
                            g.getSessionStart(), g.getSessionEnd(),
                            g.getDurationSeconds(), g.getScore(),
                            g.getRepetitionsCompleted(), g.getRepetitionsTarget(),
                            g.getMovementMetrics() != null ? g.getMovementMetrics().rangeOfMotionDegrees() : null,
                            g.getMovementMetrics() != null ? g.getMovementMetrics().averageSpeed() : null,
                            g.getMovementMetrics() != null ? g.getMovementMetrics().maxSpeed() : null,
                            g.getCompleted(), g.getReceivedAt());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        if (!any[0]) throw new NotFoundException("no sessions for " + dni);
    }

    public void streamJson(String dni, String from, String to, PrintWriter out) throws IOException {
        Query q = buildQuery(dni, from, to);
        final boolean[] any = {false};
        try (JsonGenerator gen = MAPPER.getFactory().createGenerator(out)) {
            gen.writeStartArray();
            mongoTemplate.stream(q, GameSession.class).forEach(g -> {
                any[0] = true;
                try { gen.writeObject(g); } catch (IOException e) { throw new RuntimeException(e); }
            });
            gen.writeEndArray();
            gen.flush();
        }
        if (!any[0]) throw new NotFoundException("no sessions for " + dni);
    }

    private Query buildQuery(String dni, String from, String to) {
        Instant fromI = from != null ? Instant.parse(from) : Instant.now().minus(365, ChronoUnit.DAYS);
        Criteria c = Criteria.where("patientDni").is(dni)
                .and("sessionStart").gte(fromI);
        if (to != null) c = c.and("sessionStart").lte(Instant.parse(to));
        return new Query(c).with(Sort.by(Sort.Direction.ASC, "sessionStart"));
    }
}
