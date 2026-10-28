package com.temipora.backend_springboot.servicios;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.EstadoEntregaHito;
import com.temipora.backend_springboot.modelo.EstadoPago;
import com.temipora.backend_springboot.modelo.EstadoPagoHito;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.EstadoSolicitud;
import com.temipora.backend_springboot.modelo.HitoPedido;
import com.temipora.backend_springboot.modelo.Pago;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.Presupuesto;
import com.temipora.backend_springboot.modelo.Solicitud;
import com.temipora.backend_springboot.modelo.TipoHito;
import com.temipora.backend_springboot.repositorios.HitoPedidoRepository;
import com.temipora.backend_springboot.repositorios.PagoRepository;
import com.temipora.backend_springboot.repositorios.PedidoRepository;
import com.temipora.backend_springboot.repositorios.SolicitudRepository;
import com.temipora.backend_springboot.servicios.pagos.ProveedorPago;
import com.temipora.backend_springboot.servicios.pagos.ResultadoCobro;

@Service
public class PagoService {

    private static final String TIPO_REFERENCIA_PEDIDO = "PEDIDO";
    private static final String NOTIFICACION_PAGO_RECIBIDO = "PAGO_RECIBIDO";
    private static final String NOTIFICACION_PEDIDO_FINALIZADO = "PEDIDO_FINALIZADO";
    private static final String NOTIFICACION_PAGO_RECHAZADO = "PAGO_RECHAZADO";

    private final PagoRepository pagoRepository;
    private final HitoPedidoRepository hitoPedidoRepository;
    private final PedidoRepository pedidoRepository;
    private final SolicitudRepository solicitudRepository;
    private final NotificacionService notificacionService;
    private final HitoFlujoService hitoFlujoService;
    private final ProveedorPago proveedorPago;

    public PagoService(
            PagoRepository pagoRepository,
            HitoPedidoRepository hitoPedidoRepository,
            PedidoRepository pedidoRepository,
            SolicitudRepository solicitudRepository,
            NotificacionService notificacionService,
            HitoFlujoService hitoFlujoService,
            ProveedorPago proveedorPago) {
        this.pagoRepository = pagoRepository;
        this.hitoPedidoRepository = hitoPedidoRepository;
        this.pedidoRepository = pedidoRepository;
        this.solicitudRepository = solicitudRepository;
        this.notificacionService = notificacionService;
        this.hitoFlujoService = hitoFlujoService;
        this.proveedorPago = proveedorPago;
    }

    @Transactional
    public Pago iniciarPago(HitoPedido hito, Pedido pedido) {
        if (hito.getEstadoPago() != EstadoPagoHito.PENDIENTE) {
            throw new ConflictoException(
                "La cuota ya está paga o no admite un pago nuevo.");
        }
        return pagoRepository
            .findFirstByHitoPedidoIdHitoPedidoAndEstado(
                hito.getIdHitoPedido(), EstadoPago.PENDIENTE)
            .orElseGet(() -> crearPago(hito, pedido));
    }

    private Pago crearPago(HitoPedido hito, Pedido pedido) {
        Pago pago = new Pago();
        pago.setHitoPedido(hito);
        pago.setUsuarioPagador(pedido.getCliente());
        pago.setMonto(hito.getMonto());
        pago.setMoneda(pedido.getMoneda());
        pago.setMontoCobro(hito.getMontoCobro());
        pago.setMonedaCobro(pedido.getMonedaCobro());
        pago.setCotizacionCobro(pedido.getCotizacionCobro());
        pago.setComisionMontoCobro(hito.getComisionMontoCobro());
        pago.setMontoNetoCobro(hito.getMontoNetoCobro());
        pago.setEstado(EstadoPago.PENDIENTE);
        pago.setProveedor(proveedorPago.nombre());
        pago.setClaveIdempotencia(
            "hito-" + hito.getIdHitoPedido() + "-" + UUID.randomUUID());

        ResultadoCobro resultado = proveedorPago.crearCobro(pago);
        pago.setIdExterno(resultado.idExterno());
        pago.setPayload(resultado.instruccion());
        return pagoRepository.save(pago);
    }

