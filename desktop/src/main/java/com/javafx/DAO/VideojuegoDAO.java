package com.javafx.DAO;

import com.fasterxml.jackson.core.type.TypeReference;
import com.javafx.Clases.ApiClient;
import com.javafx.Clases.Videojuego;
import com.javafx.Clases.VideojuegoRequest;
import com.javafx.dto.PageResponse;
import java.util.List;

/** DAO para consultar y gestionar videojuegos terapeuticos del catalogo. */
public class VideojuegoDAO {

    private final ApiClient apiClient = ApiClient.getInstancia();

    /**
     * Lista todos los videojuegos (hasta 200 registros, paginacion del API).
     * Extrae el contenido del PageResponse y lo devuelve como lista plana.
     */
    public List<Videojuego> listarTodos() {
        PageResponse<Videojuego> pagina = apiClient.get(
                "/api/videojuegos?size=200",
                new TypeReference<PageResponse<Videojuego>>() {});
        return pagina != null && pagina.contenido() != null ? pagina.contenido() : List.of();
    }

    /** Lista los videojuegos activos de una discapacidad concreta. */
    public List<Videojuego> listarPorDiscapacidad(String codDis) {
        return apiClient.get("/api/videojuegos/discapacidad/" + codDis,
                new TypeReference<List<Videojuego>>() {});
    }

    /** Obtiene un videojuego por su id. */
    public Videojuego obtenerPorId(Long idVideojuego) {
        return apiClient.get("/api/videojuegos/" + idVideojuego, Videojuego.class);
    }

    /** Crea un videojuego. Requiere rol SPECIALIST. */
    public Videojuego crear(VideojuegoRequest request) {
        return apiClient.post("/api/videojuegos", request, Videojuego.class);
    }

    /** Actualiza un videojuego existente. Requiere rol SPECIALIST. */
    public Videojuego actualizar(Long idVideojuego, VideojuegoRequest request) {
        return apiClient.put("/api/videojuegos/" + idVideojuego, request, Videojuego.class);
    }

    /** Soft delete (activo=false en el API). Requiere rol SPECIALIST. */
    public void eliminar(Long idVideojuego) {
        apiClient.delete("/api/videojuegos/" + idVideojuego);
    }
}
