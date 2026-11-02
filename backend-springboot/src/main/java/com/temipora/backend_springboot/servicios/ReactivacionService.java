package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public ReactivacionService(
            ConfiguracionInactividadService configuracionService,
            ReactivacionRepository reactivacionRepository,
            ServicioService servicioService,
            ReactivacionProcesador procesador) {
        this.configuracionService = configuracionService;
        this.reactivacionRepository = reactivacionRepository;
        this.servicioService = servicioService;
        this.procesador = procesador;
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
        return campanas;
    }

    @Scheduled(cron = "${temipora.reactivacion.cron:0 0 9 * * *}")
    public void ejecutarProgramado() {
        try {
            ejecutar();
        } catch (RuntimeException e) {
            log.error("Falló la corrida programada de reactivación.", e);
        }
    }
}
