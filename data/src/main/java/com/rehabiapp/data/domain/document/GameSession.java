package com.rehabiapp.data.domain.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Map;

/**
 * Telemetria de una sesion de juego terapeutico.
 *
 * patientDni es PII: cifrado CSFLE Deterministic cuando CSFLE_ENABLED=true
 * (permite consultas por indice manteniendo confidencialidad).
 * patientToken es el hash SHA-256 del DNI + salt para uso en analitica anonimizada.
 *
 * rawMetrics contiene el payload libre por juego (polimorfismo por gameId).
 * metricsHash = SHA-256 canonico de rawMetrics, usado como clave de idempotencia.
 * reportStatus indica el estado de escritura del resumen MD en PostgreSQL.
 */
@Document(collection = "game_sessions")
public record GameSession(

        @Id
        String id,

        // PII — CSFLE Deterministic (consultable)
        @Field("patientDni")
        String patientDni,

        @Field("gameId")
        String gameId,

        @Field("disabilityId")
        String disabilityId,

        // Nivel de progresion terapeutica (1-4)
        @Field("progressionLevel")
        Integer progressionLevel,

        @Field("sessionStart")
        Instant sessionStart,

        @Field("sessionEnd")
        Instant sessionEnd,

        @Field("durationSeconds")
        Long durationSeconds,

        @Field("score")
        Double score,

        @Field("repetitionsCompleted")
        Integer repetitionsCompleted,

        @Field("repetitionsTarget")
        Integer repetitionsTarget,

        // Metricas libres por juego — clave del contrato polimorfrico
        @Field("rawMetrics")
        Map<String, Object> rawMetrics,

        // Version del schema enviada por el cliente Unity
        @Field("schemaVersion")
        String schemaVersion,

        // SHA-256 canonico de rawMetrics para idempotencia en reintentos
        @Field("metricsHash")
        String metricsHash,

        @Field("completed")
        Boolean completed,

        // Timestamp del servidor al recibir la sesion (no del cliente)
        @Field("receivedAt")
        Instant receivedAt,

        // Token anonimizado: SHA-256(patientDni + salt). Usado en analitica expuesta.
        @Field("patientToken")
        String patientToken,

        // Codigo del tratamiento asociado (opcional, enriquecido por la API Core)
        @Field("codTrat")
        String codTrat,

        // Parte del cuerpo trabajada (opcional, enriquecido por la API Core)
        @Field("parteCuerpo")
        String parteCuerpo,

        // Nombre legible del tratamiento (opcional, enriquecido por la API Core)
        @Field("tratamientoNombre")
        String tratamientoNombre,

        // Estado del reporte MD en PostgreSQL: PENDING | OK | FAILED
        @Field("reportStatus")
        String reportStatus,

        // Numero de intentos de escritura del reporte en Postgres
        @Field("reportAttempts")
        Integer reportAttempts,

        // Ultimo error al intentar escribir el reporte (truncado a 500 chars)
        @Field("reportLastError")
        String reportLastError,

        // Shim de compatibilidad con pipelines existentes (WeeklyGame, RomTimeSeries, TreatmentProgress).
        // Proyectado desde rawMetrics si las claves coinciden.
        // Eliminar tras migrar pipelines a rawMetrics en iteracion futura.
        @Field("movementMetrics")
        @Deprecated
        MovementMetrics movementMetrics

) {
    public record MovementMetrics(
            Double rangeOfMotionDegrees,
            Double averageSpeed,
            Double maxSpeed
    ) {}
}
