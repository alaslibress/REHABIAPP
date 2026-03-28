package com.rehabiapp.api.application.mapper;

import com.rehabiapp.api.application.dto.SanitarioResponse;
import com.rehabiapp.api.domain.entity.Sanitario;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-28T18:15:30+0100",
    comments = "version: 1.6.3, compiler: javac, environment: Java 24.0.2 (Oracle Corporation)"
)
@Component
public class SanitarioMapperImpl implements SanitarioMapper {

    @Override
    public SanitarioResponse toResponse(Sanitario sanitario) {
        if ( sanitario == null ) {
            return null;
        }

        String dniSan = null;
        String nombreSan = null;
        String apellido1San = null;
        String apellido2San = null;
        String emailSan = null;

        dniSan = sanitario.getDniSan();
        nombreSan = sanitario.getNombreSan();
        apellido1San = sanitario.getApellido1San();
        apellido2San = sanitario.getApellido2San();
        emailSan = sanitario.getEmailSan();

        List<String> telefonos = sanitario.getTelefonos().stream().map(t -> t.getTelefono()).collect(java.util.stream.Collectors.toList());
        String cargo = sanitario.getRol() != null ? sanitario.getRol().getCargo().name() : null;
        Integer numDePacientes = sanitario.getNumDePacientes();
        Boolean activo = sanitario.isActivo();

        SanitarioResponse sanitarioResponse = new SanitarioResponse( dniSan, nombreSan, apellido1San, apellido2San, emailSan, numDePacientes, activo, cargo, telefonos );

        return sanitarioResponse;
    }
}
