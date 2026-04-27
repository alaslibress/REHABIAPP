package com.rehabiapp.data.domain.model;

/**
 * Metricas biomecanicas capturadas durante una sesion de juego terapeutico.
 * Valor embebido dentro de GameSession. Inmutable por ser record.
 */
public record MovementMetrics(
        Double rangeOfMotionDegrees,
        Double averageSpeed,
        Double maxSpeed
) {}
