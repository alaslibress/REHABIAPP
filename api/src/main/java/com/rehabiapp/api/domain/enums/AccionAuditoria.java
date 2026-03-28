package com.rehabiapp.api.domain.enums;

/**
 * Tipos de acción registrados en el log de auditoría del sistema.
 *
 * <p>Complementa a Hibernate Envers, que sólo registra escrituras.
 * Este enum se utiliza también para registrar accesos de lectura
 * sobre datos clínicos, cumpliendo con:</p>
 * <ul>
 *   <li>Ley 41/2002 (registro de accesos a historiales clínicos)</li>
 *   <li>RGPD Art. 30 (registro de actividades de tratamiento)</li>
 *   <li>ENS Alto (trazabilidad completa de operaciones)</li>
 * </ul>
 */
public enum AccionAuditoria {

    /** Creación de un nuevo registro en el sistema. */
    CREAR,

    /** Lectura o consulta de un registro existente. */
    LEER,

    /** Modificación de un registro existente. */
    ACTUALIZAR,

    /** Baja lógica (soft delete) de un registro. */
    ELIMINAR
}
