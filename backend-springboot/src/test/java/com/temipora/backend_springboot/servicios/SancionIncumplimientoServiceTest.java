package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.EstadoSancion;
import com.temipora.backend_springboot.modelo.OrigenSancion;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.SancionCuenta;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.PedidoRepository;
import com.temipora.backend_springboot.repositorios.SancionCuentaRepository;

@ExtendWith(MockitoExtension.class)
class SancionIncumplimientoServiceTest {

    @Mock
    private SancionCuentaRepository sancionRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private SancionAutomaticaService sancionAutomaticaService;

    @Mock
    private ConfiguracionPedidoService configuracionPedidoService;

    @Mock
    private EntregaPendienteService entregaPendienteService;

    @Mock
    private PedidoService pedidoService;

    @InjectMocks
    private SancionIncumplimientoService service;

    @Test
    void procesar_curaCuandoNoQuedaNingunPedidoEnMora() {
        SancionCuenta sancion = sancionEntregas(LocalDateTime.now().plusDays(5));
        when(configuracionPedidoService.obtener()).thenReturn(configuracion());
        when(sancionRepository.findPorOrigenYAlcance(
                OrigenSancion.AUTOMATICA, AlcanceSancion.ENTREGAS,
                EstadoSancion.ACTIVA))
            .thenReturn(List.of(sancion));
        when(pedidoRepository.findEnMoraDeArtista(
                eq(7L), eq(EstadoPedido.EN_CURSO)))
            .thenReturn(List.of());

        SancionIncumplimientoService.Resumen resumen = service.procesar();

        assertThat(resumen.curados()).isEqualTo(1);
        assertThat(resumen.escalados()).isZero();
        verify(sancionAutomaticaService).curar(sancion);
    }

    @Test
    void procesar_curaCuandoElHitoYaEstaAprobado() {
        SancionCuenta sancion = sancionEntregas(LocalDateTime.now().plusDays(5));
        Pedido pedido = pedido(LocalDateTime.now().minusDays(20));
        when(configuracionPedidoService.obtener()).thenReturn(configuracion());
        when(sancionRepository.findPorOrigenYAlcance(
                OrigenSancion.AUTOMATICA, AlcanceSancion.ENTREGAS,
                EstadoSancion.ACTIVA))
            .thenReturn(List.of(sancion));
        when(pedidoRepository.findEnMoraDeArtista(
                eq(7L), eq(EstadoPedido.EN_CURSO)))
            .thenReturn(List.of(pedido));
        when(entregaPendienteService.plazoVigente(pedido)).thenReturn(null);

        SancionIncumplimientoService.Resumen resumen = service.procesar();

        assertThat(resumen.curados()).isEqualTo(1);
        verify(sancionAutomaticaService).curar(sancion);
    }

    @Test
    void procesar_noCuraSiElHitoEstaEntregadoPeroSinAprobar() {
        SancionCuenta sancion = sancionEntregas(LocalDateTime.now().plusDays(5));
        Pedido pedido = pedido(LocalDateTime.now().minusDays(20));
        when(configuracionPedidoService.obtener()).thenReturn(configuracion());
        when(sancionRepository.findPorOrigenYAlcance(
                OrigenSancion.AUTOMATICA, AlcanceSancion.ENTREGAS,
                EstadoSancion.ACTIVA))
            .thenReturn(List.of(sancion));
        when(pedidoRepository.findEnMoraDeArtista(
                eq(7L), eq(EstadoPedido.EN_CURSO)))
            .thenReturn(List.of(pedido));
        when(entregaPendienteService.plazoVigente(pedido))
            .thenReturn(LocalDateTime.now().minusDays(5));

        SancionIncumplimientoService.Resumen resumen = service.procesar();

        assertThat(resumen.curados()).isZero();
        verify(sancionAutomaticaService, never()).curar(any());
    }

    @Test
    void procesar_escalaAlVencerLaSancionSiSigueEnMora() {
        SancionCuenta sancion = sancionEntregas(LocalDateTime.now().minusDays(1));
        Pedido pedido = pedido(LocalDateTime.now().minusDays(20));
        when(configuracionPedidoService.obtener()).thenReturn(configuracion());
        when(sancionRepository.findPorOrigenYAlcance(
                OrigenSancion.AUTOMATICA, AlcanceSancion.ENTREGAS,
                EstadoSancion.ACTIVA))
            .thenReturn(List.of(sancion));
        when(pedidoRepository.findEnMoraDeArtista(
                eq(7L), eq(EstadoPedido.EN_CURSO)))
            .thenReturn(List.of(pedido));
        when(entregaPendienteService.plazoVigente(pedido))
            .thenReturn(LocalDateTime.now().minusDays(20));

        SancionIncumplimientoService.Resumen resumen = service.procesar();

        assertThat(resumen.escalados()).isEqualTo(1);
        verify(sancionAutomaticaService).escalarNivel2(sancion, pedido);
    }

