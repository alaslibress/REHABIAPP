package com.rehabiapp.api.application.mapper;

import com.rehabiapp.api.application.dto.DiscapacidadResponse;
import com.rehabiapp.api.domain.entity.Discapacidad;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper MapStruct para convertir la entidad Discapacidad en DiscapacidadResponse.
 *
 * <p>necesitaProtesis: boolean primitivo en la entidad, Boolean wrapper en el DTO.</p>
 */
@Mapper(componentModel = "spring")
public interface DiscapacidadMapper {

    /**
     * Convierte la entidad Discapacidad en el DTO de respuesta.
     */
    @Mapping(target = "necesitaProtesis", expression = "java(discapacidad.isNecesitaProtesis())")
    DiscapacidadResponse toResponse(Discapacidad discapacidad);
}
