package com.rehabiapp.api.application.mapper;

import com.rehabiapp.api.application.dto.PacienteDiscapacidadResponse;
import com.rehabiapp.api.domain.entity.Discapacidad;
import com.rehabiapp.api.domain.entity.NivelProgresion;
import com.rehabiapp.api.domain.entity.PacienteDiscapacidad;
import com.rehabiapp.api.domain.entity.PacienteDiscapacidadId;
import java.time.LocalDateTime;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-28T18:15:30+0100",
    comments = "version: 1.6.3, compiler: javac, environment: Java 24.0.2 (Oracle Corporation)"
)
@Component
public class PacienteDiscapacidadMapperImpl implements PacienteDiscapacidadMapper {

    @Override
    public PacienteDiscapacidadResponse toResponse(PacienteDiscapacidad pd) {
        if ( pd == null ) {
            return null;
        }

        String dniPac = null;
        String codDis = null;
        String nombreDis = null;
        Integer idNivel = null;
        String nombreNivel = null;
        LocalDateTime fechaAsignacion = null;
        String notas = null;

        dniPac = pdIdDniPac( pd );
        codDis = pdIdCodDis( pd );
        nombreDis = pdDiscapacidadNombreDis( pd );
        idNivel = pdNivelProgresionIdNivel( pd );
        nombreNivel = pdNivelProgresionNombre( pd );
        fechaAsignacion = pd.getFechaAsignacion();
        notas = pd.getNotas();

        PacienteDiscapacidadResponse pacienteDiscapacidadResponse = new PacienteDiscapacidadResponse( dniPac, codDis, nombreDis, idNivel, nombreNivel, fechaAsignacion, notas );

        return pacienteDiscapacidadResponse;
    }

    private String pdIdDniPac(PacienteDiscapacidad pacienteDiscapacidad) {
        PacienteDiscapacidadId id = pacienteDiscapacidad.getId();
        if ( id == null ) {
            return null;
        }
        return id.getDniPac();
    }

    private String pdIdCodDis(PacienteDiscapacidad pacienteDiscapacidad) {
        PacienteDiscapacidadId id = pacienteDiscapacidad.getId();
        if ( id == null ) {
            return null;
        }
        return id.getCodDis();
    }

    private String pdDiscapacidadNombreDis(PacienteDiscapacidad pacienteDiscapacidad) {
        Discapacidad discapacidad = pacienteDiscapacidad.getDiscapacidad();
        if ( discapacidad == null ) {
            return null;
        }
        return discapacidad.getNombreDis();
    }

    private Integer pdNivelProgresionIdNivel(PacienteDiscapacidad pacienteDiscapacidad) {
        NivelProgresion nivelProgresion = pacienteDiscapacidad.getNivelProgresion();
        if ( nivelProgresion == null ) {
            return null;
        }
        return nivelProgresion.getIdNivel();
    }

    private String pdNivelProgresionNombre(PacienteDiscapacidad pacienteDiscapacidad) {
        NivelProgresion nivelProgresion = pacienteDiscapacidad.getNivelProgresion();
        if ( nivelProgresion == null ) {
            return null;
        }
        return nivelProgresion.getNombre();
    }
}
