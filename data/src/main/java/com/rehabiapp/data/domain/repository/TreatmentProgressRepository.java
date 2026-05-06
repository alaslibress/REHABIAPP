package com.rehabiapp.data.domain.repository;

import com.rehabiapp.data.analytics.dto.TreatmentProgressDto;
import com.rehabiapp.data.domain.document.TreatmentProgress;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repositorio custom para {@code treatment_progress}. Implementa upsert por
 * la clave logica (patientDni + codTrat + parteCuerpo). No usamos Spring Data
 * derivados porque necesitamos control fino de la operacion atomica.
 */
@Repository
public class TreatmentProgressRepository {

    private final MongoTemplate mongoTemplate;

    public TreatmentProgressRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Upsert masivo de progresos calculados para un paciente.
     * Cada DTO se traduce a un documento de la coleccion treatment_progress.
     */
    public void upsertAll(String patientDni, List<TreatmentProgressDto> progresos) {
        Instant now = Instant.now();
        for (TreatmentProgressDto p : progresos) {
            Query query = new Query(Criteria.where("patientDni").is(patientDni)
                    .and("codTrat").is(p.codTrat())
                    .and("parteCuerpo").is(p.parteCuerpo()));

            Update update = new Update()
                    .set("patientDni", patientDni)
                    .set("patientToken", p.patientToken())
                    .set("codTrat", p.codTrat())
                    .set("tratamientoNombre", p.tratamientoNombre())
                    .set("parteCuerpo", p.parteCuerpo())
                    .set("metricaNombre", p.metricaNombre())
                    .set("baseline", p.baseline() == null ? null
                            : new TreatmentProgress.Punto(p.baseline().fecha(), p.baseline().valor()))
                    .set("current", p.current() == null ? null
                            : new TreatmentProgress.Punto(p.current().fecha(), p.current().valor()))
                    .set("deltaPorcentaje", p.deltaPorcentaje())
                    .set("entradas", p.entradas() == null ? List.of()
                            : p.entradas().stream()
                            .map(e -> new TreatmentProgress.Punto(e.fecha(), e.valor()))
                            .toList())
                    .set("lastUpdated", now);

            mongoTemplate.upsert(query, update, TreatmentProgress.class);
        }
    }
}
