package com.rehabiapp.data.presentation;

import com.rehabiapp.data.application.service.CohortComparisonService;
import com.rehabiapp.data.application.service.RomTimeSeriesService;
import com.rehabiapp.data.application.service.dto.CohortComparisonResponse;
import com.rehabiapp.data.application.service.dto.PatientAnalyticsResponse;
import com.rehabiapp.data.application.service.dto.RomTimeSeriesResponse;
import com.rehabiapp.data.domain.model.PatientProgress;
import com.rehabiapp.data.domain.repository.PatientProgressRepository;
import com.rehabiapp.data.infrastructure.util.ValidationPatterns;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de analiticas por paciente. Consumido por /api internamente.
 * Protegido por InternalAuthFilter (X-Internal-Key).
 */
@RestController
@RequestMapping("/analytics")
@Validated
public class AnalyticsController {

    private final PatientProgressRepository repo;
    private final RomTimeSeriesService romService;
    private final CohortComparisonService cohortService;

    public AnalyticsController(PatientProgressRepository repo,
                                RomTimeSeriesService romService,
                                CohortComparisonService cohortService) {
        this.repo = repo;
        this.romService = romService;
        this.cohortService = cohortService;
    }

    @GetMapping("/patient/{dni}")
    public ResponseEntity<PatientAnalyticsResponse> patient(
            @PathVariable
            @Pattern(regexp = ValidationPatterns.DNI_REGEX, message = "DNI invalido")
            String dni) {

        List<PatientProgress> all = repo.findByPatientDni(dni);
        if (all.isEmpty()) return ResponseEntity.notFound().build();

        List<PatientProgress> weekly  = all.stream().filter(p -> !"*".equals(p.getGameId())).toList();
        List<PatientProgress> monthly = all.stream().filter(p ->  "*".equals(p.getGameId())).toList();

        return ResponseEntity.ok(new PatientAnalyticsResponse(dni, weekly, monthly));
    }

    @GetMapping("/patient/{dni}/rom-timeseries")
    public ResponseEntity<RomTimeSeriesResponse> romTimeSeries(
            @PathVariable
            @Pattern(regexp = ValidationPatterns.DNI_REGEX, message = "DNI invalido")
            String dni,
            @RequestParam(defaultValue = "week")
            @Pattern(regexp = "day|week|month", message = "bucket must be day, week or month")
            String bucket,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String gameId) {
        return ResponseEntity.ok(romService.compute(dni, bucket, from, to, gameId));
    }

    @GetMapping("/cohort-compare/patient/{dni}")
    public ResponseEntity<CohortComparisonResponse> cohortCompare(
            @PathVariable
            @Pattern(regexp = ValidationPatterns.DNI_REGEX, message = "DNI invalido")
            String dni,
            @RequestParam @NotBlank String disability,
            @RequestParam @NotNull @Min(1) @Max(4) Integer level,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseEntity.ok(cohortService.compute(dni, disability, level, from, to));
    }
}
