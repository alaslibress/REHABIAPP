package com.rehabiapp.api.infrastructure.event;

import com.rehabiapp.api.application.event.RegenerarMdEvent;
import com.rehabiapp.api.infrastructure.client.DataPipelineClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener asincrono que solicita al pipeline `/data` la regeneracion del
 * Markdown del paciente tras una nueva ingesta de telemetria.
 */
@Component
public class RegenerarMdListener {

    private static final Logger log = LoggerFactory.getLogger(RegenerarMdListener.class);

    private final DataPipelineClient client;

    public RegenerarMdListener(DataPipelineClient client) {
        this.client = client;
    }

    @Async
    @EventListener
    public void onEvent(RegenerarMdEvent event) {
        try {
            client.regenerarMarkdown(event.dniPac());
            log.debug("Markdown regenerado para paciente {}", event.dniPac());
        } catch (Exception e) {
            log.error("Fallo al regenerar Markdown para paciente {}", event.dniPac(), e);
        }
    }
}
