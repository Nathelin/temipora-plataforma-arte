package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.AlcanceSancion;
import com.temipora.backend_springboot.modelo.ConfiguracionPedido;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.EstadoSancion;
import com.temipora.backend_springboot.modelo.HitoPedido;
import com.temipora.backend_springboot.modelo.OrigenSancion;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.SancionCuenta;
import com.temipora.backend_springboot.modelo.TipoHito;
import com.temipora.backend_springboot.modelo.TipoSancion;
import com.temipora.backend_springboot.repositorios.PedidoRepository;
import com.temipora.backend_springboot.repositorios.SancionCuentaRepository;

/**
 * Escalada automática por impago de la entrega final: cura la sanción cuando el
 * cliente paga, aplica el Nivel 1 (compras suspendidas) pasada la gracia y
 * escala al Nivel 2 (solo pagos) al vencer la suspensión. El pedido no se
 * cierra: la deuda sigue viva para poder cobrarla.
 *
 * El job solo agenda {@link #procesar()}; la lógica vive acá para poder
 * probarla y dispararla a mano desde el panel.
 */
@Service
public class SancionMoraClienteService {

    public record Resumen(
            int curados,
            int escalados,
            int nivel1Aplicados) {
    }

    private static final String NOTIF_COMPRAS = "CUENTA_SUSPENDIDA_COMPRAS";
    private static final String NOTIF_PAGOS = "CUENTA_RESTRINGIDA_PAGOS";
    private static final String TIPO_REFERENCIA = "PEDIDO";
    private static final String ENTIDAD_PEDIDO = "Pedido";
    private static final DateTimeFormatter FECHA =
        DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SancionCuentaRepository sancionRepository;
    private final SancionService sancionService;
    private final PedidoRepository pedidoRepository;
    private final ConfiguracionPedidoService configuracionPedidoService;
    private final MoraClienteService moraClienteService;
    private final NotificacionService notificacionService;
    private final AuditoriaService auditoria;

