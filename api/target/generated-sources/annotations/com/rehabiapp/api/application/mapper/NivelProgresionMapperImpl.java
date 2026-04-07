package com.rehabiapp.api.application.mapper;

import com.rehabiapp.api.application.dto.NivelProgresionResponse;
import com.rehabiapp.api.domain.entity.NivelProgresion;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-07T16:03:42+0200",
    comments = "version: 1.6.3, compiler: javac, environment: Java 24.0.2 (Oracle Corporation)"
)
@Component
public class NivelProgresionMapperImpl implements NivelProgresionMapper {

    @Override
    public NivelProgresionResponse toResponse(NivelProgresion nivelProgresion) {
        if ( nivelProgresion == null ) {
            return null;
        }

        Integer idNivel = null;
        String nombre = null;
        Integer orden = null;
        String descripcion = null;

        idNivel = nivelProgresion.getIdNivel();
        nombre = nivelProgresion.getNombre();
        orden = nivelProgresion.getOrden();
        descripcion = nivelProgresion.getDescripcion();

        NivelProgresionResponse nivelProgresionResponse = new NivelProgresionResponse( idNivel, nombre, orden, descripcion );

        return nivelProgresionResponse;
    }
}
