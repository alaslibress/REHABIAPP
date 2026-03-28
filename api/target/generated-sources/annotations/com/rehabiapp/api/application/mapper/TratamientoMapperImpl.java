package com.rehabiapp.api.application.mapper;

import com.rehabiapp.api.application.dto.TratamientoResponse;
import com.rehabiapp.api.domain.entity.Tratamiento;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-28T18:15:30+0100",
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

        TratamientoResponse tratamientoResponse = new TratamientoResponse( codTrat, nombreTrat, definicionTrat );

        return tratamientoResponse;
    }
}
