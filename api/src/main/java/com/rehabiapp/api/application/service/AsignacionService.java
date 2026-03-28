package com.rehabiapp.api.application.service;

import com.rehabiapp.api.application.dto.PacienteDiscapacidadRequest;
import com.rehabiapp.api.application.dto.PacienteDiscapacidadResponse;
import com.rehabiapp.api.application.dto.PacienteTratamientoResponse;
import com.rehabiapp.api.application.mapper.PacienteDiscapacidadMapper;
import com.rehabiapp.api.application.mapper.PacienteTratamientoMapper;
import com.rehabiapp.api.domain.entity.Discapacidad;
import com.rehabiapp.api.domain.entity.NivelProgresion;
import com.rehabiapp.api.domain.entity.Paciente;
import com.rehabiapp.api.domain.entity.PacienteDiscapacidad;
import com.rehabiapp.api.domain.entity.PacienteDiscapacidadId;
import com.rehabiapp.api.domain.entity.PacienteTratamiento;
import com.rehabiapp.api.domain.entity.PacienteTratamientoId;
import com.rehabiapp.api.domain.entity.Tratamiento;
import com.rehabiapp.api.domain.enums.AccionAuditoria;
import com.rehabiapp.api.domain.exception.RecursoNoEncontradoException;
import com.rehabiapp.api.domain.repository.DiscapacidadRepository;
import com.rehabiapp.api.domain.repository.NivelProgresionRepository;
import com.rehabiapp.api.domain.repository.PacienteDiscapacidadRepository;
import com.rehabiapp.api.domain.repository.PacienteRepository;
import com.rehabiapp.api.domain.repository.PacienteTratamientoRepository;
import com.rehabiapp.api.domain.repository.TratamientoRepository;
import com.rehabiapp.api.infrastructure.audit.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de aplicación para la gestión de asignaciones clínicas.
 *
 * <p>Gestiona dos tipos de asignaciones:</p>
 * <ul>
 *   <li>PacienteDiscapacidad: qué discapacidades tiene cada paciente y en qué nivel.</li>
 *   <li>PacienteTratamiento: qué tratamientos ve cada paciente en la app móvil.</li>
 * </ul>
 *
 * <p>Toda operación se registra en audit_log para trazabilidad (ENS Alto, RGPD Art. 30).</p>
 */
@Service
@Transactional
public class AsignacionService {

    private final PacienteDiscapacidadRepository pacienteDiscapacidadRepository;
    private final PacienteTratamientoRepository pacienteTratamientoRepository;
    private final PacienteRepository pacienteRepository;
    private final DiscapacidadRepository discapacidadRepository;
    private final TratamientoRepository tratamientoRepository;
    private final NivelProgresionRepository nivelProgresionRepository;
    private final PacienteDiscapacidadMapper pacienteDiscapacidadMapper;
    private final PacienteTratamientoMapper pacienteTratamientoMapper;
    private final AuditService auditService;

    public AsignacionService(
            PacienteDiscapacidadRepository pacienteDiscapacidadRepository,
            PacienteTratamientoRepository pacienteTratamientoRepository,
            PacienteRepository pacienteRepository,
            DiscapacidadRepository discapacidadRepository,
            TratamientoRepository tratamientoRepository,
            NivelProgresionRepository nivelProgresionRepository,
            PacienteDiscapacidadMapper pacienteDiscapacidadMapper,
            PacienteTratamientoMapper pacienteTratamientoMapper,
            AuditService auditService
    ) {
        this.pacienteDiscapacidadRepository = pacienteDiscapacidadRepository;
        this.pacienteTratamientoRepository = pacienteTratamientoRepository;
        this.pacienteRepository = pacienteRepository;
        this.discapacidadRepository = discapacidadRepository;
        this.tratamientoRepository = tratamientoRepository;
        this.nivelProgresionRepository = nivelProgresionRepository;
        this.pacienteDiscapacidadMapper = pacienteDiscapacidadMapper;
        this.pacienteTratamientoMapper = pacienteTratamientoMapper;
        this.auditService = auditService;
    }

    /**
     * Asigna una discapacidad a un paciente con nivel de progresión opcional.
     *
     * @param dniPac  DNI del paciente.
     * @param request DTO con el código de discapacidad, nivel y notas.
     * @return DTO de respuesta con la asignación creada.
     * @throws RecursoNoEncontradoException si el paciente, la discapacidad o el nivel no existen.
     */
    public PacienteDiscapacidadResponse asignarDiscapacidad(String dniPac, PacienteDiscapacidadRequest request) {
        // Verificar que el paciente existe y está activo
        Paciente paciente = pacienteRepository.findByDniPacAndActivoTrue(dniPac)
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente no encontrado: " + dniPac));

        // Verificar que la discapacidad existe
        Discapacidad discapacidad = discapacidadRepository.findById(request.codDis())
                .orElseThrow(() -> new RecursoNoEncontradoException("Discapacidad no encontrada: " + request.codDis()));

        PacienteDiscapacidadId id = new PacienteDiscapacidadId(dniPac, request.codDis());
        PacienteDiscapacidad pd = new PacienteDiscapacidad();
        pd.setId(id);
        pd.setPaciente(paciente);
        pd.setDiscapacidad(discapacidad);
        pd.setNotas(request.notas());

        // Asignar nivel de progresión si se proporciona
        if (request.idNivel() != null) {
            NivelProgresion nivel = nivelProgresionRepository.findById(request.idNivel())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Nivel de progresión no encontrado: " + request.idNivel()));
            pd.setNivelProgresion(nivel);
        }

        PacienteDiscapacidad guardada = pacienteDiscapacidadRepository.save(pd);
        auditService.registrar(AccionAuditoria.CREAR, "paciente_discapacidad",
                dniPac + "-" + request.codDis(), "Discapacidad asignada al paciente");

        return pacienteDiscapacidadMapper.toResponse(guardada);
    }

