package com.rehabiapp.api.application.service;

import com.rehabiapp.api.application.dto.DiscapacidadResponse;
import com.rehabiapp.api.application.dto.NivelProgresionResponse;
import com.rehabiapp.api.application.dto.TratamientoResponse;
import com.rehabiapp.api.application.mapper.DiscapacidadMapper;
import com.rehabiapp.api.application.mapper.NivelProgresionMapper;
import com.rehabiapp.api.application.mapper.TratamientoMapper;
import com.rehabiapp.api.domain.exception.RecursoNoEncontradoException;
import com.rehabiapp.api.domain.repository.DiscapacidadRepository;
import com.rehabiapp.api.domain.repository.NivelProgresionRepository;
import com.rehabiapp.api.domain.repository.TratamientoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de aplicación para los catálogos clínicos (solo lectura desde la API).
 *
 * <p>Los catálogos (discapacidades, tratamientos, niveles de progresión) son
 * de solo lectura desde la API REST. Las operaciones de escritura se realizan
 * actualmente desde el desktop ERP con acceso JDBC directo.</p>
 *
 * <p>Todos los métodos son readOnly=true por defecto para optimizar el rendimiento
 * de las consultas frecuentes a estos catálogos.</p>
 */
@Service
@Transactional(readOnly = true)
public class CatalogoService {

    private final DiscapacidadRepository discapacidadRepository;
    private final TratamientoRepository tratamientoRepository;
    private final NivelProgresionRepository nivelProgresionRepository;
    private final DiscapacidadMapper discapacidadMapper;
    private final TratamientoMapper tratamientoMapper;
    private final NivelProgresionMapper nivelProgresionMapper;

    public CatalogoService(
            DiscapacidadRepository discapacidadRepository,
            TratamientoRepository tratamientoRepository,
            NivelProgresionRepository nivelProgresionRepository,
            DiscapacidadMapper discapacidadMapper,
            TratamientoMapper tratamientoMapper,
            NivelProgresionMapper nivelProgresionMapper
    ) {
        this.discapacidadRepository = discapacidadRepository;
        this.tratamientoRepository = tratamientoRepository;
        this.nivelProgresionRepository = nivelProgresionRepository;
        this.discapacidadMapper = discapacidadMapper;
        this.tratamientoMapper = tratamientoMapper;
        this.nivelProgresionMapper = nivelProgresionMapper;
    }

    /**
     * Devuelve todas las discapacidades del catálogo.
     *
     * @return Lista de todas las discapacidades disponibles.
     */
    public List<DiscapacidadResponse> listarDiscapacidades() {
        return discapacidadRepository.findAll()
                .stream()
                .map(discapacidadMapper::toResponse)
                .toList();
    }

    /**
     * Devuelve una discapacidad por su código.
     *
     * @param cod Código de la discapacidad.
     * @return DTO de respuesta con los datos de la discapacidad.
     * @throws RecursoNoEncontradoException si no existe la discapacidad.
     */
    public DiscapacidadResponse obtenerDiscapacidad(String cod) {
        return discapacidadRepository.findById(cod)
                .map(discapacidadMapper::toResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException("Discapacidad no encontrada: " + cod));
    }

    /**
     * Devuelve todos los tratamientos del catálogo.
     *
     * @return Lista de todos los tratamientos disponibles.
     */
    public List<TratamientoResponse> listarTratamientos() {
        return tratamientoRepository.findAll()
                .stream()
                .map(tratamientoMapper::toResponse)
                .toList();
    }

    /**
     * Devuelve un tratamiento por su código.
     *
     * @param cod Código del tratamiento.
     * @return DTO de respuesta con los datos del tratamiento.
     * @throws RecursoNoEncontradoException si no existe el tratamiento.
     */
    public TratamientoResponse obtenerTratamiento(String cod) {
        return tratamientoRepository.findById(cod)
                .map(tratamientoMapper::toResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tratamiento no encontrado: " + cod));
    }

    /**
     * Devuelve todos los tratamientos aplicables a una discapacidad concreta.
     * Usado para filtrar la selección terapéutica según el perfil del paciente.
     *
     * @param codDis Código de la discapacidad.
     * @return Lista de tratamientos aplicables a esa discapacidad.
     */
    public List<TratamientoResponse> listarTratamientosPorDiscapacidad(String codDis) {
        return tratamientoRepository.findByCodDis(codDis)
                .stream()
                .map(tratamientoMapper::toResponse)
                .toList();
    }

    /**
     * Devuelve todos los niveles de progresión ordenados por su posición terapéutica.
     *
     * @return Lista de niveles de progresión ordenados de menor a mayor.
     */
    public List<NivelProgresionResponse> listarNiveles() {
        return nivelProgresionRepository.findAllByOrderByOrdenAsc()
                .stream()
                .map(nivelProgresionMapper::toResponse)
                .toList();
    }
}
