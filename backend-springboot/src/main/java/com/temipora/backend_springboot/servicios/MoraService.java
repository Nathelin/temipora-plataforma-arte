package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.ConfiguracionPedido;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.HitoPedido;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.repositorios.PedidoRepository;

/**
 * Detecta y registra la mora de entrega. La mora se congela en el pedido con el
 * plazo incumplido, no con el instante de la detección, para que un job que
 * corre tarde no corra la escalada.
 */
@Service
public class MoraService {

    private static final String NOTIF_MORA = "ENTREGA_ATRASADA";
    private static final String TIPO_REFERENCIA = "PEDIDO";
    private static final String ENTIDAD = "Pedido";

    private final PedidoRepository pedidoRepository;
    private final ConfiguracionPedidoService configuracionPedidoService;
    private final EntregaPendienteService entregaPendienteService;
    private final NotificacionService notificacionService;
    private final AuditoriaService auditoria;

    public MoraService(
            PedidoRepository pedidoRepository,
            ConfiguracionPedidoService configuracionPedidoService,
            EntregaPendienteService entregaPendienteService,
            NotificacionService notificacionService,
            AuditoriaService auditoria) {
        this.pedidoRepository = pedidoRepository;
        this.configuracionPedidoService = configuracionPedidoService;
        this.entregaPendienteService = entregaPendienteService;
        this.notificacionService = notificacionService;
        this.auditoria = auditoria;
    }

    @Transactional
    public int detectar() {
        LocalDateTime ahora = LocalDateTime.now();
        ConfiguracionPedido config = configuracionPedidoService.obtener();
        LocalDateTime corte = config.getSancionesAutomaticasDesde();
        List<Pedido> candidatos = pedidoRepository.findCandidatosMora(
            EstadoPedido.EN_CURSO, corte);

        int total = 0;
        for (Pedido pedido : candidatos) {
            LocalDateTime plazo = entregaPendienteService.plazoVigente(pedido);
            if (plazo == null || !ahora.isAfter(plazo)) {
                continue;
            }
            pedido.setMoraDesde(plazo);
            pedidoRepository.save(pedido);

            HitoPedido hito = entregaPendienteService.hitoExigible(pedido);
            notificacionService.crearParaUsuario(
                pedido.getArtista().getIdUsuario(),
                NOTIF_MORA,
                "Entrega atrasada",
                "El pedido de '" + nombreServicio(pedido)
                    + "' tiene una entrega pendiente desde el "
                    + plazo.toLocalDate() + ". Tenés "
                    + config.getDiasGraciaEntrega()
                    + (config.getDiasGraciaEntrega() == 1 ? " día" : " días")
                    + " antes de que se suspendan tus ventas. Si sigue la mora,"
                    + " la cuenta queda limitada solo a entregar.",
                pedido.getIdPedido(),
                TIPO_REFERENCIA);
            auditoria.registrarSistema(
                AccionesAdmin.PEDIDO_MORA_DETECTAR, ENTIDAD,
                pedido.getIdPedido(),
                "Registró la mora de entrega"
                    + (hito != null && hito.getHitoPresupuesto() != null
                        ? " del hito '" + hito.getHitoPresupuesto().getNombre() + "'"
                        : "")
                    + " (plazo vencido el " + plazo.toLocalDate() + ")");
            total++;
        }
        return total;
    }

    private String nombreServicio(Pedido pedido) {
        return pedido.getServicio() != null
                && pedido.getServicio().getNombre() != null
            ? pedido.getServicio().getNombre()
            : "tu pedido";
    }
}
