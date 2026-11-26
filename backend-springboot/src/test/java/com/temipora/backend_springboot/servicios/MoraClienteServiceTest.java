package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.temipora.backend_springboot.modelo.AlcanceSancion;
import com.temipora.backend_springboot.modelo.ConfiguracionPedido;
import com.temipora.backend_springboot.modelo.EstadoApelacion;
import com.temipora.backend_springboot.modelo.EstadoEntregaHito;
import com.temipora.backend_springboot.modelo.EstadoPagoHito;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.EstadoSancion;
import com.temipora.backend_springboot.modelo.HitoPedido;
import com.temipora.backend_springboot.modelo.HitoPresupuesto;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.Presupuesto;
import com.temipora.backend_springboot.modelo.SancionCuenta;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.TipoHito;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.ApelacionSancionRepository;
import com.temipora.backend_springboot.repositorios.PedidoRepository;
import com.temipora.backend_springboot.repositorios.SancionCuentaRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MoraClienteServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ConfiguracionPedidoService configuracionPedidoService;

    @Mock
    private NotificacionService notificacionService;

    @Mock
    private AuditoriaService auditoria;

    @Mock
    private SancionService sancionService;

    @Mock
    private SancionCuentaRepository sancionRepository;

    @Mock
    private ApelacionSancionRepository apelacionRepository;

    @InjectMocks
    private MoraClienteService moraClienteService;

    @Test
    void detectar_registraElImpagoConElPlazoYNotifica() {
        ConfiguracionPedido config = configuracion();
        when(configuracionPedidoService.obtener()).thenReturn(config);
        LocalDateTime plazo = LocalDateTime.now().minusDays(2);
        Pedido pedido = pedido(TipoHito.ENTREGA,
            EstadoPagoHito.PENDIENTE, EstadoEntregaHito.ENTREGADO, plazo);
        when(pedidoRepository.findCandidatosImpagoCliente(
                eq(EstadoPedido.EN_CURSO),
                eq(config.getSancionesAutomaticasDesde()),
                eq(TipoHito.ENTREGA),
                eq(EstadoPagoHito.PENDIENTE),
                eq(EstadoEntregaHito.ENTREGADO),
                any()))
            .thenReturn(List.of(pedido));

        int registrados = moraClienteService.detectar();

        assertThat(registrados).isEqualTo(1);
        assertThat(pedido.getImpagoDesde()).isEqualTo(plazo);
        verify(pedidoRepository).save(pedido);
        verify(notificacionService).crearParaUsuario(
            eq(7L), eq("PAGO_ATRASADO"), any(), any(),
            eq(10L), eq("PEDIDO"));
        verify(auditoria).registrarSistema(
            eq(AccionesAdmin.PEDIDO_IMPAGO_DETECTAR), eq("Pedido"),
            eq(10L), any());
    }

    @Test
    void detectar_entregaNoEntregada_noRegistra() {
        when(configuracionPedidoService.obtener()).thenReturn(configuracion());
        Pedido pedido = pedido(TipoHito.ENTREGA,
            EstadoPagoHito.PENDIENTE, EstadoEntregaHito.NO_INICIADO,
            LocalDateTime.now().minusDays(2));
        when(pedidoRepository.findCandidatosImpagoCliente(
                any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of(pedido));

        assertThat(moraClienteService.detectar()).isZero();
        assertThat(pedido.getImpagoDesde()).isNull();
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void detectar_entregaPaga_noRegistra() {
        when(configuracionPedidoService.obtener()).thenReturn(configuracion());
        Pedido pedido = pedido(TipoHito.ENTREGA,
            EstadoPagoHito.PAGADO, EstadoEntregaHito.ENTREGADO,
            LocalDateTime.now().minusDays(2));
        when(pedidoRepository.findCandidatosImpagoCliente(
                any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of(pedido));

        assertThat(moraClienteService.detectar()).isZero();
        verify(notificacionService, never()).crearParaUsuario(
            anyLong(), any(), any(), any(), any(), any());
    }

    @Test
    void detectar_cuotaImpaga_noRegistra() {
        when(configuracionPedidoService.obtener()).thenReturn(configuracion());
        Pedido pedido = pedido(TipoHito.CUOTA,
            EstadoPagoHito.PENDIENTE, EstadoEntregaHito.ENTREGADO,
            LocalDateTime.now().minusDays(2));
        when(pedidoRepository.findCandidatosImpagoCliente(
                any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of(pedido));

        assertThat(moraClienteService.detectar()).isZero();
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void detectar_plazoNoVencido_noRegistra() {
        when(configuracionPedidoService.obtener()).thenReturn(configuracion());
        Pedido pedido = pedido(TipoHito.ENTREGA,
            EstadoPagoHito.PENDIENTE, EstadoEntregaHito.ENTREGADO,
            LocalDateTime.now().plusDays(2));
        when(pedidoRepository.findCandidatosImpagoCliente(
                any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of(pedido));

        assertThat(moraClienteService.detectar()).isZero();
        assertThat(pedido.getImpagoDesde()).isNull();
    }

    @Test
    void puedeComprar_falseConSancionDeCompras() {
        when(sancionService.tieneSancionDeCompra(7L)).thenReturn(true);

        assertThat(moraClienteService.puedeComprar(7L)).isFalse();
    }

    @Test
    void puedeComprar_trueSinSancion() {
        when(sancionService.tieneSancionDeCompra(7L)).thenReturn(false);

        assertThat(moraClienteService.puedeComprar(7L)).isTrue();
    }

    @Test
    void chatPermitido_permiteSoloAlArtistaDeudor() {
        Pedido pedido = pedido(TipoHito.ENTREGA,
            EstadoPagoHito.PENDIENTE, EstadoEntregaHito.ENTREGADO,
            LocalDateTime.now().minusDays(2));
        when(sancionService.tieneSancionVigente(
                7L, com.temipora.backend_springboot.modelo.AlcanceSancion.COMPRAS))
            .thenReturn(true);
        when(pedidoRepository.findConImpagoDeCliente(
                7L, EstadoPedido.EN_CURSO))
            .thenReturn(List.of(pedido));

        assertThat(moraClienteService.chatPermitido(7L, 8L)).isTrue();
        assertThat(moraClienteService.chatPermitido(7L, 9L)).isFalse();
    }

    @Test
    void chatPermitido_conAlcancePagos_bloqueaTodo() {
        when(sancionService.tieneSancionVigente(
                7L, com.temipora.backend_springboot.modelo.AlcanceSancion.PAGOS))
            .thenReturn(true);

        assertThat(moraClienteService.chatPermitido(7L, 8L)).isFalse();
    }

    @Test
    void chatPermitido_sinSancion_permiteCualquiera() {
        assertThat(moraClienteService.chatPermitido(7L, 9L)).isTrue();
    }

    @Test
    void deudas_mapeaLaEntregaImpaga() {
        Pedido pedido = pedido(TipoHito.ENTREGA,
            EstadoPagoHito.PENDIENTE, EstadoEntregaHito.ENTREGADO,
            LocalDateTime.now().minusDays(4));
        pedido.setImpagoDesde(LocalDateTime.now().minusDays(4));
        when(pedidoRepository.findConImpagoDeCliente(7L, EstadoPedido.EN_CURSO))
            .thenReturn(List.of(pedido));

        var deudas = moraClienteService.deudas(7L);

        assertThat(deudas).hasSize(1);
        var deuda = deudas.get(0);
        assertThat(deuda.getIdPedido()).isEqualTo(10L);
        assertThat(deuda.getIdHitoPedido()).isEqualTo(20L);
        assertThat(deuda.getIdArtista()).isEqualTo(8L);
        assertThat(deuda.getMontoCobro()).isEqualByComparingTo("10.00");
        assertThat(deuda.getMonedaCobro()).isEqualTo("ARS");
        assertThat(deuda.isPuedePagar()).isTrue();
        assertThat(deuda.getDiasAtraso()).isEqualTo(4);
    }

    @Test
    void deudas_sinPedidos_devuelveVacio() {
        when(pedidoRepository.findConImpagoDeCliente(7L, EstadoPedido.EN_CURSO))
            .thenReturn(List.of());

        assertThat(moraClienteService.deudas(7L)).isEmpty();
    }

    @Test
    void estadoCuenta_informaSancionYTotalAdeudado() {
        Pedido pedido = pedido(TipoHito.ENTREGA,
            EstadoPagoHito.PENDIENTE, EstadoEntregaHito.ENTREGADO,
            LocalDateTime.now().minusDays(4));
        pedido.setImpagoDesde(LocalDateTime.now().minusDays(4));
        SancionCuenta sancion = new SancionCuenta();
        sancion.setIdSancion(99L);
        sancion.setUsuario(pedido.getCliente());
        sancion.setAlcance(AlcanceSancion.COMPRAS);
        sancion.setMotivo("No pagó la entrega final.");
        sancion.setFechaFin(LocalDateTime.now().plusDays(10));
        when(pedidoRepository.findConImpagoDeCliente(7L, EstadoPedido.EN_CURSO))
            .thenReturn(List.of(pedido));
        when(sancionRepository.findVigentesByUsuarioId(
                eq(7L), eq(EstadoSancion.ACTIVA), any()))
            .thenReturn(List.of(sancion));
        when(apelacionRepository.findFirstBySancionIdSancionAndEstado(
                eq(99L), eq(EstadoApelacion.PENDIENTE)))
            .thenReturn(Optional.empty());

        var estado = moraClienteService.estadoCuenta(7L);

        assertThat(estado.isSancionado()).isTrue();
        assertThat(estado.getAlcance()).isEqualTo("COMPRAS");
        assertThat(estado.getDeudas()).hasSize(1);
        assertThat(estado.getTotalAdeudado()).isEqualByComparingTo("10.00");
        assertThat(estado.getDiasRestantes()).isEqualTo(10);
        assertThat(estado.isApelacionPendiente()).isFalse();
    }

    @Test
    void estadoCuenta_sinSancionNiDeudas() {
        when(pedidoRepository.findConImpagoDeCliente(7L, EstadoPedido.EN_CURSO))
            .thenReturn(List.of());
        when(sancionRepository.findVigentesByUsuarioId(
                eq(7L), eq(EstadoSancion.ACTIVA), any()))
            .thenReturn(List.of());

        var estado = moraClienteService.estadoCuenta(7L);

        assertThat(estado.isSancionado()).isFalse();
        assertThat(estado.getDeudas()).isEmpty();
        assertThat(estado.getTotalAdeudado()).isEqualByComparingTo("0");
    }

    private ConfiguracionPedido configuracion() {
        ConfiguracionPedido config = new ConfiguracionPedido();
        config.setDiasGraciaPago(3);
        config.setSancionesAutomaticasDesde(
            LocalDateTime.now().minusDays(30));
        return config;
    }

    private Pedido pedido(
            TipoHito tipo,
            EstadoPagoHito pago,
            EstadoEntregaHito entrega,
            LocalDateTime fechaLimitePago) {
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

        Moneda ars = new Moneda();
        ars.setCodigo("ARS");
        Presupuesto presupuestoPedido = new Presupuesto();
        presupuestoPedido.setMoneda(ars);
        presupuestoPedido.setMonedaCobro(ars);
        pedido.setPresupuesto(presupuestoPedido);

        HitoPresupuesto presupuesto = new HitoPresupuesto();
        presupuesto.setOrden(1);
        presupuesto.setTipo(tipo);
        presupuesto.setNombre("Entrega final");
        presupuesto.setRequiereEntrega(true);
        presupuesto.setMonto(new BigDecimal("10.00"));
        presupuesto.setMontoCobro(new BigDecimal("10.00"));

        HitoPedido hito = new HitoPedido();
        hito.setIdHitoPedido(20L);
        hito.setHitoPresupuesto(presupuesto);
        hito.setOrden(1);
        hito.setEstadoPago(pago);
        hito.setEstadoEntrega(entrega);
        hito.setFechaLimitePago(fechaLimitePago);
        pedido.agregarHito(hito);
        return pedido;
    }
}
