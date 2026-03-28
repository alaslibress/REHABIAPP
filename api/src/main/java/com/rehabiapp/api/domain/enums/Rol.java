package com.rehabiapp.api.domain.enums;

/**
 * Roles disponibles para los sanitarios en el sistema RehabiAPP.
 *
 * <p>SPECIALIST: acceso completo a todas las operaciones del sistema.</p>
 * <p>NURSE: acceso de sólo lectura a pacientes, sin gestión de sanitarios.</p>
 *
 * <p>Implementa el control de acceso basado en roles (RBAC) requerido
 * por ENS Alto y RGPD Art. 32 (seguridad en el tratamiento de datos).</p>
 */
public enum Rol {

    /** Especialista con acceso completo al sistema. */
    SPECIALIST,

    /** Enfermero con permisos de lectura restringidos. */
    NURSE
}
