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

    /** Inicio de sesion de un usuario en el sistema. */
    LOGIN,

    /** Cierre de sesion de un usuario. */
    LOGOUT,

    /** Creacion de un nuevo registro en el sistema. */
    CREATE,

    /** Lectura o consulta de un registro existente. */
    READ,

    /** Modificacion de un registro existente. */
    UPDATE,

    /** Baja logica (soft delete) de un registro. */
    SOFT_DELETE,

    /** Exportacion de datos (PDF, CSV, etc.). */
    EXPORT,

    /** Impresion de datos. */
    PRINT,

    /** Cambio de contrasena de un usuario. */
    CAMBIO_CONTRASENA
}
