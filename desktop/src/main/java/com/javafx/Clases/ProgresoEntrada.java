package com.javafx.Clases;

import java.time.Instant;

/** Punto en la serie temporal de progreso (espejo de ProgresoEntradaResponse del API). */
public record ProgresoEntrada(Instant fecha, Double valor) {}
