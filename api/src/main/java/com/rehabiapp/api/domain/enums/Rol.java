package com.rehabiapp.api.domain.enums;

/**
 * Roles disponibles en el sistema RehabiAPP.
 *
 * <p>SPECIALIST: acceso completo a todas las operaciones del sistema.</p>
 * <p>NURSE: acceso de solo lectura a pacientes, sin gestion de sanitarios.</p>
 * <p>PATIENT: acceso exclusivamente a los datos propios del paciente via app movil.</p>
 *
 * <p>Implementa el control de acceso basado en roles (RBAC) requerido
 * por ENS Alto y RGPD Art. 32 (seguridad en el tratamiento de datos).</p>
 */
public enum Rol {

    /** Especialista con acceso completo al sistema. */
    SPECIALIST,

    /** Enfermero con permisos de lectura restringidos. */
    NURSE,

    /**
     * Paciente — accede unicamente a sus propios datos clinicos via BFF movil.
     * El JWT con este rol solo puede ser emitido por el endpoint de login de pacientes.
     */
    PATIENT
}
