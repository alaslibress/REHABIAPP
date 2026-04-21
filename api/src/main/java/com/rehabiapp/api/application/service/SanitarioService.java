package com.rehabiapp.api.application.service;

import com.rehabiapp.api.application.dto.PageResponse;
import com.rehabiapp.api.application.dto.SanitarioRequest;
import com.rehabiapp.api.application.dto.SanitarioResponse;
import com.rehabiapp.api.application.mapper.SanitarioMapper;
import com.rehabiapp.api.domain.entity.Sanitario;
import com.rehabiapp.api.domain.entity.SanitarioRol;
import com.rehabiapp.api.domain.entity.TelefonoSanitario;
import com.rehabiapp.api.domain.enums.AccionAuditoria;
import com.rehabiapp.api.domain.exception.RecursoNoEncontradoException;
import com.rehabiapp.api.domain.repository.SanitarioRepository;
import com.rehabiapp.api.infrastructure.audit.AuditService;
import com.rehabiapp.api.infrastructure.security.PasswordService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Servicio de aplicación para la gestión de sanitarios.
 *
 * <p>Implementa las operaciones CRUD sobre la entidad Sanitario.
 * Toda operación se registra en audit_log para cumplir con
 * ENS Alto y RGPD Art. 30.</p>
 *
 * <p>Soft delete: los sanitarios nunca se eliminan físicamente.
 * El campo activo=false + fechaBaja marca la baja lógica.</p>
 */
@Service
@Transactional
public class SanitarioService {

    private final SanitarioRepository sanitarioRepository;
    private final SanitarioMapper sanitarioMapper;
    private final PasswordService passwordService;
    private final AuditService auditService;

    public SanitarioService(
            SanitarioRepository sanitarioRepository,
            SanitarioMapper sanitarioMapper,
            PasswordService passwordService,
            AuditService auditService
    ) {
        this.sanitarioRepository = sanitarioRepository;
        this.sanitarioMapper = sanitarioMapper;
        this.passwordService = passwordService;
        this.auditService = auditService;
    }

    /**
     * Devuelve todos los sanitarios activos paginados.
     *
     * @param pageable Configuración de paginación.
     * @return Página de sanitarios activos como DTOs de respuesta.
     */
    @Transactional(readOnly = true)
    public PageResponse<SanitarioResponse> listar(Pageable pageable) {
        auditService.registrar(AccionAuditoria.READ, "sanitario", "todos", "Listado de sanitarios");
        return PageResponse.de(
                sanitarioRepository.findAllByActivoTrue(pageable).map(sanitarioMapper::toResponse)
        );
    }

    /**
     * Devuelve un sanitario activo por su DNI.
     *
     * @param dni DNI del sanitario a consultar.
     * @return DTO de respuesta con los datos del sanitario.
     * @throws RecursoNoEncontradoException si no existe o está inactivo.
     */
    @Transactional(readOnly = true)
    public SanitarioResponse obtenerPorDni(String dni) {
        Sanitario sanitario = sanitarioRepository.findByDniSanAndActivoTrue(dni)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sanitario no encontrado: " + dni));
        auditService.registrar(AccionAuditoria.READ, "sanitario", dni, "Consulta sanitario");
        return sanitarioMapper.toResponse(sanitario);
    }

    /**
     * Busca sanitarios activos por texto libre en DNI, nombre, apellidos y email.
     *
     * <p>La búsqueda es case-insensitive gracias al LOWER + LIKE de la query JPQL.
     * Útil para el buscador del desktop ERP y del BFF mobile.</p>
     *
     * @param texto    Término de búsqueda libre (mínimo 1 carácter).
     * @param pageable Configuración de paginación y ordenación.
     * @return Página de sanitarios activos que coinciden con el texto.
     */
    @Transactional(readOnly = true)
    public PageResponse<SanitarioResponse> buscar(String texto, Pageable pageable) {
        auditService.registrar(AccionAuditoria.READ, "sanitario", texto, "Busqueda de sanitarios por texto");
        return PageResponse.de(
                sanitarioRepository.buscarPorTexto(texto, pageable).map(sanitarioMapper::toResponse)
        );
    }

