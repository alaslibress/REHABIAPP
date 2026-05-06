package com.rehabiapp.api.application.mapper;

import com.rehabiapp.api.application.dto.VideojuegoResponse;
import com.rehabiapp.api.domain.entity.Videojuego;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Mapper MapStruct para convertir Videojuego en VideojuegoResponse.
 *
 * <p>Aplana la relacion ManyToOne con Discapacidad en codDis y discapacidadNombre.</p>
 */
@Mapper(componentModel = "spring")
public interface VideojuegoMapper {

    @Mapping(source = "discapacidad.codDis", target = "codDis")
    @Mapping(source = "discapacidad.nombreDis", target = "discapacidadNombre")
    VideojuegoResponse toResponse(Videojuego entity);

    List<VideojuegoResponse> toResponseList(List<Videojuego> entities);
}
