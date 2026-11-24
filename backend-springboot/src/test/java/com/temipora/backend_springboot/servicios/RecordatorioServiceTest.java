package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.EmailRecordatorioPendiente;
import com.temipora.backend_springboot.modelo.EstadoEntregaHito;
import com.temipora.backend_springboot.modelo.EstadoPagoHito;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.HitoPedido;
import com.temipora.backend_springboot.modelo.HitoPresupuesto;
import com.temipora.backend_springboot.modelo.MotivoRecordatorio;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.SancionCuenta;
import com.temipora.backend_springboot.modelo.TipoHito;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.EmailRecordatorioPendienteRepository;
import com.temipora.backend_springboot.repositorios.HitoPedidoRepository;
import com.temipora.backend_springboot.repositorios.PedidoRepository;

@ExtendWith(MockitoExtension.class)
class RecordatorioServiceTest {

    private static final Long PEDIDO = 10L;

    @Mock
    private HitoPedidoRepository hitoPedidoRepository;
    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private EmailRecordatorioPendienteRepository recordatorioRepository;
    @Mock
    private SancionService sancionService;
    @Mock
    private RecordatorioProcesador procesador;

    @org.mockito.Spy
    private EntregaPendienteService entregaPendienteService =
        new EntregaPendienteService();

    @InjectMocks
    private RecordatorioService service;

