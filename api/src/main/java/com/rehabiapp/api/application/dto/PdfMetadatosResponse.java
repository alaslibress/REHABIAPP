package com.rehabiapp.api.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Metadatos del PDF asociado a un tratamiento.
 */
@Schema(description = "Metadatos del PDF asociado a un tratamiento")
public record PdfMetadatosResponse(
        @Schema(description = "Codigo del tratamiento", example = "TRAT-001")
        String codTrat,
        @Schema(description = "Nombre original del fichero PDF")
        String nombre,
        @Schema(description = "Tamano del PDF en bytes")
        Long tamano
) {}
