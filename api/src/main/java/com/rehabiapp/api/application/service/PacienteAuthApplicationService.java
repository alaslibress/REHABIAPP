package com.rehabiapp.api.application.service;

import com.rehabiapp.api.application.dto.LoginResponse;
import com.rehabiapp.api.application.dto.PacienteLoginRequest;
import com.rehabiapp.api.application.dto.RefreshRequest;
import com.rehabiapp.api.domain.entity.Paciente;
import com.rehabiapp.api.domain.enums.AccionAuditoria;
import com.rehabiapp.api.domain.enums.Rol;
import com.rehabiapp.api.domain.exception.AccesoNoPermitidoException;
import com.rehabiapp.api.domain.repository.PacienteRepository;
import com.rehabiapp.api.infrastructure.audit.AuditService;
import com.rehabiapp.api.infrastructure.security.JwtService;
import com.rehabiapp.api.infrastructure.security.PasswordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de autenticacion de pacientes para la app movil.
 *
 * <p>Independiente de {@code AuthApplicationService} (sanitarios) para mantener
 * separacion de responsabilidades y evitar que errores en uno afecten al otro.</p>
 *
 * <p>Emite JWT con {@code rol=PATIENT} — los endpoints de paciente validan este rol
 * via {@code @PreAuthorize("hasAnyRole('SPECIALIST','NURSE','PATIENT')")}.</p>
 *
 * <p>El identificador puede ser el DNI o el email del paciente.
 * El mensaje de error es generico para evitar ataques de enumeracion de usuarios.</p>
 *
 * <p>Registra todos los accesos en audit_log (Ley 41/2002, ENS Alto).</p>
 */
@Service
@Transactional
public class PacienteAuthApplicationService {

    private static final Logger log = LoggerFactory.getLogger(PacienteAuthApplicationService.class);

    private final PacienteRepository pacienteRepository;
    private final PasswordService passwordService;
    private final JwtService jwtService;
    private final AuditService auditService;

    public PacienteAuthApplicationService(
            PacienteRepository pacienteRepository,
            PasswordService passwordService,
            JwtService jwtService,
            AuditService auditService
    ) {
        this.pacienteRepository = pacienteRepository;
        this.passwordService = passwordService;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    /**
     * Autentica un paciente y devuelve un par de tokens JWT con rol PATIENT.
     *
     * <p>Flujo: buscar paciente activo (por DNI o email) -> verificar contrasena BCrypt
     * -> generar access token + refresh token -> registrar en audit_log.</p>
     *
     * @param request DTO con identificador (DNI o email) y contrasena.
     * @return LoginResponse con accessToken, refreshToken y rol "PATIENT".
     * @throws AccesoNoPermitidoException si las credenciales son incorrectas,
     *         el paciente no existe, no tiene contrasena configurada o esta inactivo.
     */
    public LoginResponse login(PacienteLoginRequest request) {
        String identifier = request.identifier().trim();

        // Buscar paciente activo por DNI o por email (segun el formato del identificador)
        Paciente paciente = buscarPacienteActivo(identifier);

        // Verificar que el paciente tiene contrasena configurada (V15)
        if (paciente.getContrasenaPac() == null || paciente.getContrasenaPac().isBlank()) {
            log.warn("Intento de login de paciente sin contrasena configurada — dni={}***",
                    paciente.getDniPac().substring(0, 3));
            // Mensaje generico — no revelar la causa exacta del fallo
            throw new AccesoNoPermitidoException("Credenciales invalidas");
        }

        // Verificar contrasena contra hash BCrypt
        if (!passwordService.verificar(request.contrasena(), paciente.getContrasenaPac())) {
            log.warn("Contrasena incorrecta para paciente — dni={}***",
                    paciente.getDniPac().substring(0, 3));
            throw new AccesoNoPermitidoException("Credenciales invalidas");
        }

        // Emitir JWT con rol PATIENT
        String dniPac = paciente.getDniPac();
        String accessToken  = jwtService.generarAccessToken(dniPac, Rol.PATIENT);
        String refreshToken = jwtService.generarRefreshToken(dniPac);

        // Registrar acceso en audit_log (Ley 41/2002, ENS Alto)
        String nombreCompleto = paciente.getNombrePac() + " " + paciente.getApellido1Pac();
        auditService.registrar(AccionAuditoria.LOGIN, "paciente", dniPac,
                "Login movil exitoso", dniPac, nombreCompleto);

        log.info("Login movil exitoso — dniPac={}***", dniPac.substring(0, 3));
        return new LoginResponse(accessToken, refreshToken, Rol.PATIENT.name());
    }

    /**
     * Renueva el par de tokens JWT de un paciente usando un refresh token valido.
     *
     * <p>Verifica que el paciente sigue activo antes de emitir nuevos tokens.
     * El refresh token no contiene rol — el nuevo access token siempre emite PATIENT.</p>
     *
     * @param request DTO con el refresh token Java a renovar.
     * @return LoginResponse con el nuevo par de tokens y rol "PATIENT".
     * @throws AccesoNoPermitidoException si el refresh token es invalido, ha expirado,
     *         o el paciente ha sido dado de baja.
     */
    public LoginResponse refresh(RefreshRequest request) {
        // Extraer el DNI del refresh token (valida firma y caducidad internamente)
        String dniPac = jwtService.extraerDni(request.refreshToken());

        // Verificar que el paciente sigue activo en el sistema
        Paciente paciente = pacienteRepository.findByDniPacAndActivoTrue(dniPac)
                .orElseThrow(() -> {
                    log.warn("Refresh paciente — paciente inactivo o no encontrado — dni={}***",
                            dniPac.substring(0, 3));
                    return new AccesoNoPermitidoException("Paciente inactivo");
                });

        // Emitir nuevo par de tokens con rol PATIENT
        String accessToken  = jwtService.generarAccessToken(dniPac, Rol.PATIENT);
        String refreshToken = jwtService.generarRefreshToken(dniPac);

        // Registrar renovacion en audit_log (ENS Alto — trazabilidad de sesiones)
        String nombreCompleto = paciente.getNombrePac() + " " + paciente.getApellido1Pac();
        auditService.registrar(AccionAuditoria.LOGIN, "paciente", dniPac,
                "Renovacion de token movil", dniPac, nombreCompleto);

        log.info("Refresh movil exitoso — dniPac={}***", dniPac.substring(0, 3));
        return new LoginResponse(accessToken, refreshToken, Rol.PATIENT.name());
    }

    /**
     * Busca un paciente activo usando el identificador.
     * Si el identificador contiene '@', se trata como email; en caso contrario, como DNI.
     *
     * @param identifier DNI o email del paciente.
     * @return Paciente activo.
     * @throws AccesoNoPermitidoException si no se encuentra.
     */
    private Paciente buscarPacienteActivo(String identifier) {
        if (identifier.contains("@")) {
            return pacienteRepository.findByEmailPacAndActivoTrue(identifier)
                    .orElseThrow(() -> {
                        log.warn("Intento de login movil — email no encontrado");
                        return new AccesoNoPermitidoException("Credenciales invalidas");
                    });
        } else {
            return pacienteRepository.findByDniPacAndActivoTrue(identifier)
                    .orElseThrow(() -> {
                        log.warn("Intento de login movil — DNI no encontrado o paciente inactivo");
                        return new AccesoNoPermitidoException("Credenciales invalidas");
                    });
        }
    }
}
