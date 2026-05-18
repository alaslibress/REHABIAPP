package com.rehabiapp.api.application.service;

import com.rehabiapp.api.application.dto.CheckProgresoResponse;
import com.rehabiapp.api.application.dto.ProgresoResumenResponse;
import com.rehabiapp.api.application.dto.ProgresoTratamientoResponse;
import com.rehabiapp.api.domain.entity.Paciente;
import com.rehabiapp.api.domain.enums.AccionAuditoria;
import com.rehabiapp.api.domain.exception.RecursoNoEncontradoException;
import com.rehabiapp.api.domain.repository.PacienteRepository;
import com.rehabiapp.api.infrastructure.audit.AuditService;
import com.rehabiapp.api.infrastructure.client.DataPipelineClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
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

    private static final Logger LOG = LoggerFactory.getLogger(ProgresoService.class);

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
        try {
            return dataClient.checkNuevosDatos(dni, since);
        } catch (RestClientException e) {
            LOG.warn("Pipeline de datos no disponible para check de progreso (dni={}): {}", dni, e.getMessage());
            return new CheckProgresoResponse(false, null, 0);
        } catch (Exception e) {
            LOG.error("Error inesperado al consultar check progreso (dni={}): {}", dni, e.getMessage(), e);
            return new CheckProgresoResponse(false, null, 0);
        }
    }

    @Transactional(readOnly = true)
    public List<ProgresoTratamientoResponse> obtenerProgreso(String dni) {
        verificarPacienteExiste(dni);
        auditService.registrar(AccionAuditoria.READ, "Paciente", dni,
                "Consulta progreso por tratamiento");
        try {
            return dataClient.obtenerProgreso(dni);
        } catch (RestClientException e) {
            LOG.warn("Pipeline de datos no disponible para progreso (dni={}): {}", dni, e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            LOG.error("Error inesperado al consultar progreso (dni={}): {}", dni, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Devuelve el Markdown y lo cachea en `paciente.archivo_progreso_md`.
     * Si el pipeline no esta disponible devuelve el cache existente o cadena vacia.
     */
    public String obtenerMarkdown(String dni) {
        Paciente paciente = pacienteRepository.findById(dni)
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente no encontrado: " + dni));
        try {
            String md = dataClient.obtenerMarkdown(dni);
            paciente.setArchivoProgresoMd(md);
            paciente.setProgresoMdActualizadoEn(LocalDateTime.now(ZoneId.systemDefault()));
            pacienteRepository.save(paciente);
            auditService.registrar(AccionAuditoria.READ, "Paciente", dni,
                    "Descarga Markdown de progreso");
            return md;
        } catch (RestClientException e) {
            LOG.warn("Pipeline de datos no disponible para markdown (dni={}): {}", dni, e.getMessage());
            // Devolver cache si existe
            String cache = paciente.getArchivoProgresoMd();
            return cache != null ? cache : "";
        } catch (Exception e) {
            LOG.error("Error inesperado al obtener markdown (dni={}): {}", dni, e.getMessage(), e);
            String cache = paciente.getArchivoProgresoMd();
            return cache != null ? cache : "";
        }
    }

    /**
     * Devuelve el resumen plano de progreso (totalSessions, averageScore,
     * improvementRate, lastSessionDate). Tolera fallos del pipeline devolviendo
     * un resumen vacio (todos los campos a 0/null) para no romper el dashboard.
     */
    @Transactional(readOnly = true)
    public ProgresoResumenResponse obtenerResumen(String dni) {
        verificarPacienteExiste(dni);
        auditService.registrar(AccionAuditoria.READ, "Paciente", dni,
                "Consulta resumen de progreso");
        try {
            return dataClient.obtenerResumen(dni);
        } catch (RestClientException e) {
            LOG.warn("Pipeline de datos no disponible para resumen (dni={}): {}", dni, e.getMessage());
            return new ProgresoResumenResponse(0L, null, null, null);
        }
    }

    public void regenerarMarkdown(String dni) {
        verificarPacienteExiste(dni);
        try {
            dataClient.regenerarMarkdown(dni);
            auditService.registrar(AccionAuditoria.UPDATE, "Paciente", dni,
                    "Regeneracion forzada del Markdown");
        } catch (RestClientException e) {
            LOG.warn("Pipeline de datos no disponible para regenerar markdown (dni={}): {}", dni, e.getMessage());
        }
    }

    private void verificarPacienteExiste(String dni) {
        if (!pacienteRepository.existsById(dni)) {
            throw new RecursoNoEncontradoException("Paciente no encontrado: " + dni);
        }
    }
}
