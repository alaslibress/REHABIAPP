package com.javafx.util;

import java.util.function.Function;

import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

/**
 * Utilidades de presentacion para celdas de {@link javafx.scene.control.TableView}.
 *
 * <p>Centraliza dos patrones repetidos en los controladores:</p>
 * <ul>
 *   <li><b>Badges semanticos</b> (Si/No, Activo, Masculino, niveles clinicos)
 *       — sustituyen texto plano por una etiqueta estilizada con clases CSS
 *       {@code badge} + modificador ({@code ok}, {@code danger}, {@code warn},
 *       {@code brand}, {@code info}).</li>
 *   <li><b>Columnas numericas</b> (DNI, codigo, edad, IDs) — aplican la clase
 *       {@code col-num} para usar tipografia monoespaciada y color secundario,
 *       segun lo definido en {@code rehabiapp.css} §3.6.</li>
 * </ul>
 *
 * <p>Estos helpers NO modifican el {@code cellValueFactory}; solo el
 * {@code cellFactory}, por lo que se aplican sobre columnas ya configuradas
 * con {@code PropertyValueFactory} o lambdas equivalentes.</p>
 */
public final class TableUiUtil {

    private TableUiUtil() {
        // Clase de utilidades, no instanciable.
    }

    /**
     * Construye una cellFactory que renderiza el valor como un {@link Label}
     * con clases {@code badge} + modificador semantico.
     *
     * @param textoFn  funcion que dado el valor de la celda devuelve el texto
     *                 visible del badge (no debe ser null).
     * @param estiloFn funcion que devuelve el modificador semantico CSS
     *                 ({@code "ok"}, {@code "danger"}, {@code "warn"},
     *                 {@code "brand"}, {@code "info"}) o cadena vacia para
     *                 estilo neutro.
     * @param <S>      tipo del item de la fila.
     * @param <T>      tipo del valor de la columna.
     * @return cellFactory lista para asignar via
     *         {@link TableColumn#setCellFactory(Callback)}.
     */
    public static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> badgeCell(
            Function<T, String> textoFn, Function<T, String> estiloFn) {
        return column -> new TableCell<S, T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                String texto = textoFn.apply(item);
                String modificador = estiloFn.apply(item);
                Label etiqueta = new Label(texto != null ? texto : "");
                etiqueta.getStyleClass().add("badge");
                if (modificador != null && !modificador.isBlank()) {
                    etiqueta.getStyleClass().add(modificador);
                }
                setText(null);
                setGraphic(etiqueta);
            }
        };
    }

    /**
     * Construye una cellFactory que muestra el valor (formateado con
     * {@code toString}) usando la clase CSS {@code col-num} —
     * tipografia monoespaciada para DNI, codigos, IDs, edades.
     */
    public static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> monoCell() {
        return column -> {
            TableCell<S, T> celda = new TableCell<S, T>() {
                @Override
                protected void updateItem(T item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.toString());
                    setGraphic(null);
                }
            };
            celda.getStyleClass().add("col-num");
            return celda;
        };
    }

    // ---------------------------------------------------------------------
    // Helpers de mapeo semantico — mantienen consistencia entre tablas.
    // ---------------------------------------------------------------------

    /**
     * Devuelve el estilo de badge para un nivel clinico de progresion.
     * Mapeo: agudo→danger, subagudo→warn, fortalecimiento→info,
     * funcional→ok. Otros valores → estilo neutro.
     */
    public static String estiloNivel(String nombreNivel) {
        if (nombreNivel == null) return "";
        String n = nombreNivel.trim().toLowerCase();
        if (n.startsWith("agud"))            return "danger";
        if (n.startsWith("subag"))           return "warn";
        if (n.startsWith("fortale"))         return "info";
        if (n.startsWith("funcion"))         return "ok";
        if (n.contains("complet"))           return "ok";
        if (n.contains("alto"))              return "info";
        if (n.contains("medio"))             return "warn";
        if (n.contains("bajo"))              return "danger";
        return "";
    }

    /**
     * Devuelve el estilo de badge para un sexo segun el catalogo del dominio.
     * Masculino → info; Femenino → brand. Otros → neutro.
     */
    public static String estiloSexo(String sexo) {
        if (sexo == null) return "";
        String s = sexo.trim().toUpperCase();
        if (s.startsWith("M")) return "info";
        if (s.startsWith("F")) return "brand";
        return "";
    }
}
