package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.ContadorMensajesDTO;
import com.temipora.backend_springboot.modelo.DineroPanelDTO;
import com.temipora.backend_springboot.modelo.EstadoSolicitud;
import com.temipora.backend_springboot.modelo.MetricasPanelDTO;
import com.temipora.backend_springboot.modelo.PanelArtistaResumenDTO;
import com.temipora.backend_springboot.modelo.PedidoResumenDTO;
import com.temipora.backend_springboot.modelo.SolicitudResumenDTO;
import com.temipora.backend_springboot.repositorios.CompraGrupoRepository;
import com.temipora.backend_springboot.repositorios.PagoRepository;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoOAuthService;

@Service
public class PanelArtistaService {

    private static final int MAX_SOLICITUDES = 5;
    private static final int HORAS_PRESUPUESTO_POR_VENCER = 48;
    private static final List<EstadoSolicitud> ESTADOS_ACTIVOS = List.of(
        EstadoSolicitud.PENDIENTE,
        EstadoSolicitud.EN_LISTA_ESPERA,
        EstadoSolicitud.PRESUPUESTADA,
        EstadoSolicitud.PAGADA);

    private final SolicitudServicioService solicitudServicioService;
    private final PedidoService pedidoService;
    private final PagoRepository pagoRepository;
    private final CompraGrupoRepository compraGrupoRepository;
    private final ConversacionService conversacionService;
    private final MercadoPagoOAuthService mercadoPagoOAuthService;
    private final SaludCuentaService saludCuentaService;

    public PanelArtistaService(
            SolicitudServicioService solicitudServicioService,
            PedidoService pedidoService,
            PagoRepository pagoRepository,
            CompraGrupoRepository compraGrupoRepository,
            ConversacionService conversacionService,
            MercadoPagoOAuthService mercadoPagoOAuthService,
            SaludCuentaService saludCuentaService) {
        this.solicitudServicioService = solicitudServicioService;
        this.pedidoService = pedidoService;
        this.pagoRepository = pagoRepository;
        this.compraGrupoRepository = compraGrupoRepository;
        this.conversacionService = conversacionService;
        this.mercadoPagoOAuthService = mercadoPagoOAuthService;
        this.saludCuentaService = saludCuentaService;
    }

    @Transactional(readOnly = true)
    public PanelArtistaResumenDTO resumen(Long idArtista) {
        LocalDateTime ahora = LocalDateTime.now();

        List<SolicitudResumenDTO> recibidas =
            solicitudServicioService.listarRecibidasEnEstados(
                idArtista, ESTADOS_ACTIVOS);

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
            .limit(MAX_SOLICITUDES)
            .toList();

        PedidoService.ActividadArtista actividad =
            pedidoService.actividadArtista(idArtista);
        SaludCuentaService.Salud salud = saludCuentaService.salud(idArtista);
        for (PedidoResumenDTO pedido : actividad.pedidosActivos()) {
            pedido.setDiasGraciaRestantes(
                salud.diasGraciaPorPedido().get(pedido.getIdPedido()));
        }

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
            .dinero(dinero(idArtista, actividad, ahora))
            .mensajesNoLeidos(contador.getNoLeidos())
            .solicitudesMensajePendientes(
                contador.getSolicitudesPendientes())
            .cuentaCobrosConectada(
                mercadoPagoOAuthService.estado(idArtista).conectada())
            .saludCuenta(salud.dto())
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
            LocalDateTime ahora) {
        YearMonth mes = YearMonth.from(ahora);
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal delMes = BigDecimal.ZERO;

        for (Object[] fila : pagoRepository.netosRealesDeArtista(idArtista)) {
            BigDecimal neto = CalculoLiquidacion.netoRecibido(
                decimal(fila[0]), decimal(fila[1]), decimal(fila[2]));
            LocalDateTime fecha = fecha(fila[3]);
            total = total.add(neto);
            if (fecha != null && YearMonth.from(fecha).equals(mes)) {
                delMes = delMes.add(neto);
            }
        }

        for (Object[] fila : compraGrupoRepository
                .netosRealesDeArtista(idArtista)) {
            BigDecimal neto = CalculoLiquidacion.netoRecibido(
                decimal(fila[0]), decimal(fila[1]), decimal(fila[2]));
            LocalDateTime fecha = fecha(fila[3]);
            total = total.add(neto);
            if (fecha != null && YearMonth.from(fecha).equals(mes)) {
                delMes = delMes.add(neto);
            }
        }

        return DineroPanelDTO.builder()
            .porCobrar(actividad.porCobrar())
            .cobradoMes(delMes.setScale(2, RoundingMode.HALF_UP))
            .cobradoTotal(total.setScale(2, RoundingMode.HALF_UP))
            .monedaMostrada(MonedaVistaService.CODIGO_COBRO)
            .build();
    }

    private static BigDecimal decimal(Object valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        if (valor instanceof BigDecimal decimal) {
            return decimal;
        }
        if (valor instanceof Number numero) {
            return new BigDecimal(numero.toString());
        }
        return new BigDecimal(valor.toString());
    }

    private static LocalDateTime fecha(Object valor) {
        if (valor instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (valor instanceof LocalDateTime fechaHora) {
            return fechaHora;
        }
        if (valor instanceof LocalDate fecha) {
            return fecha.atStartOfDay();
        }
        if (valor instanceof java.util.Date fecha) {
            return LocalDateTime.ofInstant(
                fecha.toInstant(), ZoneId.systemDefault());
        }
        return null;
    }
}
