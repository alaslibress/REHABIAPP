package com.rehabiapp.data.ingestion.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * Schema de metricas para un gameId concreto.
 * Cargado desde classpath:metric-schemas/{gameId}.json al arrancar.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GameSchema(
        String gameId,
        List<String> requiredKeys,
        Map<String, FieldSpec> fields
) {}
