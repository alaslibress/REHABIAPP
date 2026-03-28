package com.rehabiapp.api.presentation.controller;

import com.rehabiapp.api.application.dto.LoginRequest;
import com.rehabiapp.api.application.dto.LoginResponse;
import com.rehabiapp.api.application.dto.RefreshRequest;
import com.rehabiapp.api.application.service.AuthApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador de autenticación — endpoints públicos para login y renovación de tokens.
 *
 * <p>Estos endpoints son los únicos accesibles sin JWT previo (excluidos en SecurityConfig
 * con /api/auth/**). Toda la lógica reside en AuthApplicationService.</p>
 *
 * <p>Los endpoints POST /login y POST /refresh están excluidos del filtro JWT
 * (configurado en SecurityConfig con .requestMatchers("/api/auth/**").permitAll()).</p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthApplicationService authService;

    public AuthController(AuthApplicationService authService) {
        this.authService = authService;
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
}
