package com.rehabiapp.api.application.mapper;

import com.rehabiapp.api.application.dto.ArticulacionResponse;
import com.rehabiapp.api.domain.entity.Articulacion;
import org.mapstruct.Mapper;

/**
 * Mapper MapStruct para convertir la entidad Articulacion en ArticulacionResponse.
 */
@Mapper(componentModel = "spring")
public interface ArticulacionMapper {

    ArticulacionResponse toResponse(Articulacion articulacion);
}
