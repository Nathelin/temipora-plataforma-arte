package com.temipora.backend_springboot.tareas;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.temipora.backend_springboot.modelo.EstadoServicio;
import com.temipora.backend_springboot.modelo.EstadoSolicitud;
import com.temipora.backend_springboot.modelo.ModoRecepcionSolicitud;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.repositorios.ServicioRepository;
import com.temipora.backend_springboot.servicios.ModoRecepcionService;

/**
 * Red de seguridad del cambio automático de modo de recepción: si la
 * aplicación se cayó entre el commit y el listener, o si un flujo nuevo no
 * publicó el evento, este barrido diario lo corrige.
 */
@Component
public class ModoRecepcionJob {

    private static final List<EstadoSolicitud> ESTADOS_QUE_OCUPAN_CUPO = List.of(
        EstadoSolicitud.PRESUPUESTADA,
        EstadoSolicitud.PAGADA);

    private final ServicioRepository servicioRepository;
    private final ModoRecepcionService modoRecepcionService;

    @Value("${temipora.recepcion.reconciliacion-diaria:true}")
    private boolean habilitado;

    public ModoRecepcionJob(
            ServicioRepository servicioRepository,
            ModoRecepcionService modoRecepcionService) {
        this.servicioRepository = servicioRepository;
        this.modoRecepcionService = modoRecepcionService;
    }

    @Scheduled(cron = "0 10 4 * * *")
    public void reconciliarModos() {
        if (!habilitado) {
            return;
        }
        List<Servicio> llenos = servicioRepository.buscarLlenos(
            EstadoServicio.PUBLICADO,
            ModoRecepcionSolicitud.ABIERTA,
            ESTADOS_QUE_OCUPAN_CUPO);
        for (Servicio servicio : llenos) {
            modoRecepcionService.reconciliar(servicio.getIdServicio());
        }

        List<Servicio> reabribles = servicioRepository.buscarReabribles(
            EstadoServicio.PUBLICADO,
            ModoRecepcionSolicitud.SOLO_ESPERA,
            ModoRecepcionSolicitud.ABIERTA,
            ESTADOS_QUE_OCUPAN_CUPO);
        for (Servicio servicio : reabribles) {
            modoRecepcionService.reconciliar(servicio.getIdServicio());
        }
    }
}
