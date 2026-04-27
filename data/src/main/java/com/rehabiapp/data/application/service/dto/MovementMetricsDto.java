package com.rehabiapp.data.application.service.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record MovementMetricsDto(

        @NotNull
        @DecimalMin("0.0") @DecimalMax("360.0")
        Double rangeOfMotionDegrees,

        @NotNull
        @DecimalMin("0.0")
        Double averageSpeed,

        @NotNull
        @DecimalMin("0.0")
        Double maxSpeed
) {}
