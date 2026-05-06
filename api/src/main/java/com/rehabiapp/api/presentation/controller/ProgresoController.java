package com.rehabiapp.api.presentation.controller;

import com.rehabiapp.api.application.dto.CheckProgresoResponse;
import com.rehabiapp.api.application.dto.ProgresoTratamientoResponse;
import com.rehabiapp.api.application.service.ProgresoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Controlador REST para la consulta de progreso clinico del paciente.
 *
 * <p>Proxia las peticiones al pipeline `/data` y registra cada acceso
 * en el log de auditoria (Ley 41/2002).</p>
 */
@Tag(name = "Progreso paciente", description = "Consulta del progreso clinico de un paciente")
@RestController
@RequestMapping("/api/pacientes/{dni}/progreso")
public class ProgresoController {

    private final ProgresoService progresoService;

    public ProgresoController(ProgresoService progresoService) {
        this.progresoService = progresoService;
    }

    @Operation(summary = "Comprueba si hay sesiones nuevas posteriores a la marca de tiempo")
    @GetMapping("/check")
    @PreAuthorize("hasAnyRole('SPECIALIST','NURSE','PATIENT')")
    public ResponseEntity<CheckProgresoResponse> check(
            @PathVariable String dni,
            @RequestParam(required = false) Instant since) {
        return ResponseEntity.ok(progresoService.checkNuevosDatos(dni, since));
    }

    @Operation(summary = "Devuelve el progreso por tratamiento (serie temporal)")
    @GetMapping
    @PreAuthorize("hasAnyRole('SPECIALIST','NURSE','PATIENT')")
    public ResponseEntity<List<ProgresoTratamientoResponse>> obtener(@PathVariable String dni) {
        return ResponseEntity.ok(progresoService.obtenerProgreso(dni));
    }

    @Operation(summary = "Devuelve el informe Markdown de progreso (cacheado)")
    @GetMapping(value = "/markdown", produces = "text/markdown;charset=UTF-8")
    @PreAuthorize("hasAnyRole('SPECIALIST','NURSE','PATIENT')")
    public ResponseEntity<String> markdown(@PathVariable String dni) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/markdown;charset=UTF-8"))
                .body(progresoService.obtenerMarkdown(dni));
    }

    @Operation(summary = "Solicita la regeneracion forzada del Markdown")
    @PostMapping("/markdown/regenerar")
    @PreAuthorize("hasRole('SPECIALIST')")
    public ResponseEntity<Void> regenerar(@PathVariable String dni) {
        progresoService.regenerarMarkdown(dni);
        return ResponseEntity.accepted().build();
    }
}
