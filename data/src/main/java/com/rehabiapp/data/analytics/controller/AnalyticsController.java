package com.rehabiapp.data.analytics.controller;

import com.rehabiapp.data.analytics.dto.*;
import com.rehabiapp.data.analytics.service.AnalyticsService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Endpoints de analitica para consumo interno del API Core y del ERP de escritorio.
 * Todas las respuestas usan tokens anonimizados, nunca DNI en claro.
 */
@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    // Analitica completa de un paciente (semanal + mensual + ROM + cohorte)
    @GetMapping("/patient/{dni}")
    public ResponseEntity<PatientAnalyticsResponse> getPatientAnalytics(
            @PathVariable String dni
    ) {
        return ResponseEntity.ok(analyticsService.getPatientAnalytics(dni));
    }

    // Estadisticas globales por nivel de progresion (para dashboards)
    @GetMapping("/global/level")
    public ResponseEntity<List<GlobalLevelStatsDto>> getGlobalStats() {
        return ResponseEntity.ok(analyticsService.getGlobalStats());
    }

    // Serie temporal de ROM de un paciente (grafico de evolucion de movilidad)
    @GetMapping("/patient/{dni}/rom")
    public ResponseEntity<List<TimeSeriesRomDto>> getRomTimeSeries(@PathVariable String dni) {
        return ResponseEntity.ok(analyticsService.getRomTimeSeries(dni));
    }

    // Exportacion JSON del paciente para descarga del fisioterapeuta
    @GetMapping(value = "/patient/{dni}/export", params = "!format")
    public ResponseEntity<PatientAnalyticsResponse> exportPatientDataJson(
            @PathVariable String dni
    ) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"patient-analytics.json\"")
                .body(analyticsService.getPatientAnalytics(dni));
    }

    // Exportacion CSV del progreso semanal del paciente para descarga del fisioterapeuta
    @GetMapping(value = "/patient/{dni}/export", params = "format=csv")
    public ResponseEntity<byte[]> exportPatientDataCsv(@PathVariable String dni) {
        var analytics = analyticsService.getPatientAnalytics(dni);
        byte[] csv = buildCsv(analytics.weeklyProgress());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"patient-analytics.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    private byte[] buildCsv(List<WeeklyProgressDto> rows) {
        var sb = new StringBuilder();
        sb.append("patientToken,gameId,progressionLevel,week,totalSessions,averageScore,completionRate,averageRangeOfMotion\n");
        for (var r : rows) {
            sb.append("%s,%s,%d,%s,%d,%.2f,%.4f,%.2f\n".formatted(
                    r.patientToken(), r.gameId(), r.progressionLevel(), r.week(),
                    r.totalSessions(), nvl(r.averageScore()), nvl(r.completionRate()),
                    nvl(r.averageRangeOfMotion())));
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private double nvl(Double v) {
        return v != null ? v : 0.0;
    }
}
