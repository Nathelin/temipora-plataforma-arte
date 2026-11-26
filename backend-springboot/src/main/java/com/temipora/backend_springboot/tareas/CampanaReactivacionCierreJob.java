package com.temipora.backend_springboot.tareas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.temipora.backend_springboot.servicios.OfertaReactivacionService;

@Component
public class CampanaReactivacionCierreJob {

    private static final Logger log =
        LoggerFactory.getLogger(CampanaReactivacionCierreJob.class);

    private final OfertaReactivacionService ofertaService;

    public CampanaReactivacionCierreJob(
            OfertaReactivacionService ofertaService) {
        this.ofertaService = ofertaService;
    }

    @Scheduled(cron = "0 10 * * * *")
    public void ejecutar() {
        try {
            int cerradas = ofertaService.cerrarCampanasVencidas();
            if (cerradas > 0) {
                log.info("Promociones: {} campaña(s) vencida(s) cerrada(s).",
                    cerradas);
            }
        } catch (RuntimeException e) {
            log.warn("No se pudieron cerrar las campañas vencidas: {}",
                e.getMessage());
        }
    }
}
