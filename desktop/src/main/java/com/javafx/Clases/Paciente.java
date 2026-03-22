package com.javafx.Clases;

import javafx.beans.property.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Clase modelo que representa un Paciente en el sistema
 * Utiliza propiedades JavaFX para facilitar el binding con TableView
 */
public class Paciente implements Persona {

    //Propiedades principales del paciente
    private final StringProperty dni;
    private final StringProperty nombre;
    private final StringProperty apellido1;
    private final StringProperty apellido2;
    private final IntegerProperty edad;
    private final StringProperty email;
    private final StringProperty numSS;
    private final StringProperty discapacidad;
    private final StringProperty tratamiento;
    private final StringProperty estadoTratamiento;
    private final IntegerProperty protesis;
    private final StringProperty dniSanitario;

    //Propiedades de contacto
    private final StringProperty telefono1;
    private final StringProperty telefono2;

    //Propiedades de direccion
    private final StringProperty calle;
    private final StringProperty numero;
    private final StringProperty piso;
    private final StringProperty codigoPostal;
    private final StringProperty localidad;
    private final StringProperty provincia;

    //Propiedades clinicas y legales (RGPD, Ley 41/2002)
    private final StringProperty sexo;
    private final ObjectProperty<LocalDate> fechaNacimiento;
    private final StringProperty alergias;
    private final StringProperty antecedentes;
    private final StringProperty medicacionActual;
    private final BooleanProperty consentimientoRgpd;
    private final ObjectProperty<LocalDateTime> fechaConsentimiento;
    private final BooleanProperty activo;

    /**
     * Constructor completo con todos los campos principales
     */
    public Paciente(String dni, String nombre, String apellido1, String apellido2,
                    int edad, String email, String numSS, String discapacidad,
                    String tratamiento, String estadoTratamiento, int protesis,
                    String dniSanitario) {

        this.dni = new SimpleStringProperty(dni);
        this.nombre = new SimpleStringProperty(nombre);
        this.apellido1 = new SimpleStringProperty(apellido1);
        this.apellido2 = new SimpleStringProperty(apellido2);
        this.edad = new SimpleIntegerProperty(edad);
        this.email = new SimpleStringProperty(email);
        this.numSS = new SimpleStringProperty(numSS);
        this.discapacidad = new SimpleStringProperty(discapacidad);
        this.tratamiento = new SimpleStringProperty(tratamiento);
        this.estadoTratamiento = new SimpleStringProperty(estadoTratamiento);
        this.protesis = new SimpleIntegerProperty(protesis);
        this.dniSanitario = new SimpleStringProperty(dniSanitario);

        //Inicializar propiedades de contacto
        this.telefono1 = new SimpleStringProperty("");
        this.telefono2 = new SimpleStringProperty("");

        //Inicializar propiedades de direccion
        this.calle = new SimpleStringProperty("");
        this.numero = new SimpleStringProperty("");
        this.piso = new SimpleStringProperty("");
        this.codigoPostal = new SimpleStringProperty("");
        this.localidad = new SimpleStringProperty("");
        this.provincia = new SimpleStringProperty("");

        //Inicializar propiedades clinicas y legales con valores por defecto
        this.sexo = new SimpleStringProperty("");
        this.fechaNacimiento = new SimpleObjectProperty<>(null);
        this.alergias = new SimpleStringProperty("");
        this.antecedentes = new SimpleStringProperty("");
        this.medicacionActual = new SimpleStringProperty("");
        this.consentimientoRgpd = new SimpleBooleanProperty(false);
        this.fechaConsentimiento = new SimpleObjectProperty<>(null);
        this.activo = new SimpleBooleanProperty(true);
    }

    /**
     * Constructor simplificado sin campos opcionales
     */
    public Paciente(String dni, String nombre, String apellido1, String apellido2,
                    int edad, String email, String numSS, String dniSanitario) {

        this(dni, nombre, apellido1, apellido2, edad, email, numSS,
                "", "", "", 0, dniSanitario);
    }

    // ==================== GETTERS Y SETTERS ====================

    //DNI
    public String getDni() {
        return dni.get();
    }

    public void setDni(String dni) {
        this.dni.set(dni);
    }

    public StringProperty dniProperty() {
        return dni;
    }

    //Nombre
    public String getNombre() {
        return nombre.get();
    }

    public void setNombre(String nombre) {
        this.nombre.set(nombre);
    }

