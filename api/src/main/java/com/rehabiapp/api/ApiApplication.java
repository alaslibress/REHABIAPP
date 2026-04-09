package com.rehabiapp.api;

import com.rehabiapp.api.infrastructure.config.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

/**
 * Punto de entrada principal de la API REST de RehabiAPP.
 *
 * <p>Esta API es el núcleo de comunicación del ecosistema RehabiAPP:
 * gestiona pacientes, profesionales, citas médicas, tratamientos de rehabilitación
 * y enruta la telemetría de los minijuegos hacia el servicio de datos (/data).</p>
 *
 * <p>Cumplimiento legal: RGPD, LOPDGDD, Ley 41/2002, ENS Alto.</p>
 */
@SpringBootApplication
@EnableConfigurationProperties(SecurityProperties.class)
public class ApiApplication {

    private static final Logger log = LoggerFactory.getLogger(ApiApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }

    /**
     * Registra en logs el estado de la aplicacion una vez completamente iniciada.
     * Se ejecuta tras el arranque completo de Spring (ApplicationReadyEvent).
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onReady(ApplicationReadyEvent event) {
        Environment env = event.getApplicationContext().getEnvironment();
        String[] perfiles = env.getActiveProfiles();
        String puerto = env.getProperty("server.port", "8080");
        log.info("RehabiAPP API lista — puerto={} perfiles={}",
                puerto,
                perfiles.length > 0 ? String.join(", ", perfiles) : "default");
    }
}
