package com.rehabiapp.api.application.mapper;

import com.rehabiapp.api.application.dto.JuegoResponse;
import com.rehabiapp.api.domain.entity.Juego;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper MapStruct para convertir la entidad Juego en JuegoResponse.
 *
 * <p>Navega la relacion articulacion para extraer id y nombre sin N+1
 * (la entidad se carga con LAZY pero MapStruct accede dentro del mismo contexto JPA).</p>
 */
@Mapper(componentModel = "spring")
public interface JuegoMapper {

    @Mapping(target = "idArticulacion",    expression = "java(juego.getArticulacion().getIdArticulacion())")
    @Mapping(target = "nombreArticulacion", expression = "java(juego.getArticulacion().getNombre())")
    JuegoResponse toResponse(Juego juego);
}
