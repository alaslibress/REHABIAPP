package com.javafx.service;

import com.javafx.Clases.CheckProgreso;
import com.javafx.Clases.ProgresoTratamiento;
import com.javafx.DAO.ProgresoDAO;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de progreso clinico con cache de 30s por DNI.
 * Evita hits redundantes durante refrescos rapidos del controlador.
 */
public class ProgresoService {

    private static final long CACHE_TTL_MS = 30_000L;
    private final ProgresoDAO dao = new ProgresoDAO();
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    private record Cached(Instant momento, List<ProgresoTratamiento> datos) {}

    /** Devuelve el progreso del paciente, usando cache si tiene menos de 30s. */
    public List<ProgresoTratamiento> obtenerProgreso(String dni) {
        Cached c = cache.get(dni);
        if (c != null && Instant.now().toEpochMilli() - c.momento().toEpochMilli() < CACHE_TTL_MS) {
            return c.datos();
        }
        List<ProgresoTratamiento> fresco = dao.obtenerProgreso(dni);
        cache.put(dni, new Cached(Instant.now(), fresco));
        return fresco;
    }

    /** Comprueba si hay datos nuevos desde la marca de tiempo indicada. */
    public CheckProgreso comprobarNuevosDatos(String dni, Instant desde) {
        return dao.comprobarNuevosDatos(dni, desde);
    }

    /** Obtiene el informe Markdown del paciente. */
    public String obtenerMarkdown(String dni) {
        return dao.obtenerMarkdown(dni);
    }

    /** Invalida la cache del DNI indicado para forzar recarga. */
    public void invalidarCache(String dni) {
        cache.remove(dni);
    }
}
