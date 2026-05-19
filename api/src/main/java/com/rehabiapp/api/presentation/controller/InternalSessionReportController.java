package com.rehabiapp.api.presentation.controller;

import com.rehabiapp.api.domain.entity.SessionReport;
import com.rehabiapp.api.domain.enums.AccionAuditoria;
import com.rehabiapp.api.domain.repository.SessionReportRepository;
import com.rehabiapp.api.infrastructure.audit.AuditService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

/**
 * Controlador interno para la escritura de informes de sesion en PostgreSQL.
 *
 * Solo aceptado desde el servicio /data mediante la clave interna X-Internal-Key.
 * Excluido de la documentacion OpenAPI publica (paths-to-exclude en application.yml).
 * No requiere JWT — el acceso se controla exclusivamente por la clave interna.
 */
@Hidden
@RestController
@RequestMapping("/api/internal")
public class InternalSessionReportController {

    private final SessionReportRepository reportRepository;
    private final AuditService auditService;
    private final String internalKey;

    public InternalSessionReportController(
            SessionReportRepository reportRepository,
            AuditService auditService,
            @Value("${rehabiapp.internal-key:changeme}") String internalKey) {
        this.reportRepository = reportRepository;
        this.auditService = auditService;
        this.internalKey = internalKey;
    }

    /**
     * Inserta o recupera un informe de sesion de juego.
     *
     * Idempotente: si ya existe una fila con el mismo mongoId devuelve 200 OK
     * con el ID existente en lugar de 409 Conflict, permitiendo reintentos seguros.
     */
    @PostMapping("/session-reports")
    public ResponseEntity<Map<String, Object>> crear(
            @RequestHeader("X-Internal-Key") String key,
            @Valid @RequestBody SessionReportRequest req) {

        if (!internalKey.equals(key)) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "clave_interna_invalida"));
        }

        // Idempotencia: devolver la fila existente si ya fue insertada
        var existente = reportRepository.findByMongoId(req.mongoId());
        if (existente.isPresent()) {
            return ResponseEntity.ok(Map.of(
                    "id", existente.get().getId(),
                    "status", "already_exists"
            ));
        }

        var reporte = new SessionReport(
                req.mongoId(),
                req.pacienteDni(),
                req.codJuego(),
                req.codTrat(),
                req.fechaSesion(),
                req.duracionSeg(),
                req.contenidoMd(),
                req.mdHash()
        );

        var guardado = reportRepository.save(reporte);

        auditService.registrar(
                AccionAuditoria.CREATE,
                "session_reports",
                req.mongoId(),
                "Informe creado para paciente=" + req.pacienteDni()
                        + " juego=" + req.codJuego()
        );

        return ResponseEntity.status(201)
                .body(Map.of("id", guardado.getId(), "status", "created"));
    }

    public record SessionReportRequest(

            @NotBlank
            String mongoId,

            @NotBlank
            String pacienteDni,

            @NotBlank
            String codJuego,

            String codTrat,

            @NotNull
            Instant fechaSesion,

            @Positive
            Integer duracionSeg,

            @NotBlank
            String contenidoMd,

            @NotBlank
            @Pattern(regexp = "^[a-f0-9]{64}$")
            String mdHash
    ) {}
}
