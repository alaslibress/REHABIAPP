package com.rehabiapp.api.presentation.controller;

import com.rehabiapp.api.application.dto.TelemetriaSesionRequest;
import com.rehabiapp.api.application.service.TelemetriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controlador REST para la ingesta de telemetria de juegos terapeuticos.
 *
 * <p>Consumido por los builds Unity WebGL alojados en AWS S3 + CloudFront.
 * Acepta tambien peticiones con rol SPECIALIST para pruebas y carga manual.</p>
 */
@Tag(name = "Telemetria", description = "Ingesta de telemetria de juegos terapeuticos")
@RestController
@RequestMapping("/api/telemetria")
public class TelemetriaController {

    private final TelemetriaService telemetriaService;

    public TelemetriaController(TelemetriaService telemetriaService) {
        this.telemetriaService = telemetriaService;
    }

    @Operation(summary = "Ingesta una sesion de juego completada por un paciente")
    @PostMapping("/sesion-juego")
    @PreAuthorize("hasAuthority('SCOPE_GAMES_TELEMETRY') or hasRole('SPECIALIST') or hasRole('PATIENT')")
    public ResponseEntity<Map<String, Object>> ingestar(
            @Valid @RequestBody TelemetriaSesionRequest req) {
        Map<String, Object> resultado = telemetriaService.ingestar(req);
        return ResponseEntity.accepted().body(resultado);
    }
}
