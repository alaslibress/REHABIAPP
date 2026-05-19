package com.rehabiapp.data.markdown;

import com.rehabiapp.data.analytics.dto.TreatmentProgressDto;
import com.rehabiapp.data.analytics.service.TreatmentProgressService;
import com.rehabiapp.data.domain.document.GameSession;
import com.rehabiapp.data.domain.repository.GameSessionRepository;
import com.rehabiapp.data.util.PseudonymUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Genera el documento Markdown del progreso del paciente.
 *
 * El contenido del MD esta totalmente anonimizado: no aparece DNI,
 * solo el {@code patientToken}. Las secciones son:
 *   1. Cabecera con token y timestamp.
 *   2. Resumen ejecutivo (tabla de tratamientos).
 *   3. Detalle por tratamiento con baseline/actual/delta.
 *   4. Tabla con las ultimas sesiones.
 *   5. Notas para analisis automatico por IA.
 */
@Service
public class MdGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(MdGeneratorService.class);
    private static final int MAX_ULTIMAS_SESIONES = 20;

    private final TreatmentProgressService progressService;
    private final GameSessionRepository sessionRepository;
    private final PseudonymUtil pseudonymUtil;

    public MdGeneratorService(TreatmentProgressService progressService,
                              GameSessionRepository sessionRepository,
                              PseudonymUtil pseudonymUtil) {
        this.progressService = progressService;
        this.sessionRepository = sessionRepository;
        this.pseudonymUtil = pseudonymUtil;
    }

    /**
     * Resultado de la generacion: contenido MD y metadatos para la cache.
     */
    public record GenerationResult(
            String content,
            int sessionCount,
            Instant lastSessionAt,
            String patientToken
    ) {}

    public GenerationResult generar(String patientDni) {
        long inicio = System.currentTimeMillis();
        String token = pseudonymUtil.tokenize(patientDni);

        List<TreatmentProgressDto> progresos = progressService.getOrCompute(patientDni);
        List<GameSession> ultimasSesiones =
                sessionRepository.findTop20ByPatientDniOrderBySessionStartDesc(patientDni);

        StringBuilder sb = new StringBuilder();
        sb.append("# Progreso del paciente\n\n");
        sb.append("**Token anonimo:** `").append(token).append("`\n");
        sb.append("**Generado:** ").append(Instant.now()).append("\n\n");

        construirResumenEjecutivo(sb, progresos);
        construirDetallePorTratamiento(sb, progresos);
        construirUltimasSesiones(sb, ultimasSesiones);
        construirNotasIA(sb, ultimasSesiones.size());

        Instant lastSessionAt = ultimasSesiones.isEmpty()
                ? null
                : ultimasSesiones.get(0).sessionStart();

        long durationMs = System.currentTimeMillis() - inicio;
        log.info("md_regenerated dni={} sessionCount={} durationMs={}",
                token, ultimasSesiones.size(), durationMs);

        return new GenerationResult(sb.toString(), ultimasSesiones.size(), lastSessionAt, token);
    }

    private void construirResumenEjecutivo(StringBuilder sb, List<TreatmentProgressDto> progresos) {
        sb.append("## Resumen ejecutivo\n\n");
        if (progresos.isEmpty()) {
            sb.append("Sin datos de progreso disponibles.\n\n");
            return;
        }
        sb.append("| Tratamiento | Parte | Baseline | Actual | Delta |\n");
        sb.append("|---|---|---|---|---|\n");
        for (TreatmentProgressDto p : progresos) {
            sb.append("| ").append(nvlText(p.tratamientoNombre())).append(" | ")
                    .append(nvlText(p.parteCuerpo())).append(" | ")
                    .append(formatValor(p.baseline() != null ? p.baseline().valor() : null)).append(" | ")
                    .append(formatValor(p.current() != null ? p.current().valor() : null)).append(" | ")
                    .append(formatDelta(p.deltaPorcentaje())).append(" |\n");
        }
        sb.append("\n");
    }

    private void construirDetallePorTratamiento(StringBuilder sb, List<TreatmentProgressDto> progresos) {
        sb.append("## Detalle por tratamiento\n\n");
        if (progresos.isEmpty()) {
            sb.append("Sin tratamientos registrados.\n\n");
            return;
        }
        for (TreatmentProgressDto p : progresos) {
            sb.append("### ").append(nvlText(p.tratamientoNombre()))
                    .append(" (").append(nvlText(p.parteCuerpo())).append(")\n\n");
            sb.append("- Metrica: ").append(nvlText(p.metricaNombre())).append("\n");
            if (p.baseline() != null) {
                sb.append("- Baseline (").append(p.baseline().fecha()).append("): ")
                        .append(formatValor(p.baseline().valor())).append("\n");
            }
            if (p.current() != null) {
                sb.append("- Actual (").append(p.current().fecha()).append("): ")
                        .append(formatValor(p.current().valor())).append("\n");
            }
            int puntos = p.entradas() != null ? p.entradas().size() : 0;
            sb.append("- Evolucion ").append(puntos).append(" puntos\n\n");
        }
    }

    private void construirUltimasSesiones(StringBuilder sb, List<GameSession> sesiones) {
        sb.append("## Ultimas sesiones\n\n");
        if (sesiones.isEmpty()) {
            sb.append("Sin sesiones registradas.\n\n");
            return;
        }
        sb.append("| Fecha | Juego | Score | Completada |\n");
        sb.append("|---|---|---|---|\n");
        for (GameSession s : sesiones) {
            sb.append("| ").append(s.sessionStart()).append(" | ")
                    .append(nvlText(s.gameId())).append(" | ")
                    .append(formatValor(s.score())).append(" | ")
                    .append(Boolean.TRUE.equals(s.completed()) ? "Si" : "No").append(" |\n");
        }
        sb.append("\n");
    }

    private void construirNotasIA(StringBuilder sb, int sessionCount) {
        sb.append("## Notas para analisis automatico (IA)\n\n");
        sb.append("Este documento se actualiza automaticamente con cada nueva sesion ingestada.\n");
        sb.append("Total de sesiones consideradas: ").append(Math.min(sessionCount, MAX_ULTIMAS_SESIONES)).append("\n");
    }

    private String formatValor(Double v) {
        // Locale.ROOT para garantizar punto decimal independiente del entorno.
        return v == null ? "-" : String.format(Locale.ROOT, "%.2f", v);
    }

    private String formatDelta(Double d) {
        if (d == null) {
            return "-";
        }
        String signo = d >= 0 ? "+" : "";
        return signo + String.format(Locale.ROOT, "%.2f%%", d);
    }

    private String nvlText(String s) {
        return s == null ? "-" : s;
    }
}
