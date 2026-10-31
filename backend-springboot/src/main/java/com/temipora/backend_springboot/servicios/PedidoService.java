package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.temipora.backend_springboot.modelo.Conversacion;
import com.temipora.backend_springboot.modelo.DibujoResumenDTO;
import com.temipora.backend_springboot.modelo.EntregaHitoDTO;
import com.temipora.backend_springboot.modelo.EntregaHitoMultipartRequest;
import com.temipora.backend_springboot.modelo.EstadoConversacion;
import com.temipora.backend_springboot.modelo.EstadoEntregaHito;
import com.temipora.backend_springboot.modelo.EstadoMensaje;
import com.temipora.backend_springboot.modelo.EstadoPago;
import com.temipora.backend_springboot.modelo.EstadoPagoHito;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.EstadoSolicitud;
import com.temipora.backend_springboot.modelo.HitoPedido;
import com.temipora.backend_springboot.modelo.HitoPedidoDTO;
import com.temipora.backend_springboot.modelo.HitoPresupuesto;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.Pago;
import com.temipora.backend_springboot.modelo.PagoDTO;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.PedidoDTO;
import com.temipora.backend_springboot.modelo.PedidoResumenDTO;
import com.temipora.backend_springboot.modelo.Presupuesto;
import com.temipora.backend_springboot.modelo.PresupuestoDTO;
import com.temipora.backend_springboot.modelo.Solicitud;
import com.temipora.backend_springboot.modelo.SolicitudRevisionRequest;
import com.temipora.backend_springboot.modelo.TerminosPlataforma;
import com.temipora.backend_springboot.modelo.TipoHito;
import com.temipora.backend_springboot.modelo.UsuarioResumenDTO;
import com.temipora.backend_springboot.repositorios.ConversacionRepository;
import com.temipora.backend_springboot.repositorios.HitoPedidoRepository;
import com.temipora.backend_springboot.repositorios.MensajeRepository;
import com.temipora.backend_springboot.repositorios.PagoRepository;
import com.temipora.backend_springboot.repositorios.PedidoRepository;
import com.temipora.backend_springboot.repositorios.SolicitudRepository;

@Service
public class PedidoService {

    private static final String TIPO_REFERENCIA = "PEDIDO";
    private static final String NOTIF_HITO_ENTREGADO = "HITO_ENTREGADO";
    private static final String NOTIF_ENTREGA_LISTA = "ENTREGA_LISTA";
    private static final String NOTIF_HITO_APROBADO = "HITO_APROBADO";
    private static final String NOTIF_HITO_APROBADO_AUTOMATICO =
        "HITO_APROBADO_AUTOMATICAMENTE";
    private static final String NOTIF_HITO_CON_REVISION = "HITO_CON_REVISION";
    private static final String NOTIF_PEDIDO_CANCELADO = "PEDIDO_CANCELADO";
    private static final String NOTIF_PEDIDO_IMPAGO = "PEDIDO_IMPAGO";
    private static final String NOTIF_PUBLICACION_AUTORIZADA =
        "PEDIDO_PUBLICACION_AUTORIZADA";
    private static final String NOTIF_PUBLICACION_RETIRADA =
        "PEDIDO_PUBLICACION_RETIRADA";

    private static final List<EstadoPedido> ESTADOS_PANEL_ACTIVOS = List.of(
        EstadoPedido.PENDIENTE_PAGO,
        EstadoPedido.EN_CURSO,
        EstadoPedido.IMPAGO);
    private static final int MAX_HITOS_PANEL = 10;
    private static final int DIAS_HITO_POR_VENCER = 3;

    private final PedidoRepository pedidoRepository;
    private final HitoPedidoRepository hitoPedidoRepository;
    private final PagoRepository pagoRepository;
    private final SolicitudRepository solicitudRepository;
    private final PagoService pagoService;
    private final HitoFlujoService hitoFlujoService;
    private final TerminosPlataformaService terminosPlataformaService;
    private final NotificacionService notificacionService;
    private final MonedaVistaService monedaVistaService;
    private final EntregaService entregaService;
    private final PedidoCierreService pedidoCierreService;
    private final ConversacionRepository conversacionRepository;
    private final MensajeRepository mensajeRepository;

    public PedidoService(
            PedidoRepository pedidoRepository,
            HitoPedidoRepository hitoPedidoRepository,
            PagoRepository pagoRepository,
            SolicitudRepository solicitudRepository,
            PagoService pagoService,
            HitoFlujoService hitoFlujoService,
            TerminosPlataformaService terminosPlataformaService,
            NotificacionService notificacionService,
            MonedaVistaService monedaVistaService,
            EntregaService entregaService,
            PedidoCierreService pedidoCierreService,
            ConversacionRepository conversacionRepository,
            MensajeRepository mensajeRepository) {
        this.pedidoRepository = pedidoRepository;
        this.hitoPedidoRepository = hitoPedidoRepository;
        this.pagoRepository = pagoRepository;
        this.solicitudRepository = solicitudRepository;
        this.pagoService = pagoService;
        this.hitoFlujoService = hitoFlujoService;
        this.terminosPlataformaService = terminosPlataformaService;
        this.notificacionService = notificacionService;
        this.monedaVistaService = monedaVistaService;
        this.entregaService = entregaService;
        this.pedidoCierreService = pedidoCierreService;
        this.conversacionRepository = conversacionRepository;
        this.mensajeRepository = mensajeRepository;
    }

