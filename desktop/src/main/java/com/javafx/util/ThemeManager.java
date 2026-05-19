package com.javafx.util;

import javafx.scene.Scene;

/**
 * Gestor de tema visual (rediseño Claude Design - mayo 2026).
 *
 * Carga dos hojas en cada Scene:
 *   1. tokens-{theme}.css con los looked-up colors de la paleta
 *   2. rehabiapp.css con las reglas comunes de componente
 *
 * Para cambiar de tema en caliente, llamar a {@link #applyTheme(Scene, String)}.
 */
public final class ThemeManager {

    private ThemeManager() {
        throw new UnsupportedOperationException("Utilidad estatica no instanciable");
    }

    /**
     * Aplica el tema indicado a la escena.
     *
     * Elimina cualquier hoja "tokens-*.css" existente, anade la nueva al inicio
     * y garantiza que rehabiapp.css este cargada exactamente una vez.
     *
     * @param scene Escena destino. Si es {@code null}, no hace nada.
     * @param theme "light" o "dark". Cualquier otro valor se trata como "light".
     */
    public static void applyTheme(Scene scene, String theme) {
        if (scene == null) {
            return;
        }
        String normalized = "dark".equals(theme) ? "dark" : "light";

        // Limpiar cualquier hoja de tokens previa
        scene.getStylesheets().removeIf(s -> s.contains("tokens-"));

        String tokens = "/styles/tokens-" + normalized + ".css";
        String tokensUrl = ThemeManager.class.getResource(tokens).toExternalForm();
        scene.getStylesheets().add(0, tokensUrl);

        // Asegurar que rehabiapp.css esta cargada una unica vez
        String common = ThemeManager.class.getResource(ConstantesApp.CSS_REHABIAPP).toExternalForm();
        if (!scene.getStylesheets().contains(common)) {
            scene.getStylesheets().add(common);
        }
    }

    /**
     * Traduce el codigo legacy "claro"/"oscuro" al codigo "light"/"dark"
     * usado por los tokens del rediseño.
     */
    public static String legacyToCode(String legacy) {
        return "oscuro".equals(legacy) ? "dark" : "light";
    }
}
