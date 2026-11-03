package com.temipora.backend_springboot.servicios;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
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
import com.temipora.backend_springboot.servicios.pagos.EstadoCobroProveedor;
import com.temipora.backend_springboot.servicios.pagos.ProveedorPago;
import com.temipora.backend_springboot.servicios.pagos.ResultadoCobro;

@Service
public class PagoService {

    private static final String TIPO_REFERENCIA_PEDIDO = "PEDIDO";
    private static final String NOTIFICACION_PAGO_RECIBIDO = "PAGO_RECIBIDO";
    private static final String NOTIFICACION_PAGO_RECHAZADO = "PAGO_RECHAZADO";

    private static final String ENTIDAD_PAGO = "Pago";
    private static final String ENTIDAD_PEDIDO = "Pedido";
    private static final List<CampoAuditado> CAMPOS_PAGO = List.of(
        CampoAuditado.de("estado", "Estado"),
        CampoAuditado.de("monto", "Monto"),
        CampoAuditado.de("moneda", "Moneda"),
        CampoAuditado.de("montoCobro", "Monto de cobro"),
        CampoAuditado.de("monedaCobro", "Moneda de cobro"),
        CampoAuditado.de("idPagoExterno", "ID en el proveedor"),
        CampoAuditado.de("estadoExterno", "Estado del proveedor"));

    private final PagoRepository pagoRepository;
    private final HitoPedidoRepository hitoPedidoRepository;
    private final PedidoRepository pedidoRepository;
    private final SolicitudRepository solicitudRepository;
    private final NotificacionService notificacionService;
    private final HitoFlujoService hitoFlujoService;
    private final PedidoCierreService pedidoCierreService;
    private final ProveedorPago proveedorPago;
    private final AuditoriaService auditoria;

    public PagoService(
            PagoRepository pagoRepository,
            HitoPedidoRepository hitoPedidoRepository,
            PedidoRepository pedidoRepository,
            SolicitudRepository solicitudRepository,
            NotificacionService notificacionService,
            HitoFlujoService hitoFlujoService,
            PedidoCierreService pedidoCierreService,
            ProveedorPago proveedorPago,
            AuditoriaService auditoria) {
        this.pagoRepository = pagoRepository;
        this.hitoPedidoRepository = hitoPedidoRepository;
        this.pedidoRepository = pedidoRepository;
        this.solicitudRepository = solicitudRepository;
        this.notificacionService = notificacionService;
        this.hitoFlujoService = hitoFlujoService;
        this.pedidoCierreService = pedidoCierreService;
        this.proveedorPago = proveedorPago;
        this.auditoria = auditoria;
    }

    @Transactional
    public Pago iniciarPago(HitoPedido hito, Pedido pedido) {
        if (hito.getEstadoPago() != EstadoPagoHito.PENDIENTE) {
            throw new ConflictoException(
                "La cuota ya está paga o no admite un pago nuevo.");
        }
        if (pagoRepository.findFirstByHitoPedidoIdHitoPedidoAndEstado(
                hito.getIdHitoPedido(), EstadoPago.APROBADO).isPresent()) {
            throw new ConflictoException("El pago ya fue acreditado.");
        }
        Optional<Pago> existente = pagoRepository
            .findFirstByHitoPedidoIdHitoPedidoAndEstado(
                hito.getIdHitoPedido(), EstadoPago.PENDIENTE);
        if (existente.isEmpty()) {
            return crearCobro(hito, pedido);
        }

        Pago pago = existente.get();
        // Reconciliar con el proveedor antes de reutilizar o crear un cobro:
        // si el webhook se demoró, acá se acredita el pago y se evita el
        // doble cobro.
        Optional<EstadoCobroProveedor> estado =
            proveedorPago.consultarEstado(pago);
        if (estado.isPresent()) {
            aplicarEstadoProveedor(
                pago.getIdPago(),
                estado.get().estado(),
                estado.get().idPagoExterno(),
                estado.get().estadoExterno(),
                estado.get().detalle());
            pago = pagoRepository.findById(pago.getIdPago()).orElse(pago);
            if (pago.getEstado() == EstadoPago.APROBADO
                    || hito.getEstadoPago() != EstadoPagoHito.PENDIENTE) {
                return pago;
            }
            if (pago.getEstado() != EstadoPago.PENDIENTE) {
                return crearCobro(hito, pedido);
            }
        }
        if (requiereCobro(pago)) {
            aplicarCobro(pago);
        }
        return pagoRepository.save(pago);
    }

