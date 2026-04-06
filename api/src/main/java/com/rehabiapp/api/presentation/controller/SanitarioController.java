package com.rehabiapp.api.presentation.controller;

import com.rehabiapp.api.application.dto.PageResponse;
import com.rehabiapp.api.application.dto.SanitarioRequest;
import com.rehabiapp.api.application.dto.SanitarioResponse;
import com.rehabiapp.api.application.service.SanitarioService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para la gestión de sanitarios.
 *
 * <p>RBAC aplicado por método:</p>
 * <ul>
 *   <li>GET — accesible para cualquier sanitario autenticado (SPECIALIST y NURSE).</li>
 *   <li>POST, PUT, DELETE — solo rol SPECIALIST puede crear, actualizar o dar de baja sanitarios.</li>
 * </ul>
 *
 * <p>El DELETE realiza baja lógica (soft delete) — nunca elimina físicamente.
 * Datos retenidos mínimo 5 años (Ley 41/2002, RGPD Art. 17 con excepción sanitaria).</p>
 */
@RestController
@RequestMapping("/api/sanitarios")
public class SanitarioController {

    private final SanitarioService sanitarioService;

    public SanitarioController(SanitarioService sanitarioService) {
        this.sanitarioService = sanitarioService;
    }

    /**
     * Lista todos los sanitarios activos paginados.
     *
     * <p>GET /api/sanitarios?page=0&size=20&sort=apellido1San,asc</p>
     *
     * @param pageable Parámetros de paginación y ordenación desde la query string.
     * @return 200 OK con página de sanitarios activos.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<SanitarioResponse>> listar(Pageable pageable) {
        return ResponseEntity.ok(sanitarioService.listar(pageable));
    }

    /**
     * Devuelve un sanitario activo por su DNI.
     *
     * <p>GET /api/sanitarios/{dni}</p>
     *
     * @param dni DNI del sanitario a consultar.
     * @return 200 OK con los datos del sanitario, 404 si no existe.
     */
    @GetMapping("/{dni}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SanitarioResponse> obtener(@PathVariable String dni) {
        return ResponseEntity.ok(sanitarioService.obtenerPorDni(dni));
    }

    /**
     * Crea un nuevo sanitario en el sistema.
     * Solo accesible para rol SPECIALIST (RBAC).
     *
     * <p>POST /api/sanitarios</p>
     *
     * @param request DTO validado con los datos del nuevo sanitario.
     * @return 201 Created con los datos del sanitario creado.
     */
    @PostMapping
    @PreAuthorize("hasRole('SPECIALIST')")
    public ResponseEntity<SanitarioResponse> crear(@Valid @RequestBody SanitarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sanitarioService.crear(request));
    }

    /**
     * Actualiza los datos de un sanitario existente.
     * Solo accesible para rol SPECIALIST (RBAC).
     *
     * <p>PUT /api/sanitarios/{dni}</p>
     *
     * @param dni     DNI del sanitario a actualizar.
     * @param request DTO validado con los nuevos datos.
     * @return 200 OK con los datos actualizados, 404 si no existe.
     */
    @PutMapping("/{dni}")
    @PreAuthorize("hasRole('SPECIALIST')")
    public ResponseEntity<SanitarioResponse> actualizar(
            @PathVariable String dni,
            @Valid @RequestBody SanitarioRequest request) {
        return ResponseEntity.ok(sanitarioService.actualizar(dni, request));
    }

    /**
     * Da de baja lógica a un sanitario (soft delete).
     * Solo accesible para rol SPECIALIST (RBAC).
     *
     * <p>DELETE /api/sanitarios/{dni}</p>
     *
     * @param dni DNI del sanitario a dar de baja.
     * @return 204 No Content si la baja fue exitosa, 404 si no existe.
     */
    @DeleteMapping("/{dni}")
    @PreAuthorize("hasRole('SPECIALIST')")
    public ResponseEntity<Void> eliminar(@PathVariable String dni) {
        sanitarioService.eliminar(dni);
        return ResponseEntity.noContent().build();
    }

    /**
     * Busca sanitarios activos por texto libre (case-insensitive).
     * Busca en DNI, nombre, apellidos y email.
     *
     * <p>GET /api/sanitarios/buscar?texto=lopez&page=0&size=20&sort=apellido1San,asc</p>
     *
     * @param texto    Término de búsqueda libre.
     * @param pageable Parámetros de paginación y ordenación desde la query string.
     * @return 200 OK con página de sanitarios activos que coinciden.
     */
    @GetMapping("/buscar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<SanitarioResponse>> buscar(
            @RequestParam String texto,
            Pageable pageable) {
        return ResponseEntity.ok(sanitarioService.buscar(texto, pageable));
    }
}
