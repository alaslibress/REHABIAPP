package com.rehabiapp.api.application.mapper;

import com.rehabiapp.api.application.dto.CitaResponse;
import com.rehabiapp.api.domain.entity.Cita;
import com.rehabiapp.api.domain.entity.CitaId;
import java.time.LocalDate;
import java.time.LocalTime;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-06T02:48:33+0200",
    comments = "version: 1.6.3, compiler: javac, environment: Java 24.0.2 (Oracle Corporation)"
)
@Component
public class CitaMapperImpl implements CitaMapper {

    @Override
    public CitaResponse toResponse(Cita cita) {
        if ( cita == null ) {
            return null;
        }

        String dniPac = null;
        String dniSan = null;
        LocalDate fechaCita = null;
        LocalTime horaCita = null;

        dniPac = citaIdDniPac( cita );
        dniSan = citaIdDniSan( cita );
        fechaCita = citaIdFechaCita( cita );
        horaCita = citaIdHoraCita( cita );

        String nombrePaciente = nombrePaciente(cita);
        String nombreSanitario = nombreSanitario(cita);

        CitaResponse citaResponse = new CitaResponse( dniPac, dniSan, fechaCita, horaCita, nombrePaciente, nombreSanitario );

        return citaResponse;
    }

    private String citaIdDniPac(Cita cita) {
        CitaId id = cita.getId();
        if ( id == null ) {
            return null;
        }
        return id.getDniPac();
    }

    private String citaIdDniSan(Cita cita) {
        CitaId id = cita.getId();
        if ( id == null ) {
            return null;
        }
        return id.getDniSan();
    }

    private LocalDate citaIdFechaCita(Cita cita) {
        CitaId id = cita.getId();
        if ( id == null ) {
            return null;
        }
        return id.getFechaCita();
    }

    private LocalTime citaIdHoraCita(Cita cita) {
        CitaId id = cita.getId();
        if ( id == null ) {
            return null;
        }
        return id.getHoraCita();
    }
}
