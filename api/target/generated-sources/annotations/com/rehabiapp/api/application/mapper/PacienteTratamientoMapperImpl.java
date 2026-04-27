package com.rehabiapp.api.application.mapper;

import com.rehabiapp.api.application.dto.PacienteTratamientoResponse;
import com.rehabiapp.api.domain.entity.PacienteTratamiento;
import com.rehabiapp.api.domain.entity.PacienteTratamientoId;
import com.rehabiapp.api.domain.entity.Tratamiento;
import java.time.LocalDateTime;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-24T02:11:22+0200",
    comments = "version: 1.6.3, compiler: javac, environment: Java 24.0.2 (Oracle Corporation)"
)
@Component
public class PacienteTratamientoMapperImpl implements PacienteTratamientoMapper {

    @Override
    public PacienteTratamientoResponse toResponse(PacienteTratamiento pt) {
        if ( pt == null ) {
            return null;
        }

        String dniPac = null;
        String codTrat = null;
        String nombreTrat = null;
        LocalDateTime fechaAsignacion = null;

        dniPac = ptIdDniPac( pt );
        codTrat = ptIdCodTrat( pt );
        nombreTrat = ptTratamientoNombreTrat( pt );
        fechaAsignacion = pt.getFechaAsignacion();

        Boolean visible = pt.isVisible();

        PacienteTratamientoResponse pacienteTratamientoResponse = new PacienteTratamientoResponse( dniPac, codTrat, nombreTrat, visible, fechaAsignacion );

        return pacienteTratamientoResponse;
    }

    private String ptIdDniPac(PacienteTratamiento pacienteTratamiento) {
        PacienteTratamientoId id = pacienteTratamiento.getId();
        if ( id == null ) {
            return null;
        }
        return id.getDniPac();
    }

    private String ptIdCodTrat(PacienteTratamiento pacienteTratamiento) {
        PacienteTratamientoId id = pacienteTratamiento.getId();
        if ( id == null ) {
            return null;
        }
        return id.getCodTrat();
    }

    private String ptTratamientoNombreTrat(PacienteTratamiento pacienteTratamiento) {
        Tratamiento tratamiento = pacienteTratamiento.getTratamiento();
        if ( tratamiento == null ) {
            return null;
        }
        return tratamiento.getNombreTrat();
    }
}
