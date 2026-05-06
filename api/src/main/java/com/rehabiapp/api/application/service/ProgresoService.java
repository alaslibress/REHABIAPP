package com.rehabiapp.api.application.service;

import com.rehabiapp.api.application.dto.CheckProgresoResponse;
import com.rehabiapp.api.application.dto.ProgresoTratamientoResponse;
import com.rehabiapp.api.domain.entity.Paciente;
import com.rehabiapp.api.domain.enums.AccionAuditoria;
import com.rehabiapp.api.domain.exception.RecursoNoEncontradoException;
import com.rehabiapp.api.domain.repository.PacienteRepository;
import com.rehabiapp.api.infrastructure.audit.AuditService;
import com.rehabiapp.api.infrastructure.client.DataPipelineClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Servicio de aplicacion para el progreso clinico del paciente.
 *
 * <p>Proxia las consultas al pipeline `/data` y registra cada acceso en
 * el log de auditoria (Ley 41/2002). El Markdown se cachea en
 * `paciente.archivo_progreso_md` cuando es solicitado.</p>
 */
@Service
@Transactional
public class ProgresoService {

    private final DataPipelineClient dataClient;
    private final PacienteRepository pacienteRepository;
    private final AuditService auditService;

    public ProgresoService(DataPipelineClient dataClient,
                           PacienteRepository pacienteRepository,
                           AuditService auditService) {
        this.dataClient = dataClient;
        this.pacienteRepository = pacienteRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public CheckProgresoResponse checkNuevosDatos(String dni, Instant since) {
        verificarPacienteExiste(dni);
        auditService.registrar(AccionAuditoria.READ, "Paciente", dni,
                "Consulta check progreso since=" + since);
        return dataClient.checkNuevosDatos(dni, since);
    }

    @Transactional(readOnly = true)
    public List<ProgresoTratamientoResponse> obtenerProgreso(String dni) {
        verificarPacienteExiste(dni);
        auditService.registrar(AccionAuditoria.READ, "Paciente", dni,
                "Consulta progreso por tratamiento");
        return dataClient.obtenerProgreso(dni);
    }

    /**
     * Devuelve el Markdown y lo cachea en `paciente.archivo_progreso_md`.
     */
    public String obtenerMarkdown(String dni) {
        Paciente paciente = pacienteRepository.findById(dni)
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente no encontrado: " + dni));
        String md = dataClient.obtenerMarkdown(dni);
        paciente.setArchivoProgresoMd(md);
        paciente.setProgresoMdActualizadoEn(LocalDateTime.now(ZoneId.systemDefault()));
        pacienteRepository.save(paciente);

        auditService.registrar(AccionAuditoria.READ, "Paciente", dni,
                "Descarga Markdown de progreso");
        return md;
    }

    public void regenerarMarkdown(String dni) {
        verificarPacienteExiste(dni);
        dataClient.regenerarMarkdown(dni);
        auditService.registrar(AccionAuditoria.UPDATE, "Paciente", dni,
                "Regeneracion forzada del Markdown");
    }

    private void verificarPacienteExiste(String dni) {
        if (!pacienteRepository.existsById(dni)) {
            throw new RecursoNoEncontradoException("Paciente no encontrado: " + dni);
        }
    }
}
