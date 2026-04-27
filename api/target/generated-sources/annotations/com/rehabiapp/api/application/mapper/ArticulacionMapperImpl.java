package com.rehabiapp.api.application.mapper;

import com.rehabiapp.api.application.dto.ArticulacionResponse;
import com.rehabiapp.api.domain.entity.Articulacion;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-24T02:16:13+0200",
    comments = "version: 1.6.3, compiler: javac, environment: Java 26 (Red Hat, Inc.)"
)
@Component
public class ArticulacionMapperImpl implements ArticulacionMapper {

    @Override
    public ArticulacionResponse toResponse(Articulacion articulacion) {
        if ( articulacion == null ) {
            return null;
        }

        Integer idArticulacion = null;
        String codigo = null;
        String nombre = null;

        idArticulacion = articulacion.getIdArticulacion();
        codigo = articulacion.getCodigo();
        nombre = articulacion.getNombre();

        ArticulacionResponse articulacionResponse = new ArticulacionResponse( idArticulacion, codigo, nombre );

        return articulacionResponse;
    }
}
