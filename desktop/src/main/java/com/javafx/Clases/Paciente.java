package com.javafx.Clases;

import com.javafx.dto.PacienteRequest;
import com.javafx.dto.PacienteResponse;
import javafx.beans.property.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase modelo que representa un Paciente en el sistema.
 * Utiliza propiedades JavaFX para facilitar el binding con TableView.
 * Los campos legacy discapacidad, tratamiento y estadoTratamiento han sido eliminados.
 * El cifrado clinico es responsabilidad de la API (CampoClinicoConverter).
 */
public class Paciente implements Persona {

    // Propiedades principales del paciente
    private final StringProperty dni;
    private final StringProperty nombre;
    private final StringProperty apellido1;
    private final StringProperty apellido2;
    private final IntegerProperty edad;
    private final StringProperty email;
    private final StringProperty numSS;
    private final BooleanProperty protesis;
    private final StringProperty dniSanitario;

    // Propiedades de contacto
    private final StringProperty telefono1;
    private final StringProperty telefono2;

    // Propiedades clinicas y legales (RGPD, Ley 41/2002)
    private final StringProperty sexo;
    private final ObjectProperty<LocalDate> fechaNacimiento;
    private final StringProperty alergias;
    private final StringProperty antecedentes;
    private final StringProperty medicacionActual;
    private final BooleanProperty consentimientoRgpd;
    private final ObjectProperty<LocalDateTime> fechaConsentimiento;
    private final BooleanProperty activo;

    /**
     * Constructor completo (sin campos legacy).
     */
    public Paciente(String dni, String nombre, String apellido1, String apellido2,
                    int edad, String email, String numSS, boolean protesis,
                    String dniSanitario) {

        this.dni = new SimpleStringProperty(dni);
        this.nombre = new SimpleStringProperty(nombre);
        this.apellido1 = new SimpleStringProperty(apellido1);
        this.apellido2 = new SimpleStringProperty(apellido2);
        this.edad = new SimpleIntegerProperty(edad);
        this.email = new SimpleStringProperty(email);
        this.numSS = new SimpleStringProperty(numSS);
        this.protesis = new SimpleBooleanProperty(protesis);
        this.dniSanitario = new SimpleStringProperty(dniSanitario);

        // Inicializar propiedades de contacto
        this.telefono1 = new SimpleStringProperty("");
        this.telefono2 = new SimpleStringProperty("");

        // Inicializar propiedades clinicas y legales con valores por defecto
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
     * Constructor simplificado sin campos opcionales.
     */
    public Paciente(String dni, String nombre, String apellido1, String apellido2,
                    int edad, String email, String numSS, String dniSanitario) {
        this(dni, nombre, apellido1, apellido2, edad, email, numSS, false, dniSanitario);
    }

    // ==================== FACTORY METHODS ====================

    /**
     * Crea un Paciente a partir de la respuesta JSON de la API.
     * La API devuelve los campos clinicos ya descifrados.
     */
    public static Paciente desdePacienteResponse(PacienteResponse response) {
        Paciente p = new Paciente(
            response.dniPac(),
            response.nombrePac(),
            response.apellido1Pac(),
            response.apellido2Pac() != null ? response.apellido2Pac() : "",
            response.edadPac() != null ? response.edadPac() : 0,
            response.emailPac(),
            response.numSs(),
            Boolean.TRUE.equals(response.protesis()),
            response.dniSan()
        );

        p.setSexo(response.sexo() != null ? response.sexo() : "");
        p.setFechaNacimiento(response.fechaNacimiento());
        p.setAlergias(response.alergias() != null ? response.alergias() : "");
        p.setAntecedentes(response.antecedentes() != null ? response.antecedentes() : "");
        p.setMedicacionActual(response.medicacionActual() != null ? response.medicacionActual() : "");
        p.setConsentimientoRgpd(Boolean.TRUE.equals(response.consentimientoRgpd()));
        p.setActivo(Boolean.TRUE.equals(response.activo()));

        if (response.telefonos() != null && !response.telefonos().isEmpty()) {
            p.setTelefono1(response.telefonos().get(0));
            if (response.telefonos().size() > 1) {
                p.setTelefono2(response.telefonos().get(1));
            }
        }
        return p;
    }

    /**
     * Convierte este Paciente a un PacienteRequest para enviar a la API.
     */
    public PacienteRequest toPacienteRequest() {
        List<String> telefonos = new ArrayList<>();
        if (getTelefono1() != null && !getTelefono1().isEmpty()) {
            telefonos.add(getTelefono1());
        }
        if (getTelefono2() != null && !getTelefono2().isEmpty()) {
            telefonos.add(getTelefono2());
        }

        return new PacienteRequest(
            getDni(), getDniSanitario(), getNombre(),
            getApellido1(), getApellido2(),
            getEdad(), getEmail(), getNumSS(),
            getSexo(), getFechaNacimiento(),
            isProtesis(),
            getAlergias(), getAntecedentes(), getMedicacionActual(),
            isConsentimientoRgpd(),
            telefonos
        );
    }

    // ==================== GETTERS Y SETTERS ====================

    public String getDni() { return dni.get(); }
    public void setDni(String dni) { this.dni.set(dni); }
    public StringProperty dniProperty() { return dni; }

    public String getNombre() { return nombre.get(); }
    public void setNombre(String nombre) { this.nombre.set(nombre); }
    public StringProperty nombreProperty() { return nombre; }

    public String getApellido1() { return apellido1.get(); }
    public void setApellido1(String apellido1) { this.apellido1.set(apellido1); }
    public StringProperty apellido1Property() { return apellido1; }

    public String getApellido2() { return apellido2.get(); }
    public void setApellido2(String apellido2) { this.apellido2.set(apellido2); }
    public StringProperty apellido2Property() { return apellido2; }

    /**
     * Property para los apellidos concatenados (para TableView).
     */
    public StringProperty apellidosProperty() {
        return new SimpleStringProperty(getApellidos());
    }

    public int getEdad() { return edad.get(); }
    public void setEdad(int edad) { this.edad.set(edad); }
    public IntegerProperty edadProperty() { return edad; }

    public String getEmail() { return email.get(); }
    public void setEmail(String email) { this.email.set(email); }
    public StringProperty emailProperty() { return email; }

    public String getNumSS() { return numSS.get(); }
    public void setNumSS(String numSS) { this.numSS.set(numSS); }
    public StringProperty numSSProperty() { return numSS; }

    public boolean isProtesis() { return protesis.get(); }
    public void setProtesis(boolean protesis) { this.protesis.set(protesis); }
    public BooleanProperty protesisProperty() { return protesis; }

    /**
     * Indica si el paciente tiene protesis (alias de isProtesis para compatibilidad).
     */
    public boolean tieneProtesis() { return protesis.get(); }

    public String getDniSanitario() { return dniSanitario.get(); }
    public void setDniSanitario(String dniSanitario) { this.dniSanitario.set(dniSanitario); }
    public StringProperty dniSanitarioProperty() { return dniSanitario; }

    public String getTelefono1() { return telefono1.get(); }
    public void setTelefono1(String telefono1) { this.telefono1.set(telefono1); }
    public StringProperty telefono1Property() { return telefono1; }

    public String getTelefono2() { return telefono2.get(); }
    public void setTelefono2(String telefono2) { this.telefono2.set(telefono2); }
    public StringProperty telefono2Property() { return telefono2; }

    // ==================== CAMPOS CLINICOS Y LEGALES ====================

    public String getSexo() { return sexo.get(); }
    public void setSexo(String sexo) { this.sexo.set(sexo); }
    public StringProperty sexoProperty() { return sexo; }

    public LocalDate getFechaNacimiento() { return fechaNacimiento.get(); }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento.set(fechaNacimiento); }
    public ObjectProperty<LocalDate> fechaNacimientoProperty() { return fechaNacimiento; }

