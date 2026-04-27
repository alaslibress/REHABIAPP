package com.rehabiapp.data.presentation;

import com.rehabiapp.data.application.service.ChartService;
import com.rehabiapp.data.application.service.dto.chart.ChartPayload;
import com.rehabiapp.data.infrastructure.util.ValidationPatterns;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de graficas listas para consumo directo por el ERP de escritorio y el frontend movil.
 */
@RestController
@RequestMapping("/analytics/charts")
@Validated
public class ChartController {

    private final ChartService svc;

    public ChartController(ChartService svc) { this.svc = svc; }

    @GetMapping("/patient/{dni}/rom-progress")
    public ChartPayload romProgress(
            @PathVariable @Pattern(regexp = ValidationPatterns.DNI_REGEX, message = "DNI invalido")
            String dni) {
        return svc.romProgress(dni);
    }

    @GetMapping("/patient/{dni}/score-by-game")
    public ChartPayload scoreByGame(
            @PathVariable @Pattern(regexp = ValidationPatterns.DNI_REGEX, message = "DNI invalido")
            String dni) {
        return svc.scoreByGame(dni);
    }

    @GetMapping("/patient/{dni}/completion-trend")
    public ChartPayload completionTrend(
            @PathVariable @Pattern(regexp = ValidationPatterns.DNI_REGEX, message = "DNI invalido")
            String dni) {
        return svc.completionTrend(dni);
    }

    @GetMapping("/global/level-comparison")
    public ChartPayload levelComparison() {
        return svc.levelComparison();
    }
}
