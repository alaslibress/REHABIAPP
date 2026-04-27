package com.rehabiapp.data.presentation;

import com.rehabiapp.data.application.service.AnalyticsRefreshJob;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint interno para disparar el refresco de analiticas bajo demanda.
 * Protegido por InternalAuthFilter (X-Internal-Key).
 */
@RestController
@RequestMapping("/internal/analytics")
public class AnalyticsAdminController {

    private final AnalyticsRefreshJob job;

    public AnalyticsAdminController(AnalyticsRefreshJob job) { this.job = job; }

    @PostMapping("/refresh")
    public ResponseEntity<String> refresh() {
        job.refresh();
        return ResponseEntity.accepted().body("refresh triggered");
    }
}
