package com.rehabiapp.data.domain.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

/**
 * Progreso pre-agregado por paciente, juego y periodo.
 * Actualizado periodicamente por PatientProgressRefreshScheduler.
 *
 * period formato: "YYYY-Www" (semanal) o "YYYY-MM" (mensual).
 */
@Document(collection = "patient_progress")
public record PatientProgress(

        @Id
        String id,

        // PII — CSFLE Deterministic
        @Field("patientDni")
        String patientDni,

        // Token anonimizado para analitica expuesta
        @Field("patientToken")
        String patientToken,

        @Field("gameId")
        String gameId,

        @Field("disabilityId")
        String disabilityId,

        @Field("progressionLevel")
        Integer progressionLevel,

        @Field("period")
        String period,

        @Field("totalSessions")
        Integer totalSessions,

        @Field("averageScore")
        Double averageScore,

        @Field("averageDuration")
        Double averageDuration,

        // Tasa de sesiones completadas sobre el total (0.0 - 1.0)
        @Field("completionRate")
        Double completionRate,

        // Secuencia cronologica de ROM en grados para visualizar tendencia
        @Field("rangeOfMotionTrend")
        List<Double> rangeOfMotionTrend,

        @Field("lastUpdated")
        Instant lastUpdated

) {}
