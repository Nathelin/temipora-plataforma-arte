package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.ConfiguracionPedido;
import com.temipora.backend_springboot.modelo.HitoPedido;
import com.temipora.backend_springboot.modelo.HitoPresupuesto;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.TipoHito;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.HitoPedidoRepository;

@ExtendWith(MockitoExtension.class)
class HitoFlujoServiceTest {

    @Mock
    private HitoPedidoRepository hitoPedidoRepository;

    @Mock
    private ConfiguracionPedidoService configuracionPedidoService;

    @Mock
    private NotificacionService notificacionService;

    @InjectMocks
    private HitoFlujoService service;

    @Test
    void habilitarSiguiente_sinHitoSiguiente_devuelveNull() {
        when(hitoPedidoRepository
            .findFirstByPedidoIdPedidoAndOrdenGreaterThanOrderByOrdenAsc(10L, 1))
            .thenReturn(Optional.empty());

        assertThat(service.habilitarSiguiente(pedido(), hito(1), ahora())).isNull();
    }

    @Test
    void habilitarSiguiente_cuota_seteaVencimientoYNotifica() {
        HitoPedido siguiente = hito(2);
        siguiente.getHitoPresupuesto().setTipo(TipoHito.CUOTA);
        siguiente.getHitoPresupuesto().setNombre("Cuota 2");
        when(hitoPedidoRepository
            .findFirstByPedidoIdPedidoAndOrdenGreaterThanOrderByOrdenAsc(10L, 1))
            .thenReturn(Optional.of(siguiente));
        when(configuracionPedidoService.obtener()).thenReturn(configConDiasPago(5));

        HitoPedido resultado = service.habilitarSiguiente(pedido(), hito(1), ahora());

        assertThat(resultado).isSameAs(siguiente);
        assertThat(siguiente.getFechaLimitePago()).isEqualTo(ahora().plusDays(5));
        verify(hitoPedidoRepository).save(siguiente);
        verify(notificacionService).crearParaUsuario(
            eq(2L), eq("CUOTA_PENDIENTE"), eq("Cuota habilitada"),
            eq("Se habilitó el pago de 'Cuota 2' del pedido de 'Retrato'."),
            eq(10L), eq("PEDIDO"));
    }

    @Test
    void habilitarSiguiente_entrega_noNotificaNiGuarda() {
        HitoPedido siguiente = hito(2);
        siguiente.getHitoPresupuesto().setTipo(TipoHito.ENTREGA);
        when(hitoPedidoRepository
            .findFirstByPedidoIdPedidoAndOrdenGreaterThanOrderByOrdenAsc(10L, 1))
            .thenReturn(Optional.of(siguiente));

        HitoPedido resultado = service.habilitarSiguiente(pedido(), hito(1), ahora());

        assertThat(resultado).isSameAs(siguiente);
        assertThat(siguiente.getFechaLimitePago()).isNull();
        verify(hitoPedidoRepository, never()).save(any());
        verify(notificacionService, never()).crearParaUsuario(
            any(), any(), any(), any(), any(), any());
    }

    @Test
    void habilitarSiguiente_pedidoSinServicio_usaTextoGenerico() {
        HitoPedido siguiente = hito(2);
        siguiente.getHitoPresupuesto().setTipo(TipoHito.CUOTA);
        when(hitoPedidoRepository
            .findFirstByPedidoIdPedidoAndOrdenGreaterThanOrderByOrdenAsc(10L, 1))
            .thenReturn(Optional.of(siguiente));
        when(configuracionPedidoService.obtener()).thenReturn(configConDiasPago(5));
        Pedido pedido = pedido();
        pedido.setServicio(null);

        service.habilitarSiguiente(pedido, hito(1), ahora());

        verify(notificacionService).crearParaUsuario(
            eq(2L), eq("CUOTA_PENDIENTE"), eq("Cuota habilitada"),
            eq("Se habilitó el pago de 'Hito 2' del pedido de 'tu pedido'."),
            eq(10L), eq("PEDIDO"));
    }

    @Test
    void diasPagoCuota_usaElValorConfigurado() {
        when(configuracionPedidoService.obtener()).thenReturn(configConDiasPago(9));

        assertThat(service.diasPagoCuota()).isEqualTo(9);
    }

    @Test
    void diasPagoCuota_sinValor_usaElDefault() {
        ConfiguracionPedido config = new ConfiguracionPedido();
        config.setDiasPagoCuota(null);
        when(configuracionPedidoService.obtener()).thenReturn(config);

        assertThat(service.diasPagoCuota())
            .isEqualTo(ConfiguracionPedido.DIAS_PAGO_POR_DEFECTO);
    }

    @Test
    void diasAprobacion_usaElDefaultCuandoEsNulo() {
        ConfiguracionPedido config = new ConfiguracionPedido();
        config.setDiasAprobacionAutomatica(null);
        when(configuracionPedidoService.obtener()).thenReturn(config);

        assertThat(service.diasAprobacion())
            .isEqualTo(ConfiguracionPedido.DIAS_APROBACION_POR_DEFECTO);
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private LocalDateTime ahora() {
        return LocalDateTime.of(2026, 3, 1, 12, 0);
    }

    private HitoPedido hito(int orden) {
        HitoPresupuesto hitoPresupuesto = new HitoPresupuesto();
        hitoPresupuesto.setOrden(orden);
        hitoPresupuesto.setNombre("Hito " + orden);
        hitoPresupuesto.setTipo(TipoHito.CUOTA);

        HitoPedido hito = new HitoPedido();
        hito.setOrden(orden);
        hito.setHitoPresupuesto(hitoPresupuesto);
        return hito;
    }

    private Pedido pedido() {
        Pedido pedido = new Pedido();
        pedido.setIdPedido(10L);
        pedido.setCliente(Usuario.builder().idUsuario(2L).nombreUsuario("cliente").build());
        Servicio servicio = new Servicio();
        servicio.setIdServicio(1L);
        servicio.setNombre("Retrato");
        pedido.setServicio(servicio);
        return pedido;
    }

    private ConfiguracionPedido configConDiasPago(int dias) {
        ConfiguracionPedido config = new ConfiguracionPedido();
        config.setDiasPagoCuota(dias);
        return config;
    }
}
