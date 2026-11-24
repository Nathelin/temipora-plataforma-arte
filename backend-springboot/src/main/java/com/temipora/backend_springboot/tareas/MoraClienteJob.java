package com.temipora.backend_springboot.tareas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.temipora.backend_springboot.servicios.MoraClienteService;

/**
 * Registra la mora del cliente por el pago de la entrega final.
 */
@Component
public class MoraClienteJob {

    private static final Logger log =
        LoggerFactory.getLogger(MoraClienteJob.class);

    private final MoraClienteService moraClienteService;

    @Value("${temipora.impago-cliente.programado:true}")
    private boolean habilitado;

    public MoraClienteJob(MoraClienteService moraClienteService) {
        this.moraClienteService = moraClienteService;
    }

    @Scheduled(initialDelayString = "PT7M", fixedDelayString = "PT10M")
    public void ejecutar() {
        if (!habilitado) {
            return;
        }
        try {
            int registrados = moraClienteService.detectar();
            if (registrados > 0) {
                log.info("Impago de entrega final registrado en {} pedido(s).",
                    registrados);
            }
        } catch (Exception e) {
            log.error("Error al detectar el impago del cliente", e);
        }
    }
}
