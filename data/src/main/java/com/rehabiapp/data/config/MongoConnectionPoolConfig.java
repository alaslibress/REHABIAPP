package com.rehabiapp.data.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

import java.util.concurrent.TimeUnit;

/**
 * Configura el cliente MongoDB y el {@link MongoDatabaseFactory} explicitos.
 *
 * <p>Provee un bean {@link MongoClientSettings} con host/puerto del URI ({@code
 * spring.data.mongodb.uri}) y limites del pool. Al definir ese bean Spring Boot
 * deja de aplicar {@code MongoProperties}, por lo que tambien se define
 * {@code MongoDatabaseFactory} con la BD obtenida explicitamente de
 * {@code spring.data.mongodb.database}. Sin esto el driver cae a la BD "test"
 * (comportamiento por defecto del cliente MongoDB).</p>
 *
 * <p>Los valores se exponen via variables de entorno para tuning en K8s.</p>
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

    @Value("${spring.data.mongodb.uri:mongodb://localhost:27017/rehabiapp_telemetry}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database:rehabiapp_telemetry}")
    private String mongoDatabase;

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

    @Bean
    public MongoClient mongoClient(MongoClientSettings settings) {
        return MongoClients.create(settings);
    }

    @Bean
    public MongoDatabaseFactory mongoDatabaseFactory(MongoClient mongoClient) {
        return new SimpleMongoClientDatabaseFactory(mongoClient, mongoDatabase);
    }
}
