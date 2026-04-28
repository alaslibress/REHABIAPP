package com.rehabiapp.data.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

/**
 * Crea indices MongoDB explicitamente al arrancar.
 * auto-index-creation esta desactivado (skill springboot4-mongodb).
 */
@Configuration
@ConditionalOnBean(MongoTemplate.class)
public class MongoIndexConfig {

    private final MongoTemplate mongoTemplate;

    public MongoIndexConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    public void crearIndices() {
        crearIndicesGameSessions();
        crearIndicesPatientProgress();
    }

    private void crearIndicesGameSessions() {
        var ops = mongoTemplate.indexOps("game_sessions");

        ops.ensureIndex(new Index().on("patientDni", Sort.Direction.ASC).named("idx_patientDni"));
        ops.ensureIndex(new Index().on("gameId", Sort.Direction.ASC).named("idx_gameId"));
        ops.ensureIndex(new Index().on("progressionLevel", Sort.Direction.ASC).named("idx_progressionLevel"));
        ops.ensureIndex(new Index().on("sessionStart", Sort.Direction.DESC).named("idx_sessionStart"));
        ops.ensureIndex(new Index().on("disabilityId", Sort.Direction.ASC).named("idx_disabilityId"));

        // Indice compuesto para pipelines de progreso semanal por paciente y juego
        ops.ensureIndex(new Index()
                .on("patientDni", Sort.Direction.ASC)
                .on("gameId", Sort.Direction.ASC)
                .on("sessionStart", Sort.Direction.DESC)
                .named("idx_patient_game_start"));

        // Indice compuesto para pipelines de progreso mensual por discapacidad
        ops.ensureIndex(new Index()
                .on("disabilityId", Sort.Direction.ASC)
                .on("progressionLevel", Sort.Direction.ASC)
                .on("sessionStart", Sort.Direction.DESC)
                .named("idx_disability_level_start"));
    }

    private void crearIndicesPatientProgress() {
        var ops = mongoTemplate.indexOps("patient_progress");

        ops.ensureIndex(new Index().on("patientDni", Sort.Direction.ASC).named("idx_pp_patientDni"));

        // Indice unico para evitar duplicados en el scheduler de refresh
        ops.ensureIndex(new Index()
                .on("patientDni", Sort.Direction.ASC)
                .on("gameId", Sort.Direction.ASC)
                .on("period", Sort.Direction.ASC)
                .named("idx_pp_patient_game_period")
                .unique());
    }
}
