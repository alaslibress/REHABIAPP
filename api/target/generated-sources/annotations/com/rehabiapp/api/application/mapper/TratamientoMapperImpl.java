package com.rehabiapp.api.application.mapper;

import com.rehabiapp.api.application.dto.TratamientoResponse;
import com.rehabiapp.api.domain.entity.Tratamiento;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-24T02:16:13+0200",
    comments = "version: 1.6.3, compiler: javac, environment: Java 26 (Red Hat, Inc.)"
)
@Component
public class TratamientoMapperImpl implements TratamientoMapper {

    @Override
    public TratamientoResponse toResponse(Tratamiento tratamiento) {
        if ( tratamiento == null ) {
            return null;
        }

        String codTrat = null;
        String nombreTrat = null;
        String definicionTrat = null;

        codTrat = tratamiento.getCodTrat();
        nombreTrat = tratamiento.getNombreTrat();
        definicionTrat = tratamiento.getDefinicionTrat();

        Integer idNivel = tratamiento.getNivel() != null ? tratamiento.getNivel().getIdNivel() : null;
        String nombreNivel = tratamiento.getNivel() != null ? tratamiento.getNivel().getNombre() : null;
        String codJuego = tratamiento.getJuego() != null ? tratamiento.getJuego().getCodJuego() : null;
        String nombreJuego = tratamiento.getJuego() != null ? tratamiento.getJuego().getNombre() : null;

        TratamientoResponse tratamientoResponse = new TratamientoResponse( codTrat, nombreTrat, definicionTrat, idNivel, nombreNivel, codJuego, nombreJuego );

        return tratamientoResponse;
    }
}
