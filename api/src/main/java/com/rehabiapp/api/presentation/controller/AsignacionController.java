package com.rehabiapp.api.presentation.controller;

import com.rehabiapp.api.application.dto.ActualizarNotasRequest;
import com.rehabiapp.api.application.dto.PacienteDiscapacidadRequest;
import com.rehabiapp.api.application.dto.PacienteDiscapacidadResponse;
import com.rehabiapp.api.application.dto.PacienteTratamientoRequest;
import com.rehabiapp.api.application.dto.PacienteTratamientoResponse;
import com.rehabiapp.api.application.service.AsignacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST para las asignaciones clínicas del paciente.
 *
 * <p>Gestiona dos tipos de asignaciones bajo el path /api/pacientes/{dniPac}:</p>
 * <ul>
 *   <li>/discapacidades — discapacidades asignadas al paciente y su nivel de progresión.</li>
 *   <li>/tratamientos — tratamientos visibles para el paciente en la app móvil.</li>
 * </ul>
 *
 * <p>Todos los endpoints requieren autenticación. Tanto SPECIALIST como NURSE
 * pueden consultar y modificar asignaciones clínicas.</p>
 */
@RestController
@RequestMapping("/api/pacientes/{dniPac}")
public class AsignacionController {

    private final AsignacionService asignacionService;

    public AsignacionController(AsignacionService asignacionService) {
        this.asignacionService = asignacionService;
    }

    /**
     * Lista todas las discapacidades asignadas a un paciente.
     *
     * <p>GET /api/pacientes/{dniPac}/discapacidades</p>
     *
     * @param dniPac DNI del paciente cuyas discapacidades se consultan.
     * @return 200 OK con la lista de asignaciones discapacidad-paciente.
     */
    @GetMapping("/discapacidades")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PacienteDiscapacidadResponse>> listarDiscapacidades(
            @PathVariable String dniPac) {
        return ResponseEntity.ok(asignacionService.listarDiscapacidades(dniPac));
    }

    /**
     * Asigna una discapacidad a un paciente con nivel de progresión opcional.
     *
     * <p>POST /api/pacientes/{dniPac}/discapacidades</p>
     *
     * @param dniPac  DNI del paciente al que asignar la discapacidad.
     * @param request DTO validado con el código de discapacidad, nivel y notas.
     * @return 201 Created con los datos de la asignación creada.
     */
    @PostMapping("/discapacidades")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PacienteDiscapacidadResponse> asignarDiscapacidad(
            @PathVariable String dniPac,
            @Valid @RequestBody PacienteDiscapacidadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(asignacionService.asignarDiscapacidad(dniPac, request));
    }

    /**
     * Desasigna una discapacidad de un paciente.
     *
     * <p>DELETE /api/pacientes/{dniPac}/discapacidades/{codDis}</p>
     *
     * @param dniPac DNI del paciente.
     * @param codDis Codigo de la discapacidad a desasignar.
     * @return 204 No Content si se elimino correctamente, 404 si no existe.
     */
    @DeleteMapping("/discapacidades/{codDis}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> desasignarDiscapacidad(
            @PathVariable String dniPac,
            @PathVariable String codDis) {
        asignacionService.desasignarDiscapacidad(dniPac, codDis);
        return ResponseEntity.noContent().build();
    }

    /**
     * Actualiza las notas clinicas de una discapacidad asignada al paciente.
     *
     * <p>PATCH /api/pacientes/{dniPac}/discapacidades/{codDis}/notas</p>
     *
     * @param dniPac  DNI del paciente.
     * @param codDis  Codigo de la discapacidad.
     * @param request DTO con las nuevas notas.
     * @return 200 OK con la asignacion actualizada.
     */
    @PatchMapping("/discapacidades/{codDis}/notas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PacienteDiscapacidadResponse> actualizarNotas(
            @PathVariable String dniPac,
            @PathVariable String codDis,
            @RequestBody ActualizarNotasRequest request) {
        return ResponseEntity.ok(asignacionService.actualizarNotas(dniPac, codDis, request));
    }

    /**
     * Actualiza el nivel de progresión clínica de una discapacidad del paciente.
     *
     * <p>PUT /api/pacientes/{dniPac}/discapacidades/{codDis}/nivel?idNivel=3</p>
     *
     * @param dniPac  DNI del paciente.
     * @param codDis  Código de la discapacidad asignada.
     * @param idNivel Nuevo identificador del nivel de progresión.
     * @return 200 OK con la asignación actualizada, 404 si no existe.
     */
    @PutMapping("/discapacidades/{codDis}/nivel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PacienteDiscapacidadResponse> actualizarNivel(
            @PathVariable String dniPac,
            @PathVariable String codDis,
            @RequestParam Integer idNivel) {
        return ResponseEntity.ok(asignacionService.actualizarNivel(dniPac, codDis, idNivel));
    }

    /**
     * Lista todos los tratamientos asignados a un paciente (visibles y ocultos).
     *
     * <p>GET /api/pacientes/{dniPac}/tratamientos</p>
     *
     * @param dniPac DNI del paciente cuyos tratamientos se consultan.
     * @return 200 OK con la lista de asignaciones tratamiento-paciente.
     */
    @GetMapping("/tratamientos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PacienteTratamientoResponse>> listarTratamientos(
            @PathVariable String dniPac) {
        return ResponseEntity.ok(asignacionService.listarTratamientos(dniPac));
    }

    /**
     * Asigna un tratamiento a un paciente con visible=true por defecto.
     *
     * <p>POST /api/pacientes/{dniPac}/tratamientos</p>
     *
     * @param dniPac  DNI del paciente.
     * @param request DTO con el codigo del tratamiento.
     * @return 201 Created con los datos de la asignacion creada.
     */
    @PostMapping("/tratamientos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PacienteTratamientoResponse> asignarTratamiento(
            @PathVariable String dniPac,
            @Valid @RequestBody PacienteTratamientoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(asignacionService.asignarTratamiento(dniPac, request));
    }

    /**
     * Desasigna un tratamiento de un paciente.
     *
     * <p>DELETE /api/pacientes/{dniPac}/tratamientos/{codTrat}</p>
     *
     * @param dniPac  DNI del paciente.
     * @param codTrat Codigo del tratamiento a desasignar.
     * @return 204 No Content si se elimino correctamente, 404 si no existe.
     */
    @DeleteMapping("/tratamientos/{codTrat}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> desasignarTratamiento(
            @PathVariable String dniPac,
            @PathVariable String codTrat) {
        asignacionService.desasignarTratamiento(dniPac, codTrat);
        return ResponseEntity.noContent().build();
    }

    /**
     * Alterna la visibilidad de un tratamiento para el paciente en la app móvil.
     * Si no existe la asignación, la crea con visible=true (primera activación).
     * Si existe, alterna el valor del campo visible (toggle).
     *
     * <p>PUT /api/pacientes/{dniPac}/tratamientos/{codTrat}/visibilidad</p>
     *
     * @param dniPac  DNI del paciente.
     * @param codTrat Código del tratamiento cuya visibilidad se alterna.
     * @return 200 OK con el nuevo estado de visibilidad del tratamiento.
     */
    @PutMapping("/tratamientos/{codTrat}/visibilidad")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PacienteTratamientoResponse> toggleVisibilidad(
            @PathVariable String dniPac,
            @PathVariable String codTrat) {
        return ResponseEntity.ok(asignacionService.toggleVisibilidad(dniPac, codTrat));
    }
}