    /**
     * Devuelve todas las discapacidades asignadas a un paciente.
     *
     * @param dniPac DNI del paciente.
     * @return Lista de asignaciones discapacidad-paciente.
     */
    @Transactional(readOnly = true)
    public List<PacienteDiscapacidadResponse> listarDiscapacidades(String dniPac) {
        auditService.registrar(AccionAuditoria.LEER, "paciente_discapacidad", dniPac,
                "Consulta discapacidades del paciente");
        return pacienteDiscapacidadRepository.findByIdDniPac(dniPac)
                .stream()
                .map(pacienteDiscapacidadMapper::toResponse)
                .toList();
    }

    /**
     * Actualiza el nivel de progresión clínica de una discapacidad asignada a un paciente.
     *
     * @param dniPac  DNI del paciente.
     * @param codDis  Código de la discapacidad.
     * @param idNivel Nuevo identificador del nivel de progresión.
     * @return DTO de respuesta con la asignación actualizada.
     * @throws RecursoNoEncontradoException si la asignación o el nivel no existen.
     */
    public PacienteDiscapacidadResponse actualizarNivel(String dniPac, String codDis, Integer idNivel) {
        PacienteDiscapacidadId id = new PacienteDiscapacidadId(dniPac, codDis);
        PacienteDiscapacidad pd = pacienteDiscapacidadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Asignación no encontrada para paciente: " + dniPac + " discapacidad: " + codDis));

        NivelProgresion nivel = nivelProgresionRepository.findById(idNivel)
                .orElseThrow(() -> new RecursoNoEncontradoException("Nivel de progresión no encontrado: " + idNivel));

        pd.setNivelProgresion(nivel);

        PacienteDiscapacidad actualizada = pacienteDiscapacidadRepository.save(pd);
        auditService.registrar(AccionAuditoria.ACTUALIZAR, "paciente_discapacidad",
                dniPac + "-" + codDis, "Nivel de progresión actualizado a: " + idNivel);

        return pacienteDiscapacidadMapper.toResponse(actualizada);
    }

    /**
     * Devuelve todos los tratamientos asignados a un paciente (visibles y ocultos).
     *
     * @param dniPac DNI del paciente.
     * @return Lista de asignaciones tratamiento-paciente.
     */
    @Transactional(readOnly = true)
    public List<PacienteTratamientoResponse> listarTratamientos(String dniPac) {
        auditService.registrar(AccionAuditoria.LEER, "paciente_tratamiento", dniPac,
                "Consulta tratamientos del paciente");
        return pacienteTratamientoRepository.findByIdDniPac(dniPac)
                .stream()
                .map(pacienteTratamientoMapper::toResponse)
                .toList();
    }

    /**
     * Cambia la visibilidad de un tratamiento para un paciente en la app móvil.
     *
     * <p>Si no existe la asignación, la crea con visible=true.
     * Si existe, alterna el valor del campo visible (toggle).
     * Esto permite al especialista ocultar tratamientos sin eliminar la asignación clínica.</p>
     *
     * @param dniPac  DNI del paciente.
     * @param codTrat Código del tratamiento.
     * @return DTO de respuesta con el nuevo estado de visibilidad.
     * @throws RecursoNoEncontradoException si el paciente o el tratamiento no existen.
     */
    public PacienteTratamientoResponse toggleVisibilidad(String dniPac, String codTrat) {
        PacienteTratamientoId id = new PacienteTratamientoId(dniPac, codTrat);

        PacienteTratamiento pt = pacienteTratamientoRepository.findById(id)
                .orElseGet(() -> {
                    // Crear la asignación si no existe
                    Paciente paciente = pacienteRepository.findByDniPacAndActivoTrue(dniPac)
                            .orElseThrow(() -> new RecursoNoEncontradoException("Paciente no encontrado: " + dniPac));
                    Tratamiento tratamiento = tratamientoRepository.findById(codTrat)
                            .orElseThrow(() -> new RecursoNoEncontradoException("Tratamiento no encontrado: " + codTrat));

                    PacienteTratamiento nuevo = new PacienteTratamiento();
                    nuevo.setId(id);
                    nuevo.setPaciente(paciente);
                    nuevo.setTratamiento(tratamiento);
                    nuevo.setVisible(true);
                    return nuevo;
                });

        // Toggle del campo visible
        pt.setVisible(!pt.isVisible());

        PacienteTratamiento guardado = pacienteTratamientoRepository.save(pt);
        auditService.registrar(AccionAuditoria.ACTUALIZAR, "paciente_tratamiento",
                dniPac + "-" + codTrat,
                "Visibilidad de tratamiento cambiada a: " + guardado.isVisible());

        return pacienteTratamientoMapper.toResponse(guardado);
    }
}
