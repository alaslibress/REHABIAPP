package com.rehabiapp.api.application.mapper;

import com.rehabiapp.api.application.dto.PacienteTratamientoResponse;
import com.rehabiapp.api.domain.entity.PacienteTratamiento;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper MapStruct para convertir la entidad PacienteTratamiento en PacienteTratamientoResponse.
 *
 * <p>visible: boolean primitivo en la entidad, Boolean wrapper en el DTO.</p>
 */
@Mapper(componentModel = "spring")
public interface PacienteTratamientoMapper {

    /**
     * Convierte la entidad PacienteTratamiento en el DTO de respuesta.
     * Navega desde el EmbeddedId y la relación tratamiento.
     */
    @Mapping(target = "dniPac", source = "id.dniPac")
    @Mapping(target = "codTrat", source = "id.codTrat")
    @Mapping(target = "nombreTrat", source = "tratamiento.nombreTrat")
    @Mapping(target = "visible", expression = "java(pt.isVisible())")
    PacienteTratamientoResponse toResponse(PacienteTratamiento pt);
}
