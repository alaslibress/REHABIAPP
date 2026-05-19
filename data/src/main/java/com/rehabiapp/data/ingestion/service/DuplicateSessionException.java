package com.rehabiapp.data.ingestion.service;

public class DuplicateSessionException extends RuntimeException {

    public DuplicateSessionException(String message) {
        super(message);
    }
}
