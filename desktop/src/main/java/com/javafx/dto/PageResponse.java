package com.javafx.dto;

import java.util.List;

public record PageResponse<T>(
    List<T> contenido,
    int pagina,
    int tamano,
    long totalElementos,
    int totalPaginas,
    boolean ultima
) {}
