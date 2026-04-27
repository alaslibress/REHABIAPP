package com.rehabiapp.api.application.service;

import com.rehabiapp.api.application.dto.ArticulacionResponse;
import com.rehabiapp.api.application.dto.DiscapacidadRequest;
import com.rehabiapp.api.application.dto.DiscapacidadResponse;
import com.rehabiapp.api.application.dto.JuegoAsociarRequest;
import com.rehabiapp.api.application.dto.JuegoRequest;
import com.rehabiapp.api.application.dto.JuegoResponse;
import com.rehabiapp.api.application.dto.NivelProgresionResponse;
import com.rehabiapp.api.application.dto.TratamientoRequest;
import com.rehabiapp.api.application.dto.TratamientoResponse;
import com.rehabiapp.api.application.mapper.ArticulacionMapper;
import com.rehabiapp.api.application.mapper.DiscapacidadMapper;
import com.rehabiapp.api.application.mapper.JuegoMapper;
import com.rehabiapp.api.application.mapper.NivelProgresionMapper;
import com.rehabiapp.api.application.mapper.TratamientoMapper;
import com.rehabiapp.api.domain.entity.Articulacion;
import com.rehabiapp.api.domain.entity.Discapacidad;
import com.rehabiapp.api.domain.entity.DiscapacidadTratamiento;
import com.rehabiapp.api.domain.entity.DiscapacidadTratamientoId;
import com.rehabiapp.api.domain.entity.Juego;
import com.rehabiapp.api.domain.entity.NivelProgresion;
import com.rehabiapp.api.domain.entity.Tratamiento;
import com.rehabiapp.api.domain.exception.RecursoNoEncontradoException;
import com.rehabiapp.api.domain.repository.ArticulacionRepository;
import com.rehabiapp.api.domain.repository.DiscapacidadRepository;
import com.rehabiapp.api.domain.repository.DiscapacidadTratamientoRepository;
import com.rehabiapp.api.domain.repository.JuegoRepository;
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
    private final ArticulacionRepository articulacionRepository;
    private final JuegoRepository juegoRepository;
    private final DiscapacidadMapper discapacidadMapper;
    private final TratamientoMapper tratamientoMapper;
    private final NivelProgresionMapper nivelProgresionMapper;
    private final ArticulacionMapper articulacionMapper;
    private final JuegoMapper juegoMapper;
    private final DiscapacidadTratamientoRepository discapacidadTratamientoRepository;
    private final PacienteDiscapacidadRepository pacienteDiscapacidadRepository;
    private final PacienteTratamientoRepository pacienteTratamientoRepository;

    public CatalogoService(
            DiscapacidadRepository discapacidadRepository,
            TratamientoRepository tratamientoRepository,
            NivelProgresionRepository nivelProgresionRepository,
            ArticulacionRepository articulacionRepository,
            JuegoRepository juegoRepository,
            DiscapacidadMapper discapacidadMapper,
            TratamientoMapper tratamientoMapper,
            NivelProgresionMapper nivelProgresionMapper,
            ArticulacionMapper articulacionMapper,
            JuegoMapper juegoMapper,
            DiscapacidadTratamientoRepository discapacidadTratamientoRepository,
            PacienteDiscapacidadRepository pacienteDiscapacidadRepository,
            PacienteTratamientoRepository pacienteTratamientoRepository
    ) {
        this.discapacidadRepository = discapacidadRepository;
        this.tratamientoRepository = tratamientoRepository;
        this.nivelProgresionRepository = nivelProgresionRepository;
        this.articulacionRepository = articulacionRepository;
        this.juegoRepository = juegoRepository;
        this.discapacidadMapper = discapacidadMapper;
        this.tratamientoMapper = tratamientoMapper;
        this.nivelProgresionMapper = nivelProgresionMapper;
        this.articulacionMapper = articulacionMapper;
        this.juegoMapper = juegoMapper;
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

    // ==================== ARTICULACIONES ====================

    /**
     * Lista todas las articulaciones del catalogo (seed estatico).
     */
    @Transactional(readOnly = true)
    public List<ArticulacionResponse> listarArticulaciones() {
        return articulacionRepository.findAll()
                .stream()
                .map(articulacionMapper::toResponse)
                .toList();
    }

    // ==================== JUEGOS ====================

    /**
     * Lista todos los juegos activos del catalogo.
     * Si se proporciona idArticulacion, filtra por esa articulacion.
     *
     * @param idArticulacion filtro opcional; null devuelve todos los activos.
     */
    @Transactional(readOnly = true)
    public List<JuegoResponse> listarJuegos(Integer idArticulacion) {
        List<Juego> juegos = (idArticulacion != null)
                ? juegoRepository.findByArticulacionIdArticulacionAndActivoTrue(idArticulacion)
                : juegoRepository.findByActivoTrue();
        return juegos.stream().map(juegoMapper::toResponse).toList();
    }

    /**
     * Devuelve un juego por su codigo.
     *
     * @throws RecursoNoEncontradoException si no existe.
     */
    @Transactional(readOnly = true)
    public JuegoResponse obtenerJuego(String codJuego) {
        return juegoRepository.findById(codJuego)
                .map(juegoMapper::toResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException("Juego no encontrado: " + codJuego));
    }

    /**
     * Crea un nuevo juego en el catalogo.
     * Solo SPECIALIST (validado en el controlador).
     */
    public JuegoResponse crearJuego(JuegoRequest request) {
        if (juegoRepository.existsById(request.codJuego())) {
            throw new DataIntegrityViolationException(
                    "Ya existe un juego con codigo: " + request.codJuego());
        }
        if (juegoRepository.existsByNombre(request.nombre())) {
            throw new DataIntegrityViolationException(
                    "Ya existe un juego con nombre: " + request.nombre());
        }

        Articulacion articulacion = articulacionRepository.findById(request.idArticulacion())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Articulacion no encontrada: " + request.idArticulacion()));

        Juego entidad = new Juego();
        entidad.setCodJuego(request.codJuego());
        entidad.setNombre(request.nombre());
        entidad.setDescripcion(request.descripcion());
        entidad.setUrlJuego(request.urlJuego());
        entidad.setArticulacion(articulacion);
        entidad.setActivo(Boolean.TRUE.equals(request.activo()) || request.activo() == null);

        return juegoMapper.toResponse(juegoRepository.save(entidad));
    }

    /**
     * Actualiza los campos de un juego existente.
     */
    public JuegoResponse actualizarJuego(String codJuego, JuegoRequest request) {
        Juego entidad = juegoRepository.findById(codJuego)
                .orElseThrow(() -> new RecursoNoEncontradoException("Juego no encontrado: " + codJuego));

        if (juegoRepository.existsByNombreAndCodJuegoNot(request.nombre(), codJuego)) {
            throw new DataIntegrityViolationException(
                    "Ya existe otro juego con nombre: " + request.nombre());
        }

        Articulacion articulacion = articulacionRepository.findById(request.idArticulacion())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Articulacion no encontrada: " + request.idArticulacion()));

        entidad.setNombre(request.nombre());
        entidad.setDescripcion(request.descripcion());
        entidad.setUrlJuego(request.urlJuego());
        entidad.setArticulacion(articulacion);
        if (request.activo() != null) {
            entidad.setActivo(request.activo());
        }

        return juegoMapper.toResponse(juegoRepository.save(entidad));
    }

    /**
     * Elimina un juego del catalogo.
     * Rechaza con 409 si algun tratamiento lo tiene asociado.
     */
    public void eliminarJuego(String codJuego) {
        if (!juegoRepository.existsById(codJuego)) {
            throw new RecursoNoEncontradoException("Juego no encontrado: " + codJuego);
        }
        if (tratamientoRepository.existsByJuegoCodJuego(codJuego)) {
            throw new DataIntegrityViolationException(
                    "No se puede eliminar: hay tratamientos vinculados a este juego");
        }
        juegoRepository.deleteById(codJuego);
    }

    // ==================== ASOCIACION TRATAMIENTO-JUEGO ====================

    /**
     * Asocia o desasocia un juego a un tratamiento.
     *
     * <p>Si codJuego es null se desasocia el juego actual (el tratamiento queda sin juego).
     * Si codJuego no es null, valida que la articulacion del juego coincida con al menos
     * una de las articulaciones de las discapacidades vinculadas al tratamiento.
     * Si ninguna discapacidad vinculada tiene articulacion definida, el juego se acepta
     * sin restriccion de articulacion (fallback permisivo).</p>
     *
     * @throws RecursoNoEncontradoException  si el tratamiento o el juego no existen.
     * @throws IllegalArgumentException      si la articulacion del juego no coincide.
     */
    public TratamientoResponse asociarJuegoATratamiento(String codTrat, JuegoAsociarRequest request) {
        Tratamiento tratamiento = tratamientoRepository.findById(codTrat)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tratamiento no encontrado: " + codTrat));

        if (request.codJuego() == null) {
            // Desasociacion explicita
            tratamiento.setJuego(null);
        } else {
            Juego juego = juegoRepository.findById(request.codJuego())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Juego no encontrado: " + request.codJuego()));

            // Validar coherencia de articulacion con las discapacidades del tratamiento
            List<DiscapacidadTratamiento> vinculos =
                    discapacidadTratamientoRepository.findByIdCodTrat(codTrat);

            boolean hayArticulacionDefinida = vinculos.stream()
                    .anyMatch(v -> v.getDiscapacidad().getArticulacion() != null);

            if (hayArticulacionDefinida) {
                Integer idArtJuego = juego.getArticulacion().getIdArticulacion();
                boolean coincide = vinculos.stream()
                        .filter(v -> v.getDiscapacidad().getArticulacion() != null)
                        .anyMatch(v -> v.getDiscapacidad().getArticulacion()
                                .getIdArticulacion().equals(idArtJuego));
                if (!coincide) {
                    throw new IllegalArgumentException(
                            "El juego seleccionado no corresponde a ninguna articulacion "
                            + "de las discapacidades vinculadas al tratamiento.");
                }
            }

            tratamiento.setJuego(juego);
        }

        return tratamientoMapper.toResponse(tratamientoRepository.save(tratamiento));
    }
}
