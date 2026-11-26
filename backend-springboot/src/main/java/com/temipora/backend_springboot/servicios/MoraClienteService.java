package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.AlcanceSancion;
import com.temipora.backend_springboot.modelo.ConfiguracionPedido;
import com.temipora.backend_springboot.modelo.DeudaClienteDTO;
import com.temipora.backend_springboot.modelo.EstadoApelacion;
import com.temipora.backend_springboot.modelo.EstadoEntregaHito;
import com.temipora.backend_springboot.modelo.EstadoPagoHito;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.EstadoSancion;
import com.temipora.backend_springboot.modelo.HitoPedido;
import com.temipora.backend_springboot.modelo.MiEstadoCuentaDTO;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.SancionCuenta;
import com.temipora.backend_springboot.modelo.TipoHito;
import com.temipora.backend_springboot.repositorios.ApelacionSancionRepository;
import com.temipora.backend_springboot.repositorios.PedidoRepository;
import com.temipora.backend_springboot.repositorios.SancionCuentaRepository;

/**
 * Detecta y registra la mora del cliente por el pago de la entrega final. El
 * impago se congela en el pedido con el plazo incumplido, no con el instante de
 * la detección, para que un job que corre tarde no corra la escalada.
 *
 * Solo aplica a los pagos que van después de la entrega ({@link TipoHito#ENTREGA}).
 * Las cuotas y la seña se resuelven con la gracia y el paso a IMPAGO de
 * {@code PedidoService.expirarHitos}.
 */
@Service
public class MoraClienteService {

    private static final String NOTIF_PAGO_ATRASADO = "PAGO_ATRASADO";
    private static final String TIPO_REFERENCIA = "PEDIDO";
    private static final String ENTIDAD = "Pedido";
    private static final DateTimeFormatter FECHA =
        DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static final String MENSAJE_COMPRA_BLOQUEADA =
        "Tenés un pago pendiente de una entrega final. Regularizalo para"
            + " volver a contratar o comprar.";
    public static final String MENSAJE_CHAT_BLOQUEADO =
        "Tenés un pago pendiente de una entrega final. Solo podés mensajear al"
            + " artista al que le debés; regularizalo para volver a chatear con"
            + " el resto.";

    private final PedidoRepository pedidoRepository;
    private final ConfiguracionPedidoService configuracionPedidoService;
    private final NotificacionService notificacionService;
    private final AuditoriaService auditoria;
    private final SancionService sancionService;
    private final SancionCuentaRepository sancionRepository;
    private final ApelacionSancionRepository apelacionRepository;

    public MoraClienteService(
            PedidoRepository pedidoRepository,
            ConfiguracionPedidoService configuracionPedidoService,
            NotificacionService notificacionService,
            AuditoriaService auditoria,
            SancionService sancionService,
            SancionCuentaRepository sancionRepository,
            ApelacionSancionRepository apelacionRepository) {
        this.pedidoRepository = pedidoRepository;
        this.configuracionPedidoService = configuracionPedidoService;
        this.notificacionService = notificacionService;
        this.auditoria = auditoria;
        this.sancionService = sancionService;
        this.sancionRepository = sancionRepository;
        this.apelacionRepository = apelacionRepository;
    }

    @Transactional
    public int detectar() {
        LocalDateTime ahora = LocalDateTime.now();
        ConfiguracionPedido config = configuracionPedidoService.obtener();
        List<Pedido> candidatos = pedidoRepository.findCandidatosImpagoCliente(
            EstadoPedido.EN_CURSO,
            config.getSancionesAutomaticasDesde(),
            TipoHito.ENTREGA,
            EstadoPagoHito.PENDIENTE,
            EstadoEntregaHito.ENTREGADO,
            ahora);

        int total = 0;
        for (Pedido pedido : candidatos) {
            HitoPedido hito = hitoImpago(pedido, ahora);
            if (hito == null) {
                continue;
            }
            LocalDateTime plazo = hito.getFechaLimitePago();
            pedido.setImpagoDesde(plazo);
            pedidoRepository.save(pedido);

            notificacionService.crearParaUsuario(
                pedido.getCliente().getIdUsuario(),
                NOTIF_PAGO_ATRASADO,
                "Pago atrasado: tu cuenta en riesgo",
                "El pago de la entrega final de '" + nombreServicio(pedido)
                    + "' venció el " + plazo.format(FECHA) + ". Tenés "
                    + config.getDiasGraciaPago()
                    + (config.getDiasGraciaPago() == 1 ? " día" : " días")
                    + " antes de que se suspendan tus compras y solo puedas"
                    + " mensajear al artista. Si sigue impago, la cuenta queda"
                    + " limitada a pagos: no podés comprar ni contratar.",
                pedido.getIdPedido(),
                TIPO_REFERENCIA);
            auditoria.registrarSistema(
                AccionesAdmin.PEDIDO_IMPAGO_DETECTAR, ENTIDAD,
                pedido.getIdPedido(),
                "Registró el impago de la entrega final"
                    + " (plazo vencido el " + plazo.format(FECHA) + ")");
            total++;
        }
        return total;
    }