    public String getAlergias() { return alergias.get(); }
    public void setAlergias(String alergias) { this.alergias.set(alergias); }
    public StringProperty alergiasProperty() { return alergias; }

    public String getAntecedentes() { return antecedentes.get(); }
    public void setAntecedentes(String antecedentes) { this.antecedentes.set(antecedentes); }
    public StringProperty antecedentesProperty() { return antecedentes; }

    public String getMedicacionActual() { return medicacionActual.get(); }
    public void setMedicacionActual(String medicacionActual) { this.medicacionActual.set(medicacionActual); }
    public StringProperty medicacionActualProperty() { return medicacionActual; }

    public boolean isConsentimientoRgpd() { return consentimientoRgpd.get(); }
    public void setConsentimientoRgpd(boolean consentimientoRgpd) { this.consentimientoRgpd.set(consentimientoRgpd); }
    public BooleanProperty consentimientoRgpdProperty() { return consentimientoRgpd; }

    public LocalDateTime getFechaConsentimiento() { return fechaConsentimiento.get(); }
    public void setFechaConsentimiento(LocalDateTime fechaConsentimiento) { this.fechaConsentimiento.set(fechaConsentimiento); }
    public ObjectProperty<LocalDateTime> fechaConsentimientoProperty() { return fechaConsentimiento; }

    public boolean isActivo() { return activo.get(); }
    public void setActivo(boolean activo) { this.activo.set(activo); }
    public BooleanProperty activoProperty() { return activo; }

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
