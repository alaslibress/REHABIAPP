package com.rehabiapp.data.presentation;

import com.rehabiapp.data.application.service.GameSessionIngestService;
import com.rehabiapp.data.application.service.dto.GameSessionIngestRequest;
import com.rehabiapp.data.application.service.dto.GameSessionIngestResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint interno de ingesta de telemetria. Consumido SOLO por el Core API.
 * No expuesto externamente (la NetworkPolicy lo garantiza en K8s).
 */
@RestController
@RequestMapping("/ingest")
public class IngestController {

    private final GameSessionIngestService service;

    public IngestController(GameSessionIngestService service) {
        this.service = service;
    }

    @PostMapping("/game-session")
    public ResponseEntity<GameSessionIngestResponse> ingestGameSession(
            @Valid @RequestBody GameSessionIngestRequest request) {
        GameSessionIngestResponse body = service.ingest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
}
