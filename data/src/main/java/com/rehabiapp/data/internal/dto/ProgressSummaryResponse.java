package com.rehabiapp.data.internal.dto;

import java.time.Instant;

/**
 * Resumen ligero del progreso del paciente. Pensado para alimentar la welcome
 * card del movil via BFF: nunca expone PII ni metricas crudas, solo agregados.
 *
 * <ul>
 *   <li>{@code totalSessions} — recuento de sesiones registradas.</li>
 *   <li>{@code averageScore} — media de {@code score} (null si no hay sesiones).</li>
 *   <li>{@code improvementRate} — variacion porcentual del score entre la primera
 *       y la ultima sesion (null si hay menos de 2 sesiones o baseline=0).</li>
 *   <li>{@code lastSessionDate} — timestamp UTC de la sesion mas reciente.</li>
 * </ul>
 */
public record ProgressSummaryResponse(
        long totalSessions,
        Double averageScore,
        Double improvementRate,
        Instant lastSessionDate
) {}
