package com.rehabiapp.api.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Respuesta agregada del dashboard del paciente para la app movil.
 *
 * <p>Construida por {@code DashboardService} a partir de varias entidades
 * del API y de la ultima sesion del pipeline `/data`.</p>
 */
@Schema(description = "Vista agregada del dashboard del paciente")
public record DashboardResponse(
        PacienteResumenDto paciente,
        List<DiscapacidadActivaDto> discapacidadesActivas,
        List<TratamientoVisibleDto> tratamientosVisibles,
        List<JuegoDesbloqueadoDto> juegosDesbloqueados,
        UltimaSesionDto ultimaSesionJuego,
        ProximaCitaDto proximaCita
) {

    @Schema(description = "Resumen identificativo del paciente")
    public record PacienteResumenDto(
            String dniPac,
            String nombrePac,
            String apellido1Pac,
            String apellido2Pac,
            Integer edadPac
    ) {}

    @Schema(description = "Discapacidad activa del paciente con su nivel de progresion")
    public record DiscapacidadActivaDto(
            String codDis,
            String nombreDis,
            Integer idNivelActual,
            String nombreNivelActual,
            Integer ordenNivelActual
    ) {}

    @Schema(description = "Tratamiento visible para el paciente")
    public record TratamientoVisibleDto(
            String codTrat,
            String nombreTrat,
            Integer idNivel,
            Integer ordenNivel
    ) {}

    @Schema(description = "Videojuego potencialmente disponible para el paciente")
    public record JuegoDesbloqueadoDto(
            Long idVideojuego,
            String codigo,
            String nombre,
            String urlUnity,
            String parteCuerpo,
            boolean desbloqueado
    ) {}

    @Schema(description = "Resumen de la proxima cita del paciente")
    public record ProximaCitaDto(
            String dniSanitario,
            LocalDate fecha,
            LocalTime hora
    ) {}
}
