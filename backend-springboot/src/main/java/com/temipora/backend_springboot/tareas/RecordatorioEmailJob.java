package com.temipora.backend_springboot.tareas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.temipora.backend_springboot.servicios.RecordatorioService;

/**
 * Encola y envía los recordatorios de pagos y entregas. Corre cada quince
 * minutos; el envío de mails se puede deshabilitar por configuración.
 */
@Component
public class RecordatorioEmailJob {

    private static final Logger log =
        LoggerFactory.getLogger(RecordatorioEmailJob.class);

    private final RecordatorioService recordatorioService;

    @Value("${temipora.recordatorio-email.programado:true}")
    private boolean habilitado;

    public RecordatorioEmailJob(RecordatorioService recordatorioService) {
        this.recordatorioService = recordatorioService;
    }

    @Scheduled(initialDelayString = "PT3M", fixedDelayString = "PT15M")
    public void ejecutar() {
        if (!habilitado) {
            return;
        }
        try {
            int encolados = recordatorioService.encolar();
            int enviados = recordatorioService.procesarPendientes();
            if (encolados > 0 || enviados > 0) {
                log.info("Recordatorios: {} encolados, {} correos enviados.",
                    encolados, enviados);
            }
        } catch (Exception e) {
            log.error("Error al procesar recordatorios", e);
        }
    }
}
