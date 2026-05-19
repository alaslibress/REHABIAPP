package com.rehabiapp.data.ingestion.schema;

public class InvalidMetricException extends RuntimeException {
    public InvalidMetricException(String message) {
        super(message);
    }
}
