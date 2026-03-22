package com.javafx.util;

import java.time.format.DateTimeFormatter;

/**
 * Constantes de aplicacion centralizadas para RehabiAPP
 */
public final class ConstantesApp {

    // Constructor privado para evitar instanciacion
    private ConstantesApp() {
        throw new UnsupportedOperationException("Clase de constantes no instanciable");
    }

    // ==================== FORMATOS DE FECHA Y HORA ====================

    /**
     * Formato para visualizacion de fechas: dd/MM/yyyy
     */
    public static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Formato para visualizacion de horas: HH:mm
     */
    public static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Formato para timestamps en nombres de archivo: yyyyMMdd_HHmmss
     */
    public static final DateTimeFormatter FORMATO_TIMESTAMP_ARCHIVO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // ==================== DURACIONES DE ANIMACIONES (milisegundos) ====================

    /**
     * Duracion animacion corta: 200ms
     */
    public static final int DURACION_ANIMACION_CORTA = 200;

    /**
     * Duracion animacion media: 300ms
     */
    public static final int DURACION_ANIMACION_MEDIA = 300;

    /**
     * Duracion animacion larga: 500ms
     */
    public static final int DURACION_ANIMACION_LARGA = 500;

    /**
     * Duracion animacion ventana modal: 250ms
     */
    public static final int DURACION_VENTANA_MODAL = 250;

    // ==================== CONFIGURACION DE PROPERTIES ====================

    /**
     * Archivo de configuracion
     */
    public static final String CONFIG_FILE = "config.properties";

    /**
     * Clave para tamaño de letra en properties
     */
    public static final String KEY_TAMANIO_LETRA = "tamanio.letra";

    /**
     * Clave para tema en properties
     */
    public static final String KEY_TEMA = "tema";

    // ==================== VALORES POR DEFECTO ====================

    /**
     * Tamaño de letra por defecto
     */
    public static final String TAMANIO_LETRA_DEFAULT = "Mediano (14px)";

    /**
     * Tema por defecto
     */
    public static final String TEMA_DEFAULT = "claro";

    // ==================== RUTAS DE CSS ====================

    /**
     * Ruta al archivo CSS del tema claro
     */
    public static final String CSS_TEMA_CLARO = "/tema_claro.css";

    /**
     * Ruta al archivo CSS del tema oscuro
     */
    public static final String CSS_TEMA_OSCURO = "/tema_oscuro.css";

    // ==================== RUTAS DE IMAGENES ====================

    /**
     * Ruta imagen usuario por defecto
     */
    public static final String IMG_USUARIO_DEFAULT = "/usuario_default.png";

    // ==================== MENSAJES COMUNES ====================

    /**
     * Mensaje de error al conectar con BD
     */
    public static final String MSG_ERROR_BD = "Error al conectar con la base de datos";

    /**
     * Mensaje campos vacios
     */
    public static final String MSG_CAMPOS_VACIOS = "Por favor, complete todos los campos obligatorios";

    /**
     * Mensaje operacion exitosa
     */
    public static final String MSG_OPERACION_EXITOSA = "Operacion realizada correctamente";

    /**
     * Mensaje operacion fallida
     */
    public static final String MSG_OPERACION_FALLIDA = "No se pudo completar la operacion";

    // ==================== CONFIGURACION DE SPINNERS ====================

    /**
     * Edad minima para spinner
     */
    public static final int EDAD_MINIMA = 0;

    /**
     * Edad maxima para spinner
     */
    public static final int EDAD_MAXIMA = 120;

    /**
     * Edad por defecto para spinner
     */
    public static final int EDAD_DEFAULT = 18;

    /**
     * Hora minima para spinner
     */
    public static final int HORA_MINIMA = 0;

    /**
     * Hora maxima para spinner
     */
    public static final int HORA_MAXIMA = 23;

    /**
     * Minutos minimos para spinner
     */
    public static final int MINUTOS_MINIMOS = 0;

    /**
     * Minutos maximos para spinner
     */
    public static final int MINUTOS_MAXIMOS = 59;

    // ==================== CONFIGURACION DE DIRECTORIOS ====================

    /**
     * Directorio para informes generados
     */
    public static final String DIRECTORIO_INFORMES = "informes";
}
