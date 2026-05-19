package com.rehabiapp.api.application.mapper;

import com.rehabiapp.api.application.dto.PacienteTratamientoResponse;
import com.rehabiapp.api.domain.entity.PacienteTratamiento;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper MapStruct para convertir la entidad PacienteTratamiento en PacienteTratamientoResponse.
 *
 * <p>visible: boolean primitivo en la entidad, Boolean wrapper en el DTO.</p>
 *
 * <p>codDis e idNivel se ignoran aqui porque se resuelven via la query JPQL enriquecida
 * en PacienteTratamientoRepository.findEnriquecidoByDniPac(). El mapper solo se usa
 * para las operaciones de alta/baja de tratamientos donde no necesitamos esos campos.</p>
 */
@Mapper(componentModel = "spring")
public interface PacienteTratamientoMapper {

    /**
     * Convierte la entidad PacienteTratamiento en el DTO de respuesta.
     * Los campos codDis, idNivel y tienePdf se ignoran (no disponibles desde la entidad).
     */
    @Mapping(target = "dniPac", source = "id.dniPac")
    @Mapping(target = "codTrat", source = "id.codTrat")
    @Mapping(target = "nombreTrat", source = "tratamiento.nombreTrat")
    @Mapping(target = "visible", expression = "java(pt.isVisible())")
    @Mapping(target = "codDis", ignore = true)
    @Mapping(target = "idNivel", ignore = true)
    @Mapping(target = "tienePdf", ignore = true)
    PacienteTratamientoResponse toResponse(PacienteTratamiento pt);
}
