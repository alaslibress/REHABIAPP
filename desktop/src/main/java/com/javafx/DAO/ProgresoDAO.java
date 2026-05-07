package com.javafx.DAO;

import com.fasterxml.jackson.core.type.TypeReference;
import com.javafx.Clases.ApiClient;
import com.javafx.Clases.CheckProgreso;
import com.javafx.Clases.ProgresoTratamiento;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/** DAO para consultar el progreso clinico de un paciente via la API REST. */
public class ProgresoDAO {

    private final ApiClient api = ApiClient.getInstancia();

    /** Obtiene la lista de progreso por tratamiento del paciente. */
    public List<ProgresoTratamiento> obtenerProgreso(String dni) {
        return api.get("/api/pacientes/" + dni + "/progreso",
                new TypeReference<List<ProgresoTratamiento>>() {});
    }

    /** Comprueba si hay sesiones nuevas posteriores a la marca de tiempo indicada. */
    public CheckProgreso comprobarNuevosDatos(String dni, Instant desde) {
        String qs = (desde != null) ? "?since=" + desde.toString() : "";
        return api.get("/api/pacientes/" + dni + "/progreso/check" + qs, CheckProgreso.class);
    }

    /** Obtiene el informe Markdown de progreso del paciente. */
    public String obtenerMarkdown(String dni) {
        byte[] bytes = api.getBytes("/api/pacientes/" + dni + "/progreso/markdown");
        return bytes != null ? new String(bytes, StandardCharsets.UTF_8) : "";
    }
}
