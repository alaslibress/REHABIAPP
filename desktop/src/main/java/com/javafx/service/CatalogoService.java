package com.javafx.service;

import com.javafx.Clases.Discapacidad;
import com.javafx.Clases.NivelProgresion;
import com.javafx.Clases.Tratamiento;
import com.javafx.DAO.CatalogoDAO;
import com.javafx.dto.DiscapacidadRequest;
import com.javafx.dto.TratamientoRequest;

import java.util.List;

/**
 * Capa de servicio para el catalogo clinico (discapacidades, tratamientos, niveles).
 * Delega al CatalogoDAO que consume la API REST (solo lectura).
 */
public class CatalogoService {

    private final CatalogoDAO catalogoDAO = new CatalogoDAO();

    public List<Discapacidad> listarDiscapacidades() {
        return catalogoDAO.listarDiscapacidades();
    }

    public Discapacidad obtenerDiscapacidad(String codDis) {
        return catalogoDAO.obtenerDiscapacidad(codDis);
    }

    public List<Tratamiento> listarTratamientos() {
        return catalogoDAO.listarTratamientos();
    }

    public Tratamiento obtenerTratamiento(String codTrat) {
        return catalogoDAO.obtenerTratamiento(codTrat);
    }

    public List<Tratamiento> listarTratamientosPorDiscapacidad(String codDis) {
        return catalogoDAO.listarTratamientosPorDiscapacidad(codDis);
    }

    public List<NivelProgresion> listarNivelesProgresion() {
        return catalogoDAO.listarNivelesProgresion();
    }

    /**
     * Alias para compatibilidad con el controlador de ficha de paciente.
     */
    public List<NivelProgresion> listarNiveles() {
        return listarNivelesProgresion();
    }

    // ==================== DISCAPACIDADES CRUD ====================

    public Discapacidad crearDiscapacidad(DiscapacidadRequest request) {
        return catalogoDAO.crearDiscapacidad(request);
    }

    public Discapacidad actualizarDiscapacidad(String codDis, DiscapacidadRequest request) {
        return catalogoDAO.actualizarDiscapacidad(codDis, request);
    }

    public void eliminarDiscapacidad(String codDis) {
        catalogoDAO.eliminarDiscapacidad(codDis);
    }

    // ==================== TRATAMIENTOS CRUD ====================

    public Tratamiento crearTratamiento(TratamientoRequest request) {
        return catalogoDAO.crearTratamiento(request);
    }

    public Tratamiento actualizarTratamiento(String codTrat, TratamientoRequest request) {
        return catalogoDAO.actualizarTratamiento(codTrat, request);
    }

    public void eliminarTratamiento(String codTrat) {
        catalogoDAO.eliminarTratamiento(codTrat);
    }

    // ==================== VINCULOS TRATAMIENTO-DISCAPACIDAD ====================

    public void vincularTratamientoDiscapacidad(String codTrat, String codDis) {
        catalogoDAO.vincularTratamientoDiscapacidad(codTrat, codDis);
    }

    public void desvincularTratamientoDiscapacidad(String codTrat, String codDis) {
        catalogoDAO.desvincularTratamientoDiscapacidad(codTrat, codDis);
    }

    public List<Discapacidad> listarDiscapacidadesDeTratamiento(String codTrat) {
        return catalogoDAO.listarDiscapacidadesDeTratamiento(codTrat);
    }
}
