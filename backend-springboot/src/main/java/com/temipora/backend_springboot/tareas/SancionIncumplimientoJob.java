package com.temipora.backend_springboot.tareas;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.temipora.backend_springboot.modelo.AlcanceSancion;
import com.temipora.backend_springboot.modelo.ConfiguracionPedido;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.EstadoSancion;
import com.temipora.backend_springboot.modelo.OrigenSancion;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.SancionCuenta;
import com.temipora.backend_springboot.repositorios.PedidoRepository;
import com.temipora.backend_springboot.repositorios.SancionCuentaRepository;
import com.temipora.backend_springboot.servicios.ConfiguracionPedidoService;
import com.temipora.backend_springboot.servicios.PedidoService;
import com.temipora.backend_springboot.servicios.SancionAutomaticaService;

/**
 * Escalada automática por incumplimiento de entrega: cura la sanción cuando el
 * cliente aprueba, aplica el Nivel 1 pasado el plazo de gracia, escala al
 * Nivel 2 al vencer la suspensión y cierra los pedidos que quedan sin entregar.
 */
@Component
public class SancionIncumplimientoJob {

    private static final Logger log =
        LoggerFactory.getLogger(SancionIncumplimientoJob.class);

    private final SancionCuentaRepository sancionRepository;
    private final PedidoRepository pedidoRepository;
    private final SancionAutomaticaService sancionAutomaticaService;
    private final ConfiguracionPedidoService configuracionPedidoService;
    private final PedidoService pedidoService;

    @Value("${temipora.incumplimiento.programado:true}")
    private boolean habilitado;

    public SancionIncumplimientoJob(
            SancionCuentaRepository sancionRepository,
            PedidoRepository pedidoRepository,
            SancionAutomaticaService sancionAutomaticaService,
            ConfiguracionPedidoService configuracionPedidoService,
            PedidoService pedidoService) {
        this.sancionRepository = sancionRepository;
        this.pedidoRepository = pedidoRepository;
        this.sancionAutomaticaService = sancionAutomaticaService;
        this.configuracionPedidoService = configuracionPedidoService;
        this.pedidoService = pedidoService;
    }

    @Scheduled(initialDelayString = "PT5M", fixedDelayString = "PT1H")
    public void ejecutar() {
        if (!habilitado) {
            return;
        }
        try {
            procesar();
        } catch (Exception e) {
            log.error("Error al procesar sanciones por incumplimiento", e);
        }
    }

    private void procesar() {
        ConfiguracionPedido config = configuracionPedidoService.obtener();
        LocalDateTime ahora = LocalDateTime.now();
        int gracia = config.getDiasGraciaEntrega() != null
            ? config.getDiasGraciaEntrega()
            : ConfiguracionPedido.DIAS_GRACIA_ENTREGA_POR_DEFECTO;
        int diasVentas = config.getDiasSancionVentas() != null
            ? config.getDiasSancionVentas()
            : ConfiguracionPedido.DIAS_SANCION_VENTAS_POR_DEFECTO;
        int diasCierre = config.getDiasCierreTrasBloqueo() != null
            ? config.getDiasCierreTrasBloqueo()
            : ConfiguracionPedido.DIAS_CIERRE_TRAS_BLOQUEO_POR_DEFECTO;

        curarOEscalar(ahora);
        aplicarNivel1(ahora, gracia, diasVentas);
        cerrarPedidos(ahora, diasCierre);
    }

    private void curarOEscalar(LocalDateTime ahora) {
        List<SancionCuenta> ventas = sancionRepository.findPorOrigenYAlcance(
            OrigenSancion.AUTOMATICA, AlcanceSancion.VENTAS, EstadoSancion.ACTIVA);
        for (SancionCuenta sancion : ventas) {
            List<Pedido> enMora = pedidoRepository.findEnMoraDeArtista(
                sancion.getUsuario().getIdUsuario(), EstadoPedido.EN_CURSO);
            if (enMora.isEmpty()) {
                sancionAutomaticaService.curar(sancion);
                continue;
            }
            if (sancion.getFechaFin() != null
                    && !ahora.isBefore(sancion.getFechaFin())) {
                sancionAutomaticaService.escalarNivel2(
                    sancion, masAntiguo(enMora));
            }
        }
    }

    private void aplicarNivel1(
            LocalDateTime ahora, int gracia, int diasVentas) {
        Map<Long, Pedido> disparadores = new LinkedHashMap<>();
        for (Pedido pedido : pedidoRepository.findEnMora(EstadoPedido.EN_CURSO)) {
            if (pedido.getMoraDesde() == null) {
                continue;
            }
            Long idArtista = pedido.getArtista().getIdUsuario();
            disparadores.merge(idArtista, pedido,
                (primero, segundo) -> primero.getMoraDesde()
                    .isBefore(segundo.getMoraDesde()) ? primero : segundo);
        }

        for (Map.Entry<Long, Pedido> entrada : disparadores.entrySet()) {
            Pedido pedido = entrada.getValue();
            if (ahora.isBefore(pedido.getMoraDesde().plusDays(gracia))) {
                continue;
            }
            if (sancionRepository.findFirstByUsuarioIdUsuarioAndEstado(
                    entrada.getKey(), EstadoSancion.ACTIVA).isPresent()) {
                continue;
            }
            sancionAutomaticaService.aplicarNivel1(
                pedido, gracia, diasVentas);
        }
    }

    private void cerrarPedidos(LocalDateTime ahora, int diasCierre) {
        List<SancionCuenta> entregas = sancionRepository.findPorOrigenYAlcance(
            OrigenSancion.AUTOMATICA, AlcanceSancion.ENTREGAS,
            EstadoSancion.ACTIVA);
        for (SancionCuenta sancion : entregas) {
            if (sancion.getFechaEscalada() == null
                    || ahora.isBefore(sancion.getFechaEscalada()
                        .plusDays(diasCierre))) {
                continue;
            }
            List<Pedido> enMora = pedidoRepository.findEnMoraDeArtista(
                sancion.getUsuario().getIdUsuario(), EstadoPedido.EN_CURSO);
            for (Pedido pedido : enMora) {
                pedidoService.cerrarPorIncumplimientoSistema(
                    pedido.getIdPedido());
            }
        }
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