    public StringProperty nombreProperty() {
        return nombre;
    }

    //Apellido 1
    public String getApellido1() {
        return apellido1.get();
    }

    public void setApellido1(String apellido1) {
        this.apellido1.set(apellido1);
    }

    public StringProperty apellido1Property() {
        return apellido1;
    }

    //Apellido 2
    public String getApellido2() {
        return apellido2.get();
    }

    public void setApellido2(String apellido2) {
        this.apellido2.set(apellido2);
    }

    public StringProperty apellido2Property() {
        return apellido2;
    }

    /**
     * Property para los apellidos concatenados (para TableView)
     * Usa el metodo getApellidos() heredado de la interface Persona
     */
    public StringProperty apellidosProperty() {
        return new SimpleStringProperty(getApellidos());
    }

    //Edad
    public int getEdad() {
        return edad.get();
    }

    public void setEdad(int edad) {
        this.edad.set(edad);
    }

    public IntegerProperty edadProperty() {
        return edad;
    }

    //Email
    public String getEmail() {
        return email.get();
    }

    public void setEmail(String email) {
        this.email.set(email);
    }

    public StringProperty emailProperty() {
        return email;
    }

    //Numero de Seguridad Social
    public String getNumSS() {
        return numSS.get();
    }

    public void setNumSS(String numSS) {
        this.numSS.set(numSS);
    }

    public StringProperty numSSProperty() {
        return numSS;
    }

    //Discapacidad
    public String getDiscapacidad() {
        return discapacidad.get();
    }

    public void setDiscapacidad(String discapacidad) {
        this.discapacidad.set(discapacidad);
    }

    public StringProperty discapacidadProperty() {
        return discapacidad;
    }

    //Tratamiento
    public String getTratamiento() {
        return tratamiento.get();
    }

    public void setTratamiento(String tratamiento) {
        this.tratamiento.set(tratamiento);
    }

    public StringProperty tratamientoProperty() {
        return tratamiento;
    }

    //Estado del tratamiento
    public String getEstadoTratamiento() {
        return estadoTratamiento.get();
    }

    public void setEstadoTratamiento(String estadoTratamiento) {
        this.estadoTratamiento.set(estadoTratamiento);
    }

    public StringProperty estadoTratamientoProperty() {
        return estadoTratamiento;
    }

    //Protesis
    public int getProtesis() {
        return protesis.get();
    }

    public void setProtesis(int protesis) {
        this.protesis.set(protesis);
    }

    public IntegerProperty protesisProperty() {
        return protesis;
    }

    /**
     * Indica si el paciente tiene protesis
     * @return true si tiene al menos una protesis
     */
    public boolean tieneProtesis() {
        return protesis.get() > 0;
    }

    //DNI del sanitario asignado
    public String getDniSanitario() {
        return dniSanitario.get();
    }

    public void setDniSanitario(String dniSanitario) {
        this.dniSanitario.set(dniSanitario);
    }

    public StringProperty dniSanitarioProperty() {
        return dniSanitario;
    }

    //Telefono 1
    public String getTelefono1() {
        return telefono1.get();
    }

    public void setTelefono1(String telefono1) {
        this.telefono1.set(telefono1);
    }

    public StringProperty telefono1Property() {
        return telefono1;
    }

    //Telefono 2
    public String getTelefono2() {
        return telefono2.get();
    }

    public void setTelefono2(String telefono2) {
        this.telefono2.set(telefono2);
    }

    public StringProperty telefono2Property() {
        return telefono2;
    }

    //Calle
    public String getCalle() {
        return calle.get();
    }

    public void setCalle(String calle) {
        this.calle.set(calle);
    }

    public StringProperty calleProperty() {
        return calle;
    }

    //Numero
    public String getNumero() {
        return numero.get();
    }

    public void setNumero(String numero) {
        this.numero.set(numero);
    }

    public StringProperty numeroProperty() {
        return numero;
    }

    //Piso
    public String getPiso() {
        return piso.get();
    }

    public void setPiso(String piso) {
        this.piso.set(piso);
    }

    public StringProperty pisoProperty() {
        return piso;
    }

    //Codigo Postal
    public String getCodigoPostal() {
        return codigoPostal.get();
    }

    public void setCodigoPostal(String codigoPostal) {
        this.codigoPostal.set(codigoPostal);
    }

