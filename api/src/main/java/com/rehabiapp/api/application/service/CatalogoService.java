package com.rehabiapp.api.application.service;

import com.rehabiapp.api.application.dto.DiscapacidadRequest;
import com.rehabiapp.api.application.dto.DiscapacidadResponse;
import com.rehabiapp.api.application.dto.NivelProgresionResponse;
import com.rehabiapp.api.application.dto.TratamientoRequest;
import com.rehabiapp.api.application.dto.TratamientoResponse;
import com.rehabiapp.api.application.mapper.DiscapacidadMapper;
import com.rehabiapp.api.application.mapper.NivelProgresionMapper;
import com.rehabiapp.api.application.mapper.TratamientoMapper;
import com.rehabiapp.api.domain.entity.Discapacidad;
import com.rehabiapp.api.domain.entity.DiscapacidadTratamiento;
import com.rehabiapp.api.domain.entity.DiscapacidadTratamientoId;
import com.rehabiapp.api.domain.entity.NivelProgresion;
import com.rehabiapp.api.domain.entity.Tratamiento;
import com.rehabiapp.api.domain.exception.RecursoNoEncontradoException;
import com.rehabiapp.api.domain.repository.DiscapacidadRepository;
import com.rehabiapp.api.domain.repository.DiscapacidadTratamientoRepository;
import com.rehabiapp.api.domain.repository.NivelProgresionRepository;
import com.rehabiapp.api.domain.repository.PacienteDiscapacidadRepository;
import com.rehabiapp.api.domain.repository.PacienteTratamientoRepository;
import com.rehabiapp.api.domain.repository.TratamientoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de aplicacion para los catalogos clinicos.
 *
 * <p>Expone operaciones de lectura para todos los usuarios autenticados
 * y operaciones de escritura restringidas al rol SPECIALIST.</p>
 */
@Service
@Transactional
public class CatalogoService {

    private final DiscapacidadRepository discapacidadRepository;
    private final TratamientoRepository tratamientoRepository;
    private final NivelProgresionRepository nivelProgresionRepository;
    private final DiscapacidadMapper discapacidadMapper;
    private final TratamientoMapper tratamientoMapper;
    private final NivelProgresionMapper nivelProgresionMapper;
    private final DiscapacidadTratamientoRepository discapacidadTratamientoRepository;
    private final PacienteDiscapacidadRepository pacienteDiscapacidadRepository;
    private final PacienteTratamientoRepository pacienteTratamientoRepository;

    public CatalogoService(
            DiscapacidadRepository discapacidadRepository,
            TratamientoRepository tratamientoRepository,
            NivelProgresionRepository nivelProgresionRepository,
            DiscapacidadMapper discapacidadMapper,
            TratamientoMapper tratamientoMapper,
            NivelProgresionMapper nivelProgresionMapper,
            DiscapacidadTratamientoRepository discapacidadTratamientoRepository,
            PacienteDiscapacidadRepository pacienteDiscapacidadRepository,
            PacienteTratamientoRepository pacienteTratamientoRepository
    ) {
        this.discapacidadRepository = discapacidadRepository;
        this.tratamientoRepository = tratamientoRepository;
        this.nivelProgresionRepository = nivelProgresionRepository;
        this.discapacidadMapper = discapacidadMapper;
        this.tratamientoMapper = tratamientoMapper;
        this.nivelProgresionMapper = nivelProgresionMapper;
        this.discapacidadTratamientoRepository = discapacidadTratamientoRepository;
        this.pacienteDiscapacidadRepository = pacienteDiscapacidadRepository;
        this.pacienteTratamientoRepository = pacienteTratamientoRepository;
    }

    // ==================== LECTURA (readOnly=true) ====================

    /**
     * Devuelve todas las discapacidades del catalogo.
     */
    @Transactional(readOnly = true)
    public List<DiscapacidadResponse> listarDiscapacidades() {
        return discapacidadRepository.findAll()
                .stream()
                .map(discapacidadMapper::toResponse)
                .toList();
    }

    /**
     * Devuelve una discapacidad por su codigo.
     *
     * @throws RecursoNoEncontradoException si no existe.
     */
    @Transactional(readOnly = true)
    public DiscapacidadResponse obtenerDiscapacidad(String cod) {
        return discapacidadRepository.findById(cod)
                .map(discapacidadMapper::toResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException("Discapacidad no encontrada: " + cod));
    }

    /**
     * Devuelve todos los tratamientos del catalogo.
     */
    @Transactional(readOnly = true)
    public List<TratamientoResponse> listarTratamientos() {
        return tratamientoRepository.findAll()
                .stream()
                .map(tratamientoMapper::toResponse)
                .toList();
    }

