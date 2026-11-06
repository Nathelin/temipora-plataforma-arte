package com.temipora.backend_springboot.servicios;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.temipora.backend_springboot.eventos.CuposServicioCambiadosEvent;
import com.temipora.backend_springboot.modelo.EstadoServicio;
import com.temipora.backend_springboot.modelo.EstadoSolicitud;
import com.temipora.backend_springboot.modelo.ModoRecepcionSolicitud;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.repositorios.ServicioRepository;
import com.temipora.backend_springboot.repositorios.SolicitudRepository;

/**
 * Mantiene el modo de recepción alineado con los cupos: cuando se ocupa el
 * último, el servicio pasa a lista de espera; cuando se libera uno, vuelve a
 * abierto. Solo actúa sobre servicios cuya preferencia era ABIERTA, así nunca
 * pisa un SOLO_ESPERA elegido por el artista.
 */
@Service
public class ModoRecepcionService {

    private static final List<EstadoSolicitud> ESTADOS_QUE_OCUPAN_CUPO = List.of(
        EstadoSolicitud.PRESUPUESTADA,
        EstadoSolicitud.PAGADA);

    private static final String ENTIDAD = "Servicio";
    private static final List<CampoAuditado> CAMPOS = List.of(
        CampoAuditado.de("modoRecepcion", "Modo de recepción"),
        CampoAuditado.de("modoRecepcionPreferido", "Modo preferido"));

    private final ServicioRepository servicioRepository;
    private final SolicitudRepository solicitudRepository;
    private final AuditoriaService auditoria;

    public ModoRecepcionService(
            ServicioRepository servicioRepository,
            SolicitudRepository solicitudRepository,
            AuditoriaService auditoria) {
        this.servicioRepository = servicioRepository;
        this.solicitudRepository = solicitudRepository;
        this.auditoria = auditoria;
    }

    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT,
        fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alCambiarCupos(CuposServicioCambiadosEvent evento) {
        if (evento == null || evento.idServicio() == null) {
            return;
        }
        reconciliar(evento.idServicio());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reconciliar(Long idServicio) {
        Servicio servicio = servicioRepository.findByIdServicioForUpdate(idServicio)
            .orElse(null);
        if (servicio == null || servicio.getEstado() != EstadoServicio.PUBLICADO) {
            return;
        }

        ModoRecepcionSolicitud preferido = servicio.getModoRecepcionPreferido() != null
            ? servicio.getModoRecepcionPreferido()
            : servicio.getModoRecepcion();
        if (preferido != ModoRecepcionSolicitud.ABIERTA) {
            return;
        }

        int total = servicio.getCupos() == null ? 0 : servicio.getCupos();
        long ocupados = solicitudRepository.countByServicioIdServicioAndEstadoIn(
            idServicio, ESTADOS_QUE_OCUPAN_CUPO);
        int disponibles = (int) Math.max(0, total - ocupados);

        if (disponibles <= 0) {
            if (servicio.getModoRecepcion() != ModoRecepcionSolicitud.ABIERTA) {
                return;
            }
            Snapshot antes = Snapshot.de(servicio, CAMPOS);
            servicio.setModoRecepcion(ModoRecepcionSolicitud.SOLO_ESPERA);
            servicio.setModoRecepcionPreferido(ModoRecepcionSolicitud.ABIERTA);
            servicio.setPermiteListaEspera(true);
            servicioRepository.save(servicio);
            auditoria.registrar(
                AccionesAdmin.SERVICIO_MODO_AUTOMATICO, ENTIDAD, servicio.getIdServicio(),
                "Pasó a lista de espera automáticamente: no quedan cupos libres.",
                antes, servicio, CAMPOS);
            return;
        }

        if (servicio.getModoRecepcion() == ModoRecepcionSolicitud.SOLO_ESPERA) {
            Snapshot antes = Snapshot.de(servicio, CAMPOS);
            servicio.setModoRecepcion(ModoRecepcionSolicitud.ABIERTA);
            servicio.setModoRecepcionPreferido(null);
            servicio.setPermiteListaEspera(false);
            servicioRepository.save(servicio);
            auditoria.registrar(
                AccionesAdmin.SERVICIO_MODO_AUTOMATICO, ENTIDAD, servicio.getIdServicio(),
                "Volvió a recepción abierta al liberarse un cupo.",
                antes, servicio, CAMPOS);
        }
    }
}
