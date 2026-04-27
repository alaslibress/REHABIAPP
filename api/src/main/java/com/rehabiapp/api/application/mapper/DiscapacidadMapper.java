package com.rehabiapp.api.application.mapper;

import com.rehabiapp.api.application.dto.DiscapacidadResponse;
import com.rehabiapp.api.domain.entity.Discapacidad;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper MapStruct para convertir la entidad Discapacidad en DiscapacidadResponse.
 *
 * <p>necesitaProtesis: boolean primitivo en la entidad, Boolean wrapper en el DTO.
 * idArticulacion y nombreArticulacion son nullable — discapacidades sin articulacion
 * asignada devuelven null (compatibilidad hacia atras).</p>
 */
@Mapper(componentModel = "spring")
public interface DiscapacidadMapper {

    @Mapping(target = "necesitaProtesis",    expression = "java(discapacidad.isNecesitaProtesis())")
    @Mapping(target = "idArticulacion",      expression = "java(discapacidad.getArticulacion() != null ? discapacidad.getArticulacion().getIdArticulacion() : null)")
    @Mapping(target = "nombreArticulacion",  expression = "java(discapacidad.getArticulacion() != null ? discapacidad.getArticulacion().getNombre() : null)")
    DiscapacidadResponse toResponse(Discapacidad discapacidad);
}
