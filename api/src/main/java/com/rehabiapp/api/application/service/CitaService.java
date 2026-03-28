package com.rehabiapp.api.application.service;

import com.rehabiapp.api.application.dto.CitaRequest;
import com.rehabiapp.api.application.dto.CitaResponse;
import com.rehabiapp.api.application.dto.PageResponse;
import com.rehabiapp.api.application.mapper.CitaMapper;
import com.rehabiapp.api.domain.entity.Cita;
import com.rehabiapp.api.domain.entity.CitaId;
import com.rehabiapp.api.domain.entity.Paciente;
import com.rehabiapp.api.domain.entity.Sanitario;
import com.rehabiapp.api.domain.enums.AccionAuditoria;
import com.rehabiapp.api.domain.exception.RecursoNoEncontradoException;
import com.rehabiapp.api.domain.repository.CitaRepository;
import com.rehabiapp.api.domain.repository.PacienteRepository;
import com.rehabiapp.api.domain.repository.SanitarioRepository;
import com.rehabiapp.api.infrastructure.audit.AuditService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Servicio de aplicación para la gestión de citas médicas.
 *
 * <p>Las citas tienen eliminación física (DELETE real) a diferencia de pacientes
 * y sanitarios que usan soft delete. Una cita cancelada no necesita retención
 * por la Ley 41/2002 ya que no contiene datos clínicos directos.</p>
 *
 * <p>Toda operación se registra en audit_log para trazabilidad (ENS Alto).</p>
 */
@Service
@Transactional
public class CitaService {

    private final CitaRepository citaRepository;
    private final PacienteRepository pacienteRepository;
    private final SanitarioRepository sanitarioRepository;
    private final CitaMapper citaMapper;
    private final AuditService auditService;

    public CitaService(
            CitaRepository citaRepository,
            PacienteRepository pacienteRepository,
            SanitarioRepository sanitarioRepository,
            CitaMapper citaMapper,
            AuditService auditService
    ) {
        this.citaRepository = citaRepository;
        this.pacienteRepository = pacienteRepository;
        this.sanitarioRepository = sanitarioRepository;
        this.citaMapper = citaMapper;
        this.auditService = auditService;
    }

    /**
     * Devuelve todas las citas de una fecha concreta paginadas.
     * Usado para la vista de agenda diaria del centro.
     *
     * @param fecha    Fecha a consultar.
     * @param pageable Configuración de paginación.
     * @return Página de citas de la fecha indicada.
     */
    @Transactional(readOnly = true)
    public PageResponse<CitaResponse> listarPorFecha(LocalDate fecha, Pageable pageable) {
        auditService.registrar(AccionAuditoria.LEER, "cita", fecha.toString(), "Agenda diaria del centro");
        return PageResponse.de(
                citaRepository.findByIdFechaCita(fecha, pageable).map(citaMapper::toResponse)
        );
    }

    /**
     * Devuelve todas las citas de un sanitario concreto paginadas.
     * Usado para la agenda personal del profesional.
     *
     * @param dniSan   DNI del sanitario.
     * @param pageable Configuración de paginación.
     * @return Página de citas del sanitario indicado.
     */
    @Transactional(readOnly = true)
    public PageResponse<CitaResponse> listarPorSanitario(String dniSan, Pageable pageable) {
        auditService.registrar(AccionAuditoria.LEER, "cita", dniSan, "Agenda del sanitario");
        return PageResponse.de(
                citaRepository.findByIdDniSan(dniSan, pageable).map(citaMapper::toResponse)
        );
    }

    /**
     * Crea una nueva cita médica en el sistema.
     *
     * <p>Verifica que tanto el paciente como el sanitario existen y están activos
     * antes de crear la cita. La clave primaria compuesta garantiza que no pueden
     * crearse dos citas con la misma combinación (paciente, sanitario, fecha, hora).</p>
     *
     * @param request DTO con los datos de la nueva cita.
     * @return DTO de respuesta con la cita creada.
     * @throws RecursoNoEncontradoException si el paciente o el sanitario no existen.
     */
    public CitaResponse crear(CitaRequest request) {
        // Verificar que el paciente existe y está activo
        Paciente paciente = pacienteRepository.findByDniPacAndActivoTrue(request.dniPac())
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente no encontrado: " + request.dniPac()));

        // Verificar que el sanitario existe y está activo
        Sanitario sanitario = sanitarioRepository.findByDniSanAndActivoTrue(request.dniSan())
                .orElseThrow(() -> new RecursoNoEncontradoException("Sanitario no encontrado: " + request.dniSan()));

        // Construir la clave primaria compuesta y la entidad
        CitaId citaId = new CitaId(request.dniPac(), request.dniSan(), request.fechaCita(), request.horaCita());
        Cita cita = new Cita();
        cita.setId(citaId);
        cita.setPaciente(paciente);
        cita.setSanitario(sanitario);

        Cita guardada = citaRepository.save(cita);
        auditService.registrar(AccionAuditoria.CREAR, "cita",
                request.dniPac() + "-" + request.dniSan() + "-" + request.fechaCita() + "-" + request.horaCita(),
                "Cita creada");

        return citaMapper.toResponse(guardada);
    }

    /**
     * Elimina físicamente una cita del sistema.
     *
     * <p>Las citas no tienen datos clínicos directos, por lo que se permite
     * la eliminación física a diferencia de pacientes y sanitarios.</p>
     *
     * @param dniPac    DNI del paciente.
     * @param dniSan    DNI del sanitario.
     * @param fechaCita Fecha de la cita.
     * @param horaCita  Hora de la cita.
     * @throws RecursoNoEncontradoException si la cita no existe.
     */
    public void eliminar(String dniPac, String dniSan, LocalDate fechaCita, LocalTime horaCita) {
        CitaId citaId = new CitaId(dniPac, dniSan, fechaCita, horaCita);

        // Verificar que la cita existe antes de eliminar
        if (!citaRepository.existsById(citaId)) {
            throw new RecursoNoEncontradoException(
                    "Cita no encontrada: " + dniPac + "-" + dniSan + "-" + fechaCita + "-" + horaCita
            );
        }

        citaRepository.deleteById(citaId);
        auditService.registrar(AccionAuditoria.ELIMINAR, "cita",
                dniPac + "-" + dniSan + "-" + fechaCita + "-" + horaCita,
                "Cita eliminada");
    }
}
