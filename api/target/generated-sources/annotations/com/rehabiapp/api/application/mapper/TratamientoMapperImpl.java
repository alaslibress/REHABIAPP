package com.rehabiapp.api.application.mapper;

import com.rehabiapp.api.application.dto.TratamientoResponse;
import com.rehabiapp.api.domain.entity.Tratamiento;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-13T12:05:14+0200",
    comments = "version: 1.6.3, compiler: javac, environment: Java 24.0.2 (Oracle Corporation)"
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

        TratamientoResponse tratamientoResponse = new TratamientoResponse( codTrat, nombreTrat, definicionTrat, idNivel, nombreNivel );

        return tratamientoResponse;
    }
}
