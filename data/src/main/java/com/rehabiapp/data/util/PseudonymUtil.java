package com.rehabiapp.data.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Genera tokens opacos anonimizados para sustituir identificadores de pacientes
 * en respuestas de analitica. El salt debe rotarse periodicamente (K8s Secret).
 */
@Component
public class PseudonymUtil {

    private final byte[] salt;

    public PseudonymUtil(@Value("${rehabiapp.pseudonym.salt}") String salt) {
        this.salt = salt.getBytes(StandardCharsets.UTF_8);
    }

    public String tokenize(String patientDni) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] digest = md.digest(patientDni.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
