package com.rehabiapp.data.domain.repository;

import com.rehabiapp.data.domain.document.PatientMarkdown;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * Repositorio custom para la cache de markdowns por paciente.
 * Implementa upsert por DNI con MongoTemplate para mantener atomicidad.
 */
@Repository
public class PatientMarkdownRepository {

    private final MongoTemplate mongoTemplate;

    public PatientMarkdownRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Optional<PatientMarkdown> findByPatientDni(String patientDni) {
        Query query = new Query(Criteria.where("patientDni").is(patientDni));
        return Optional.ofNullable(mongoTemplate.findOne(query, PatientMarkdown.class));
    }

    public void upsert(String patientDni,
                       String patientToken,
                       String content,
                       int sessionCount,
                       Instant lastSessionAt) {
        Query query = new Query(Criteria.where("patientDni").is(patientDni));
        Update update = new Update()
                .set("patientDni", patientDni)
                .set("patientToken", patientToken)
                .set("content", content)
                .set("sessionCount", sessionCount)
                .set("updatedAt", Instant.now())
                .set("lastSessionAt", lastSessionAt);
        mongoTemplate.upsert(query, update, PatientMarkdown.class);
    }
}
