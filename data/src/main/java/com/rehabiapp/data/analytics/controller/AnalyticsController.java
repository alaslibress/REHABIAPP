package com.rehabiapp.data.analytics.controller;

import com.rehabiapp.data.analytics.dto.*;
import com.rehabiapp.data.analytics.service.AnalyticsService;
import com.rehabiapp.data.analytics.service.MarkdownService;
import com.rehabiapp.data.analytics.service.TreatmentProgressService;
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
    private final TreatmentProgressService treatmentProgressService;
    private final MarkdownService markdownService;

    public AnalyticsController(AnalyticsService analyticsService,
                               TreatmentProgressService treatmentProgressService,
                               MarkdownService markdownService) {
        this.analyticsService = analyticsService;
        this.treatmentProgressService = treatmentProgressService;
        this.markdownService = markdownService;
    }

    // Progreso por tratamiento (baseline vs actual con serie diaria)
    @GetMapping("/patient/{dni}/treatment-progress")
    public ResponseEntity<List<TreatmentProgressDto>> getTreatmentProgress(@PathVariable String dni) {
        return ResponseEntity.ok(treatmentProgressService.getOrCompute(dni));
    }

    // Resumen de la ultima sesion del paciente
    @GetMapping("/patient/{dni}/last-session")
    public ResponseEntity<LastSessionDto> getLastSession(@PathVariable String dni) {
        return ResponseEntity.ok(analyticsService.getLastSession(dni));
    }

    // Markdown del progreso del paciente (cache si es reciente, recompila si stale)
    @GetMapping(value = "/patient/{dni}/markdown", produces = "text/markdown;charset=UTF-8")
    public ResponseEntity<String> getMarkdown(@PathVariable String dni) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/markdown;charset=UTF-8"))
                .body(markdownService.getOrGenerate(dni));
    }

    // Forzar regeneracion del markdown (operacion administrativa)
    @PostMapping("/patient/{dni}/markdown/regenerar")
    public ResponseEntity<Void> regenerarMarkdown(@PathVariable String dni) {
        markdownService.regenerar(dni);
        return ResponseEntity.accepted().build();
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