    @Transactional
    public PedidoDTO crearDesdePresupuesto(Long idSolicitud, Long idCliente) {
        Solicitud solicitud = solicitudRepository
            .findByIdSolicitudForUpdate(idSolicitud)
            .orElseThrow(() -> new NoSuchElementException(
                "Solicitud no encontrada: " + idSolicitud));
        if (solicitud.getCliente() == null
                || !solicitud.getCliente().getIdUsuario().equals(idCliente)) {
            throw new AccesoDenegadoException("No podés pagar esta solicitud.");
        }

        Optional<Pedido> existente = pedidoRepository
            .findBySolicitudIdSolicitud(idSolicitud);
        if (existente.isPresent()) {
            Pedido pedidoExistente = existente.get();
            if (pedidoExistente.getEstado() == EstadoPedido.PENDIENTE_PAGO) {
                HitoPedido primero = pedidoExistente.getHitos().isEmpty()
                    ? null
                    : pedidoExistente.getHitos().get(0);
                if (primero != null
                        && primero.getTipo() == TipoHito.SENA
                        && primero.getEstadoPago() == EstadoPagoHito.PENDIENTE
                        && pagoRepository
                            .findFirstByHitoPedidoIdHitoPedidoAndEstado(
                                primero.getIdHitoPedido(), EstadoPago.PENDIENTE)
                            .isEmpty()) {
                    pagoService.iniciarPago(primero, pedidoExistente);
                } else if (primero != null
                        && primero.getTipo() == TipoHito.ENTREGA) {
                    pagoService.cancelarPendientes(primero);
                    iniciarPedidoSinPago(pedidoExistente, LocalDateTime.now());
                }
            } else if (pedidoExistente.getEstado() != EstadoPedido.EN_CURSO) {
                throw new ConflictoException("El pedido ya está cerrado.");
            }
            return construirDTO(pedidoExistente, idCliente);
        }

        if (solicitud.getEstado() != EstadoSolicitud.PRESUPUESTADA) {
            throw new ConflictoException(
                "La solicitud no está en condiciones de ser pagada.");
        }
        Presupuesto presupuesto = solicitud.getPresupuesto();
        if (presupuesto == null) {
            throw new ConflictoException("La solicitud no tiene un presupuesto.");
        }
        LocalDateTime ahora = LocalDateTime.now();
        if (presupuesto.getFechaVencimiento() != null
                && presupuesto.getFechaVencimiento().isBefore(ahora)) {
            throw new ConflictoException(
                "El presupuesto venció. Pedile al artista que te envíe uno nuevo.");
        }
        if (presupuesto.getHitos() == null || presupuesto.getHitos().isEmpty()) {
            throw new ConflictoException(
                "El presupuesto no tiene un plan de pagos.");
        }

        TerminosPlataforma terminos = null;
        try {
            terminos = terminosPlataformaService.obtenerEntidadVigente();
        } catch (NoSuchElementException ignored) {
            // La plataforma puede no tener términos publicados; no bloquea el pago.
        }

        Pedido pedido = new Pedido();
        pedido.setSolicitud(solicitud);
        pedido.setPresupuesto(presupuesto);
        pedido.setCliente(solicitud.getCliente());
        pedido.setArtista(solicitud.getArtista());
        pedido.setServicio(solicitud.getServicio());
        pedido.setEstado(EstadoPedido.PENDIENTE_PAGO);
        pedido.setPrecioFinal(presupuesto.getPrecioFinal());
        pedido.setPrecioFinalCobro(presupuesto.getPrecioFinalCobro());
        pedido.setMoneda(presupuesto.getMoneda());
        pedido.setMonedaCobro(presupuesto.getMonedaCobro());
        pedido.setCotizacionCobro(presupuesto.getCotizacionCobro());
        pedido.setComisionPorcentaje(presupuesto.getComisionPorcentaje());
        if (terminos != null) {
            pedido.setTerminosPlataformaVersion(terminos.getVersion());
            pedido.setTerminosPlataformaSnapshot(terminos.getContenido());
        }

        for (HitoPresupuesto hitoPresupuesto : presupuesto.getHitos()) {
            HitoPedido hito = new HitoPedido();
            hito.setHitoPresupuesto(hitoPresupuesto);
            hito.setOrden(hitoPresupuesto.getOrden());
            hito.setNombre(hitoPresupuesto.getNombre());
            hito.setDescripcion(hitoPresupuesto.getDescripcion());
            hito.setPorcentaje(hitoPresupuesto.getPorcentaje());
            hito.setTipo(hitoPresupuesto.getTipo());
            hito.setRequiereEntrega(hitoPresupuesto.isRequiereEntrega());
            hito.setMonto(hitoPresupuesto.getMonto());
            hito.setMontoCobro(hitoPresupuesto.getMontoCobro());
            hito.setComisionMonto(hitoPresupuesto.getComisionMonto());
            hito.setComisionMontoCobro(
                hitoPresupuesto.getComisionMontoCobro());
            hito.setMontoNeto(hitoPresupuesto.getMontoNeto());
            hito.setMontoNetoCobro(hitoPresupuesto.getMontoNetoCobro());
            if (hitoPresupuesto.getTipo() == TipoHito.SENA) {
                hito.setFechaLimitePago(
                    ahora.plusDays(hitoFlujoService.diasPagoCuota()));
            }
            pedido.agregarHito(hito);
        }

        pedidoRepository.save(pedido);
        HitoPedido primero = pedido.getHitos().get(0);
        if (primero.getTipo() == TipoHito.SENA) {
            pagoService.iniciarPago(primero, pedido);
        } else {
            pagoService.cancelarPendientes(primero);
            iniciarPedidoSinPago(pedido, ahora);
        }
        return construirDTO(pedido, idCliente);
    }

    private void iniciarPedidoSinPago(
            Pedido pedido, LocalDateTime ahora) {
        Presupuesto presupuesto = pedido.getPresupuesto();
        LocalDate inicio = CalculadoraEntrega.inicioReal(
            ahora.toLocalDate(), presupuesto.getFechaInicioEstimada());
        LocalDate limite = CalculadoraEntrega.sumarPlazo(
            inicio,
            presupuesto.getTiempoEntregaValor(),
            presupuesto.getTiempoEntregaUnidad());
        pedido.setEstado(EstadoPedido.EN_CURSO);
        pedido.setFechaInicio(inicio);
        pedido.setLimiteEntrega(limite);
        pedidoRepository.save(pedido);
    }

    @Transactional
    public PedidoDTO pagarHito(Long idPedido, Long idHito, Long idCliente) {
        Pedido pedido = pedidoRepository.findByIdPedidoForUpdate(idPedido)
            .orElseThrow(() -> new NoSuchElementException(
                "Pedido no encontrado: " + idPedido));
        if (!pedido.getCliente().getIdUsuario().equals(idCliente)) {
            throw new AccesoDenegadoException("No podés pagar este pedido.");
        }
        HitoPedido hito = hitoPedidoRepository
            .findByIdHitoPedidoForUpdate(idHito)
            .orElseThrow(() -> new NoSuchElementException(
                "Hito no encontrado: " + idHito));
        validarHitoDelPedido(pedido, hito);

        List<Pago> pagos = pagoRepository
            .findByHitoPedidoIdHitoPedidoOrderByFechaCreacionDesc(idHito);
        if (!puedePagar(pedido, hito, pagos)) {
            throw new ConflictoException(motivoNoPago(pedido, hito, pagos));
        }
        pagoService.iniciarPago(hito, pedido);
        return construirDTO(pedido, idCliente);
    }

    @Transactional
    public PedidoDTO entregar(
            Long idPedido, Long idHito, Long idArtista,
            EntregaHitoMultipartRequest request,
            List<MultipartFile> archivos) {
        Pedido pedido = pedidoRepository.findByIdPedidoForUpdate(idPedido)
            .orElseThrow(() -> new NoSuchElementException(
                "Pedido no encontrado: " + idPedido));
        if (!pedido.getArtista().getIdUsuario().equals(idArtista)) {
            throw new AccesoDenegadoException("No podés entregar este pedido.");
        }
        HitoPedido hito = hitoPedidoRepository
            .findByIdHitoPedidoForUpdate(idHito)
            .orElseThrow(() -> new NoSuchElementException(
                "Hito no encontrado: " + idHito));
        validarHitoDelPedido(pedido, hito);

        if (pedido.getEstado() != EstadoPedido.EN_CURSO) {
            throw new ConflictoException("El pedido no está en curso.");
        }
        if (!hito.isRequiereEntrega()) {
            throw new ConflictoException("Esta cuota no requiere un entregable.");
        }
        if (hito.getEstadoEntrega() != EstadoEntregaHito.NO_INICIADO
                && hito.getEstadoEntrega() != EstadoEntregaHito.EN_PROCESO
                && hito.getEstadoEntrega() != EstadoEntregaHito.CON_REVISION) {
            throw new ConflictoException("Este hito ya fue entregado.");
        }
        if (hito.getTipo() == TipoHito.ENTREGA) {
            HitoPedido anterior = hitoAnterior(pedido, hito);
            if (anterior != null && !esAprobado(anterior)) {
                throw new ConflictoException(
                    "Primero tiene que aprobarse la etapa anterior.");
            }
            if (hito.getEstadoPago() != EstadoPagoHito.PENDIENTE) {
                throw new ConflictoException(
                    "La entrega final ya fue pagada.");
            }
        } else if (hito.getEstadoPago() != EstadoPagoHito.PAGADO) {
            throw new ConflictoException("La cuota todavía no está paga.");
        }

        entregaService.crear(pedido, hito, request, archivos);

        LocalDateTime ahora = LocalDateTime.now();
        hito.setEstadoEntrega(EstadoEntregaHito.ENTREGADO);
        hito.setFechaEntrega(ahora);
        hito.setFechaLimiteAprobacion(null);
        if (hito.getTipo() == TipoHito.ENTREGA) {
            hito.setFechaLimitePago(
                ahora.plusDays(hitoFlujoService.diasPagoCuota()));
        } else {
            hito.setFechaLimiteAprobacion(
                ahora.plusDays(hitoFlujoService.diasAprobacion()));
        }
        hitoPedidoRepository.save(hito);
        notificarEntrega(pedido, hito);
        return construirDTO(pedido, idArtista);
    }

