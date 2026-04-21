package com.rehabiapp.api.application.mapper;

import com.rehabiapp.api.application.dto.DireccionDto;
import com.rehabiapp.api.application.dto.PacienteResponse;
import com.rehabiapp.api.domain.entity.Paciente;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper MapStruct para convertir la entidad Paciente en PacienteResponse.
 *
 * <p>Los campos clínicos (alergias, antecedentes, medicacionActual) se mapean
 * tal como vienen de la entidad — ya descifrados por CampoClinicoConverter.</p>
 *
 * <p>El DNI del sanitario responsable se navega por la relación Paciente.sanitario.</p>
 */
@Mapper(componentModel = "spring")
public interface PacienteMapper {

    /**
     * Convierte la entidad Paciente en el DTO de respuesta.
     *
     * <p>dniSan: navega la relación paciente.sanitario.dniSan.</p>
     * <p>sexo: convierte el enum Sexo a String con su nombre.</p>
     * <p>telefonos: extrae los números desde los objetos TelefonoPaciente.</p>
     * <p>activo, protesis, consentimientoRgpd: boolean primitivo a Boolean wrapper.</p>
     */
    @Mapping(target = "dniSan", source = "sanitario.dniSan")
    @Mapping(
            target = "sexo",
            expression = "java(paciente.getSexo() != null ? paciente.getSexo().name() : null)"
    )
    @Mapping(
            target = "telefonos",
            expression = "java(paciente.getTelefonos().stream().map(t -> t.getTelefono()).collect(java.util.stream.Collectors.toList()))"
    )
    @Mapping(target = "activo", expression = "java(paciente.isActivo())")
    @Mapping(target = "protesis", expression = "java(paciente.isProtesis())")
    @Mapping(target = "consentimientoRgpd", expression = "java(paciente.isConsentimientoRgpd())")
    @Mapping(target = "direccion", expression =
            "java(paciente.getDireccion() == null ? null : new DireccionDto(" +
            "paciente.getDireccion().getCalle(), " +
            "paciente.getDireccion().getNumero(), " +
            "paciente.getDireccion().getPiso(), " +
            "paciente.getDireccion().getCodigoPostal().getCp(), " +
            "paciente.getDireccion().getCodigoPostal().getLocalidad().getNombreLocalidad(), " +
            "paciente.getDireccion().getCodigoPostal().getLocalidad().getProvincia()))")
    PacienteResponse toResponse(Paciente paciente);
}
