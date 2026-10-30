package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.ConfiguracionPedido;
import com.temipora.backend_springboot.modelo.ETiempoEntregaUnidad;
import com.temipora.backend_springboot.modelo.EstadoEntregaHito;
import com.temipora.backend_springboot.modelo.EstadoPago;
import com.temipora.backend_springboot.modelo.EstadoPagoHito;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.EstadoSolicitud;
import com.temipora.backend_springboot.modelo.HitoPedido;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.Pago;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.Presupuesto;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.Solicitud;
import com.temipora.backend_springboot.modelo.TipoHito;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.HitoPedidoRepository;
import com.temipora.backend_springboot.repositorios.PagoRepository;
import com.temipora.backend_springboot.repositorios.PedidoRepository;
import com.temipora.backend_springboot.repositorios.SolicitudRepository;
import com.temipora.backend_springboot.servicios.pagos.ProveedorPago;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    private static final Long CLIENTE = 1L;
    private static final Long ARTISTA = 2L;
    private static final Long PEDIDO = 10L;
    private static final Long HITO = 20L;
    private static final Long PAGO = 30L;

    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private HitoPedidoRepository hitoPedidoRepository;
    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private SolicitudRepository solicitudRepository;
    @Mock
    private NotificacionService notificacionService;
    @Mock
    private HitoFlujoService hitoFlujoService;
    @Mock
    private PedidoCierreService pedidoCierreService;
    @Mock
    private ProveedorPago proveedorPago;

    @InjectMocks
    private PagoService pagoService;

    private Pedido pedido;
    private Solicitud solicitud;
    private Presupuesto presupuesto;
    private HitoPedido hito;
    private Pago pago;

    @BeforeEach
    void setUp() {
        Usuario cliente = new Usuario();
        cliente.setIdUsuario(CLIENTE);
        Usuario artista = new Usuario();
        artista.setIdUsuario(ARTISTA);

        Servicio servicio = new Servicio();
        servicio.setIdServicio(5L);
        servicio.setNombre("Ilustración");

        Moneda ars = Moneda.builder()
            .idMoneda(3L)
            .codigo("ARS")
            .nombre("Peso argentino")
            .build();

        presupuesto = new Presupuesto();
        presupuesto.setIdPresupuesto(7L);
        presupuesto.setTiempoEntregaValor(2);
        presupuesto.setTiempoEntregaUnidad(ETiempoEntregaUnidad.SEMANAS);
        presupuesto.setTiempoEntregaDias(14);

        solicitud = new Solicitud();
        solicitud.setIdSolicitud(9L);
        solicitud.setCliente(cliente);
        solicitud.setArtista(artista);
        solicitud.setServicio(servicio);
        solicitud.setPresupuesto(presupuesto);
        solicitud.setEstado(EstadoSolicitud.PRESUPUESTADA);

        pedido = new Pedido();
        pedido.setIdPedido(PEDIDO);
        pedido.setSolicitud(solicitud);
        pedido.setPresupuesto(presupuesto);
        pedido.setCliente(cliente);
        pedido.setArtista(artista);
        pedido.setServicio(servicio);
        pedido.setEstado(EstadoPedido.PENDIENTE_PAGO);
        pedido.setPrecioFinal(new BigDecimal("100.00"));
        pedido.setPrecioFinalCobro(new BigDecimal("100.00"));
        pedido.setMoneda(ars);
        pedido.setMonedaCobro(ars);
        pedido.setCotizacionCobro(BigDecimal.ONE);
        pedido.setComisionPorcentaje(new BigDecimal("10.00"));
        pedido.setTerminosPlataformaVersion(1);
        pedido.setTerminosPlataformaSnapshot("TOS");

        hito = new HitoPedido();
        hito.setIdHitoPedido(HITO);
        hito.setOrden(0);
        hito.setNombre("Seña");
        hito.setTipo(TipoHito.SENA);
        hito.setRequiereEntrega(false);
        hito.setEstadoPago(EstadoPagoHito.PENDIENTE);
        hito.setMonto(new BigDecimal("50.00"));
        hito.setMontoCobro(new BigDecimal("50.00"));
        hito.setComisionMonto(new BigDecimal("5.00"));
        hito.setComisionMontoCobro(new BigDecimal("5.00"));
        hito.setMontoNeto(new BigDecimal("45.00"));
        hito.setMontoNetoCobro(new BigDecimal("45.00"));
        pedido.agregarHito(hito);

        pago = new Pago();
        pago.setIdPago(PAGO);
        pago.setHitoPedido(hito);
        pago.setUsuarioPagador(cliente);
        pago.setMonto(new BigDecimal("50.00"));
        pago.setMontoCobro(new BigDecimal("50.00"));
        pago.setMoneda(ars);
        pago.setMonedaCobro(ars);
        pago.setCotizacionCobro(BigDecimal.ONE);
        pago.setComisionMontoCobro(new BigDecimal("5.00"));
        pago.setMontoNetoCobro(new BigDecimal("45.00"));
        pago.setEstado(EstadoPago.PENDIENTE);
        pago.setProveedor("SIMULADO");
    }

    @Test
    void confirmarSenaSinEntregable_iniciaPedidoYAprobaElHito() {
        when(pagoRepository.findByIdPagoForUpdate(PAGO))
            .thenReturn(Optional.of(pago));
        when(hitoPedidoRepository.findByIdHitoPedidoForUpdate(HITO))
            .thenReturn(Optional.of(hito));
        when(pedidoRepository.findByIdPedidoForUpdate(PEDIDO))
            .thenReturn(Optional.of(pedido));

        Pedido resultado = pagoService.confirmar(PAGO, CLIENTE, null);

        assertThat(resultado.getEstado()).isEqualTo(EstadoPedido.EN_CURSO);
        assertThat(resultado.getFechaInicio()).isNotNull();
        assertThat(resultado.getLimiteEntrega()).isEqualTo(
            resultado.getFechaInicio().plusWeeks(2));
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.PAGADA);
        assertThat(pago.getEstado()).isEqualTo(EstadoPago.APROBADO);
        assertThat(hito.getEstadoPago()).isEqualTo(EstadoPagoHito.PAGADO);
        assertThat(hito.getEstadoEntrega())
            .isEqualTo(EstadoEntregaHito.APROBADO);
        verify(hitoFlujoService).habilitarSiguiente(
            eq(pedido), eq(hito), any(LocalDateTime.class));
        verify(solicitudRepository).save(solicitud);
        verify(pedidoCierreService).finalizar(
            eq(pedido), any(LocalDateTime.class));
    }

    @Test
    void confirmarEntregaFinal_delegaElCierreDelPedido() {
        hito.setTipo(TipoHito.ENTREGA);
        hito.setRequiereEntrega(true);
        hito.setEstadoPago(EstadoPagoHito.PENDIENTE);
        hito.setEstadoEntrega(EstadoEntregaHito.ENTREGADO);
        pedido.setEstado(EstadoPedido.EN_CURSO);
        solicitud.setEstado(EstadoSolicitud.PAGADA);

        when(pagoRepository.findByIdPagoForUpdate(PAGO))
            .thenReturn(Optional.of(pago));
        when(hitoPedidoRepository.findByIdHitoPedidoForUpdate(HITO))
            .thenReturn(Optional.of(hito));
        when(pedidoRepository.findByIdPedidoForUpdate(PEDIDO))
            .thenReturn(Optional.of(pedido));

        Pedido resultado = pagoService.confirmar(PAGO, CLIENTE, null);

        assertThat(resultado).isSameAs(pedido);
        assertThat(hito.getEstadoPago()).isEqualTo(EstadoPagoHito.PAGADO);
        assertThat(hito.getEstadoEntrega())
            .isEqualTo(EstadoEntregaHito.APROBADO);
        verify(pedidoCierreService).finalizar(
            eq(pedido), any(LocalDateTime.class));
    }

    @Test
    void confirmarEntregaSinEntregar_lanzaConflicto() {
        hito.setTipo(TipoHito.ENTREGA);
        hito.setRequiereEntrega(true);
        hito.setEstadoPago(EstadoPagoHito.PENDIENTE);
        hito.setEstadoEntrega(EstadoEntregaHito.NO_INICIADO);
        pedido.setEstado(EstadoPedido.EN_CURSO);

        when(pagoRepository.findByIdPagoForUpdate(PAGO))
            .thenReturn(Optional.of(pago));
        when(hitoPedidoRepository.findByIdHitoPedidoForUpdate(HITO))
            .thenReturn(Optional.of(hito));
        when(pedidoRepository.findByIdPedidoForUpdate(PEDIDO))
            .thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pagoService.confirmar(PAGO, CLIENTE, null))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("entrega final");
    }

    @Test
    void confirmarPagoAprobado_esIdempotente() {
        pago.setEstado(EstadoPago.APROBADO);
        when(pagoRepository.findByIdPagoForUpdate(PAGO))
            .thenReturn(Optional.of(pago));
        when(pedidoRepository.findById(PEDIDO))
            .thenReturn(Optional.of(pedido));

        Pedido resultado = pagoService.confirmar(PAGO, CLIENTE, null);

        assertThat(resultado).isSameAs(pedido);
        assertThat(pago.getEstado()).isEqualTo(EstadoPago.APROBADO);
        verify(hitoPedidoRepository, org.mockito.Mockito.never())
            .findByIdHitoPedidoForUpdate(any());
    }

    @Test
    void aplicarEstadoProveedor_aprobado_activaElPedido() {
        when(pagoRepository.findByIdPagoForUpdate(PAGO))
            .thenReturn(Optional.of(pago));
        when(hitoPedidoRepository.findByIdHitoPedidoForUpdate(HITO))
            .thenReturn(Optional.of(hito));
        when(pedidoRepository.findByIdPedidoForUpdate(PEDIDO))
            .thenReturn(Optional.of(pedido));

        pagoService.aplicarEstadoProveedor(
            PAGO, EstadoPago.APROBADO, "mp-1", "approved", "accredited");

        assertThat(pago.getEstado()).isEqualTo(EstadoPago.APROBADO);
        assertThat(pago.getIdPagoExterno()).isEqualTo("mp-1");
        assertThat(pago.getEstadoExterno()).isEqualTo("approved / accredited");
        assertThat(hito.getEstadoPago()).isEqualTo(EstadoPagoHito.PAGADO);
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.EN_CURSO);
    }

    @Test
    void aplicarEstadoProveedor_rechazado_marcaPagoYNotifica() {
        when(pagoRepository.findByIdPagoForUpdate(PAGO))
            .thenReturn(Optional.of(pago));
        when(pedidoRepository.findById(PEDIDO))
            .thenReturn(Optional.of(pedido));

        pagoService.aplicarEstadoProveedor(
            PAGO, EstadoPago.RECHAZADO, "mp-1", "rejected", "cc_rejected");

        assertThat(pago.getEstado()).isEqualTo(EstadoPago.RECHAZADO);
        assertThat(pago.getEstadoExterno())
            .isEqualTo("rejected / cc_rejected");
        verify(notificacionService).crearParaUsuario(
            eq(CLIENTE), eq("PAGO_RECHAZADO"), eq("Pago rechazado"),
            any(String.class), eq(PEDIDO), eq("PEDIDO"));
    }

    @Test
    void aplicarEstadoProveedor_aprobadoDuplicado_esIdempotente() {
        pago.setEstado(EstadoPago.APROBADO);
        when(pagoRepository.findByIdPagoForUpdate(PAGO))
            .thenReturn(Optional.of(pago));

        pagoService.aplicarEstadoProveedor(
            PAGO, EstadoPago.APROBADO, "mp-1", "approved", "accredited");

        assertThat(pago.getEstado()).isEqualTo(EstadoPago.APROBADO);
        verify(hitoPedidoRepository, org.mockito.Mockito.never())
            .findByIdHitoPedidoForUpdate(any());
    }
}
