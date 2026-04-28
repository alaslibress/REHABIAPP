package com.rehabiapp.data.domain.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

/**
 * Telemetria de una sesion de juego terapeutico.
 *
 * patientDni es PII: cifrado CSFLE Deterministic cuando CSFLE_ENABLED=true
 * (permite consultas por indice manteniendo confidencialidad).
 * patientToken es el hash SHA-256 del DNI + salt para uso en analitica anonimizada.
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

        // Datos de movimiento — CSFLE Random (no consultable directamente)
        @Field("movementMetrics")
        MovementMetrics movementMetrics,

        @Field("completed")
        Boolean completed,

        // Timestamp del servidor al recibir la sesion (no del cliente)
        @Field("receivedAt")
        Instant receivedAt,

        // Token anonimizado: SHA-256(patientDni + salt). Usado en analitica expuesta.
        @Field("patientToken")
        String patientToken

) {
    public record MovementMetrics(
            Double rangeOfMotionDegrees,
            Double averageSpeed,
            Double maxSpeed
    ) {}
}
