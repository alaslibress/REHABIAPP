package com.rehabiapp.data.domain.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

/**
 * Cache del documento Markdown del paciente. El contenido (campo {@code content})
 * se construye con {@code patientToken} y nunca contiene DNI en claro.
 */
@Document(collection = "patient_markdown")
public record PatientMarkdown(

        @Id
        String id,

        @Field("patientDni")
        String patientDni,

        @Field("patientToken")
        String patientToken,

        @Field("content")
        String content,

        @Field("sessionCount")
        Integer sessionCount,

        @Field("updatedAt")
        Instant updatedAt,

        @Field("lastSessionAt")
        Instant lastSessionAt

) {}
