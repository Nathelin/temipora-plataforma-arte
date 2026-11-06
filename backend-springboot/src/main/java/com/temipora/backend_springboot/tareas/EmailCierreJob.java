package com.temipora.backend_springboot.tareas;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.temipora.backend_springboot.servicios.CierreEmailService;

/**
 * Envía los mails de cierre agrupados por usuario. Corre cada cinco minutos;
 * la ventana de agrupación (por defecto 10) se configura desde el panel.
 */
@Component
public class EmailCierreJob {

    private final CierreEmailService cierreEmailService;

    @Value("${temipora.cierre-email.programado:true}")
    private boolean habilitado;

    public EmailCierreJob(CierreEmailService cierreEmailService) {
        this.cierreEmailService = cierreEmailService;
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void procesar() {
        if (habilitado) {
            cierreEmailService.procesarPendientes();
        }
    }
}
