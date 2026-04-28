package com.rehabiapp.data.ingestion.controller;

import com.rehabiapp.data.ingestion.dto.GameSessionIngestionRequest;
import com.rehabiapp.data.ingestion.service.DuplicateSessionException;
import com.rehabiapp.data.ingestion.service.IngestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/ingest")
public class IngestController {

    private final IngestService ingestService;

    public IngestController(IngestService ingestService) {
        this.ingestService = ingestService;
    }

    @PostMapping("/game-session")
    public ResponseEntity<Map<String, String>> ingestGameSession(
            @Valid @RequestBody GameSessionIngestionRequest request
    ) {
        var session = ingestService.ingestSession(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", session.id(), "status", "accepted"));
    }

    @ExceptionHandler(DuplicateSessionException.class)
    public ResponseEntity<Map<String, String>> handleDuplicate(DuplicateSessionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "duplicate_session", "message", ex.getMessage()));
    }
}
