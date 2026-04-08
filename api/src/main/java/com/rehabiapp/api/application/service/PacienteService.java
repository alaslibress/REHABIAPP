package com.rehabiapp.api.application.service;

import com.rehabiapp.api.application.dto.DireccionDto;
import com.rehabiapp.api.application.dto.PageResponse;
import com.rehabiapp.api.application.dto.PacienteRequest;
import com.rehabiapp.api.application.dto.PacienteResponse;
import com.rehabiapp.api.application.mapper.PacienteMapper;
import com.rehabiapp.api.domain.entity.CodigoPostal;
import com.rehabiapp.api.domain.entity.Direccion;
import com.rehabiapp.api.domain.entity.Localidad;
import com.rehabiapp.api.domain.entity.Paciente;
import com.rehabiapp.api.domain.entity.Sanitario;
import com.rehabiapp.api.domain.entity.TelefonoPaciente;
import com.rehabiapp.api.domain.enums.AccionAuditoria;
import com.rehabiapp.api.domain.enums.Sexo;
import com.rehabiapp.api.domain.exception.RecursoNoEncontradoException;
import com.rehabiapp.api.domain.repository.CodigoPostalRepository;
import com.rehabiapp.api.domain.repository.DireccionRepository;
import com.rehabiapp.api.domain.repository.LocalidadRepository;
import com.rehabiapp.api.domain.repository.PacienteRepository;
import com.rehabiapp.api.domain.repository.SanitarioRepository;
import com.rehabiapp.api.infrastructure.audit.AuditService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Servicio de aplicación para la gestión de pacientes.
 *
 * <p>CRITICO — Seguridad y privacidad:</p>
 * <ul>
 *   <li>Los campos clínicos (alergias, antecedentes, medicacionActual) se cifran
 *       automáticamente con AES-256-GCM vía CampoClinicoConverter al persistir (RGPD Art. 9).
 *       El servicio trabaja con texto plano; el cifrado es transparente en la entidad.</li>
 *   <li>Los pacientes NUNCA se eliminan físicamente. Soft delete obligatorio.
 *       Retención mínima 5 años tras la baja (Ley 41/2002).</li>
 *   <li>Toda operación, incluyendo lecturas, se registra en audit_log.</li>
 * </ul>
 */
