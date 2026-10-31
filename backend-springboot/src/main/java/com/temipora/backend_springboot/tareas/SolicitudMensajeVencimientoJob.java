package com.temipora.backend_springboot.tareas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.temipora.backend_springboot.servicios.ConversacionService;

@Component
public class SolicitudMensajeVencimientoJob {

    private static final Logger log =
        LoggerFactory.getLogger(SolicitudMensajeVencimientoJob.class);

    private final ConversacionService conversacionService;

    public SolicitudMensajeVencimientoJob(
            ConversacionService conversacionService) {
        this.conversacionService = conversacionService;
    }

    @Scheduled(initialDelayString = "PT5M", fixedDelayString = "PT1H")
    public void ejecutar() {
        try {
            int vencidas = conversacionService.vencerPendientes();
            if (vencidas > 0) {
                log.info(
                    "Se vencieron {} solicitud(es) de mensaje pendientes.",
                    vencidas);
            }
        } catch (Exception e) {
            log.warn(
                "No se pudieron procesar las solicitudes de mensaje vencidas: {}",
                e.getMessage());
        }
    }
}
