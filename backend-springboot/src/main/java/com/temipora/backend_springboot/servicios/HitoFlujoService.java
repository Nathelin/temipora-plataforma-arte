package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.ConfiguracionPedido;
import com.temipora.backend_springboot.modelo.HitoPedido;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.TipoHito;
import com.temipora.backend_springboot.repositorios.HitoPedidoRepository;

/**
 * Reglas comunes del flujo de hitos que comparten el pago y la aprobación.
 */
@Service
public class HitoFlujoService {

    private static final String NOTIFICACION_CUOTA_PENDIENTE = "CUOTA_PENDIENTE";
    private static final String TIPO_REFERENCIA_PEDIDO = "PEDIDO";

    private final HitoPedidoRepository hitoPedidoRepository;
    private final ConfiguracionPedidoService configuracionPedidoService;
    private final NotificacionService notificacionService;

    public HitoFlujoService(
            HitoPedidoRepository hitoPedidoRepository,
            ConfiguracionPedidoService configuracionPedidoService,
            NotificacionService notificacionService) {
        this.hitoPedidoRepository = hitoPedidoRepository;
        this.configuracionPedidoService = configuracionPedidoService;
        this.notificacionService = notificacionService;
    }

    /**
     * Habilita el hito siguiente. Las cuotas anticipadas reciben vencimiento y
     * notificación; la entrega final arranca cuando el artista la produce y su
     * plazo de pago se fija recién al entregarla.
     */
    @Transactional
    public HitoPedido habilitarSiguiente(
            Pedido pedido, HitoPedido actual, LocalDateTime ahora) {
        HitoPedido siguiente = hitoPedidoRepository
            .findFirstByPedidoIdPedidoAndOrdenGreaterThanOrderByOrdenAsc(
                pedido.getIdPedido(), actual.getOrden())
            .orElse(null);
        if (siguiente == null) {
            return null;
        }
        if (siguiente.getHitoPresupuesto().getTipo() != TipoHito.ENTREGA) {
            // Un pedido en mora congela su escalera de pagos: el hito siguiente
            // existe, pero sin plazo de pago no se puede abonar.
            if (pedido.getMoraDesde() != null) {
                return siguiente;
            }
            siguiente.setFechaLimitePago(
                PedidoFechas.ventanaPago(pedido, ahora, diasPagoCuota()));
            hitoPedidoRepository.save(siguiente);
            notificacionService.crearParaUsuario(
                pedido.getCliente().getIdUsuario(),
                NOTIFICACION_CUOTA_PENDIENTE,
                "Cuota habilitada",
                "Se habilitó el pago de '"
                    + siguiente.getHitoPresupuesto().getNombre()
                    + "' del pedido de '" + nombreServicio(pedido) + "'.",
                pedido.getIdPedido(),
                TIPO_REFERENCIA_PEDIDO);
        }
        return siguiente;
    }

    public int diasPagoCuota() {
        ConfiguracionPedido config = configuracionPedidoService.obtener();
        return config.getDiasPagoCuota() != null
            ? config.getDiasPagoCuota()
            : ConfiguracionPedido.DIAS_PAGO_POR_DEFECTO;
    }

    public int diasAprobacion() {
        ConfiguracionPedido config = configuracionPedidoService.obtener();
        return config.getDiasAprobacionAutomatica() != null
            ? config.getDiasAprobacionAutomatica()
            : ConfiguracionPedido.DIAS_APROBACION_POR_DEFECTO;
    }

    public int revisionesMaximasPorHito() {
        ConfiguracionPedido config = configuracionPedidoService.obtener();
        return config.getRevisionesMaximasPorHito() != null
            ? config.getRevisionesMaximasPorHito()
            : ConfiguracionPedido.REVISIONES_MAXIMAS_POR_DEFECTO;
    }

    public int diasReentrega() {
        ConfiguracionPedido config = configuracionPedidoService.obtener();
        return config.getDiasReentrega() != null
            ? config.getDiasReentrega()
            : ConfiguracionPedido.DIAS_REENTREGA_POR_DEFECTO;
    }

    private String nombreServicio(Pedido pedido) {
        return pedido.getServicio() != null && pedido.getServicio().getNombre() != null
            ? pedido.getServicio().getNombre()
            : "tu pedido";
    }
}
