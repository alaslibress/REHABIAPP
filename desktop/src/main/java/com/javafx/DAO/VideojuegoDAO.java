package com.javafx.DAO;

import com.fasterxml.jackson.core.type.TypeReference;
import com.javafx.Clases.ApiClient;
import com.javafx.Clases.Videojuego;
import java.util.List;

/** DAO para consultar videojuegos terapeuticos del catalogo. */
public class VideojuegoDAO {

    private final ApiClient apiClient = ApiClient.getInstancia();

    /** Lista todos los videojuegos activos. */
    public List<Videojuego> listarTodos() {
        return apiClient.get("/api/videojuegos", new TypeReference<List<Videojuego>>() {});
    }

    /** Lista los videojuegos activos de una discapacidad concreta. */
    public List<Videojuego> listarPorDiscapacidad(String codDis) {
        return apiClient.get("/api/videojuegos/discapacidad/" + codDis,
                new TypeReference<List<Videojuego>>() {});
    }
}
