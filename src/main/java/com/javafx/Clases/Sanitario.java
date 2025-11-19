package com.javafx.Clases;

//Clase que representa un sanitario del sistema
public class Sanitario {
    
    //Atributos del sanitario
    private String dni;
    private String nombre;
    private String apellido1;
    private String apellido2;
    private String cargo;
    private String email;
    private String telefono1;
    private String telefono2;
    private int numPacientes;
    
    //Constructor vacio
    public Sanitario() {
    }
    
    //Constructor completo
    public Sanitario(String dni, String nombre, String apellido1, String apellido2, String cargo, 
                    String email, String telefono1, String telefono2, int numPacientes) {
        this.dni = dni;
        this.nombre = nombre;
        this.apellido1 = apellido1;
        this.apellido2 = apellido2;
        this.cargo = cargo;
        this.email = email;
        this.telefono1 = telefono1;
        this.telefono2 = telefono2;
        this.numPacientes = numPacientes;
    }
    
    //Getters y Setters
    public String getDni() {
        return dni;
    }
    
    public void setDni(String dni) {
        this.dni = dni;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    
    public String getApellido1() {
        return apellido1;
    }
    
    public void setApellido1(String apellido1) {
        this.apellido1 = apellido1;
    }
    
    public String getApellido2() {
        return apellido2;
    }
    
    public void setApellido2(String apellido2) {
        this.apellido2 = apellido2;
    }
    
    public String getCargo() {
        return cargo;
    }
    
    public void setCargo(String cargo) {
        this.cargo = cargo;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getTelefono1() {
        return telefono1;
    }
    
    public void setTelefono1(String telefono1) {
        this.telefono1 = telefono1;
    }
    
    public String getTelefono2() {
        return telefono2;
    }
    
    public void setTelefono2(String telefono2) {
        this.telefono2 = telefono2;
    }
    
    public int getNumPacientes() {
        return numPacientes;
    }
    
    public void setNumPacientes(int numPacientes) {
        this.numPacientes = numPacientes;
    }
    
    //Metodo para obtener apellidos completos usado en la tabla
    public String getApellidos() {
        return apellido1 + " " + apellido2;
    }
}
