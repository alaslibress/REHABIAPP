package com.rehabiapp.data.analytics.service;

import com.rehabiapp.data.domain.document.PatientMarkdown;
import com.rehabiapp.data.domain.repository.PatientMarkdownRepository;
import com.rehabiapp.data.markdown.MdGeneratorService;
import com.rehabiapp.data.markdown.MdGeneratorService.GenerationResult;
import com.rehabiapp.data.observability.DataMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Servicio de orquestacion de Markdown:
 *  - Sirve la version cacheada si todavia es fresca (TTL 15 min).
 *  - Regenera bajo demanda y persiste el resultado.
 */
@Service
public class MarkdownService {

    private static final long TTL_MINUTOS = 15;

    private final MdGeneratorService generator;
    private final PatientMarkdownRepository repository;

    @Autowired(required = false)
    private DataMetrics metrics;

    public MarkdownService(MdGeneratorService generator, PatientMarkdownRepository repository) {
        this.generator = generator;
        this.repository = repository;
    }

    public String getOrGenerate(String patientDni) {
        return repository.findByPatientDni(patientDni)
                .filter(this::esFresco)
                .map(PatientMarkdown::content)
                .orElseGet(() -> regenerar(patientDni));
    }

    public String regenerar(String patientDni) {
        GenerationResult result = generator.generar(patientDni);
        repository.upsert(
                patientDni,
                result.patientToken(),
                result.content(),
                result.sessionCount(),
                result.lastSessionAt()
        );
        if (metrics != null) {
            metrics.incMd();
        }
        return result.content();
    }

    private boolean esFresco(PatientMarkdown md) {
        if (md.updatedAt() == null) {
            return false;
        }
        return md.updatedAt().isAfter(Instant.now().minus(TTL_MINUTOS, ChronoUnit.MINUTES));
    }
}
