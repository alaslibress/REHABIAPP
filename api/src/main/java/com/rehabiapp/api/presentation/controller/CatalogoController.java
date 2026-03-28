package com.rehabiapp.api.presentation.controller;

import com.rehabiapp.api.application.dto.DiscapacidadResponse;
import com.rehabiapp.api.application.dto.NivelProgresionResponse;
import com.rehabiapp.api.application.dto.TratamientoResponse;
import com.rehabiapp.api.application.service.CatalogoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST para los catálogos clínicos de solo lectura.
 *
 * <p>Los catálogos (discapacidades, tratamientos, niveles de progresión) son
 * de solo lectura desde la API REST. Las operaciones de escritura se realizan
 * actualmente desde el desktop ERP con acceso JDBC directo.</p>
 *
 * <p>Todos los endpoints requieren autenticación pero no restricción de rol —
 * tanto SPECIALIST como NURSE pueden consultar los catálogos clínicos.</p>
 */
@RestController
@RequestMapping("/api/catalogo")
public class CatalogoController {

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    /**
     * Lista todas las discapacidades del catálogo clínico.
     *
     * <p>GET /api/catalogo/discapacidades</p>
     *
     * @return 200 OK con la lista completa de discapacidades.
     */
    @GetMapping("/discapacidades")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DiscapacidadResponse>> listarDiscapacidades() {
        return ResponseEntity.ok(catalogoService.listarDiscapacidades());
    }

    /**
     * Devuelve una discapacidad por su código CIE-10.
     *
     * <p>GET /api/catalogo/discapacidades/{cod}</p>
     *
     * @param cod Código de la discapacidad.
     * @return 200 OK con los datos de la discapacidad, 404 si no existe.
     */
    @GetMapping("/discapacidades/{cod}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DiscapacidadResponse> obtenerDiscapacidad(@PathVariable String cod) {
        return ResponseEntity.ok(catalogoService.obtenerDiscapacidad(cod));
    }

    /**
     * Lista todos los tratamientos del catálogo terapéutico.
     *
     * <p>GET /api/catalogo/tratamientos</p>
     *
     * @return 200 OK con la lista completa de tratamientos.
     */
    @GetMapping("/tratamientos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TratamientoResponse>> listarTratamientos() {
        return ResponseEntity.ok(catalogoService.listarTratamientos());
    }

    /**
     * Devuelve un tratamiento por su código.
     *
     * <p>GET /api/catalogo/tratamientos/{cod}</p>
     *
     * @param cod Código del tratamiento.
     * @return 200 OK con los datos del tratamiento, 404 si no existe.
     */
    @GetMapping("/tratamientos/{cod}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TratamientoResponse> obtenerTratamiento(@PathVariable String cod) {
        return ResponseEntity.ok(catalogoService.obtenerTratamiento(cod));
    }

    /**
     * Lista los tratamientos aplicables a una discapacidad concreta.
     * Usado para filtrar la selección terapéutica según el perfil del paciente.
     *
     * <p>GET /api/catalogo/tratamientos/discapacidad/{codDis}</p>
     *
     * @param codDis Código de la discapacidad para filtrar tratamientos.
     * @return 200 OK con la lista de tratamientos de esa discapacidad.
     */
    @GetMapping("/tratamientos/discapacidad/{codDis}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TratamientoResponse>> listarPorDiscapacidad(@PathVariable String codDis) {
        return ResponseEntity.ok(catalogoService.listarTratamientosPorDiscapacidad(codDis));
    }

    /**
     * Lista todos los niveles de progresión clínica ordenados.
     *
     * <p>GET /api/catalogo/niveles-progresion</p>
     *
     * @return 200 OK con la lista de niveles ordenados de menor a mayor progresión.
     */
    @GetMapping("/niveles-progresion")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NivelProgresionResponse>> listarNiveles() {
        return ResponseEntity.ok(catalogoService.listarNiveles());
    }
}