@Service
@Transactional
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final SanitarioRepository sanitarioRepository;
    private final PacienteMapper pacienteMapper;
    private final AuditService auditService;
    private final DireccionRepository direccionRepository;
    private final CodigoPostalRepository codigoPostalRepository;
    private final LocalidadRepository localidadRepository;

    public PacienteService(
            PacienteRepository pacienteRepository,
            SanitarioRepository sanitarioRepository,
            PacienteMapper pacienteMapper,
            AuditService auditService,
            DireccionRepository direccionRepository,
            CodigoPostalRepository codigoPostalRepository,
            LocalidadRepository localidadRepository
    ) {
        this.pacienteRepository = pacienteRepository;
        this.sanitarioRepository = sanitarioRepository;
        this.pacienteMapper = pacienteMapper;
        this.auditService = auditService;
        this.direccionRepository = direccionRepository;
        this.codigoPostalRepository = codigoPostalRepository;
        this.localidadRepository = localidadRepository;
    }

    /**
     * Devuelve todos los pacientes activos paginados.
     * Solo accesible para rol SPECIALIST (restricción RBAC en el controlador).
     *
     * @param pageable Configuración de paginación.
     * @return Página de pacientes activos como DTOs de respuesta.
     */
    @Transactional(readOnly = true)
    public PageResponse<PacienteResponse> listar(Pageable pageable) {
        auditService.registrar(AccionAuditoria.READ, "paciente", "todos", "Listado de pacientes");
        return PageResponse.de(
                pacienteRepository.findAllByActivoTrue(pageable).map(pacienteMapper::toResponse)
        );
    }

    /**
     * Devuelve todos los pacientes asignados a un sanitario concreto (activos e inactivos).
     *
     * @param dniSan   DNI del sanitario responsable.
     * @param pageable Configuración de paginación.
     * @return Página de pacientes del sanitario.
     */
    @Transactional(readOnly = true)
    public PageResponse<PacienteResponse> listarPorSanitario(String dniSan, Pageable pageable) {
        auditService.registrar(AccionAuditoria.READ, "paciente", dniSan, "Listado de pacientes por sanitario");
        return PageResponse.de(
                pacienteRepository.findAllBySanitarioDniSan(dniSan, pageable).map(pacienteMapper::toResponse)
        );
    }

    /**
     * Devuelve un paciente activo por su DNI.
     *
     * <p>Los campos clínicos se devuelven descifrados por CampoClinicoConverter.
     * El acceso queda registrado en audit_log (Ley 41/2002).</p>
     *
     * @param dni DNI del paciente.
     * @return DTO de respuesta con los datos del paciente.
     * @throws RecursoNoEncontradoException si no existe o está inactivo.
     */
    @Transactional(readOnly = true)
    public PacienteResponse obtenerPorDni(String dni) {
        Paciente paciente = pacienteRepository.findByDniPacAndActivoTrue(dni)
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente no encontrado: " + dni));
        auditService.registrar(AccionAuditoria.READ, "paciente", dni, "Consulta historial paciente");
        return pacienteMapper.toResponse(paciente);
    }

    /**
     * Crea un nuevo paciente en el sistema.
     *
     * <p>El sanitario responsable se obtiene por DNI y se asigna por referencia.
     * Los campos clínicos se cifran automáticamente al persistir la entidad.</p>
     *
     * @param request DTO con los datos del nuevo paciente.
     * @return DTO de respuesta con el paciente creado.
     * @throws RecursoNoEncontradoException si el sanitario responsable no existe.
     */
    public PacienteResponse crear(PacienteRequest request) {
        // Obtener sanitario responsable
        Sanitario sanitario = sanitarioRepository.findByDniSanAndActivoTrue(request.dniSan())
                .orElseThrow(() -> new RecursoNoEncontradoException("Sanitario no encontrado: " + request.dniSan()));

        Paciente paciente = new Paciente();
        paciente.setDniPac(request.dniPac());
        paciente.setSanitario(sanitario);
        paciente.setNombrePac(request.nombrePac());
        paciente.setApellido1Pac(request.apellido1Pac());
        paciente.setApellido2Pac(request.apellido2Pac());
        paciente.setEdadPac(request.edadPac());
        paciente.setEmailPac(request.emailPac());
        paciente.setNumSs(request.numSs());

        // Convertir string de sexo a enum si se proporciona
        if (request.sexo() != null) {
            paciente.setSexo(Sexo.valueOf(request.sexo()));
        }

        paciente.setFechaNacimiento(request.fechaNacimiento());
        paciente.setProtesis(request.protesis() != null && request.protesis());

        // Campos clínicos — se cifran automáticamente por CampoClinicoConverter al persistir
        paciente.setAlergias(request.alergias());
        paciente.setAntecedentes(request.antecedentes());
        paciente.setMedicacionActual(request.medicacionActual());

        // Registrar consentimiento RGPD si se proporciona
        if (Boolean.TRUE.equals(request.consentimientoRgpd())) {
            paciente.setConsentimientoRgpd(true);
            paciente.setFechaConsentimiento(LocalDateTime.now());
        }

        paciente.setActivo(true);
        paciente.setDireccion(resolverDireccion(request.direccion()));

        // Añadir teléfonos en cascada
        if (request.telefonos() != null) {
            request.telefonos().forEach(tel -> {
                TelefonoPaciente t = new TelefonoPaciente();
                t.setPaciente(paciente);
                t.setTelefono(tel);
                paciente.getTelefonos().add(t);
            });
        }

        Paciente guardado = pacienteRepository.save(paciente);
        auditService.registrar(AccionAuditoria.CREATE, "paciente", request.dniPac(), "Paciente creado");
        return pacienteMapper.toResponse(guardado);
    }

    /**
     * Actualiza los datos de un paciente existente.
     *
     * <p>Los campos clínicos se actualizan con nuevo cifrado AES-256-GCM
     * (IV aleatorio por operación de escritura, RGPD Art. 9).</p>
     *
     * @param dni     DNI del paciente a actualizar.
     * @param request DTO con los nuevos datos del paciente.
     * @return DTO de respuesta con el paciente actualizado.
     * @throws RecursoNoEncontradoException si el paciente no existe o está inactivo.
     */
    public PacienteResponse actualizar(String dni, PacienteRequest request) {
        Paciente paciente = pacienteRepository.findByDniPacAndActivoTrue(dni)
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente no encontrado: " + dni));

        paciente.setNombrePac(request.nombrePac());
        paciente.setApellido1Pac(request.apellido1Pac());
        paciente.setApellido2Pac(request.apellido2Pac());
        paciente.setEdadPac(request.edadPac());
        paciente.setEmailPac(request.emailPac());
        paciente.setNumSs(request.numSs());

        if (request.sexo() != null) {
            paciente.setSexo(Sexo.valueOf(request.sexo()));
        }

        paciente.setFechaNacimiento(request.fechaNacimiento());

        if (request.protesis() != null) {
            paciente.setProtesis(request.protesis());
        }

        // Actualizar campos clínicos — nuevo cifrado con IV aleatorio por escritura
        paciente.setAlergias(request.alergias());
        paciente.setAntecedentes(request.antecedentes());
        paciente.setMedicacionActual(request.medicacionActual());

        if (Boolean.TRUE.equals(request.consentimientoRgpd()) && !paciente.isConsentimientoRgpd()) {
            paciente.setConsentimientoRgpd(true);
            paciente.setFechaConsentimiento(LocalDateTime.now());
        }

        if (request.direccion() != null) {
            paciente.setDireccion(resolverDireccion(request.direccion()));
        }

        auditService.registrar(AccionAuditoria.UPDATE, "paciente", dni, "Paciente actualizado");
        return pacienteMapper.toResponse(pacienteRepository.save(paciente));
    }

    /**
     * Crea un paciente con direccion, telefonos y opcionalmente foto, todo en una unica transaccion.
     * Si la persistencia de cualquier parte falla, se hace rollback completo del agregado.
     *
     * @param request   DTO con los datos del paciente.
     * @param fotoBytes Bytes de la foto, o null si no hay foto.
     * @return DTO de respuesta con el paciente creado.
     */
    public PacienteResponse crearConFoto(PacienteRequest request, byte[] fotoBytes) {
        Sanitario sanitario = sanitarioRepository.findByDniSanAndActivoTrue(request.dniSan())
                .orElseThrow(() -> new RecursoNoEncontradoException("Sanitario no encontrado: " + request.dniSan()));

        Paciente paciente = new Paciente();
        paciente.setDniPac(request.dniPac());
        paciente.setSanitario(sanitario);
        paciente.setNombrePac(request.nombrePac());
        paciente.setApellido1Pac(request.apellido1Pac());
        paciente.setApellido2Pac(request.apellido2Pac());
        paciente.setEdadPac(request.edadPac());
        paciente.setEmailPac(request.emailPac());
        paciente.setNumSs(request.numSs());

        if (request.sexo() != null) {
            paciente.setSexo(Sexo.valueOf(request.sexo()));
        }

        paciente.setFechaNacimiento(request.fechaNacimiento());
        paciente.setProtesis(request.protesis() != null && request.protesis());
        paciente.setAlergias(request.alergias());
        paciente.setAntecedentes(request.antecedentes());
        paciente.setMedicacionActual(request.medicacionActual());

        if (Boolean.TRUE.equals(request.consentimientoRgpd())) {
            paciente.setConsentimientoRgpd(true);
            paciente.setFechaConsentimiento(LocalDateTime.now());
        }

        paciente.setActivo(true);
        paciente.setDireccion(resolverDireccion(request.direccion()));

        if (request.telefonos() != null) {
            request.telefonos().forEach(tel -> {
                TelefonoPaciente t = new TelefonoPaciente();
                t.setPaciente(paciente);
                t.setTelefono(tel);
                paciente.getTelefonos().add(t);
            });
        }

        // Persistir foto en la misma entidad antes del save -> mismo flush, misma transaccion
        if (fotoBytes != null && fotoBytes.length > 0) {
            paciente.setFoto(fotoBytes);
        }

        Paciente guardado = pacienteRepository.save(paciente);
        auditService.registrar(AccionAuditoria.CREATE, "paciente", request.dniPac(),
                "Paciente creado" + (fotoBytes != null ? " (con foto)" : ""));
        return pacienteMapper.toResponse(guardado);
    }

    /**
     * Actualiza un paciente y opcionalmente su foto en una unica transaccion.
     * Si fotoBytes es null, la foto existente se mantiene intacta.
     *
     * @param dni       DNI del paciente a actualizar.
     * @param request   DTO con los nuevos datos del paciente.
     * @param fotoBytes Bytes de la nueva foto, o null para mantener la actual.
     * @return DTO de respuesta con el paciente actualizado.
     */
    public PacienteResponse actualizarConFoto(String dni, PacienteRequest request, byte[] fotoBytes) {
        Paciente paciente = pacienteRepository.findByDniPacAndActivoTrue(dni)
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente no encontrado: " + dni));

        paciente.setNombrePac(request.nombrePac());
        paciente.setApellido1Pac(request.apellido1Pac());
        paciente.setApellido2Pac(request.apellido2Pac());
        paciente.setEdadPac(request.edadPac());
        paciente.setEmailPac(request.emailPac());
        paciente.setNumSs(request.numSs());

        if (request.sexo() != null) {
            paciente.setSexo(Sexo.valueOf(request.sexo()));
        }

        paciente.setFechaNacimiento(request.fechaNacimiento());

        if (request.protesis() != null) {
            paciente.setProtesis(request.protesis());
        }

        paciente.setAlergias(request.alergias());
        paciente.setAntecedentes(request.antecedentes());
        paciente.setMedicacionActual(request.medicacionActual());

        if (Boolean.TRUE.equals(request.consentimientoRgpd()) && !paciente.isConsentimientoRgpd()) {
            paciente.setConsentimientoRgpd(true);
            paciente.setFechaConsentimiento(LocalDateTime.now());
        }

        if (request.direccion() != null) {
            paciente.setDireccion(resolverDireccion(request.direccion()));
        }

        if (fotoBytes != null && fotoBytes.length > 0) {
            paciente.setFoto(fotoBytes);
        }

        auditService.registrar(AccionAuditoria.UPDATE, "paciente", dni,
                "Paciente actualizado" + (fotoBytes != null ? " (con foto)" : ""));
        return pacienteMapper.toResponse(pacienteRepository.save(paciente));
    }

    /**
     * Find-or-create idempotente de Direccion + CodigoPostal + Localidad.
     * Ejecuta dentro de la misma @Transactional del metodo llamante.
     *
     * @param dto Datos de direccion del request, o null.
     * @return Entidad Direccion gestionada por JPA, o null si dto es null.
     */
    private Direccion resolverDireccion(DireccionDto dto) {
        if (dto == null) return null;

        // 1. Find-or-create Localidad por nombre
        Localidad localidad = localidadRepository.findByNombreLocalidad(dto.nombreLocalidad())
                .orElseGet(() -> {
                    Localidad nueva = new Localidad();
                    nueva.setNombreLocalidad(dto.nombreLocalidad());
                    nueva.setProvincia(dto.provincia());
                    return localidadRepository.save(nueva);
                });

        // 2. Find-or-create CodigoPostal por cp
        CodigoPostal cp = codigoPostalRepository.findById(dto.cp())
                .orElseGet(() -> {
                    CodigoPostal nuevo = new CodigoPostal();
                    nuevo.setCp(dto.cp());
                    nuevo.setLocalidad(localidad);
                    return codigoPostalRepository.save(nuevo);
                });

        // 3. Find-or-create Direccion por (calle, numero, piso, cp) — reutiliza si ya existe
        return direccionRepository
                .findByCalleAndNumeroAndPisoAndCodigoPostalCp(
                        dto.calle(), dto.numero(), dto.piso(), dto.cp())
                .orElseGet(() -> {
                    Direccion d = new Direccion();
                    d.setCalle(dto.calle());
                    d.setNumero(dto.numero());
                    d.setPiso(dto.piso());
                    d.setCodigoPostal(cp);
                    return direccionRepository.save(d);
                });
    }

    /**
     * Guarda o reemplaza la fotografía de un paciente activo.
     *
     * <p>Los bytes de la imagen se persisten directamente en la columna BYTEA.
     * No se aplica cifrado AES-256-GCM a la foto (no es campo clínico sensible).
     * La operación queda registrada en audit_log.</p>
     *
     * @param dni   DNI del paciente.
     * @param bytes Bytes de la imagen (image/png o image/jpeg).
     * @throws RecursoNoEncontradoException si el paciente no existe o está inactivo.
     */
    public void guardarFoto(String dni, byte[] bytes) {
        Paciente paciente = pacienteRepository.findByDniPacAndActivoTrue(dni)
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente no encontrado: " + dni));
        paciente.setFoto(bytes);
        pacienteRepository.save(paciente);
        auditService.registrar(AccionAuditoria.UPDATE, "paciente", dni, "Foto de paciente actualizada");
    }

    /**
     * Devuelve los bytes de la fotografía de un paciente activo.
     *
     * <p>Si el paciente no tiene foto, devuelve null.
     * El acceso queda registrado en audit_log (acceso a datos del paciente).</p>
     *
     * @param dni DNI del paciente.
     * @return Bytes de la imagen, o null si no tiene foto.
     * @throws RecursoNoEncontradoException si el paciente no existe o está inactivo.
     */
    @Transactional(readOnly = true)
    public byte[] obtenerFoto(String dni) {
        Paciente paciente = pacienteRepository.findByDniPacAndActivoTrue(dni)
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente no encontrado: " + dni));
        auditService.registrar(AccionAuditoria.READ, "paciente", dni, "Foto de paciente consultada");
        return paciente.getFoto();
    }

    /**
     * Busca pacientes activos por texto libre en DNI, nombre, apellidos, email y NSS.
     *
     * <p>La búsqueda es case-insensitive gracias al LOWER + LIKE de la query JPQL.
     * Útil para el buscador del desktop ERP y del BFF mobile.</p>
     *
     * @param texto    Término de búsqueda libre (mínimo 1 carácter).
     * @param pageable Configuración de paginación y ordenación.
     * @return Página de pacientes activos que coinciden con el texto.
     */
    @Transactional(readOnly = true)
    public PageResponse<PacienteResponse> buscar(String texto, Pageable pageable) {
        auditService.registrar(AccionAuditoria.READ, "paciente", texto, "Busqueda de pacientes por texto");
        return PageResponse.de(
                pacienteRepository.buscarPorTexto(texto, pageable).map(pacienteMapper::toResponse)
        );
    }

    /**
     * Da de baja lógica a un paciente (soft delete).
     *
     * <p>NUNCA se elimina el registro físicamente. Retención mínima 5 años
     * tras la baja (Ley 41/2002, RGPD Art. 17 con excepción sanitaria).</p>
     *
     * @param dni DNI del paciente a dar de baja.
     * @throws RecursoNoEncontradoException si el paciente no existe o ya está inactivo.
     */
    public void eliminar(String dni) {
        Paciente paciente = pacienteRepository.findByDniPacAndActivoTrue(dni)
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente no encontrado: " + dni));

        // Baja lógica — nunca borrar físicamente (Ley 41/2002 + RGPD Art. 17)
        paciente.setActivo(false);
        paciente.setFechaBaja(LocalDateTime.now());
        pacienteRepository.save(paciente);

        auditService.registrar(AccionAuditoria.SOFT_DELETE, "paciente", dni, "Paciente dado de baja");
    }
}
