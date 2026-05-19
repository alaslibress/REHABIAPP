package com.rehabiapp.data.ingestion.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Carga y expone los schemas de metricas por gameId.
 *
 * Lee todos los JSON en classpath:metric-schemas/ al arrancar.
 * Para agregar soporte a un nuevo juego basta con crear el fichero
 * metric-schemas/{gameId}.json sin cambiar codigo.
 */
@Component
public class MetricSchemaRegistry {

    private static final Logger log = LoggerFactory.getLogger(MetricSchemaRegistry.class);
    private static final String SCHEMA_PATH = "classpath:metric-schemas/*.json";

    // Instancia directa: Spring Boot 4 expone Jackson 3 (tools.jackson) como bean,
    // no Jackson 2 (com.fasterxml). Aqui solo necesitamos parsear ficheros JSON
    // estaticos en el classpath, asi que evitamos depender del contenedor.
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, GameSchema> schemas = new HashMap<>();

    public MetricSchemaRegistry() {
    }

    @PostConstruct
    public void cargar() throws IOException {
        var resolver = new PathMatchingResourcePatternResolver();
        Resource[] recursos = resolver.getResources(SCHEMA_PATH);
        for (Resource recurso : recursos) {
            try {
                var schema = objectMapper.readValue(recurso.getInputStream(), GameSchema.class);
                schemas.put(schema.gameId(), schema);
                log.info("Schema cargado: {}", schema.gameId());
            } catch (Exception e) {
                log.warn("Error cargando schema {}: {}", recurso.getFilename(), e.getMessage());
            }
        }
        log.info("MetricSchemaRegistry listo con {} schemas", schemas.size());
    }

    /**
     * Devuelve el schema del juego o lanza UnknownGameException si no existe.
     */
    public GameSchema require(String gameId) {
        var schema = schemas.get(gameId);
        if (schema == null) {
            throw new UnknownGameException(gameId);
        }
        return schema;
    }

    /**
     * Valida rawMetrics contra el schema del gameId.
     * Lanza InvalidMetricException o UnknownGameException si no cumple.
     */
    public void validar(String gameId, Map<String, Object> rawMetrics) {
        var schema = require(gameId);

        // Verificar claves obligatorias
        for (String clave : schema.requiredKeys()) {
            if (!rawMetrics.containsKey(clave)) {
                throw new InvalidMetricException("clave obligatoria ausente: " + clave);
            }
        }

        // Verificar que todas las claves enviadas esten permitidas y cumplan el spec
        for (var entrada : rawMetrics.entrySet()) {
            var spec = schema.fields().get(entrada.getKey());
            if (spec == null) {
                throw new InvalidMetricException("clave no permitida para juego '"
                        + gameId + "': " + entrada.getKey());
            }
            spec.validate(entrada.getKey(), entrada.getValue());
        }
    }

    public boolean tieneSchema(String gameId) {
        return schemas.containsKey(gameId);
    }
}
