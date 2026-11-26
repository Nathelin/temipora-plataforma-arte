package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.ConfiguracionPedido;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.EstadoProrroga;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.ProrrogaPedido;
import com.temipora.backend_springboot.modelo.ProrrogaRequest;
import com.temipora.backend_springboot.modelo.RespuestaProrrogaRequest;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.PedidoRepository;
import com.temipora.backend_springboot.repositorios.ProrrogaPedidoRepository;

@ExtendWith(MockitoExtension.class)
class ProrrogaServiceTest {

    private static final Long PEDIDO = 10L;
    private static final Long OTRO_PEDIDO = 99L;
    private static final Long ARTISTA = 2L;
    private static final Long CLIENTE = 1L;
    private static final Long PRORROGA = 30L;
    private static final LocalDate PROMETIDA = LocalDate.of(2026, 12, 12);
    private static final String TIPO_PEDIDO = "PEDIDO";

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private ProrrogaPedidoRepository prorrogaRepository;
    @Mock
    private ConfiguracionPedidoService configuracionPedidoService;
    @Mock
    private NotificacionService notificacionService;
    @Mock
    private AuditoriaService auditoria;

    @InjectMocks
    private ProrrogaService service;

    private Pedido pedido;

    @BeforeEach
    void setUp() {
        Usuario artista = new Usuario();
        artista.setIdUsuario(ARTISTA);
        artista.setNombreUsuario("artista");
        Usuario cliente = new Usuario();
        cliente.setIdUsuario(CLIENTE);
        cliente.setNombreUsuario("cliente");

        pedido = new Pedido();
        pedido.setIdPedido(PEDIDO);
        pedido.setArtista(artista);
        pedido.setCliente(cliente);
        pedido.setEstado(EstadoPedido.EN_CURSO);
        pedido.setLimiteEntrega(PROMETIDA);

        lenient().when(pedidoRepository.findByIdPedidoForUpdate(PEDIDO))
            .thenReturn(Optional.of(pedido));
        lenient().when(configuracionPedidoService.obtener())
            .thenReturn(new ConfiguracionPedido());
        lenient().when(prorrogaRepository.save(any(ProrrogaPedido.class)))
            .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void solicitar_creaPendienteConLaVentanaConfigurada() {
        service.solicitar(PEDIDO, ARTISTA, request(7, "Me enfermé"));

        ArgumentCaptor<ProrrogaPedido> captor =
            ArgumentCaptor.forClass(ProrrogaPedido.class);
        verify(prorrogaRepository).save(captor.capture());
        ProrrogaPedido guardada = captor.getValue();
        assertThat(guardada.getEstado()).isEqualTo(EstadoProrroga.PENDIENTE);
        assertThat(guardada.getDiasSolicitados()).isEqualTo(7);
        assertThat(guardada.getMotivo()).isEqualTo("Me enfermé");
        assertThat(ChronoUnit.HOURS.between(
            guardada.getFechaSolicitud(),
            guardada.getFechaLimiteRespuesta())).isEqualTo(24);
        verify(notificacionService).crearParaUsuario(
            eq(CLIENTE), eq("PRORROGA_SOLICITADA"), anyString(), anyString(),
            eq(PEDIDO), eq(TIPO_PEDIDO));
        verify(auditoria).registrar(
            eq(AccionesAdmin.PRORROGA_SOLICITAR), eq("ProrrogaPedido"),
            any(), anyString(), isNull(), any(), any());
    }

    @Test
    void solicitar_rechazaSiYaSeUsoLaProrrogaDelPedido() {
        when(prorrogaRepository.existsByPedidoIdPedido(PEDIDO))
            .thenReturn(true);

        assertThatThrownBy(() -> service.solicitar(
            PEDIDO, ARTISTA, request(3, "Más tiempo")))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("solo se puede pedir una vez");
    }

    @Test
    void solicitar_rechazaSiElPedidoYaEstaEnMora() {
        pedido.setMoraDesde(LocalDateTime.now().minusDays(1));

        assertThatThrownBy(() -> service.solicitar(
            PEDIDO, ARTISTA, request(3, "Más tiempo")))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("mora");
    }

    @Test
    void solicitar_requiereMotivoYDiasValidos() {
        assertThatThrownBy(() -> service.solicitar(
            PEDIDO, ARTISTA, request(0, "Motivo")))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.solicitar(
            PEDIDO, ARTISTA, request(3, "   ")))
            .isInstanceOf(IllegalArgumentException.class);
        verify(prorrogaRepository, never()).save(any(ProrrogaPedido.class));
    }