    @Test
    void procesar_aplicaNivel1PasadaLaGracia() {
        Pedido pedido = pedido(LocalDateTime.now().minusDays(5));
        when(configuracionPedidoService.obtener()).thenReturn(configuracion());
        when(pedidoRepository.findEnMora(EstadoPedido.EN_CURSO))
            .thenReturn(List.of(pedido));
        when(entregaPendienteService.plazoVigente(pedido))
            .thenReturn(LocalDateTime.now().minusDays(5));
        when(sancionRepository.findFirstByUsuarioIdUsuarioAndEstado(
                eq(7L), eq(EstadoSancion.ACTIVA)))
            .thenReturn(Optional.empty());

        SancionIncumplimientoService.Resumen resumen = service.procesar();

        assertThat(resumen.nivel1Aplicados()).isEqualTo(1);
        verify(sancionAutomaticaService).aplicarNivel1(pedido, 14);
    }

    @Test
    void procesar_noAplicaNivel1DentroDeLaGracia() {
        Pedido pedido = pedido(LocalDateTime.now().minusDays(1));
        when(configuracionPedidoService.obtener()).thenReturn(configuracion());
        when(pedidoRepository.findEnMora(EstadoPedido.EN_CURSO))
            .thenReturn(List.of(pedido));
        when(entregaPendienteService.plazoVigente(pedido))
            .thenReturn(LocalDateTime.now().minusDays(1));

        SancionIncumplimientoService.Resumen resumen = service.procesar();

        assertThat(resumen.nivel1Aplicados()).isZero();
        verify(sancionAutomaticaService, never())
            .aplicarNivel1(any(), anyInt());
    }

    @Test
    void procesar_noAplicaNivel1SiElHitoEstaAprobado() {
        Pedido pedido = pedido(LocalDateTime.now().minusDays(5));
        when(configuracionPedidoService.obtener()).thenReturn(configuracion());
        when(pedidoRepository.findEnMora(EstadoPedido.EN_CURSO))
            .thenReturn(List.of(pedido));
        when(entregaPendienteService.plazoVigente(pedido)).thenReturn(null);

        SancionIncumplimientoService.Resumen resumen = service.procesar();

        assertThat(resumen.nivel1Aplicados()).isZero();
        verify(sancionAutomaticaService, never())
            .aplicarNivel1(any(), anyInt());
    }

    @Test
    void procesar_noAplicaNivel1SiYaHayUnaSancionActiva() {
        Pedido pedido = pedido(LocalDateTime.now().minusDays(5));
        when(configuracionPedidoService.obtener()).thenReturn(configuracion());
        when(pedidoRepository.findEnMora(EstadoPedido.EN_CURSO))
            .thenReturn(List.of(pedido));
        when(entregaPendienteService.plazoVigente(pedido))
            .thenReturn(LocalDateTime.now().minusDays(5));
        when(sancionRepository.findFirstByUsuarioIdUsuarioAndEstado(
                eq(7L), eq(EstadoSancion.ACTIVA)))
            .thenReturn(Optional.of(sancionEntregas(
                LocalDateTime.now().plusDays(3))));

        SancionIncumplimientoService.Resumen resumen = service.procesar();

        assertThat(resumen.nivel1Aplicados()).isZero();
        verify(sancionAutomaticaService, never())
            .aplicarNivel1(any(), anyInt());
    }

    @Test
    void procesar_cierraLosPedidosAlVencerElPlazoDelNivel2() {
        SancionCuenta sancion = sancionAcceso(
            LocalDateTime.now().minusDays(10));
        Pedido pedido = pedido(LocalDateTime.now().minusDays(20));
        when(configuracionPedidoService.obtener()).thenReturn(configuracion());
        when(sancionRepository.findPorOrigenYAlcance(
                OrigenSancion.AUTOMATICA, AlcanceSancion.ENTREGAS,
                EstadoSancion.ACTIVA))
            .thenReturn(List.of());
        when(sancionRepository.findPorOrigenYAlcance(
                OrigenSancion.AUTOMATICA, AlcanceSancion.ACCESO,
                EstadoSancion.ACTIVA))
            .thenReturn(List.of(sancion));
        when(pedidoRepository.findEnMoraDeArtista(
                eq(7L), eq(EstadoPedido.EN_CURSO)))
            .thenReturn(List.of(pedido));
        when(entregaPendienteService.plazoVigente(pedido))
            .thenReturn(LocalDateTime.now().minusDays(20));
        when(pedidoService.cerrarPorIncumplimientoSistema(10L))
            .thenReturn(true);

        SancionIncumplimientoService.Resumen resumen = service.procesar();

        assertThat(resumen.pedidosCerrados()).isEqualTo(1);
        verify(pedidoService).cerrarPorIncumplimientoSistema(10L);
    }

