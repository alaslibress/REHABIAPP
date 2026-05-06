package com.rehabiapp.api.presentation.controller;

import com.rehabiapp.api.application.dto.PdfMetadatosResponse;
import com.rehabiapp.api.application.service.TratamientoPdfService;
import com.rehabiapp.api.application.service.TratamientoPdfService.PdfBytes;
import com.rehabiapp.api.domain.exception.RecursoNoEncontradoException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;

/**
 * Controlador REST para el PDF asociado a un tratamiento.
 *
 * <p>Limite: 10 MB. Validacion de magic bytes %PDF-. Solo SPECIALIST puede
 * subir o eliminar; cualquier sanitario autenticado puede descargar.</p>
 */
@Tag(name = "Tratamiento PDF", description = "Gestion del PDF de protocolo de un tratamiento")
@RestController
@RequestMapping("/api/tratamientos/{cod}/pdf")
public class TratamientoPdfController {

    private final TratamientoPdfService pdfService;

    public TratamientoPdfController(TratamientoPdfService pdfService) {
        this.pdfService = pdfService;
    }

    @Operation(summary = "Sube el PDF del protocolo del tratamiento")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SPECIALIST')")
    public ResponseEntity<Void> subir(
            @PathVariable String cod,
            @RequestPart("file") MultipartFile file
    ) throws IOException {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (file.getSize() > TratamientoPdfService.MAX_PDF_SIZE_BYTES) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
        }
        byte[] bytes = file.getBytes();
        // Validacion de magic bytes: PDF empieza siempre por "%PDF-"
        if (bytes.length < 5
                || bytes[0] != '%' || bytes[1] != 'P' || bytes[2] != 'D' || bytes[3] != 'F'
                || bytes[4] != '-') {
            return ResponseEntity.badRequest().build();
        }
        pdfService.guardarPdf(cod, bytes, file.getOriginalFilename(), file.getSize());
        return ResponseEntity.created(URI.create("/api/tratamientos/" + cod + "/pdf")).build();
    }

    @Operation(summary = "Descarga el PDF del protocolo del tratamiento")
    @GetMapping(produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> descargar(@PathVariable String cod) {
        PdfBytes pdf = pdfService.obtenerPdf(cod);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + (pdf.nombre() != null ? pdf.nombre() : cod + ".pdf") + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf.bytes());
    }

    @Operation(summary = "Devuelve los metadatos del PDF (nombre y tamano)")
    @GetMapping("/metadatos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PdfMetadatosResponse> metadatos(@PathVariable String cod) {
        return ResponseEntity.ok(pdfService.obtenerMetadatos(cod));
    }

    @Operation(summary = "Elimina el PDF del tratamiento")
    @DeleteMapping
    @PreAuthorize("hasRole('SPECIALIST')")
    public ResponseEntity<Void> eliminar(@PathVariable String cod) {
        pdfService.eliminarPdf(cod);
        return ResponseEntity.noContent().build();
    }
}
