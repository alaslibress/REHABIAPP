package com.rehabiapp.api.application.mapper;

import com.rehabiapp.api.application.dto.PacienteDiscapacidadResponse;
import com.rehabiapp.api.domain.entity.PacienteDiscapacidad;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper MapStruct para convertir la entidad PacienteDiscapacidad en PacienteDiscapacidadResponse.
 *
 * <p>Los campos del EmbeddedId y las relaciones se navegan con expresiones de source.
 * nivelProgresion puede ser null si no se ha asignado nivel aún.</p>
 */
@Mapper(componentModel = "spring")
public interface PacienteDiscapacidadMapper {

    /**
     * Convierte la entidad PacienteDiscapacidad en el DTO de respuesta.
     * Navega desde el EmbeddedId y las relaciones hacia los campos del DTO.
     */
    @Mapping(target = "dniPac", source = "id.dniPac")
    @Mapping(target = "codDis", source = "id.codDis")
    @Mapping(target = "nombreDis", source = "discapacidad.nombreDis")
    @Mapping(target = "idNivel", source = "nivelProgresion.idNivel")
    @Mapping(target = "nombreNivel", source = "nivelProgresion.nombre")
    PacienteDiscapacidadResponse toResponse(PacienteDiscapacidad pd);
}
