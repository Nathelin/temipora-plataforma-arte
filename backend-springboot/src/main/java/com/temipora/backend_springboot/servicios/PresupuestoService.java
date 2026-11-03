package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.ConfiguracionPresupuesto;
import com.temipora.backend_springboot.modelo.ConfiguracionComision;
import com.temipora.backend_springboot.modelo.ConfiguracionPedido;
import com.temipora.backend_springboot.modelo.EstadoOfertaReactivacion;
import com.temipora.backend_springboot.modelo.EstadoServicio;
import com.temipora.backend_springboot.modelo.EstadoSolicitud;
import com.temipora.backend_springboot.modelo.HitoPresupuesto;
import com.temipora.backend_springboot.modelo.ItemPresupuesto;
import com.temipora.backend_springboot.modelo.ItemPresupuestoRequest;
import com.temipora.backend_springboot.modelo.ItemSolicitud;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.OfertaReactivacion;
import com.temipora.backend_springboot.modelo.Presupuesto;
import com.temipora.backend_springboot.modelo.PresupuestoRequest;
import com.temipora.backend_springboot.modelo.RecargoItemPresupuesto;
import com.temipora.backend_springboot.modelo.RecargoPresupuestoRequest;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.Solicitud;
import com.temipora.backend_springboot.modelo.SolicitudDTO;
import com.temipora.backend_springboot.modelo.TipoRecargo;
import com.temipora.backend_springboot.repositorios.PresupuestoRepository;
import com.temipora.backend_springboot.repositorios.ServicioRepository;
import com.temipora.backend_springboot.repositorios.SolicitudRepository;

@Service
public class PresupuestoService {

    private static final List<EstadoSolicitud> ESTADOS_PRESUPUESTABLES = List.of(
        EstadoSolicitud.PENDIENTE,
        EstadoSolicitud.EN_LISTA_ESPERA);

    private static final int MAX_RECARGOS_POR_ITEM = 20;
    private static final int MAX_ITEMS = 50;
    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final BigDecimal MAX_PORCENTAJE = new BigDecimal("1000");

    private static final String NOTIFICACION_PRESUPUESTO_RECIBIDO = "PRESUPUESTO_RECIBIDO";
    private static final String NOTIFICACION_PRESUPUESTO_VENCIDO = "PRESUPUESTO_VENCIDO";
    private static final String TIPO_REFERENCIA_SOLICITUD = "SOLICITUD";

    private final SolicitudRepository solicitudRepository;
    private final ServicioRepository servicioRepository;
    private final PresupuestoRepository presupuestoRepository;
    private final ColaSolicitudesService colaSolicitudesService;
    private final ListaEsperaService listaEsperaService;
    private final NotificacionService notificacionService;
    private final ConfiguracionPresupuestoService configuracionPresupuestoService;
    private final ComisionService comisionService;
    private final ConfiguracionPedidoService configuracionPedidoService;
    private final MonedaVistaService monedaVistaService;
    private final OfertaReactivacionService ofertaReactivacionService;
    private final AuditoriaService auditoria;

    private static final String ENTIDAD = "Presupuesto";
    private static final List<CampoAuditado> CAMPOS = List.of(
        CampoAuditado.de("precioBaseTotal", "Precio base total"),
        CampoAuditado.de("precioFinal", "Precio final"),
        CampoAuditado.de("moneda", "Moneda"),
        CampoAuditado.de("monedaCobro", "Moneda de cobro"),
        CampoAuditado.de("cotizacionCobro", "Cotización de cobro"),
        CampoAuditado.de("comisionPorcentaje", "Comisión (%)"),
        CampoAuditado.de("comisionMonto", "Comisión"),
        CampoAuditado.de("montoNetoArtista", "Neto del artista"),
        CampoAuditado.de("descuentoPorcentaje", "Descuento (%)"),
        CampoAuditado.de("descuentoMonto", "Descuento"),
        CampoAuditado.de("precioFinalCliente", "Total del cliente"),
        CampoAuditado.de("tiempoEntregaValor", "Tiempo de entrega"),
        CampoAuditado.de("tiempoEntregaUnidad", "Unidad de entrega"),
        CampoAuditado.de("fechaInicioEstimada", "Inicio estimado"),
        CampoAuditado.de("fechaVencimiento", "Vencimiento"));

