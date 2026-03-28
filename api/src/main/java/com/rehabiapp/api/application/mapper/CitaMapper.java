package com.rehabiapp.api.application.mapper;

import com.rehabiapp.api.application.dto.CitaResponse;
import com.rehabiapp.api.domain.entity.Cita;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper MapStruct para convertir la entidad Cita en CitaResponse.
 *
 * <p>La entidad Cita tiene clave primaria compuesta (@EmbeddedId CitaId).
 * Los cuatro campos del response se navegan desde cita.id.*</p>
 */
@Mapper(componentModel = "spring")
public interface CitaMapper {

    /**
     * Convierte la entidad Cita en el DTO de respuesta.
     * Navega los campos desde el EmbeddedId.
     */
    @Mapping(target = "dniPac", source = "id.dniPac")
    @Mapping(target = "dniSan", source = "id.dniSan")
    @Mapping(target = "fechaCita", source = "id.fechaCita")
    @Mapping(target = "horaCita", source = "id.horaCita")
    CitaResponse toResponse(Cita cita);
}
