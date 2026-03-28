package com.rehabiapp.api.presentation.controller;

import com.rehabiapp.api.application.dto.CitaRequest;
import com.rehabiapp.api.application.dto.CitaResponse;
import com.rehabiapp.api.application.dto.PageResponse;
import com.rehabiapp.api.application.service.CitaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Controlador REST para la gestión de citas médicas.
 *
 * <p>Las citas permiten eliminación física (DELETE real) a diferencia de
 * pacientes y sanitarios. Una cita cancelada no contiene datos clínicos
 * directos que requieran retención por la Ley 41/2002.</p>
 *
 * <p>La clave primaria compuesta (dniPac, dniSan, fecha, hora) garantiza
 * unicidad de citas. El DELETE usa los 4 campos como identificador compuesto.</p>
 */
@RestController
@RequestMapping("/api/citas")
public class CitaController {

    private final CitaService citaService;

    public CitaController(CitaService citaService) {
        this.citaService = citaService;
    }

    /**
     * Lista todas las citas de una fecha concreta paginadas.
     * Usado para la vista de agenda diaria del centro.
     *
     * <p>GET /api/citas?fecha=2025-03-28&page=0&size=20</p>
     *
     * @param fecha    Fecha a consultar en formato ISO (yyyy-MM-dd).
     * @param pageable Parámetros de paginación desde la query string.
     * @return 200 OK con página de citas de la fecha indicada.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<CitaResponse>> listarPorFecha(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            Pageable pageable) {
        return ResponseEntity.ok(citaService.listarPorFecha(fecha, pageable));
    }

    /**
     * Lista todas las citas de un sanitario concreto paginadas.
     * Usado para la agenda personal del profesional.
     *
     * <p>GET /api/citas/sanitario/{dniSan}?page=0&size=20</p>
     *
     * @param dniSan   DNI del sanitario cuya agenda se consulta.
     * @param pageable Parámetros de paginación desde la query string.
     * @return 200 OK con página de citas del sanitario indicado.
     */
    @GetMapping("/sanitario/{dniSan}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<CitaResponse>> listarPorSanitario(
            @PathVariable String dniSan,
            Pageable pageable) {
        return ResponseEntity.ok(citaService.listarPorSanitario(dniSan, pageable));
    }

    /**
     * Crea una nueva cita médica en el sistema.
     * Verifica que el paciente y el sanitario existen y están activos.
     *
     * <p>POST /api/citas</p>
     *
     * @param request DTO validado con los datos de la nueva cita.
     * @return 201 Created con los datos de la cita creada.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CitaResponse> crear(@Valid @RequestBody CitaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(citaService.crear(request));
    }

    /**
     * Elimina físicamente una cita del sistema.
     * El identificador compuesto se pasa como query params para compatibilidad REST.
     *
     * <p>DELETE /api/citas?dniPac=X&dniSan=Y&fecha=2025-03-28&hora=10:00</p>
     *
     * @param dniPac DNI del paciente de la cita.
     * @param dniSan DNI del sanitario de la cita.
     * @param fecha  Fecha de la cita en formato ISO (yyyy-MM-dd).
     * @param hora   Hora de la cita en formato ISO (HH:mm:ss).
     * @return 204 No Content si la eliminación fue exitosa, 404 si no existe.
     */
    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> eliminar(
            @RequestParam String dniPac,
            @RequestParam String dniSan,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime hora) {
        citaService.eliminar(dniPac, dniSan, fecha, hora);
        return ResponseEntity.noContent().build();
    }
}