    public PresupuestoService(
            SolicitudRepository solicitudRepository,
            ServicioRepository servicioRepository,
            PresupuestoRepository presupuestoRepository,
            ColaSolicitudesService colaSolicitudesService,
            ListaEsperaService listaEsperaService,
            NotificacionService notificacionService,
            ConfiguracionPresupuestoService configuracionPresupuestoService,
            ComisionService comisionService,
            ConfiguracionPedidoService configuracionPedidoService,
            MonedaVistaService monedaVistaService,
            OfertaReactivacionService ofertaReactivacionService,
            AuditoriaService auditoria) {
        this.solicitudRepository = solicitudRepository;
        this.servicioRepository = servicioRepository;
        this.presupuestoRepository = presupuestoRepository;
        this.colaSolicitudesService = colaSolicitudesService;
        this.listaEsperaService = listaEsperaService;
        this.notificacionService = notificacionService;
        this.configuracionPresupuestoService = configuracionPresupuestoService;
        this.comisionService = comisionService;
        this.configuracionPedidoService = configuracionPedidoService;
        this.monedaVistaService = monedaVistaService;
        this.ofertaReactivacionService = ofertaReactivacionService;
        this.auditoria = auditoria;
    }

    @Transactional
    public SolicitudDTO crear(Long idSolicitud, Long idArtista, PresupuestoRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("El presupuesto es obligatorio.");
        }
        if (request.getTiempoEntregaValor() == null || request.getTiempoEntregaValor() < 1) {
            throw new IllegalArgumentException("El tiempo máximo de entrega es obligatorio.");
        }
        if (request.getTiempoEntregaUnidad() == null) {
            throw new IllegalArgumentException("La unidad del tiempo de entrega es obligatoria.");
        }

        LocalDateTime ahora = LocalDateTime.now();
        LocalDate hoy = ahora.toLocalDate();

        if (CalculadoraEntrega.excedePlazoMaximo(
                request.getTiempoEntregaValor(),
                request.getTiempoEntregaUnidad())) {
            throw new IllegalArgumentException(
                "El plazo máximo de entrega es de 1 año "
                    + "(365 días, 52 semanas o 12 meses).");
        }
        if (request.getFechaInicioEstimada() != null) {
            if (request.getFechaInicioEstimada().isBefore(hoy)) {
                throw new IllegalArgumentException(
                    "La fecha de inicio estimada no puede ser anterior a hoy.");
            }
            if (request.getFechaInicioEstimada().isAfter(
                    hoy.plusYears(CalculadoraEntrega.MAX_ANIOS_INICIO_ESTIMADO))) {
                throw new IllegalArgumentException(
                    "La fecha de inicio estimada no puede superar un año desde hoy.");
            }
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException(
                "Tenés que incluir todos los dibujos de la solicitud.");
        }
        if (request.getItems().size() > MAX_ITEMS) {
            throw new IllegalArgumentException(
                "Una solicitud puede tener como máximo " + MAX_ITEMS + " dibujos.");
        }

        Solicitud solicitud = solicitudRepository.findByIdSolicitudForUpdate(idSolicitud)
            .orElseThrow(() -> new NoSuchElementException(
                "Solicitud no encontrada: " + idSolicitud));