    @BeforeEach
    void setUp() {
        lenient().when(sancionService.obtenerVigente(anyLong()))
            .thenReturn(Optional.empty());
        lenient().when(recordatorioRepository.existsByClave(anyString()))
            .thenReturn(false);
        lenient().when(recordatorioRepository.save(any()))
            .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void encolar_senaDespuesDe24h_encolaPagoSena() {
        Pedido pedido = pedido(EstadoPedido.PENDIENTE_PAGO, null, null);
        pedido.setFechaCreacion(LocalDateTime.now().minusHours(25));
        HitoPedido hito = hito(pedido, TipoHito.SENA,
            EstadoPagoHito.PENDIENTE, EstadoEntregaHito.NO_INICIADO);
        hito.setFechaLimitePago(LocalDateTime.now().plusDays(5));
        when(hitoPedidoRepository.findSenasPendientes(
            any(), any(), any(), any())).thenReturn(List.of(hito));

        int total = service.encolar();

        assertThat(total).isEqualTo(1);
        EmailRecordatorioPendiente fila = capturarFila();
        assertThat(fila.getMotivo()).isEqualTo(MotivoRecordatorio.PAGO_SENA);
        assertThat(fila.getUsuario()).isSameAs(pedido.getCliente());
        assertThat(fila.getPedido()).isSameAs(pedido);
    }

    @Test
    void encolar_cuotaEnMenosDe24h_encolaPago24h() {
        Pedido pedido = pedido(EstadoPedido.EN_CURSO,
            LocalDate.now().minusDays(10), LocalDate.now().plusDays(10));
        HitoPedido hito = hito(pedido, TipoHito.CUOTA,
            EstadoPagoHito.PENDIENTE, EstadoEntregaHito.NO_INICIADO);
        hito.setFechaLimitePago(LocalDateTime.now().plusHours(10));
        when(hitoPedidoRepository.findPagosPorVencer(
            any(), any(), any(), any(), any())).thenReturn(List.of(hito));

        service.encolar();

        assertThat(capturarFila().getMotivo())
            .isEqualTo(MotivoRecordatorio.PAGO_24H);
    }

    @Test
    void encolar_cuotaEntre24y72h_encolaPago72h() {
        Pedido pedido = pedido(EstadoPedido.EN_CURSO,
            LocalDate.now().minusDays(10), LocalDate.now().plusDays(10));
        HitoPedido hito = hito(pedido, TipoHito.CUOTA,
            EstadoPagoHito.PENDIENTE, EstadoEntregaHito.NO_INICIADO);
        hito.setFechaLimitePago(LocalDateTime.now().plusHours(50));
        when(hitoPedidoRepository.findPagosPorVencer(
            any(), any(), any(), any(), any())).thenReturn(List.of(hito));

        service.encolar();

        assertThat(capturarFila().getMotivo())
            .isEqualTo(MotivoRecordatorio.PAGO_72H);
    }

    @Test
    void encolar_duplicado_noVuelveAEncolar() {
        Pedido pedido = pedido(EstadoPedido.PENDIENTE_PAGO, null, null);
        pedido.setFechaCreacion(LocalDateTime.now().minusHours(25));
        HitoPedido hito = hito(pedido, TipoHito.SENA,
            EstadoPagoHito.PENDIENTE, EstadoEntregaHito.NO_INICIADO);
        hito.setFechaLimitePago(LocalDateTime.now().plusDays(5));
        when(hitoPedidoRepository.findSenasPendientes(
            any(), any(), any(), any())).thenReturn(List.of(hito));
        when(recordatorioRepository.existsByClave(anyString())).thenReturn(true);

        assertThat(service.encolar()).isZero();
        verify(recordatorioRepository, never()).save(any());
    }

    @Test
    void encolar_pagoSeEncolaSiempre() {
        Pedido pedido = pedido(EstadoPedido.PENDIENTE_PAGO, null, null);
        pedido.setFechaCreacion(LocalDateTime.now().minusHours(25));
        HitoPedido hito = hito(pedido, TipoHito.SENA,
            EstadoPagoHito.PENDIENTE, EstadoEntregaHito.NO_INICIADO);
        hito.setFechaLimitePago(LocalDateTime.now().plusDays(5));
        when(hitoPedidoRepository.findSenasPendientes(
            any(), any(), any(), any())).thenReturn(List.of(hito));

        assertThat(service.encolar()).isEqualTo(1);
        assertThat(capturarFila().getMotivo())
            .isEqualTo(MotivoRecordatorio.PAGO_SENA);
    }

    @Test
    void encolar_artistaSancionado_noEncolaLaEntrega() {
        LocalDate hoy = LocalDate.now();
        Pedido pedido = pedido(EstadoPedido.EN_CURSO,
            hoy.minusDays(10), hoy.plusDays(7));
        hito(pedido, TipoHito.ENTREGA,
            EstadoPagoHito.PENDIENTE, EstadoEntregaHito.NO_INICIADO);
        when(pedidoRepository.findConHitosByEstado(EstadoPedido.EN_CURSO))
            .thenReturn(List.of(pedido));
        when(sancionService.obtenerVigente(anyLong()))
            .thenReturn(Optional.of(new SancionCuenta()));

        assertThat(service.encolar()).isZero();
        verify(recordatorioRepository, never()).save(any());
    }

    @Test
    void encolar_artistaASieteDias_conPedidoLargo_encolaEntrega7d() {
        LocalDate hoy = LocalDate.now();
        Pedido pedido = pedido(EstadoPedido.EN_CURSO,
            hoy.minusDays(10), hoy.plusDays(7));
        hito(pedido, TipoHito.ENTREGA,
            EstadoPagoHito.PENDIENTE, EstadoEntregaHito.NO_INICIADO);
        when(pedidoRepository.findConHitosByEstado(EstadoPedido.EN_CURSO))
            .thenReturn(List.of(pedido));

        service.encolar();

        assertThat(capturarFila().getMotivo())
            .isEqualTo(MotivoRecordatorio.ENTREGA_7D);
        assertThat(capturarFila().getUsuario()).isSameAs(pedido.getArtista());
    }

    @Test
    void encolar_pedidoCorto_noEncolaAvisoSemanal() {
        LocalDate hoy = LocalDate.now();
        Pedido pedido = pedido(EstadoPedido.EN_CURSO,
            hoy, hoy.plusDays(7));
        hito(pedido, TipoHito.ENTREGA,
            EstadoPagoHito.PENDIENTE, EstadoEntregaHito.NO_INICIADO);
        when(pedidoRepository.findConHitosByEstado(EstadoPedido.EN_CURSO))
            .thenReturn(List.of(pedido));

        assertThat(service.encolar()).isZero();
        verify(recordatorioRepository, never()).save(any());
    }

    @Test
    void encolar_artistaConEntregaHecha_noEncola() {
        LocalDate hoy = LocalDate.now();
        Pedido pedido = pedido(EstadoPedido.EN_CURSO,
            hoy.minusDays(10), hoy.plusDays(7));
        hito(pedido, TipoHito.ENTREGA,
            EstadoPagoHito.PENDIENTE, EstadoEntregaHito.ENTREGADO);
        when(pedidoRepository.findConHitosByEstado(EstadoPedido.EN_CURSO))
            .thenReturn(List.of(pedido));

        assertThat(service.encolar()).isZero();
        verify(recordatorioRepository, never()).save(any());
    }

    @Test
    void encolar_artistaManana_encolaEntrega1d() {
        LocalDate hoy = LocalDate.now();
        Pedido pedido = pedido(EstadoPedido.EN_CURSO,
            hoy.minusDays(3), hoy.plusDays(1));
        hito(pedido, TipoHito.ENTREGA,
            EstadoPagoHito.PENDIENTE, EstadoEntregaHito.NO_INICIADO);
        when(pedidoRepository.findConHitosByEstado(EstadoPedido.EN_CURSO))
            .thenReturn(List.of(pedido));

        service.encolar();

        assertThat(capturarFila().getMotivo())
            .isEqualTo(MotivoRecordatorio.ENTREGA_1D);
    }

    @Test
    void procesarPendientes_delegaEnElProcesador() {
        when(recordatorioRepository.findUsuariosConPendientes(any()))
            .thenReturn(List.of(1L, 2L));
        when(procesador.procesar(1L)).thenReturn(true);
        when(procesador.procesar(2L)).thenReturn(false);

        assertThat(service.procesarPendientes()).isEqualTo(1);
        verify(procesador).procesar(1L);
        verify(procesador).procesar(2L);
    }

    private EmailRecordatorioPendiente capturarFila() {
        ArgumentCaptor<EmailRecordatorioPendiente> captor =
            ArgumentCaptor.forClass(EmailRecordatorioPendiente.class);
        verify(recordatorioRepository).save(captor.capture());
        return captor.getValue();
    }

    private Pedido pedido(
            EstadoPedido estado, LocalDate inicio, LocalDate limite) {
        Pedido pedido = new Pedido();
        pedido.setIdPedido(PEDIDO);
        pedido.setEstado(estado);
        pedido.setFechaInicio(inicio);
        pedido.setLimiteEntrega(limite);
        pedido.setCliente(usuario(1L, "cliente"));
        pedido.setArtista(usuario(2L, "artista"));
        return pedido;
    }

    private HitoPedido hito(
            Pedido pedido,
            TipoHito tipo,
            EstadoPagoHito pago,
            EstadoEntregaHito entrega) {
        HitoPresupuesto presupuesto = new HitoPresupuesto();
        presupuesto.setTipo(tipo);
        presupuesto.setNombre("Hito " + tipo.name());
        presupuesto.setRequiereEntrega(true);
        HitoPedido hito = new HitoPedido();
        hito.setPedido(pedido);
        hito.setHitoPresupuesto(presupuesto);
        hito.setEstadoPago(pago);
        hito.setEstadoEntrega(entrega);
        pedido.agregarHito(hito);
        return hito;
    }

    private Usuario usuario(Long id, String nombre) {
        return Usuario.builder()
            .idUsuario(id)
            .nombreUsuario(nombre)
            .correo(nombre + "@test.local")
            .build();
    }
}
