package com.rehabiapp.api.presentation.controller;

import com.rehabiapp.api.application.dto.DiscapacidadRequest;
import com.rehabiapp.api.application.dto.DiscapacidadResponse;
import com.rehabiapp.api.application.dto.NivelProgresionResponse;
import com.rehabiapp.api.application.dto.TratamientoRequest;
import com.rehabiapp.api.application.dto.TratamientoResponse;
import com.rehabiapp.api.application.service.CatalogoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST para los catalogos clinicos.
 *
 * <p>Operaciones de lectura accesibles a todos los usuarios autenticados.
 * Operaciones de escritura restringidas al rol SPECIALIST.</p>
 */
@RestController
@RequestMapping("/api/catalogo")
public class CatalogoController {

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    // ==================== DISCAPACIDADES GET ====================

    /**
     * Lista todas las discapacidades del catalogo clinico.
     * GET /api/catalogo/discapacidades
     */
    @GetMapping("/discapacidades")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DiscapacidadResponse>> listarDiscapacidades() {
        return ResponseEntity.ok(catalogoService.listarDiscapacidades());
    }

    /**
     * Devuelve una discapacidad por su codigo.
     * GET /api/catalogo/discapacidades/{cod}
     */
    @GetMapping("/discapacidades/{cod}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DiscapacidadResponse> obtenerDiscapacidad(@PathVariable String cod) {
        return ResponseEntity.ok(catalogoService.obtenerDiscapacidad(cod));
    }

    // ==================== DISCAPACIDADES CRUD ====================