    @Transactional
    public Pedido confirmar(Long idPago, Long idUsuario, String payload) {
        Pago pago = pagoRepository.findByIdPagoForUpdate(idPago)
            .orElseThrow(() -> new NoSuchElementException(
                "Pago no encontrado: " + idPago));
        validarPagador(pago, idUsuario);

        if (pago.getEstado() == EstadoPago.APROBADO) {
            return cargarPedido(pago);
        }
        if (pago.getEstado() != EstadoPago.PENDIENTE) {
            throw new ConflictoException("El pago ya no está pendiente.");
        }

        HitoPedido hito = hitoPedidoRepository
            .findByIdHitoPedidoForUpdate(pago.getHitoPedido().getIdHitoPedido())
            .orElseThrow(() -> new NoSuchElementException("Hito no encontrado."));
        Pedido pedido = pedidoRepository.findByIdPedidoForUpdate(
                hito.getPedido().getIdPedido())
            .orElseThrow(() -> new NoSuchElementException("Pedido no encontrado."));

        if (hito.getEstadoPago() != EstadoPagoHito.PENDIENTE) {
            throw new ConflictoException("La cuota ya no está pendiente de pago.");
        }
        if (pedido.getEstado() != EstadoPedido.PENDIENTE_PAGO
                && pedido.getEstado() != EstadoPedido.EN_CURSO) {
            throw new ConflictoException("El pedido ya está cerrado.");
        }

        LocalDateTime ahora = LocalDateTime.now();
        pago.setEstado(EstadoPago.APROBADO);
        pago.setFechaAcreditacion(ahora);
        if (payload != null && !payload.isBlank()) {
            pago.setPayload(payload);
        }

        hito.setEstadoPago(EstadoPagoHito.PAGADO);
        hito.setFechaPago(ahora);
        hito.setIdPagoAprobado(pago.getIdPago());

        boolean primerPago = pedido.getEstado() == EstadoPedido.PENDIENTE_PAGO;
        if (primerPago) {
            iniciarPedido(pedido, ahora);
        }

        if (hito.getTipo() == TipoHito.ENTREGA) {
            hito.setEstadoEntrega(EstadoEntregaHito.APROBADO);
            hito.setFechaAprobacion(ahora);
            pedido.setEstado(EstadoPedido.FINALIZADO);
            pedido.setFinalizadoEn(ahora);
            Solicitud solicitud = pedido.getSolicitud();
            solicitud.setEstado(EstadoSolicitud.FINALIZADA);
            solicitud.setFechaActualizacion(ahora);
            solicitudRepository.save(solicitud);
        } else if (!hito.isRequiereEntrega()) {
            hito.setEstadoEntrega(EstadoEntregaHito.APROBADO);
            hito.setFechaAprobacion(ahora);
            hitoFlujoService.habilitarSiguiente(pedido, hito, ahora);
        }

        hitoPedidoRepository.save(hito);
        pedidoRepository.save(pedido);
        pagoRepository.save(pago);

        notificarPagoRecibido(pedido, hito);
        if (hito.getTipo() == TipoHito.ENTREGA) {
            notificarFinalizado(pedido);
        }
        return pedido;
    }

    @Transactional
    public Pedido rechazar(Long idPago, Long idUsuario, String motivo) {
        Pago pago = pagoRepository.findByIdPagoForUpdate(idPago)
            .orElseThrow(() -> new NoSuchElementException(
                "Pago no encontrado: " + idPago));
        validarPagador(pago, idUsuario);

        Pedido pedido = cargarPedido(pago);
        if (pago.getEstado() == EstadoPago.RECHAZADO) {
            return pedido;
        }
        if (pago.getEstado() != EstadoPago.PENDIENTE) {
            throw new ConflictoException("El pago ya no está pendiente.");
        }
        pago.setEstado(EstadoPago.RECHAZADO);
        pagoRepository.save(pago);
        notificacionService.crearParaUsuario(
            pago.getUsuarioPagador().getIdUsuario(),
            NOTIFICACION_PAGO_RECHAZADO,
            "Pago rechazado",
            "El pago de '" + pago.getHitoPedido().getNombre()
                + "' del pedido de '" + nombreServicio(pedido)
                + "' fue rechazado." + (motivo != null && !motivo.isBlank()
                    ? " Motivo: " + motivo.trim()
                    : ""),
            pedido.getIdPedido(),
            TIPO_REFERENCIA_PEDIDO);
        return pedido;
    }

    @Transactional
    public void cancelarPendientes(HitoPedido hito) {
        List<Pago> pendientes = pagoRepository
            .findByHitoPedidoIdHitoPedidoAndEstado(
                hito.getIdHitoPedido(), EstadoPago.PENDIENTE);
        for (Pago pago : pendientes) {
            pago.setEstado(EstadoPago.CANCELADO);
            pagoRepository.save(pago);
        }
    }

    private void iniciarPedido(Pedido pedido, LocalDateTime ahora) {
        Presupuesto presupuesto = pedido.getPresupuesto();
        LocalDate fechaPago = ahora.toLocalDate();
        LocalDate inicio = CalculadoraEntrega.inicioReal(
            fechaPago, presupuesto.getFechaInicioEstimada());
        LocalDate limite = CalculadoraEntrega.sumarPlazo(
            inicio,
            presupuesto.getTiempoEntregaValor(),
            presupuesto.getTiempoEntregaUnidad());
        pedido.setEstado(EstadoPedido.EN_CURSO);
        pedido.setFechaInicio(inicio);
        pedido.setLimiteEntrega(limite);

        Solicitud solicitud = pedido.getSolicitud();
        solicitud.setEstado(EstadoSolicitud.PAGADA);
        solicitud.setFechaActualizacion(ahora);
        solicitudRepository.save(solicitud);
    }

    private void notificarPagoRecibido(Pedido pedido, HitoPedido hito) {
        notificacionService.crearParaUsuario(
            pedido.getArtista().getIdUsuario(),
            NOTIFICACION_PAGO_RECIBIDO,
            "Pago recibido",
            "El cliente pagó '" + hito.getNombre()
                + "' del pedido de '" + nombreServicio(pedido) + "'.",
            pedido.getIdPedido(),
            TIPO_REFERENCIA_PEDIDO);
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

    private void validarPagador(Pago pago, Long idUsuario) {
        if (pago.getUsuarioPagador() == null
                || !pago.getUsuarioPagador().getIdUsuario().equals(idUsuario)) {
            throw new AccesoDenegadoException("No podés operar sobre este pago.");
        }
    }

    private Pedido cargarPedido(Pago pago) {
        return pedidoRepository.findById(
                pago.getHitoPedido().getPedido().getIdPedido())
            .orElseThrow(() -> new NoSuchElementException("Pedido no encontrado."));
    }

    private String nombreServicio(Pedido pedido) {
        return pedido.getServicio() != null && pedido.getServicio().getNombre() != null
            ? pedido.getServicio().getNombre()
            : "tu pedido";
    }
}