    public SancionMoraClienteService(
            SancionCuentaRepository sancionRepository,
            SancionService sancionService,
            PedidoRepository pedidoRepository,
            ConfiguracionPedidoService configuracionPedidoService,
            MoraClienteService moraClienteService,
            NotificacionService notificacionService,
            AuditoriaService auditoria) {
        this.sancionRepository = sancionRepository;
        this.sancionService = sancionService;
        this.pedidoRepository = pedidoRepository;
        this.configuracionPedidoService = configuracionPedidoService;
        this.moraClienteService = moraClienteService;
        this.notificacionService = notificacionService;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resumen procesar() {
        ConfiguracionPedido config = configuracionPedidoService.obtener();
        LocalDateTime ahora = LocalDateTime.now();
        int gracia = config.getDiasGraciaPago() != null
            ? config.getDiasGraciaPago()
            : ConfiguracionPedido.DIAS_GRACIA_PAGO_POR_DEFECTO;
        int diasCompras = config.getDiasSancionCompras() != null
            ? config.getDiasSancionCompras()
            : ConfiguracionPedido.DIAS_SANCION_COMPRAS_POR_DEFECTO;

        Resumen curacion = curarOEscalar(ahora);
        int nivel1 = aplicarNivel1(ahora, gracia, diasCompras);
        return new Resumen(curacion.curados(), curacion.escalados(), nivel1);
    }

    private Resumen curarOEscalar(LocalDateTime ahora) {
        int curados = 0;
        int escalados = 0;
        for (SancionCuenta sancion : sancionesAutomaticasActivas()) {
            List<Pedido> impagos = impagosDeCliente(
                sancion.getUsuario().getIdUsuario());
            if (impagos.isEmpty()) {
                curar(sancion);
                curados++;
                continue;
            }
            if (sancion.getAlcance() == AlcanceSancion.COMPRAS
                    && sancion.getFechaFin() != null
                    && !ahora.isBefore(sancion.getFechaFin())) {
                escalarNivel2(sancion, masAntiguo(impagos));
                escalados++;
            }
        }
        return new Resumen(curados, escalados, 0);
    }

    private int aplicarNivel1(
            LocalDateTime ahora, int gracia, int diasCompras) {
        Map<Long, Pedido> disparadores = new LinkedHashMap<>();
        for (Pedido pedido : pedidoRepository.findConImpago(
                EstadoPedido.EN_CURSO)) {
            if (pedido.getImpagoDesde() == null) {
                continue;
            }
            Long idCliente = pedido.getCliente().getIdUsuario();
            disparadores.merge(idCliente, pedido,
                (primero, segundo) -> primero.getImpagoDesde()
                    .isBefore(segundo.getImpagoDesde()) ? primero : segundo);
        }

        int aplicados = 0;
        for (Map.Entry<Long, Pedido> entrada : disparadores.entrySet()) {
            Pedido pedido = entrada.getValue();
            if (ahora.isBefore(
                    pedido.getImpagoDesde().plusDays(gracia))) {
                continue;
            }
            if (sancionRepository.findFirstByUsuarioIdUsuarioAndEstado(
                    entrada.getKey(), EstadoSancion.ACTIVA).isPresent()) {
                continue;
            }
            aplicarNivel1(pedido, gracia, diasCompras);
            aplicados++;
        }
        return aplicados;
    }

    @Transactional
    public SancionCuenta aplicarNivel1(
            Pedido pedido, int gracia, int diasCompras) {
        LocalDateTime ahora = LocalDateTime.now();
        // La ventana se cuenta desde el vencimiento; si el job corre tarde se
        // garantiza la duración completa desde la aplicación.
        LocalDateTime inicio = pedido.getImpagoDesde().plusDays(gracia);
        if (inicio.isBefore(ahora)) {
            inicio = ahora;
        }
        LocalDateTime fin = inicio.plusDays(Math.max(1, diasCompras));
        String clave = "AUTO:PAGO:" + pedido.getIdPedido() + ":NIVEL1";

        SancionCuenta sancion = sancionService.aplicarSistema(
            pedido.getCliente().getIdUsuario(),
            TipoSancion.SUSPENSION,
            AlcanceSancion.COMPRAS,
            motivoNivel1(pedido),
            fin,
            clave,
            pedido,
            hitoDeEntrega(pedido),
            pedido.getImpagoDesde());
        if (sancion.getOrigen() != OrigenSancion.AUTOMATICA) {
            return sancion;
        }
        notificacionService.crearParaUsuario(
            pedido.getCliente().getIdUsuario(),
            NOTIF_COMPRAS,
            "Compras suspendidas",
            "No pagaste la entrega final del pedido #" + pedido.getIdPedido()
                + ". Tus compras quedan suspendidas hasta el " + fin.format(FECHA)
                + " y solo podés mensajear al artista al que le debés. Podés"
                + " regularizar el pago desde el detalle del pedido.",
            pedido.getIdPedido(),
            TIPO_REFERENCIA);
        return sancion;
    }

    @Transactional
    public SancionCuenta escalarNivel2(SancionCuenta sancion, Pedido pedido) {
        String clave = "AUTO:PAGO:" + pedido.getIdPedido() + ":NIVEL2";
        SancionCuenta escalada = sancionService.escalarSistema(
            sancion.getIdSancion(),
            AlcanceSancion.PAGOS,
            motivoNivel2(pedido),
            clave,
            pedido,
            hitoDeEntrega(pedido),
            pedido.getImpagoDesde());
        notificacionService.crearParaUsuario(
            escalada.getUsuario().getIdUsuario(),
            NOTIF_PAGOS,
            "Cuenta limitada a pagos",
            "Tu cuenta quedó limitada a regularizar el pago pendiente del"
                + " pedido #" + pedido.getIdPedido() + ". Podés pagar la deuda,"
                + " ver tus notificaciones y apelar.",
            pedido.getIdPedido(),
            TIPO_REFERENCIA);
        return escalada;
    }

    @Transactional
    public void curar(SancionCuenta sancion) {
        SancionCuenta revertida = sancionService.revertirSistema(
            sancion.getIdSancion(),
            "Se regularizó el pago pendiente: el cliente pagó la entrega final");
        if (revertida.getPedido() != null) {
            auditoria.registrarSistema(
                AccionesAdmin.PEDIDO_IMPAGO_CURAR, ENTIDAD_PEDIDO,
                revertida.getPedido().getIdPedido(),
                "Se curó el impago de la entrega final al acreditarse el pago");
        }
    }

    /**
     * Cura la sanción del cliente si ya no le queda ningún pedido impago. Se
     * llama al acreditarse un pago y también la usa el job como red de
     * seguridad.
     */
    @Transactional
    public boolean curarSiCorresponde(Long idCliente) {
        if (idCliente == null || moraClienteService.tieneImpago(idCliente)) {
            return false;
        }
        SancionCuenta activa = sancionRepository
            .findFirstByUsuarioIdUsuarioAndEstado(
                idCliente, EstadoSancion.ACTIVA)
            .orElse(null);
        if (activa == null || activa.getOrigen() != OrigenSancion.AUTOMATICA) {
            return false;
        }
        if (activa.getAlcance() != AlcanceSancion.COMPRAS
                && activa.getAlcance() != AlcanceSancion.PAGOS) {
            return false;
        }
        curar(activa);
        return true;
    }

    private List<SancionCuenta> sancionesAutomaticasActivas() {
        Map<Long, SancionCuenta> porUsuario = new LinkedHashMap<>();
        for (AlcanceSancion alcance : List.of(
                AlcanceSancion.COMPRAS, AlcanceSancion.PAGOS)) {
            for (SancionCuenta sancion : sancionRepository.findPorOrigenYAlcance(
                    OrigenSancion.AUTOMATICA, alcance, EstadoSancion.ACTIVA)) {
                porUsuario.putIfAbsent(
                    sancion.getUsuario().getIdUsuario(), sancion);
            }
        }
        return new ArrayList<>(porUsuario.values());
    }

    private List<Pedido> impagosDeCliente(Long idCliente) {
        return pedidoRepository.findConImpagoDeCliente(
            idCliente, EstadoPedido.EN_CURSO);
    }

    private Pedido masAntiguo(List<Pedido> pedidos) {
        Pedido elegido = pedidos.get(0);
        for (Pedido pedido : pedidos) {
            if (pedido.getImpagoDesde() != null
                    && (elegido.getImpagoDesde() == null
                        || pedido.getImpagoDesde().isBefore(
                            elegido.getImpagoDesde()))) {
                elegido = pedido;
            }
        }
        return elegido;
    }

    private HitoPedido hitoDeEntrega(Pedido pedido) {
        return pedido.getHitos().stream()
            .filter(hito -> hito.getHitoPresupuesto() != null
                && hito.getHitoPresupuesto().getTipo() == TipoHito.ENTREGA)
            .findFirst()
            .orElse(null);
    }

    private String motivoNivel1(Pedido pedido) {
        return "No pagó '" + nombreHito(pedido) + "' del pedido #"
            + pedido.getIdPedido() + " en el plazo previsto (vencido el "
            + pedido.getImpagoDesde().format(FECHA) + ").";
    }

    private String motivoNivel2(Pedido pedido) {
        return "No pagó '" + nombreHito(pedido) + "' del pedido #"
            + pedido.getIdPedido()
            + " dentro de la ventana de la suspensión de compras.";
    }

    private String nombreHito(Pedido pedido) {
        HitoPedido hito = hitoDeEntrega(pedido);
        return hito != null && hito.getHitoPresupuesto() != null
                && hito.getHitoPresupuesto().getNombre() != null
            ? hito.getHitoPresupuesto().getNombre()
            : "la entrega final";
    }
}
