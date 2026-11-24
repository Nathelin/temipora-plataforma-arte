package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.ConfiguracionPedido;
import com.temipora.backend_springboot.modelo.EstadoEntregaHito;
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
            HitoPedido hito = entregaPendienteService.hitoExigible(pedido);
            // Si ya entregó, la mora solo existe si la entrega fue tarde; en
            // ese caso quedó registrada al subir el entregable.
            if (hito != null
                    && hito.getEstadoEntrega() == EstadoEntregaHito.ENTREGADO) {
                continue;
            }
            pedido.setMoraDesde(plazo);
            pedidoRepository.save(pedido);

            notificacionService.crearParaUsuario(
                pedido.getArtista().getIdUsuario(),
                NOTIF_MORA,
                "Entrega atrasada",
                "El pedido de '" + nombreServicio(pedido)
                    + "' tiene una entrega pendiente desde el "
                    + plazo.toLocalDate() + ". Si no la subís, tus ventas"
                    + " quedan suspendidas.",
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
        curarMoraVencida(ahora);
        return total;
    }

    /**
     * Red de seguridad: si la mora se curó por una aprobación y el flag no se
     * limpió, el job lo deja consistente.
     */
    private void curarMoraVencida(LocalDateTime ahora) {
        for (Pedido pedido : pedidoRepository.findEnMora(EstadoPedido.EN_CURSO)) {
            if (entregaPendienteService.estaEnMora(pedido, ahora)) {
                continue;
            }
            pedido.setMoraDesde(null);
            pedidoRepository.save(pedido);
            auditoria.registrarSistema(
                AccionesAdmin.PEDIDO_MORA_CURAR, ENTIDAD, pedido.getIdPedido(),
                "Limpió la mora: el cliente aprobó los entregables pendientes");
        }
    }

    private String nombreServicio(Pedido pedido) {
        return pedido.getServicio() != null
                && pedido.getServicio().getNombre() != null
            ? pedido.getServicio().getNombre()
            : "tu pedido";
    }
}
