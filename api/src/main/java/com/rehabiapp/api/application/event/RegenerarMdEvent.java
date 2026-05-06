package com.rehabiapp.api.application.event;

/**
 * Evento de aplicacion que solicita la regeneracion asincrona del Markdown
 * de progreso de un paciente tras una nueva sesion de juego.
 */
public record RegenerarMdEvent(String dniPac) {}
