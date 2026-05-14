package com.rehabiapp.data.internal.client;

import com.rehabiapp.data.domain.document.GameSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Cliente HTTP para la llamada interna /data -> /api/internal/session-reports.
 *
 * Reintentos: 3 intentos con backoff exponencial (100ms, 400ms, 1600ms).
 * Solo reintenta en errores 5xx y de red — los 4xx no se reintentan.
 * Timeout: configurable via RH_API_TIMEOUT_MS (default 5000ms).
 */
@Component
public class ApiInternalClient {

    private static final Logger log = LoggerFactory.getLogger(ApiInternalClient.class);
    private static final long[] BACKOFF_MS = {100L, 400L, 1600L};

    private final RestClient restClient;
    private final String internalKey;
    private final int maxRetries;

    public ApiInternalClient(
            @Value("${rehabiapp.api.base-url:http://localhost:8080}") String baseUrl,
            @Value("${rehabiapp.api.internal-key:changeme}") String internalKey,
            @Value("${rehabiapp.api.timeout-ms:5000}") int timeoutMs,
            @Value("${rehabiapp.api.max-retries:3}") int maxRetries) {

        this.internalKey = internalKey;
        this.maxRetries = maxRetries;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-Internal-Key", internalKey)
                .build();
    }

    /**
     * Envia el informe de sesion al endpoint interno del API Core.
     *
     * @param sesion  documento GameSession ya guardado en MongoDB
     * @param contenidoMd Markdown generado por SessionReportMdGenerator
     * @param mdHash  SHA-256 del contenido Markdown
     */
    public void enviarInforme(GameSession sesion, String contenidoMd, String mdHash) {
        var body = construirBody(sesion, contenidoMd, mdHash);
        ejecutarConReintentos(body);
    }

    private Map<String, Object> construirBody(GameSession s, String md, String hash) {
        return Map.of(
                "mongoId", s.id(),
                "pacienteDni", s.patientDni(),
                "codJuego", s.gameId(),
                "codTrat", s.codTrat() != null ? s.codTrat() : "",
                "fechaSesion", s.sessionStart() != null ? s.sessionStart() : Instant.now(),
                "duracionSeg", s.durationSeconds() != null ? s.durationSeconds().intValue() : 0,
                "contenidoMd", md,
                "mdHash", hash
        );
    }

    private void ejecutarConReintentos(Map<String, Object> body) {
        Exception ultimoError = null;
        for (int intento = 0; intento < maxRetries; intento++) {
            try {
                restClient.post()
                        .uri("/api/internal/session-reports")
                        .body(body)
                        .retrieve()
                        .toBodilessEntity();
                return;
            } catch (Exception e) {
                // 4xx: no reintentar — son errores de contrato que el dev debe corregir
                if (esCuatrocientos(e)) {
                    throw e;
                }
                ultimoError = e;
                log.warn("Intento {} fallido enviando informe a /api: {}", intento + 1, e.getMessage());
                esperarBackoff(intento);
            }
        }
        throw new RuntimeException("Fallo al enviar informe tras " + maxRetries + " intentos", ultimoError);
    }

    private boolean esCuatrocientos(Exception e) {
        String msg = e.getMessage();
        return msg != null && (msg.contains("400") || msg.contains("401")
                || msg.contains("403") || msg.contains("422"));
    }

    private void esperarBackoff(int intento) {
        long ms = intento < BACKOFF_MS.length ? BACKOFF_MS[intento] : BACKOFF_MS[BACKOFF_MS.length - 1];
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
