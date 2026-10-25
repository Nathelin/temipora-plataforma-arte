package com.temipora.backend_springboot.tareas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.temipora.backend_springboot.servicios.PresupuestoService;

@Component
public class PresupuestoVencimientoJob {

    private static final Logger log = LoggerFactory.getLogger(PresupuestoVencimientoJob.class);

    private final PresupuestoService presupuestoService;

    public PresupuestoVencimientoJob(PresupuestoService presupuestoService) {
        this.presupuestoService = presupuestoService;
    }

    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT10M")
    public void expirarPresupuestos() {
        try {
            int expirados = presupuestoService.expirarVencidos();
            if (expirados > 0) {
                log.info("Se vencieron {} presupuesto(s) de servicio.", expirados);
            }
        } catch (Exception e) {
            log.warn("No se pudieron procesar los presupuestos vencidos: {}", e.getMessage());
        }
    }
}
