package com.rehabiapp.api.application.mapper;

import com.rehabiapp.api.application.dto.JuegoResponse;
import com.rehabiapp.api.domain.entity.Juego;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-24T02:16:13+0200",
    comments = "version: 1.6.3, compiler: javac, environment: Java 26 (Red Hat, Inc.)"
)
@Component
public class JuegoMapperImpl implements JuegoMapper {

    @Override
    public JuegoResponse toResponse(Juego juego) {
        if ( juego == null ) {
            return null;
        }

        String codJuego = null;
        String nombre = null;
        String descripcion = null;
        String urlJuego = null;
        boolean activo = false;

        codJuego = juego.getCodJuego();
        nombre = juego.getNombre();
        descripcion = juego.getDescripcion();
        urlJuego = juego.getUrlJuego();
        activo = juego.isActivo();

        Integer idArticulacion = juego.getArticulacion().getIdArticulacion();
        String nombreArticulacion = juego.getArticulacion().getNombre();

        JuegoResponse juegoResponse = new JuegoResponse( codJuego, nombre, descripcion, urlJuego, idArticulacion, nombreArticulacion, activo );

        return juegoResponse;
    }
}
