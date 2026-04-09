package com.rehabiapp.api.application.service;

import com.rehabiapp.api.application.dto.LoginRequest;
import com.rehabiapp.api.application.dto.LoginResponse;
import com.rehabiapp.api.application.dto.RefreshRequest;
import com.rehabiapp.api.domain.entity.Sanitario;
import com.rehabiapp.api.domain.enums.AccionAuditoria;
import com.rehabiapp.api.domain.enums.Rol;
import com.rehabiapp.api.domain.exception.AccesoNoPermitidoException;
import com.rehabiapp.api.domain.repository.SanitarioRepository;
import com.rehabiapp.api.infrastructure.audit.AuditService;
import com.rehabiapp.api.infrastructure.security.JwtService;
import com.rehabiapp.api.infrastructure.security.PasswordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de autenticación de la capa de aplicación.
 *
 * <p>Gestiona el flujo de login y renovación de tokens JWT.
 * Nunca lanza mensajes de error que distingan entre "usuario no existe"
 * y "contraseña incorrecta" para evitar ataques de enumeración de usuarios.</p>
 *
 * <p>Registra todos los accesos en audit_log para cumplir con:
 * Ley 41/2002 (trazabilidad de accesos), ENS Alto.</p>
 */
@Service
@Transactional
public class AuthApplicationService {

    private static final Logger log = LoggerFactory.getLogger(AuthApplicationService.class);

    private final SanitarioRepository sanitarioRepository;
    private final PasswordService passwordService;
    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthApplicationService(
            SanitarioRepository sanitarioRepository,
            PasswordService passwordService,
            JwtService jwtService,
            AuditService auditService
    ) {
        this.sanitarioRepository = sanitarioRepository;
        this.passwordService = passwordService;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    /**
     * Autentica un sanitario y devuelve un par de tokens JWT.
     *
     * <p>Flujo: buscar sanitario activo -> verificar contraseña -> extraer rol
     * -> generar access token + refresh token -> registrar en audit_log.</p>
     *
     * <p>El mensaje de error es genérico para evitar enumeración de usuarios.</p>
     *
     * @param request DTO con DNI y contraseña en texto plano.
     * @return LoginResponse con accessToken, refreshToken y nombre del rol.
     * @throws AccesoNoPermitidoException si las credenciales son incorrectas o el sanitario está inactivo.
     */
    public LoginResponse login(LoginRequest request) {
        // Buscar sanitario activo por DNI
        Sanitario sanitario = sanitarioRepository.findByDniSanAndActivoTrue(request.dni())
                .orElseThrow(() -> {
                    log.warn("Intento de login fallido — sanitario no encontrado o inactivo");
                    return new AccesoNoPermitidoException("Credenciales inválidas");
                });

        // Verificar contraseña contra el hash BCrypt almacenado
        if (!passwordService.verificar(request.contrasena(), sanitario.getContrasenaSan())) {
            log.warn("Intento de login fallido — contrasena incorrecta");
            throw new AccesoNoPermitidoException("Credenciales inválidas");
        }

        // Obtener rol — si no tiene rol asignado, se asigna NURSE por defecto
        Rol rol = sanitario.getRol() != null ? sanitario.getRol().getCargo() : Rol.NURSE;

        // Generar par de tokens JWT
        String accessToken = jwtService.generarAccessToken(request.dni(), rol);
        String refreshToken = jwtService.generarRefreshToken(request.dni());

        // Registrar acceso en audit_log (Ley 41/2002, ENS Alto)
        // Se usa la sobrecarga con dniUsuario explicito porque en el momento del login
        // el SecurityContext todavia no tiene al usuario autenticado.
        String nombreCompleto = sanitario.getNombreSan() + " " + sanitario.getApellido1San();
        auditService.registrar(AccionAuditoria.LOGIN, "sanitario", request.dni(), "Login exitoso",
                request.dni(), nombreCompleto);

        log.info("Login exitoso — rol={}", rol.name());
        return new LoginResponse(accessToken, refreshToken, rol.name());
    }

    /**
     * Renueva el par de tokens JWT usando un refresh token válido.
     *
     * <p>Verifica que el sanitario sigue activo en el sistema antes de emitir nuevos tokens.
     * Un refresh token no proporciona acceso directo a recursos — solo permite renovar tokens.</p>
     *
     * @param request DTO con el refresh token a renovar.
     * @return LoginResponse con el nuevo par de tokens y el rol actual del sanitario.
     * @throws AccesoNoPermitidoException si el sanitario ha sido dado de baja.
     */
    public LoginResponse refresh(RefreshRequest request) {
        // Extraer el DNI del refresh token (valida firma y caducidad internamente)
        String dni = jwtService.extraerDni(request.refreshToken());

        // Verificar que el sanitario sigue activo en el sistema
        Sanitario sanitario = sanitarioRepository.findByDniSanAndActivoTrue(dni)
                .orElseThrow(() -> new AccesoNoPermitidoException("Usuario inactivo"));

        // Generar nuevo par de tokens con el rol actual
        Rol rol = sanitario.getRol() != null ? sanitario.getRol().getCargo() : Rol.NURSE;

        return new LoginResponse(
                jwtService.generarAccessToken(dni, rol),
                jwtService.generarRefreshToken(dni),
                rol.name()
        );
    }
}