    @Test
    void aceptar_correLaEntregaPrevistaYNotificaAlArtista() {
        ProrrogaPedido pendiente = pendiente(7);
        when(prorrogaRepository.findByIdProrrogaForUpdate(PRORROGA))
            .thenReturn(Optional.of(pendiente));

        service.aceptar(PEDIDO, PRORROGA, CLIENTE);

        assertThat(pendiente.getEstado()).isEqualTo(EstadoProrroga.ACEPTADA);
        assertThat(pendiente.getFechaRespuesta()).isNotNull();
        assertThat(pendiente.getEntregaPrevistaAntes()).isEqualTo(PROMETIDA);
        assertThat(pendiente.getEntregaPrevistaDespues())
            .isEqualTo(PROMETIDA.plusDays(7));
        assertThat(PedidoFechas.entregaPrevista(pedido))
            .isEqualTo(PROMETIDA.plusDays(7));
        verify(notificacionService).crearParaUsuario(
            eq(ARTISTA), eq("PRORROGA_ACEPTADA"), anyString(), anyString(),
            eq(PEDIDO), eq(TIPO_PEDIDO));
        verify(auditoria).registrar(
            eq(AccionesAdmin.PRORROGA_ACEPTAR), eq("ProrrogaPedido"),
            any(), anyString(), any(), any(), any());
    }

    @Test
    void aceptar_rechazaUnaPropuestaVencida() {
        ProrrogaPedido pendiente = pendiente(7);
        pendiente.setFechaLimiteRespuesta(
            LocalDateTime.now().minusMinutes(1));
        when(prorrogaRepository.findByIdProrrogaForUpdate(PRORROGA))
            .thenReturn(Optional.of(pendiente));

        assertThatThrownBy(() -> service.aceptar(PEDIDO, PRORROGA, CLIENTE))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("venció");
        assertThat(pendiente.getEstado()).isEqualTo(EstadoProrroga.PENDIENTE);
    }

    @Test
    void aceptar_rechazaUnaProrrogaDeOtroPedido() {
        Pedido otro = new Pedido();
        otro.setIdPedido(OTRO_PEDIDO);
        ProrrogaPedido ajena = new ProrrogaPedido();
        ajena.setPedido(otro);
        when(prorrogaRepository.findByIdProrrogaForUpdate(PRORROGA))
            .thenReturn(Optional.of(ajena));

        assertThatThrownBy(() -> service.aceptar(PEDIDO, PRORROGA, CLIENTE))
            .isInstanceOf(java.util.NoSuchElementException.class)
            .hasMessageContaining("no pertenece");
    }

    @Test
    void rechazar_mantieneLaFechaYGuardaElMotivo() {
        ProrrogaPedido pendiente = pendiente(7);
        when(prorrogaRepository.findByIdProrrogaForUpdate(PRORROGA))
            .thenReturn(Optional.of(pendiente));
        RespuestaProrrogaRequest request = new RespuestaProrrogaRequest();
        request.setMotivo("Necesito la pieza para esa fecha");

        service.rechazar(PEDIDO, PRORROGA, CLIENTE, request);

        assertThat(pendiente.getEstado()).isEqualTo(EstadoProrroga.RECHAZADA);
        assertThat(pendiente.getMotivoRespuesta())
            .isEqualTo("Necesito la pieza para esa fecha");
        assertThat(PedidoFechas.entregaPrevista(pedido)).isEqualTo(PROMETIDA);
        verify(notificacionService).crearParaUsuario(
            eq(ARTISTA), eq("PRORROGA_RECHAZADA"), anyString(), anyString(),
            eq(PEDIDO), eq(TIPO_PEDIDO));
    }

    @Test
    void expirarVencidas_cierraLaPropuestaYNotificaAAmbasPartes() {
        ProrrogaPedido pendiente = pendiente(5);
        pendiente.setFechaLimiteRespuesta(LocalDateTime.now().minusHours(1));
        when(prorrogaRepository.findByEstadoAndFechaLimiteRespuestaBefore(
            eq(EstadoProrroga.PENDIENTE), any(LocalDateTime.class)))
            .thenReturn(List.of(pendiente));
        when(prorrogaRepository.findByIdProrrogaForUpdate(PRORROGA))
            .thenReturn(Optional.of(pendiente));

        int vencidas = service.expirarVencidas();

        assertThat(vencidas).isEqualTo(1);
        assertThat(pendiente.getEstado()).isEqualTo(EstadoProrroga.VENCIDA);
        assertThat(PedidoFechas.entregaPrevista(pedido)).isEqualTo(PROMETIDA);
        verify(notificacionService).crearParaUsuario(
            eq(ARTISTA), eq("PRORROGA_VENCIDA"), anyString(), anyString(),
            eq(PEDIDO), eq(TIPO_PEDIDO));
        verify(notificacionService).crearParaUsuario(
            eq(CLIENTE), eq("PRORROGA_VENCIDA"), anyString(), anyString(),
            eq(PEDIDO), eq(TIPO_PEDIDO));
    }

    private ProrrogaRequest request(int dias, String motivo) {
        ProrrogaRequest request = new ProrrogaRequest();
        request.setDias(dias);
        request.setMotivo(motivo);
        return request;
    }

    private ProrrogaPedido pendiente(int dias) {
        ProrrogaPedido prorroga = new ProrrogaPedido();
        prorroga.setIdProrroga(PRORROGA);
        prorroga.setDiasSolicitados(dias);
        prorroga.setMotivo("Motivo de prueba");
        prorroga.setEstado(EstadoProrroga.PENDIENTE);
        prorroga.setFechaSolicitud(LocalDateTime.now());
        prorroga.setFechaLimiteRespuesta(
            LocalDateTime.now().plusHours(24));
        pedido.agregarProrroga(prorroga);
        return prorroga;
    }
}
