package com.rehabiapp.data.analytics.service;

import com.rehabiapp.data.analytics.dto.*;
import com.rehabiapp.data.analytics.pipeline.*;
import com.rehabiapp.data.domain.repository.GameSessionRepository;
import com.rehabiapp.data.util.PseudonymUtil;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orquesta los pipelines de analitica. Todas las respuestas usan patientToken (anonimizado).
 * El acceso a datos no anonimizados debe registrarse en el audit trail del API Core.
 */
@Service
public class AnalyticsService {

    private final WeeklyProgressPipeline weeklyPipeline;
    private final MonthlyProgressPipeline monthlyPipeline;
    private final GlobalLevelStatsPipeline globalPipeline;
    private final TimeSeriesRomPipeline romPipeline;
    private final CohortComparisonPipeline cohortPipeline;
    private final PseudonymUtil pseudonymUtil;
    private final GameSessionRepository gameSessionRepository;

    public AnalyticsService(WeeklyProgressPipeline weeklyPipeline,
                            MonthlyProgressPipeline monthlyPipeline,
                            GlobalLevelStatsPipeline globalPipeline,
                            TimeSeriesRomPipeline romPipeline,
                            CohortComparisonPipeline cohortPipeline,
                            PseudonymUtil pseudonymUtil,
                            GameSessionRepository gameSessionRepository) {
        this.weeklyPipeline = weeklyPipeline;
        this.monthlyPipeline = monthlyPipeline;
        this.globalPipeline = globalPipeline;
        this.romPipeline = romPipeline;
        this.cohortPipeline = cohortPipeline;
        this.pseudonymUtil = pseudonymUtil;
        this.gameSessionRepository = gameSessionRepository;
    }

    /**
     * Devuelve un resumen anonimizado de la ultima sesion del paciente.
     * Si no existe ninguna sesion, devuelve un DTO con campos a null pero
     * con el patientToken correctamente derivado.
     */
    public LastSessionDto getLastSession(String patientDni) {
        String token = pseudonymUtil.tokenize(patientDni);
        return gameSessionRepository.findTopByPatientDniOrderByReceivedAtDesc(patientDni)
                .map(s -> new LastSessionDto(
                        token,
                        s.gameId(),
                        s.codTrat(),
                        s.sessionStart(),
                        s.score()))
                .orElse(new LastSessionDto(token, null, null, null, null));
    }

    public PatientAnalyticsResponse getPatientAnalytics(String patientDni) {
        String token = pseudonymUtil.tokenize(patientDni);
        return new PatientAnalyticsResponse(
                token,
                weeklyPipeline.execute(patientDni),
                monthlyPipeline.execute(patientDni),
                romPipeline.execute(patientDni),
                cohortPipeline.buildComparisons(patientDni)
        );
    }

    public List<GlobalLevelStatsDto> getGlobalStats() {
        return globalPipeline.execute();
    }

    public List<WeeklyProgressDto> getWeeklyProgress(String patientDni) {
        return weeklyPipeline.execute(patientDni);
    }

    public List<TimeSeriesRomDto> getRomTimeSeries(String patientDni) {
        return romPipeline.execute(patientDni);
    }
}