    @Transactional
    public PedidoDTO aprobar(Long idPedido, Long idHito, Long idCliente) {
        Pedido pedido = pedidoRepository.findByIdPedidoForUpdate(idPedido)
            .orElseThrow(() -> new NoSuchElementException(
                "Pedido no encontrado: " + idPedido));
        if (!pedido.getCliente().getIdUsuario().equals(idCliente)) {
            throw new AccesoDenegadoException("No podés aprobar este pedido.");
        }
        HitoPedido hito = hitoPedidoRepository
            .findByIdHitoPedidoForUpdate(idHito)
            .orElseThrow(() -> new NoSuchElementException(
                "Hito no encontrado: " + idHito));
        validarHitoDelPedido(pedido, hito);

        if (hito.getTipo() == TipoHito.ENTREGA) {
            throw new ConflictoException(
                "La entrega final se aprueba al pagar la última cuota.");
        }
        if (hito.getEstadoPago() != EstadoPagoHito.PAGADO) {
            throw new ConflictoException("La cuota todavía no está paga.");
        }
        if (hito.getEstadoEntrega() != EstadoEntregaHito.ENTREGADO) {
            throw new ConflictoException("El hito no está entregado.");
        }

        LocalDateTime ahora = LocalDateTime.now();
        hito.setEstadoEntrega(EstadoEntregaHito.APROBADO);
        hito.setFechaAprobacion(ahora);
        hito.setFechaLimiteAprobacion(null);
        hitoPedidoRepository.save(hito);
        HitoPedido siguiente = hitoFlujoService.habilitarSiguiente(
            pedido, hito, ahora);
        notificarAprobado(pedido, hito, false);
        if (siguiente == null) {
            pedidoCierreService.finalizar(pedido, ahora);
        }
        return construirDTO(pedido, idCliente);
    }

    @Transactional
    public PedidoDTO solicitarRevision(
            Long idPedido, Long idHito, Long idCliente,
            SolicitudRevisionRequest request) {
        Pedido pedido = pedidoRepository.findByIdPedidoForUpdate(idPedido)
            .orElseThrow(() -> new NoSuchElementException(
                "Pedido no encontrado: " + idPedido));
        if (!pedido.getCliente().getIdUsuario().equals(idCliente)) {
            throw new AccesoDenegadoException(
                "No podés solicitar revisiones en este pedido.");
        }
        HitoPedido hito = hitoPedidoRepository
            .findByIdHitoPedidoForUpdate(idHito)
            .orElseThrow(() -> new NoSuchElementException(
                "Hito no encontrado: " + idHito));
        validarHitoDelPedido(pedido, hito);

        if (hito.getEstadoEntrega() != EstadoEntregaHito.ENTREGADO) {
            throw new ConflictoException(
                "Solo podés pedir una revisión de un entregable ya subido.");
        }

        hito.setEstadoEntrega(EstadoEntregaHito.CON_REVISION);
        hito.setFechaLimiteAprobacion(null);
        if (hito.getTipo() == TipoHito.ENTREGA) {
            hito.setFechaLimitePago(null);
        }
        hitoPedidoRepository.save(hito);
        String motivo = limpiarTexto(
            request != null ? request.getMotivo() : null, 500);
        entregaService.registrarRevision(hito, pedido.getCliente(), motivo);
        notificarRevision(pedido, hito, motivo);
        return construirDTO(pedido, idCliente);
    }

    @Transactional
    public PedidoDTO cancelar(Long idPedido, Long idCliente) {
        Pedido pedido = pedidoRepository.findByIdPedidoForUpdate(idPedido)
            .orElseThrow(() -> new NoSuchElementException(
                "Pedido no encontrado: " + idPedido));
        if (!pedido.getCliente().getIdUsuario().equals(idCliente)) {
            throw new AccesoDenegadoException("No podés cancelar este pedido.");
        }
        if (pedido.getEstado() != EstadoPedido.PENDIENTE_PAGO
                && pedido.getEstado() != EstadoPedido.EN_CURSO) {
            throw new ConflictoException("El pedido ya está cerrado.");
        }

        LocalDateTime ahora = LocalDateTime.now();
        pedido.setEstado(EstadoPedido.CANCELADO);
        for (HitoPedido hito : pedido.getHitos()) {
            if (hito.getEstadoPago() == EstadoPagoHito.PENDIENTE) {
                hito.setEstadoPago(EstadoPagoHito.CANCELADO);
                pagoService.cancelarPendientes(hito);
            }
        }
        pedidoRepository.save(pedido);

        Solicitud solicitud = pedido.getSolicitud();
        solicitud.setEstado(EstadoSolicitud.CANCELADA);
        solicitud.setFechaActualizacion(ahora);
        solicitudRepository.save(solicitud);
        notificarCancelacion(pedido);
        return construirDTO(pedido, idCliente);
    }

    @Transactional
    public PedidoDTO autorizarPublicacion(
            Long idPedido, Long idArtista, boolean autoriza) {
        Pedido pedido = pedidoRepository.findByIdPedidoForUpdate(idPedido)
            .orElseThrow(() -> new NoSuchElementException(
                "Pedido no encontrado: " + idPedido));
        if (!pedido.getArtista().getIdUsuario().equals(idArtista)) {
            throw new AccesoDenegadoException(
                "No podés autorizar la publicación de este pedido.");
        }
        if (pedido.getEstado() == EstadoPedido.CANCELADO
                || pedido.getEstado() == EstadoPedido.IMPAGO) {
            throw new ConflictoException(
                "El pedido está cerrado y no admite publicación.");
        }
        if (autoriza && pedido.getEstado() != EstadoPedido.FINALIZADO
                && !tieneEntregaFinal(pedido)) {
            throw new ConflictoException(
                "Todavía no hay una entrega final para autorizar.");
        }
        if (pedido.isPublicacionAutorizada() != autoriza) {
            pedido.setPublicacionAutorizada(autoriza);
            if (autoriza) {
                pedido.setPublicacionAutorizadaEn(LocalDateTime.now());
            }
            pedidoRepository.save(pedido);
            notificarPublicacion(pedido, autoriza);
        }
        return construirDTO(pedido, idArtista);
    }

    @Transactional
    public PedidoDTO ocultarPublicacion(
            Long idPedido, Long idCliente, boolean ocultar) {
        Pedido pedido = pedidoRepository.findByIdPedidoForUpdate(idPedido)
            .orElseThrow(() -> new NoSuchElementException(
                "Pedido no encontrado: " + idPedido));
        if (!pedido.getCliente().getIdUsuario().equals(idCliente)) {
            throw new AccesoDenegadoException(
                "No podés cambiar la visibilidad de este pedido.");
        }
        if (pedido.getEstado() != EstadoPedido.FINALIZADO) {
            throw new ConflictoException(
                "El pedido todavía no está completo.");
        }
        if (!pedido.isPublicacionAutorizada()) {
            throw new ConflictoException(
                "El artista todavía no autorizó la publicación.");
        }
        if (pedido.isPublicacionOcultadaCliente() != ocultar) {
            pedido.setPublicacionOcultadaCliente(ocultar);
            pedido.setPublicacionOcultadaEn(
                ocultar ? LocalDateTime.now() : null);
            pedidoRepository.save(pedido);
        }
        return construirDTO(pedido, idCliente);
    }

