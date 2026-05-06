package com.rehabiapp.api.application.service;

import com.rehabiapp.api.application.dto.VideojuegoRequest;
import com.rehabiapp.api.application.dto.VideojuegoResponse;
import com.rehabiapp.api.application.mapper.VideojuegoMapper;
import com.rehabiapp.api.domain.entity.Discapacidad;
import com.rehabiapp.api.domain.entity.Tratamiento;
import com.rehabiapp.api.domain.entity.TratamientoVideojuego;
import com.rehabiapp.api.domain.entity.TratamientoVideojuegoId;
import com.rehabiapp.api.domain.entity.Videojuego;
import com.rehabiapp.api.domain.enums.AccionAuditoria;
import com.rehabiapp.api.domain.exception.RecursoNoEncontradoException;
import com.rehabiapp.api.domain.repository.DiscapacidadRepository;
import com.rehabiapp.api.domain.repository.TratamientoRepository;
import com.rehabiapp.api.domain.repository.TratamientoVideojuegoRepository;
import com.rehabiapp.api.domain.repository.VideojuegoRepository;
import com.rehabiapp.api.infrastructure.audit.AuditService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de aplicacion para los videojuegos terapeuticos.
 *
 * <p>Gestiona el catalogo de videojuegos y sus asociaciones N:M con tratamientos.
 * Los borrados son siempre logicos (activo=false) para preservar la trazabilidad
 * historica exigida por la Ley 41/2002.</p>
 */
@Service
@Transactional
public class VideojuegoService {

    private final VideojuegoRepository videojuegoRepository;
    private final TratamientoVideojuegoRepository tratamientoVideojuegoRepository;
    private final DiscapacidadRepository discapacidadRepository;
    private final TratamientoRepository tratamientoRepository;
    private final VideojuegoMapper videojuegoMapper;
    private final AuditService auditService;

