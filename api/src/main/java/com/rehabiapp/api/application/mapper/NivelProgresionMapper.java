package com.rehabiapp.api.application.mapper;

import com.rehabiapp.api.application.dto.NivelProgresionResponse;
import com.rehabiapp.api.domain.entity.NivelProgresion;
import org.mapstruct.Mapper;

/**
 * Mapper MapStruct para convertir la entidad NivelProgresion en NivelProgresionResponse.
 *
 * <p>Los nombres de campo son idénticos entre entidad y DTO,
 * por lo que MapStruct genera el mapping automáticamente.</p>
 */
@Mapper(componentModel = "spring")
public interface NivelProgresionMapper {

    /**
     * Convierte la entidad NivelProgresion en el DTO de respuesta.
     */
    NivelProgresionResponse toResponse(NivelProgresion nivelProgresion);
}