        if (solicitud.getArtista() == null
                || !solicitud.getArtista().getIdUsuario().equals(idArtista)) {
            throw new AccesoDenegadoException("No podés presupuestar esta solicitud.");
        }
        if (!ESTADOS_PRESUPUESTABLES.contains(solicitud.getEstado())) {
            throw new ConflictoException(
                "Solo podés presupuestar solicitudes pendientes o en lista de espera.");
        }
        if (presupuestoRepository.existsBySolicitudIdSolicitud(idSolicitud)) {
            throw new ConflictoException("Ya enviaste un presupuesto para esta solicitud.");
        }
        if (solicitud.getServicio() == null
                || solicitud.getServicio().getIdServicio() == null) {
            throw new ConflictoException("La solicitud no tiene un servicio asociado.");
        }

        Servicio servicio = servicioRepository
            .findByIdServicioForUpdate(solicitud.getServicio().getIdServicio())
            .orElseThrow(() -> new NoSuchElementException(
                "Servicio no encontrado: " + solicitud.getServicio().getIdServicio()));
        if (servicio.getEstado() == EstadoServicio.ARCHIVADO) {
            throw new ConflictoException(
                "El servicio está archivado y no admite presupuestos.");
        }

        ColaSolicitudesService.EvaluacionPresupuesto evaluacion =
            colaSolicitudesService.evaluar(solicitud);
        if (!evaluacion.puede()) {
            throw new ConflictoException(evaluacion.motivo());
        }

        Map<Long, ItemSolicitud> itemsSolicitud = new LinkedHashMap<>();
        for (ItemSolicitud item : solicitud.getItems()) {
            itemsSolicitud.put(item.getIdItemSolicitud(), item);
        }
        Map<Long, ItemPresupuestoRequest> requestsPorItem = validarItems(
            request.getItems(), itemsSolicitud);

        BigDecimal baseDibujo = solicitud.getPrecioBase() != null
            ? solicitud.getPrecioBase()
            : servicio.getPrecioBase();
        if (baseDibujo == null || baseDibujo.signum() < 0) {
            throw new ConflictoException("El servicio no tiene un precio base válido.");
        }
        baseDibujo = baseDibujo.setScale(2, RoundingMode.HALF_UP);

        Moneda moneda = solicitud.getMoneda() != null
            ? solicitud.getMoneda()
            : servicio.getMoneda();
        if (moneda == null) {
            throw new ConflictoException("El servicio no tiene una moneda definida.");
        }

        Moneda monedaCobro = monedaVistaService.monedaCobro();
        if (monedaCobro == null) {
            throw new ConflictoException(
                "La moneda de cobro (" + MonedaVistaService.CODIGO_COBRO
                    + ") no está configurada.");
        }
        BigDecimal cotizacionCobro = monedaVistaService.factorCobro(moneda, ahora);
        if (cotizacionCobro == null || cotizacionCobro.signum() <= 0) {
            throw new ConflictoException(
                "No hay cotización de " + moneda.getCodigo() + " a "
                    + monedaCobro.getCodigo()
                    + ". No se puede emitir el presupuesto en este momento.");
        }

        long diasEntrega = CalculadoraEntrega.diasNormalizados(
            request.getTiempoEntregaValor(),
            request.getTiempoEntregaUnidad());

        Presupuesto presupuesto = new Presupuesto();
        presupuesto.setSolicitud(solicitud);
        presupuesto.setMoneda(moneda);
        presupuesto.setTiempoEntregaValor(request.getTiempoEntregaValor());
        presupuesto.setTiempoEntregaUnidad(request.getTiempoEntregaUnidad());
        presupuesto.setTiempoEntregaDias((int) diasEntrega);
        presupuesto.setFechaInicioEstimada(request.getFechaInicioEstimada());
        presupuesto.setMensaje(limpiarTexto(request.getMensaje(), 500));
        presupuesto.setFechaEmision(ahora);
        presupuesto.setFechaVencimiento(
            calcularVencimiento(request.getFechaVencimiento(), ahora));

