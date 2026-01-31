package com.javafx.util;

import java.util.regex.Pattern;

/**
 * Utilidades para validacion de datos del sistema RehabiAPP
 * Centraliza todas las expresiones regulares y validaciones
 */
public class ValidacionUtil {

    // Expresiones regulares compiladas (mejor rendimiento)
    private static final Pattern REGEX_DNI = Pattern.compile("^[0-9]{8}[A-Za-z]$");
    private static final Pattern REGEX_EMAIL = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern REGEX_TELEFONO = Pattern.compile("^[0-9]{9}$");
    private static final Pattern REGEX_NSS = Pattern.compile("^[0-9]{12}$");
    private static final Pattern REGEX_CP = Pattern.compile("^[0-9]{5}$");

    // Constructor privado para evitar instanciacion
    private ValidacionUtil() {
        throw new UnsupportedOperationException("Clase de utilidad no instanciable");
    }

    /**
     * Valida formato de DNI español (8 digitos + letra)
     * @param dni DNI a validar
     * @return true si el formato es valido
     */
    public static boolean validarDNI(String dni) {
        return dni != null && REGEX_DNI.matcher(dni).matches();
    }

    /**
     * Valida formato de email
     * @param email Email a validar
     * @return true si el formato es valido
     */
    public static boolean validarEmail(String email) {
        return email != null && REGEX_EMAIL.matcher(email).matches();
    }

    /**
     * Valida formato de telefono (9 digitos)
     * @param telefono Telefono a validar
     * @return true si el formato es valido
     */
    public static boolean validarTelefono(String telefono) {
        return telefono != null && REGEX_TELEFONO.matcher(telefono).matches();
    }

    /**
     * Valida formato de Numero de Seguridad Social (12 digitos)
     * @param nss NSS a validar
     * @return true si el formato es valido
     */
    public static boolean validarNSS(String nss) {
        return nss != null && REGEX_NSS.matcher(nss).matches();
    }

    /**
     * Valida formato de codigo postal (5 digitos)
     * @param cp Codigo postal a validar
     * @return true si el formato es valido
     */
    public static boolean validarCodigoPostal(String cp) {
        return cp != null && REGEX_CP.matcher(cp).matches();
    }

    /**
     * Obtiene el patron regex para DNI (util para ControlsFX validators)
     * @return String con el patron regex
     */
    public static String getPatronDNI() {
        return "^[0-9]{8}[A-Za-z]$";
    }

    /**
     * Obtiene el patron regex para email (util para ControlsFX validators)
     * @return String con el patron regex
     */
    public static String getPatronEmail() {
        return "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$";
    }

    /**
     * Obtiene el patron regex para telefono (util para ControlsFX validators)
     * @return String con el patron regex
     */
    public static String getPatronTelefono() {
        return "^[0-9]{9}$";
    }

    /**
     * Obtiene el patron regex para NSS (util para ControlsFX validators)
     * @return String con el patron regex
     */
    public static String getPatronNSS() {
        return "^[0-9]{12}$";
    }

    /**
     * Obtiene el patron regex para codigo postal (util para ControlsFX validators)
     * @return String con el patron regex
     */
    public static String getPatronCodigoPostal() {
        return "^[0-9]{5}$";
    }
}
