package com.rehabiapp.api.application.mapper;

import com.rehabiapp.api.application.dto.TratamientoResponse;
import com.rehabiapp.api.domain.entity.Tratamiento;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper MapStruct para convertir la entidad Tratamiento en TratamientoResponse.
 *
 * <p>Los campos idNivel y nombreNivel se obtienen de la relación ManyToOne
 * con NivelProgresion (nullable). Si el tratamiento no tiene nivel asignado,
 * ambos campos devuelven null.</p>
 */
@Mapper(componentModel = "spring")
public interface TratamientoMapper {

    /**
     * Convierte la entidad Tratamiento en el DTO de respuesta.
     * Navega la relación nivel para extraer id y nombre del nivel de progresión.
     */
    @Mapping(target = "idNivel",     expression = "java(tratamiento.getNivel() != null ? tratamiento.getNivel().getIdNivel() : null)")
    @Mapping(target = "nombreNivel", expression = "java(tratamiento.getNivel() != null ? tratamiento.getNivel().getNombre() : null)")
    TratamientoResponse toResponse(Tratamiento tratamiento);
}