    private boolean tieneEntregaFinal(Pedido pedido) {
        HitoPedido hitoFinal = pedido.getHitos().stream()
            .max(Comparator.comparingInt(
                hito -> hito.getOrden() != null ? hito.getOrden() : 0))
            .orElse(null);
        if (hitoFinal == null || hitoFinal.getIdHitoPedido() == null) {
            return false;
        }
        return entregaService.listarPorPedido(pedido.getIdPedido()).stream()
            .anyMatch(entrega -> entrega.entrega().getHitoPedido()
                .getIdHitoPedido().equals(hitoFinal.getIdHitoPedido())
                && entrega.entrega().isVigente()
                && !entrega.entrega().isRetirada());
    }

    private void notificarPublicacion(Pedido pedido, boolean autoriza) {
        String tipo = autoriza
            ? NOTIF_PUBLICACION_AUTORIZADA
            : NOTIF_PUBLICACION_RETIRADA;
        String titulo = autoriza
            ? "Pedido autorizado para tu galería"
            : "Publicación retirada de tu galería";
        String descripcion = autoriza
            ? "El artista autorizó que el pedido de '"
                + nombreServicio(pedido) + "' se muestre en tu galería."
            : "El artista retiró la autorización para mostrar el pedido de '"
                + nombreServicio(pedido) + "' en tu galería.";
        notificacionService.crearParaUsuario(
            pedido.getCliente().getIdUsuario(),
            tipo,
            titulo,
            descripcion,
            pedido.getIdPedido(),
            TIPO_REFERENCIA);
    }

    @Transactional(readOnly = true)
    public PedidoDTO obtener(Long idPedido, Long idUsuario) {
        return obtener(idPedido, idUsuario, false);
    }

    @Transactional(readOnly = true)
    public PedidoDTO obtener(Long idPedido, Long idUsuario, boolean esAdmin) {
        Pedido pedido = pedidoRepository.findById(idPedido)
            .orElseThrow(() -> new NoSuchElementException(
                "Pedido no encontrado: " + idPedido));
        validarVista(pedido, idUsuario, esAdmin);
        return construirDTO(pedido, idUsuario, esAdmin);
    }

    @Transactional(readOnly = true)
    public PaginaDTO<PedidoResumenDTO> listarEnviados(
            Long idCliente, EstadoPedido estado, int pagina, int tamanio) {
        Pageable pageable = pageable(pagina, tamanio);
        Page<Pedido> page = estado == null
            ? pedidoRepository.findByClienteIdUsuario(idCliente, pageable)
            : pedidoRepository.findByClienteIdUsuarioAndEstado(
                idCliente, estado, pageable);
        return aPagina(page, idCliente);
    }

    @Transactional(readOnly = true)
    public PaginaDTO<PedidoResumenDTO> listarRecibidos(
            Long idArtista, EstadoPedido estado, int pagina, int tamanio) {
        Pageable pageable = pageable(pagina, tamanio);
        Page<Pedido> page = estado == null
            ? pedidoRepository.findByArtistaIdUsuario(idArtista, pageable)
            : pedidoRepository.findByArtistaIdUsuarioAndEstado(
                idArtista, estado, pageable);
        return aPagina(page, idArtista);
    }

    public record ActividadArtista(
            List<PedidoResumenDTO> pedidosActivos,
            List<HitoPedidoDTO> hitosAtencion,
            long pedidosEnCurso,
            long pedidosPendientesPago,
            long pedidosImpagos,
            long entregasPendientes,
            long entregasConRevision,
            long hitosPorVencer,
            BigDecimal porCobrar,
            String monedaMostrada) {
    }

    private record HitoAtencion(
            int prioridad, LocalDateTime fecha, HitoPedidoDTO dto) {
    }

    @Transactional(readOnly = true)
    public ActividadArtista actividadArtista(Long idArtista) {
        LocalDateTime ahora = LocalDateTime.now();
        Moneda mostrada = monedaVistaService.preferidaDe(idArtista);
        List<Pedido> pedidos = pedidoRepository
            .findByArtistaIdUsuarioAndEstadoInOrderByFechaCreacionDesc(
                idArtista, ESTADOS_PANEL_ACTIVOS, PageRequest.of(0, 50));

        List<PedidoResumenDTO> resumenes = new ArrayList<>();
        List<HitoAtencion> atenciones = new ArrayList<>();
        long enCurso = 0;
        long pendientePago = 0;
        long impagos = 0;
        long entregasPendientes = 0;
        long conRevision = 0;
        long porVencer = 0;
        BigDecimal porCobrar = BigDecimal.ZERO;

        for (Pedido pedido : pedidos) {
            if (pedido.getEstado() == EstadoPedido.EN_CURSO) {
                enCurso++;
            } else if (pedido.getEstado() == EstadoPedido.PENDIENTE_PAGO) {
                pendientePago++;
            } else if (pedido.getEstado() == EstadoPedido.IMPAGO) {
                impagos++;
            }

            boolean muestraOrigen = PresupuestoDTO.mismaMoneda(
                mostrada, pedido.getMoneda());
            boolean muestraCobro = !muestraOrigen
                && PresupuestoDTO.mismaMoneda(
                    mostrada, pedido.getMonedaCobro());
            BigDecimal factorCobro = mostrada != null
                ? monedaVistaService.factorCobroAMostrada(mostrada, ahora)
                : null;
            BigDecimal factorOrigen = mostrada != null
                ? monedaVistaService.factorOrigenAMostrada(
                    pedido.getMoneda(), mostrada, ahora)
                : null;

            for (HitoPedido hito : pedido.getHitos()) {
                boolean entregable = puedeEntregar(pedido, hito);
                boolean revision = hito.getEstadoEntrega()
                    == EstadoEntregaHito.CON_REVISION;
                boolean vencido = hito.getEstadoPago()
                    == EstadoPagoHito.VENCIDO;
                boolean vencePronto = hito.getEstadoPago()
                        == EstadoPagoHito.PENDIENTE
                    && hito.getFechaLimitePago() != null
                    && !hito.getFechaLimitePago().isAfter(
                        ahora.plusDays(DIAS_HITO_POR_VENCER));

                if (revision) {
                    conRevision++;
                }
                if (entregable) {
                    entregasPendientes++;
                }
                if (vencePronto) {
                    porVencer++;
                }
                if (hito.getEstadoPago() == EstadoPagoHito.PENDIENTE) {
                    porCobrar = sumar(porCobrar, netoMostrado(
                        hito,
                        muestraOrigen,
                        muestraCobro,
                        factorCobro,
                        factorOrigen));
                }

                int prioridad = revision ? 0
                    : vencido ? 1
                    : entregable ? 2
                    : vencePronto ? 3
                    : -1;
                if (prioridad < 0) {
                    continue;
                }
                LocalDateTime fecha = hito.getFechaLimitePago() != null
                    ? hito.getFechaLimitePago()
                    : hito.getFechaEntrega();
                atenciones.add(new HitoAtencion(
                    prioridad,
                    fecha,
                    HitoPedidoDTO.desdeHito(
                        hito,
                        List.of(),
                        true,
                        true,
                        List.of(),
                        false,
                        entregable,
                        false,
                        false,
                        muestraOrigen,
                        muestraCobro,
                        factorCobro,
                        factorOrigen)));
            }

            resumenes.add(construirResumen(pedido, idArtista));
        }

        resumenes.sort(Comparator
            .comparingInt(this::urgenciaPedido)
            .thenComparing(
                PedidoResumenDTO::getFechaCreacion,
                Comparator.nullsLast(
                    Comparator.<LocalDateTime>reverseOrder())));
        atenciones.sort(Comparator
            .comparingInt(HitoAtencion::prioridad)
            .thenComparing(
                HitoAtencion::fecha,
                Comparator.nullsLast(
                    Comparator.<LocalDateTime>naturalOrder())));

        return new ActividadArtista(
            resumenes,
            atenciones.stream()
                .limit(MAX_HITOS_PANEL)
                .map(HitoAtencion::dto)
                .toList(),
            enCurso,
            pendientePago,
            impagos,
            entregasPendientes,
            conRevision,
            porVencer,
            porCobrar,
            mostrada != null ? mostrada.getCodigo() : null);
    }

