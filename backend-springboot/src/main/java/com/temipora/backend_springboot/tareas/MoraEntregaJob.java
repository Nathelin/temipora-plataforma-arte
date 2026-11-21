package com.temipora.backend_springboot.tareas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.temipora.backend_springboot.servicios.MoraService;

/**
 * Registra la mora de entrega de los pedidos en curso.
 */
@Component
public class MoraEntregaJob {

    private static final Logger log =
        LoggerFactory.getLogger(MoraEntregaJob.class);

    private final MoraService moraService;

    @Value("${temipora.incumplimiento.programado:true}")
    private boolean habilitado;

    public MoraEntregaJob(MoraService moraService) {
        this.moraService = moraService;
    }

    @Scheduled(initialDelayString = "PT2M", fixedDelayString = "PT10M")
    public void ejecutar() {
        if (!habilitado) {
            return;
        }
        try {
            int registradas = moraService.detectar();
            if (registradas > 0) {
                log.info("Mora de entrega registrada en {} pedido(s).", registradas);
            }
        } catch (Exception e) {
            log.error("Error al detectar la mora de entrega", e);
        }
    }
}