        BigDecimal totalBase = BigDecimal.ZERO;
        BigDecimal totalFinal = BigDecimal.ZERO;
        int orden = 0;
        for (ItemSolicitud itemSolicitud : solicitud.getItems()) {
            ItemPresupuestoRequest itemRequest =
                requestsPorItem.get(itemSolicitud.getIdItemSolicitud());
            BigDecimal finalDibujo = calcularFinalItem(baseDibujo, itemRequest.getRecargos());

            ItemPresupuesto item = new ItemPresupuesto();
            item.setItemSolicitud(itemSolicitud);
            item.setOrden(orden++);
            item.setPrecioBase(baseDibujo);
            item.setPrecioFinal(finalDibujo);
            item.setNota(limpiarTexto(itemRequest.getNota(), 250));

            List<RecargoPresupuestoRequest> recargos = itemRequest.getRecargos() != null
                ? itemRequest.getRecargos()
                : List.of();
            int ordenRecargo = 0;
            for (RecargoPresupuestoRequest recargoRequest : recargos) {
                RecargoItemPresupuesto recargo = new RecargoItemPresupuesto();
                recargo.setDescripcion(recargoRequest.getDescripcion().trim());
                recargo.setTipo(recargoRequest.getTipo());
                recargo.setValor(recargoRequest.getValor().setScale(2, RoundingMode.HALF_UP));
                recargo.setOrden(ordenRecargo++);
                item.agregarRecargo(recargo);
            }

            presupuesto.agregarItem(item);
            totalBase = totalBase.add(item.getPrecioBase());
            totalFinal = totalFinal.add(item.getPrecioFinal());
        }

        presupuesto.setPrecioBaseTotal(totalBase.setScale(2, RoundingMode.HALF_UP));
        presupuesto.setPrecioFinal(totalFinal.setScale(2, RoundingMode.HALF_UP));
        presupuesto.setMonedaCobro(monedaCobro);
        presupuesto.setCotizacionCobro(cotizacionCobro);
        presupuesto.setPrecioBaseTotalCobro(
            enMoneda(presupuesto.getPrecioBaseTotal(), cotizacionCobro));
        presupuesto.setPrecioFinalCobro(
            enMoneda(presupuesto.getPrecioFinal(), cotizacionCobro));

        ConfiguracionComision comision = comisionService.obtener();
        BigDecimal comisionPorcentaje = comision.getPorcentajeServicio()
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal comisionMonto = comisionService.calcularComision(
            presupuesto.getPrecioFinal(), comisionPorcentaje);
        BigDecimal comisionMontoCobro = enMoneda(comisionMonto, cotizacionCobro);

        presupuesto.setComisionPorcentaje(comisionPorcentaje);
        presupuesto.setComisionMonto(comisionMonto);
        presupuesto.setComisionMontoCobro(comisionMontoCobro);
        presupuesto.setMontoNetoArtista(
            presupuesto.getPrecioFinal().subtract(comisionMonto)
                .setScale(2, RoundingMode.HALF_UP));
        presupuesto.setMontoNetoArtistaCobro(
            presupuesto.getPrecioFinalCobro().subtract(comisionMontoCobro)
                .setScale(2, RoundingMode.HALF_UP));

