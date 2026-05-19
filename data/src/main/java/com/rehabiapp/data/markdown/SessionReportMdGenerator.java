package com.rehabiapp.data.markdown;

import com.rehabiapp.data.domain.document.GameSession;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Genera el informe Markdown de una sesion individual de juego terapeutico.
 *
 * Estructura del MD (orientada a consumo por agente IA):
 *   1. Cabecera — identidad de la sesion (token anonimizado, no DNI en claro)
 *   2. Datos de la sesion — timestamps, duracion, nivel
 *   3. Datos brutos por dedo — hits/misses desglosados finger_0..4
 *   4. Metricas de sesion brutas — totales y tiempos sin derivar
 *   5. Scores computados — valores derivados (accuracy, IRS, streak)
 *   6. Contexto para IA — schema version, instrucciones de analisis
 *
 * Funcion pura: mismas entradas -> misma salida -> mismo hash SHA-256.
 * Hash usado como clave de idempotencia en session_reports (PostgreSQL).
 * Contenido anonimizado: patientToken, nunca DNI en claro.
 */
@Component
public class SessionReportMdGenerator {

    public record Resultado(String contenido, String hash) {}

    // Metricas brutas de sesion (pre-agregacion, sin derivar)
    private static final Set<String> METRICAS_BRUTAS = Set.of(
            "notesHit", "notesMissed", "fingerSpeed", "avgReactionMs"
    );

    // Scores computados a partir de los datos brutos
    private static final Set<String> SCORES_COMPUTADOS = Set.of(
            "accuracyPct", "maxStreak", "irsScore"
    );

    public Resultado render(GameSession sesion) {
        StringBuilder sb = new StringBuilder();

        seccionCabecera(sb, sesion);
        seccionDatosSesion(sb, sesion);
        seccionDatosBrutosPorDedo(sb, sesion.rawMetrics());
        seccionMetricasBrutas(sb, sesion.rawMetrics());
        seccionScoresComputados(sb, sesion);
        seccionContextoIA(sb, sesion);

        String contenido = sb.toString();
        return new Resultado(contenido, sha256(contenido));
    }

    // -----------------------------------------------------------------------

    private void seccionCabecera(StringBuilder sb, GameSession s) {
        sb.append("# Informe de sesion de juego terapeutico\n\n");
        sb.append("> Documento generado automaticamente. Uso interno — analisis por agente IA.\n\n");
        sb.append("- **ID sesion (MongoDB):** `").append(nvl(s.id())).append("`\n");
        sb.append("- **Paciente (token):** `").append(nvl(s.patientToken())).append("`\n");
        sb.append("- **Juego:** `").append(nvl(s.gameId())).append("`\n");
        if (s.tratamientoNombre() != null)
            sb.append("- **Tratamiento:** ").append(s.tratamientoNombre())
              .append(s.codTrat() != null ? " (`" + s.codTrat() + "`)" : "").append("\n");
        if (s.parteCuerpo() != null)
            sb.append("- **Parte del cuerpo:** ").append(s.parteCuerpo()).append("\n");
        if (s.disabilityId() != null)
            sb.append("- **Discapacidad:** `").append(s.disabilityId()).append("`\n");
        sb.append("\n");
    }

    private void seccionDatosSesion(StringBuilder sb, GameSession s) {
        sb.append("## Datos de la sesion\n\n");
        sb.append("| Campo | Valor |\n|---|---|\n");
        sb.append("| Inicio | ").append(nvl(s.sessionStart())).append(" |\n");
        sb.append("| Fin | ").append(nvl(s.sessionEnd())).append(" |\n");
        sb.append("| Duracion | ").append(s.durationSeconds() != null ? s.durationSeconds() + " s" : "-").append(" |\n");
        sb.append("| Nivel de progresion | ").append(nvl(s.progressionLevel())).append(" |\n");
        sb.append("| Completada | ").append(Boolean.TRUE.equals(s.completed()) ? "Si" : "No").append(" |\n");
        if (s.repetitionsCompleted() != null)
            sb.append("| Repeticiones completadas | ").append(s.repetitionsCompleted())
              .append(" / ").append(s.repetitionsTarget() != null ? s.repetitionsTarget() : "?").append(" |\n");
        sb.append("\n");
    }

    /**
     * Desglose por dedo: detecta claves con patron hits_finger_N / misses_finger_N.
     * Estos son los datos mas brutos disponibles — previos a cualquier agregacion.
     */
    private void seccionDatosBrutosPorDedo(StringBuilder sb, Map<String, Object> raw) {
        if (raw == null) return;

        // Detectar cuantos dedos hay (maximo indice presente)
        int maxDedo = -1;
        for (String k : raw.keySet()) {
            if (k.startsWith("hits_finger_") || k.startsWith("misses_finger_")) {
                try {
                    int idx = Integer.parseInt(k.substring(k.lastIndexOf('_') + 1));
                    if (idx > maxDedo) maxDedo = idx;
                } catch (NumberFormatException ignored) {}
            }
        }

        if (maxDedo < 0) return;

        sb.append("## Datos brutos por dedo\n\n");
        sb.append("_Observaciones por dedo previas a cualquier calculo de media o score._\n\n");
        sb.append("| Dedo | Aciertos | Fallos | Total notas | Precision dedo (%) |\n");
        sb.append("|---|---|---|---|---|\n");

        for (int i = 0; i <= maxDedo; i++) {
            int hits   = extractInt(raw, "hits_finger_" + i);
            int misses = extractInt(raw, "misses_finger_" + i);
            int total  = hits + misses;
            String pct = total > 0
                    ? String.format(Locale.ROOT, "%.1f", hits * 100.0 / total)
                    : "-";
            sb.append("| Dedo ").append(i).append(" | ")
              .append(hits).append(" | ")
              .append(misses).append(" | ")
              .append(total).append(" | ")
              .append(pct).append(" |\n");
        }
        sb.append("\n");
    }

