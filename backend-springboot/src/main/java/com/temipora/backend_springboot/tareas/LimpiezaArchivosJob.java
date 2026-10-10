package com.temipora.backend_springboot.tareas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.temipora.backend_springboot.servicios.LimpiezaArchivosService;

@Component
public class LimpiezaArchivosJob {

    private static final Logger log = LoggerFactory.getLogger(LimpiezaArchivosJob.class);

    private final LimpiezaArchivosService limpiezaArchivosService;

    public LimpiezaArchivosJob(LimpiezaArchivosService limpiezaArchivosService) {
        this.limpiezaArchivosService = limpiezaArchivosService;
    }

    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT10M")
    public void limpiarArchivosHuerfanos() {
        try {
            int borrados = limpiezaArchivosService.limpiarHuerfanos();
            if (borrados > 0) {
                log.info("Limpieza de archivos huérfanos: {} archivo(s) eliminado(s)", borrados);
            }
        } catch (Exception e) {
            log.warn("No se pudo ejecutar la limpieza de archivos huérfanos", e);
        }
    }
}
