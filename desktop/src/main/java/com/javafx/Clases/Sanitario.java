package com.javafx.Clases;

import com.javafx.dto.SanitarioRequest;
import com.javafx.dto.SanitarioResponse;
import javafx.beans.property.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Clase modelo que representa un sanitario del sistema.
 * Corresponde a las tablas: sanitario, sanitario_agrega_sanitario y telefono_sanitario.
 */
public class Sanitario implements Persona {

    // Propiedades observables para enlazar con TableView de JavaFX
    private final StringProperty dni;
    private final StringProperty nombre;
    private final StringProperty apellido1;
    private final StringProperty apellido2;
    private final StringProperty email;
    private final StringProperty cargo;
    private final IntegerProperty numPacientes;
    private final BooleanProperty activo;

    // Atributos simples que no necesitan ser observables
    private String contrasena;
    private String telefono1;
    private String telefono2;

    /**
     * Constructor completo con todos los campos principales.
     */
    public Sanitario(String dni, String nombre, String apellido1, String apellido2,
                     String email, String cargo, int numPacientes) {
        this.dni = new SimpleStringProperty(dni);
        this.nombre = new SimpleStringProperty(nombre);
        this.apellido1 = new SimpleStringProperty(apellido1);
        this.apellido2 = new SimpleStringProperty(apellido2);
        this.email = new SimpleStringProperty(email);
        this.cargo = new SimpleStringProperty(cargo);
        this.numPacientes = new SimpleIntegerProperty(numPacientes);
        this.activo = new SimpleBooleanProperty(true);
        this.contrasena = "";
        this.telefono1 = "";
        this.telefono2 = "";
    }

    /**
     * Constructor simplificado para crear sanitarios nuevos.
     */
    public Sanitario(String dni, String nombre, String apellido1, String apellido2,
                     String email, String cargo) {
        this(dni, nombre, apellido1, apellido2, email, cargo, 0);
    }

    // ==================== FACTORY METHODS ====================

    /**
     * Crea un Sanitario a partir de la respuesta JSON de la API.
     */
    public static Sanitario desdeSanitarioResponse(SanitarioResponse response) {
        Sanitario s = new Sanitario(
            response.dniSan(),
            response.nombreSan(),
            response.apellido1San(),
            response.apellido2San() != null ? response.apellido2San() : "",
            response.emailSan(),
            response.cargo() != null ? response.cargo() : "",
            response.numDePacientes() != null ? response.numDePacientes() : 0
        );

        s.setActivo(Boolean.TRUE.equals(response.activo()));

        if (response.telefonos() != null && !response.telefonos().isEmpty()) {
            s.setTelefono1(response.telefonos().get(0));
            if (response.telefonos().size() > 1) {
                s.setTelefono2(response.telefonos().get(1));
            }
        }
        return s;
    }

    /**
     * Convierte este Sanitario a un SanitarioRequest para enviar a la API.
     * @param contrasena Contrasena en texto plano (null para no cambiarla en actualizaciones)
     */
    public SanitarioRequest toSanitarioRequest(String contrasena) {
        List<String> telefonos = new ArrayList<>();
        if (telefono1 != null && !telefono1.isEmpty()) {
            telefonos.add(telefono1);
        }
        if (telefono2 != null && !telefono2.isEmpty()) {
            telefonos.add(telefono2);
        }

        return new SanitarioRequest(
            getDni(), getNombre(),
            getApellido1(), getApellido2(),
            getEmail(), contrasena,
            getCargo(), telefonos
        );
    }

    // ==================== GETTERS ====================

    public String getDni() { return dni.get(); }
    public String getNombre() { return nombre.get(); }
    public String getApellido1() { return apellido1.get(); }
    public String getApellido2() { return apellido2.get(); }
    public String getEmail() { return email.get(); }
    public String getCargo() { return cargo.get(); }
    public int getNumPacientes() { return numPacientes.get(); }
    public String getContrasena() { return contrasena; }
    public String getTelefono1() { return telefono1; }
    public String getTelefono2() { return telefono2; }
    public boolean isActivo() { return activo.get(); }

    // ==================== PROPERTY METHODS ====================

    public StringProperty dniProperty() { return dni; }
    public StringProperty nombreProperty() { return nombre; }
    public StringProperty apellido1Property() { return apellido1; }
    public StringProperty apellido2Property() { return apellido2; }

    /**
     * Property para mostrar apellidos completos en TableView.
     */
    public StringProperty apellidosProperty() {
        return new SimpleStringProperty(getApellidos());
    }

    public StringProperty emailProperty() { return email; }
    public StringProperty cargoProperty() { return cargo; }
    public IntegerProperty numPacientesProperty() { return numPacientes; }
    public BooleanProperty activoProperty() { return activo; }

    // ==================== SETTERS ====================

    public void setDni(String dni) { this.dni.set(dni); }
    public void setNombre(String nombre) { this.nombre.set(nombre); }
    public void setApellido1(String apellido1) { this.apellido1.set(apellido1); }
    public void setApellido2(String apellido2) { this.apellido2.set(apellido2); }
    public void setEmail(String email) { this.email.set(email); }
    public void setCargo(String cargo) { this.cargo.set(cargo); }
    public void setNumPacientes(int numPacientes) { this.numPacientes.set(numPacientes); }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }
    public void setTelefono1(String telefono1) { this.telefono1 = telefono1; }
    public void setTelefono2(String telefono2) { this.telefono2 = telefono2; }
    public void setActivo(boolean activo) { this.activo.set(activo); }

    // ==================== METODOS AUXILIARES ====================

    /**
     * Comprueba si el sanitario es medico especialista.
     * Acepta tanto el formato de la API ("SPECIALIST") como el formato legado.
     */
    public boolean esEspecialista() {
        return cargo.get() != null &&
            (cargo.get().equalsIgnoreCase("medico especialista") ||
             cargo.get().equalsIgnoreCase("SPECIALIST"));
    }

    @Override
    public String toString() {
        return "Sanitario{" +
                "dni='" + getDni() + '\'' +
                ", nombre='" + getNombre() + '\'' +
                ", apellidos='" + getApellidos() + '\'' +
                ", cargo='" + getCargo() + '\'' +
                '}';
    }
}
