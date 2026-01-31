package com.javafx.Clases;

import javafx.beans.property.*;

/**
 * Clase modelo que representa un sanitario del sistema
 * Corresponde a las tablas: sanitario, sanitario_agrega_sanitario y telefono_sanitario
 */
public class Sanitario implements Persona {

    //Propiedades observables para enlazar con TableView de JavaFX
    private final StringProperty dni;
    private final StringProperty nombre;
    private final StringProperty apellido1;
    private final StringProperty apellido2;
    private final StringProperty email;
    private final StringProperty cargo;
    private final IntegerProperty numPacientes;

    //Atributos simples que no necesitan ser observables
    private String contrasena;
    private String telefono1;
    private String telefono2;

    /**
     * Constructor completo con todos los campos principales
     * @param dni DNI del sanitario (9 caracteres)
     * @param nombre Nombre del sanitario
     * @param apellido1 Primer apellido del sanitario
     * @param apellido2 Segundo apellido del sanitario
     * @param email Correo electronico del sanitario
     * @param cargo Cargo del sanitario (medico especialista o enfermero)
     * @param numPacientes Numero de pacientes asignados
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
        this.contrasena = "";
        this.telefono1 = "";
        this.telefono2 = "";
    }

    /**
     * Constructor simplificado para crear sanitarios nuevos
     * @param dni DNI del sanitario
     * @param nombre Nombre del sanitario
     * @param apellido1 Primer apellido
     * @param apellido2 Segundo apellido
     * @param email Correo electronico
     * @param cargo Cargo del sanitario
     */
    public Sanitario(String dni, String nombre, String apellido1, String apellido2,
                     String email, String cargo) {
        this(dni, nombre, apellido1, apellido2, email, cargo, 0);
    }

    // ==================== GETTERS ====================

    public String getDni() {
        return dni.get();
    }

    public String getNombre() {
        return nombre.get();
    }

    public String getApellido1() {
        return apellido1.get();
    }

    public String getApellido2() {
        return apellido2.get();
    }

    public String getEmail() {
        return email.get();
    }

    public String getCargo() {
        return cargo.get();
    }

    public int getNumPacientes() {
        return numPacientes.get();
    }

    public String getContrasena() {
        return contrasena;
    }

    public String getTelefono1() {
        return telefono1;
    }

    public String getTelefono2() {
        return telefono2;
    }

    // ==================== PROPERTY METHODS ====================
    // Estos metodos son necesarios para que TableView pueda enlazar las columnas

    public StringProperty dniProperty() {
        return dni;
    }

    public StringProperty nombreProperty() {
        return nombre;
    }

    public StringProperty apellido1Property() {
        return apellido1;
    }

    public StringProperty apellido2Property() {
        return apellido2;
    }

    /**
     * Property para mostrar apellidos completos en TableView
     * @return StringProperty con ambos apellidos concatenados
     */
    public StringProperty apellidosProperty() {
        return new SimpleStringProperty(getApellidos());
    }

    public StringProperty emailProperty() {
        return email;
    }

    public StringProperty cargoProperty() {
        return cargo;
    }

    public IntegerProperty numPacientesProperty() {
        return numPacientes;
    }

    // ==================== SETTERS ====================

    public void setDni(String dni) {
        this.dni.set(dni);
    }

    public void setNombre(String nombre) {
        this.nombre.set(nombre);
    }

    public void setApellido1(String apellido1) {
        this.apellido1.set(apellido1);
    }

    public void setApellido2(String apellido2) {
        this.apellido2.set(apellido2);
    }

    public void setEmail(String email) {
        this.email.set(email);
    }

    public void setCargo(String cargo) {
        this.cargo.set(cargo);
    }

    public void setNumPacientes(int numPacientes) {
        this.numPacientes.set(numPacientes);
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public void setTelefono1(String telefono1) {
        this.telefono1 = telefono1;
    }

    public void setTelefono2(String telefono2) {
        this.telefono2 = telefono2;
    }

    // ==================== METODOS AUXILIARES ====================

    /**
     * Comprueba si el sanitario es medico especialista
     * @return true si es medico especialista, false si es enfermero
     */
    public boolean esEspecialista() {
        return cargo.get() != null && cargo.get().equalsIgnoreCase("medico especialista");
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