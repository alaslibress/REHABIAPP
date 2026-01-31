package com.javafx.Clases;

/**
 * Interface para entidades que representan personas
 * Evita duplicacion de metodos getApellidos() y getNombreCompleto()
 * Implementada por Paciente y Sanitario
 */
public interface Persona {

    /**
     * Obtiene el nombre de la persona
     * @return Nombre
     */
    String getNombre();

    /**
     * Obtiene el primer apellido de la persona
     * @return Primer apellido
     */
    String getApellido1();

    /**
     * Obtiene el segundo apellido de la persona
     * @return Segundo apellido (puede ser null)
     */
    String getApellido2();

    /**
     * Obtiene apellidos concatenados
     * Implementacion por defecto que concatena apellido1 y apellido2 (si existe)
     * @return Apellidos completos
     */
    default String getApellidos() {
        String ap1 = getApellido1() != null ? getApellido1() : "";
        String ap2 = getApellido2() != null ? getApellido2() : "";

        if (ap2.isEmpty()) {
            return ap1;
        }
        return ap1 + " " + ap2;
    }

    /**
     * Obtiene nombre completo de la persona
     * Implementacion por defecto que concatena nombre y apellidos
     * @return Nombre completo
     */
    default String getNombreCompleto() {
        return getNombre() + " " + getApellidos();
    }
}
