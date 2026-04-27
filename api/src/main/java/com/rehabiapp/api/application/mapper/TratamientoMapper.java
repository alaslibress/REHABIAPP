package com.rehabiapp.api.application.mapper;

import com.rehabiapp.api.application.dto.TratamientoResponse;
import com.rehabiapp.api.domain.entity.Tratamiento;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper MapStruct para convertir la entidad Tratamiento en TratamientoResponse.
 *
 * <p>Los campos idNivel y nombreNivel se obtienen de la relacion ManyToOne
 * con NivelProgresion (nullable). Si el tratamiento no tiene nivel asignado,
 * ambos campos devuelven null.</p>
 *
 * <p>codJuego y nombreJuego se obtienen de la relacion ManyToOne con Juego (nullable).
 * Si el tratamiento no tiene juego asociado, ambos campos devuelven null.</p>
 */
@Mapper(componentModel = "spring")
public interface TratamientoMapper {

    @Mapping(target = "idNivel",     expression = "java(tratamiento.getNivel() != null ? tratamiento.getNivel().getIdNivel() : null)")
    @Mapping(target = "nombreNivel", expression = "java(tratamiento.getNivel() != null ? tratamiento.getNivel().getNombre() : null)")
    @Mapping(target = "codJuego",    expression = "java(tratamiento.getJuego() != null ? tratamiento.getJuego().getCodJuego() : null)")
    @Mapping(target = "nombreJuego", expression = "java(tratamiento.getJuego() != null ? tratamiento.getJuego().getNombre() : null)")
    TratamientoResponse toResponse(Tratamiento tratamiento);
}
