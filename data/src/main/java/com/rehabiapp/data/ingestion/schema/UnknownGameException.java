package com.rehabiapp.data.ingestion.schema;

public class UnknownGameException extends RuntimeException {
    public UnknownGameException(String gameId) {
        super("gameId desconocido — sin schema registrado: " + gameId);
    }
}