    public BigDecimal netoMostradoDe(
            HitoPedido hito, Moneda mostrada, LocalDateTime fecha) {
        Pedido pedido = hito.getPedido();
        boolean muestraOrigen = PresupuestoDTO.mismaMoneda(
            mostrada, pedido.getMoneda());
        boolean muestraCobro = !muestraOrigen && PresupuestoDTO.mismaMoneda(
            mostrada, pedido.getMonedaCobro());
        BigDecimal factorCobro = mostrada != null
            ? monedaVistaService.factorCobroAMostrada(mostrada, fecha)
            : null;
        BigDecimal factorOrigen = mostrada != null
            ? monedaVistaService.factorOrigenAMostrada(
                pedido.getMoneda(), mostrada, fecha)
            : null;
        return netoMostrado(
            hito, muestraOrigen, muestraCobro, factorCobro, factorOrigen);
    }

    private int urgenciaPedido(PedidoResumenDTO resumen) {
        if (EstadoPedido.IMPAGO.name().equals(resumen.getEstado())) {
            return 0;
        }
        if (resumen.isPuedeEntregar()) {
            return 1;
        }
        if (EstadoPedido.PENDIENTE_PAGO.name().equals(resumen.getEstado())) {
            return 2;
        }
        return 3;
    }

    private BigDecimal netoMostrado(
            HitoPedido hito,
            boolean muestraOrigen,
            boolean muestraCobro,
            BigDecimal factorCobro,
            BigDecimal factorOrigen) {
        BigDecimal neto = hito.getMontoNeto();
        BigDecimal netoCobro = hito.getMontoNetoCobro();
        if (neto == null) {
            neto = hito.getMonto() != null && hito.getComisionMonto() != null
                ? hito.getMonto().subtract(hito.getComisionMonto())
                : hito.getMonto();
        }
        if (netoCobro == null) {
            netoCobro = hito.getMontoCobro() != null
                    && hito.getComisionMontoCobro() != null
                ? hito.getMontoCobro().subtract(hito.getComisionMontoCobro())
                : hito.getMontoCobro();
        }
        return PresupuestoDTO.totalMostrado(
            neto, netoCobro, muestraOrigen, muestraCobro,
            factorCobro, factorOrigen);
    }

    @Transactional
    public int aprobarAutomaticamente() {
        LocalDateTime ahora = LocalDateTime.now();
        List<HitoPedido> candidatos = hitoPedidoRepository
            .findByEstadoEntregaAndFechaLimiteAprobacionBefore(
                EstadoEntregaHito.ENTREGADO, ahora);
        int aprobados = 0;
        for (HitoPedido candidato : candidatos) {
            HitoPedido hito = hitoPedidoRepository
                .findByIdHitoPedidoForUpdate(candidato.getIdHitoPedido())
                .orElse(null);
            if (hito == null || hito.getTipo() == TipoHito.ENTREGA
                    || hito.getEstadoEntrega() != EstadoEntregaHito.ENTREGADO
                    || hito.getFechaLimiteAprobacion() == null
                    || !hito.getFechaLimiteAprobacion().isBefore(ahora)) {
                continue;
            }
            Pedido pedido = pedidoRepository.findByIdPedidoForUpdate(
                    hito.getPedido().getIdPedido())
                .orElse(null);
            if (pedido == null || pedido.getEstado() != EstadoPedido.EN_CURSO) {
                continue;
            }
            hito.setEstadoEntrega(
                EstadoEntregaHito.APROBADO_AUTOMATICAMENTE);
            hito.setFechaAprobacion(ahora);
            hito.setFechaLimiteAprobacion(null);
            hitoPedidoRepository.save(hito);
            HitoPedido siguiente = hitoFlujoService.habilitarSiguiente(
                pedido, hito, ahora);
            notificarAprobado(pedido, hito, true);
            if (siguiente == null) {
                pedidoCierreService.finalizar(pedido, ahora);
            }
            aprobados++;
        }
        return aprobados;
    }

    @Transactional
    public int expirarHitos() {
        LocalDateTime ahora = LocalDateTime.now();
        List<HitoPedido> candidatos = hitoPedidoRepository
            .findByEstadoPagoAndFechaLimitePagoBefore(
                EstadoPagoHito.PENDIENTE, ahora);
        int expirados = 0;
        for (HitoPedido candidato : candidatos) {
            HitoPedido hito = hitoPedidoRepository
                .findByIdHitoPedidoForUpdate(candidato.getIdHitoPedido())
                .orElse(null);
            if (hito == null || hito.getEstadoPago() != EstadoPagoHito.PENDIENTE
                    || hito.getFechaLimitePago() == null
                    || !hito.getFechaLimitePago().isBefore(ahora)) {
                continue;
            }
            Pedido pedido = pedidoRepository.findByIdPedidoForUpdate(
                    hito.getPedido().getIdPedido())
                .orElse(null);
            if (pedido == null
                    || (pedido.getEstado() != EstadoPedido.PENDIENTE_PAGO
                        && pedido.getEstado() != EstadoPedido.EN_CURSO)) {
                continue;
            }
            hito.setEstadoPago(EstadoPagoHito.VENCIDO);
            hitoPedidoRepository.save(hito);
            pagoService.cancelarPendientes(hito);

            pedido.setEstado(EstadoPedido.IMPAGO);
            pedidoRepository.save(pedido);
            Solicitud solicitud = pedido.getSolicitud();
            solicitud.setEstado(EstadoSolicitud.CANCELADA);
            solicitud.setFechaActualizacion(ahora);
            solicitudRepository.save(solicitud);
            notificarImpago(pedido, hito);
            expirados++;
        }
        return expirados;
    }

    private PedidoDTO construirDTO(Pedido pedido, Long idVista) {
        return construirDTO(pedido, idVista, false);
    }

