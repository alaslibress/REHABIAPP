package com.javafx.Clases;

import com.javafx.dto.NivelProgresionResponse;
import javafx.beans.property.*;

/**
 * Modelo JavaFX para los niveles de progresion clinica.
 * Los 4 niveles son: agudo, subagudo, fortalecimiento y funcional.
 */
public class NivelProgresion {

    private final IntegerProperty idNivel;
    private final StringProperty nombre;
    private final IntegerProperty orden;
    private final StringProperty descripcion;

    public NivelProgresion(int idNivel, String nombre, int orden, String descripcion) {
        this.idNivel = new SimpleIntegerProperty(idNivel);
        this.nombre = new SimpleStringProperty(nombre);
        this.orden = new SimpleIntegerProperty(orden);
        this.descripcion = new SimpleStringProperty(descripcion);
    }

    /**
     * Crea un NivelProgresion a partir de la respuesta de la API.
     */
    public static NivelProgresion desdeNivelProgresionResponse(NivelProgresionResponse response) {
        return new NivelProgresion(
            response.idNivel() != null ? response.idNivel() : 0,
            response.nombre() != null ? response.nombre() : "",
            response.orden() != null ? response.orden() : 0,
            response.descripcion() != null ? response.descripcion() : ""
        );
    }

    public int getIdNivel() { return idNivel.get(); }
    public void setIdNivel(int idNivel) { this.idNivel.set(idNivel); }
    public IntegerProperty idNivelProperty() { return idNivel; }

    public String getNombre() { return nombre.get(); }
    public void setNombre(String nombre) { this.nombre.set(nombre); }
    public StringProperty nombreProperty() { return nombre; }

    public int getOrden() { return orden.get(); }
    public void setOrden(int orden) { this.orden.set(orden); }
    public IntegerProperty ordenProperty() { return orden; }

    public String getDescripcion() { return descripcion.get(); }
    public void setDescripcion(String descripcion) { this.descripcion.set(descripcion); }
    public StringProperty descripcionProperty() { return descripcion; }

    /**
     * Devuelve un nombre corto del nivel (abreviatura del nombre completo).
     * La API actual no provee este campo; se genera localmente.
     */
    public String getNombreCorto() {
        String n = nombre.get();
        if (n == null || n.isEmpty()) return "";
        return n.length() > 4 ? n.substring(0, 4).toUpperCase() : n.toUpperCase();
    }

    /**
     * Estado clinico esperado del paciente en este nivel.
     * La API actual no provee este campo; se usa la descripcion como fallback.
     */
    public String getEstadoPaciente() {
        return descripcion.get() != null ? descripcion.get() : "";
    }

    /**
     * Tipos de ejercicio recomendados en este nivel.
     * La API actual no provee este campo.
     */
    public String getTiposEjercicio() {
        return "";
    }

    @Override
    public String toString() {
        return getNombre();
    }
}
