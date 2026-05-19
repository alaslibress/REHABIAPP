package com.rehabiapp.data.analytics.service;

import com.rehabiapp.data.analytics.dto.TreatmentProgressDto;
import com.rehabiapp.data.analytics.pipeline.TreatmentProgressPipeline;
import com.rehabiapp.data.domain.repository.TreatmentProgressRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orquesta el pipeline de progreso terapeutico y persiste el resultado
 * en la coleccion {@code treatment_progress} para servir como cache.
 */
@Service
public class TreatmentProgressService {

    private final TreatmentProgressPipeline pipeline;
    private final TreatmentProgressRepository repository;

    public TreatmentProgressService(TreatmentProgressPipeline pipeline,
                                    TreatmentProgressRepository repository) {
        this.pipeline = pipeline;
        this.repository = repository;
    }

    /**
     * Calcula on-demand el progreso del paciente y actualiza la cache.
     */
    public List<TreatmentProgressDto> getOrCompute(String patientDni) {
        List<TreatmentProgressDto> fresh = pipeline.execute(patientDni);
        repository.upsertAll(patientDni, fresh);
        return fresh;
    }
}
