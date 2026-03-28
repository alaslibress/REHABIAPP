package com.rehabiapp.api.infrastructure.encryption;

import com.rehabiapp.api.infrastructure.config.SecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para CampoClinicoConverter.
 *
 * <p>Verifica el cifrado y descifrado AES-256-GCM de campos clínicos sensibles.
 * No requiere contexto de Spring — el converter se instancia directamente.</p>
 *
 * <p>La clave AES debe tener exactamente 32 bytes (256 bits) según el algoritmo.
 * SecurityProperties recorta o rellena hasta 32 bytes automáticamente.</p>
 */
class CampoClinicoConverterTest {

    // Clave AES de exactamente 32 caracteres ASCII (256 bits)
    private static final String CLAVE_AES_PRUEBA = "clave-aes-32-caracteres-exactos!";
    private static final String CLAVE_JWT_PLACEHOLDER =
            "jwt-key-placeholder-64chars-needed-for-hmac-sha512-algorithm!!!!!";

    private CampoClinicoConverter converter;

    @BeforeEach
    void setUp() {
        // Instanciar converter directamente con propiedades de prueba
        SecurityProperties props = new SecurityProperties(
                CLAVE_JWT_PLACEHOLDER,
                900000L,
                604800000L,
                CLAVE_AES_PRUEBA
        );
        converter = new CampoClinicoConverter(props);
    }

    @Test
    void cifrarYDescifrar_devuelveTextoOriginal() {
        // El ciclo completo cifrado->descifrado debe recuperar el texto original
        String original = "Alergias: penicilina, aspirina";
        String cifrado = converter.convertToDatabaseColumn(original);
        String descifrado = converter.convertToEntityAttribute(cifrado);
        assertEquals(original, descifrado);
    }

    @Test
    void cifrar_generaIVAleatorio_resultadoDiferentePorCadaLlamada() {
        // Cada llamada genera un IV aleatorio de 96 bits, por lo que
        // el mismo texto plano produce un cifrado diferente en cada operación
        String texto = "mismo texto";
        String cifrado1 = converter.convertToDatabaseColumn(texto);
        String cifrado2 = converter.convertToDatabaseColumn(texto);
        assertNotEquals(cifrado1, cifrado2, "Cada cifrado debe producir un resultado diferente por el IV aleatorio");
    }

    @Test
    void cifrar_conValorNull_retornaNull() {
        // Los campos null se almacenan como null, no cifrados
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void descifrar_conValorNull_retornaNull() {
        // Los valores null recuperados de BD se devuelven como null sin procesamiento
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    void descifrar_conDatosCorruptos_lanzaExcepcion() {
        // Datos no cifrados con AES-GCM deben lanzar excepción
        // (el tag de autenticación GCM detectará la corrupción)
        assertThrows(RuntimeException.class,
                () -> converter.convertToEntityAttribute("datos-no-validos-base64corrupto"));
    }

    @Test
    void cifrarYDescifrar_conTextoLargo_funcionaCorrectamente() {
        // AES-GCM no tiene límite práctico de longitud de texto
        String textoLargo = "A".repeat(10000);
        String cifrado = converter.convertToDatabaseColumn(textoLargo);
        String descifrado = converter.convertToEntityAttribute(cifrado);
        assertEquals(textoLargo, descifrado);
    }
}