    /**
     * Indica si al cliente le queda al menos un pedido con la entrega final
     * impaga. Es la condición que sostiene la sanción de su cuenta.
     */
    @Transactional(readOnly = true)
    public boolean tieneImpago(Long idCliente) {
        return !pedidoRepository.findConImpagoDeCliente(
            idCliente, EstadoPedido.EN_CURSO).isEmpty();
    }

    /**
     * ¿Puede contratar o comprar? Con una sanción de compras o solo-pagos
     * vigente, no.
     */
    @Transactional(readOnly = true)
    public boolean puedeComprar(Long idUsuario) {
        return idUsuario != null
            && !sancionService.tieneSancionDeCompra(idUsuario);
    }

    /** Artistas a los que el cliente les debe una entrega final. */
    @Transactional(readOnly = true)
    public Set<Long> artistasDeudores(Long idCliente) {
        if (idCliente == null) {
            return Set.of();
        }
        return pedidoRepository.findConImpagoDeCliente(
                idCliente, EstadoPedido.EN_CURSO)
            .stream()
            .map(pedido -> pedido.getArtista().getIdUsuario())
            .collect(Collectors.toSet());
    }

    /**
     * Mensajería permitida entre dos usuarios. Un cliente sancionado solo puede
     * hablar con el artista al que le debe; con alcance PAGOS, con nadie.
     */
    @Transactional(readOnly = true)
    public boolean chatPermitido(Long idA, Long idB) {
        return !bloqueaChat(idA, idB) && !bloqueaChat(idB, idA);
    }

    private boolean bloqueaChat(Long idUsuario, Long idOtro) {
        if (idUsuario == null) {
            return false;
        }
        if (sancionService.tieneSancionVigente(
                idUsuario, AlcanceSancion.PAGOS)) {
            return true;
        }
        if (!sancionService.tieneSancionVigente(
                idUsuario, AlcanceSancion.COMPRAS)) {
            return false;
        }
        return !artistasDeudores(idUsuario).contains(idOtro);
    }

    /** Pagos de entrega final que el cliente tiene pendientes. */
    @Transactional(readOnly = true)
    public List<DeudaClienteDTO> deudas(Long idCliente) {
        if (idCliente == null) {
            return List.of();
        }
        List<DeudaClienteDTO> deudas = new ArrayList<>();
        for (Pedido pedido : pedidoRepository.findConImpagoDeCliente(
                idCliente, EstadoPedido.EN_CURSO)) {
            HitoPedido hito = hitoDeEntregaPendiente(pedido);
            if (hito == null) {
                continue;
            }
            deudas.add(DeudaClienteDTO.builder()
                .idPedido(pedido.getIdPedido())
                .nombreServicio(nombreServicio(pedido))
                .idArtista(pedido.getArtista().getIdUsuario())
                .nombreUsuarioArtista(pedido.getArtista().getNombreUsuario())
                .idHitoPedido(hito.getIdHitoPedido())
                .nombreHito(hito.getHitoPresupuesto().getNombre())
                .monto(hito.getHitoPresupuesto().getMonto())
                .montoCobro(hito.getHitoPresupuesto().getMontoCobro())
                .moneda(codigoMoneda(pedido, true))
                .monedaCobro(codigoMoneda(pedido, false))
                .fechaEntrega(hito.getFechaEntrega())
                .fechaLimitePago(hito.getFechaLimitePago())
                .impagoDesde(pedido.getImpagoDesde())
                .diasAtraso(diasAtraso(pedido))
                .puedePagar(puedePagarDeuda(pedido, hito))
                .build());
        }
        return deudas;
    }