    private Pago crearCobro(HitoPedido hito, Pedido pedido) {
        Pago pago = nuevoPago(hito, pedido);
        aplicarCobro(pago);
        Pago guardado = pagoRepository.save(pago);
        auditoria.registrar(
            AccionesAdmin.PAGO_INICIAR, ENTIDAD_PAGO, guardado.getIdPago(),
            "Inició el pago del hito '" + hito.getNombre()
                + "' del pedido #" + pedido.getIdPedido(),
            null, guardado, CAMPOS_PAGO);
        return guardado;
    }

    private Pago nuevoPago(HitoPedido hito, Pedido pedido) {
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
        // Se guarda antes de crear el cobro para que el proveedor externo
        // pueda referenciar el idPago en su preferencia.
        return pagoRepository.save(pago);
    }

    private boolean requiereCobro(Pago pago) {
        if (pago.getPayload() == null || pago.getPayload().isBlank()) {
            return true;
        }
        return pago.getExpiraEn() != null
            && !pago.getExpiraEn().isAfter(LocalDateTime.now());
    }

    private void aplicarCobro(Pago pago) {
        ResultadoCobro resultado = proveedorPago.crearCobro(pago);
        pago.setIdExterno(resultado.idExterno());
        pago.setPayload(resultado.instruccion());
        pago.setExpiraEn(resultado.expiraEn());
        pago.setFechaActualizacionProveedor(LocalDateTime.now());
    }

    @Transactional
    public Pedido confirmar(Long idPago, Long idUsuario, String payload) {
        Pago pago = pagoRepository.findByIdPagoForUpdate(idPago)
            .orElseThrow(() -> new NoSuchElementException(
                "Pago no encontrado: " + idPago));
        validarPagador(pago, idUsuario);
        Snapshot antes = Snapshot.de(pago, CAMPOS_PAGO);
        Pedido pedido = aplicarAprobacion(pago, payload);
        auditoria.registrar(
            AccionesAdmin.PAGO_CONFIRMAR, ENTIDAD_PAGO, pago.getIdPago(),
            "Confirmó el pago del hito '" + pago.getHitoPedido().getNombre() + "'",
            antes, pago, CAMPOS_PAGO);
        return pedido;
    }

    /**
     * Reconciliación manual: consulta el estado real en el proveedor y lo
     * aplica. Se usa cuando el webhook se demora o no llega.
     */
    @Transactional
    public void sincronizar(Long idPago) {
        Pago pago = pagoRepository.findByIdPagoForUpdate(idPago)
            .orElseThrow(() -> new NoSuchElementException(
                "Pago no encontrado: " + idPago));
        proveedorPago.consultarEstado(pago).ifPresent(estado ->
            aplicarEstadoProveedor(
                idPago,
                estado.estado(),
                estado.idPagoExterno(),
                estado.estadoExterno(),
                estado.detalle()));
    }