    public VideojuegoService(
            VideojuegoRepository videojuegoRepository,
            TratamientoVideojuegoRepository tratamientoVideojuegoRepository,
            DiscapacidadRepository discapacidadRepository,
            TratamientoRepository tratamientoRepository,
            VideojuegoMapper videojuegoMapper,
            AuditService auditService
    ) {
        this.videojuegoRepository = videojuegoRepository;
        this.tratamientoVideojuegoRepository = tratamientoVideojuegoRepository;
        this.discapacidadRepository = discapacidadRepository;
        this.tratamientoRepository = tratamientoRepository;
        this.videojuegoMapper = videojuegoMapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<VideojuegoResponse> listarActivos(Pageable pageable) {
        return videojuegoRepository.findByActivoTrueOrderByNombreAsc(pageable)
                .map(videojuegoMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public VideojuegoResponse obtener(Long id) {
        Videojuego v = videojuegoRepository.findWithDiscapacidadByIdVideojuego(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Videojuego no encontrado: " + id));
        return videojuegoMapper.toResponse(v);
    }

    @Transactional(readOnly = true)
    public List<VideojuegoResponse> listarPorDiscapacidad(String codDis) {
        if (!discapacidadRepository.existsById(codDis)) {
            throw new RecursoNoEncontradoException("Discapacidad no encontrada: " + codDis);
        }
        return videojuegoRepository
                .findByDiscapacidadCodDisAndActivoTrueOrderByNombreAsc(codDis)
                .stream()
                .map(videojuegoMapper::toResponse)
                .toList();
    }

    public VideojuegoResponse crear(VideojuegoRequest req) {
        if (videojuegoRepository.existsByCodigo(req.codigo())) {
            throw new DataIntegrityViolationException(
                    "Ya existe un videojuego con codigo: " + req.codigo());
        }
        Discapacidad dis = discapacidadRepository.findById(req.codDis())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Discapacidad no encontrada: " + req.codDis()));

        Videojuego v = new Videojuego();
        v.setCodigo(req.codigo());
        v.setNombre(req.nombre());
        v.setDescripcion(req.descripcion());
        v.setDiscapacidad(dis);
        v.setParteCuerpo(req.parteCuerpo());
        v.setUrlUnity(req.urlUnity());
        v.setActivo(true);
        Videojuego guardado = videojuegoRepository.save(v);

        auditService.registrar(AccionAuditoria.CREATE, "Videojuego",
                String.valueOf(guardado.getIdVideojuego()),
                "Alta de videojuego: " + guardado.getCodigo());

        return videojuegoMapper.toResponse(guardado);
    }

    public VideojuegoResponse actualizar(Long id, VideojuegoRequest req) {
        Videojuego v = videojuegoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Videojuego no encontrado: " + id));

        if (videojuegoRepository.existsByCodigoAndIdVideojuegoNot(req.codigo(), id)) {
            throw new DataIntegrityViolationException(
                    "Ya existe otro videojuego con codigo: " + req.codigo());
        }
        Discapacidad dis = discapacidadRepository.findById(req.codDis())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Discapacidad no encontrada: " + req.codDis()));

        v.setCodigo(req.codigo());
        v.setNombre(req.nombre());
        v.setDescripcion(req.descripcion());
        v.setDiscapacidad(dis);
        v.setParteCuerpo(req.parteCuerpo());
        v.setUrlUnity(req.urlUnity());
        Videojuego guardado = videojuegoRepository.save(v);

        auditService.registrar(AccionAuditoria.UPDATE, "Videojuego",
                String.valueOf(id),
                "Actualizacion de videojuego: " + guardado.getCodigo());

        return videojuegoMapper.toResponse(guardado);
    }

    /** Soft delete: marca el videojuego como inactivo sin borrarlo de la base. */
    public void desactivar(Long id) {
        Videojuego v = videojuegoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Videojuego no encontrado: " + id));
        v.setActivo(false);
        videojuegoRepository.save(v);

        auditService.registrar(AccionAuditoria.SOFT_DELETE, "Videojuego",
                String.valueOf(id), "Baja logica de videojuego: " + v.getCodigo());
    }

    // ==================== ASOCIACION TRATAMIENTO-VIDEOJUEGO ====================

    @Transactional(readOnly = true)
    public List<VideojuegoResponse> listarVideojuegosDeTratamiento(String codTrat) {
        if (!tratamientoRepository.existsById(codTrat)) {
            throw new RecursoNoEncontradoException("Tratamiento no encontrado: " + codTrat);
        }
        return tratamientoVideojuegoRepository.findByIdCodTrat(codTrat).stream()
                .map(tv -> videojuegoMapper.toResponse(tv.getVideojuego()))
                .toList();
    }

    public void vincular(String codTrat, Long idVideojuego) {
        Tratamiento t = tratamientoRepository.findById(codTrat)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Tratamiento no encontrado: " + codTrat));
        Videojuego v = videojuegoRepository.findById(idVideojuego)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Videojuego no encontrado: " + idVideojuego));

        TratamientoVideojuegoId id = new TratamientoVideojuegoId(codTrat, idVideojuego);
        if (tratamientoVideojuegoRepository.existsById(id)) {
            throw new DataIntegrityViolationException(
                    "Tratamiento " + codTrat + " ya esta vinculado al videojuego " + idVideojuego);
        }
        TratamientoVideojuego vinculo = new TratamientoVideojuego();
        vinculo.setId(id);
        vinculo.setTratamiento(t);
        vinculo.setVideojuego(v);
        tratamientoVideojuegoRepository.save(vinculo);

        auditService.registrar(AccionAuditoria.CREATE, "TratamientoVideojuego",
                codTrat + ":" + idVideojuego,
                "Vinculo tratamiento-videojuego creado");
    }

    public void desvincular(String codTrat, Long idVideojuego) {
        TratamientoVideojuegoId id = new TratamientoVideojuegoId(codTrat, idVideojuego);
        if (!tratamientoVideojuegoRepository.existsById(id)) {
            throw new RecursoNoEncontradoException(
                    "No existe vinculo entre tratamiento " + codTrat
                            + " y videojuego " + idVideojuego);
        }
        tratamientoVideojuegoRepository.deleteById(id);

        auditService.registrar(AccionAuditoria.DELETE, "TratamientoVideojuego",
                codTrat + ":" + idVideojuego,
                "Vinculo tratamiento-videojuego eliminado");
    }
}
