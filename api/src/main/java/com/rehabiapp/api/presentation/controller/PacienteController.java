package com.rehabiapp.api.presentation.controller;

import com.rehabiapp.api.application.dto.PageResponse;
import com.rehabiapp.api.application.dto.PacienteRequest;
import com.rehabiapp.api.application.dto.PacienteResponse;
import com.rehabiapp.api.application.service.PacienteService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Controlador REST para la gestión de pacientes.
 *
 * <p>CRITICO — Datos de salud (RGPD Art. 9, categoría especial):</p>
 * <ul>
 *   <li>Los campos clínicos (alergias, antecedentes, medicación actual) se cifran
 *       automáticamente por CampoClinicoConverter. El controlador trabaja con texto plano.</li>
 *   <li>Toda operación, incluyendo lecturas, queda registrada en audit_log (Ley 41/2002).</li>
 *   <li>El DELETE realiza baja lógica (soft delete). Retención mínima 5 años.</li>
 * </ul>
 *
 * <p>RBAC aplicado por método:</p>
 * <ul>
 *   <li>GET, POST, PUT — accesible para cualquier sanitario autenticado.</li>
 *   <li>DELETE — solo rol SPECIALIST puede dar de baja pacientes.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {

    private final PacienteService pacienteService;

    public PacienteController(PacienteService pacienteService) {
        this.pacienteService = pacienteService;
    }

    /**
     * Lista todos los pacientes activos paginados.
     *
     * <p>GET /api/pacientes?page=0&size=20&sort=apellido1Pac,asc</p>
     *
     * @param pageable Parámetros de paginación y ordenación desde la query string.
     * @return 200 OK con página de pacientes activos.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<PacienteResponse>> listar(Pageable pageable) {
        return ResponseEntity.ok(pacienteService.listar(pageable));
    }

    /**
     * Devuelve un paciente activo por su DNI.
     * El acceso queda registrado en audit_log (Ley 41/2002 — acceso a historial clínico).
     *
     * <p>GET /api/pacientes/{dni}</p>
     *
     * @param dni DNI del paciente a consultar.
     * @return 200 OK con los datos del paciente, 404 si no existe.
     */
    @GetMapping("/{dni}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PacienteResponse> obtener(@PathVariable String dni) {
        return ResponseEntity.ok(pacienteService.obtenerPorDni(dni));
    }

    /**
     * Crea un nuevo paciente en el sistema.
     * Los campos clínicos se cifran automáticamente al persistir.
     *
     * <p>POST /api/pacientes</p>
     *
     * @param request DTO validado con los datos del nuevo paciente.
     * @return 201 Created con los datos del paciente creado.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PacienteResponse> crear(@Valid @RequestBody PacienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pacienteService.crear(request));
    }

    /**
     * Actualiza los datos de un paciente existente.
     * Los campos clínicos se re-cifran con nuevo IV aleatorio por escritura (RGPD Art. 9).
     *
     * <p>PUT /api/pacientes/{dni}</p>
     *
     * @param dni     DNI del paciente a actualizar.
     * @param request DTO validado con los nuevos datos.
     * @return 200 OK con los datos actualizados, 404 si no existe.
     */
    @PutMapping("/{dni}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PacienteResponse> actualizar(
            @PathVariable String dni,
            @Valid @RequestBody PacienteRequest request) {
        return ResponseEntity.ok(pacienteService.actualizar(dni, request));
    }

    /**
     * Da de baja lógica a un paciente (soft delete).
     * Solo accesible para rol SPECIALIST (RBAC).
     * NUNCA se elimina físicamente el registro (Ley 41/2002, RGPD Art. 17).
     *
     * <p>DELETE /api/pacientes/{dni}</p>
     *
     * @param dni DNI del paciente a dar de baja.
     * @return 204 No Content si la baja fue exitosa, 404 si no existe.
     */
    @DeleteMapping("/{dni}")
    @PreAuthorize("hasRole('SPECIALIST')")
    public ResponseEntity<Void> eliminar(@PathVariable String dni) {
        pacienteService.eliminar(dni);
        return ResponseEntity.noContent().build();
    }

    /**
     * Busca pacientes activos por texto libre (case-insensitive).
     * Busca en DNI, nombre, apellidos, email y número de la Seguridad Social.
     *
     * <p>GET /api/pacientes/buscar?texto=garcia&page=0&size=20&sort=apellido1Pac,asc</p>
     *
     * @param texto    Término de búsqueda libre.
     * @param pageable Parámetros de paginación y ordenación desde la query string.
     * @return 200 OK con página de pacientes activos que coinciden.
     */
    @GetMapping("/buscar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<PacienteResponse>> buscar(
            @RequestParam String texto,
            Pageable pageable) {
        return ResponseEntity.ok(pacienteService.buscar(texto, pageable));
    }

    /**
     * Sube o reemplaza la fotografía de un paciente.
     * Recibe el archivo como multipart/form-data con campo "foto".
     *
     * <p>POST /api/pacientes/{dni}/foto</p>
     *
     * @param dni  DNI del paciente al que se sube la foto.
     * @param foto Archivo de imagen (image/png o image/jpeg).
     * @return 200 OK si se guardó, 404 si el paciente no existe.
     */
    @PostMapping(value = "/{dni}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> subirFoto(
            @PathVariable String dni,
            @RequestParam("foto") MultipartFile foto) throws IOException {
        pacienteService.guardarFoto(dni, foto.getBytes());
        return ResponseEntity.ok().build();
    }

    /**
     * Devuelve la fotografía de un paciente como bytes.
     * Responde con Content-Type image/png.
     *
     * <p>GET /api/pacientes/{dni}/foto</p>
     *
     * @param dni DNI del paciente.
     * @return 200 OK con los bytes de la imagen, 204 si no tiene foto, 404 si no existe el paciente.
     */
    @GetMapping("/{dni}/foto")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> obtenerFoto(@PathVariable String dni) {
        byte[] foto = pacienteService.obtenerFoto(dni);
        if (foto == null || foto.length == 0) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(foto);
    }
}
