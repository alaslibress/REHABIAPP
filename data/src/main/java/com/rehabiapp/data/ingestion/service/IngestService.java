package com.rehabiapp.data.ingestion.service;

import com.rehabiapp.data.domain.document.GameSession;
import com.rehabiapp.data.domain.repository.GameSessionRepository;
import com.rehabiapp.data.ingestion.dto.GameSessionIngestionRequest;
import com.rehabiapp.data.ingestion.schema.MetricSchemaRegistry;
import com.rehabiapp.data.internal.client.ApiInternalClient;
import com.rehabiapp.data.markdown.SessionReportMdGenerator;
import com.rehabiapp.data.observability.DataMetrics;
import com.rehabiapp.data.util.PseudonymUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

@Service
public class IngestService {

    private static final Logger log = LoggerFactory.getLogger(IngestService.class);

    private final GameSessionRepository repository;
    private final PseudonymUtil pseudonymUtil;
    private final MetricSchemaRegistry schemaRegistry;
    private final SessionReportMdGenerator mdGenerator;
    private final ApiInternalClient apiClient;

    @Autowired(required = false)
    private DataMetrics metrics_;

    public IngestService(GameSessionRepository repository,
                         PseudonymUtil pseudonymUtil,
                         MetricSchemaRegistry schemaRegistry,
                         SessionReportMdGenerator mdGenerator,
                         ApiInternalClient apiClient) {
        this.repository = repository;
        this.pseudonymUtil = pseudonymUtil;
        this.schemaRegistry = schemaRegistry;
        this.mdGenerator = mdGenerator;
        this.apiClient = apiClient;
    }

    public GameSession ingestSession(GameSessionIngestionRequest req) {
        // Validacion semantica: schema de metricas por gameId
        // Solo valida si el gameId tiene schema registrado — juegos sin schema pasan sin restriccion
        if (schemaRegistry.tieneSchema(req.gameId())) {
            schemaRegistry.validar(req.gameId(), req.rawMetrics());
        }

        // Calcular metricsHash para idempotencia (orden canonico de claves)
        String metricsHash = hashCanonicoRawMetrics(req.rawMetrics());

        // Idempotencia: si ya existe la sesion con el mismo metricsHash devolver el doc existente
        var existente = repository.findByPatientDniAndGameIdAndSessionStartAndMetricsHash(
                req.patientDni(), req.gameId(), req.sessionStart(), metricsHash);
        if (existente.isPresent()) {
            log.debug("sesion_duplicada patientDni={} gameId={} hash={}", req.patientDni(), req.gameId(), metricsHash);
            throw new DuplicateSessionException("Sesion ya ingestada: paciente=%s juego=%s hash=%s"
                    .formatted(req.patientDni(), req.gameId(), metricsHash));
        }

        // Deteccion de duplicados por combinacion sin hash (compatibilidad con clientes sin metricsHash)
        if (repository.existsByPatientDniAndGameIdAndSessionStart(
                req.patientDni(), req.gameId(), req.sessionStart())) {
            throw new DuplicateSessionException(
                    "Sesion duplicada: paciente=%s juego=%s inicio=%s"
                            .formatted(req.patientDni(), req.gameId(), req.sessionStart()));
        }

        // Shim de compatibilidad: proyectar rawMetrics en movementMetrics si aplica
        GameSession.MovementMetrics shimMetrics = proyectarShim(req.rawMetrics());

        var sesion = new GameSession(
                null,
                req.patientDni(),
                req.gameId(),
                req.disabilityId(),
                req.progressionLevel(),
                req.sessionStart(),
                req.sessionEnd(),
                req.durationSeconds(),
                req.score(),
                req.repetitionsCompleted(),
                req.repetitionsTarget(),
                req.rawMetrics(),
                req.schemaVersion() != null ? req.schemaVersion() : "v1",
                metricsHash,
                req.completed(),
                Instant.now(),
                pseudonymUtil.tokenize(req.patientDni()),
                req.codTrat(),
                req.parteCuerpo(),
                req.tratamientoNombre(),
                "PENDING",
                0,
                null,
                shimMetrics
        );

        var guardada = repository.save(sesion);

        if (metrics_ != null) {
            metrics_.incIngest();
        }

        // Dual-write: generar Markdown y enviar al API Core para persistir en Postgres
        try {
            var resultado = mdGenerator.render(guardada);
            apiClient.enviarInforme(guardada, resultado.contenido(), resultado.hash());
            repository.actualizarReportStatus(guardada.id(), "OK", null);
        } catch (Exception e) {
            // Fallo no bloquea la ingesta — el scheduler reintentara
            String errorCorto = e.getMessage() != null
                    ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 500))
                    : "error desconocido";
            repository.actualizarReportStatus(guardada.id(), "PENDING", errorCorto);
            log.warn("session_report_fallo id={} error={}", guardada.id(), errorCorto);
        }

        return guardada;
    }

    /**
     * Proyecta rawMetrics en MovementMetrics para compatibilidad con pipelines existentes.
     * Si rawMetrics contiene las claves estandar ROM/velocidad, las mapea al shim.
     */
    @SuppressWarnings("deprecation")
    private GameSession.MovementMetrics proyectarShim(Map<String, Object> rawMetrics) {
        if (rawMetrics == null) return null;
        Double rom = extractDouble(rawMetrics, "rangeOfMotionDegrees");
        Double avgSpd = extractDouble(rawMetrics, "averageSpeed");
        Double maxSpd = extractDouble(rawMetrics, "maxSpeed");
        if (rom == null && avgSpd == null && maxSpd == null) return null;
        return new GameSession.MovementMetrics(rom, avgSpd, maxSpd);
    }

    private Double extractDouble(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof Number n) return n.doubleValue();
        return null;
    }

    /**
     * SHA-256 del JSON canonico de rawMetrics (claves ordenadas alfabeticamente).
     * Garantiza que el mismo payload produce siempre el mismo hash.
     */
    private String hashCanonicoRawMetrics(Map<String, Object> rawMetrics) {
        try {
            // TreeMap ordena las claves canonicamente
            String repr = new TreeMap<>(rawMetrics).toString();
            var md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(repr.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
