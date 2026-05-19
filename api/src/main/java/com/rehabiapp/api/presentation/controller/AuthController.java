package com.rehabiapp.api.presentation.controller;

import com.rehabiapp.api.application.dto.LoginRequest;
import com.rehabiapp.api.application.dto.LoginResponse;
import com.rehabiapp.api.application.dto.PacienteLoginRequest;
import com.rehabiapp.api.application.dto.RefreshRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.rehabiapp.api.application.service.AuthApplicationService;
import com.rehabiapp.api.application.service.PacienteAuthApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador de autenticacion — endpoints publicos para login y renovacion de tokens.
 *
 * <p>Estos endpoints son los unicos accesibles sin JWT previo (excluidos en SecurityConfig
 * con /api/auth/**). Toda la logica reside en los servicios de aplicacion.</p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthApplicationService authService;
    private final PacienteAuthApplicationService pacienteAuthService;

    public AuthController(AuthApplicationService authService,
                          PacienteAuthApplicationService pacienteAuthService) {
        this.authService = authService;
        this.pacienteAuthService = pacienteAuthService;
    }

    /**
     * Autentica un sanitario y devuelve un par de tokens JWT (access + refresh).
     *
     * <p>POST /api/auth/login</p>
     *
     * @param request DTO con DNI y contraseña en texto plano.
     * @return 200 OK con accessToken, refreshToken y nombre del rol del sanitario.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Renueva el par de tokens JWT usando un refresh token válido.
     *
     * <p>POST /api/auth/refresh</p>
     *
     * @param request DTO con el refresh token a renovar.
     * @return 200 OK con el nuevo par de tokens y el rol actual del sanitario.
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    /**
     * Renueva el par de tokens JWT de un paciente usando un refresh token valido.
     *
     * <p>POST /api/auth/refresh-paciente</p>
     *
     * <p>Endpoint especifico para pacientes — el endpoint /refresh solo maneja sanitarios.
     * El BFF llama aqui cuando el token Java del paciente esta proximo a expirar.</p>
     *
     * @param request DTO con el refresh token Java del paciente a renovar.
     * @return 200 OK con el nuevo par de tokens y rol "PATIENT".
     */
    @PostMapping("/refresh-paciente")
    public ResponseEntity<LoginResponse> refreshPaciente(
            @Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(pacienteAuthService.refresh(request));
    }

    /**
     * Autentica un paciente desde la app movil y devuelve un par de tokens JWT.
     *
     * <p>POST /api/auth/login-paciente</p>
     *
     * <p>Emite JWT con rol PATIENT. El BFF usa este endpoint cuando MOCK_API=false
     * para obtener un token Java valido con el que llamar a los endpoints del paciente.</p>
     *
     * @param request DTO con identificador (DNI o email) y contrasena del paciente.
     * @return 200 OK con accessToken, refreshToken y rol "PATIENT".
     */
    @PostMapping("/login-paciente")
    public ResponseEntity<LoginResponse> loginPaciente(
            @Valid @RequestBody PacienteLoginRequest request) {
        return ResponseEntity.ok(pacienteAuthService.login(request));
    }
}
