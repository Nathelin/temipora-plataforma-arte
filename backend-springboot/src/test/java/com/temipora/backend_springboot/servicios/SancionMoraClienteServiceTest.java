package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.AlcanceSancion;
import com.temipora.backend_springboot.modelo.ConfiguracionPedido;
import com.temipora.backend_springboot.modelo.EstadoEntregaHito;
import com.temipora.backend_springboot.modelo.EstadoPagoHito;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.EstadoSancion;
import com.temipora.backend_springboot.modelo.HitoPedido;
import com.temipora.backend_springboot.modelo.HitoPresupuesto;
import com.temipora.backend_springboot.modelo.OrigenSancion;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.SancionCuenta;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.TipoHito;
import com.temipora.backend_springboot.modelo.TipoSancion;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.PedidoRepository;
import com.temipora.backend_springboot.repositorios.SancionCuentaRepository;

@ExtendWith(MockitoExtension.class)
class SancionMoraClienteServiceTest {

    @Mock
    private SancionCuentaRepository sancionRepository;

    @Mock
    private SancionService sancionService;

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ConfiguracionPedidoService configuracionPedidoService;

    @Mock
    private MoraClienteService moraClienteService;

    @Mock
    private NotificacionService notificacionService;

    @Mock
    private AuditoriaService auditoria;

    @InjectMocks
    private SancionMoraClienteService service;

    @Test
    void procesar_aplicaNivel1PasadaLaGraciaYNotifica() {
        ConfiguracionPedido config = configuracion(3, 27);
        when(configuracionPedidoService.obtener()).thenReturn(config);
        Pedido pedido = pedido(LocalDateTime.now().minusDays(5));
        when(pedidoRepository.findConImpago(EstadoPedido.EN_CURSO))
            .thenReturn(List.of(pedido));
        when(sancionRepository.findFirstByUsuarioIdUsuarioAndEstado(
                eq(7L), eq(EstadoSancion.ACTIVA)))
            .thenReturn(Optional.empty());
        SancionCuenta aplicada = sancion(
            OrigenSancion.AUTOMATICA, AlcanceSancion.COMPRAS,
            LocalDateTime.now().plusDays(27), pedido);
        when(sancionService.aplicarSistema(
                eq(7L), eq(TipoSancion.SUSPENSION), eq(AlcanceSancion.COMPRAS),
                anyString(), any(), eq("AUTO:PAGO:10:NIVEL1"),
                eq(pedido), any(), eq(pedido.getImpagoDesde())))
            .thenReturn(aplicada);

        SancionMoraClienteService.Resumen resumen = service.procesar();

        assertThat(resumen.nivel1Aplicados()).isEqualTo(1);
        verify(notificacionService).crearParaUsuario(
            eq(7L), eq("CUENTA_SUSPENDIDA_COMPRAS"), any(), any(),
            eq(10L), eq("PEDIDO"));
    }

