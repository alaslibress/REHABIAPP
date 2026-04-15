package com.javafx.Clases;

/**
 * Clase Singleton para gestionar la sesion del usuario logueado.
 * Almacena los datos del sanitario que ha iniciado sesion, sus permisos y los tokens JWT.
 */
public class SesionUsuario {

    // Instancia unica (Singleton)
    private static SesionUsuario instancia;

    // Datos del usuario logueado
    private String dniUsuario;
    private String nombreUsuario;
    private String apellidosUsuario;
    private String emailUsuario;
    private String cargo;

    // Tokens JWT de la API
    private String accessToken;
    private String refreshToken;
    // Rol devuelto por la API: "SPECIALIST" o "NURSE"
    private String rolApi;

    // Constantes para los tipos de cargo (formato legado de la BD)
    public static final String CARGO_ESPECIALISTA = "medico especialista";
    public static final String CARGO_ENFERMERO = "enfermero";

    // Constantes para el rol de la API
    public static final String ROL_SPECIALIST = "SPECIALIST";
    public static final String ROL_NURSE = "NURSE";

    /**
     * Constructor privado (Singleton).
     */
    private SesionUsuario() {
        limpiarSesion();
    }

    /**
     * Obtiene la instancia unica de SesionUsuario.
     */
    public static SesionUsuario getInstancia() {
        if (instancia == null) {
            instancia = new SesionUsuario();
        }
        return instancia;
    }

    /**
     * Inicia sesion con los datos del sanitario y los tokens JWT.
     * @param dni DNI del sanitario
     * @param nombre Nombre del sanitario
     * @param apellidos Apellidos del sanitario
     * @param email Email del sanitario
     * @param cargo Cargo (medico especialista o enfermero)
     * @param accessToken Token de acceso JWT
     * @param refreshToken Token de refresco JWT
     * @param rolApi Rol de la API ("SPECIALIST" o "NURSE")
     */
    public void iniciarSesion(String dni, String nombre, String apellidos, String email,
                              String cargo, String accessToken, String refreshToken, String rolApi) {
        this.dniUsuario = dni;
        this.nombreUsuario = nombre;
        this.apellidosUsuario = apellidos;
        this.emailUsuario = email;
        this.cargo = cargo != null ? cargo.toLowerCase().trim() : "";
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.rolApi = rolApi;

        System.out.println("Sesion iniciada: " + getNombreCompleto() + " (" + this.cargo + ")");
    }

    /**
     * Cierra la sesion actual.
     */
    public void cerrarSesion() {
        System.out.println("Sesion cerrada: " + getNombreCompleto());
        ApiClient.getInstancia().logout();
        limpiarSesion();
    }

    /**
     * Limpia todos los datos de sesion.
     */
    private void limpiarSesion() {
        this.dniUsuario = null;
        this.nombreUsuario = null;
        this.apellidosUsuario = null;
        this.emailUsuario = null;
        this.cargo = null;
        this.accessToken = null;
        this.refreshToken = null;
        this.rolApi = null;
    }

    /**
     * Verifica si hay una sesion activa.
     */
    public boolean haySesionActiva() {
        return dniUsuario != null && !dniUsuario.isEmpty();
    }

    // ==================== VERIFICACION DE PERMISOS ====================

    /**
     * Verifica si el usuario es Especialista.
     * Acepta tanto el formato de la API ("SPECIALIST") como el formato legado.
     */
    public boolean esEspecialista() {
        if (ROL_SPECIALIST.equalsIgnoreCase(rolApi)) {
            return true;
        }
        return CARGO_ESPECIALISTA.equalsIgnoreCase(cargo);
    }

    /**
     * Verifica si el usuario es Enfermero.
     */
    public boolean esEnfermero() {
        if (ROL_NURSE.equalsIgnoreCase(rolApi)) {
            return true;
        }
        return CARGO_ENFERMERO.equalsIgnoreCase(cargo);
    }

    /**
     * Verifica si el usuario puede insertar pacientes/sanitarios.
     */
    public boolean puedeInsertarUsuarios() {
        return esEspecialista();
    }

    /**
     * Verifica si el usuario puede editar pacientes/sanitarios.
     */
    public boolean puedeEditarUsuarios() {
        return esEspecialista();
    }

    /**
     * Verifica si el usuario puede eliminar pacientes/sanitarios.
     */
    public boolean puedeEliminarUsuarios() {
        return esEspecialista();
    }

    /**
     * Verifica si el usuario puede gestionar citas.
     */
    public boolean puedeGestionarCitas() {
        return esEspecialista() || esEnfermero();
    }

    /**
     * Verifica si el usuario puede ver datos (lectura).
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

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getRolApi() {
        return rolApi;
    }

    /**
     * Devuelve el cargo traducido al espanol para mostrar en la interfaz.
     * Prioriza el rol de la API ("SPECIALIST", "NURSE") sobre el campo cargo legado.
     */
    public String getCargoTraducido() {
        String c = rolApi != null ? rolApi : cargo;
        if (c == null) return "-";
        return switch (c.toUpperCase()) {
            case "SPECIALIST" -> "Especialista";
            case "NURSE" -> "Enfermero/a";
            default -> c;
        };
    }

    /**
     * Obtiene el nombre completo del usuario.
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
                ", rolApi='" + rolApi + '\'' +
                '}';
    }
}
