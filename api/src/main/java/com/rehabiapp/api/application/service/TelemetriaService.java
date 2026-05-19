package com.rehabiapp.api.application.service;

import com.rehabiapp.api.application.dto.TelemetriaSesionRequest;
import com.rehabiapp.api.application.event.RegenerarMdEvent;
import com.rehabiapp.api.domain.entity.Paciente;
import com.rehabiapp.api.domain.entity.PacienteDiscapacidad;
import com.rehabiapp.api.domain.entity.PacienteTratamiento;
import com.rehabiapp.api.domain.enums.AccionAuditoria;
import com.rehabiapp.api.domain.exception.AccesoNoPermitidoException;
import com.rehabiapp.api.domain.exception.RecursoNoEncontradoException;
import com.rehabiapp.api.domain.repository.PacienteDiscapacidadRepository;
import com.rehabiapp.api.domain.repository.PacienteRepository;
import com.rehabiapp.api.domain.repository.PacienteTratamientoRepository;
import com.rehabiapp.api.infrastructure.audit.AuditService;
import com.rehabiapp.api.infrastructure.client.DataPipelineClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de aplicacion para la ingesta de telemetria de juegos terapeuticos.
 *
 * <p>Valida que el paciente exista y este activo, enriquece el payload
 * con la discapacidad/tratamiento inferidos cuando faltan, llama al pipeline
 * `/data` para persistir la sesion y publica un evento asincrono que
 * regenerara el Markdown del paciente.</p>
 */
@Service
@Transactional
public class TelemetriaService {

    private final PacienteRepository pacienteRepository;
    private final PacienteDiscapacidadRepository pacienteDiscapacidadRepository;
    private final PacienteTratamientoRepository pacienteTratamientoRepository;
    private final DataPipelineClient dataClient;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    public TelemetriaService(PacienteRepository pacienteRepository,
                             PacienteDiscapacidadRepository pacienteDiscapacidadRepository,
                             PacienteTratamientoRepository pacienteTratamientoRepository,
                             DataPipelineClient dataClient,
                             AuditService auditService,
                             ApplicationEventPublisher eventPublisher) {
        this.pacienteRepository = pacienteRepository;
        this.pacienteDiscapacidadRepository = pacienteDiscapacidadRepository;
        this.pacienteTratamientoRepository = pacienteTratamientoRepository;
        this.dataClient = dataClient;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    public Map<String, Object> ingestar(TelemetriaSesionRequest req) {
        Paciente paciente = pacienteRepository.findById(req.dniPaciente())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Paciente no encontrado: " + req.dniPaciente()));
        if (!paciente.isActivo()) {
            throw new AccesoNoPermitidoException(
                    "Paciente inactivo: " + req.dniPaciente());
        }

        // Inferencia de campos opcionales a partir de las asignaciones del paciente.
        String disabilityId = req.disabilityId();
        String codTrat = req.codTratamiento();
        String parteCuerpo = req.parteCuerpo();
        String tratamientoNombre = null;

        if (disabilityId == null) {
            List<PacienteDiscapacidad> dis = pacienteDiscapacidadRepository
                    .findByIdDniPac(req.dniPaciente());
            if (!dis.isEmpty()) {
                disabilityId = dis.get(dis.size() - 1).getId().getCodDis();
            }
        }
        if (codTrat == null || tratamientoNombre == null) {
            List<PacienteTratamiento> tratos = pacienteTratamientoRepository
                    .findByIdDniPac(req.dniPaciente());
            if (!tratos.isEmpty()) {
                PacienteTratamiento ult = tratos.get(tratos.size() - 1);
                if (codTrat == null) {
                    codTrat = ult.getId().getCodTrat();
                }
                if (ult.getTratamiento() != null) {
                    tratamientoNombre = ult.getTratamiento().getNombreTrat();
                }
            }
        }

        // Los nombres de campo deben coincidir con GameSessionIngestionRequest del pipeline /data
        Map<String, Object> payload = new HashMap<>();
        payload.put("patientDni", req.dniPaciente());
        payload.put("gameId", req.videojuegoCodigo());
        payload.put("disabilityId", disabilityId);
        payload.put("progressionLevel", 1);
        payload.put("codTrat", codTrat);
        payload.put("tratamientoNombre", tratamientoNombre);
        payload.put("parteCuerpo", parteCuerpo);
        payload.put("sessionStart", req.inicio());
        payload.put("sessionEnd", req.fin());
        payload.put("durationSeconds", req.duracionMs() / 1000L);
        payload.put("score", req.puntuacion() != null ? req.puntuacion().doubleValue() : null);
        payload.put("completed", true);
        payload.put("schemaVersion", "v1");
        // metricas del cliente Unity se exponen como rawMetrics polimorficas
        payload.put("rawMetrics", req.metricas() != null ? req.metricas() : Map.of("score", req.puntuacion() != null ? req.puntuacion() : 0));

        Map<String, Object> respuesta = dataClient.ingestar(payload);

        auditService.registrar(AccionAuditoria.CREATE, "TelemetriaSesion",
                req.dniPaciente(),
                "Ingesta de sesion de juego: " + req.videojuegoCodigo());

        // Disparo asincrono de la regeneracion del Markdown
        eventPublisher.publishEvent(new RegenerarMdEvent(req.dniPaciente()));

        return respuesta != null ? respuesta : Map.of();
    }
}
