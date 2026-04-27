package com.rehabiapp.api.application.mapper;

import com.rehabiapp.api.application.dto.DiscapacidadResponse;
import com.rehabiapp.api.domain.entity.Discapacidad;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-24T02:16:13+0200",
    comments = "version: 1.6.3, compiler: javac, environment: Java 26 (Red Hat, Inc.)"
)
@Component
public class DiscapacidadMapperImpl implements DiscapacidadMapper {

    @Override
    public DiscapacidadResponse toResponse(Discapacidad discapacidad) {
        if ( discapacidad == null ) {
            return null;
        }

        String codDis = null;
        String nombreDis = null;
        String descripcionDis = null;

        codDis = discapacidad.getCodDis();
        nombreDis = discapacidad.getNombreDis();
        descripcionDis = discapacidad.getDescripcionDis();

        Boolean necesitaProtesis = discapacidad.isNecesitaProtesis();
        Integer idArticulacion = discapacidad.getArticulacion() != null ? discapacidad.getArticulacion().getIdArticulacion() : null;
        String nombreArticulacion = discapacidad.getArticulacion() != null ? discapacidad.getArticulacion().getNombre() : null;

        DiscapacidadResponse discapacidadResponse = new DiscapacidadResponse( codDis, nombreDis, descripcionDis, necesitaProtesis, idArticulacion, nombreArticulacion );

        return discapacidadResponse;
    }
}
