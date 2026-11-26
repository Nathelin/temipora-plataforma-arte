package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.CompraItem;
import com.temipora.backend_springboot.modelo.CompraResumenDTO;
import com.temipora.backend_springboot.modelo.ConfiguracionResenia;
import com.temipora.backend_springboot.modelo.DineroCompradorDTO;
import com.temipora.backend_springboot.modelo.EstadoCompraGrupo;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.EstadoSolicitud;
import com.temipora.backend_springboot.modelo.MetricasCompradorDTO;
import com.temipora.backend_springboot.modelo.MiEstadoCuentaDTO;
import com.temipora.backend_springboot.modelo.PanelCompradorResumenDTO;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.ReseniaPendienteDTO;
import com.temipora.backend_springboot.modelo.SolicitudResumenDTO;
import com.temipora.backend_springboot.repositorios.CompraItemRepository;
import com.temipora.backend_springboot.repositorios.CompraRepository;
import com.temipora.backend_springboot.repositorios.PedidoRepository;

@Service
public class PanelCompradorService {

    private static final int MAX_ITEMS = 5;
    private static final int HORAS_PRESUPUESTO_POR_VENCER = 48;
    private static final List<EstadoPedido> ESTADOS_CON_RESENIA = List.of(
        EstadoPedido.FINALIZADO,
        EstadoPedido.INCUMPLIMIENTO);

    private final PedidoService pedidoService;
    private final CompraService compraService;
    private final SolicitudServicioService solicitudServicioService;
    private final MoraClienteService moraClienteService;
    private final ConversacionService conversacionService;
    private final ConfiguracionReseniaService configuracionReseniaService;
    private final PedidoRepository pedidoRepository;
    private final CompraItemRepository compraItemRepository;
    private final CompraRepository compraRepository;

    public PanelCompradorService(
            PedidoService pedidoService,
            CompraService compraService,
            SolicitudServicioService solicitudServicioService,
            MoraClienteService moraClienteService,
            ConversacionService conversacionService,
            ConfiguracionReseniaService configuracionReseniaService,
            PedidoRepository pedidoRepository,
            CompraItemRepository compraItemRepository,
            CompraRepository compraRepository) {
        this.pedidoService = pedidoService;
        this.compraService = compraService;
        this.solicitudServicioService = solicitudServicioService;
        this.moraClienteService = moraClienteService;
        this.conversacionService = conversacionService;
        this.configuracionReseniaService = configuracionReseniaService;
        this.pedidoRepository = pedidoRepository;
        this.compraItemRepository = compraItemRepository;
        this.compraRepository = compraRepository;
    }

    @Transactional(readOnly = true)
    public PanelCompradorResumenDTO resumen(Long idUsuario, String nombreUsuario) {
        LocalDateTime ahora = LocalDateTime.now();

        PedidoService.ActividadComprador actividad =
            pedidoService.actividadComprador(idUsuario);

        long comprasPendientes =
            compraService.contadores(nombreUsuario).getPendientes();
        List<CompraResumenDTO> compras = compraService
            .listar(nombreUsuario, 0, MAX_ITEMS, "pendientes")
            .getContenido();

        BigDecimal porPagarServicios = actividad.porPagar() != null
            ? actividad.porPagar() : BigDecimal.ZERO;
        BigDecimal porPagarCompras = compraRepository.sumarPendienteGrupos(
            idUsuario, EstadoCompraGrupo.PENDIENTE);
        if (porPagarCompras == null) {
            porPagarCompras = BigDecimal.ZERO;
        }

        List<SolicitudResumenDTO> presupuestos =
            solicitudServicioService.listarEnviadas(
                idUsuario, EstadoSolicitud.PRESUPUESTADA);
        LocalDateTime limiteVencimiento =
            ahora.plusHours(HORAS_PRESUPUESTO_POR_VENCER);
        long presupuestosPorVencer = presupuestos.stream()
            .filter(solicitud -> solicitud.getFechaVencimiento() != null
                && !solicitud.getFechaVencimiento()
                    .isAfter(limiteVencimiento))
            .count();
        List<SolicitudResumenDTO> presupuestosVisibles = presupuestos.stream()
            .sorted(Comparator.comparing(
                SolicitudResumenDTO::getFechaVencimiento,
                Comparator.nullsLast(
                    Comparator.<LocalDateTime>naturalOrder())))
            .limit(MAX_ITEMS)
            .toList();

        List<ReseniaPendienteDTO> resenias =
            reseniasPendientes(idUsuario, ahora);
        List<ReseniaPendienteDTO> reseniasVisibles = resenias.stream()
            .sorted(Comparator.comparing(
                ReseniaPendienteDTO::getFechaLimite,
                Comparator.nullsLast(
                    Comparator.<LocalDateTime>naturalOrder())))
            .limit(MAX_ITEMS)
            .toList();

        MiEstadoCuentaDTO estadoCuenta =
            moraClienteService.estadoCuenta(idUsuario);
        boolean hayEstadoCuenta = estadoCuenta != null
            && (estadoCuenta.isSancionado()
                || (estadoCuenta.getDeudas() != null
                    && !estadoCuenta.getDeudas().isEmpty()));

        return PanelCompradorResumenDTO.builder()
            .metricas(MetricasCompradorDTO.builder()
                .pedidosEnCurso(actividad.pedidosEnCurso())
                .pedidosPendientesPago(actividad.pedidosPendientesPago())
                .entregasPorAprobar(actividad.entregasPorAprobar())
                .pagosPendientes(actividad.pagosPendientes())
                .pagosVencidos(actividad.pagosVencidos())
                .comprasPendientes(comprasPendientes)
                .presupuestosRecibidos(presupuestos.size())
                .presupuestosPorVencer(presupuestosPorVencer)
                .reseniasPendientes(resenias.size())
                .build())
            .dinero(dinero(
                porPagarServicios, porPagarCompras))
            .mensajesNoLeidos(
                conversacionService.contador(idUsuario).getNoLeidos())
            .estadoCuenta(hayEstadoCuenta ? estadoCuenta : null)
            .pedidosActivos(actividad.pedidosActivos())
            .hitosAtencion(actividad.hitosAtencion())
            .comprasPendientes(compras)
            .presupuestosRecibidos(presupuestosVisibles)
            .reseniasPorDejar(reseniasVisibles)
            .build();
    }