    private PedidoDTO construirDTO(
            Pedido pedido, Long idVista, boolean esAdmin) {
        boolean esArtista = pedido.getArtista().getIdUsuario().equals(idVista);
        boolean esCliente = pedido.getCliente().getIdUsuario().equals(idVista);
        boolean incluirComision = esArtista || esAdmin;
        Conversacion conversacion = esAdmin ? null : conversacionDe(pedido);
        LocalDateTime ahora = LocalDateTime.now();
        Moneda mostrada = monedaVistaService.preferidaDe(idVista);
        boolean muestraOrigen = PresupuestoDTO.mismaMoneda(
            mostrada, pedido.getMoneda());
        boolean muestraCobro = !muestraOrigen && PresupuestoDTO.mismaMoneda(
            mostrada, pedido.getMonedaCobro());
        BigDecimal factorOrigen = mostrada != null
            ? monedaVistaService.factorOrigenAMostrada(
                pedido.getMoneda(), mostrada, ahora)
            : null;
        BigDecimal factorCobro = mostrada != null
            ? monedaVistaService.factorCobroAMostrada(mostrada, ahora)
            : null;

        List<HitoPedidoDTO> hitosDto = new ArrayList<>();
        BigDecimal comision = BigDecimal.ZERO;
        BigDecimal comisionCobro = BigDecimal.ZERO;
        BigDecimal neto = BigDecimal.ZERO;
        BigDecimal netoCobro = BigDecimal.ZERO;

        Map<Long, List<EntregaService.EntregaConArchivos>> entregasPorHito =
            entregaService.listarPorPedido(pedido.getIdPedido()).stream()
                .collect(Collectors.groupingBy(
                    entrega -> entrega.entrega().getHitoPedido()
                        .getIdHitoPedido(),
                    LinkedHashMap::new,
                    Collectors.toList()));

        HitoPedido hitoFinal = pedido.getHitos().stream()
            .max(Comparator.comparingInt(
                hito -> hito.getOrden() != null ? hito.getOrden() : 0))
            .orElse(null);
        boolean tieneEntregaFinal = hitoFinal != null
            && entregasPorHito.getOrDefault(
                hitoFinal.getIdHitoPedido(), List.of()).stream()
                .anyMatch(entrega -> entrega.entrega().isVigente()
                    && !entrega.entrega().isRetirada());
        boolean galeriaClientePublica = pedido.getCliente().isPedidosPublicos();
        boolean publicacionVisible = pedido.getEstado() == EstadoPedido.FINALIZADO
            && pedido.isPublicacionAutorizada()
            && !pedido.isPublicacionOcultadaCliente()
            && galeriaClientePublica;
        boolean puedeAutorizarPublicacion = esArtista
            && (pedido.getEstado() == EstadoPedido.FINALIZADO
                || tieneEntregaFinal);
        boolean puedeOcultarPublicacion = esCliente
            && pedido.getEstado() == EstadoPedido.FINALIZADO
            && pedido.isPublicacionAutorizada();

        for (HitoPedido hito : pedido.getHitos()) {
            List<Pago> pagos = pagoRepository
                .findByHitoPedidoIdHitoPedidoOrderByFechaCreacionDesc(
                    hito.getIdHitoPedido());
            boolean puedePagar = esCliente && puedePagar(pedido, hito, pagos);
            boolean puedeEntregar = esArtista && puedeEntregar(pedido, hito);
            boolean puedeAprobar = esCliente && puedeAprobar(hito);
            boolean puedeSolicitarRevision = esCliente
                && hito.getEstadoEntrega() == EstadoEntregaHito.ENTREGADO;
            boolean incluirEntrega = esArtista || esAdmin
                || hito.getTipo() != TipoHito.ENTREGA
                || hito.getEstadoPago() == EstadoPagoHito.PAGADO;
            boolean incluirOriginal = esArtista || esAdmin
                || hito.getEstadoPago() == EstadoPagoHito.PAGADO;
            boolean hitoAprobado = esAprobado(hito)
                || (hito.getTipo() == TipoHito.ENTREGA
                    && hito.getEstadoPago() == EstadoPagoHito.PAGADO);
            List<EntregaHitoDTO> entregasDto = entregasPorHito
                .getOrDefault(hito.getIdHitoPedido(), List.of()).stream()
                .map(entrega -> EntregaHitoDTO.desdeEntrega(
                    entrega.entrega(),
                    entrega.archivos(),
                    pedido.getIdPedido(),
                    incluirOriginal,
                    entrega.entrega().isVigente() && hitoAprobado))
                .toList();
            List<PagoDTO> pagosDto = pagos.stream()
                .map(pago -> PagoDTO.desdePago(pago, esCliente))
                .toList();

            hitosDto.add(HitoPedidoDTO.desdeHito(
                hito,
                pagosDto,
                incluirComision,
                incluirEntrega,
                entregasDto,
                puedePagar,
                puedeEntregar,
                puedeAprobar,
                puedeSolicitarRevision,
                muestraOrigen,
                muestraCobro,
                factorCobro,
                factorOrigen));

            comision = sumar(comision, hito.getComisionMonto());
            comisionCobro = sumar(comisionCobro, hito.getComisionMontoCobro());
            neto = sumar(neto, hito.getMontoNeto());
            netoCobro = sumar(netoCobro, hito.getMontoNetoCobro());
        }

        return PedidoDTO.builder()
            .idPedido(pedido.getIdPedido())
            .idSolicitud(pedido.getSolicitud().getIdSolicitud())
            .idServicio(pedido.getServicio() != null
                ? pedido.getServicio().getIdServicio()
                : null)
            .nombreServicio(nombreServicio(pedido))
            .cliente(UsuarioResumenDTO.desdeUsuario(pedido.getCliente()))
            .artista(UsuarioResumenDTO.desdeUsuario(pedido.getArtista()))
            .estado(pedido.getEstado() != null
                ? pedido.getEstado().name()
                : null)
            .fechaCreacion(pedido.getFechaCreacion())
            .fechaInicio(pedido.getFechaInicio())
            .limiteEntrega(pedido.getLimiteEntrega())
            .finalizadoEn(pedido.getFinalizadoEn())
            .precioFinal(pedido.getPrecioFinal())
            .precioFinalCobro(pedido.getPrecioFinalCobro())
            .precioFinalMostrado(PresupuestoDTO.totalMostrado(
                pedido.getPrecioFinal(),
                pedido.getPrecioFinalCobro(),
                muestraOrigen,
                muestraCobro,
                factorCobro,
                factorOrigen))
            .moneda(pedido.getMoneda() != null
                ? pedido.getMoneda().getCodigo()
                : null)
            .monedaCobro(pedido.getMonedaCobro() != null
                ? pedido.getMonedaCobro().getCodigo()
                : null)
            .monedaMostrada(mostrada != null
                ? mostrada.getCodigo()
                : (pedido.getMoneda() != null
                    ? pedido.getMoneda().getCodigo()
                    : null))
            .cotizacionCobro(pedido.getCotizacionCobro())
            .comisionPorcentaje(incluirComision
                ? pedido.getComisionPorcentaje()
                : null)
            .comisionMonto(incluirComision ? comision : null)
            .comisionMontoCobro(incluirComision ? comisionCobro : null)
            .comisionMontoMostrado(incluirComision
                ? PresupuestoDTO.totalMostrado(
                    comision,
                    comisionCobro,
                    muestraOrigen,
                    muestraCobro,
                    factorCobro,
                    factorOrigen)
                : null)
            .montoNetoArtista(incluirComision ? neto : null)
            .montoNetoArtistaCobro(incluirComision ? netoCobro : null)
            .montoNetoArtistaMostrado(incluirComision
                ? PresupuestoDTO.totalMostrado(
                    neto,
                    netoCobro,
                    muestraOrigen,
                    muestraCobro,
                    factorCobro,
                    factorOrigen)
                : null)
            .terminosPlataformaVersion(pedido.getTerminosPlataformaVersion())
            .terminosPlataformaAceptadosEn(
                pedido.getTerminosPlataformaAceptadosEn())
            .hitos(hitosDto)
            .dibujos(pedido.getSolicitud().getItems().stream()
                .map(DibujoResumenDTO::desdeItem)
                .toList())
            .puedeCancelar(esCliente
                && (pedido.getEstado() == EstadoPedido.PENDIENTE_PAGO
                    || pedido.getEstado() == EstadoPedido.EN_CURSO))
            .rolVista(esAdmin
                ? "ADMIN"
                : esArtista ? "ARTISTA" : "CLIENTE")
            .idConversacion(conversacion != null
                ? conversacion.getIdConversacion()
                : null)
            .estadoConversacion(conversacion != null
                    && conversacion.getEstado() != null
                ? conversacion.getEstado().name()
                : null)
            .noLeidos(noLeidosDe(conversacion, idVista))
            .publicacionAutorizada(pedido.isPublicacionAutorizada())
            .publicacionAutorizadaEn(pedido.getPublicacionAutorizadaEn())
            .publicacionOcultadaCliente(pedido.isPublicacionOcultadaCliente())
            .publicacionOcultadaEn(pedido.getPublicacionOcultadaEn())
            .galeriaClientePublica(galeriaClientePublica)
            .publicacionVisible(publicacionVisible)
            .puedeAutorizarPublicacion(puedeAutorizarPublicacion)
            .puedeOcultarPublicacion(puedeOcultarPublicacion)
            .build();
    }