        OfertaReactivacion oferta = solicitud.getOfertaReactivacion();
        BigDecimal descuentoPorcentaje = BigDecimal.ZERO
            .setScale(2, RoundingMode.HALF_UP);
        if (oferta != null) {
            if (oferta.getEstado() != EstadoOfertaReactivacion.OFRECIDA
                    && oferta.getEstado() != EstadoOfertaReactivacion.RESERVADA) {
                throw new ConflictoException(
                    "La oferta ya fue utilizada o venció.");
            }
            if (oferta.getFechaVencimiento() == null
                    || !oferta.getFechaVencimiento().isAfter(ahora)
                    || oferta.getCampana() == null
                    || oferta.getCampana().getEstado()
                        != com.temipora.backend_springboot.modelo
                            .EstadoCampanaReactivacion.ACTIVA
                    || oferta.getCampana().getFechaVencimiento() == null
                    || !oferta.getCampana().getFechaVencimiento().isAfter(ahora)) {
                throw new ConflictoException(
                    "La oferta venció. Pedile al artista un presupuesto sin descuento.");
            }
            if (oferta.getCampana().getUsuario() == null
                    || !oferta.getCampana().getUsuario().getIdUsuario()
                        .equals(solicitud.getCliente().getIdUsuario())) {
                throw new AccesoDenegadoException(
                    "La oferta no pertenece a tu cuenta.");
            }
            if (oferta.getServicio() == null
                    || !oferta.getServicio().getIdServicio()
                        .equals(servicio.getIdServicio())) {
                throw new ConflictoException(
                    "La oferta no corresponde a este servicio.");
            }
            descuentoPorcentaje = oferta.getPorcentajeDescuento()
                .min(comisionPorcentaje)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        }
        presupuesto.setOfertaReactivacion(oferta);
        presupuesto.setDescuentoPorcentaje(descuentoPorcentaje);

        ConfiguracionPedido configuracionPedido = configuracionPedidoService.obtener();
        List<HitoPresupuesto> hitos = HitoCalculadora.construir(
            request.getHitos(),
            presupuesto.getPrecioFinal(),
            presupuesto.getPrecioFinalCobro(),
            presupuesto.getComisionMonto(),
            presupuesto.getComisionMontoCobro(),
            descuentoPorcentaje,
            new HitoCalculadora.Limites(
                configuracionPedido.getMaxHitos(),
                configuracionPedido.getPorcentajeMaxPrimerHito(),
                configuracionPedido.getMontoMinimoCuota()));
        hitos.forEach(presupuesto::agregarHito);

