package com.rehabiapp.api;

import com.rehabiapp.api.infrastructure.config.SecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

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

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}
