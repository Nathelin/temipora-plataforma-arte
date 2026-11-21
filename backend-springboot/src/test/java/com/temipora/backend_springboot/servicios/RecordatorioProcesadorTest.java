package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.ConfiguracionPedido;
import com.temipora.backend_springboot.modelo.EmailRecordatorioPendiente;
import com.temipora.backend_springboot.modelo.EnvioEmail;
import com.temipora.backend_springboot.modelo.EstadoEmailRecordatorio;
import com.temipora.backend_springboot.modelo.EstadoEnvioEmail;
import com.temipora.backend_springboot.modelo.EstadoEntregaHito;
import com.temipora.backend_springboot.modelo.EstadoPagoHito;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.HitoPedido;
import com.temipora.backend_springboot.modelo.HitoPresupuesto;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.MotivoRecordatorio;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.Presupuesto;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.TipoHito;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.EmailRecordatorioPendienteRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class RecordatorioProcesadorTest {

    @Mock
    private EmailRecordatorioPendienteRepository repository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private SancionService sancionService;
    @Mock
    private ConfiguracionPedidoService configuracionPedidoService;
    @Mock
    private EmailPlantillaService emailPlantillaService;
    @Mock
    private EnvioEmailService envioEmailService;
    @Mock
    private AccesoEnlaceService accesoEnlaceService;

    @InjectMocks
    private RecordatorioProcesador procesador;

    @Test
    void procesar_enviaElCorreoYMarcaLasFilas() {
        Usuario usuario = usuario(1L);
        Pedido pedido = pedido(usuario, 10L, EstadoPedido.PENDIENTE_PAGO);
        EmailRecordatorioPendiente fila = fila(
            pedido, MotivoRecordatorio.PAGO_24H);
        prepararUsuario(usuario);
        prepararEnvio("<html>ok</html>");
        when(repository.findByUsuarioIdUsuarioAndEstadoOrderByCreadoEnAsc(
            eq(1L), eq(EstadoEmailRecordatorio.PENDIENTE), any()))
            .thenReturn(new ArrayList<>(List.of(fila)));

        boolean enviado = procesador.procesar(1L);

        assertThat(enviado).isTrue();
        assertThat(fila.getEstado()).isEqualTo(EstadoEmailRecordatorio.ENVIADO);
        assertThat(fila.getEnviadoEn()).isNotNull();
        assertThat(fila.getIdLote()).isNotNull();
        verify(envioEmailService).enviar(
            eq(usuario), eq("RECORDATORIO"), any(), any(),
            anyString(), eq("<html>ok</html>"), anyString());
    }

    @Test
    void procesar_pagosYEntregas_mandaDosCorreosSeparados() {
        Usuario usuario = usuario(1L);
        Pedido pago = pedido(usuario, 10L, EstadoPedido.PENDIENTE_PAGO);
        Pedido entrega = pedidoEntrega(usuario, 11L);
        EmailRecordatorioPendiente filaPago = fila(
            pago, MotivoRecordatorio.PAGO_24H);
        EmailRecordatorioPendiente filaEntrega = fila(
            entrega, MotivoRecordatorio.ENTREGA_HOY);
        prepararUsuario(usuario);
        when(repository.findByUsuarioIdUsuarioAndEstadoOrderByCreadoEnAsc(
            eq(1L), eq(EstadoEmailRecordatorio.PENDIENTE), any()))
            .thenReturn(new ArrayList<>(List.of(filaPago, filaEntrega)));
        when(emailPlantillaService.asuntoRecordatorio(
            eq(true), eq(false), anyBoolean())).thenReturn("Pagos");
        when(emailPlantillaService.asuntoRecordatorio(
            eq(false), eq(true), anyBoolean())).thenReturn("Entregas");
        when(emailPlantillaService.recordatorio(any(), any(), any(), any()))
            .thenReturn("<html>ok</html>");
        EnvioEmail envio = new EnvioEmail();
        envio.setEstado(EstadoEnvioEmail.ENVIADO);
        when(envioEmailService.enviar(
            any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(envio);

        boolean enviado = procesador.procesar(1L);

        assertThat(enviado).isTrue();
        ArgumentCaptor<String> asuntos = ArgumentCaptor.forClass(String.class);
        verify(envioEmailService, times(2)).enviar(
            eq(usuario), eq("RECORDATORIO"), any(), any(),
            asuntos.capture(), anyString(), anyString());
        assertThat(asuntos.getAllValues()).containsExactly("Pagos", "Entregas");
        assertThat(filaPago.getEstado())
            .isEqualTo(EstadoEmailRecordatorio.ENVIADO);
        assertThat(filaEntrega.getEstado())
            .isEqualTo(EstadoEmailRecordatorio.ENVIADO);
    }

    private void prepararUsuario(Usuario usuario) {
        when(usuarioRepository.findByIdUsuario(1L))
            .thenReturn(Optional.of(usuario));
        when(sancionService.obtenerVigente(1L)).thenReturn(Optional.empty());
        when(configuracionPedidoService.obtener()).thenReturn(configuracion());
        when(accesoEnlaceService.generar(any(), any(), any(), anyInt()))
            .thenReturn("tok");
    }

    private ConfiguracionPedido configuracion() {
        ConfiguracionPedido config = new ConfiguracionPedido();
        config.setDiasGraciaEntrega(3);
        return config;
    }

    private void prepararEnvio(String html) {
        when(emailPlantillaService.asuntoRecordatorio(
            anyBoolean(), anyBoolean(), anyBoolean())).thenReturn("Asunto");
        when(emailPlantillaService.recordatorio(any(), any(), any(), any()))
            .thenReturn(html);
        EnvioEmail envio = new EnvioEmail();
        envio.setEstado(EstadoEnvioEmail.ENVIADO);
        when(envioEmailService.enviar(
            any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(envio);
    }

    private EmailRecordatorioPendiente fila(
            Pedido pedido, MotivoRecordatorio motivo) {
        EmailRecordatorioPendiente fila = new EmailRecordatorioPendiente();
        fila.setUsuario(pedido.getCliente());
        fila.setPedido(pedido);
        fila.setHitoPedido(pedido.getHitos().get(0));
        fila.setMotivo(motivo);
        fila.setClave(motivo.name() + ":" + pedido.getIdPedido() + ":1:");
        fila.setFechaReferencia(LocalDateTime.now().plusHours(10));
        return fila;
    }

    private Pedido pedido(Usuario usuario, Long id, EstadoPedido estado) {
        Pedido pedido = base(usuario, id, estado);
        HitoPedido hito = hito(pedido, TipoHito.SENA);
        hito.setFechaLimitePago(LocalDateTime.now().plusHours(10));
        return pedido;
    }

    private Pedido pedidoEntrega(Usuario usuario, Long id) {
        LocalDate hoy = LocalDate.now();
        Pedido pedido = base(usuario, id, EstadoPedido.EN_CURSO);
        pedido.setFechaInicio(hoy);
        pedido.setLimiteEntrega(hoy.plusDays(3));
        HitoPedido hito = hito(pedido, TipoHito.ENTREGA);
        hito.setEstadoEntrega(EstadoEntregaHito.NO_INICIADO);
        return pedido;
    }

    private Pedido base(
            Usuario usuario, Long id, EstadoPedido estado) {
        Moneda moneda = Moneda.builder().codigo("ARS").build();
        Presupuesto presupuesto = new Presupuesto();
        presupuesto.setMoneda(moneda);
        Servicio servicio = new Servicio();
        servicio.setNombre("Retrato");

        Pedido pedido = new Pedido();
        pedido.setIdPedido(id);
        pedido.setEstado(estado);
        pedido.setCliente(usuario);
        pedido.setArtista(usuario);
        pedido.setServicio(servicio);
        pedido.setPresupuesto(presupuesto);
        return pedido;
    }

    private HitoPedido hito(Pedido pedido, TipoHito tipo) {
        HitoPresupuesto hitoPresupuesto = new HitoPresupuesto();
        hitoPresupuesto.setTipo(tipo);
        hitoPresupuesto.setNombre("Hito " + tipo.name());
        hitoPresupuesto.setMonto(new BigDecimal("100.00"));
        hitoPresupuesto.setRequiereEntrega(true);
        HitoPedido hito = new HitoPedido();
        hito.setPedido(pedido);
        hito.setHitoPresupuesto(hitoPresupuesto);
        hito.setEstadoPago(EstadoPagoHito.PENDIENTE);
        pedido.agregarHito(hito);
        return hito;
    }

    private Usuario usuario(Long id) {
        return Usuario.builder()
            .idUsuario(id)
            .nombreUsuario("usuario")
            .nombreDisplay("Usuario")
            .correo("usuario@test.local")
            .estadoCuenta(true)
            .build();
    }
}
