package com.rehabiapp.api.infrastructure.encryption;

import com.rehabiapp.api.infrastructure.config.SecurityProperties;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Converter JPA para cifrado AES-256-GCM transparente de campos clínicos sensibles.
 *
 * <p>Compatible con el CifradoService del desktop ERP. Usa el mismo formato de
 * almacenamiento: Base64(iv[12] || ciphertext+authTag[variable]).</p>
 *
 * <p>Campos protegidos: número de seguridad social, diagnósticos, alergias,
 * medicación actual e historial médico (RGPD Art. 9 — datos sanitarios
 * como categoría especial).</p>
 *
 * <p>IV aleatorio de 96 bits (12 bytes) por operación de escritura —
 * nunca se reutiliza el mismo IV con la misma clave (requisito GCM).</p>
 *
 * <p>Tag de autenticación de 128 bits — detecta cualquier modificación no autorizada
 * del dato cifrado (integridad garantizada).</p>
 */
@Converter
@Component
public class CampoClinicoConverter implements AttributeConverter<String, String> {

    private static final String ALGORITMO = "AES/GCM/NoPadding";
    // IV recomendado para GCM: 96 bits (12 bytes)
    private static final int IV_BYTES = 12;
    // Tag de autenticación GCM: 128 bits
    private static final int TAG_BITS = 128;

    private final byte[] claveBytes;

    public CampoClinicoConverter(SecurityProperties props) {
        // Derivar clave de exactamente 32 bytes (256 bits) desde la propiedad de configuración
        byte[] raw = props.encryptionKey().getBytes(StandardCharsets.UTF_8);
        this.claveBytes = Arrays.copyOf(raw, 32);
    }

    /**
     * Cifra el texto plano con AES-256-GCM antes de almacenarlo en PostgreSQL.
     *
     * <p>Formato de salida: Base64(iv[12 bytes] || ciphertext+authTag).</p>
     *
     * @param texto Texto plano a cifrar. Null se almacena como null.
     * @return Texto cifrado en Base64 o null.
     */
    @Override
    public String convertToDatabaseColumn(String texto) {
        if (texto == null) {
            return null;
        }
        try {
            // Generar IV aleatorio de 96 bits por cada operación de escritura
            byte[] iv = new byte[IV_BYTES];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(claveBytes, "AES"),
                    new GCMParameterSpec(TAG_BITS, iv)
            );

            byte[] cifrado = cipher.doFinal(texto.getBytes(StandardCharsets.UTF_8));

            // Concatenar IV + ciphertext+authTag y codificar en Base64
            byte[] resultado = new byte[IV_BYTES + cifrado.length];
            System.arraycopy(iv, 0, resultado, 0, IV_BYTES);
            System.arraycopy(cifrado, 0, resultado, IV_BYTES, cifrado.length);

            return Base64.getEncoder().encodeToString(resultado);
        } catch (Exception e) {
            throw new RuntimeException("Error al cifrar campo clínico", e);
        }
    }

    /**
     * Descifra el valor almacenado en Base64 y devuelve el texto plano.
     *
     * <p>Si el tag de autenticación no coincide, el descifrado falla con excepción —
     * lo que indica que el dato fue modificado sin autorización.</p>
     *
     * @param datos Dato cifrado en Base64 almacenado en PostgreSQL. Null se devuelve como null.
     * @return Texto plano descifrado o null.
     */
    @Override
    public String convertToEntityAttribute(String datos) {
        if (datos == null) {
            return null;
        }
        try {
            byte[] decodificado = Base64.getDecoder().decode(datos);

            // Separar IV (primeros 12 bytes) y ciphertext+authTag (resto)
            byte[] iv = new byte[IV_BYTES];
            byte[] cifrado = new byte[decodificado.length - IV_BYTES];
            System.arraycopy(decodificado, 0, iv, 0, IV_BYTES);
            System.arraycopy(decodificado, IV_BYTES, cifrado, 0, cifrado.length);

            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    new SecretKeySpec(claveBytes, "AES"),
                    new GCMParameterSpec(TAG_BITS, iv)
            );

            return new String(cipher.doFinal(cifrado), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error al descifrar campo clínico", e);
        }
    }
}
