package com.rehabiapp.data.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador de comprobacion de salud del servicio.
 *
 * Este endpoint es utilizado por el HEALTHCHECK del Dockerfile cuando el contenedor
 * se ejecuta fuera de Kubernetes (por ejemplo, en docker-compose local).
 *
 * En Kubernetes, los probes de liveness y readiness apuntan a los endpoints
 * del Actuator (/actuator/health/liveness y /actuator/health/readiness),
 * que incluyen la comprobacion de conectividad con MongoDB.
 *
 * Este endpoint NO comprueba conectividad con dependencias externas: solo
 * verifica que el proceso Java esta activo y respondiendo peticiones HTTP.
 */
@RestController
public class HealthController {

    /**
     * Endpoint de salud simple para el HEALTHCHECK del Dockerfile.
     *
     * @return 200 OK con cuerpo "OK" si el servicio esta operativo
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

}
