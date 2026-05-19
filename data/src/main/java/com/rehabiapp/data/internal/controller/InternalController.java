package com.rehabiapp.data.internal.controller;

import com.rehabiapp.data.domain.document.GameSession;
import com.rehabiapp.data.domain.repository.GameSessionRepository;
import com.rehabiapp.data.internal.dto.CheckNewDataResponse;
import com.rehabiapp.data.internal.dto.ProgressSummaryResponse;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Endpoints internos consumidos exclusivamente por el API Core.
 * Permiten polling eficiente del dashboard sin acceso directo a MongoDB.
 */
@RestController
@RequestMapping("/internal/patient")
public class InternalController {

    private final GameSessionRepository gameSessionRepository;
    private final MongoTemplate mongoTemplate;

    public InternalController(GameSessionRepository gameSessionRepository,
                              MongoTemplate mongoTemplate) {
        this.gameSessionRepository = gameSessionRepository;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Comprueba si hay sesiones nuevas para un paciente desde un instante dado.
     * Si {@code since} es null, devuelve el conteo total.
     */
    @GetMapping("/{dni}/check-new-data")
    public ResponseEntity<CheckNewDataResponse> checkNewData(
            @PathVariable String dni,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since
    ) {
        Instant ref = since != null ? since : Instant.EPOCH;
        long count = gameSessionRepository.countByPatientDniAndReceivedAtAfter(dni, ref);
        Instant last = gameSessionRepository.findTopByPatientDniOrderByReceivedAtDesc(dni)
                .map(GameSession::receivedAt)
                .orElse(null);
        return ResponseEntity.ok(new CheckNewDataResponse(count > 0, last, (int) count));
    }

    /**
     * Resumen agregado de progreso del paciente para alimentar la welcome card
     * del movil. Calcula totalSessions, averageScore, improvementRate y
     * lastSessionDate en una unica pasada. {@code improvementRate} compara el
     * score de la primera sesion (orden por sessionStart) con el de la ultima.
     *
     * <p>Esta implementacion carga las sesiones por DNI (alineado con el filtro
     * usado en el resto del modulo: campo Mongo {@code patientDni}, no
     * {@code dniPaciente}). Para volumenes altos se podria mover a $group; aqui
     * el numero de sesiones por paciente es bajo y la simplicidad gana.</p>
     */
    @GetMapping("/{dni}/summary")
    public ResponseEntity<ProgressSummaryResponse> summary(@PathVariable String dni) {
        Query q = Query.query(Criteria.where("patientDni").is(dni));
        List<GameSession> sesiones = mongoTemplate.find(q, GameSession.class);
        if (sesiones.isEmpty()) {
            return ResponseEntity.ok(new ProgressSummaryResponse(0L, null, null, null));
        }

        // Promedio robusto frente a sesiones sin score (null).
        double[] scores = sesiones.stream()
                .map(GameSession::score)
                .filter(s -> s != null)
                .mapToDouble(Double::doubleValue)
                .toArray();
        Double avg = scores.length > 0
                ? java.util.Arrays.stream(scores).average().orElse(0.0)
                : null;

        // Ordenamos por sessionStart para identificar baseline y current.
        List<GameSession> orden = sesiones.stream()
                .filter(s -> s.sessionStart() != null)
                .sorted(Comparator.comparing(GameSession::sessionStart))
                .toList();

        Double improvement = null;
        if (orden.size() >= 2) {
            Double base = orden.get(0).score();
            Double cur = orden.get(orden.size() - 1).score();
            if (base != null && cur != null && base != 0.0) {
                improvement = ((cur - base) / base) * 100.0;
            }
        }

        // lastSessionDate prioriza receivedAt (timestamp del servidor); si falta,
        // cae a sessionStart de la ultima sesion ordenada.
        Instant last = sesiones.stream()
                .map(GameSession::receivedAt)
                .filter(i -> i != null)
                .max(Comparator.naturalOrder())
                .orElseGet(() -> orden.isEmpty() ? null : orden.get(orden.size() - 1).sessionStart());

        return ResponseEntity.ok(new ProgressSummaryResponse(
                sesiones.size(), avg, improvement, last));
    }
}
