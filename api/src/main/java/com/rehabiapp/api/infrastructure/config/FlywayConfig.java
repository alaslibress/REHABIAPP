package com.rehabiapp.api.infrastructure.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Configuracion manual de Flyway.
 *
 * Spring Boot 4.0.5 elimino la autoconfiguracion de Flyway del modulo
 * spring-boot-autoconfigure. Es necesario instanciar y configurar Flyway
 * explicitamente como bean de Spring para que las migraciones se ejecuten
 * al arrancar la aplicacion.
 */
@Configuration
public class FlywayConfig {

    /**
     * Configura e inicia Flyway con el DataSource de la aplicacion.
     *
     * - baseline-on-migrate: permite aplicar migraciones sobre una BD ya existente
     *   (compatibilidad con el esquema creado por el ERP de escritorio via JDBC directo).
     * - baseline-version: 0 indica que el baseline representa el estado previo a V1.
     * - validate-on-migrate: verifica el checksum de cada migracion antes de aplicarla.
     * - locations: directorio de los scripts SQL versionados en el classpath.
     */
    @Bean(initMethod = "migrate")
    public Flyway flyway(
            DataSource dataSource,
            @Value("${spring.flyway.locations:classpath:db/migration}") String locations,
            @Value("${spring.flyway.baseline-on-migrate:true}") boolean baselineOnMigrate,
            @Value("${spring.flyway.baseline-version:0}") String baselineVersion,
            @Value("${spring.flyway.validate-on-migrate:true}") boolean validateOnMigrate
    ) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations(locations)
                .baselineOnMigrate(baselineOnMigrate)
                .baselineVersion(baselineVersion)
                .validateOnMigrate(validateOnMigrate)
                .load();
    }
}