    private PedidoResumenDTO construirResumen(Pedido pedido, Long idVista) {
        boolean esArtista = pedido.getArtista().getIdUsuario().equals(idVista);
        boolean esCliente = pedido.getCliente().getIdUsuario().equals(idVista);
        Conversacion conversacion = conversacionDe(pedido);
        LocalDateTime ahora = LocalDateTime.now();
        Moneda mostrada = monedaVistaService.preferidaDe(idVista);
        boolean muestraOrigen = PresupuestoDTO.mismaMoneda(
            mostrada, pedido.getMoneda());
        boolean muestraCobro = !muestraOrigen && PresupuestoDTO.mismaMoneda(
            mostrada, pedido.getMonedaCobro());
        BigDecimal factorOrigen = mostrada != null
            ? monedaVistaService.factorOrigenAMostrada(
                pedido.getMoneda(), mostrada, ahora)
            : null;
        BigDecimal factorCobro = mostrada != null
            ? monedaVistaService.factorCobroAMostrada(mostrada, ahora)
            : null;
        HitoPedido proximo = proximoHito(pedido);
        BigDecimal montoMostrado = proximo != null
            ? PresupuestoDTO.totalMostrado(
                proximo.getMonto(),
                proximo.getMontoCobro(),
                muestraOrigen,
                muestraCobro,
                factorCobro,
                factorOrigen)
            : null;

        return PedidoResumenDTO.builder()
            .idPedido(pedido.getIdPedido())
            .idSolicitud(pedido.getSolicitud().getIdSolicitud())
            .nombreServicio(nombreServicio(pedido))
            .contraparte(UsuarioResumenDTO.desdeUsuario(
                esCliente ? pedido.getArtista() : pedido.getCliente()))
            .estado(pedido.getEstado() != null
                ? pedido.getEstado().name()
                : null)
            .fechaCreacion(pedido.getFechaCreacion())
            .limiteEntrega(pedido.getLimiteEntrega())
            .precioFinalCobro(pedido.getPrecioFinalCobro())
            .monedaCobro(pedido.getMonedaCobro() != null
                ? pedido.getMonedaCobro().getCodigo()
                : null)
            .proximoHitoNombre(proximo != null ? proximo.getNombre() : null)
            .proximoHitoTipo(proximo != null && proximo.getTipo() != null
                ? proximo.getTipo().name()
                : null)
            .proximoHitoEstado(proximo != null
                    && proximo.getEstadoEntrega() != null
                ? proximo.getEstadoEntrega().name()
                : null)
            .proximoHitoMontoMostrado(montoMostrado)
            .monedaMostrada(mostrada != null
                ? mostrada.getCodigo()
                : (pedido.getMoneda() != null
                    ? pedido.getMoneda().getCodigo()
                    : null))
            .rolVista(esArtista ? "ARTISTA" : "CLIENTE")
            .hitosTotales(pedido.getHitos().size())
            .hitosPagados(hitosPagadosDe(pedido))
            .porcentajeAvance(porcentajeAvanceDe(pedido))
            .idConversacion(conversacion != null
                ? conversacion.getIdConversacion()
                : null)
            .estadoConversacion(conversacion != null
                    && conversacion.getEstado() != null
                ? conversacion.getEstado().name()
                : null)
            .noLeidos(noLeidosDe(conversacion, idVista))
            .puedePagar(esCliente && pedido.getHitos().stream()
                .anyMatch(hito -> puedePagar(pedido, hito, List.of())))
            .puedeEntregar(esArtista && pedido.getHitos().stream()
                .anyMatch(hito -> puedeEntregar(pedido, hito)))
            .puedeAprobar(esCliente && pedido.getHitos().stream()
                .anyMatch(this::puedeAprobar))
            .build();
    }

    private int hitosPagadosDe(Pedido pedido) {
        return (int) pedido.getHitos().stream()
            .filter(hito -> hito.getEstadoPago() == EstadoPagoHito.PAGADO)
            .count();
    }

    private int porcentajeAvanceDe(Pedido pedido) {
        int total = pedido.getHitos().size();
        if (total == 0) {
            return 0;
        }
        return Math.round(hitosPagadosDe(pedido) * 100f / total);
    }

    private PaginaDTO<PedidoResumenDTO> aPagina(
            Page<Pedido> page, Long idVista) {
        return PaginaDTO.<PedidoResumenDTO>builder()
            .contenido(page.getContent().stream()
                .map(pedido -> construirResumen(pedido, idVista))
                .toList())
            .pagina(page.getNumber())
            .tamanio(page.getSize())
            .totalElementos(page.getTotalElements())
            .totalPaginas(page.getTotalPages())
            .hayMas(page.hasNext())
            .build();
    }

    private Conversacion conversacionDe(Pedido pedido) {
        return conversacionRepository
            .buscarPorPar(
                pedido.getCliente().getIdUsuario(),
                pedido.getArtista().getIdUsuario())
            .orElse(null);
    }

    private long noLeidosDe(Conversacion conversacion, Long idUsuario) {
        if (conversacion == null
                || idUsuario == null
                || conversacion.getEstado() != EstadoConversacion.ACEPTADA) {
            return 0L;
        }
        List<Object[]> filas = mensajeRepository.contarNoLeidosPorConversacion(
            idUsuario,
            List.of(conversacion.getIdConversacion()),
            EstadoMensaje.ENVIADO);
        if (filas.isEmpty()) {
            return 0L;
        }
        return ((Number) filas.get(0)[1]).longValue();
    }

    private Pageable pageable(int pagina, int tamanio) {
        return PageRequest.of(
            Math.max(0, pagina),
            Math.min(Math.max(1, tamanio), 50),
            Sort.by(Sort.Direction.DESC, "fechaCreacion"));
    }

    private boolean puedePagar(
            Pedido pedido, HitoPedido hito, List<Pago> pagos) {
        if (pedido.getEstado() != EstadoPedido.PENDIENTE_PAGO
                && pedido.getEstado() != EstadoPedido.EN_CURSO) {
            return false;
        }
        if (hito.getEstadoPago() != EstadoPagoHito.PENDIENTE) {
            return false;
        }
        if (pagos != null && pagos.stream()
                .anyMatch(pago -> pago.getEstado() == EstadoPago.PENDIENTE)) {
            return false;
        }
        if (hito.getTipo() == TipoHito.ENTREGA) {
            return hito.getEstadoEntrega() == EstadoEntregaHito.ENTREGADO;
        }
        if (hito.getOrden() == null || hito.getOrden() == 0) {
            return true;
        }
        HitoPedido anterior = hitoAnterior(pedido, hito);
        return anterior != null && esAprobado(anterior);
    }

    private String motivoNoPago(
            Pedido pedido, HitoPedido hito, List<Pago> pagos) {
        if (hito.getEstadoPago() != EstadoPagoHito.PENDIENTE) {
            return "La cuota ya está paga o no admite un pago nuevo.";
        }
        if (pagos != null && pagos.stream()
                .anyMatch(pago -> pago.getEstado() == EstadoPago.PENDIENTE)) {
            return "Ya hay un pago pendiente de confirmación para esta cuota.";
        }
        if (hito.getTipo() == TipoHito.ENTREGA) {
            return "La entrega final todavía no está disponible para pagar.";
        }
        if (hito.getOrden() != null && hito.getOrden() > 0) {
            HitoPedido anterior = hitoAnterior(pedido, hito);
            if (anterior == null || !esAprobado(anterior)) {
                return "Primero tiene que aprobarse la etapa anterior.";
            }
        }
        return "La cuota no está habilitada para pago.";
    }

