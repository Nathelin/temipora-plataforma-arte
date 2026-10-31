package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.ContadorMensajesDTO;
import com.temipora.backend_springboot.modelo.DineroPanelDTO;
import com.temipora.backend_springboot.modelo.EstadoPagoHito;
import com.temipora.backend_springboot.modelo.EstadoSolicitud;
import com.temipora.backend_springboot.modelo.HitoPedido;
import com.temipora.backend_springboot.modelo.MetricasPanelDTO;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.PanelArtistaResumenDTO;
import com.temipora.backend_springboot.modelo.SolicitudResumenDTO;
import com.temipora.backend_springboot.repositorios.HitoPedidoRepository;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoOAuthService;

@Service
public class PanelArtistaService {

    private static final int MAX_SOLICITUDES = 5;
    private static final int HORAS_PRESUPUESTO_POR_VENCER = 48;
    private static final List<EstadoSolicitud> ESTADOS_VISIBLES = List.of(
        EstadoSolicitud.PENDIENTE,
        EstadoSolicitud.EN_LISTA_ESPERA,
        EstadoSolicitud.PRESUPUESTADA,
        EstadoSolicitud.PAGADA);

    private final SolicitudServicioService solicitudServicioService;
    private final PedidoService pedidoService;
    private final HitoPedidoRepository hitoPedidoRepository;
    private final ConversacionService conversacionService;
    private final MercadoPagoOAuthService mercadoPagoOAuthService;
    private final MonedaVistaService monedaVistaService;

    public PanelArtistaService(
            SolicitudServicioService solicitudServicioService,
            PedidoService pedidoService,
            HitoPedidoRepository hitoPedidoRepository,
            ConversacionService conversacionService,
            MercadoPagoOAuthService mercadoPagoOAuthService,
            MonedaVistaService monedaVistaService) {
        this.solicitudServicioService = solicitudServicioService;
        this.pedidoService = pedidoService;
        this.hitoPedidoRepository = hitoPedidoRepository;
        this.conversacionService = conversacionService;
        this.mercadoPagoOAuthService = mercadoPagoOAuthService;
        this.monedaVistaService = monedaVistaService;
    }

    @Transactional(readOnly = true)
    public PanelArtistaResumenDTO resumen(Long idArtista) {
        LocalDateTime ahora = LocalDateTime.now();
        Moneda mostrada = monedaVistaService.preferidaDe(idArtista);

        List<SolicitudResumenDTO> recibidas =
            solicitudServicioService.listarRecibidas(idArtista, null);

        List<SolicitudResumenDTO> porResponder = recibidas.stream()
            .filter(solicitud ->
                solicitud.getEstado() == EstadoSolicitud.PENDIENTE)
            .sorted(Comparator.comparing(
                SolicitudResumenDTO::getFechaCreacion,
                Comparator.nullsLast(
                    Comparator.<LocalDateTime>naturalOrder())))
            .toList();

        long enEspera = recibidas.stream()
            .filter(solicitud ->
                solicitud.getEstado() == EstadoSolicitud.EN_LISTA_ESPERA)
            .count();

        LocalDateTime limitePresupuesto =
            ahora.plusHours(HORAS_PRESUPUESTO_POR_VENCER);
        List<SolicitudResumenDTO> porVencer = recibidas.stream()
            .filter(solicitud ->
                solicitud.getEstado() == EstadoSolicitud.PRESUPUESTADA)
            .filter(solicitud -> solicitud.getFechaVencimiento() != null
                && !solicitud.getFechaVencimiento()
                    .isAfter(limitePresupuesto))
            .sorted(Comparator.comparing(
                SolicitudResumenDTO::getFechaVencimiento,
                Comparator.nullsLast(
                    Comparator.<LocalDateTime>naturalOrder())))
            .toList();

        List<SolicitudResumenDTO> recientes = recibidas.stream()
            .filter(solicitud ->
                ESTADOS_VISIBLES.contains(solicitud.getEstado()))
            .limit(MAX_SOLICITUDES)
            .toList();

        PedidoService.ActividadArtista actividad =
            pedidoService.actividadArtista(idArtista);

        ContadorMensajesDTO contador = conversacionService.contador(idArtista);

        return PanelArtistaResumenDTO.builder()
            .metricas(MetricasPanelDTO.builder()
                .pedidosEnCurso(actividad.pedidosEnCurso())
                .pedidosPendientesPago(actividad.pedidosPendientesPago())
                .pedidosImpagos(actividad.pedidosImpagos())
                .entregasPendientes(actividad.entregasPendientes())
                .entregasConRevision(actividad.entregasConRevision())
                .solicitudesPorResponder(porResponder.size())
                .solicitudesEnEspera(enEspera)
                .presupuestosPorVencer(porVencer.size())
                .hitosPorVencer(actividad.hitosPorVencer())
                .build())
            .dinero(dinero(idArtista, actividad, mostrada, ahora))
            .mensajesNoLeidos(contador.getNoLeidos())
            .solicitudesMensajePendientes(
                contador.getSolicitudesPendientes())
            .cuentaCobrosConectada(
                mercadoPagoOAuthService.estado(idArtista).conectada())
            .pedidosActivos(actividad.pedidosActivos())
            .hitosAtencion(actividad.hitosAtencion())
            .solicitudesRecientes(recientes)
            .solicitudesPorResponder(
                porResponder.stream().limit(MAX_SOLICITUDES).toList())
            .presupuestosPorVencer(
                porVencer.stream().limit(MAX_SOLICITUDES).toList())
            .build();
    }

    private DineroPanelDTO dinero(
            Long idArtista,
            PedidoService.ActividadArtista actividad,
            Moneda mostrada,
            LocalDateTime ahora) {
        List<HitoPedido> pagados = hitoPedidoRepository
            .findPagadosDeArtista(idArtista, EstadoPagoHito.PAGADO);
        YearMonth mes = YearMonth.from(ahora);
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal delMes = BigDecimal.ZERO;

        for (HitoPedido hito : pagados) {
            BigDecimal neto = pedidoService.netoMostradoDe(
                hito, mostrada, ahora);
            if (neto == null) {
                continue;
            }
            total = total.add(neto);
            if (hito.getFechaPago() != null
                    && YearMonth.from(hito.getFechaPago()).equals(mes)) {
                delMes = delMes.add(neto);
            }
        }

        return DineroPanelDTO.builder()
            .porCobrar(actividad.porCobrar())
            .cobradoMes(delMes)
            .cobradoTotal(total)
            .monedaMostrada(mostrada != null ? mostrada.getCodigo() : null)
            .build();
    }
}
