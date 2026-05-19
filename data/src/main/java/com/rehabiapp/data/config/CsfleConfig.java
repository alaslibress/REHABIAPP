package com.rehabiapp.data.config;

import com.mongodb.AutoEncryptionSettings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Configura CSFLE en el MongoClient cuando rehabiapp.csfle.enabled=true.
 *
 * Algoritmos de cifrado (skill springboot4-mongodb):
 *   - Deterministic: campos consultables (patientDni, patientToken)
 *   - Random: campos clinicos no consultables (metricas de movimiento)
 *
 * En desarrollo: CSFLE_ENABLED=false (sin cifrado).
 * En produccion AWS: usa KMS via IRSA. Requiere libmongocrypt nativa en runtime.
 */
@Configuration
@EnableConfigurationProperties(CsfleProperties.class)
@ConditionalOnProperty(name = "rehabiapp.csfle.enabled", havingValue = "true")
public class CsfleConfig {

    private final CsfleProperties props;

    public CsfleConfig(CsfleProperties props) {
        this.props = props;
    }

    @Bean
    public MongoClientSettingsBuilderCustomizer csfleCustomizer() {
        return builder -> {
            var kmsProviders = buildKmsProviders();
            var encryptionSettings = AutoEncryptionSettings.builder()
                    .keyVaultNamespace(props.keyVaultNamespace())
                    .kmsProviders(kmsProviders)
                    .build();
            builder.autoEncryptionSettings(encryptionSettings);
        };
    }

    private Map<String, Map<String, Object>> buildKmsProviders() {
        return switch (props.kmsProvider()) {
            case "local" -> buildLocalKmsProvider();
            case "aws" -> Map.of("aws", Map.of());  // IRSA: credenciales via rol EC2/EKS
            default -> throw new IllegalStateException("KMS provider no soportado: " + props.kmsProvider());
        };
    }

    private Map<String, Map<String, Object>> buildLocalKmsProvider() {
        try {
            byte[] masterKey = Files.readAllBytes(Path.of(props.masterKeyPath()));
            return Map.of("local", Map.of("key", masterKey));
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo leer la clave maestra CSFLE desde: " + props.masterKeyPath(), e);
        }
    }
}
