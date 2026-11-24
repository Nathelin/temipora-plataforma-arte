package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
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
import com.temipora.backend_springboot.modelo.OrigenSancion;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.SancionCuenta;
import com.temipora.backend_springboot.repositorios.PedidoRepository;
import com.temipora.backend_springboot.repositorios.SancionCuentaRepository;

/**
 * Escalada automática por incumplimiento de entrega: cura la sanción cuando el
 * cliente aprueba, aplica el Nivel 1 pasado el plazo de gracia, escala al
 * Nivel 2 al vencer la suspensión y cierra los pedidos que quedan sin entregar.
 * El job solo lo agenda; la lógica vive acá para poder probarla y dispararla a
 * mano desde el panel.
 */
@Service
public class SancionIncumplimientoService {

    public record Resumen(
            int curados,
            int escalados,
            int nivel1Aplicados,
            int pedidosCerrados) {
    }

    private final SancionCuentaRepository sancionRepository;
    private final PedidoRepository pedidoRepository;
    private final SancionAutomaticaService sancionAutomaticaService;
    private final ConfiguracionPedidoService configuracionPedidoService;
    private final EntregaPendienteService entregaPendienteService;
    private final PedidoService pedidoService;

    public SancionIncumplimientoService(
            SancionCuentaRepository sancionRepository,
            PedidoRepository pedidoRepository,
            SancionAutomaticaService sancionAutomaticaService,
            ConfiguracionPedidoService configuracionPedidoService,
            EntregaPendienteService entregaPendienteService,
            PedidoService pedidoService) {
        this.sancionRepository = sancionRepository;
        this.pedidoRepository = pedidoRepository;
        this.sancionAutomaticaService = sancionAutomaticaService;
        this.configuracionPedidoService = configuracionPedidoService;
        this.entregaPendienteService = entregaPendienteService;
        this.pedidoService = pedidoService;
    }

    @Transactional
    public Resumen procesar() {
        ConfiguracionPedido config = configuracionPedidoService.obtener();
        LocalDateTime ahora = LocalDateTime.now();
        int diasVentas = config.getDiasSancionVentas() != null
            ? config.getDiasSancionVentas()
            : ConfiguracionPedido.DIAS_SANCION_VENTAS_POR_DEFECTO;
        int diasCierre = config.getDiasCierreTrasBloqueo() != null
            ? config.getDiasCierreTrasBloqueo()
            : ConfiguracionPedido.DIAS_CIERRE_TRAS_BLOQUEO_POR_DEFECTO;

        Resumen curacion = curarOEscalar(ahora);
        int nivel1 = aplicarNivel1(ahora, diasVentas);
        int cerrados = cerrarPedidos(ahora, diasCierre);
        return new Resumen(
            curacion.curados(), curacion.escalados(), nivel1, cerrados);
    }

    private Resumen curarOEscalar(LocalDateTime ahora) {
        int curados = 0;
        int escalados = 0;
        List<SancionCuenta> ventas = sancionRepository.findPorOrigenYAlcance(
            OrigenSancion.AUTOMATICA, AlcanceSancion.VENTAS, EstadoSancion.ACTIVA);
        for (SancionCuenta sancion : ventas) {
            List<Pedido> enMora = enMoraDeArtista(
                sancion.getUsuario().getIdUsuario(), ahora);
            if (enMora.isEmpty()) {
                sancionAutomaticaService.curar(sancion);
                curados++;
                continue;
            }
            if (sancion.getFechaFin() != null
                    && !ahora.isBefore(sancion.getFechaFin())) {
                sancionAutomaticaService.escalarNivel2(
                    sancion, masAntiguo(enMora));
                escalados++;
            }
        }
        return new Resumen(curados, escalados, 0, 0);
    }

    private int aplicarNivel1(
            LocalDateTime ahora, int diasVentas) {
        Map<Long, Pedido> disparadores = new LinkedHashMap<>();
        for (Pedido pedido : pedidoRepository.findEnMora(EstadoPedido.EN_CURSO)) {
            if (pedido.getMoraDesde() == null || !enMora(pedido, ahora)) {
                continue;
            }
            Long idArtista = pedido.getArtista().getIdUsuario();
            disparadores.merge(idArtista, pedido,
                (primero, segundo) -> primero.getMoraDesde()
                    .isBefore(segundo.getMoraDesde()) ? primero : segundo);
        }

        int aplicados = 0;
        for (Map.Entry<Long, Pedido> entrada : disparadores.entrySet()) {
            Pedido pedido = entrada.getValue();
            LocalDateTime plazo = entregaPendienteService.plazoVigente(pedido);
            if (plazo == null || !ahora.isAfter(plazo)) {
                continue;
            }
            if (sancionRepository.findFirstByUsuarioIdUsuarioAndEstado(
                    entrada.getKey(), EstadoSancion.ACTIVA).isPresent()) {
                continue;
            }
            sancionAutomaticaService.aplicarNivel1(
                pedido, diasVentas);
            aplicados++;
        }
        return aplicados;
    }

    private int cerrarPedidos(LocalDateTime ahora, int diasCierre) {
        int cerrados = 0;
        List<SancionCuenta> entregas = sancionRepository.findPorOrigenYAlcance(
            OrigenSancion.AUTOMATICA, AlcanceSancion.ENTREGAS,
            EstadoSancion.ACTIVA);
        for (SancionCuenta sancion : entregas) {
            if (sancion.getFechaEscalada() == null
                    || ahora.isBefore(sancion.getFechaEscalada()
                        .plusDays(diasCierre))) {
                continue;
            }
            List<Pedido> enMora = enMoraDeArtista(
                sancion.getUsuario().getIdUsuario(), ahora);
            for (Pedido pedido : enMora) {
                if (pedidoService.cerrarPorIncumplimientoSistema(
                        pedido.getIdPedido())) {
                    cerrados++;
                }
            }
        }
        return cerrados;
    }

    private List<Pedido> enMoraDeArtista(Long idArtista, LocalDateTime ahora) {
        List<Pedido> enMora = new ArrayList<>();
        for (Pedido pedido : pedidoRepository.findEnMoraDeArtista(
                idArtista, EstadoPedido.EN_CURSO)) {
            if (enMora(pedido, ahora)) {
                enMora.add(pedido);
            }
        }
        return enMora;
    }

    private boolean enMora(Pedido pedido, LocalDateTime ahora) {
        LocalDateTime plazo = entregaPendienteService.plazoVigente(pedido);
        return plazo != null && ahora.isAfter(plazo);
    }

    private Pedido masAntiguo(List<Pedido> pedidos) {
        Pedido elegido = pedidos.get(0);
        for (Pedido pedido : pedidos) {
            if (pedido.getMoraDesde() != null
                    && (elegido.getMoraDesde() == null
                        || pedido.getMoraDesde().isBefore(
                            elegido.getMoraDesde()))) {
                elegido = pedido;
            }
        }
        return elegido;
    }
}
