package com.rehabiapp.api.application.mapper;

import com.rehabiapp.api.application.dto.VideojuegoResponse;
import com.rehabiapp.api.domain.entity.Discapacidad;
import com.rehabiapp.api.domain.entity.Videojuego;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-15T00:48:14+0200",
    comments = "version: 1.6.3, compiler: javac, environment: Java 24.0.2 (Oracle Corporation)"
)
@Component
public class VideojuegoMapperImpl implements VideojuegoMapper {

    @Override
    public VideojuegoResponse toResponse(Videojuego entity) {
        if ( entity == null ) {
            return null;
        }

        String codDis = null;
        String discapacidadNombre = null;
        Long idVideojuego = null;
        String codigo = null;
        String nombre = null;
        String descripcion = null;
        String parteCuerpo = null;
        String urlUnity = null;
        boolean activo = false;

        codDis = entityDiscapacidadCodDis( entity );
        discapacidadNombre = entityDiscapacidadNombreDis( entity );
        idVideojuego = entity.getIdVideojuego();
        codigo = entity.getCodigo();
        nombre = entity.getNombre();
        descripcion = entity.getDescripcion();
        parteCuerpo = entity.getParteCuerpo();
        urlUnity = entity.getUrlUnity();
        activo = entity.isActivo();

        VideojuegoResponse videojuegoResponse = new VideojuegoResponse( idVideojuego, codigo, nombre, descripcion, codDis, discapacidadNombre, parteCuerpo, urlUnity, activo );

        return videojuegoResponse;
    }

    @Override
    public List<VideojuegoResponse> toResponseList(List<Videojuego> entities) {
        if ( entities == null ) {
            return null;
        }

        List<VideojuegoResponse> list = new ArrayList<VideojuegoResponse>( entities.size() );
        for ( Videojuego videojuego : entities ) {
            list.add( toResponse( videojuego ) );
        }

        return list;
    }

    private String entityDiscapacidadCodDis(Videojuego videojuego) {
        Discapacidad discapacidad = videojuego.getDiscapacidad();
        if ( discapacidad == null ) {
            return null;
        }
        return discapacidad.getCodDis();
    }

    private String entityDiscapacidadNombreDis(Videojuego videojuego) {
        Discapacidad discapacidad = videojuego.getDiscapacidad();
        if ( discapacidad == null ) {
            return null;
        }
        return discapacidad.getNombreDis();
    }
}
