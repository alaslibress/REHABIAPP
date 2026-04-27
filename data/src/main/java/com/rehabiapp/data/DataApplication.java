package com.rehabiapp.data;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Punto de entrada del pipeline de datos de RehabiAPP.
 *
 * Este servicio es INTERNO: recibe telemetria de sesiones de juego
 * exclusivamente desde el API Core (/api) via endpoints de ingesta.
 * Nunca esta expuesto directamente a trafico externo.
 *
 * Puerto por defecto: 8081 (evita colision con el API Core en 8080).
 */
@SpringBootApplication
@EnableScheduling
public class DataApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataApplication.class, args);
    }

}
