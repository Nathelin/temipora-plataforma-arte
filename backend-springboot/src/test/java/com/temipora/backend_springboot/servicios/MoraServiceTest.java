package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.ConfiguracionPedido;
import com.temipora.backend_springboot.modelo.EstadoEntregaHito;
import com.temipora.backend_springboot.modelo.EstadoPagoHito;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.HitoPedido;
import com.temipora.backend_springboot.modelo.HitoPresupuesto;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.TipoHito;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.PedidoRepository;

@ExtendWith(MockitoExtension.class)
class MoraServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ConfiguracionPedidoService configuracionPedidoService;

    @Mock
    private NotificacionService notificacionService;

    @Mock
    private AuditoriaService auditoria;

    @Spy
    private EntregaPendienteService entregaPendienteService =
        new EntregaPendienteService();

    @InjectMocks
    private MoraService moraService;

    @Test
    void detectar_registraLaMoraConElPlazoYNotifica() {
        ConfiguracionPedido config = configuracion();
        when(configuracionPedidoService.obtener()).thenReturn(config);
        Pedido pedido = pedido(LocalDate.now().minusDays(10),
            EstadoPagoHito.PAGADO, EstadoEntregaHito.NO_INICIADO);
        when(pedidoRepository.findCandidatosMora(
                eq(EstadoPedido.EN_CURSO),
                eq(config.getSancionesAutomaticasDesde())))
            .thenReturn(List.of(pedido));

        int registradas = moraService.detectar();

        LocalDateTime esperado = pedido.getLimiteEntrega()
            .atTime(23, 59, 59);
        assertThat(registradas).isEqualTo(1);
        assertThat(pedido.getMoraDesde()).isEqualTo(esperado);
        verify(pedidoRepository).save(pedido);
        verify(notificacionService).crearParaUsuario(
            eq(7L), eq("ENTREGA_ATRASADA"), any(), any(),
            eq(10L), eq("PEDIDO"));
    }

    @Test
    void detectar_plazoNoVencido_noRegistra() {
        when(configuracionPedidoService.obtener()).thenReturn(configuracion());
        Pedido pedido = pedido(LocalDate.now().plusDays(5),
            EstadoPagoHito.PAGADO, EstadoEntregaHito.NO_INICIADO);
        when(pedidoRepository.findCandidatosMora(any(), any()))
            .thenReturn(List.of(pedido));

        assertThat(moraService.detectar()).isZero();
        assertThat(pedido.getMoraDesde()).isNull();
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void detectar_cuotaImpagaNoGeneraMora() {
        when(configuracionPedidoService.obtener()).thenReturn(configuracion());
        Pedido pedido = pedido(LocalDate.now().minusDays(10),
            EstadoPagoHito.PENDIENTE, EstadoEntregaHito.NO_INICIADO);
        when(pedidoRepository.findCandidatosMora(any(), any()))
            .thenReturn(List.of(pedido));

        assertThat(moraService.detectar()).isZero();
        verify(pedidoRepository, never()).save(any());
        verify(notificacionService, never()).crearParaUsuario(
            anyLong(), any(), any(), any(), any(), any());
    }

    @Test
    void detectar_usaElCorteDeLaConfiguracion() {
        ConfiguracionPedido config = configuracion();
        when(configuracionPedidoService.obtener()).thenReturn(config);
        when(pedidoRepository.findCandidatosMora(any(), any()))
            .thenReturn(List.of());

        moraService.detectar();

        verify(pedidoRepository).findCandidatosMora(
            EstadoPedido.EN_CURSO, config.getSancionesAutomaticasDesde());
    }

    private ConfiguracionPedido configuracion() {
        ConfiguracionPedido config = new ConfiguracionPedido();
        config.setDiasGraciaEntrega(3);
        config.setSancionesAutomaticasDesde(
            LocalDateTime.now().minusDays(30));
        return config;
    }

    private Pedido pedido(
            LocalDate limite,
            EstadoPagoHito pago,
            EstadoEntregaHito entrega) {
        Usuario artista = Usuario.builder()
            .idUsuario(7L)
            .nombreUsuario("artista")
            .build();
        Servicio servicio = new Servicio();
        servicio.setIdServicio(3L);
        servicio.setNombre("Retrato");

        Pedido pedido = new Pedido();
        pedido.setIdPedido(10L);
        pedido.setEstado(EstadoPedido.EN_CURSO);
        pedido.setArtista(artista);
        pedido.setServicio(servicio);
        pedido.setLimiteEntrega(limite);

        HitoPresupuesto presupuesto = new HitoPresupuesto();
        presupuesto.setOrden(1);
        presupuesto.setTipo(TipoHito.CUOTA);
        presupuesto.setNombre("Etapa 1");
        presupuesto.setRequiereEntrega(true);

        HitoPedido hito = new HitoPedido();
        hito.setIdHitoPedido(20L);
        hito.setHitoPresupuesto(presupuesto);
        hito.setOrden(1);
        hito.setEstadoPago(pago);
        hito.setEstadoEntrega(entrega);
        pedido.agregarHito(hito);
        return pedido;
    }
}
