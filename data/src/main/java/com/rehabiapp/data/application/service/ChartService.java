package com.rehabiapp.data.application.service;

import com.rehabiapp.data.application.service.dto.RomTimeSeriesPoint;
import com.rehabiapp.data.application.service.dto.chart.ChartPayload;
import com.rehabiapp.data.application.service.dto.chart.ChartSeries;
import com.rehabiapp.data.domain.model.LevelStatistics;
import com.rehabiapp.data.domain.model.PatientProgress;
import com.rehabiapp.data.domain.repository.LevelStatisticsRepository;
import com.rehabiapp.data.domain.repository.PatientProgressRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ChartService {

    private final RomTimeSeriesService romService;
    private final PatientProgressRepository progressRepo;
    private final LevelStatisticsRepository levelRepo;

    public ChartService(RomTimeSeriesService romService,
                        PatientProgressRepository progressRepo,
                        LevelStatisticsRepository levelRepo) {
        this.romService = romService;
        this.progressRepo = progressRepo;
        this.levelRepo = levelRepo;
    }

    public ChartPayload romProgress(String dni) {
        List<RomTimeSeriesPoint> pts = romService.compute(dni, "week", null, null, null).points();
        List<String> x = pts.stream().map(RomTimeSeriesPoint::date).toList();
        List<Double> y = pts.stream().map(RomTimeSeriesPoint::romAvg).toList();
        return new ChartPayload("rom_progress", "Range of motion progress",
                "Week", "Degrees", "category", "number",
                List.of(new ChartSeries("Patient", x, y)));
    }

    public ChartPayload scoreByGame(String dni) {
        List<PatientProgress> rows = progressRepo.findByPatientDni(dni).stream()
                .filter(p -> !"*".equals(p.getGameId()))
                .toList();

        Map<String, List<Double>> byGame = new LinkedHashMap<>();
        for (PatientProgress p : rows) {
            byGame.computeIfAbsent(p.getGameId(), k -> new ArrayList<>())
                  .add(p.getAverageScore());
        }

        List<String> x = new ArrayList<>(byGame.keySet());
        List<Double> y = x.stream()
                .map(g -> byGame.get(g).stream().mapToDouble(Double::doubleValue).average().orElse(0))
                .toList();

        return new ChartPayload("score_by_game", "Average score per game (last 90 days)",
                "Game", "Score", "category", "number",
                List.of(new ChartSeries("Patient", x, y)));
    }

    public ChartPayload completionTrend(String dni) {
        List<PatientProgress> rows = progressRepo.findByPatientDniOrderByPeriodAsc(dni).stream()
                .filter(p -> !"*".equals(p.getGameId()))
                .toList();

        List<String> x = rows.stream().map(PatientProgress::getPeriod).toList();
        List<Double> y = rows.stream().map(p -> p.getCompletionRate() != null ? p.getCompletionRate() : 0.0).toList();

        return new ChartPayload("completion_trend", "Weekly completion rate",
                "Period", "Completion rate", "category", "number",
                List.of(new ChartSeries("Patient", x, y)));
    }

    public ChartPayload levelComparison() {
        List<LevelStatistics> stats = levelRepo.findAllByOrderByProgressionLevelAsc();

        List<String> x = stats.stream().map(s -> String.valueOf(s.getProgressionLevel())).toList();
        List<Double> y = stats.stream().map(s -> s.getAverageScore() != null ? s.getAverageScore() : 0.0).toList();

        return new ChartPayload("level_comparison", "Global average score per progression level",
                "Level", "Average score", "category", "number",
                List.of(new ChartSeries("Global", x, y)));
    }
}
