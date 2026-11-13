package com.temipora.backend_springboot.tareas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.temipora.backend_springboot.servicios.AnclajeService;

/**
 * Sella periódicamente los eventos nuevos del ledger en Bitcoin (OpenTimestamps)
 * e intenta confirmar los anclajes pendientes.
 */
@Component
public class AnclajeLedgerJob {

    private static final Logger log = LoggerFactory.getLogger(AnclajeLedgerJob.class);

    private final AnclajeService anclajeService;

    @Value("${temipora.anclaje.habilitado:true}")
    private boolean habilitado;

    public AnclajeLedgerJob(AnclajeService anclajeService) {
        this.anclajeService = anclajeService;
    }

    @Scheduled(initialDelayString = "PT5M", fixedDelayString = "PT1H")
    public void anclar() {
        if (!habilitado) {
            return;
        }
        try {
            anclajeService.anclar().ifPresent(anclaje -> log.info(
                "Sellado el lote de anclaje {} ({} eventos).",
                anclaje.getIdAnclaje(), anclaje.getCantidadEventos()));
            int confirmados = anclajeService.confirmarPendientes();
            if (confirmados > 0) {
                log.info("Se confirmaron {} anclaje(s) en Bitcoin.", confirmados);
            }
        } catch (Exception e) {
            log.warn("No se pudo anclar el ledger: {}", e.getMessage());
        }
    }
}