    @Test
    void procesar_dentroDeLaGracia_noAplicaNivel1() {
        when(configuracionPedidoService.obtener()).thenReturn(configuracion(3, 27));
        Pedido pedido = pedido(LocalDateTime.now().minusDays(1));
        when(pedidoRepository.findConImpago(EstadoPedido.EN_CURSO))
            .thenReturn(List.of(pedido));

        SancionMoraClienteService.Resumen resumen = service.procesar();

        assertThat(resumen.nivel1Aplicados()).isZero();
        verify(sancionService, never()).aplicarSistema(
            any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void procesar_conSancionManualActiva_noAplicaNivel1() {
        when(configuracionPedidoService.obtener()).thenReturn(configuracion(3, 27));
        Pedido pedido = pedido(LocalDateTime.now().minusDays(5));
        when(pedidoRepository.findConImpago(EstadoPedido.EN_CURSO))
            .thenReturn(List.of(pedido));
        when(sancionRepository.findFirstByUsuarioIdUsuarioAndEstado(
                eq(7L), eq(EstadoSancion.ACTIVA)))
            .thenReturn(Optional.of(sancion(
                OrigenSancion.MANUAL, AlcanceSancion.ACCESO, null, pedido)));

        SancionMoraClienteService.Resumen resumen = service.procesar();

        assertThat(resumen.nivel1Aplicados()).isZero();
        verify(sancionService, never()).aplicarSistema(
            any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void procesar_escalaNivel2AlVencerLaSuspension() {
        when(configuracionPedidoService.obtener()).thenReturn(configuracion(3, 27));
        Pedido pedido = pedido(LocalDateTime.now().minusDays(30));
        SancionCuenta compras = sancion(
            OrigenSancion.AUTOMATICA, AlcanceSancion.COMPRAS,
            LocalDateTime.now().minusDays(1), pedido);
        when(sancionRepository.findPorOrigenYAlcance(
                OrigenSancion.AUTOMATICA, AlcanceSancion.COMPRAS,
                EstadoSancion.ACTIVA))
            .thenReturn(List.of(compras));
        when(pedidoRepository.findConImpagoDeCliente(7L, EstadoPedido.EN_CURSO))
            .thenReturn(List.of(pedido));
        SancionCuenta escalada = sancion(
            OrigenSancion.AUTOMATICA, AlcanceSancion.PAGOS, null, pedido);
        when(sancionService.escalarSistema(
                eq(99L), eq(AlcanceSancion.PAGOS), anyString(),
                eq("AUTO:PAGO:10:NIVEL2"), eq(pedido), any(), any()))
            .thenReturn(escalada);

        SancionMoraClienteService.Resumen resumen = service.procesar();

        assertThat(resumen.escalados()).isEqualTo(1);
        verify(notificacionService).crearParaUsuario(
            eq(7L), eq("CUENTA_RESTRINGIDA_PAGOS"), any(), any(),
            eq(10L), eq("PEDIDO"));
    }

    @Test
    void procesar_curaLaSancionCuandoElClientePaga() {
        when(configuracionPedidoService.obtener()).thenReturn(configuracion(3, 27));
        Pedido pedido = pedido(LocalDateTime.now().minusDays(5));
        SancionCuenta compras = sancion(
            OrigenSancion.AUTOMATICA, AlcanceSancion.COMPRAS,
            LocalDateTime.now().plusDays(20), pedido);
        when(sancionRepository.findPorOrigenYAlcance(
                OrigenSancion.AUTOMATICA, AlcanceSancion.COMPRAS,
                EstadoSancion.ACTIVA))
            .thenReturn(List.of(compras));
        when(pedidoRepository.findConImpagoDeCliente(7L, EstadoPedido.EN_CURSO))
            .thenReturn(List.of());
        when(sancionService.revertirSistema(eq(99L), anyString()))
            .thenReturn(compras);

        SancionMoraClienteService.Resumen resumen = service.procesar();

        assertThat(resumen.curados()).isEqualTo(1);
        verify(sancionService).revertirSistema(eq(99L), anyString());
        verify(auditoria).registrarSistema(
            eq(AccionesAdmin.PEDIDO_IMPAGO_CURAR), eq("Pedido"),
            eq(10L), any());
    }

    @Test
    void curarSiCorresponde_revierteSiYaNoDebe() {
        SancionCuenta compras = sancion(
            OrigenSancion.AUTOMATICA, AlcanceSancion.PAGOS, null,
            pedido(LocalDateTime.now().minusDays(40)));
        when(moraClienteService.tieneImpago(7L)).thenReturn(false);
        when(sancionRepository.findFirstByUsuarioIdUsuarioAndEstado(
                7L, EstadoSancion.ACTIVA))
            .thenReturn(Optional.of(compras));
        when(sancionService.revertirSistema(eq(99L), anyString()))
            .thenReturn(compras);

        assertThat(service.curarSiCorresponde(7L)).isTrue();
        verify(sancionService).revertirSistema(eq(99L), anyString());
    }

    @Test
    void curarSiCorresponde_noTocaSiSigueDebiendo() {
        when(moraClienteService.tieneImpago(7L)).thenReturn(true);

        assertThat(service.curarSiCorresponde(7L)).isFalse();
        verify(sancionService, never()).revertirSistema(any(), anyString());
    }

    @Test
    void curarSiCorresponde_noTocaUnaSancionManual() {
        SancionCuenta manual = sancion(
            OrigenSancion.MANUAL, AlcanceSancion.ACCESO, null,
            pedido(LocalDateTime.now().minusDays(40)));
        when(moraClienteService.tieneImpago(7L)).thenReturn(false);
        when(sancionRepository.findFirstByUsuarioIdUsuarioAndEstado(
                7L, EstadoSancion.ACTIVA))
            .thenReturn(Optional.of(manual));

        assertThat(service.curarSiCorresponde(7L)).isFalse();
        verify(sancionService, never()).revertirSistema(any(), anyString());
    }

    private ConfiguracionPedido configuracion(int gracia, int diasCompras) {
        ConfiguracionPedido config = new ConfiguracionPedido();
        config.setDiasGraciaPago(gracia);
        config.setDiasSancionCompras(diasCompras);
        return config;
    }

    private Pedido pedido(LocalDateTime impagoDesde) {
        Usuario cliente = Usuario.builder()
            .idUsuario(7L)
            .nombreUsuario("cliente")
            .build();
        Usuario artista = Usuario.builder()
            .idUsuario(8L)
            .nombreUsuario("artista")
            .build();
        Servicio servicio = new Servicio();
        servicio.setIdServicio(3L);
        servicio.setNombre("Retrato");

        Pedido pedido = new Pedido();
        pedido.setIdPedido(10L);
        pedido.setEstado(EstadoPedido.EN_CURSO);
        pedido.setCliente(cliente);
        pedido.setArtista(artista);
        pedido.setServicio(servicio);
        pedido.setImpagoDesde(impagoDesde);

        HitoPresupuesto presupuesto = new HitoPresupuesto();
        presupuesto.setOrden(1);
        presupuesto.setTipo(TipoHito.ENTREGA);
        presupuesto.setNombre("Entrega final");
        presupuesto.setRequiereEntrega(true);

        HitoPedido hito = new HitoPedido();
        hito.setIdHitoPedido(20L);
        hito.setHitoPresupuesto(presupuesto);
        hito.setOrden(1);
        hito.setEstadoPago(EstadoPagoHito.PENDIENTE);
        hito.setEstadoEntrega(EstadoEntregaHito.ENTREGADO);
        pedido.agregarHito(hito);
        return pedido;
    }

    private SancionCuenta sancion(
            OrigenSancion origen,
            AlcanceSancion alcance,
            LocalDateTime fechaFin,
            Pedido pedido) {
        SancionCuenta sancion = new SancionCuenta();
        sancion.setIdSancion(99L);
        sancion.setUsuario(pedido.getCliente());
        sancion.setOrigen(origen);
        sancion.setAlcance(alcance);
        sancion.setEstado(EstadoSancion.ACTIVA);
        sancion.setFechaInicio(LocalDateTime.now().minusDays(40));
        sancion.setFechaFin(fechaFin);
        sancion.setPedido(pedido);
        return sancion;
    }
}
