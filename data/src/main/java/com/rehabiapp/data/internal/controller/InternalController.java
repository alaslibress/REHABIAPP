package com.rehabiapp.data.internal.controller;

import com.rehabiapp.data.domain.document.GameSession;
import com.rehabiapp.data.domain.repository.GameSessionRepository;
import com.rehabiapp.data.internal.dto.CheckNewDataResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Endpoints internos consumidos exclusivamente por el API Core.
 * Permiten polling eficiente del dashboard sin acceso directo a MongoDB.
 */
@RestController
@RequestMapping("/internal/patient")
public class InternalController {

    private final GameSessionRepository gameSessionRepository;

    public InternalController(GameSessionRepository gameSessionRepository) {
        this.gameSessionRepository = gameSessionRepository;
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
}
