package com.temipora.backend_springboot.tareas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.temipora.backend_springboot.servicios.PedidoService;

@Component
public class PedidoJob {

    private static final Logger log = LoggerFactory.getLogger(PedidoJob.class);

    private final PedidoService pedidoService;

    public PedidoJob(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @Scheduled(initialDelayString = "PT2M", fixedDelayString = "PT10M")
    public void ejecutar() {
        try {
            int aprobados = pedidoService.aprobarAutomaticamente();
            int expirados = pedidoService.expirarHitos();
            if (aprobados > 0 || expirados > 0) {
                log.info(
                    "Pedidos: {} hitos aprobados automáticamente, {} hitos vencidos.",
                    aprobados,
                    expirados);
            }
        } catch (Exception e) {
            log.error("Error al procesar hitos de pedidos", e);
        }
    }
}
