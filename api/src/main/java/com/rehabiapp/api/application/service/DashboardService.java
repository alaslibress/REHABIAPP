package com.rehabiapp.api.application.service;

import com.rehabiapp.api.application.dto.DashboardResponse;
import com.rehabiapp.api.application.dto.DashboardResponse.DiscapacidadActivaDto;
import com.rehabiapp.api.application.dto.DashboardResponse.JuegoDesbloqueadoDto;
import com.rehabiapp.api.application.dto.DashboardResponse.PacienteResumenDto;
import com.rehabiapp.api.application.dto.DashboardResponse.ProximaCitaDto;
import com.rehabiapp.api.application.dto.DashboardResponse.TratamientoVisibleDto;
import com.rehabiapp.api.application.dto.UltimaSesionDto;
import com.rehabiapp.api.domain.entity.Cita;
import com.rehabiapp.api.domain.entity.NivelProgresion;
import com.rehabiapp.api.domain.entity.Paciente;
import com.rehabiapp.api.domain.entity.PacienteDiscapacidad;
import com.rehabiapp.api.domain.entity.PacienteTratamiento;
import com.rehabiapp.api.domain.entity.Tratamiento;
import com.rehabiapp.api.domain.entity.TratamientoVideojuego;
import com.rehabiapp.api.domain.entity.Videojuego;
import com.rehabiapp.api.domain.enums.AccionAuditoria;
import com.rehabiapp.api.domain.exception.RecursoNoEncontradoException;
import com.rehabiapp.api.domain.repository.CitaRepository;
import com.rehabiapp.api.domain.repository.PacienteDiscapacidadRepository;
import com.rehabiapp.api.domain.repository.PacienteRepository;
import com.rehabiapp.api.domain.repository.PacienteTratamientoRepository;
import com.rehabiapp.api.domain.repository.TratamientoVideojuegoRepository;
import com.rehabiapp.api.infrastructure.audit.AuditService;
import com.rehabiapp.api.infrastructure.client.DataPipelineClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de aplicacion que construye el dashboard agregado del paciente.
 *
 * <p>Consume varios repositorios del API y la ultima sesion del pipeline `/data`.
 * Marca como desbloqueados los videojuegos cuyo nivel de tratamiento es menor
 * o igual al nivel actual del paciente para la discapacidad correspondiente.</p>
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final PacienteRepository pacienteRepository;
    private final PacienteDiscapacidadRepository pacienteDiscapacidadRepository;
    private final PacienteTratamientoRepository pacienteTratamientoRepository;
    private final TratamientoVideojuegoRepository tratamientoVideojuegoRepository;
    private final CitaRepository citaRepository;
    private final DataPipelineClient dataClient;
    private final AuditService auditService;

    public DashboardService(PacienteRepository pacienteRepository,
                            PacienteDiscapacidadRepository pacienteDiscapacidadRepository,
                            PacienteTratamientoRepository pacienteTratamientoRepository,
                            TratamientoVideojuegoRepository tratamientoVideojuegoRepository,
                            CitaRepository citaRepository,
                            DataPipelineClient dataClient,
                            AuditService auditService) {
        this.pacienteRepository = pacienteRepository;
        this.pacienteDiscapacidadRepository = pacienteDiscapacidadRepository;
        this.pacienteTratamientoRepository = pacienteTratamientoRepository;
        this.tratamientoVideojuegoRepository = tratamientoVideojuegoRepository;
        this.citaRepository = citaRepository;
        this.dataClient = dataClient;
        this.auditService = auditService;
    }

    public DashboardResponse obtener(String dni) {
        Paciente paciente = pacienteRepository.findById(dni)
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente no encontrado: " + dni));

        // Resumen del paciente
        PacienteResumenDto resumen = new PacienteResumenDto(
                paciente.getDniPac(),
                paciente.getNombrePac(),
                paciente.getApellido1Pac(),
                paciente.getApellido2Pac(),
                paciente.getEdadPac()
        );

        // Discapacidades activas con nivel actual
        List<PacienteDiscapacidad> discapacidades = pacienteDiscapacidadRepository
                .findByIdDniPac(dni);
        List<DiscapacidadActivaDto> discapacidadesDto = new ArrayList<>();
        for (PacienteDiscapacidad pd : discapacidades) {
            NivelProgresion nivel = pd.getNivelProgresion();
            discapacidadesDto.add(new DiscapacidadActivaDto(
                    pd.getId().getCodDis(),
                    pd.getDiscapacidad() != null ? pd.getDiscapacidad().getNombreDis() : null,
                    nivel != null ? nivel.getIdNivel() : null,
                    nivel != null ? nivel.getNombre() : null,
                    nivel != null ? nivel.getOrden() : null
            ));
        }

        // Tratamientos visibles del paciente
        List<PacienteTratamiento> tratamientos = pacienteTratamientoRepository
                .findByIdDniPac(dni);
        List<TratamientoVisibleDto> tratamientosDto = new ArrayList<>();
        Integer ordenMaxNivelPaciente = discapacidadesDto.stream()
                .map(DiscapacidadActivaDto::ordenNivelActual)
                .filter(o -> o != null)
                .max(Integer::compareTo)
                .orElse(null);
        for (PacienteTratamiento pt : tratamientos) {
            if (!pt.isVisible()) continue;
            Tratamiento t = pt.getTratamiento();
            if (t == null) continue;
            tratamientosDto.add(new TratamientoVisibleDto(
                    t.getCodTrat(),
                    t.getNombreTrat(),
                    t.getNivel() != null ? t.getNivel().getIdNivel() : null,
                    t.getNivel() != null ? t.getNivel().getOrden() : null
            ));
        }

        // Juegos desbloqueados: por cada tratamiento visible, sus videojuegos asociados.
        // Desbloqueado si el orden del nivel del paciente >= orden del nivel del tratamiento.
        List<JuegoDesbloqueadoDto> juegosDto = new ArrayList<>();
        for (TratamientoVisibleDto tv : tratamientosDto) {
            List<TratamientoVideojuego> vinculos = tratamientoVideojuegoRepository
                    .findByIdCodTrat(tv.codTrat());
            for (TratamientoVideojuego tv2 : vinculos) {
                Videojuego v = tv2.getVideojuego();
                if (v == null || !v.isActivo()) continue;
                boolean desbloqueado = ordenMaxNivelPaciente != null
                        && tv.ordenNivel() != null
                        && ordenMaxNivelPaciente >= tv.ordenNivel();
                if (tv.ordenNivel() == null) {
                    desbloqueado = true;
                }
                juegosDto.add(new JuegoDesbloqueadoDto(
                        v.getIdVideojuego(),
                        v.getCodigo(),
                        v.getNombre(),
                        v.getUrlUnity(),
                        v.getParteCuerpo(),
                        desbloqueado
                ));
            }
        }

        // Ultima sesion via /data — tolerar fallos para no romper el dashboard.
        UltimaSesionDto ultimaSesion = null;
        try {
            ultimaSesion = dataClient.ultimaSesion(dni);
        } catch (Exception e) {
            log.warn("No se pudo obtener la ultima sesion para {}: {}", dni, e.getMessage());
        }

        // Proxima cita
        ProximaCitaDto proximaCita = null;
        List<Cita> proximas = citaRepository.findProximasByPaciente(
                dni, LocalDate.now(), LocalTime.now(), PageRequest.of(0, 1));
        if (!proximas.isEmpty()) {
            Cita c = proximas.get(0);
            proximaCita = new ProximaCitaDto(
                    c.getId().getDniSan(),
                    c.getId().getFechaCita(),
                    c.getId().getHoraCita()
            );
        }

        auditService.registrar(AccionAuditoria.READ, "Paciente", dni,
                "Lectura del dashboard agregado");

        return new DashboardResponse(
                resumen, discapacidadesDto, tratamientosDto, juegosDto,
                ultimaSesion, proximaCita
        );
    }
}
