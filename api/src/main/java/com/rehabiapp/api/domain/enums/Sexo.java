package com.rehabiapp.api.domain.enums;

/**
 * Sexo biológico del paciente para fines clínicos y estadísticos.
 *
 * <p>Almacenado como cadena de texto en la base de datos para legibilidad
 * y compatibilidad con el esquema del ERP de escritorio.</p>
 */
public enum Sexo {

    /** Sexo masculino. */
    MASCULINO,

    /** Sexo femenino. */
    FEMENINO,

    /** Otro (no binario o no especificado). */
    OTRO
}
