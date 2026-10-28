package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.EntregaHitoRequest;
import com.temipora.backend_springboot.modelo.EstadoEntregaHito;
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
import com.temipora.backend_springboot.repositorios.HitoPedidoRepository;
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

    private final PedidoRepository pedidoRepository;
    private final HitoPedidoRepository hitoPedidoRepository;
    private final PagoRepository pagoRepository;
    private final SolicitudRepository solicitudRepository;
    private final PagoService pagoService;
    private final HitoFlujoService hitoFlujoService;
    private final TerminosPlataformaService terminosPlataformaService;
    private final NotificacionService notificacionService;
    private final MonedaVistaService monedaVistaService;

    public PedidoService(
            PedidoRepository pedidoRepository,
            HitoPedidoRepository hitoPedidoRepository,
            PagoRepository pagoRepository,
            SolicitudRepository solicitudRepository,
            PagoService pagoService,
            HitoFlujoService hitoFlujoService,
            TerminosPlataformaService terminosPlataformaService,
            NotificacionService notificacionService,
            MonedaVistaService monedaVistaService) {
        this.pedidoRepository = pedidoRepository;
        this.hitoPedidoRepository = hitoPedidoRepository;
        this.pagoRepository = pagoRepository;
        this.solicitudRepository = solicitudRepository;
        this.pagoService = pagoService;
        this.hitoFlujoService = hitoFlujoService;
        this.terminosPlataformaService = terminosPlataformaService;
        this.notificacionService = notificacionService;
        this.monedaVistaService = monedaVistaService;
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
                        && primero.getEstadoPago() == EstadoPagoHito.PENDIENTE
                        && pagoRepository
                            .findFirstByHitoPedidoIdHitoPedidoAndEstado(
                                primero.getIdHitoPedido(), EstadoPago.PENDIENTE)
                            .isEmpty()) {
                    pagoService.iniciarPago(primero, pedidoExistente);
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
        pagoService.iniciarPago(pedido.getHitos().get(0), pedido);
        return construirDTO(pedido, idCliente);
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
            EntregaHitoRequest request) {
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
            if (anterior == null || !esAprobado(anterior)) {
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
        hito.setEstadoEntrega(EstadoEntregaHito.ENTREGADO);
        hito.setFechaEntrega(ahora);
        hito.setEntregaDescripcion(
            limpiarTexto(request.getDescripcion(), 500));
        hito.setEntregaImagen(request.getImagen().trim());
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
        hitoFlujoService.habilitarSiguiente(pedido, hito, ahora);
        notificarAprobado(pedido, hito, false);
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

    @Transactional(readOnly = true)
    public PedidoDTO obtener(Long idPedido, Long idUsuario) {
        Pedido pedido = pedidoRepository.findById(idPedido)
            .orElseThrow(() -> new NoSuchElementException(
                "Pedido no encontrado: " + idPedido));
        validarVista(pedido, idUsuario);
        return construirDTO(pedido, idUsuario);
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
            hitoFlujoService.habilitarSiguiente(pedido, hito, ahora);
            notificarAprobado(pedido, hito, true);
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
        boolean esArtista = pedido.getArtista().getIdUsuario().equals(idVista);
        boolean esCliente = pedido.getCliente().getIdUsuario().equals(idVista);
        boolean incluirComision = esArtista;
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

        for (HitoPedido hito : pedido.getHitos()) {
            List<Pago> pagos = pagoRepository
                .findByHitoPedidoIdHitoPedidoOrderByFechaCreacionDesc(
                    hito.getIdHitoPedido());
            boolean puedePagar = esCliente && puedePagar(pedido, hito, pagos);
            boolean puedeEntregar = esArtista && puedeEntregar(pedido, hito);
            boolean puedeAprobar = esCliente && puedeAprobar(hito);
            boolean puedeSolicitarRevision = esCliente
                && hito.getEstadoEntrega() == EstadoEntregaHito.ENTREGADO;
            boolean incluirEntrega = esArtista
                || hito.getTipo() != TipoHito.ENTREGA
                || hito.getEstadoPago() == EstadoPagoHito.PAGADO;
            List<PagoDTO> pagosDto = pagos.stream()
                .map(pago -> PagoDTO.desdePago(pago, esCliente))
                .toList();

            hitosDto.add(HitoPedidoDTO.desdeHito(
                hito,
                pagosDto,
                incluirComision,
                incluirEntrega,
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
            .puedeCancelar(esCliente
                && (pedido.getEstado() == EstadoPedido.PENDIENTE_PAGO
                    || pedido.getEstado() == EstadoPedido.EN_CURSO))
            .rolVista(esArtista ? "ARTISTA" : "CLIENTE")
            .build();
    }

    private PedidoResumenDTO construirResumen(Pedido pedido, Long idVista) {
        boolean esArtista = pedido.getArtista().getIdUsuario().equals(idVista);
        boolean esCliente = pedido.getCliente().getIdUsuario().equals(idVista);
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
            .puedePagar(esCliente && pedido.getHitos().stream()
                .anyMatch(hito -> puedePagar(pedido, hito, List.of())))
            .puedeEntregar(esArtista && pedido.getHitos().stream()
                .anyMatch(hito -> puedeEntregar(pedido, hito)))
            .puedeAprobar(esCliente && pedido.getHitos().stream()
                .anyMatch(this::puedeAprobar))
            .build();
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
            return anterior != null && esAprobado(anterior)
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

    private void validarVista(Pedido pedido, Long idUsuario) {
        boolean esCliente = pedido.getCliente() != null
            && pedido.getCliente().getIdUsuario().equals(idUsuario);
        boolean esArtista = pedido.getArtista() != null
            && pedido.getArtista().getIdUsuario().equals(idUsuario);
        if (!esCliente && !esArtista) {
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
