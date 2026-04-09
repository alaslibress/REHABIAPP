package com.rehabiapp.api.application.mapper;

import com.rehabiapp.api.application.dto.DireccionDto;
import com.rehabiapp.api.application.dto.PacienteResponse;
import com.rehabiapp.api.domain.entity.Paciente;
import com.rehabiapp.api.domain.entity.Sanitario;
import java.time.LocalDate;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-09T15:29:50+0200",
    comments = "version: 1.6.3, compiler: javac, environment: Java 24.0.2 (Oracle Corporation)"
)
@Component
public class PacienteMapperImpl implements PacienteMapper {

    @Override
    public PacienteResponse toResponse(Paciente paciente) {
        if ( paciente == null ) {
            return null;
        }

        String dniSan = null;
        String dniPac = null;
        String nombrePac = null;
        String apellido1Pac = null;
        String apellido2Pac = null;
        Integer edadPac = null;
        String emailPac = null;
        String numSs = null;
        LocalDate fechaNacimiento = null;
        String alergias = null;
        String antecedentes = null;
        String medicacionActual = null;

        dniSan = pacienteSanitarioDniSan( paciente );
        dniPac = paciente.getDniPac();
        nombrePac = paciente.getNombrePac();
        apellido1Pac = paciente.getApellido1Pac();
        apellido2Pac = paciente.getApellido2Pac();
        edadPac = paciente.getEdadPac();
        emailPac = paciente.getEmailPac();
        numSs = paciente.getNumSs();
        fechaNacimiento = paciente.getFechaNacimiento();
        alergias = paciente.getAlergias();
        antecedentes = paciente.getAntecedentes();
        medicacionActual = paciente.getMedicacionActual();

        String sexo = paciente.getSexo() != null ? paciente.getSexo().name() : null;
        List<String> telefonos = paciente.getTelefonos().stream().map(t -> t.getTelefono()).collect(java.util.stream.Collectors.toList());
        Boolean activo = paciente.isActivo();
        Boolean protesis = paciente.isProtesis();
        Boolean consentimientoRgpd = paciente.isConsentimientoRgpd();
        DireccionDto direccion = paciente.getDireccion() == null ? null : new DireccionDto(paciente.getDireccion().getCalle(), paciente.getDireccion().getNumero(), paciente.getDireccion().getPiso(), paciente.getDireccion().getCodigoPostal().getCp(), paciente.getDireccion().getCodigoPostal().getLocalidad().getNombreLocalidad(), paciente.getDireccion().getCodigoPostal().getLocalidad().getProvincia());

        PacienteResponse pacienteResponse = new PacienteResponse( dniPac, dniSan, nombrePac, apellido1Pac, apellido2Pac, edadPac, emailPac, numSs, sexo, fechaNacimiento, protesis, activo, alergias, antecedentes, medicacionActual, consentimientoRgpd, telefonos, direccion );

        return pacienteResponse;
    }

    private String pacienteSanitarioDniSan(Paciente paciente) {
        Sanitario sanitario = paciente.getSanitario();
        if ( sanitario == null ) {
            return null;
        }
        return sanitario.getDniSan();
    }
}
