package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.EstadoSolicitud;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.Solicitud;
import com.temipora.backend_springboot.repositorios.PedidoRepository;
import com.temipora.backend_springboot.repositorios.SolicitudRepository;

/**
 * Cierre único del pedido. Se llama solo cuando el trabajo terminó:
 * el cliente aprobó la entrega de un pago único al inicio, o se pagó
 * la entrega final. Nunca al acreditar la seña.
 */
@Service
public class PedidoCierreService {

    private static final String NOTIFICACION_PEDIDO_FINALIZADO = "PEDIDO_FINALIZADO";
    private static final String TIPO_REFERENCIA_PEDIDO = "PEDIDO";

    private final PedidoRepository pedidoRepository;
    private final SolicitudRepository solicitudRepository;
    private final NotificacionService notificacionService;

    public PedidoCierreService(
            PedidoRepository pedidoRepository,
            SolicitudRepository solicitudRepository,
            NotificacionService notificacionService) {
        this.pedidoRepository = pedidoRepository;
        this.solicitudRepository = solicitudRepository;
        this.notificacionService = notificacionService;
    }

    @Transactional
    public void finalizar(Pedido pedido, LocalDateTime ahora) {
        if (pedido.getEstado() == EstadoPedido.FINALIZADO) {
            return;
        }

        pedido.setEstado(EstadoPedido.FINALIZADO);
        pedido.setFinalizadoEn(ahora);
        pedidoRepository.save(pedido);

        Solicitud solicitud = pedido.getSolicitud();
        if (solicitud != null) {
            solicitud.setEstado(EstadoSolicitud.FINALIZADA);
            solicitud.setFechaActualizacion(ahora);
            solicitudRepository.save(solicitud);
        }

        notificarFinalizado(pedido);
    }

    private void notificarFinalizado(Pedido pedido) {
        String descripcion = "El pedido de '" + nombreServicio(pedido)
            + "' se completó.";
        notificacionService.crearParaUsuario(
            pedido.getCliente().getIdUsuario(),
            NOTIFICACION_PEDIDO_FINALIZADO,
            "Pedido finalizado",
            descripcion,
            pedido.getIdPedido(),
            TIPO_REFERENCIA_PEDIDO);
        notificacionService.crearParaUsuario(
            pedido.getArtista().getIdUsuario(),
            NOTIFICACION_PEDIDO_FINALIZADO,
            "Pedido finalizado",
            descripcion,
            pedido.getIdPedido(),
            TIPO_REFERENCIA_PEDIDO);
    }

    private String nombreServicio(Pedido pedido) {
        return pedido.getServicio() != null
                && pedido.getServicio().getNombre() != null
            ? pedido.getServicio().getNombre()
            : "tu pedido";
    }
}