        BigDecimal totalCliente = hitos.stream()
            .map(HitoPresupuesto::getMonto)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalClienteCobro = hitos.stream()
            .map(HitoPresupuesto::getMontoCobro)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal descuentoMonto = presupuesto.getPrecioFinal()
            .subtract(totalCliente)
            .max(BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal descuentoMontoCobro = presupuesto.getPrecioFinalCobro()
            .subtract(totalClienteCobro)
            .max(BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_UP);

        presupuesto.setDescuentoMonto(descuentoMonto);
        presupuesto.setDescuentoMontoCobro(descuentoMontoCobro);
        presupuesto.setPrecioFinalCliente(totalCliente);
        presupuesto.setPrecioFinalClienteCobro(totalClienteCobro);
        presupuesto.setComisionMonto(
            presupuesto.getComisionMonto().subtract(descuentoMonto)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP));
        presupuesto.setComisionMontoCobro(
            presupuesto.getComisionMontoCobro().subtract(descuentoMontoCobro)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP));

        solicitud.setPresupuesto(presupuesto);
        Presupuesto guardado = presupuestoRepository.save(presupuesto);
        auditoria.registrar(
            AccionesAdmin.PRESUPUESTO_CREAR, ENTIDAD, guardado.getIdPresupuesto(),
            "Envió un presupuesto para la solicitud #" + solicitud.getIdSolicitud(),
            null, guardado, CAMPOS);

        if (oferta != null) {
            ofertaReactivacionService.marcarUtilizada(oferta);
        }

        solicitud.setEstado(EstadoSolicitud.PRESUPUESTADA);
        solicitud.setFechaActualizacion(ahora);
        listaEsperaService.marcarConvertida(solicitud);

        String importeCliente = detalleImporte(
            presupuesto.getPrecioFinalCliente(),
            moneda.getCodigo(),
            presupuesto.getPrecioFinalClienteCobro(),
            monedaCobro.getCodigo());
        String notaDescuento = descuentoMonto.signum() > 0
            ? " (con " + descuentoPorcentaje.stripTrailingZeros().toPlainString()
                + "% de descuento)"
            : "";
        notificacionService.crearParaUsuario(
            solicitud.getCliente().getIdUsuario(),
            NOTIFICACION_PRESUPUESTO_RECIBIDO,
            "Recibiste un presupuesto",
            "El artista envió un presupuesto de " + importeCliente
                + notaDescuento
                + " para '" + nombreServicio(servicio) + "'.",
            solicitud.getIdSolicitud(),
            TIPO_REFERENCIA_SOLICITUD);

        Moneda monedaMostrada = monedaVistaService.preferidaDe(idArtista);
        BigDecimal factorOrigen = monedaMostrada != null
            ? monedaVistaService.factorOrigenAMostrada(moneda, monedaMostrada, ahora)
            : null;
        BigDecimal factorCobro = monedaMostrada != null
            ? monedaVistaService.factorCobroAMostrada(monedaMostrada, ahora)
            : null;
        SolicitudDTO dto = SolicitudDTO.desdeSolicitud(
            solicitud, monedaMostrada, factorOrigen, factorCobro, true);
        dto.setPuedePresupuestar(false);
        dto.setMotivoNoPresupuestable(null);
        dto.setPosicionCola(null);
        return dto;
    }

    @Transactional
    public int expirarVencidos() {
        LocalDateTime ahora = LocalDateTime.now();
        List<Presupuesto> vencidos = presupuestoRepository.findVencidos(
            ahora,
            EstadoSolicitud.PRESUPUESTADA,
            PageRequest.of(0, 100));

        int expirados = 0;
        for (Presupuesto presupuesto : vencidos) {
            if (presupuesto.getSolicitud() == null
                    || presupuesto.getSolicitud().getIdSolicitud() == null) {
                continue;
            }
            Solicitud solicitud = solicitudRepository
                .findByIdSolicitudForUpdate(presupuesto.getSolicitud().getIdSolicitud())
                .orElse(null);
            if (solicitud == null
                    || solicitud.getEstado() != EstadoSolicitud.PRESUPUESTADA) {
                continue;
            }
            if (solicitud.getPedido() != null) {
                continue;
            }
            if (presupuesto.getFechaVencimiento() == null
                    || !presupuesto.getFechaVencimiento().isBefore(ahora)) {
                continue;
            }

            solicitud.setEstado(EstadoSolicitud.VENCIDA);
            solicitud.setFechaActualizacion(ahora);
            notificarVencimiento(solicitud);
            expirados++;
        }
        return expirados;
    }

    private Map<Long, ItemPresupuestoRequest> validarItems(
            List<ItemPresupuestoRequest> itemsRequest,
            Map<Long, ItemSolicitud> itemsSolicitud) {
        Map<Long, ItemPresupuestoRequest> porId = new LinkedHashMap<>();
        for (ItemPresupuestoRequest item : itemsRequest) {
            if (item == null || item.getIdItemSolicitud() == null) {
                throw new IllegalArgumentException(
                    "Cada ítem del presupuesto debe indicar su dibujo.");
            }
            if (porId.putIfAbsent(item.getIdItemSolicitud(), item) != null) {
                throw new IllegalArgumentException(
                    "No podés repetir un dibujo en el presupuesto.");
            }
            if (!itemsSolicitud.containsKey(item.getIdItemSolicitud())) {
                throw new IllegalArgumentException(
                    "El dibujo " + item.getIdItemSolicitud()
                        + " no pertenece a la solicitud.");
            }
        }
        if (porId.size() != itemsSolicitud.size()) {
            throw new IllegalArgumentException(
                "Tenés que incluir todos los dibujos de la solicitud.");
        }
        return porId;
    }

    private BigDecimal calcularFinalItem(
            BigDecimal base,
            List<RecargoPresupuestoRequest> recargos) {
        BigDecimal fijos = BigDecimal.ZERO;
        BigDecimal porcentajes = BigDecimal.ZERO;

        if (recargos != null) {
            if (recargos.size() > MAX_RECARGOS_POR_ITEM) {
                throw new IllegalArgumentException(
                    "Un dibujo puede tener como máximo " + MAX_RECARGOS_POR_ITEM
                        + " recargos.");
            }
            for (RecargoPresupuestoRequest recargo : recargos) {
                if (recargo == null) {
                    throw new IllegalArgumentException("El recargo no es válido.");
                }
                if (recargo.getDescripcion() == null
                        || recargo.getDescripcion().isBlank()) {
                    throw new IllegalArgumentException(
                        "La descripción del recargo es obligatoria.");
                }
                if (recargo.getDescripcion().trim().length() > 100) {
                    throw new IllegalArgumentException(
                        "La descripción del recargo no puede superar los 100 caracteres.");
                }
                if (recargo.getTipo() == null || recargo.getValor() == null) {
                    throw new IllegalArgumentException(
                        "El tipo y el valor del recargo son obligatorios.");
                }
                if (recargo.getValor().signum() <= 0) {
                    throw new IllegalArgumentException(
                        "El valor del recargo debe ser mayor a cero.");
                }
                if (recargo.getTipo() == TipoRecargo.PORCENTAJE) {
                    if (recargo.getValor().compareTo(MAX_PORCENTAJE) > 0) {
                        throw new IllegalArgumentException(
                            "El porcentaje de recargo no puede superar el 1000%.");
                    }
                    porcentajes = porcentajes.add(recargo.getValor());
                } else {
                    fijos = fijos.add(recargo.getValor());
                }
            }
        }

        BigDecimal porcentajeParte = base.multiply(porcentajes)
            .divide(CIEN, 6, RoundingMode.HALF_UP);
        return base.add(fijos).add(porcentajeParte).setScale(2, RoundingMode.HALF_UP);
    }

    private LocalDateTime calcularVencimiento(LocalDate fecha, LocalDateTime ahora) {
        if (fecha != null) {
            LocalDateTime vencimiento = fecha.atTime(23, 59, 59);
            if (!vencimiento.isAfter(ahora)) {
                throw new IllegalArgumentException(
                    "La fecha de vencimiento debe ser futura.");
            }
            return vencimiento;
        }

        ConfiguracionPresupuesto config = configuracionPresupuestoService.obtener();
        Integer horas = config.getVigenciaHoras();
        return ahora.plusHours(horas != null
            ? horas
            : ConfiguracionPresupuesto.VIGENCIA_POR_DEFECTO_HORAS);
    }

    private void notificarVencimiento(Solicitud solicitud) {
        String nombre = nombreServicio(solicitud.getServicio());
        notificacionService.crearParaUsuario(
            solicitud.getCliente().getIdUsuario(),
            NOTIFICACION_PRESUPUESTO_VENCIDO,
            "Presupuesto vencido",
            "El presupuesto de '" + nombre
                + "' venció. Podés volver a solicitar el servicio.",
            solicitud.getIdSolicitud(),
            TIPO_REFERENCIA_SOLICITUD);
        notificacionService.crearParaUsuario(
            solicitud.getArtista().getIdUsuario(),
            NOTIFICACION_PRESUPUESTO_VENCIDO,
            "Presupuesto vencido",
            "El presupuesto de '" + nombre + "' venció y se liberó el cupo.",
            solicitud.getIdSolicitud(),
            TIPO_REFERENCIA_SOLICITUD);
    }

    private BigDecimal enMoneda(BigDecimal monto, BigDecimal factor) {
        return monto.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    private String detalleImporte(
            BigDecimal nativo,
            String codigoNativo,
            BigDecimal cobro,
            String codigoCobro) {
        if (codigoNativo != null && codigoNativo.equalsIgnoreCase(codigoCobro)) {
            return nativo.toPlainString() + " " + codigoNativo;
        }
        return nativo.toPlainString() + " " + codigoNativo
            + " (" + codigoCobro + " " + cobro.toPlainString() + ")";
    }

    private String nombreServicio(Servicio servicio) {
        return servicio != null && servicio.getNombre() != null
            ? servicio.getNombre()
            : "tu servicio";
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
