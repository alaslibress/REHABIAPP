package com.javafx.Clases;

//Clase que representa un paciente del sistema
public class Paciente {
    
    //Atributos del paciente
    private String dni;
    private String nombre;
    private String apellido1;
    private String apellido2;
    private int protesis;
    private int edad;
    private String email;
    private String telefono1;
    private String telefono2;
    private String numSS;
    private String direccion;
    private String estadoPaciente;
    private String discapacidad;
    private String tratamiento;
    
    //Constructor vacio
    public Paciente() {
    }
    
    //Constructor completo
    public Paciente(String dni, String nombre, String apellido1, String apellido2, int protesis,
                   int edad, String email, String telefono1, String telefono2, String numSS,
                   String direccion, String estadoPaciente, String discapacidad, String tratamiento) {
        this.dni = dni;
        this.nombre = nombre;
        this.apellido1 = apellido1;
        this.apellido2 = apellido2;
        this.protesis = protesis;
        this.edad = edad;
        this.email = email;
        this.telefono1 = telefono1;
        this.telefono2 = telefono2;
        this.numSS = numSS;
        this.direccion = direccion;
        this.estadoPaciente = estadoPaciente;
        this.discapacidad = discapacidad;
        this.tratamiento = tratamiento;
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
    
    public int getProtesis() {
        return protesis;
    }
    
    public void setProtesis(int protesis) {
        this.protesis = protesis;
    }
    
    public int getEdad() {
        return edad;
    }
    
    public void setEdad(int edad) {
        this.edad = edad;
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
    
    public String getNumSS() {
        return numSS;
    }
    
    public void setNumSS(String numSS) {
        this.numSS = numSS;
    }
    
    public String getDireccion() {
        return direccion;
    }
    
    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }
    
    public String getEstadoPaciente() {
        return estadoPaciente;
    }
    
    public void setEstadoPaciente(String estadoPaciente) {
        this.estadoPaciente = estadoPaciente;
    }
    
    public String getDiscapacidad() {
        return discapacidad;
    }
    
    public void setDiscapacidad(String discapacidad) {
        this.discapacidad = discapacidad;
    }
    
    public String getTratamiento() {
        return tratamiento;
    }
    
    public void setTratamiento(String tratamiento) {
        this.tratamiento = tratamiento;
    }
    
    //Metodo para obtener apellidos completos usado en la tabla
    public String getApellidos() {
        return apellido1 + " " + apellido2;
    }
}
