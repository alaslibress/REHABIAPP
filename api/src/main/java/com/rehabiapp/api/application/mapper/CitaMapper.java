package com.rehabiapp.api.application.mapper;

import com.rehabiapp.api.application.dto.CitaResponse;
import com.rehabiapp.api.domain.entity.Cita;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper MapStruct para convertir la entidad Cita en CitaResponse.
 *
 * <p>La entidad Cita tiene clave primaria compuesta (@EmbeddedId CitaId).
 * Los campos del response se navegan desde cita.id.* y desde las relaciones
 * ManyToOne con Paciente y Sanitario para obtener los nombres completos.</p>
 *
 * <p>NOTA: Los campos foto de Paciente son @Lob y no se cargan en este mapper.
 * Las relaciones paciente y sanitario se cargan con FetchType.LAZY;
 * deben estar dentro de una transacción activa para evitar LazyInitializationException.</p>
 */
@Mapper(componentModel = "spring")
public interface CitaMapper {

    /**
     * Convierte la entidad Cita en el DTO de respuesta.
     * Los nombres completos se construyen concatenando nombre + apellidos
     * con un método auxiliar implementado como default en este mapper.
     */
    @Mapping(target = "dniPac",          source = "id.dniPac")
    @Mapping(target = "dniSan",          source = "id.dniSan")
    @Mapping(target = "fechaCita",       source = "id.fechaCita")
    @Mapping(target = "horaCita",        source = "id.horaCita")
    @Mapping(target = "nombrePaciente",  expression = "java(nombrePaciente(cita))")
    @Mapping(target = "nombreSanitario", expression = "java(nombreSanitario(cita))")
    CitaResponse toResponse(Cita cita);

    /**
     * Construye el nombre completo del paciente (nombre + apellido1 + apellido2 opcional).
     */
    default String nombrePaciente(Cita cita) {
        String nombre = cita.getPaciente().getNombrePac()
                + " " + cita.getPaciente().getApellido1Pac();
        if (cita.getPaciente().getApellido2Pac() != null) {
            nombre += " " + cita.getPaciente().getApellido2Pac();
        }
        return nombre.trim();
    }

    /**
     * Construye el nombre completo del sanitario (nombre + apellido1 + apellido2 opcional).
     */
    default String nombreSanitario(Cita cita) {
        String nombre = cita.getSanitario().getNombreSan()
                + " " + cita.getSanitario().getApellido1San();
        if (cita.getSanitario().getApellido2San() != null) {
            nombre += " " + cita.getSanitario().getApellido2San();
        }
        return nombre.trim();
    }
}
