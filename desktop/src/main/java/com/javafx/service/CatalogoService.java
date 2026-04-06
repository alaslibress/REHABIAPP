package com.javafx.service;

import com.javafx.Clases.Discapacidad;
import com.javafx.Clases.NivelProgresion;
import com.javafx.Clases.Tratamiento;
import com.javafx.DAO.CatalogoDAO;

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
}