    /**
     * Devuelve un tratamiento por su codigo.
     *
     * @throws RecursoNoEncontradoException si no existe.
     */
    @Transactional(readOnly = true)
    public TratamientoResponse obtenerTratamiento(String cod) {
        return tratamientoRepository.findById(cod)
                .map(tratamientoMapper::toResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tratamiento no encontrado: " + cod));
    }

    /**
     * Devuelve todos los tratamientos aplicables a una discapacidad concreta.
     */
    @Transactional(readOnly = true)
    public List<TratamientoResponse> listarTratamientosPorDiscapacidad(String codDis) {
        return tratamientoRepository.findByCodDis(codDis)
                .stream()
                .map(tratamientoMapper::toResponse)
                .toList();
    }

    /**
     * Devuelve todos los niveles de progresion ordenados.
     */
    @Transactional(readOnly = true)
    public List<NivelProgresionResponse> listarNiveles() {
        return nivelProgresionRepository.findAllByOrderByOrdenAsc()
                .stream()
                .map(nivelProgresionMapper::toResponse)
                .toList();
    }

    // ==================== DISCAPACIDADES CRUD ====================

    /**
     * Crea una nueva discapacidad en el catalogo.
     * Valida unicidad de codigo y nombre.
     */
    public DiscapacidadResponse crearDiscapacidad(DiscapacidadRequest request) {
        if (discapacidadRepository.existsById(request.codDis())) {
            throw new DataIntegrityViolationException(
                    "Ya existe una discapacidad con codigo: " + request.codDis());
        }
        if (discapacidadRepository.existsByNombreDis(request.nombreDis())) {
            throw new DataIntegrityViolationException(
                    "Ya existe una discapacidad con nombre: " + request.nombreDis());
        }

        Discapacidad entidad = new Discapacidad();
        entidad.setCodDis(request.codDis());
        entidad.setNombreDis(request.nombreDis());
        entidad.setDescripcionDis(request.descripcionDis());
        entidad.setNecesitaProtesis(Boolean.TRUE.equals(request.necesitaProtesis()));

        Discapacidad guardada = discapacidadRepository.save(entidad);
        return discapacidadMapper.toResponse(guardada);
    }

    /**
     * Actualiza los campos de una discapacidad existente.
     * El codigo (PK) no se puede cambiar.
     */
    public DiscapacidadResponse actualizarDiscapacidad(String cod, DiscapacidadRequest request) {
        Discapacidad entidad = discapacidadRepository.findById(cod)
                .orElseThrow(() -> new RecursoNoEncontradoException("Discapacidad no encontrada: " + cod));

        if (discapacidadRepository.existsByNombreDisAndCodDisNot(request.nombreDis(), cod)) {
            throw new DataIntegrityViolationException(
                    "Ya existe otra discapacidad con nombre: " + request.nombreDis());
        }

        entidad.setNombreDis(request.nombreDis());
        entidad.setDescripcionDis(request.descripcionDis());
        entidad.setNecesitaProtesis(Boolean.TRUE.equals(request.necesitaProtesis()));

        Discapacidad guardada = discapacidadRepository.save(entidad);
        return discapacidadMapper.toResponse(guardada);
    }

    /**
     * Elimina una discapacidad del catalogo.
     * Rechaza si algun paciente la tiene asignada.
     */
    public void eliminarDiscapacidad(String cod) {
        if (!discapacidadRepository.existsById(cod)) {
            throw new RecursoNoEncontradoException("Discapacidad no encontrada: " + cod);
        }
        if (pacienteDiscapacidadRepository.existsByIdCodDis(cod)) {
            throw new DataIntegrityViolationException(
                    "No se puede eliminar: hay pacientes con esta discapacidad asignada");
        }
        discapacidadTratamientoRepository.findByIdCodDis(cod)
                .forEach(discapacidadTratamientoRepository::delete);
        discapacidadRepository.deleteById(cod);
    }

    // ==================== TRATAMIENTOS CRUD ====================

    /**
     * Crea un nuevo tratamiento en el catalogo.
     */
    public TratamientoResponse crearTratamiento(TratamientoRequest request) {
        if (tratamientoRepository.existsById(request.codTrat())) {
            throw new DataIntegrityViolationException(
                    "Ya existe un tratamiento con codigo: " + request.codTrat());
        }
        if (tratamientoRepository.existsByNombreTrat(request.nombreTrat())) {
            throw new DataIntegrityViolationException(
                    "Ya existe un tratamiento con nombre: " + request.nombreTrat());
        }

        Tratamiento entidad = new Tratamiento();
        entidad.setCodTrat(request.codTrat());
        entidad.setNombreTrat(request.nombreTrat());
        entidad.setDefinicionTrat(request.definicionTrat());

        if (request.idNivel() != null) {
            NivelProgresion nivel = nivelProgresionRepository.findById(request.idNivel())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Nivel de progresion no encontrado: " + request.idNivel()));
            entidad.setNivel(nivel);
        }

        Tratamiento guardado = tratamientoRepository.save(entidad);
        return tratamientoMapper.toResponse(guardado);
    }

    /**
     * Actualiza los campos de un tratamiento existente.
     * El codigo (PK) no se puede cambiar.
     */
    public TratamientoResponse actualizarTratamiento(String cod, TratamientoRequest request) {
        Tratamiento entidad = tratamientoRepository.findById(cod)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tratamiento no encontrado: " + cod));

        if (tratamientoRepository.existsByNombreTratAndCodTratNot(request.nombreTrat(), cod)) {
            throw new DataIntegrityViolationException(
                    "Ya existe otro tratamiento con nombre: " + request.nombreTrat());
        }

        entidad.setNombreTrat(request.nombreTrat());
        entidad.setDefinicionTrat(request.definicionTrat());

        if (request.idNivel() != null) {
            NivelProgresion nivel = nivelProgresionRepository.findById(request.idNivel())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Nivel de progresion no encontrado: " + request.idNivel()));
            entidad.setNivel(nivel);
        } else {
            entidad.setNivel(null);
        }

        Tratamiento guardado = tratamientoRepository.save(entidad);
        return tratamientoMapper.toResponse(guardado);
    }

    /**
     * Elimina un tratamiento del catalogo.
     * Rechaza si algun paciente lo tiene asignado.
     */
    public void eliminarTratamiento(String cod) {
        if (!tratamientoRepository.existsById(cod)) {
            throw new RecursoNoEncontradoException("Tratamiento no encontrado: " + cod);
        }
        if (pacienteTratamientoRepository.existsByIdCodTrat(cod)) {
            throw new DataIntegrityViolationException(
                    "No se puede eliminar: hay pacientes con este tratamiento asignado");
        }
        discapacidadTratamientoRepository.findByIdCodTrat(cod)
                .forEach(discapacidadTratamientoRepository::delete);
        tratamientoRepository.deleteById(cod);
    }

    // ==================== VINCULOS TRATAMIENTO-DISCAPACIDAD ====================

    /**
     * Vincula un tratamiento a una discapacidad (tabla N:M).
     */
    public void vincularTratamientoDiscapacidad(String codTrat, String codDis) {
        if (!tratamientoRepository.existsById(codTrat)) {
            throw new RecursoNoEncontradoException("Tratamiento no encontrado: " + codTrat);
        }
        if (!discapacidadRepository.existsById(codDis)) {
            throw new RecursoNoEncontradoException("Discapacidad no encontrada: " + codDis);
        }

        // Orden del constructor: codDis primero, codTrat segundo
        DiscapacidadTratamientoId id = new DiscapacidadTratamientoId(codDis, codTrat);
        if (discapacidadTratamientoRepository.existsById(id)) {
            throw new DataIntegrityViolationException(
                    "El tratamiento ya esta vinculado a esta discapacidad");
        }

        DiscapacidadTratamiento vinculo = new DiscapacidadTratamiento();
        vinculo.setId(id);
        discapacidadTratamientoRepository.save(vinculo);
    }

    /**
     * Desvincula un tratamiento de una discapacidad.
     */
    public void desvincularTratamientoDiscapacidad(String codTrat, String codDis) {
        DiscapacidadTratamientoId id = new DiscapacidadTratamientoId(codDis, codTrat);
        if (!discapacidadTratamientoRepository.existsById(id)) {
            throw new RecursoNoEncontradoException(
                    "No existe vinculo entre tratamiento " + codTrat + " y discapacidad " + codDis);
        }
        discapacidadTratamientoRepository.deleteById(id);
    }

    /**
     * Lista las discapacidades vinculadas a un tratamiento.
     */
    @Transactional(readOnly = true)
    public List<DiscapacidadResponse> listarDiscapacidadesDeTratamiento(String codTrat) {
        if (!tratamientoRepository.existsById(codTrat)) {
            throw new RecursoNoEncontradoException("Tratamiento no encontrado: " + codTrat);
        }
        return discapacidadTratamientoRepository.findByIdCodTrat(codTrat).stream()
                .map(dt -> discapacidadMapper.toResponse(dt.getDiscapacidad()))
                .toList();
    }
}