    /**
     * Metricas de sesion brutas: totales y tiempos sin derivar ni ponderar.
     * No incluye per-finger ni scores.
     */
    private void seccionMetricasBrutas(StringBuilder sb, Map<String, Object> raw) {
        if (raw == null) return;

        Map<String, Object> brutas = new TreeMap<>();
        for (var e : raw.entrySet()) {
            String k = e.getKey();
            if (METRICAS_BRUTAS.contains(k)) brutas.put(k, e.getValue());
        }
        // Tambien incluir cualquier clave no reconocida que no sea per-finger ni score
        for (var e : raw.entrySet()) {
            String k = e.getKey();
            if (!METRICAS_BRUTAS.contains(k) && !SCORES_COMPUTADOS.contains(k)
                    && !k.startsWith("hits_finger_") && !k.startsWith("misses_finger_")) {
                brutas.put(k, e.getValue());
            }
        }

        if (brutas.isEmpty()) return;

        sb.append("## Metricas de sesion brutas\n\n");
        sb.append("_Valores de sesion completa sin calculo de scores derivados._\n\n");
        sb.append("| Metrica | Valor | Descripcion |\n|---|---|---|\n");
        brutas.forEach((k, v) ->
                sb.append("| ").append(k).append(" | ").append(fmtValor(v))
                  .append(" | ").append(descripcionMetrica(k)).append(" |\n")
        );
        sb.append("\n");
    }

    /**
     * Scores computados: valores derivados de los datos brutos.
     * El agente IA debe usar los datos brutos por dedo para razonar;
     * estos scores son referencias secundarias.
     */
    private void seccionScoresComputados(StringBuilder sb, GameSession s) {
        Map<String, Object> raw = s.rawMetrics();

        sb.append("## Scores computados\n\n");
        sb.append("_Derivados de los datos brutos. No usar como fuente primaria de analisis._\n\n");
        sb.append("| Score | Valor |\n|---|---|\n");

        // Score global de la sesion
        if (s.score() != null)
            sb.append("| Score global sesion | ").append(fmtDouble(s.score())).append(" |\n");

        if (raw != null) {
            for (String k : SCORES_COMPUTADOS) {
                if (raw.containsKey(k))
                    sb.append("| ").append(k).append(" | ").append(fmtValor(raw.get(k))).append(" |\n");
            }
        }
        sb.append("\n");
    }

    private void seccionContextoIA(StringBuilder sb, GameSession s) {
        sb.append("## Contexto para analisis automatico\n\n");
        sb.append("- **Schema version:** ").append(nvl(s.schemaVersion())).append("\n");
        sb.append("- **Recibido en pipeline:** ").append(nvl(s.receivedAt())).append("\n");
        sb.append("- **Metrics hash (idempotencia):** `").append(nvl(s.metricsHash())).append("`\n");
        sb.append("\n");
        sb.append("### Instrucciones para el agente IA\n\n");
        sb.append("1. Priorizar la seccion **Datos brutos por dedo** para evaluar asimetrias laterales.\n");
        sb.append("2. Comparar `avgReactionMs` con el umbral clinico de referencia (1500 ms).\n");
        sb.append("3. Los valores de la seccion **Scores computados** son referencias secundarias.\n");
        sb.append("4. No inferir diagnostico clinico — solo identificar patrones y anomalias.\n");
        sb.append("5. Si `completada = No`, ajustar interpretacion por sesion incompleta.\n");
    }

    // -----------------------------------------------------------------------

    private String descripcionMetrica(String k) {
        return switch (k) {
            case "notesHit"      -> "Total notas acertadas en la sesion";
            case "notesMissed"   -> "Total notas falladas en la sesion";
            case "fingerSpeed"   -> "Velocidad media (notas/segundo)";
            case "avgReactionMs" -> "Tiempo medio de reaccion (ms)";
            default              -> "";
        };
    }

    private String fmtValor(Object v) {
        if (v == null) return "-";
        if (v instanceof Double d) return fmtDouble(d);
        if (v instanceof Float f) return fmtDouble(f.doubleValue());
        return v.toString();
    }

    private String fmtDouble(double v) {
        return String.format(Locale.ROOT, "%.4f", v);
    }

    private int extractInt(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof Number n) return n.intValue();
        return 0;
    }

    private String nvl(Object o) {
        return o == null ? "-" : o.toString();
    }

    private String sha256(String contenido) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(contenido.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
