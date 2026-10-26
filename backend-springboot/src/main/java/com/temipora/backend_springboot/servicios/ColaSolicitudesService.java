package com.temipora.backend_springboot.servicios;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.EstadoServicio;
import com.temipora.backend_springboot.modelo.EstadoSolicitud;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.Solicitud;
import com.temipora.backend_springboot.repositorios.SolicitudRepository;

@Service
public class ColaSolicitudesService {

    private static final List<EstadoSolicitud> ESTADOS_EN_COLA = List.of(
        EstadoSolicitud.PENDIENTE,
        EstadoSolicitud.EN_LISTA_ESPERA);

    private static final List<EstadoSolicitud> ESTADOS_QUE_OCUPAN_CUPO = List.of(
        EstadoSolicitud.PRESUPUESTADA,
        EstadoSolicitud.PAGADA);

    private final SolicitudRepository solicitudRepository;

    public ColaSolicitudesService(SolicitudRepository solicitudRepository) {
        this.solicitudRepository = solicitudRepository;
    }

    @Transactional(readOnly = true)
    public int cuposOcupados(Long idServicio) {
        long ocupados = solicitudRepository.countByServicioIdServicioAndEstadoIn(
            idServicio, ESTADOS_QUE_OCUPAN_CUPO);
        return (int) Math.min(ocupados, Integer.MAX_VALUE);
    }

    @Transactional(readOnly = true)
    public int cuposDisponibles(Servicio servicio) {
        int total = servicio != null && servicio.getCupos() != null
            ? servicio.getCupos()
            : 0;
        return Math.max(0, total - cuposOcupados(servicio.getIdServicio()));
    }

    @Transactional(readOnly = true)
    public List<Solicitud> cola(Long idServicio) {
        return solicitudRepository
            .findByServicioIdServicioAndEstadoInOrderByFechaCreacionAscIdSolicitudAsc(
                idServicio, ESTADOS_EN_COLA);
    }

    @Transactional(readOnly = true)
    public Integer posicion(Long idServicio, Long idSolicitud) {
        List<Solicitud> cola = cola(idServicio);
        for (int i = 0; i < cola.size(); i++) {
            if (Objects.equals(cola.get(i).getIdSolicitud(), idSolicitud)) {
                return i + 1;
            }
        }
        return null;
    }

    @Transactional(readOnly = true)
    public EvaluacionPresupuesto evaluar(Solicitud solicitud) {
        if (solicitud == null || solicitud.getServicio() == null) {
            return new EvaluacionPresupuesto(
                false, null, "No se pudo evaluar la solicitud para presupuestar.");
        }
        if (!ESTADOS_EN_COLA.contains(solicitud.getEstado())) {
            return new EvaluacionPresupuesto(
                false, null, "Esta solicitud ya no se puede presupuestar.");
        }

        Servicio servicio = solicitud.getServicio();
        if (servicio.getEstado() == EstadoServicio.ARCHIVADO) {
            return new EvaluacionPresupuesto(
                false, null, "El servicio está archivado y no admite presupuestos.");
        }

        int disponibles = cuposDisponibles(servicio);
        Integer posicion = posicion(servicio.getIdServicio(), solicitud.getIdSolicitud());

        if (disponibles <= 0) {
            return new EvaluacionPresupuesto(
                false,
                posicion,
                "No hay cupos libres. Podés ampliar los cupos del servicio o esperar a que se libere uno.");
        }
        if (posicion == null) {
            return new EvaluacionPresupuesto(
                false, null, "La solicitud no está en la cola de presupuestos.");
        }
        if (posicion > disponibles) {
            return new EvaluacionPresupuesto(
                false,
                posicion,
                "Hay solicitudes anteriores esperando cupo. Presupuestá primero la solicitud que está al frente de la cola.");
        }
        return new EvaluacionPresupuesto(true, posicion, null);
    }

    public boolean puedePresupuestar(Solicitud solicitud) {
        return evaluar(solicitud).puede();
    }

    public String motivoNoPresupuestable(Solicitud solicitud) {
        return evaluar(solicitud).motivo();
    }

    public record EvaluacionPresupuesto(boolean puede, Integer posicion, String motivo) {
    }
}
