package com.temipora.backend_springboot.tareas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.temipora.backend_springboot.servicios.ProrrogaService;

/**
 * Vence las propuestas de prórroga que el cliente no respondió en la ventana
 * configurada. Al vencer, se mantienen los plazos originales del pedido.
 */
@Component
public class ProrrogaVencimientoJob {

    private static final Logger log =
        LoggerFactory.getLogger(ProrrogaVencimientoJob.class);

    private final ProrrogaService prorrogaService;

    public ProrrogaVencimientoJob(ProrrogaService prorrogaService) {
        this.prorrogaService = prorrogaService;
    }

    @Scheduled(initialDelayString = "PT3M", fixedDelayString = "PT10M")
    public void ejecutar() {
        try {
            int vencidas = prorrogaService.expirarVencidas();
            if (vencidas > 0) {
                log.info(
                    "Se vencieron {} propuesta(s) de prórroga sin respuesta.",
                    vencidas);
            }
        } catch (Exception e) {
            log.error("Error al vencer propuestas de prórroga", e);
        }
    }
}
