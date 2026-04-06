package com.javafx.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record CitaRequest(
    String dniPac,
    String dniSan,
    LocalDate fechaCita,
    LocalTime horaCita
) {}
