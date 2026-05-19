package com.rehabiapp.api.presentation.controller;

import com.rehabiapp.api.application.dto.DashboardResponse;
import com.rehabiapp.api.application.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para el dashboard agregado del paciente, consumido por el
 * BFF de la app movil ({@code /mobile/backend}).
 */
@Tag(name = "Dashboard paciente",
        description = "Vista agregada del paciente para la app movil")
@RestController
@RequestMapping("/api/pacientes/{dni}/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "Devuelve la vista agregada del paciente para el dashboard")
    @GetMapping
    @PreAuthorize("hasAnyRole('SPECIALIST','NURSE','PATIENT')")
    public ResponseEntity<DashboardResponse> obtener(@PathVariable String dni) {
        return ResponseEntity.ok(dashboardService.obtener(dni));
    }
}
