package com.rehabiapp.api.application.mapper;

import com.rehabiapp.api.application.dto.TratamientoResponse;
import com.rehabiapp.api.domain.entity.Tratamiento;
import org.mapstruct.Mapper;

/**
 * Mapper MapStruct para convertir la entidad Tratamiento en TratamientoResponse.
 *
 * <p>Los nombres de campo son idénticos entre entidad y DTO,
 * por lo que MapStruct genera el mapping automáticamente.</p>
 */
@Mapper(componentModel = "spring")
public interface TratamientoMapper {

    /**
     * Convierte la entidad Tratamiento en el DTO de respuesta.
     */
    TratamientoResponse toResponse(Tratamiento tratamiento);
}
