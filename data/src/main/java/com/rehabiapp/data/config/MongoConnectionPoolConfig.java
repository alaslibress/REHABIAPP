package com.rehabiapp.data.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.concurrent.TimeUnit;

/**
 * Ajusta el pool de conexiones del driver MongoDB.
 *
 * Spring Data MongoDB detecta el bean MongoClientSettings y lo usa en lugar
 * de construir uno propio, respetando las propiedades spring.data.mongodb.*
 * que se aplican despues mediante MongoClientSettingsCustomizer (SB4 internamente).
 * Los valores se exponen via variables de entorno para tuning en K8s.
 */
@Configuration
@Profile("!test")
public class MongoConnectionPoolConfig {

    @Value("${rehabiapp.mongo.pool.min-size:5}")
    private int minSize;

    @Value("${rehabiapp.mongo.pool.max-size:50}")
    private int maxSize;

    @Value("${rehabiapp.mongo.pool.max-wait-time-ms:30000}")
    private long maxWaitTimeMs;

    @Value("${rehabiapp.mongo.pool.max-connection-idle-time-ms:600000}")
    private long maxConnectionIdleTimeMs;

    @Value("${spring.data.mongodb.uri:mongodb://localhost:27017/rehabiapp_data}")
    private String mongoUri;

    @Bean
    public MongoClientSettings mongoClientSettings() {
        return MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(mongoUri))
                .applyToConnectionPoolSettings(pool ->
                        pool.minSize(minSize)
                            .maxSize(maxSize)
                            .maxWaitTime(maxWaitTimeMs, TimeUnit.MILLISECONDS)
                            .maxConnectionIdleTime(maxConnectionIdleTimeMs, TimeUnit.MILLISECONDS)
                )
                .build();
    }
}
