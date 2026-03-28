package com.rehabiapp.api.application.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * DTO de respuesta paginada genérico para todos los endpoints de listado.
 *
 * <p>Envuelve el objeto Page de Spring Data en un record serializable
 * sin exponer las clases internas de Spring al cliente.</p>
 *
 * <p>El método de fábrica "de" construye el PageResponse a partir
 * de cualquier Page de Spring Data con un solo llamado.</p>
 *
 * @param <T> Tipo de los elementos contenidos en la página.
 */
public record PageResponse<T>(
        List<T> contenido,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas,
        boolean ultima
) {

    /**
     * Construye un PageResponse a partir de un Page de Spring Data.
     *
     * @param page Página de Spring Data con los elementos y metadatos.
     * @param <T>  Tipo de los elementos.
     * @return PageResponse con los datos de la página.
     */
    public static <T> PageResponse<T> de(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
