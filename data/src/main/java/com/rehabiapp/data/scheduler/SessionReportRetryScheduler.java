package com.rehabiapp.data.scheduler;

import com.rehabiapp.data.domain.document.GameSession;
import com.rehabiapp.data.domain.repository.GameSessionRepository;
import com.rehabiapp.data.internal.client.ApiInternalClient;
import com.rehabiapp.data.markdown.SessionReportMdGenerator;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Reintenta la escritura del informe MD en PostgreSQL para sesiones con reportStatus=PENDING.
 *
 * Se ejecuta cada 5 minutos (300 segundos). Procesa un maximo de 50 sesiones por tick
 * para evitar saturar el API Core. Si una sesion supera 72 intentos (6h SLA) se marca
 * como FAILED y se emite una metrica de alerta.
 */
@Component
public class SessionReportRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(SessionReportRetryScheduler.class);
    private static final int MAX_INTENTOS = 72;

    private final GameSessionRepository repository;
    private final ApiInternalClient apiClient;
    private final SessionReportMdGenerator mdGenerator;
    private final MeterRegistry meterRegistry;

    public SessionReportRetryScheduler(GameSessionRepository repository,
                                       ApiInternalClient apiClient,
                                       SessionReportMdGenerator mdGenerator,
                                       MeterRegistry meterRegistry) {
        this.repository = repository;
        this.apiClient = apiClient;
        this.mdGenerator = mdGenerator;
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void reintentarPendientes() {
        List<GameSession> pendientes = repository.findTop50ByReportStatusOrderByReceivedAtAsc("PENDING");
        if (pendientes.isEmpty()) return;

        log.info("session_report_retry: {} sesiones pendientes", pendientes.size());

        for (GameSession sesion : pendientes) {
            int intentos = sesion.reportAttempts() != null ? sesion.reportAttempts() : 0;

            if (intentos >= MAX_INTENTOS) {
                repository.actualizarReportStatus(sesion.id(), "FAILED", "max intentos alcanzados");
                meterRegistry.counter("rehabiapp.data.session_report.failed").increment();
                log.error("session_report giving_up id={} intentos={}", sesion.id(), intentos);
                continue;
            }

            try {
                var resultado = mdGenerator.render(sesion);
                apiClient.enviarInforme(sesion, resultado.contenido(), resultado.hash());
                repository.actualizarReportStatus(sesion.id(), "OK", null);
                log.debug("session_report reintento_ok id={}", sesion.id());
            } catch (Exception e) {
                String errorCorto = truncar(e.getMessage(), 500);
                repository.incrementarIntentos(sesion.id(), errorCorto);
                log.warn("session_report reintento_fallo id={} intento={} error={}",
                        sesion.id(), intentos + 1, errorCorto);
            }
        }
    }

    private String truncar(String s, int maxLen) {
        if (s == null) return "null";
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }
}