    public StringProperty codigoPostalProperty() {
        return codigoPostal;
    }

    //Localidad
    public String getLocalidad() {
        return localidad.get();
    }

    public void setLocalidad(String localidad) {
        this.localidad.set(localidad);
    }

    public StringProperty localidadProperty() {
        return localidad;
    }

    //Provincia
    public String getProvincia() {
        return provincia.get();
    }

    public void setProvincia(String provincia) {
        this.provincia.set(provincia);
    }

    public StringProperty provinciaProperty() {
        return provincia;
    }

    /**
     * Obtiene la direccion completa formateada
     * @return Direccion completa como texto
     */
    public String getDireccionCompleta() {
        StringBuilder sb = new StringBuilder();

        if (calle.get() != null && !calle.get().isEmpty()) {
            sb.append(calle.get());
        }
        if (numero.get() != null && !numero.get().isEmpty()) {
            sb.append(" ").append(numero.get());
        }
        if (piso.get() != null && !piso.get().isEmpty()) {
            sb.append(", ").append(piso.get());
        }
        if (codigoPostal.get() != null && !codigoPostal.get().isEmpty()) {
            sb.append(", ").append(codigoPostal.get());
        }
        if (localidad.get() != null && !localidad.get().isEmpty()) {
            sb.append(" ").append(localidad.get());
        }
        if (provincia.get() != null && !provincia.get().isEmpty()) {
            sb.append(" (").append(provincia.get()).append(")");
        }

        return sb.toString();
    }

    // ==================== CAMPOS CLINICOS Y LEGALES ====================

    //Sexo
    public String getSexo() {
        return sexo.get();
    }

    public void setSexo(String sexo) {
        this.sexo.set(sexo);
    }

    public StringProperty sexoProperty() {
        return sexo;
    }

    //Fecha de nacimiento
    public LocalDate getFechaNacimiento() {
        return fechaNacimiento.get();
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento.set(fechaNacimiento);
    }

    public ObjectProperty<LocalDate> fechaNacimientoProperty() {
        return fechaNacimiento;
    }

    //Alergias
    public String getAlergias() {
        return alergias.get();
    }

    public void setAlergias(String alergias) {
        this.alergias.set(alergias);
    }

    public StringProperty alergiasProperty() {
        return alergias;
    }

    //Antecedentes
    public String getAntecedentes() {
        return antecedentes.get();
    }

    public void setAntecedentes(String antecedentes) {
        this.antecedentes.set(antecedentes);
    }

    public StringProperty antecedentesProperty() {
        return antecedentes;
    }

    //Medicacion actual
    public String getMedicacionActual() {
        return medicacionActual.get();
    }

    public void setMedicacionActual(String medicacionActual) {
        this.medicacionActual.set(medicacionActual);
    }

    public StringProperty medicacionActualProperty() {
        return medicacionActual;
    }

    //Consentimiento RGPD
    public boolean isConsentimientoRgpd() {
        return consentimientoRgpd.get();
    }

    public void setConsentimientoRgpd(boolean consentimientoRgpd) {
        this.consentimientoRgpd.set(consentimientoRgpd);
    }

    public BooleanProperty consentimientoRgpdProperty() {
        return consentimientoRgpd;
    }

    //Fecha de consentimiento
    public LocalDateTime getFechaConsentimiento() {
        return fechaConsentimiento.get();
    }

    public void setFechaConsentimiento(LocalDateTime fechaConsentimiento) {
        this.fechaConsentimiento.set(fechaConsentimiento);
    }

    public ObjectProperty<LocalDateTime> fechaConsentimientoProperty() {
        return fechaConsentimiento;
    }

    //Activo (soft delete)
    public boolean isActivo() {
        return activo.get();
    }

    public void setActivo(boolean activo) {
        this.activo.set(activo);
    }

    public BooleanProperty activoProperty() {
        return activo;
    }

    // ==================== METODOS AUXILIARES ====================

    @Override
    public String toString() {
        return "Paciente{" +
                "dni='" + getDni() + '\'' +
                ", nombre='" + getNombre() + '\'' +
                ", apellidos='" + getApellidos() + '\'' +
                ", edad=" + getEdad() +
                ", email='" + getEmail() + '\'' +
                ", numSS='" + getNumSS() + '\'' +
                ", dniSanitario='" + getDniSanitario() + '\'' +
                '}';
    }
}