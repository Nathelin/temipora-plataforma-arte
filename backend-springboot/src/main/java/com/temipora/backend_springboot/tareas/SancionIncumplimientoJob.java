package com.temipora.backend_springboot.tareas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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

    @Value("${temipora.incumplimiento.programado:true}")
    private boolean habilitado;

    public SancionIncumplimientoJob(
            SancionIncumplimientoService sancionIncumplimientoService) {
        this.sancionIncumplimientoService = sancionIncumplimientoService;
    }

    @Scheduled(initialDelayString = "PT5M", fixedDelayString = "PT1H")
    public void ejecutar() {
        if (!habilitado) {
            return;
        }
        try {
            SancionIncumplimientoService.Resumen resumen =
                sancionIncumplimientoService.procesar();
            if (resumen.curados() + resumen.escalados()
                    + resumen.nivel1Aplicados() + resumen.pedidosCerrados() > 0) {
                log.info(
                    "Incumplimientos: {} sanciones curadas, {} escaladas, "
                        + "{} nivel 1 aplicados, {} pedidos cerrados.",
                    resumen.curados(), resumen.escalados(),
                    resumen.nivel1Aplicados(), resumen.pedidosCerrados());
            }
        } catch (Exception e) {
            log.error("Error al procesar sanciones por incumplimiento", e);
        }
    }
}
