package com.rehabiapp.data.application.service.dto;

import java.time.Instant;

public record GameSessionIngestResponse(
        String id,
        String sessionId,
        Instant receivedAt,
        String status
) {}