    /**
     * Crea una nueva discapacidad.
     * POST /api/catalogo/discapacidades
     * Solo SPECIALIST.
     */
    @PostMapping("/discapacidades")
    @PreAuthorize("hasRole('SPECIALIST')")
    public ResponseEntity<DiscapacidadResponse> crearDiscapacidad(
            @Valid @RequestBody DiscapacidadRequest request) {
        DiscapacidadResponse response = catalogoService.crearDiscapacidad(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Actualiza una discapacidad existente.
     * PUT /api/catalogo/discapacidades/{cod}
     * Solo SPECIALIST.
     */
    @PutMapping("/discapacidades/{cod}")
    @PreAuthorize("hasRole('SPECIALIST')")
    public ResponseEntity<DiscapacidadResponse> actualizarDiscapacidad(
            @PathVariable String cod,
            @Valid @RequestBody DiscapacidadRequest request) {
        DiscapacidadResponse response = catalogoService.actualizarDiscapacidad(cod, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Elimina una discapacidad. Rechaza con 409 si hay pacientes asignados.
     * DELETE /api/catalogo/discapacidades/{cod}
     * Solo SPECIALIST.
     */
    @DeleteMapping("/discapacidades/{cod}")
    @PreAuthorize("hasRole('SPECIALIST')")
    public ResponseEntity<Void> eliminarDiscapacidad(@PathVariable String cod) {
        catalogoService.eliminarDiscapacidad(cod);
        return ResponseEntity.noContent().build();
    }

    // ==================== TRATAMIENTOS GET ====================

    /**
     * Lista todos los tratamientos del catalogo terapeutico.
     * GET /api/catalogo/tratamientos
     */
    @GetMapping("/tratamientos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TratamientoResponse>> listarTratamientos() {
        return ResponseEntity.ok(catalogoService.listarTratamientos());
    }

    /**
     * Devuelve un tratamiento por su codigo.
     * GET /api/catalogo/tratamientos/{cod}
     */
    @GetMapping("/tratamientos/{cod}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TratamientoResponse> obtenerTratamiento(@PathVariable String cod) {
        return ResponseEntity.ok(catalogoService.obtenerTratamiento(cod));
    }

    /**
     * Lista los tratamientos aplicables a una discapacidad concreta.
     * GET /api/catalogo/tratamientos/discapacidad/{codDis}
     */
    @GetMapping("/tratamientos/discapacidad/{codDis}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TratamientoResponse>> listarPorDiscapacidad(@PathVariable String codDis) {
        return ResponseEntity.ok(catalogoService.listarTratamientosPorDiscapacidad(codDis));
    }

    // ==================== TRATAMIENTOS CRUD ====================

    /**
     * Crea un nuevo tratamiento.
     * POST /api/catalogo/tratamientos
     * Solo SPECIALIST.
     */
    @PostMapping("/tratamientos")
    @PreAuthorize("hasRole('SPECIALIST')")
    public ResponseEntity<TratamientoResponse> crearTratamiento(
            @Valid @RequestBody TratamientoRequest request) {
        TratamientoResponse response = catalogoService.crearTratamiento(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Actualiza un tratamiento existente.
     * PUT /api/catalogo/tratamientos/{cod}
     * Solo SPECIALIST.
     */
    @PutMapping("/tratamientos/{cod}")
    @PreAuthorize("hasRole('SPECIALIST')")
    public ResponseEntity<TratamientoResponse> actualizarTratamiento(
            @PathVariable String cod,
            @Valid @RequestBody TratamientoRequest request) {
        TratamientoResponse response = catalogoService.actualizarTratamiento(cod, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Elimina un tratamiento. Rechaza con 409 si hay pacientes asignados.
     * DELETE /api/catalogo/tratamientos/{cod}
     * Solo SPECIALIST.
     */
    @DeleteMapping("/tratamientos/{cod}")
    @PreAuthorize("hasRole('SPECIALIST')")
    public ResponseEntity<Void> eliminarTratamiento(@PathVariable String cod) {
        catalogoService.eliminarTratamiento(cod);
        return ResponseEntity.noContent().build();
    }

    // ==================== VINCULOS TRATAMIENTO-DISCAPACIDAD ====================

    /**
     * Lista las discapacidades vinculadas a un tratamiento.
     * GET /api/catalogo/tratamientos/{codTrat}/discapacidades
     */
    @GetMapping("/tratamientos/{codTrat}/discapacidades")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DiscapacidadResponse>> listarDiscapacidadesDeTratamiento(
            @PathVariable String codTrat) {
        return ResponseEntity.ok(catalogoService.listarDiscapacidadesDeTratamiento(codTrat));
    }

    /**
     * Vincula un tratamiento a una discapacidad.
     * POST /api/catalogo/tratamientos/{codTrat}/discapacidades/{codDis}
     * Solo SPECIALIST.
     */
    @PostMapping("/tratamientos/{codTrat}/discapacidades/{codDis}")
    @PreAuthorize("hasRole('SPECIALIST')")
    public ResponseEntity<Void> vincularTratamientoDiscapacidad(
            @PathVariable String codTrat, @PathVariable String codDis) {
        catalogoService.vincularTratamientoDiscapacidad(codTrat, codDis);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Desvincula un tratamiento de una discapacidad.
     * DELETE /api/catalogo/tratamientos/{codTrat}/discapacidades/{codDis}
     * Solo SPECIALIST.
     */
    @DeleteMapping("/tratamientos/{codTrat}/discapacidades/{codDis}")
    @PreAuthorize("hasRole('SPECIALIST')")
    public ResponseEntity<Void> desvincularTratamientoDiscapacidad(
            @PathVariable String codTrat, @PathVariable String codDis) {
        catalogoService.desvincularTratamientoDiscapacidad(codTrat, codDis);
        return ResponseEntity.noContent().build();
    }

    // ==================== NIVELES DE PROGRESION ====================

    /**
     * Lista todos los niveles de progresion clinica ordenados.
     * GET /api/catalogo/niveles-progresion
     */
    @GetMapping("/niveles-progresion")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NivelProgresionResponse>> listarNiveles() {
        return ResponseEntity.ok(catalogoService.listarNiveles());
    }
}