    /**
     * Crea un nuevo sanitario en el sistema.
     *
     * <p>La contraseña se hashea con BCrypt (factor 12) antes de persistir.
     * Se asigna el rol y los teléfonos en cascada junto con el sanitario.</p>
     *
     * @param request DTO con los datos del nuevo sanitario.
     * @return DTO de respuesta con el sanitario creado.
     */
    public SanitarioResponse crear(SanitarioRequest request) {
        Sanitario sanitario = new Sanitario();
        sanitario.setDniSan(request.dniSan());
        sanitario.setNombreSan(request.nombreSan());
        sanitario.setApellido1San(request.apellido1San());
        sanitario.setApellido2San(request.apellido2San());
        sanitario.setEmailSan(request.emailSan());
        // Hashear contraseña con BCrypt factor 12 (RGPD Art. 32)
        sanitario.setContrasenaSan(passwordService.hashear(request.contrasena()));
        sanitario.setActivo(true);
        sanitario.setNumDePacientes(0);

        // Añadir teléfonos en cascada
        if (request.telefonos() != null) {
            request.telefonos().forEach(tel -> {
                TelefonoSanitario t = new TelefonoSanitario();
                t.setSanitario(sanitario);
                t.setTelefono(tel);
                sanitario.getTelefonos().add(t);
            });
        }

        // Asignar rol RBAC al sanitario
        SanitarioRol rol = new SanitarioRol();
        rol.setSanitario(sanitario);
        rol.setCargo(request.cargo());
        sanitario.setRol(rol);

        Sanitario guardado = sanitarioRepository.save(sanitario);
        auditService.registrar(AccionAuditoria.CREATE, "sanitario", request.dniSan(), "Sanitario creado");
        return sanitarioMapper.toResponse(guardado);
    }

    /**
     * Actualiza los datos de un sanitario existente.
     *
     * <p>Si la contraseña viene vacía o null, no se actualiza el hash existente.
     * Esto permite actualizar otros campos sin forzar el cambio de contraseña.</p>
     *
     * @param dni     DNI del sanitario a actualizar.
     * @param request DTO con los nuevos datos del sanitario.
     * @return DTO de respuesta con el sanitario actualizado.
     * @throws RecursoNoEncontradoException si no existe o está inactivo.
     */
    public SanitarioResponse actualizar(String dni, SanitarioRequest request) {
        Sanitario sanitario = sanitarioRepository.findByDniSanAndActivoTrue(dni)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sanitario no encontrado: " + dni));

        sanitario.setNombreSan(request.nombreSan());
        sanitario.setApellido1San(request.apellido1San());
        sanitario.setApellido2San(request.apellido2San());
        sanitario.setEmailSan(request.emailSan());

        // Solo actualizar contraseña si se proporciona una nueva
        if (request.contrasena() != null && !request.contrasena().isBlank()) {
            sanitario.setContrasenaSan(passwordService.hashear(request.contrasena()));
        }

        auditService.registrar(AccionAuditoria.UPDATE, "sanitario", dni, "Sanitario actualizado");
        return sanitarioMapper.toResponse(sanitarioRepository.save(sanitario));
    }

    /**
     * Da de baja lógica a un sanitario (soft delete).
     *
     * <p>NUNCA se elimina el registro físicamente. Se marca activo=false
     * y se registra la fecha de baja (RGPD Art. 17 con excepción sanitaria,
     * datos retenidos mínimo 5 años: Ley 41/2002).</p>
     *
     * @param dni DNI del sanitario a dar de baja.
     * @throws RecursoNoEncontradoException si no existe o ya está inactivo.
     */
    public void eliminar(String dni) {
        Sanitario sanitario = sanitarioRepository.findByDniSanAndActivoTrue(dni)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sanitario no encontrado: " + dni));

        // Baja lógica — nunca borrar físicamente (RGPD + Ley 41/2002)
        sanitario.setActivo(false);
        sanitario.setFechaBaja(LocalDateTime.now());
        sanitarioRepository.save(sanitario);

        auditService.registrar(AccionAuditoria.SOFT_DELETE, "sanitario", dni, "Sanitario dado de baja");
    }
}
