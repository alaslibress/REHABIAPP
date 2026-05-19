package com.rehabiapp.api.infrastructure.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
 *
 * Property `rehabiapp.flyway.repair-on-startup` (default false): cuando
 * true, ejecuta Flyway.repair() ANTES de migrate. Util tras renombrar o
 * editar migraciones ya aplicadas en la BD (corrige checksums en
 * flyway_schema_history sin perder datos). Levantar UNA vez con la flag
 * en true y volver a apagarla despues.
 */
@Configuration
public class FlywayConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true", matchIfMissing = true)
    public Flyway flyway(
            DataSource dataSource,
            @Value("${spring.flyway.locations:classpath:db/migration}") String locations,
            @Value("${spring.flyway.baseline-on-migrate:true}") boolean baselineOnMigrate,
            @Value("${spring.flyway.baseline-version:0}") String baselineVersion,
            @Value("${spring.flyway.validate-on-migrate:true}") boolean validateOnMigrate,
            @Value("${rehabiapp.flyway.repair-on-startup:false}") boolean repairOnStartup
    ) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(locations)
                .baselineOnMigrate(baselineOnMigrate)
                .baselineVersion(baselineVersion)
                .validateOnMigrate(validateOnMigrate)
                .load();

        // Repair condicional: corrige checksums en flyway_schema_history cuando
        // una migracion fue editada/renombrada despues de aplicarse. NO altera
        // datos de la BD; solo la tabla de historial de Flyway.
        if (repairOnStartup) {
            flyway.repair();
        }
        flyway.migrate();
        return flyway;
    }
}
