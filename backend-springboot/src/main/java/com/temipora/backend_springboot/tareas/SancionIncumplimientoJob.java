package com.temipora.backend_springboot.tareas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.temipora.backend_springboot.servicios.MoraService;
import com.temipora.backend_springboot.servicios.SancionIncumplimientoService;

/**
 * Agenda la escalada automática por incumplimiento de entrega. La lógica vive
 * en {@link SancionIncumplimientoService}.
 */
@Component
public class SancionIncumplimientoJob {

    private static final Logger log =
        LoggerFactory.getLogger(SancionIncumplimientoJob.class);

    private final SancionIncumplimientoService sancionIncumplimientoService;
    private final MoraService moraService;

    @Value("${temipora.incumplimiento.programado:true}")
    private boolean habilitado;

    public SancionIncumplimientoJob(
            SancionIncumplimientoService sancionIncumplimientoService,
            MoraService moraService) {
        this.sancionIncumplimientoService = sancionIncumplimientoService;
        this.moraService = moraService;
    }

    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT10M")
    public void ejecutar() {
        if (!habilitado) {
            return;
        }
        try {
            // Detecta la mora en la misma corrida para no depender del orden
            // entre jobs.
            int moras = moraService.detectar();
            SancionIncumplimientoService.Resumen resumen =
                sancionIncumplimientoService.procesar();
            if (moras > 0 || resumen.curados() + resumen.escalados()
                    + resumen.nivel1Aplicados() + resumen.pedidosCerrados() > 0) {
                log.info(
                    "Incumplimientos: {} moras, {} sanciones curadas, "
                        + "{} escaladas, {} nivel 1 aplicados, {} pedidos cerrados.",
                    moras, resumen.curados(), resumen.escalados(),
                    resumen.nivel1Aplicados(), resumen.pedidosCerrados());
            }
        } catch (Exception e) {
            log.error("Error al procesar sanciones por incumplimiento", e);
        }
    }
}
