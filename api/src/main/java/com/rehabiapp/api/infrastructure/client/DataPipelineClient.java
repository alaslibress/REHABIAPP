package com.rehabiapp.api.infrastructure.client;

import com.rehabiapp.api.application.dto.CheckProgresoResponse;
import com.rehabiapp.api.application.dto.ProgresoResumenResponse;
import com.rehabiapp.api.application.dto.ProgresoTratamientoResponse;
import com.rehabiapp.api.application.dto.UltimaSesionDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Cliente HTTP para el pipeline de datos (`/data`, Node.js + MongoDB).
 *
 * <p>Encapsula todas las llamadas salientes que el API realiza al servicio de
 * datos: consulta de progreso, generacion/regeneracion de Markdown, ultima
 * sesion de juego e ingesta de telemetria. Timeout fijo de 5 segundos.</p>
 */
@Component
public class DataPipelineClient {

    private final RestClient restClient;

    public DataPipelineClient(
            @Value("${rehabiapp.data.url:${rehabiapp.data-service.url:http://localhost:8081}}") String baseUrl,
            @Value("${rehabiapp.internal-key:changeme}") String internalKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(5));

        // X-Internal-Key autentica /api → /data en todas las llamadas; sin este header
        // el filtro InternalKeyFilter de /data rechaza la peticion con 401.
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-Internal-Key", internalKey)
                .build();
    }

    /** Consulta si hay sesiones nuevas posteriores a `since`. */
    public CheckProgresoResponse checkNuevosDatos(String dni, Instant since) {
        return restClient.get()
                .uri(uriBuilder -> {
                    var b = uriBuilder.path("/internal/patient/{dni}/check-new-data");
                    if (since != null) {
                        b = b.queryParam("since", since);
                    }
                    return b.build(dni);
                })
                .retrieve()
                .body(CheckProgresoResponse.class);
    }

    /** Devuelve el progreso por tratamiento (serie temporal completa). */
    public List<ProgresoTratamientoResponse> obtenerProgreso(String dni) {
        return restClient.get()
                .uri("/analytics/patient/{dni}/treatment-progress", dni)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    /** Devuelve el contenido Markdown del informe de progreso. */
    public String obtenerMarkdown(String dni) {
        return restClient.get()
                .uri("/analytics/patient/{dni}/markdown", dni)
                .accept(MediaType.parseMediaType("text/markdown"))
                .retrieve()
                .body(String.class);
    }

    /** Solicita la regeneracion forzada del Markdown. */
    public void regenerarMarkdown(String dni) {
        restClient.post()
                .uri("/analytics/patient/{dni}/markdown/regenerar", dni)
                .retrieve()
                .toBodilessEntity();
    }

    /** Devuelve la ultima sesion de juego registrada en el pipeline. */
    public UltimaSesionDto ultimaSesion(String dni) {
        return restClient.get()
                .uri("/analytics/patient/{dni}/last-session", dni)
                .retrieve()
                .body(UltimaSesionDto.class);
    }

    /** Devuelve el resumen plano de progreso (welcome card movil). */
    public ProgresoResumenResponse obtenerResumen(String dni) {
        return restClient.get()
                .uri("/internal/patient/{dni}/summary", dni)
                .retrieve()
                .body(ProgresoResumenResponse.class);
    }

    /** Reenvia la telemetria de juego al pipeline. Devuelve el dataId asignado. */
    public Map<String, Object> ingestar(Map<String, Object> payload) {
        return restClient.post()
                .uri("/ingest/game-session")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
