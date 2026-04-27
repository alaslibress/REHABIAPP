package com.rehabiapp.data.presentation;

import com.rehabiapp.data.application.service.ExportService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de exportacion de sesiones de un paciente en CSV o JSON.
 * Usa streaming — no carga el resultado completo en memoria.
 */
@RestController
@RequestMapping("/analytics/export")
@Validated
public class ExportController {

    private final ExportService service;

    public ExportController(ExportService service) { this.service = service; }

    @GetMapping("/patient/{dni}")
    public void export(
            @PathVariable
            @Pattern(regexp = "^[0-9]{8}[A-HJ-NP-TV-Z]$", message = "DNI invalido")
            String dni,
            @RequestParam
            @Pattern(regexp = "csv|json", message = "format must be csv or json")
            String format,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            HttpServletResponse response) throws Exception {

        String stamp = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String filename = "patient_" + dni + "_" + stamp + "." + format;
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        if ("csv".equals(format)) {
            response.setContentType("text/csv; charset=utf-8");
            service.streamCsv(dni, from, to, response.getWriter());
        } else {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            service.streamJson(dni, from, to, response.getWriter());
        }
    }
}
