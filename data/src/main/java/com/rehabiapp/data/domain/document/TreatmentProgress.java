package com.rehabiapp.data.domain.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

/**
 * Pre-agregado de progreso por (paciente, tratamiento, parte del cuerpo).
 *
 * Se actualiza on-demand cuando el dashboard solicita el progreso del paciente.
 * El upsert se realiza por la clave logica (patientDni + codTrat + parteCuerpo).
 */
@Document(collection = "treatment_progress")
public record TreatmentProgress(

        @Id
        String id,

        @Field("patientDni")
        String patientDni,

        @Field("patientToken")
        String patientToken,

        @Field("codTrat")
        String codTrat,

        @Field("tratamientoNombre")
        String tratamientoNombre,

        @Field("parteCuerpo")
        String parteCuerpo,

        @Field("metricaNombre")
        String metricaNombre,

        @Field("baseline")
        Punto baseline,

        @Field("current")
        Punto current,

        @Field("deltaPorcentaje")
        Double deltaPorcentaje,

        @Field("entradas")
        List<Punto> entradas,

        @Field("lastUpdated")
        Instant lastUpdated

) {
    public record Punto(Instant fecha, Double valor) {}
}