    private boolean puedeEntregar(Pedido pedido, HitoPedido hito) {
        if (pedido.getEstado() != EstadoPedido.EN_CURSO) {
            return false;
        }
        if (!hito.isRequiereEntrega()) {
            return false;
        }
        if (hito.getEstadoEntrega() != EstadoEntregaHito.NO_INICIADO
                && hito.getEstadoEntrega() != EstadoEntregaHito.EN_PROCESO
                && hito.getEstadoEntrega() != EstadoEntregaHito.CON_REVISION) {
            return false;
        }
        if (hito.getTipo() == TipoHito.ENTREGA) {
            HitoPedido anterior = hitoAnterior(pedido, hito);
            boolean anteriorOk = anterior == null || esAprobado(anterior);
            return anteriorOk
                && hito.getEstadoPago() == EstadoPagoHito.PENDIENTE;
        }
        return hito.getEstadoPago() == EstadoPagoHito.PAGADO;
    }

    private boolean puedeAprobar(HitoPedido hito) {
        return hito.getTipo() != TipoHito.ENTREGA
            && hito.getEstadoEntrega() == EstadoEntregaHito.ENTREGADO;
    }

    private boolean esAprobado(HitoPedido hito) {
        return hito.getEstadoEntrega() == EstadoEntregaHito.APROBADO
            || hito.getEstadoEntrega()
                == EstadoEntregaHito.APROBADO_AUTOMATICAMENTE;
    }

    private HitoPedido hitoAnterior(Pedido pedido, HitoPedido hito) {
        if (hito.getOrden() == null || hito.getOrden() <= 0) {
            return null;
        }
        int orden = hito.getOrden() - 1;
        return pedido.getHitos().stream()
            .filter(candidato -> candidato.getOrden() != null
                && candidato.getOrden() == orden)
            .findFirst()
            .orElse(null);
    }

    private HitoPedido proximoHito(Pedido pedido) {
        for (HitoPedido hito : pedido.getHitos()) {
            if (hito.getEstadoPago() == EstadoPagoHito.CANCELADO) {
                continue;
            }
            if (hito.getEstadoPago() == EstadoPagoHito.PAGADO
                    && esAprobado(hito)) {
                continue;
            }
            return hito;
        }
        return null;
    }

    private void validarVista(
            Pedido pedido, Long idUsuario, boolean esAdmin) {
        boolean esCliente = pedido.getCliente() != null
            && pedido.getCliente().getIdUsuario().equals(idUsuario);
        boolean esArtista = pedido.getArtista() != null
            && pedido.getArtista().getIdUsuario().equals(idUsuario);
        if (!esAdmin && !esCliente && !esArtista) {
            throw new AccesoDenegadoException("No podés ver este pedido.");
        }
    }

    private void validarHitoDelPedido(Pedido pedido, HitoPedido hito) {
        if (hito.getPedido() == null
                || !hito.getPedido().getIdPedido().equals(pedido.getIdPedido())) {
            throw new NoSuchElementException(
                "El hito no pertenece a este pedido.");
        }
    }

    private void notificarEntrega(Pedido pedido, HitoPedido hito) {
        boolean entregaFinal = hito.getTipo() == TipoHito.ENTREGA;
        notificacionService.crearParaUsuario(
            pedido.getCliente().getIdUsuario(),
            entregaFinal ? NOTIF_ENTREGA_LISTA : NOTIF_HITO_ENTREGADO,
            entregaFinal ? "Entrega final lista" : "Entregable recibido",
            entregaFinal
                ? "El artista terminó el pedido de '" + nombreServicio(pedido)
                    + "'. Pagá la última cuota para recibir el archivo final."
                : "El artista entregó '" + hito.getNombre()
                    + "' del pedido de '" + nombreServicio(pedido)
                    + "'. Revisalo y aprobalo.",
            pedido.getIdPedido(),
            TIPO_REFERENCIA);
    }

    private void notificarAprobado(
            Pedido pedido, HitoPedido hito, boolean automatico) {
        String codigo = automatico
            ? NOTIF_HITO_APROBADO_AUTOMATICO
            : NOTIF_HITO_APROBADO;
        String nombre = automatico
            ? "Hito aprobado automáticamente"
            : "Hito aprobado";
        String descripcion = automatico
            ? "Se aprobó automáticamente '" + hito.getNombre()
                + "' del pedido de '" + nombreServicio(pedido) + "'."
            : "El cliente aprobó '" + hito.getNombre()
                + "' del pedido de '" + nombreServicio(pedido) + "'.";
        notificacionService.crearParaUsuario(
            pedido.getArtista().getIdUsuario(),
            codigo, nombre, descripcion,
            pedido.getIdPedido(), TIPO_REFERENCIA);
        if (automatico) {
            notificacionService.crearParaUsuario(
                pedido.getCliente().getIdUsuario(),
                codigo, nombre, descripcion,
                pedido.getIdPedido(), TIPO_REFERENCIA);
        }
    }

    private void notificarRevision(
            Pedido pedido, HitoPedido hito, String motivo) {
        notificacionService.crearParaUsuario(
            pedido.getArtista().getIdUsuario(),
            NOTIF_HITO_CON_REVISION,
            "Revisión solicitada",
            "El cliente pidió una revisión de '" + hito.getNombre()
                + "' del pedido de '" + nombreServicio(pedido)
                + "'. Motivo: "
                + (motivo != null ? motivo : "no especificado") + ".",
            pedido.getIdPedido(),
            TIPO_REFERENCIA);
    }

    private void notificarCancelacion(Pedido pedido) {
        String descripcion = "El pedido de '" + nombreServicio(pedido)
            + "' fue cancelado por el cliente.";
        notificacionService.crearParaUsuario(
            pedido.getCliente().getIdUsuario(),
            NOTIF_PEDIDO_CANCELADO, "Pedido cancelado", descripcion,
            pedido.getIdPedido(), TIPO_REFERENCIA);
        notificacionService.crearParaUsuario(
            pedido.getArtista().getIdUsuario(),
            NOTIF_PEDIDO_CANCELADO, "Pedido cancelado", descripcion,
            pedido.getIdPedido(), TIPO_REFERENCIA);
    }

    private void notificarImpago(Pedido pedido, HitoPedido hito) {
        String descripcion = "El pedido de '" + nombreServicio(pedido)
            + "' se cerró porque no se pagó '" + hito.getNombre() + "'.";
        notificacionService.crearParaUsuario(
            pedido.getCliente().getIdUsuario(),
            NOTIF_PEDIDO_IMPAGO, "Pedido sin pago", descripcion,
            pedido.getIdPedido(), TIPO_REFERENCIA);
        notificacionService.crearParaUsuario(
            pedido.getArtista().getIdUsuario(),
            NOTIF_PEDIDO_IMPAGO, "Pedido sin pago", descripcion,
            pedido.getIdPedido(), TIPO_REFERENCIA);
    }

    private BigDecimal sumar(BigDecimal acumulado, BigDecimal valor) {
        return valor != null ? acumulado.add(valor) : acumulado;
    }

    private String nombreServicio(Pedido pedido) {
        return pedido.getServicio() != null
                && pedido.getServicio().getNombre() != null
            ? pedido.getServicio().getNombre()
            : "tu pedido";
    }

    private String limpiarTexto(String valor, int maximo) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        if (limpio.isEmpty()) {
            return null;
        }
        if (limpio.length() > maximo) {
            throw new IllegalArgumentException(
                "El texto no puede superar los " + maximo + " caracteres.");
        }
        return limpio;
    }
}
