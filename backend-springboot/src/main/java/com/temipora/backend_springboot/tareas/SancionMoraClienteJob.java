package com.temipora.backend_springboot.tareas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.temipora.backend_springboot.servicios.SancionMoraClienteService;

/**
 * Agenda la escalada automática por impago del cliente. La lógica vive en
 * {@link SancionMoraClienteService}.
 */
@Component
public class SancionMoraClienteJob {

    private static final Logger log =
        LoggerFactory.getLogger(SancionMoraClienteJob.class);

    private final SancionMoraClienteService sancionMoraClienteService;

    @Value("${temipora.impago-cliente.programado:true}")
    private boolean habilitado;

    public SancionMoraClienteJob(
            SancionMoraClienteService sancionMoraClienteService) {
        this.sancionMoraClienteService = sancionMoraClienteService;
    }

    @Scheduled(initialDelayString = "PT9M", fixedDelayString = "PT1H")
    public void ejecutar() {
        if (!habilitado) {
            return;
        }
        try {
            SancionMoraClienteService.Resumen resumen =
                sancionMoraClienteService.procesar();
            if (resumen.curados() + resumen.escalados()
                    + resumen.nivel1Aplicados() > 0) {
                log.info(
                    "Impagos del cliente: {} sanciones curadas, {} escaladas, "
                        + "{} nivel 1 aplicados.",
                    resumen.curados(), resumen.escalados(),
                    resumen.nivel1Aplicados());
            }
        } catch (Exception e) {
            log.error("Error al procesar sanciones por impago del cliente", e);
        }
    }
}
