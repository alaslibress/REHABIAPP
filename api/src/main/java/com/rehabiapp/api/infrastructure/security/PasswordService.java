package com.rehabiapp.api.infrastructure.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Servicio de hashing de contraseñas con BCrypt.
 *
 * <p>Compatible con jBCrypt 0.4 del desktop ERP: ambas implementaciones
 * generan y verifican hashes BCrypt con el mismo formato estándar.</p>
 *
 * <p>Factor de coste 12 según la política de seguridad del proyecto
 * (RGPD Art. 32, ENS Alto). Suficientemente lento para resistir
 * ataques de fuerza bruta sin degradar la experiencia de usuario.</p>
 *
 * <p>NUNCA se almacenan contraseñas en texto plano.
 * El hash incluye el salt integrando el factor de coste.</p>
 */
@Service
public class PasswordService {

    // Factor de coste 12 según política de seguridad del proyecto
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    /**
     * Genera el hash BCrypt de una contraseña en texto plano.
     *
     * @param contrasenaPlan Contraseña en texto plano.
     * @return Hash BCrypt con salt integrado.
     */
    public String hashear(String contrasenaPlan) {
        return encoder.encode(contrasenaPlan);
    }

    /**
     * Verifica una contraseña en texto plano contra un hash BCrypt almacenado.
     * Compatible con hashes generados por el desktop ERP (jBCrypt 0.4).
     *
     * @param contrasenaPlan Contraseña en texto plano introducida por el usuario.
     * @param hash           Hash BCrypt almacenado en la base de datos.
     * @return true si la contraseña coincide con el hash, false en caso contrario.
     */
    public boolean verificar(String contrasenaPlan, String hash) {
        return encoder.matches(contrasenaPlan, hash);
    }
}
