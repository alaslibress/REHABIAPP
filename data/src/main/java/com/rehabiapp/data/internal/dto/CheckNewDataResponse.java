package com.rehabiapp.data.internal.dto;

import java.time.Instant;

/**
 * Respuesta del endpoint interno de polling. La API Core la usa para decidir
 * si debe refrescar el dashboard del fisioterapeuta.
 */
public record CheckNewDataResponse(
        boolean hasNewData,
        Instant lastSessionAt,
        int count
) {}