    /** Estado consolidado de la cuenta y sus deudas, para el banner y la pantalla restringida. */
    @Transactional(readOnly = true)
    public MiEstadoCuentaDTO estadoCuenta(Long idUsuario) {
        List<DeudaClienteDTO> deudas = deudas(idUsuario);
        SancionCuenta sancion = sancionRepository
            .findVigentesByUsuarioId(idUsuario, EstadoSancion.ACTIVA, LocalDateTime.now())
            .stream()
            .findFirst()
            .orElse(null);
        boolean apelacionPendiente = sancion != null
            && apelacionRepository.findFirstBySancionIdSancionAndEstado(
                sancion.getIdSancion(), EstadoApelacion.PENDIENTE).isPresent();
        BigDecimal total = deudas.stream()
            .map(DeudaClienteDTO::getMontoCobro)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return MiEstadoCuentaDTO.builder()
            .sancionado(sancion != null)
            .alcance(sancion != null && sancion.getAlcance() != null
                ? sancion.getAlcance().name()
                : null)
            .motivo(sancion != null ? sancion.getMotivo() : null)
            .fechaFin(sancion != null ? sancion.getFechaFin() : null)
            .diasRestantes(diasRestantes(sancion))
            .apelacionPendiente(apelacionPendiente)
            .deudas(deudas)
            .totalAdeudado(total)
            .build();
    }

    private HitoPedido hitoDeEntregaPendiente(Pedido pedido) {
        return pedido.getHitos().stream()
            .filter(hito -> hito.getHitoPresupuesto() != null
                && hito.getHitoPresupuesto().getTipo() == TipoHito.ENTREGA)
            .filter(hito -> hito.getEstadoPago() == EstadoPagoHito.PENDIENTE)
            .filter(hito -> hito.getEstadoEntrega() == EstadoEntregaHito.ENTREGADO)
            .findFirst()
            .orElse(null);
    }

    private boolean puedePagarDeuda(Pedido pedido, HitoPedido hito) {
        return pedido.getEstado() == EstadoPedido.EN_CURSO
            && hito.getEstadoPago() == EstadoPagoHito.PENDIENTE
            && hito.getEstadoEntrega() == EstadoEntregaHito.ENTREGADO;
    }

    private String codigoMoneda(Pedido pedido, boolean original) {
        var moneda = original
            ? pedido.getPresupuesto().getMoneda()
            : pedido.getPresupuesto().getMonedaCobro();
        return moneda != null ? moneda.getCodigo() : null;
    }

    private long diasAtraso(Pedido pedido) {
        if (pedido.getImpagoDesde() == null) {
            return 0;
        }
        return Math.max(0, ChronoUnit.DAYS.between(
            pedido.getImpagoDesde().toLocalDate(), LocalDateTime.now().toLocalDate()));
    }

    private Long diasRestantes(SancionCuenta sancion) {
        if (sancion == null || sancion.getFechaFin() == null) {
            return null;
        }
        return Math.max(0, ChronoUnit.DAYS.between(
            LocalDateTime.now().toLocalDate(), sancion.getFechaFin().toLocalDate()));
    }

    /**
     * Hito de entrega final entregado, impago y con el plazo vencido. Se toma
     * el de vencimiento más antiguo por si el pedido tuviera más de uno.
     */
    private HitoPedido hitoImpago(Pedido pedido, LocalDateTime ahora) {
        return pedido.getHitos().stream()
            .filter(hito -> hito.getHitoPresupuesto() != null
                && hito.getHitoPresupuesto().getTipo() == TipoHito.ENTREGA)
            .filter(hito -> hito.getEstadoPago() == EstadoPagoHito.PENDIENTE)
            .filter(hito -> hito.getEstadoEntrega() == EstadoEntregaHito.ENTREGADO)
            .filter(hito -> hito.getFechaLimitePago() != null
                && hito.getFechaLimitePago().isBefore(ahora))
            .min(Comparator.comparing(HitoPedido::getFechaLimitePago))
            .orElse(null);
    }

    private String nombreServicio(Pedido pedido) {
        return pedido.getServicio() != null
                && pedido.getServicio().getNombre() != null
            ? pedido.getServicio().getNombre()
            : "tu pedido";
    }
}