    @Test
    void procesar_noCierraSiElHitoEstaAprobado() {
        SancionCuenta sancion = sancionAcceso(
            LocalDateTime.now().minusDays(10));
        Pedido pedido = pedido(LocalDateTime.now().minusDays(20));
        when(configuracionPedidoService.obtener()).thenReturn(configuracion());
        when(sancionRepository.findPorOrigenYAlcance(
                OrigenSancion.AUTOMATICA, AlcanceSancion.ENTREGAS,
                EstadoSancion.ACTIVA))
            .thenReturn(List.of());
        when(sancionRepository.findPorOrigenYAlcance(
                OrigenSancion.AUTOMATICA, AlcanceSancion.ACCESO,
                EstadoSancion.ACTIVA))
            .thenReturn(List.of(sancion));
        when(pedidoRepository.findEnMoraDeArtista(
                eq(7L), eq(EstadoPedido.EN_CURSO)))
            .thenReturn(List.of(pedido));
        when(entregaPendienteService.plazoVigente(pedido)).thenReturn(null);

        SancionIncumplimientoService.Resumen resumen = service.procesar();

        assertThat(resumen.pedidosCerrados()).isZero();
        verify(pedidoService, never())
            .cerrarPorIncumplimientoSistema(any());
    }

    @Test
    void procesar_noCierraAntesDelPlazoConfigurado() {
        SancionCuenta sancion = sancionAcceso(
            LocalDateTime.now().minusDays(2));
        when(configuracionPedidoService.obtener()).thenReturn(configuracion());
        when(sancionRepository.findPorOrigenYAlcance(
                OrigenSancion.AUTOMATICA, AlcanceSancion.ENTREGAS,
                EstadoSancion.ACTIVA))
            .thenReturn(List.of());
        when(sancionRepository.findPorOrigenYAlcance(
                OrigenSancion.AUTOMATICA, AlcanceSancion.ACCESO,
                EstadoSancion.ACTIVA))
            .thenReturn(List.of(sancion));

        SancionIncumplimientoService.Resumen resumen = service.procesar();

        assertThat(resumen.pedidosCerrados()).isZero();
        verify(pedidoService, never())
            .cerrarPorIncumplimientoSistema(any());
    }

    private ConfiguracionPedido configuracion() {
        ConfiguracionPedido config = new ConfiguracionPedido();
        config.setDiasGraciaEntrega(3);
        config.setDiasSancionVentas(14);
        config.setDiasCierreTrasBloqueo(7);
        return config;
    }

    private SancionCuenta sancionEntregas(LocalDateTime fechaFin) {
        return SancionCuenta.builder()
            .idSancion(9L)
            .usuario(Usuario.builder().idUsuario(7L).build())
            .origen(OrigenSancion.AUTOMATICA)
            .alcance(AlcanceSancion.ENTREGAS)
            .estado(EstadoSancion.ACTIVA)
            .fechaInicio(LocalDateTime.now().minusDays(20))
            .fechaFin(fechaFin)
            .build();
    }

    private SancionCuenta sancionAcceso(LocalDateTime fechaEscalada) {
        return SancionCuenta.builder()
            .idSancion(9L)
            .usuario(Usuario.builder().idUsuario(7L).build())
            .origen(OrigenSancion.AUTOMATICA)
            .alcance(AlcanceSancion.ACCESO)
            .estado(EstadoSancion.ACTIVA)
            .fechaInicio(LocalDateTime.now().minusDays(30))
            .fechaEscalada(fechaEscalada)
            .build();
    }

    private Pedido pedido(LocalDateTime moraDesde) {
        Pedido pedido = new Pedido();
        pedido.setIdPedido(10L);
        pedido.setEstado(EstadoPedido.EN_CURSO);
        pedido.setArtista(Usuario.builder().idUsuario(7L).build());
        pedido.setMoraDesde(moraDesde);
        return pedido;
    }
}
