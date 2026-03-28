package com.rehabiapp.api.application.dto;

/**
 * DTO de respuesta de autenticación — contiene los tokens JWT y el rol del sanitario.
 *
 * <p>accessToken: token de corta duración (15 min) con DNI y rol.</p>
 * <p>refreshToken: token de larga duración (7 días) con solo el DNI.
 * Se usa para renovar el access token sin volver a autenticarse.</p>
 * <p>rol: nombre del rol asignado al sanitario (SPECIALIST o NURSE).</p>
 */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        String rol
) {}