    /**
     * Aplica el estado que informa un proveedor externo (webhook). No exige
     * usuario autenticado: la confianza viene de la firma del webhook y de la
     * consulta server-to-server del pago.
     */
    @Transactional
    public void aplicarEstadoProveedor(
            Long idPago,
            EstadoPago estado,
            String idPagoExterno,
            String estadoExterno,
            String detalle) {
        Pago pago = pagoRepository.findByIdPagoForUpdate(idPago)
            .orElseThrow(() -> new NoSuchElementException(
                "Pago no encontrado: " + idPago));
        Snapshot antes = Snapshot.de(pago, CAMPOS_PAGO);
        pago.setIdPagoExterno(idPagoExterno);
        pago.setEstadoExterno(detalle != null && !detalle.isBlank()
            ? estadoExterno + " / " + detalle
            : estadoExterno);
        pago.setFechaActualizacionProveedor(LocalDateTime.now());

        if (pago.getEstado() == EstadoPago.APROBADO) {
            if (estado == EstadoPago.REEMBOLSADO) {
                pago.setEstado(EstadoPago.REEMBOLSADO);
            }
            pagoRepository.save(pago);
            auditarEstadoProveedor(pago, antes, estadoExterno);
            return;
        }
        if (estado == EstadoPago.APROBADO) {
            // El payload conserva la URL del checkout; el detalle del
            // proveedor queda en estadoExterno.
            aplicarAprobacion(pago, null);
            auditarEstadoProveedor(pago, antes, estadoExterno);
            return;
        }
        if (pago.getEstado() == EstadoPago.PENDIENTE) {
            pago.setEstado(estado);
            if (estado == EstadoPago.RECHAZADO) {
                notificarRechazo(
                    pago,
                    cargarPedido(pago),
                    detalle != null && !detalle.isBlank()
                        ? detalle
                        : estadoExterno);
            }
        }
        pagoRepository.save(pago);
        auditarEstadoProveedor(pago, antes, estadoExterno);
    }

    private void auditarEstadoProveedor(
            Pago pago, Snapshot antes, String estadoExterno) {
        auditoria.registrar(
            AccionesAdmin.PAGO_ESTADO_ACTUALIZAR, ENTIDAD_PAGO, pago.getIdPago(),
            "Actualizó el pago desde el proveedor (" + estadoExterno + ")",
            antes, pago, CAMPOS_PAGO);
    }

    private Pedido aplicarAprobacion(Pago pago, String payload) {
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
            // Otro pago ya cubrió la cuota: se registra este como aprobado
            // sin volver a mover el pedido (idempotencia del webhook).
            pago.setEstado(EstadoPago.APROBADO);
            pago.setFechaAcreditacion(LocalDateTime.now());
            pagoRepository.save(pago);
            return pedido;
        }
        if (pedido.getEstado() != EstadoPedido.PENDIENTE_PAGO
                && pedido.getEstado() != EstadoPedido.EN_CURSO) {
            throw new ConflictoException("El pedido ya está cerrado.");
        }

        LocalDateTime ahora = LocalDateTime.now();
        if (hito.getTipo() == TipoHito.ENTREGA
                && hito.getEstadoEntrega() != EstadoEntregaHito.ENTREGADO) {
            throw new ConflictoException(
                "La entrega final todavía no está disponible para pagar.");
        }

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

        HitoPedido siguiente = null;
        if (hito.getTipo() == TipoHito.ENTREGA) {
            hito.setEstadoEntrega(EstadoEntregaHito.APROBADO);
            hito.setFechaAprobacion(ahora);
        } else if (!hito.isRequiereEntrega()) {
            hito.setEstadoEntrega(EstadoEntregaHito.APROBADO);
            hito.setFechaAprobacion(ahora);
            siguiente = hitoFlujoService.habilitarSiguiente(
                pedido, hito, ahora);
        }

        hitoPedidoRepository.save(hito);
        pedidoRepository.save(pedido);
        pagoRepository.save(pago);

        notificarPagoRecibido(pedido, hito);

        if (hito.getTipo() == TipoHito.ENTREGA
                || (!hito.isRequiereEntrega() && siguiente == null)) {
            pedidoCierreService.finalizar(pedido, ahora);
            auditoria.registrar(
                AccionesAdmin.PEDIDO_FINALIZAR, ENTIDAD_PEDIDO, pedido.getIdPedido(),
                "Finalizó el pedido al acreditarse el último hito",
                "Finalización automática por pago");
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
        Snapshot antes = Snapshot.de(pago, CAMPOS_PAGO);
        pago.setEstado(EstadoPago.RECHAZADO);
        pagoRepository.save(pago);
        notificarRechazo(pago, pedido, motivo);
        auditoria.registrar(
            AccionesAdmin.PAGO_RECHAZAR, ENTIDAD_PAGO, pago.getIdPago(),
            motivo != null && !motivo.isBlank()
                ? "Rechazó el pago. Motivo: " + motivo.trim()
                : "Rechazó el pago",
            antes, pago, CAMPOS_PAGO);
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

    private void notificarRechazo(Pago pago, Pedido pedido, String motivo) {
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
