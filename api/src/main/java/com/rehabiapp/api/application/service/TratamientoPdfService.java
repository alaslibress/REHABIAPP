package com.rehabiapp.api.application.service;

import com.rehabiapp.api.application.dto.PdfMetadatosResponse;
import com.rehabiapp.api.domain.entity.Tratamiento;
import com.rehabiapp.api.domain.enums.AccionAuditoria;
import com.rehabiapp.api.domain.exception.RecursoNoEncontradoException;
import com.rehabiapp.api.domain.repository.TratamientoRepository;
import com.rehabiapp.api.infrastructure.audit.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicacion para la gestion del PDF asociado a un tratamiento.
 *
 * <p>El PDF se almacena directamente como BYTEA en la tabla tratamiento.
 * Limite por base de datos: 10 MB (chk_tamano_pdf en V13). Cada operacion
 * (subida, descarga, eliminacion) registra una entrada de auditoria.</p>
 */
@Service
@Transactional
public class TratamientoPdfService {

    /** Tamano maximo permitido para el PDF: 10 MB. */
    public static final long MAX_PDF_SIZE_BYTES = 10L * 1024L * 1024L;

    private final TratamientoRepository tratamientoRepository;
    private final AuditService auditService;

    public TratamientoPdfService(TratamientoRepository tratamientoRepository,
                                 AuditService auditService) {
        this.tratamientoRepository = tratamientoRepository;
        this.auditService = auditService;
    }

    public void guardarPdf(String codTrat, byte[] bytes, String filename, long size) {
        Tratamiento t = tratamientoRepository.findById(codTrat)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Tratamiento no encontrado: " + codTrat));
        t.setArchivoPdf(bytes);
        t.setNombreArchivoPdf(filename);
        t.setTamanoPdfBytes(size);
        tratamientoRepository.save(t);

        auditService.registrar(AccionAuditoria.UPDATE, "Tratamiento", codTrat,
                "PDF subido: " + filename + " (" + size + " bytes)");
    }

    @Transactional(readOnly = true)
    public PdfBytes obtenerPdf(String codTrat) {
        Tratamiento t = tratamientoRepository.findById(codTrat)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Tratamiento no encontrado: " + codTrat));
        if (t.getArchivoPdf() == null) {
            throw new RecursoNoEncontradoException("El tratamiento no tiene PDF asociado: " + codTrat);
        }
        auditService.registrar(AccionAuditoria.READ, "Tratamiento", codTrat,
                "Descarga de PDF: " + t.getNombreArchivoPdf());
        return new PdfBytes(t.getArchivoPdf(), t.getNombreArchivoPdf(), t.getTamanoPdfBytes());
    }

    @Transactional(readOnly = true)
    public PdfMetadatosResponse obtenerMetadatos(String codTrat) {
        Tratamiento t = tratamientoRepository.findById(codTrat)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Tratamiento no encontrado: " + codTrat));
        if (t.getArchivoPdf() == null) {
            throw new RecursoNoEncontradoException("El tratamiento no tiene PDF asociado: " + codTrat);
        }
        return new PdfMetadatosResponse(codTrat, t.getNombreArchivoPdf(), t.getTamanoPdfBytes());
    }

    public void eliminarPdf(String codTrat) {
        Tratamiento t = tratamientoRepository.findById(codTrat)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Tratamiento no encontrado: " + codTrat));
        if (t.getArchivoPdf() == null) {
            throw new RecursoNoEncontradoException("El tratamiento no tiene PDF asociado: " + codTrat);
        }
        String nombre = t.getNombreArchivoPdf();
        t.setArchivoPdf(null);
        t.setNombreArchivoPdf(null);
        t.setTamanoPdfBytes(null);
        tratamientoRepository.save(t);

        auditService.registrar(AccionAuditoria.DELETE, "Tratamiento", codTrat,
                "PDF eliminado: " + nombre);
    }

    /** Tupla con el contenido y los metadatos del PDF para el controlador. */
    public record PdfBytes(byte[] bytes, String nombre, Long tamano) {}
}
