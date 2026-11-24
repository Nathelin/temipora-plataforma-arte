package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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

import com.temipora.backend_springboot.modelo.ContadoresPedidosDTO;
import com.temipora.backend_springboot.modelo.Conversacion;
import com.temipora.backend_springboot.modelo.DibujoResumenDTO;
import com.temipora.backend_springboot.modelo.EntregaHito;
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
import com.temipora.backend_springboot.modelo.MotivoSustitucion;
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
import com.temipora.backend_springboot.modelo.AceptacionTerminosPlataforma;
import com.temipora.backend_springboot.modelo.AlcanceSancion;
import com.temipora.backend_springboot.modelo.TipoHito;
import com.temipora.backend_springboot.modelo.UsuarioResumenDTO;
import com.temipora.backend_springboot.repositorios.ConversacionRepository;
import com.temipora.backend_springboot.repositorios.HitoPedidoRepository;
import com.temipora.backend_springboot.repositorios.MensajeRepository;
import com.temipora.backend_springboot.repositorios.PagoRepository;
import com.temipora.backend_springboot.repositorios.PedidoRepository;
import com.temipora.backend_springboot.repositorios.SolicitudRepository;
import com.temipora.backend_springboot.servicios.pagos.ArancelEstimadoService;

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
    private static final String NOTIF_PEDIDO_INCUMPLIDO = "PEDIDO_INCUMPLIDO";
    private static final String NOTIF_PUBLICACION_AUTORIZADA =
        "PEDIDO_PUBLICACION_AUTORIZADA";
    private static final String NOTIF_PUBLICACION_RETIRADA =
        "PEDIDO_PUBLICACION_RETIRADA";

    private static final String ENTIDAD_PEDIDO = "Pedido";
    private static final List<CampoAuditado> CAMPOS_PEDIDO = List.of(
        CampoAuditado.de("estado", "Estado"),
        CampoAuditado.de("fechaInicio", "Inicio"),
        CampoAuditado.de("limiteEntrega", "Límite de entrega"),
        CampoAuditado.de("publicacionAutorizada", "Publicación autorizada"),
        CampoAuditado.de("publicacionOcultadaCliente", "Oculto en la galería del cliente"));

    private static final String ENTIDAD_HITO = "HitoPedido";
    private static final List<CampoAuditado> CAMPOS_HITO = List.of(
        CampoAuditado.de("estadoPago", "Estado del pago"),
        CampoAuditado.de("estadoEntrega", "Estado de la entrega"),
        CampoAuditado.de("fechaPago", "Fecha de pago"),
        CampoAuditado.de("fechaEntrega", "Fecha de entrega"),
        CampoAuditado.de("fechaAprobacion", "Fecha de aprobación"),
        CampoAuditado.de("revisiones", "Revisiones solicitadas"),
        CampoAuditado.de("fechaLimiteReentrega", "Plazo de reentrega"));

    private static final String ENTIDAD_ENTREGA = "Entrega";
    private static final List<CampoAuditado> CAMPOS_ENTREGA = List.of(
        CampoAuditado.de("numero", "Versión"),
        CampoAuditado.de("descripcion", "Descripción"),
        CampoAuditado.de("fechaEntrega", "Fecha de entrega"),
        CampoAuditado.de("vigente", "Vigente"));

    private static final List<EstadoPedido> ESTADOS_PANEL_ACTIVOS = List.of(
        EstadoPedido.PENDIENTE_PAGO,
        EstadoPedido.EN_CURSO);
    private static final int MAX_HITOS_PANEL = 10;
    private static final int DIAS_HITO_POR_VENCER = 3;

    private static final String GRUPO_EN_CURSO = "en-curso";
    private static final String GRUPO_ACCION = "accion";
    private static final String GRUPO_FINALIZADOS = "finalizados";
    private static final String GRUPO_CERRADOS = "cerrados";
    private static final String GRUPO_ARCHIVO = "archivo";
    private static final String ORDEN_LIMITE = "limite";
    private static final String ORDEN_MONTO = "monto";

    private static final List<EstadoPedido> ESTADOS_EN_CURSO = List.of(
        EstadoPedido.PENDIENTE_PAGO, EstadoPedido.EN_CURSO);
    private static final List<EstadoPedido> ESTADOS_ARCHIVO = List.of(
        EstadoPedido.FINALIZADO,
        EstadoPedido.CANCELADO,
        EstadoPedido.IMPAGO,
        EstadoPedido.INCUMPLIMIENTO);
    private static final List<EstadoPedido> ESTADOS_CERRADOS = List.of(
        EstadoPedido.CANCELADO,
        EstadoPedido.IMPAGO,
        EstadoPedido.INCUMPLIMIENTO);
    private static final List<EstadoEntregaHito> ESTADOS_ENTREGA_ABIERTA =
        List.of(
            EstadoEntregaHito.NO_INICIADO,
            EstadoEntregaHito.EN_PROCESO,
            EstadoEntregaHito.CON_REVISION);

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
    private final AuditoriaService auditoria;
    private final ArancelEstimadoService arancelEstimadoService;
    private final CuposEventPublisher cuposEventPublisher;
    private final CierreEmailService cierreEmailService;
    private final SancionService sancionService;
    private final EntregaPendienteService entregaPendienteService;

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
            MensajeRepository mensajeRepository,
            AuditoriaService auditoria,
            ArancelEstimadoService arancelEstimadoService,
            CuposEventPublisher cuposEventPublisher,
            CierreEmailService cierreEmailService,
            SancionService sancionService,
            EntregaPendienteService entregaPendienteService) {
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
        this.auditoria = auditoria;
        this.arancelEstimadoService = arancelEstimadoService;
        this.cuposEventPublisher = cuposEventPublisher;
        this.cierreEmailService = cierreEmailService;
        this.sancionService = sancionService;
        this.entregaPendienteService = entregaPendienteService;
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
        if (sancionService.tieneSancionDeCompra(idCliente)) {
            throw new ConflictoException(
                MoraClienteService.MENSAJE_COMPRA_BLOQUEADA);
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
                        && primero.getHitoPresupuesto().getTipo() == TipoHito.SENA
                        && primero.getEstadoPago() == EstadoPagoHito.PENDIENTE
                        && pagoRepository
                            .findFirstByHitoPedidoIdHitoPedidoAndEstado(
                                primero.getIdHitoPedido(), EstadoPago.PENDIENTE)
                            .isEmpty()) {
                    pagoService.iniciarPago(primero, pedidoExistente);
                } else if (primero != null
                        && primero.getHitoPresupuesto().getTipo() == TipoHito.ENTREGA) {
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
        if (solicitud.getArtista() != null
                && sancionService.tieneSancionVigente(
                    solicitud.getArtista().getIdUsuario(),
                    AlcanceSancion.VENTAS)) {
            throw new ConflictoException(
                "El artista no está recibiendo encargos en este momento.");
        }

        AceptacionTerminosPlataforma aceptacionTerminos =
            terminosPlataformaService.obtenerAceptacionVigente(
                solicitud.getCliente().getIdUsuario());

        Pedido pedido = new Pedido();
        pedido.setSolicitud(solicitud);
        pedido.setPresupuesto(presupuesto);
        pedido.setCliente(solicitud.getCliente());
        pedido.setArtista(solicitud.getArtista());
        pedido.setServicio(solicitud.getServicio());
        pedido.setEstado(EstadoPedido.PENDIENTE_PAGO);
        pedido.setAceptacionTerminosPlataforma(aceptacionTerminos);

        for (HitoPresupuesto hitoPresupuesto : presupuesto.getHitos()) {
            HitoPedido hito = new HitoPedido();
            hito.setHitoPresupuesto(hitoPresupuesto);
            hito.setOrden(hitoPresupuesto.getOrden());
            if (hitoPresupuesto.getTipo() == TipoHito.SENA) {
                hito.setFechaLimitePago(
                    ahora.plusDays(hitoFlujoService.diasPagoCuota()));
            }
            pedido.agregarHito(hito);
        }

        pedidoRepository.save(pedido);
        HitoPedido primero = pedido.getHitos().get(0);
        if (primero.getHitoPresupuesto().getTipo() == TipoHito.SENA) {
            pagoService.iniciarPago(primero, pedido);
        } else {
            pagoService.cancelarPendientes(primero);
            iniciarPedidoSinPago(pedido, ahora);
        }
        auditoria.registrar(
            AccionesAdmin.PEDIDO_CREAR, ENTIDAD_PEDIDO, pedido.getIdPedido(),
            "Aceptó el presupuesto de la solicitud #" + solicitud.getIdSolicitud(),
            null, pedido, CAMPOS_PEDIDO);
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
        if (!hito.getHitoPresupuesto().isRequiereEntrega()) {
            throw new ConflictoException("Esta cuota no requiere un entregable.");
        }
        if (hito.getEstadoEntrega() != EstadoEntregaHito.NO_INICIADO
                && hito.getEstadoEntrega() != EstadoEntregaHito.EN_PROCESO
                && hito.getEstadoEntrega() != EstadoEntregaHito.CON_REVISION) {
            throw new ConflictoException("Este hito ya fue entregado.");
        }
        if (hito.getHitoPresupuesto().getTipo() == TipoHito.ENTREGA) {
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

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime plazoEntrega =
            entregaPendienteService.plazoVigente(pedido, hito);
        boolean entregaTardia = plazoEntrega != null
            && ahora.isAfter(plazoEntrega);

        EntregaHito entrega = entregaService.crear(pedido, hito, request, archivos);

        hito.setEstadoEntrega(EstadoEntregaHito.ENTREGADO);
        hito.setFechaEntrega(ahora);
        hito.setFechaLimiteAprobacion(null);
        hito.setFechaLimiteReentrega(null);
        if (hito.getHitoPresupuesto().getTipo() == TipoHito.ENTREGA) {
            hito.setFechaLimitePago(
                ahora.plusDays(hitoFlujoService.diasPagoCuota()));
        } else {
            hito.setFechaLimiteAprobacion(
                ahora.plusDays(hitoFlujoService.diasAprobacion()));
        }
        hitoPedidoRepository.save(hito);
        if (entregaTardia && pedido.getMoraDesde() == null) {
            // La entrega llegó tarde: la mora nace acá, sin esperar al job.
            pedido.setMoraDesde(plazoEntrega);
            pedidoRepository.save(pedido);
            auditoria.registrarSistema(
                AccionesAdmin.PEDIDO_MORA_DETECTAR, ENTIDAD_PEDIDO,
                pedido.getIdPedido(),
                "Registró la mora de entrega al subir el entregable fuera de plazo"
                    + " (vencido el " + plazoEntrega.toLocalDate() + ")");
        }
        auditoria.registrar(
            AccionesAdmin.ENTREGA_CREAR, ENTIDAD_ENTREGA, entrega.getIdEntrega(),
            "Entregó el hito '" + hito.getHitoPresupuesto().getNombre() + "' del pedido #"
                + pedido.getIdPedido() + " (" + archivos.size() + " archivo(s))",
            null, entrega, CAMPOS_ENTREGA);
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

        if (hito.getHitoPresupuesto().getTipo() == TipoHito.ENTREGA) {
            throw new ConflictoException(
                "La entrega final se aprueba al pagar la última cuota.");
        }
        if (hito.getEstadoPago() != EstadoPagoHito.PAGADO) {
            throw new ConflictoException("La cuota todavía no está paga.");
        }
        if (hito.getEstadoEntrega() != EstadoEntregaHito.ENTREGADO) {
            throw new ConflictoException("El hito no está entregado.");
        }

        Snapshot antesHito = Snapshot.de(hito, CAMPOS_HITO);
        LocalDateTime ahora = LocalDateTime.now();
        hito.setEstadoEntrega(EstadoEntregaHito.APROBADO);
        hito.setFechaAprobacion(ahora);
        hito.setFechaLimiteAprobacion(null);
        hitoPedidoRepository.save(hito);
        auditoria.registrar(
            AccionesAdmin.HITO_APROBAR, ENTIDAD_HITO, hito.getIdHitoPedido(),
            "Aprobó el hito '" + hito.getHitoPresupuesto().getNombre() + "' del pedido #"
                + pedido.getIdPedido(),
            antesHito, hito, CAMPOS_HITO);
        HitoPedido siguiente = hitoFlujoService.habilitarSiguiente(
            pedido, hito, ahora);
        limpiarMoraSiCorresponde(pedido, ahora);
        notificarAprobado(pedido, hito, false);
        if (siguiente == null) {
            pedidoCierreService.finalizar(pedido, ahora);
            auditoria.registrar(
                AccionesAdmin.PEDIDO_FINALIZAR, ENTIDAD_PEDIDO, pedido.getIdPedido(),
                "Finalizó el pedido al aprobarse el último hito",
                null, pedido, CAMPOS_PEDIDO);
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

        int maxRevisiones = hitoFlujoService.revisionesMaximasPorHito();
        if (hito.getRevisiones() >= maxRevisiones) {
            throw new ConflictoException(
                "Ya usaste las " + maxRevisiones
                    + " revisiones incluidas en esta etapa.");
        }

        Snapshot antesHito = Snapshot.de(hito, CAMPOS_HITO);
        LocalDateTime ahoraRevision = LocalDateTime.now();
        hito.setEstadoEntrega(EstadoEntregaHito.CON_REVISION);
        hito.setFechaLimiteAprobacion(null);
        hito.setRevisiones(hito.getRevisiones() + 1);
        hito.setFechaLimiteReentrega(
            ahoraRevision.plusDays(hitoFlujoService.diasReentrega()));
        if (hito.getHitoPresupuesto().getTipo() == TipoHito.ENTREGA) {
            hito.setFechaLimitePago(null);
        }
        hitoPedidoRepository.save(hito);
        String motivo = limpiarTexto(
            request != null ? request.getMotivo() : null, 500);
        entregaService.registrarRevision(hito, pedido.getCliente(), motivo);
        auditoria.registrar(
            AccionesAdmin.HITO_REVISION, ENTIDAD_HITO, hito.getIdHitoPedido(),
            motivo != null
                ? "Solicitó una revisión del hito '" + hito.getHitoPresupuesto().getNombre()
                    + "'. Motivo: " + motivo
                : "Solicitó una revisión del hito '" + hito.getHitoPresupuesto().getNombre() + "'",
            antesHito, hito, CAMPOS_HITO);
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

        Snapshot antesPedido = Snapshot.de(pedido, CAMPOS_PEDIDO);
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
        if (pedido.getServicio() != null) {
            cuposEventPublisher.publicar(pedido.getServicio().getIdServicio());
        }
        auditoria.registrar(
            AccionesAdmin.PEDIDO_CANCELAR, ENTIDAD_PEDIDO, pedido.getIdPedido(),
            "Canceló el pedido", antesPedido, pedido, CAMPOS_PEDIDO);
        return construirDTO(pedido, idCliente);
    }

    /**
     * El cliente cierra el pedido cuando el artista ya está en Nivel 2. Conserva
     * los archivos de las etapas entregadas; no hay reembolso.
     */
    @Transactional
    public PedidoDTO cerrarPorIncumplimiento(Long idPedido, Long idCliente) {
        Pedido pedido = pedidoRepository.findByIdPedidoForUpdate(idPedido)
            .orElseThrow(() -> new NoSuchElementException(
                "Pedido no encontrado: " + idPedido));
        if (!pedido.getCliente().getIdUsuario().equals(idCliente)) {
            throw new AccesoDenegadoException(
                "No podés cerrar este pedido.");
        }
        if (pedido.getEstado() != EstadoPedido.EN_CURSO) {
            throw new ConflictoException("El pedido no está en curso.");
        }
        if (!sancionService.tieneSancionVigente(
                pedido.getArtista().getIdUsuario(),
                AlcanceSancion.ENTREGAS)) {
            throw new ConflictoException(
                "El artista todavía no está bloqueado por incumplimiento.");
        }
        cerrarPorIncumplimiento(pedido);
        return construirDTO(pedido, idCliente);
    }

    @Transactional
    public boolean cerrarPorIncumplimientoSistema(Long idPedido) {
        Pedido pedido = pedidoRepository.findByIdPedidoForUpdate(idPedido)
            .orElse(null);
        if (pedido == null || pedido.getEstado() != EstadoPedido.EN_CURSO) {
            return false;
        }
        cerrarPorIncumplimiento(pedido);
        return true;
    }

    private void cerrarPorIncumplimiento(Pedido pedido) {
        LocalDateTime ahora = LocalDateTime.now();
        for (HitoPedido hito : pedido.getHitos()) {
            if (hito.getEstadoPago() == EstadoPagoHito.PENDIENTE) {
                hito.setEstadoPago(EstadoPagoHito.CANCELADO);
                pagoService.cancelarPendientes(hito);
            }
        }
        pedido.setEstado(EstadoPedido.INCUMPLIMIENTO);
        pedido.setIncumplimientoEn(ahora);
        pedido.setCierrePorIncumplimiento(true);
        pedido.setFinalizadoEn(ahora);
        pedidoRepository.save(pedido);
        notificarIncumplimiento(pedido);
        if (pedido.getServicio() != null) {
            cuposEventPublisher.publicar(pedido.getServicio().getIdServicio());
        }
        auditoria.registrarSistema(
            AccionesAdmin.PEDIDO_CERRAR_INCUMPLIMIENTO, ENTIDAD_PEDIDO,
            pedido.getIdPedido(),
            "Cerró el pedido por incumplimiento del artista");
    }

    private void notificarIncumplimiento(Pedido pedido) {
        String descripcion = "El pedido de '" + nombreServicio(pedido)
            + "' se cerró por incumplimiento del artista. Conservás los "
            + "archivos de las etapas entregadas.";
        notificacionService.crearParaUsuario(
            pedido.getCliente().getIdUsuario(),
            NOTIF_PEDIDO_INCUMPLIDO, "Pedido incumplido", descripcion,
            pedido.getIdPedido(), TIPO_REFERENCIA);
        notificacionService.crearParaUsuario(
            pedido.getArtista().getIdUsuario(),
            NOTIF_PEDIDO_INCUMPLIDO, "Pedido cerrado por incumplimiento",
            descripcion, pedido.getIdPedido(), TIPO_REFERENCIA);
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
            Snapshot antesPedido = Snapshot.de(pedido, CAMPOS_PEDIDO);
            pedido.setPublicacionAutorizada(autoriza);
            if (autoriza) {
                pedido.setPublicacionAutorizadaEn(LocalDateTime.now());
            }
            pedidoRepository.save(pedido);
            notificarPublicacion(pedido, autoriza);
            auditoria.registrar(
                autoriza
                    ? AccionesAdmin.PEDIDO_PUBLICACION_AUTORIZAR
                    : AccionesAdmin.PEDIDO_PUBLICACION_RETIRAR,
                ENTIDAD_PEDIDO, pedido.getIdPedido(),
                autoriza
                    ? "Autorizó la publicación del pedido en la galería"
                    : "Retiró la autorización de publicación del pedido",
                antesPedido, pedido, CAMPOS_PEDIDO);
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
            Snapshot antesPedido = Snapshot.de(pedido, CAMPOS_PEDIDO);
            pedido.setPublicacionOcultadaCliente(ocultar);
            pedido.setPublicacionOcultadaEn(
                ocultar ? LocalDateTime.now() : null);
            pedidoRepository.save(pedido);
            auditoria.registrar(
                ocultar
                    ? AccionesAdmin.PEDIDO_GALERIA_OCULTAR
                    : AccionesAdmin.PEDIDO_GALERIA_MOSTRAR,
                ENTIDAD_PEDIDO, pedido.getIdPedido(),
                ocultar
                    ? "Ocultó el pedido de su galería pública"
                    : "Volvió a mostrar el pedido en su galería pública",
                antesPedido, pedido, CAMPOS_PEDIDO);
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

    public record FiltroPedidos(
            EstadoPedido estado,
            String grupo,
            Long idServicio,
            String busqueda,
            String orden) {
    }

    @Transactional(readOnly = true)
    public List<PedidoResumenDTO> pedidosRestringidos(Long idArtista) {
        return pedidoRepository.findEnMoraDeArtista(
                idArtista, EstadoPedido.EN_CURSO).stream()
            .map(pedido -> construirResumen(pedido, idArtista))
            .toList();
    }

    @Transactional(readOnly = true)
    public PaginaDTO<PedidoResumenDTO> listarEnviados(
            Long idCliente, FiltroPedidos filtro, int pagina, int tamanio) {
        return listar(idCliente, false, filtro, pagina, tamanio);
    }

    @Transactional(readOnly = true)
    public PaginaDTO<PedidoResumenDTO> listarRecibidos(
            Long idArtista, FiltroPedidos filtro, int pagina, int tamanio) {
        return listar(idArtista, true, filtro, pagina, tamanio);
    }

    @Transactional(readOnly = true)
    public ContadoresPedidosDTO contadores(Long idVista, boolean recibidos) {
        long todos = recibidos
            ? pedidoRepository.countByArtistaIdUsuario(idVista)
            : pedidoRepository.countByClienteIdUsuario(idVista);
        return ContadoresPedidosDTO.builder()
            .todos(todos)
            .enCurso(contar(recibidos, idVista, ESTADOS_EN_CURSO))
            .archivo(contar(recibidos, idVista, ESTADOS_ARCHIVO))
            .accion(candidatosAccion(
                idVista,
                recibidos,
                new FiltroPedidos(null, GRUPO_ACCION, null, null, null))
                .size())
            .build();
    }

    private PaginaDTO<PedidoResumenDTO> listar(
            Long idVista,
            boolean recibidos,
            FiltroPedidos filtro,
            int pagina,
            int tamanio) {
        String grupo = filtro.grupo() != null ? filtro.grupo().trim() : null;
        if (GRUPO_ACCION.equals(grupo)) {
            return listarAccion(idVista, recibidos, filtro, pagina, tamanio);
        }

        List<EstadoPedido> estados = estadosDe(grupo, filtro.estado());
        Pageable pageable = pageable(pagina, tamanio, orden(filtro.orden()));
        String patron = patron(filtro.busqueda());
        Page<Pedido> page = recibidos
            ? pedidoRepository.buscarRecibidos(
                idVista, estados, filtro.idServicio(), patron, pageable)
            : pedidoRepository.buscarEnviados(
                idVista, estados, filtro.idServicio(), patron, pageable);
        return aPagina(page, idVista);
    }

    private PaginaDTO<PedidoResumenDTO> listarAccion(
            Long idVista,
            boolean recibidos,
            FiltroPedidos filtro,
            int pagina,
            int tamanio) {
        List<Pedido> pedidos = candidatosAccion(idVista, recibidos, filtro);
        int tamanioPagina = Math.min(Math.max(1, tamanio), 50);
        int paginaSegura = Math.max(0, pagina);
        int total = pedidos.size();
        int desde = paginaSegura * tamanioPagina;
        List<PedidoResumenDTO> contenido = desde >= total
            ? List.of()
            : pedidos.subList(desde, Math.min(desde + tamanioPagina, total))
                .stream()
                .map(pedido -> construirResumen(pedido, idVista))
                .toList();

        return PaginaDTO.<PedidoResumenDTO>builder()
            .contenido(contenido)
            .pagina(paginaSegura)
            .tamanio(tamanioPagina)
            .totalElementos(total)
            .totalPaginas((int) Math.ceil((double) total / tamanioPagina))
            .hayMas(desde + contenido.size() < total)
            .build();
    }

    private List<Pedido> candidatosAccion(
            Long idVista, boolean recibidos, FiltroPedidos filtro) {
        String patron = patron(filtro.busqueda());
        List<Pedido> candidatos = recibidos
            ? pedidoRepository.candidatosAccionRecibidos(
                idVista,
                EstadoPedido.EN_CURSO,
                ESTADOS_ENTREGA_ABIERTA,
                filtro.idServicio(),
                patron)
            : pedidoRepository.candidatosAccionEnviados(
                idVista,
                ESTADOS_EN_CURSO,
                EstadoPagoHito.PENDIENTE,
                EstadoEntregaHito.ENTREGADO,
                filtro.idServicio(),
                patron);

        return candidatos.stream()
            .filter(pedido -> tieneAccion(pedido, recibidos))
            .sorted(comparador(filtro.orden()))
            .toList();
    }

    private boolean tieneAccion(Pedido pedido, boolean recibidos) {
        if (recibidos) {
            return pedido.getHitos().stream()
                .anyMatch(hito -> puedeEntregar(pedido, hito));
        }
        return pedido.getHitos().stream()
            .anyMatch(hito -> puedePagar(pedido, hito, List.of())
                || puedeAprobar(hito));
    }

    private long contar(
            boolean recibidos, Long idVista, List<EstadoPedido> estados) {
        return recibidos
            ? pedidoRepository.countByArtistaIdUsuarioAndEstadoIn(
                idVista, estados)
            : pedidoRepository.countByClienteIdUsuarioAndEstadoIn(
                idVista, estados);
    }

    private List<EstadoPedido> estadosDe(String grupo, EstadoPedido estado) {
        if (grupo == null || grupo.isBlank()) {
            return estado != null
                ? List.of(estado)
                : List.of(EstadoPedido.values());
        }
        return switch (grupo) {
            case GRUPO_EN_CURSO -> ESTADOS_EN_CURSO;
            case GRUPO_ARCHIVO -> ESTADOS_ARCHIVO;
            case GRUPO_FINALIZADOS -> List.of(EstadoPedido.FINALIZADO);
            case GRUPO_CERRADOS -> ESTADOS_CERRADOS;
            default -> List.of(EstadoPedido.values());
        };
    }

    private String patron(String busqueda) {
        if (busqueda == null) {
            return "";
        }
        String limpio = busqueda.trim().toLowerCase();
        return limpio.length() < 2 ? "" : limpio;
    }

    private Sort orden(String valor) {
        if (ORDEN_LIMITE.equals(valor)) {
            return Sort.by(Sort.Direction.ASC, "limiteEntrega");
        }
        if (ORDEN_MONTO.equals(valor)) {
            return Sort.by(Sort.Direction.DESC, "presupuesto.precioFinalCobro");
        }
        return Sort.by(Sort.Direction.DESC, "fechaCreacion");
    }

    private Comparator<Pedido> comparador(String valor) {
        if (ORDEN_LIMITE.equals(valor)) {
            return Comparator.comparing(
                Pedido::getLimiteEntrega,
                Comparator.nullsLast(Comparator.<LocalDate>naturalOrder()));
        }
        if (ORDEN_MONTO.equals(valor)) {
            return Comparator.comparing(
                (Pedido pedido) -> pedido.getPresupuesto().getPrecioFinalCobro(),
                Comparator.nullsLast(Comparator.<BigDecimal>reverseOrder()));
        }
        return Comparator.comparing(
            Pedido::getFechaCreacion,
            Comparator.nullsLast(
                Comparator.<LocalDateTime>reverseOrder()));
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
                idArtista, ESTADOS_PANEL_ACTIVOS, PageRequest.of(0, 20));

        List<PedidoResumenDTO> resumenes = new ArrayList<>();
        List<HitoAtencion> atenciones = new ArrayList<>();
        long enCurso = 0;
        long pendientePago = 0;
        long entregasPendientes = 0;
        long conRevision = 0;
        long porVencer = 0;
        BigDecimal porCobrar = BigDecimal.ZERO;

        for (Pedido pedido : pedidos) {
            if (pedido.getEstado() == EstadoPedido.EN_CURSO) {
                enCurso++;
            } else if (pedido.getEstado() == EstadoPedido.PENDIENTE_PAGO) {
                pendientePago++;
            }

            boolean muestraOrigen = PresupuestoDTO.mismaMoneda(
                mostrada, pedido.getPresupuesto().getMoneda());
            boolean muestraCobro = !muestraOrigen
                && PresupuestoDTO.mismaMoneda(
                    mostrada, pedido.getPresupuesto().getMonedaCobro());
            BigDecimal factorCobro = mostrada != null
                ? monedaVistaService.factorCobroAMostrada(mostrada, ahora)
                : null;
            BigDecimal factorOrigen = mostrada != null
                ? monedaVistaService.factorOrigenAMostrada(
                    pedido.getPresupuesto().getMoneda(), mostrada, ahora)
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
                    porCobrar = sumar(porCobrar,
                        hito.getHitoPresupuesto().getMontoNetoCobro());
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
                        0,
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
            0,
            entregasPendientes,
            conRevision,
            porVencer,
            porCobrar,
            monedaVistaService.CODIGO_COBRO);
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
            if (hito == null || hito.getHitoPresupuesto().getTipo() == TipoHito.ENTREGA
                    || hito.getEstadoEntrega() != EstadoEntregaHito.ENTREGADO
                    || hito.getFechaLimiteAprobacion() == null
                    || !hito.getFechaLimiteAprobacion().isBefore(ahora)) {
                continue;
            }
            Pedido pedido = pedidoRepository.findByIdPedidoForUpdate(
                    hito.getPedido().getIdPedido())
                .orElse(null);
            if (pedido == null || pedido.getEstado() != EstadoPedido.EN_CURSO
                    || pedido.getMoraDesde() != null) {
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
            auditoria.registrarSistema(
                AccionesAdmin.HITO_APROBAR_AUTOMATICO, ENTIDAD_HITO,
                hito.getIdHitoPedido(),
                "Aprobó automáticamente el hito '" + hito.getHitoPresupuesto().getNombre()
                    + "' del pedido #" + pedido.getIdPedido());
            if (siguiente == null) {
                pedidoCierreService.finalizar(pedido, ahora);
                auditoria.registrarSistema(
                    AccionesAdmin.PEDIDO_FINALIZAR, ENTIDAD_PEDIDO,
                    pedido.getIdPedido(),
                    "Finalizó el pedido por aprobación automática de la entrega");
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
            // La entrega final impaga no cierra el pedido: la gestiona
            // MoraClienteService para conservar la deuda y poder cobrarla.
            if (hito.getHitoPresupuesto().getTipo() == TipoHito.ENTREGA) {
                continue;
            }
            if (ahora.isBefore(hito.getFechaLimitePago()
                    .plusDays(hitoFlujoService.diasGraciaPago()))) {
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
            cierreEmailService.encolar(
                pedido.getCliente(),
                solicitud,
                pedido,
                MotivoSustitucion.PEDIDO_IMPAGO,
                ahora);
            if (pedido.getServicio() != null) {
                cuposEventPublisher.publicar(pedido.getServicio().getIdServicio());
            }
            auditoria.registrarSistema(
                AccionesAdmin.HITO_VENCER, ENTIDAD_HITO,
                hito.getIdHitoPedido(),
                "Venció el pago del hito '" + hito.getHitoPresupuesto().getNombre()
                    + "' del pedido #" + pedido.getIdPedido()
                    + "; el pedido quedó impago");
            expirados++;
        }
        return expirados;
    }

    /**
     * Días que le quedan al cliente antes de que se le suspendan las compras.
     * Se cuenta hasta el fin de la gracia y nunca es negativo.
     */
    private Integer diasGraciaPagoRestantes(Pedido pedido) {
        if (pedido.getImpagoDesde() == null) {
            return null;
        }
        LocalDateTime finGracia = pedido.getImpagoDesde()
            .plusDays(hitoFlujoService.diasGraciaPago());
        long dias = ChronoUnit.DAYS.between(
            LocalDateTime.now().toLocalDate(), finGracia.toLocalDate());
        return (int) Math.max(0, dias);
    }

    /**
     * Limpia la mora cuando el cliente aprobó todo lo pendiente. Sin esto el
     * pedido queda marcado como en mora para siempre y se congelan la escalera
     * de pagos y la aprobación automática.
     */
    private void limpiarMoraSiCorresponde(Pedido pedido, LocalDateTime ahora) {
        if (pedido.getMoraDesde() == null
                || entregaPendienteService.estaEnMora(pedido, ahora)) {
            return;
        }
        pedido.setMoraDesde(null);
        pedidoRepository.save(pedido);
        auditoria.registrarSistema(
            AccionesAdmin.PEDIDO_MORA_CURAR, ENTIDAD_PEDIDO, pedido.getIdPedido(),
            "Limpió la mora: el cliente aprobó los entregables pendientes");
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
            mostrada, pedido.getPresupuesto().getMoneda());
        boolean muestraCobro = !muestraOrigen && PresupuestoDTO.mismaMoneda(
            mostrada, pedido.getPresupuesto().getMonedaCobro());
        BigDecimal factorOrigen = mostrada != null
            ? monedaVistaService.factorOrigenAMostrada(
                pedido.getPresupuesto().getMoneda(), mostrada, ahora)
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

        int revisionesMaximas = hitoFlujoService.revisionesMaximasPorHito();
        for (HitoPedido hito : pedido.getHitos()) {
            List<Pago> pagos = pagoRepository
                .findByHitoPedidoIdHitoPedidoOrderByFechaCreacionDesc(
                    hito.getIdHitoPedido());
            boolean puedePagar = esCliente && puedePagar(pedido, hito, pagos);
            boolean puedeEntregar = esArtista && puedeEntregar(pedido, hito);
            boolean puedeAprobar = esCliente && puedeAprobar(hito);
            boolean puedeSolicitarRevision = esCliente
                && hito.getEstadoEntrega() == EstadoEntregaHito.ENTREGADO
                && hito.getRevisiones() < revisionesMaximas;
            boolean incluirEntrega = esArtista || esAdmin
                || hito.getHitoPresupuesto().getTipo() != TipoHito.ENTREGA
                || hito.getEstadoPago() == EstadoPagoHito.PAGADO;
            boolean incluirOriginal = esArtista || esAdmin
                || hito.getEstadoPago() == EstadoPagoHito.PAGADO;
            boolean hitoAprobado = esAprobado(hito)
                || (hito.getHitoPresupuesto().getTipo() == TipoHito.ENTREGA
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
                revisionesMaximas,
                muestraOrigen,
                muestraCobro,
                factorCobro,
                factorOrigen));

            comision = sumar(comision,
                hito.getHitoPresupuesto().getComisionMonto());
            comisionCobro = sumar(comisionCobro,
                hito.getHitoPresupuesto().getComisionMontoCobro());
            neto = sumar(neto, hito.getHitoPresupuesto().getMontoNeto());
            netoCobro = sumar(netoCobro,
                hito.getHitoPresupuesto().getMontoNetoCobro());
        }

        BigDecimal finalCliente = pedido.getPresupuesto().getPrecioFinalCliente();
        if (finalCliente == null || finalCliente.signum() == 0) {
            finalCliente = pedido.getPresupuesto().getPrecioFinal();
        }
        BigDecimal finalClienteCobro = pedido.getPresupuesto().getPrecioFinalClienteCobro();
        if (finalClienteCobro == null || finalClienteCobro.signum() == 0) {
            finalClienteCobro = pedido.getPresupuesto().getPrecioFinalCobro();
        }

        BigDecimal comisionNominal = pedido.getPresupuesto().getPrecioFinal()
            .subtract(neto)
            .max(BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal comisionNominalCobro = pedido.getPresupuesto().getPrecioFinalCobro()
            .subtract(netoCobro)
            .max(BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_UP);

        PedidoDTO dto = PedidoDTO.builder()
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
            .fechaEntregaPrevista(PedidoFechas.entregaPrevista(pedido))
            .diasDemoraCliente(PedidoFechas.diasDemoraCliente(pedido))
            .entregaAtrasada(PedidoFechas.entregaAtrasada(
                pedido, LocalDate.now()))
            .finalizadoEn(pedido.getFinalizadoEn())
            .impagoDesde(pedido.getImpagoDesde())
            .pagoVencido(pedido.getImpagoDesde() != null)
            .diasGraciaPagoRestantes(diasGraciaPagoRestantes(pedido))
            .precioFinal(pedido.getPresupuesto().getPrecioFinal())
            .precioFinalCobro(pedido.getPresupuesto().getPrecioFinalCobro())
            .precioFinalMostrado(PresupuestoDTO.totalMostrado(
                pedido.getPresupuesto().getPrecioFinal(),
                pedido.getPresupuesto().getPrecioFinalCobro(),
                muestraOrigen,
                muestraCobro,
                factorCobro,
                factorOrigen))
            .descuentoPorcentaje(pedido.getPresupuesto().getDescuentoPorcentaje())
            .descuentoMonto(pedido.getPresupuesto().getDescuentoMonto())
            .descuentoMontoCobro(pedido.getPresupuesto().getDescuentoMontoCobro())
            .descuentoMostrado(PresupuestoDTO.totalMostrado(
                pedido.getPresupuesto().getDescuentoMonto(),
                pedido.getPresupuesto().getDescuentoMontoCobro(),
                muestraOrigen,
                muestraCobro,
                factorCobro,
                factorOrigen))
            .precioFinalCliente(finalCliente)
            .precioFinalClienteCobro(finalClienteCobro)
            .precioFinalClienteMostrado(PresupuestoDTO.totalMostrado(
                finalCliente,
                finalClienteCobro,
                muestraOrigen,
                muestraCobro,
                factorCobro,
                factorOrigen))
            .moneda(pedido.getPresupuesto().getMoneda() != null
                ? pedido.getPresupuesto().getMoneda().getCodigo()
                : null)
            .monedaCobro(pedido.getPresupuesto().getMonedaCobro() != null
                ? pedido.getPresupuesto().getMonedaCobro().getCodigo()
                : null)
            .monedaMostrada(mostrada != null
                ? mostrada.getCodigo()
                : (pedido.getPresupuesto().getMoneda() != null
                    ? pedido.getPresupuesto().getMoneda().getCodigo()
                    : null))
            .cotizacionCobro(pedido.getPresupuesto().getCotizacionCobro())
            .comisionPorcentaje(incluirComision
                ? pedido.getPresupuesto().getComisionPorcentaje()
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
            .comisionNominal(incluirComision ? comisionNominal : null)
            .comisionNominalCobro(incluirComision ? comisionNominalCobro : null)
            .comisionNominalMostrado(incluirComision
                ? PresupuestoDTO.totalMostrado(
                    comisionNominal,
                    comisionNominalCobro,
                    muestraOrigen,
                    muestraCobro,
                    factorCobro,
                    factorOrigen)
                : null)
            .terminosPlataformaVersion(pedido.getAceptacionTerminosPlataforma() != null
                ? pedido.getAceptacionTerminosPlataforma().getVersion()
                : null)
            .terminosPlataformaAceptadosEn(
                pedido.getAceptacionTerminosPlataforma() != null
                    ? pedido.getAceptacionTerminosPlataforma().getFechaAceptacion()
                    : null)
            .terminosPlataformaHuella(
                pedido.getAceptacionTerminosPlataforma() != null
                    ? HuellaTexto.sha256(
                        pedido.getAceptacionTerminosPlataforma().getSnapshot())
                    : null)
            .terminosArtistaVersion(
                pedido.getSolicitud().getTerminos() != null
                    ? pedido.getSolicitud().getTerminosVersion()
                    : null)
            .terminosArtistaAceptadosEn(
                pedido.getSolicitud().getTerminosAceptadosEn())
            .terminosArtistaDelServicio(
                pedido.getSolicitud().getTerminos() != null
                    && pedido.getSolicitud().getTerminos().getServicio()
                        != null)
            .terminosArtistaHuella(
                HuellaTexto.sha256(
                    pedido.getSolicitud().getTerminosSnapshot()))
            .hitos(hitosDto)
            .dibujos(pedido.getSolicitud().getItems().stream()
                .map(DibujoResumenDTO::desdeItem)
                .toList())
            .puedeCancelar(esCliente
                && (pedido.getEstado() == EstadoPedido.PENDIENTE_PAGO
                    || pedido.getEstado() == EstadoPedido.EN_CURSO))
            .puedeCerrarPorIncumplimiento(esCliente
                && pedido.getEstado() == EstadoPedido.EN_CURSO
                && sancionService.tieneSancionVigente(
                    pedido.getArtista().getIdUsuario(),
                    AlcanceSancion.ENTREGAS))
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

        if (incluirComision) {
            dto.aplicarArancelMp(
                arancelEstimadoService.estimar().porcentajeTotal());
        }
        return dto;
    }

    private PedidoResumenDTO construirResumen(Pedido pedido, Long idVista) {
        boolean esArtista = pedido.getArtista().getIdUsuario().equals(idVista);
        boolean esCliente = pedido.getCliente().getIdUsuario().equals(idVista);
        Conversacion conversacion = conversacionDe(pedido);
        LocalDateTime ahora = LocalDateTime.now();
        Moneda mostrada = monedaVistaService.preferidaDe(idVista);
        boolean muestraOrigen = PresupuestoDTO.mismaMoneda(
            mostrada, pedido.getPresupuesto().getMoneda());
        boolean muestraCobro = !muestraOrigen && PresupuestoDTO.mismaMoneda(
            mostrada, pedido.getPresupuesto().getMonedaCobro());
        BigDecimal factorOrigen = mostrada != null
            ? monedaVistaService.factorOrigenAMostrada(
                pedido.getPresupuesto().getMoneda(), mostrada, ahora)
            : null;
        BigDecimal factorCobro = mostrada != null
            ? monedaVistaService.factorCobroAMostrada(mostrada, ahora)
            : null;
        HitoPedido proximo = proximoHito(pedido);
        BigDecimal montoMostrado = proximo != null
            ? PresupuestoDTO.totalMostrado(
                proximo.getHitoPresupuesto().getMonto(),
                proximo.getHitoPresupuesto().getMontoCobro(),
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
            .fechaEntregaPrevista(PedidoFechas.entregaPrevista(pedido))
            .diasDemoraCliente(PedidoFechas.diasDemoraCliente(pedido))
            .entregaAtrasada(PedidoFechas.entregaAtrasada(
                pedido, LocalDate.now()))
            .impagoDesde(pedido.getImpagoDesde())
            .pagoVencido(pedido.getImpagoDesde() != null)
            .diasGraciaPagoRestantes(diasGraciaPagoRestantes(pedido))
            .precioFinalCobro(pedido.getPresupuesto().getPrecioFinalCobro())
            .monedaCobro(pedido.getPresupuesto().getMonedaCobro() != null
                ? pedido.getPresupuesto().getMonedaCobro().getCodigo()
                : null)
            .proximoHitoNombre(proximo != null
                ? proximo.getHitoPresupuesto().getNombre()
                : null)
            .proximoHitoTipo(proximo != null
                    && proximo.getHitoPresupuesto().getTipo() != null
                ? proximo.getHitoPresupuesto().getTipo().name()
                : null)
            .proximoHitoEstado(proximo != null
                    && proximo.getEstadoEntrega() != null
                ? proximo.getEstadoEntrega().name()
                : null)
            .proximoHitoMontoMostrado(montoMostrado)
            .monedaMostrada(mostrada != null
                ? mostrada.getCodigo()
                : (pedido.getPresupuesto().getMoneda() != null
                    ? pedido.getPresupuesto().getMoneda().getCodigo()
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

    private Pageable pageable(int pagina, int tamanio, Sort sort) {
        return PageRequest.of(
            Math.max(0, pagina),
            Math.min(Math.max(1, tamanio), 50),
            sort);
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
        if (hito.getHitoPresupuesto().getTipo() == TipoHito.ENTREGA) {
            return hito.getEstadoEntrega() == EstadoEntregaHito.ENTREGADO;
        }
        if (hito.getOrden() == null || hito.getOrden() == 0) {
            return true;
        }
        if (hito.getFechaLimitePago() == null) {
            return false;
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
        if (hito.getHitoPresupuesto().getTipo() == TipoHito.ENTREGA) {
            return "La entrega final todavía no está disponible para pagar.";
        }
        if (hito.getOrden() != null && hito.getOrden() > 0
                && hito.getFechaLimitePago() == null) {
            return "La cuota todavía no está habilitada para pagar.";
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
        if (!hito.getHitoPresupuesto().isRequiereEntrega()) {
            return false;
        }
        if (hito.getEstadoEntrega() != EstadoEntregaHito.NO_INICIADO
                && hito.getEstadoEntrega() != EstadoEntregaHito.EN_PROCESO
                && hito.getEstadoEntrega() != EstadoEntregaHito.CON_REVISION) {
            return false;
        }
        if (hito.getHitoPresupuesto().getTipo() == TipoHito.ENTREGA) {
            HitoPedido anterior = hitoAnterior(pedido, hito);
            boolean anteriorOk = anterior == null || esAprobado(anterior);
            return anteriorOk
                && hito.getEstadoPago() == EstadoPagoHito.PENDIENTE;
        }
        return hito.getEstadoPago() == EstadoPagoHito.PAGADO;
    }

    private boolean puedeAprobar(HitoPedido hito) {
        return hito.getHitoPresupuesto().getTipo() != TipoHito.ENTREGA
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
        boolean entregaFinal = hito.getHitoPresupuesto().getTipo() == TipoHito.ENTREGA;
        notificacionService.crearParaUsuario(
            pedido.getCliente().getIdUsuario(),
            entregaFinal ? NOTIF_ENTREGA_LISTA : NOTIF_HITO_ENTREGADO,
            entregaFinal ? "Entrega final lista" : "Entregable recibido",
            entregaFinal
                ? "El artista terminó el pedido de '" + nombreServicio(pedido)
                    + "'. Pagá la última cuota para recibir el archivo final."
                : "El artista entregó '" + hito.getHitoPresupuesto().getNombre()
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
            ? "Se aprobó automáticamente '" + hito.getHitoPresupuesto().getNombre()
                + "' del pedido de '" + nombreServicio(pedido) + "'."
            : "El cliente aprobó '" + hito.getHitoPresupuesto().getNombre()
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
            "El cliente pidió una revisión de '" + hito.getHitoPresupuesto().getNombre()
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
            + "' se cerró porque no se pagó '" + hito.getHitoPresupuesto().getNombre() + "'.";
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
