package com.rehabiapp.data.application.service;

public class DuplicateSessionException extends RuntimeException {
    private final String sessionId;

    public DuplicateSessionException(String sessionId) {
        super("Sesion ya ingerida: " + sessionId);
        this.sessionId = sessionId;
    }

    public String getSessionId() { return sessionId; }
}
