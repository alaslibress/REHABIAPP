package com.javafx.Clases;

/**
 * Clase Singleton para gestionar la sesion del usuario logueado
 * Almacena los datos del sanitario que ha iniciado sesion y sus permisos
 */
public class SesionUsuario {

    //Instancia unica (Singleton)
    private static SesionUsuario instancia;

    //Datos del usuario logueado
    private String dniUsuario;
    private String nombreUsuario;
    private String apellidosUsuario;
    private String emailUsuario;
    private String cargo;

    //Constantes para los tipos de cargo (en minusculas segun CHECK de la BD)
    public static final String CARGO_ESPECIALISTA = "medico especialista";
    public static final String CARGO_ENFERMERO = "enfermero";

    /**
     * Constructor privado (Singleton)
     */
    private SesionUsuario() {
        limpiarSesion();
    }

    /**
     * Obtiene la instancia unica de SesionUsuario
     * @return Instancia de SesionUsuario
     */
    public static SesionUsuario getInstancia() {
        if (instancia == null) {
            instancia = new SesionUsuario();
        }
        return instancia;
    }

    /**
     * Inicia sesion con los datos del sanitario
     * @param dni DNI del sanitario
     * @param nombre Nombre del sanitario
     * @param apellidos Apellidos del sanitario
     * @param email Email del sanitario
     * @param cargo Cargo (medico especialista o enfermero)
     */
    public void iniciarSesion(String dni, String nombre, String apellidos, String email, String cargo) {
        this.dniUsuario = dni;
        this.nombreUsuario = nombre;
        this.apellidosUsuario = apellidos;
        this.emailUsuario = email;
        this.cargo = cargo != null ? cargo.toLowerCase().trim() : "";

        System.out.println("Sesion iniciada: " + getNombreCompleto() + " (" + this.cargo + ")");
    }

    /**
     * Cierra la sesion actual
     */
    public void cerrarSesion() {
        System.out.println("Sesion cerrada: " + getNombreCompleto());
        limpiarSesion();
    }

    /**
     * Limpia todos los datos de sesion
     */
    private void limpiarSesion() {
        this.dniUsuario = null;
        this.nombreUsuario = null;
        this.apellidosUsuario = null;
        this.emailUsuario = null;
        this.cargo = null;
    }

    /**
     * Verifica si hay una sesion activa
     * @return true si hay un usuario logueado
     */
    public boolean haySesionActiva() {
        return dniUsuario != null && !dniUsuario.isEmpty();
    }

    // ==================== VERIFICACION DE PERMISOS ====================

    /**
     * Verifica si el usuario es Especialista (tiene todos los permisos)
     * @return true si es Especialista
     */
    public boolean esEspecialista() {
        return CARGO_ESPECIALISTA.equalsIgnoreCase(cargo);
    }

    /**
     * Verifica si el usuario es Enfermero
     * @return true si es Enfermero
     */
    public boolean esEnfermero() {
        return CARGO_ENFERMERO.equalsIgnoreCase(cargo);
    }

    /**
     * Verifica si el usuario puede insertar pacientes/sanitarios
     * Solo los Especialistas pueden
     * @return true si tiene permiso
     */
    public boolean puedeInsertarUsuarios() {
        return esEspecialista();
    }

    /**
     * Verifica si el usuario puede editar pacientes/sanitarios
     * Solo los Especialistas pueden
     * @return true si tiene permiso
     */
    public boolean puedeEditarUsuarios() {
        return esEspecialista();
    }

    /**
     * Verifica si el usuario puede eliminar pacientes/sanitarios
     * Solo los Especialistas pueden
     * @return true si tiene permiso
     */
    public boolean puedeEliminarUsuarios() {
        return esEspecialista();
    }

    /**
     * Verifica si el usuario puede gestionar citas
     * Tanto Especialistas como Enfermeros pueden
     * @return true si tiene permiso
     */
    public boolean puedeGestionarCitas() {
        return esEspecialista() || esEnfermero();
    }

    /**
     * Verifica si el usuario puede ver datos (lectura)
     * Todos los usuarios logueados pueden
     * @return true si tiene permiso
     */
    public boolean puedeLeerDatos() {
        return haySesionActiva();
    }

    // ==================== GETTERS ====================

    public String getDniUsuario() {
        return dniUsuario;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public String getApellidosUsuario() {
        return apellidosUsuario;
    }

    public String getEmailUsuario() {
        return emailUsuario;
    }

    public String getCargo() {
        return cargo;
    }

    /**
     * Obtiene el nombre completo del usuario
     * @return Nombre + Apellidos
     */
    public String getNombreCompleto() {
        if (nombreUsuario == null) {
            return "";
        }
        return nombreUsuario + " " + (apellidosUsuario != null ? apellidosUsuario : "");
    }

    @Override
    public String toString() {
        return "SesionUsuario{" +
                "dni='" + dniUsuario + '\'' +
                ", nombre='" + getNombreCompleto() + '\'' +
                ", cargo='" + cargo + '\'' +
                '}';
    }
}