package com.rehabiapp.api.application.mapper;

import com.rehabiapp.api.application.dto.SanitarioResponse;
import com.rehabiapp.api.domain.entity.Sanitario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper MapStruct para convertir la entidad Sanitario en SanitarioResponse.
 *
 * <p>Las expresiones Java inline se usan para:
 * - Extraer la lista de números de teléfono desde los objetos TelefonoSanitario.
 * - Obtener el nombre del cargo (Rol) desde la relación SanitarioRol.</p>
 *
 * <p>componentModel = "spring" integra el mapper como bean Spring
 * para inyección por constructor en los servicios.</p>
 */
@Mapper(componentModel = "spring")
public interface SanitarioMapper {

    /**
     * Convierte la entidad Sanitario en el DTO de respuesta.
     *
     * <p>numDePacientes: la entidad almacena int primitivo, el DTO usa Integer wrapper.</p>
     * <p>activo: la entidad almacena boolean primitivo, el DTO usa Boolean wrapper.</p>
     */
    @Mapping(
            target = "telefonos",
            expression = "java(sanitario.getTelefonos().stream().map(t -> t.getTelefono()).collect(java.util.stream.Collectors.toList()))"
    )
    @Mapping(
            target = "cargo",
            expression = "java(sanitario.getRol() != null ? sanitario.getRol().getCargo().name() : null)"
    )
    @Mapping(target = "numDePacientes", expression = "java(sanitario.getNumDePacientes())")
    @Mapping(target = "activo", expression = "java(sanitario.isActivo())")
    SanitarioResponse toResponse(Sanitario sanitario);
}
