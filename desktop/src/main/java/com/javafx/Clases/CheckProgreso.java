package com.javafx.Clases;

import java.time.Instant;

/** Resultado de la consulta de novedades de progreso (espejo de CheckProgresoResponse del API). */
public record CheckProgreso(boolean hasNewData, Instant lastSessionAt, int count) {}
