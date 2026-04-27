package com.rehabiapp.data.application.service.dto;

import com.rehabiapp.data.domain.model.PatientProgress;
import java.util.List;

public record PatientAnalyticsResponse(
        String patientDni,
        List<PatientProgress> weekly,
        List<PatientProgress> monthlyByDisability
) {}
