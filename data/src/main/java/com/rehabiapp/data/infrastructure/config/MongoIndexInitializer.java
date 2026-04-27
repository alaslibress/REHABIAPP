package com.rehabiapp.data.infrastructure.config;

import com.rehabiapp.data.domain.model.GameSession;
import com.rehabiapp.data.domain.model.LevelStatistics;
import com.rehabiapp.data.domain.model.PatientProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Crea todos los indices declarados por anotacion al arrancar la aplicacion.
 * Necesario cuando spring.data.mongodb.auto-index-creation = false (perfil aws).
 * Excluido en perfil "test" para evitar conexion a MongoDB en tests unitarios.
 */
@Component
@Profile("!test")
public class MongoIndexInitializer {

    private static final Logger log = LoggerFactory.getLogger(MongoIndexInitializer.class);

    private final MongoTemplate mongoTemplate;

    public MongoIndexInitializer(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void createIndexes() {
        ensureIndexes(GameSession.class);
        ensureIndexes(PatientProgress.class);
        ensureIndexes(LevelStatistics.class);
    }

    private void ensureIndexes(Class<?> entity) {
        IndexResolver resolver = new MongoPersistentEntityIndexResolver(mongoTemplate.getConverter().getMappingContext());
        IndexOperations ops = mongoTemplate.indexOps(entity);
        resolver.resolveIndexFor(entity).forEach(index -> {
            ops.ensureIndex(index);
            log.info("Indice asegurado coleccion={} indice={}", entity.getSimpleName(), index);
        });
    }
}
