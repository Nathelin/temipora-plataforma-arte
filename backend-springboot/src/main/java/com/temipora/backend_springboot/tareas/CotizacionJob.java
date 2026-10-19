package com.temipora.backend_springboot.tareas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.temipora.backend_springboot.modelo.ConfiguracionCotizacion;
import com.temipora.backend_springboot.servicios.CotizacionService;

@Component
public class CotizacionJob {

    private static final Logger log = LoggerFactory.getLogger(CotizacionJob.class);

    private final CotizacionService cotizacionService;

    public CotizacionJob(CotizacionService cotizacionService) {
        this.cotizacionService = cotizacionService;
    }

    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT24H")
    public void actualizarCotizacion() {
        try {
            ConfiguracionCotizacion config = cotizacionService.obtenerConfiguracion();
            if (!config.isApiHabilitada()) {
                return;
            }
            cotizacionService.refrescarDesdeApi();
        } catch (Exception e) {
            log.warn("No se pudo actualizar la cotización desde la API: {}", e.getMessage());
        }
    }
}
