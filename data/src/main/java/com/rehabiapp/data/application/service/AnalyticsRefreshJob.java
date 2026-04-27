package com.rehabiapp.data.application.service;

import com.rehabiapp.data.application.pipeline.LevelStatisticsPipeline;
import com.rehabiapp.data.application.pipeline.MonthlyDisabilityPipeline;
import com.rehabiapp.data.application.pipeline.WeeklyGamePipeline;
import com.rehabiapp.data.domain.model.LevelStatistics;
import com.rehabiapp.data.domain.model.PatientProgress;
import com.rehabiapp.data.domain.repository.LevelStatisticsRepository;
import com.rehabiapp.data.domain.repository.PatientProgressRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Refresca patient_progress y level_statistics a partir de game_sessions.
 * Ventana: ultimos 90 dias (cubre reingestas tardias y correcciones).
 * Frecuencia: cada hora (configurable via rehabiapp.analytics.cron).
 */
@Service
public class AnalyticsRefreshJob {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsRefreshJob.class);
    private static final long WINDOW_DAYS = 90L;

    private final WeeklyGamePipeline weekly;
    private final MonthlyDisabilityPipeline monthly;
    private final LevelStatisticsPipeline levels;
    private final PatientProgressRepository progressRepo;
    private final LevelStatisticsRepository levelRepo;
    private final MongoTemplate mongoTemplate;

    public AnalyticsRefreshJob(WeeklyGamePipeline weekly,
                               MonthlyDisabilityPipeline monthly,
                               LevelStatisticsPipeline levels,
                               PatientProgressRepository progressRepo,
                               LevelStatisticsRepository levelRepo,
                               MongoTemplate mongoTemplate) {
        this.weekly = weekly;
        this.monthly = monthly;
        this.levels = levels;
        this.progressRepo = progressRepo;
        this.levelRepo = levelRepo;
        this.mongoTemplate = mongoTemplate;
    }

    @Scheduled(cron = "${rehabiapp.analytics.cron:0 5 * * * *}")
    public void refresh() {
        Instant since = Instant.now().minus(WINDOW_DAYS, ChronoUnit.DAYS);
        log.info("Iniciando refresco de analiticas desde {}", since);

        long t0 = System.currentTimeMillis();
        upsertProgress(weekly.run(since));
        upsertProgress(monthly.run(since));
        upsertLevelStatistics(levels.run());
        log.info("Refresco completado en {} ms", System.currentTimeMillis() - t0);
    }

    private void upsertProgress(List<PatientProgress> rows) {
        for (PatientProgress p : rows) {
            Query q = Query.query(Criteria.where("patientDni").is(p.getPatientDni())
                    .and("gameId").is(p.getGameId())
                    .and("progressionLevel").is(p.getProgressionLevel())
                    .and("period").is(p.getPeriod())
                    .and("disabilityCode").is(p.getDisabilityCode()));
            Update u = new Update()
                    .set("totalSessions", p.getTotalSessions())
                    .set("averageScore", p.getAverageScore())
                    .set("averageDuration", p.getAverageDuration())
                    .set("completionRate", p.getCompletionRate())
                    .set("rangeOfMotionTrend", p.getRangeOfMotionTrend())
                    .set("disabilityCode", p.getDisabilityCode())
                    .set("lastUpdated", p.getLastUpdated());
            mongoTemplate.upsert(q, u, PatientProgress.class);
        }
    }

    private void upsertLevelStatistics(List<LevelStatistics> rows) {
        for (LevelStatistics s : rows) {
            Query q = Query.query(Criteria.where("progressionLevel").is(s.getProgressionLevel()));
            Update u = new Update()
                    .set("totalSessions", s.getTotalSessions())
                    .set("totalPatients", s.getTotalPatients())
                    .set("averageScore", s.getAverageScore())
                    .set("averageDuration", s.getAverageDuration())
                    .set("globalCompletionRate", s.getGlobalCompletionRate())
                    .set("averageRangeOfMotion", s.getAverageRangeOfMotion())
                    .set("lastUpdated", s.getLastUpdated());
            mongoTemplate.upsert(q, u, LevelStatistics.class);
        }
    }
}
