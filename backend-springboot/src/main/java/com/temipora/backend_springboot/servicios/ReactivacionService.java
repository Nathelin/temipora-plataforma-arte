package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.temipora.backend_springboot.modelo.ConfiguracionInactividad;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.repositorios.ReactivacionRepository;

@Service
public class ReactivacionService {

    private static final Logger log =
        LoggerFactory.getLogger(ReactivacionService.class);

    private final ConfiguracionInactividadService configuracionService;
    private final ReactivacionRepository reactivacionRepository;
    private final ServicioService servicioService;
    private final ReactivacionProcesador procesador;
    private final AuditoriaService auditoria;

    @Value("${temipora.reactivacion.programada:true}")
    private boolean programadaHabilitada;

    public ReactivacionService(
            ConfiguracionInactividadService configuracionService,
            ReactivacionRepository reactivacionRepository,
            ServicioService servicioService,
            ReactivacionProcesador procesador,
            AuditoriaService auditoria) {
        this.configuracionService = configuracionService;
        this.reactivacionRepository = reactivacionRepository;
        this.servicioService = servicioService;
        this.procesador = procesador;
        this.auditoria = auditoria;
    }

    public int ejecutar() {
        ConfiguracionInactividad config = configuracionService.obtener();
        LocalDateTime ahora = LocalDateTime.now();

        List<Long> candidatosIds = reactivacionRepository.candidatos(
            ahora.minusDays(Math.max(0, config.getDiasMinimosCuenta())),
            ahora.minusDays(Math.max(0, config.getDiasSinCompra())),
            ahora.minusDays(Math.max(0, config.getDiasSinConexion())),
            ahora.minusDays(Math.max(0, config.getDiasCooldown())),
            Math.max(1, config.getMaxUsuariosPorCorrida()));

        if (candidatosIds.isEmpty()) {
            log.info("Reactivación: no hay usuarios inactivos para procesar.");
            return 0;
        }

        List<Servicio> candidatos = servicioService.candidatosDisponibles();
        if (candidatos.isEmpty()) {
            log.info("Reactivación: no hay servicios disponibles para recomendar.");
            return 0;
        }

        int campanas = 0;
        for (Long idUsuario : candidatosIds) {
            try {
                campanas += procesador.procesar(idUsuario, config, candidatos, ahora);
            } catch (RuntimeException e) {
                log.warn("No se pudo generar la campaña del usuario {}: {}",
                    idUsuario, e.getMessage());
            }
        }
        log.info("Reactivación: {} campaña(s) generadas sobre {} candidato(s).",
            campanas, candidatosIds.size());
        registrarAuditoria(config, campanas);
        return campanas;
    }

    private void registrarAuditoria(ConfiguracionInactividad config, int campanas) {
        try {
            auditoria.registrarSistema(
                AccionesAdmin.REACTIVACION_EJECUTAR,
                "ConfiguracionInactividad",
                config.getIdConfiguracion(),
                "Ejecutó promociones por inactividad: " + campanas + " campaña(s).");
        } catch (RuntimeException e) {
            log.warn("No se pudo registrar la auditoría de la reactivación: {}", e.getMessage());
        }
    }

    // Tick liviano: cada minuto decide si ya es la hora configurada y si aún no corrió hoy.
    @Scheduled(cron = "0 * * * * *")
    public void evaluarEjecucionProgramada() {
        if (!programadaHabilitada) {
            return;
        }
        ConfiguracionInactividad config = configuracionService.obtener();
        LocalDateTime ahora = LocalDateTime.now();
        if (!correspondeEjecutar(config, ahora)) {
            return;
        }
        configuracionService.marcarCorridaProgramada(ahora);
        try {
            ejecutar();
        } catch (RuntimeException e) {
            log.error("Falló la corrida programada de reactivación.", e);
        }
    }

    boolean correspondeEjecutar(
            ConfiguracionInactividad config,
            LocalDateTime ahora) {
        if (config == null || config.getHoraEjecucion() == null) {
            return false;
        }
        if (ahora.toLocalTime().isBefore(config.getHoraEjecucion())) {
            return false;
        }
        LocalDateTime horarioDeHoy = LocalDateTime.of(
            ahora.toLocalDate(), config.getHoraEjecucion());
        return config.getUltimaCorridaProgramada() == null
            || config.getUltimaCorridaProgramada().isBefore(horarioDeHoy);
    }
}
