package com.rehabiapp.api.presentation.controller;

import com.rehabiapp.api.application.dto.PageResponse;
import com.rehabiapp.api.application.dto.VideojuegoRequest;
import com.rehabiapp.api.application.dto.VideojuegoResponse;
import com.rehabiapp.api.application.service.VideojuegoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST para los videojuegos terapeuticos.
 *
 * <p>Lectura abierta a cualquier usuario autenticado. Las operaciones de
 * escritura (crear, actualizar, desactivar) requieren rol SPECIALIST.</p>
 */
@Tag(name = "Videojuegos", description = "Catalogo de videojuegos terapeuticos")
@RestController
@RequestMapping("/api/videojuegos")
public class VideojuegoController {

    private final VideojuegoService videojuegoService;

    public VideojuegoController(VideojuegoService videojuegoService) {
        this.videojuegoService = videojuegoService;
    }

    @Operation(summary = "Lista paginada de videojuegos activos")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<VideojuegoResponse>> listar(Pageable pageable) {
        return ResponseEntity.ok(PageResponse.de(videojuegoService.listarActivos(pageable)));
    }

    @Operation(summary = "Obtiene un videojuego por su identificador")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VideojuegoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(videojuegoService.obtener(id));
    }

    @Operation(summary = "Lista los videojuegos asociados a una discapacidad")
    @GetMapping("/discapacidad/{codDis}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<VideojuegoResponse>> porDiscapacidad(@PathVariable String codDis) {
        return ResponseEntity.ok(videojuegoService.listarPorDiscapacidad(codDis));
    }

    @Operation(summary = "Crea un nuevo videojuego")
    @PostMapping
    @PreAuthorize("hasRole('SPECIALIST')")
    public ResponseEntity<VideojuegoResponse> crear(@Valid @RequestBody VideojuegoRequest request) {
        VideojuegoResponse response = videojuegoService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Actualiza un videojuego existente")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SPECIALIST')")
    public ResponseEntity<VideojuegoResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody VideojuegoRequest request) {
        return ResponseEntity.ok(videojuegoService.actualizar(id, request));
    }

    @Operation(summary = "Desactiva un videojuego (borrado logico)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SPECIALIST')")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        videojuegoService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