    private DineroCompradorDTO dinero(
            BigDecimal porPagarServicios,
            BigDecimal porPagarCompras) {
        BigDecimal total = porPagarServicios.add(porPagarCompras);
        return DineroCompradorDTO.builder()
            .porPagarServicios(porPagarServicios.setScale(
                2, RoundingMode.HALF_UP))
            .porPagarCompras(porPagarCompras.setScale(
                2, RoundingMode.HALF_UP))
            .porPagarTotal(total.setScale(2, RoundingMode.HALF_UP))
            .moneda(MonedaVistaService.CODIGO_COBRO)
            .build();
    }

    private List<ReseniaPendienteDTO> reseniasPendientes(
            Long idUsuario, LocalDateTime ahora) {
        ConfiguracionResenia config = configuracionReseniaService.obtener();
        int dias = config != null && config.getDiasVentanaCreacion() != null
            ? config.getDiasVentanaCreacion()
            : 0;
        if (dias <= 0) {
            return List.of();
        }
        LocalDateTime desde = ahora.minusDays(dias);
        List<ReseniaPendienteDTO> items = new ArrayList<>();

        for (Pedido pedido : pedidoRepository
                .findFinalizadosSinReseniaCliente(
                    idUsuario, ESTADOS_CON_RESENIA, desde)) {
            items.add(ReseniaPendienteDTO.builder()
                .tipo("PEDIDO")
                .idPedido(pedido.getIdPedido())
                .titulo(pedido.getServicio() != null
                    ? pedido.getServicio().getNombre() : null)
                .contraparte(pedido.getArtista() != null
                    ? pedido.getArtista().getNombreUsuario() : null)
                .fechaLimite(pedido.getFinalizadoEn().plusDays(dias))
                .build());
        }

        Map<Long, ReseniaPendienteDTO> productos = new LinkedHashMap<>();
        for (CompraItem item : compraItemRepository
                .findPagadosSinReseniaProducto(
                    idUsuario,
                    EstadoCompraGrupo.PAGADO,
                    desde)) {
            if (item.getProducto() == null
                    || item.getProducto().getIdProducto() == null) {
                continue;
            }
            Long idProducto = item.getProducto().getIdProducto();
            if (productos.containsKey(idProducto)) {
                continue;
            }
            LocalDateTime fechaLimite = item.getGrupo() != null
                    && item.getGrupo().getFechaPago() != null
                ? item.getGrupo().getFechaPago().plusDays(dias)
                : null;
            productos.put(idProducto, ReseniaPendienteDTO.builder()
                .tipo("PRODUCTO")
                .idCompra(item.getGrupo() != null
                        && item.getGrupo().getCompra() != null
                    ? item.getGrupo().getCompra().getIdCompra() : null)
                .idProducto(idProducto)
                .titulo(item.getTituloProducto())
                .contraparte(item.getProducto().getArtista() != null
                    ? item.getProducto().getArtista().getNombreUsuario()
                    : null)
                .imagenUrl(compraService.imagenDeItem(item))
                .fechaLimite(fechaLimite)
                .build());
        }
        items.addAll(productos.values());
        return items;
    }
}
