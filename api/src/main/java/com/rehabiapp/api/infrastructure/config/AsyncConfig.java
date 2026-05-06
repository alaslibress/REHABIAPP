package com.rehabiapp.api.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Habilita la ejecucion asincrona de eventos (`@Async`) para los listeners
 * de aplicacion como {@code RegenerarMdListener}.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
