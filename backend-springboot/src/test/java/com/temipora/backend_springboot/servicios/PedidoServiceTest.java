package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.multipart.MultipartFile;

import com.temipora.backend_springboot.modelo.AceptacionTerminosPlataforma;
import com.temipora.backend_springboot.modelo.ContadoresPedidosDTO;
import com.temipora.backend_springboot.modelo.ArancelEstimadoDTO;
import com.temipora.backend_springboot.modelo.Conversacion;
import com.temipora.backend_springboot.modelo.EntregaHito;
import com.temipora.backend_springboot.modelo.EntregaHitoMultipartRequest;
import com.temipora.backend_springboot.modelo.ETiempoEntregaUnidad;
import com.temipora.backend_springboot.modelo.EstadoConversacion;
import com.temipora.backend_springboot.modelo.EstadoEntregaHito;
import com.temipora.backend_springboot.modelo.EstadoMensaje;
import com.temipora.backend_springboot.modelo.EstadoPago;
import com.temipora.backend_springboot.modelo.EstadoPagoHito;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.EstadoSolicitud;
import com.temipora.backend_springboot.modelo.HitoPedido;
import com.temipora.backend_springboot.modelo.HitoPresupuesto;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.Pago;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.PedidoDTO;
import com.temipora.backend_springboot.modelo.PedidoResumenDTO;
import com.temipora.backend_springboot.modelo.Presupuesto;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.Solicitud;
import com.temipora.backend_springboot.modelo.SolicitudRevisionRequest;
import com.temipora.backend_springboot.modelo.TipoHito;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.ConversacionRepository;
import com.temipora.backend_springboot.repositorios.HitoPedidoRepository;
import com.temipora.backend_springboot.repositorios.MensajeRepository;
import com.temipora.backend_springboot.repositorios.PagoRepository;
import com.temipora.backend_springboot.repositorios.PedidoRepository;
import com.temipora.backend_springboot.repositorios.SolicitudRepository;
import com.temipora.backend_springboot.servicios.pagos.ArancelEstimadoService;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    private static final Long CLIENTE = 1L;
    private static final Long ARTISTA = 2L;
    private static final Long PEDIDO = 10L;
    private static final Long HITO = 20L;

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private HitoPedidoRepository hitoPedidoRepository;
    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private SolicitudRepository solicitudRepository;
    @Mock
    private PagoService pagoService;
    @Mock
    private HitoFlujoService hitoFlujoService;
    @Mock
    private TerminosPlataformaService terminosPlataformaService;
    @Mock
    private NotificacionService notificacionService;
    @Mock
    private MonedaVistaService monedaVistaService;
    @Mock
    private EntregaService entregaService;
    @Mock
    private PedidoCierreService pedidoCierreService;
    @Mock
    private ConversacionRepository conversacionRepository;
    @Mock
    private MensajeRepository mensajeRepository;
    @Mock
    private AuditoriaService auditoria;
    @Mock
    private ArancelEstimadoService arancelEstimadoService;

    @Mock
    private CuposEventPublisher cuposEventPublisher;

    @Mock
    private CierreEmailService cierreEmailService;

    @Mock
    private SancionService sancionService;

    @Mock
    private EntregaPendienteService entregaPendienteService;

    @InjectMocks
    private PedidoService pedidoService;

    private Usuario cliente;
    private Usuario artista;
    private Moneda moneda;
    private Servicio servicio;
    private Presupuesto presupuesto;
    private HitoPresupuesto hitoPresupuesto;
    private Solicitud solicitud;
    private Pedido pedido;
    private HitoPedido hito;

    @BeforeEach
    void setUp() {
        cliente = usuario(CLIENTE, "cliente");
        artista = usuario(ARTISTA, "artista");
        lenient().when(arancelEstimadoService.estimar())
            .thenReturn(new ArancelEstimadoDTO(
                new BigDecimal("3.49"),
                new BigDecimal("21"),
                new BigDecimal("4.2229")));

        moneda = Moneda.builder()
            .idMoneda(3L)
            .codigo("ARS")
            .nombre("Peso argentino")
            .build();

        servicio = new Servicio();
        servicio.setIdServicio(5L);
        servicio.setNombre("Ilustración");

        presupuesto = new Presupuesto();
        presupuesto.setIdPresupuesto(7L);
        presupuesto.setPrecioFinal(new BigDecimal("100.00"));
        presupuesto.setPrecioFinalCobro(new BigDecimal("100.00"));
        presupuesto.setPrecioBaseTotal(new BigDecimal("100.00"));
        presupuesto.setComisionPorcentaje(new BigDecimal("10.00"));
        presupuesto.setComisionMonto(new BigDecimal("10.00"));
        presupuesto.setComisionMontoCobro(new BigDecimal("10.00"));
        presupuesto.setMontoNetoArtista(new BigDecimal("90.00"));
        presupuesto.setMontoNetoArtistaCobro(new BigDecimal("90.00"));
        presupuesto.setMoneda(moneda);
        presupuesto.setMonedaCobro(moneda);
        presupuesto.setCotizacionCobro(BigDecimal.ONE);
        presupuesto.setTiempoEntregaValor(2);
        presupuesto.setTiempoEntregaUnidad(ETiempoEntregaUnidad.SEMANAS);
        presupuesto.setTiempoEntregaDias(14);

        hitoPresupuesto = new HitoPresupuesto();
        hitoPresupuesto.setIdHitoPresupuesto(8L);
        hitoPresupuesto.setPresupuesto(presupuesto);
        hitoPresupuesto.setOrden(0);
        hitoPresupuesto.setNombre("Pago único");
        hitoPresupuesto.setDescripcion("Bocetos");
        hitoPresupuesto.setTipo(TipoHito.SENA);
        hitoPresupuesto.setPorcentaje(new BigDecimal("100.00"));
        hitoPresupuesto.setMonto(new BigDecimal("100.00"));
        hitoPresupuesto.setMontoCobro(new BigDecimal("100.00"));
        hitoPresupuesto.setComisionMonto(new BigDecimal("10.00"));
        hitoPresupuesto.setComisionMontoCobro(new BigDecimal("10.00"));
        hitoPresupuesto.setMontoNeto(new BigDecimal("90.00"));
        hitoPresupuesto.setMontoNetoCobro(new BigDecimal("90.00"));
        hitoPresupuesto.setRequiereEntrega(true);
        presupuesto.setHitos(new ArrayList<>(List.of(hitoPresupuesto)));

        solicitud = new Solicitud();
        solicitud.setIdSolicitud(9L);
        solicitud.setCliente(cliente);
        solicitud.setArtista(artista);
        solicitud.setServicio(servicio);
        solicitud.setPresupuesto(presupuesto);
        solicitud.setEstado(EstadoSolicitud.PRESUPUESTADA);
        solicitud.setItems(new ArrayList<>());

        pedido = new Pedido();
        pedido.setIdPedido(PEDIDO);
        pedido.setSolicitud(solicitud);
        pedido.setPresupuesto(presupuesto);
        pedido.setCliente(cliente);
        pedido.setArtista(artista);
        pedido.setServicio(servicio);
        pedido.setEstado(EstadoPedido.PENDIENTE_PAGO);

        hito = new HitoPedido();
        hito.setIdHitoPedido(HITO);
        hito.setPedido(pedido);
        hito.setHitoPresupuesto(hitoPresupuesto);
        hito.setOrden(0);
        hito.setEstadoPago(EstadoPagoHito.PENDIENTE);
        hito.setEstadoEntrega(EstadoEntregaHito.NO_INICIADO);
        pedido.agregarHito(hito);

        lenient().when(monedaVistaService.preferidaDe(anyLong()))
            .thenReturn(moneda);
        lenient().when(monedaVistaService.factorOrigenAMostrada(
            any(), any(), any())).thenReturn(BigDecimal.ONE);
        lenient().when(monedaVistaService.factorCobroAMostrada(
            any(), any())).thenReturn(BigDecimal.ONE);
        lenient().when(entregaService.listarPorPedido(anyLong()))
            .thenReturn(List.of());
        lenient().when(entregaService.crear(any(), any(), any(), any()))
            .thenReturn(new EntregaHito());
        lenient().when(pagoRepository
            .findByHitoPedidoIdHitoPedidoOrderByFechaCreacionDesc(anyLong()))
            .thenReturn(List.of());
        lenient().when(pedidoRepository.save(any()))
            .thenAnswer(invocacion -> invocacion.getArgument(0));
        lenient().when(hitoPedidoRepository.save(any()))
            .thenAnswer(invocacion -> invocacion.getArgument(0));
        lenient().when(solicitudRepository.save(any()))
            .thenAnswer(invocacion -> invocacion.getArgument(0));
        lenient().when(hitoFlujoService.revisionesMaximasPorHito()).thenReturn(2);
        lenient().when(hitoFlujoService.diasReentrega()).thenReturn(7);
    }

    @Test
    void crearDesdePresupuesto_conSena_creaElPagoInicial() {
        prepararCreacion();

        PedidoDTO dto = pedidoService.crearDesdePresupuesto(
            solicitud.getIdSolicitud(), CLIENTE);

        assertThat(dto.getEstado()).isEqualTo("PENDIENTE_PAGO");
        assertThat(dto.getHitos()).hasSize(1);
        verify(pagoService).iniciarPago(any(HitoPedido.class), any(Pedido.class));
        verify(pagoService, never()).cancelarPendientes(any());
    }

    @Test
    void crearDesdePresupuesto_conEntrega_iniciaSinPago() {
        hitoPresupuesto.setTipo(TipoHito.ENTREGA);
        hitoPresupuesto.setNombre("Entrega final");
        prepararCreacion();

        PedidoDTO dto = pedidoService.crearDesdePresupuesto(
            solicitud.getIdSolicitud(), CLIENTE);

        assertThat(dto.getEstado()).isEqualTo("EN_CURSO");
        assertThat(dto.getFechaInicio()).isNotNull();
        assertThat(dto.getLimiteEntrega()).isNotNull();
        verify(pagoService).cancelarPendientes(any(HitoPedido.class));
        verify(pagoService, never()).iniciarPago(any(), any());
    }

    @Test
    void crearDesdePresupuesto_sinAceptacionTerminos_lanzaConflicto() {
        when(solicitudRepository.findByIdSolicitudForUpdate(
            solicitud.getIdSolicitud())).thenReturn(Optional.of(solicitud));
        when(pedidoRepository.findBySolicitudIdSolicitud(
            solicitud.getIdSolicitud())).thenReturn(Optional.empty());
        when(terminosPlataformaService.obtenerAceptacionVigente(anyLong()))
            .thenThrow(new ConflictoException(
                "Tenés que aceptar los términos de la plataforma vigentes."));

        assertThatThrownBy(() -> pedidoService.crearDesdePresupuesto(
            solicitud.getIdSolicitud(), CLIENTE))
            .isInstanceOf(ConflictoException.class);

        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    void crearDesdePresupuesto_pedidoExistenteSena_iniciaElPago() {
        pedido.setEstado(EstadoPedido.PENDIENTE_PAGO);
        when(solicitudRepository.findByIdSolicitudForUpdate(
            solicitud.getIdSolicitud())).thenReturn(Optional.of(solicitud));
        when(pedidoRepository.findBySolicitudIdSolicitud(
            solicitud.getIdSolicitud())).thenReturn(Optional.of(pedido));
        when(pagoRepository.findFirstByHitoPedidoIdHitoPedidoAndEstado(
            HITO, EstadoPago.PENDIENTE)).thenReturn(Optional.empty());

        PedidoDTO dto = pedidoService.crearDesdePresupuesto(
            solicitud.getIdSolicitud(), CLIENTE);

        assertThat(dto.getEstado()).isEqualTo("PENDIENTE_PAGO");
        verify(pagoService).iniciarPago(hito, pedido);
    }

    @Test
    void crearDesdePresupuesto_pedidoExistenteEntrega_iniciaSinPago() {
        hito.getHitoPresupuesto().setTipo(TipoHito.ENTREGA);
        hito.getHitoPresupuesto().setNombre("Entrega final");
        when(solicitudRepository.findByIdSolicitudForUpdate(
            solicitud.getIdSolicitud())).thenReturn(Optional.of(solicitud));
        when(pedidoRepository.findBySolicitudIdSolicitud(
            solicitud.getIdSolicitud())).thenReturn(Optional.of(pedido));

        PedidoDTO dto = pedidoService.crearDesdePresupuesto(
            solicitud.getIdSolicitud(), CLIENTE);

        assertThat(dto.getEstado()).isEqualTo("EN_CURSO");
        verify(pagoService).cancelarPendientes(hito);
        verify(pagoService, never()).iniciarPago(any(), any());
    }

    @Test
    void pagarHito_sena_creaElPago() {
        pedido.setEstado(EstadoPedido.PENDIENTE_PAGO);
        bloquearPedidoYHito();
        when(pagoRepository
            .findByHitoPedidoIdHitoPedidoOrderByFechaCreacionDesc(HITO))
            .thenReturn(List.of());

        PedidoDTO dto = pedidoService.pagarHito(PEDIDO, HITO, CLIENTE);

        assertThat(dto).isNotNull();
        verify(pagoService).iniciarPago(hito, pedido);
    }

    @Test
    void entregar_sena_marcaEntregadoYNotifica() {
        pedido.setEstado(EstadoPedido.EN_CURSO);
        hito.setEstadoPago(EstadoPagoHito.PAGADO);
        bloquearPedidoYHito();

        PedidoDTO dto = pedidoService.entregar(
            PEDIDO, HITO, ARTISTA,
            new EntregaHitoMultipartRequest(), List.<MultipartFile>of());

        assertThat(dto).isNotNull();
        assertThat(hito.getEstadoEntrega())
            .isEqualTo(EstadoEntregaHito.ENTREGADO);
        assertThat(hito.getFechaEntrega()).isNotNull();
        assertThat(hito.getFechaLimiteAprobacion()).isNotNull();
        verify(entregaService).crear(eq(pedido), eq(hito), any(), any());
    }

    @Test
    void entregar_fueraDePlazo_registraLaMora() {
        pedido.setEstado(EstadoPedido.EN_CURSO);
        hito.setEstadoPago(EstadoPagoHito.PAGADO);
        bloquearPedidoYHito();
        LocalDateTime plazo = LocalDateTime.now().minusDays(2);
        when(entregaPendienteService.plazoVigente(pedido, hito))
            .thenReturn(plazo);

        pedidoService.entregar(
            PEDIDO, HITO, ARTISTA,
            new EntregaHitoMultipartRequest(), List.<MultipartFile>of());

        assertThat(pedido.getMoraDesde()).isEqualTo(plazo);
        verify(auditoria).registrarSistema(
            eq(AccionesAdmin.PEDIDO_MORA_DETECTAR), eq("Pedido"),
            eq(PEDIDO), any());
    }

    @Test
    void entregar_entregaUnica_permiteSinEtapaAnterior() {
        pedido.setEstado(EstadoPedido.EN_CURSO);
        hito.getHitoPresupuesto().setTipo(TipoHito.ENTREGA);
        hito.setEstadoPago(EstadoPagoHito.PENDIENTE);
        bloquearPedidoYHito();

        PedidoDTO dto = pedidoService.entregar(
            PEDIDO, HITO, ARTISTA,
            new EntregaHitoMultipartRequest(), List.<MultipartFile>of());

        assertThat(dto).isNotNull();
        assertThat(hito.getEstadoEntrega())
            .isEqualTo(EstadoEntregaHito.ENTREGADO);
        assertThat(hito.getFechaLimitePago()).isNotNull();
    }

    @Test
    void aprobar_senaUnica_finalizaElPedido() {
        pedido.setEstado(EstadoPedido.EN_CURSO);
        hito.setEstadoPago(EstadoPagoHito.PAGADO);
        hito.setEstadoEntrega(EstadoEntregaHito.ENTREGADO);
        bloquearPedidoYHito();
        when(hitoFlujoService.habilitarSiguiente(any(), any(), any()))
            .thenReturn(null);

        PedidoDTO dto = pedidoService.aprobar(PEDIDO, HITO, CLIENTE);

        assertThat(dto).isNotNull();
        assertThat(hito.getEstadoEntrega())
            .isEqualTo(EstadoEntregaHito.APROBADO);
        verify(pedidoCierreService).finalizar(eq(pedido), any());
    }

    @Test
    void aprobar_conSiguienteHito_noFinaliza() {
        pedido.setEstado(EstadoPedido.EN_CURSO);
        hito.setEstadoPago(EstadoPagoHito.PAGADO);
        hito.setEstadoEntrega(EstadoEntregaHito.ENTREGADO);
        bloquearPedidoYHito();
        HitoPedido siguiente = new HitoPedido();
        siguiente.setIdHitoPedido(21L);
        siguiente.setOrden(1);
        HitoPresupuesto presupuestoSiguiente = new HitoPresupuesto();
        presupuestoSiguiente.setTipo(TipoHito.ENTREGA);
        siguiente.setHitoPresupuesto(presupuestoSiguiente);
        when(hitoFlujoService.habilitarSiguiente(any(), any(), any()))
            .thenReturn(siguiente);

        pedidoService.aprobar(PEDIDO, HITO, CLIENTE);

        verify(pedidoCierreService, never()).finalizar(any(), any());
    }

    @Test
    void solicitarRevision_marcaElHitoYRegistraElMotivo() {
        pedido.setEstado(EstadoPedido.EN_CURSO);
        hito.setEstadoPago(EstadoPagoHito.PAGADO);
        hito.setEstadoEntrega(EstadoEntregaHito.ENTREGADO);
        bloquearPedidoYHito();
        SolicitudRevisionRequest request = new SolicitudRevisionRequest();
        request.setMotivo("Cambiar la pose");

        PedidoDTO dto = pedidoService.solicitarRevision(
            PEDIDO, HITO, CLIENTE, request);

        assertThat(dto).isNotNull();
        assertThat(hito.getEstadoEntrega())
            .isEqualTo(EstadoEntregaHito.CON_REVISION);
        assertThat(hito.getRevisiones()).isEqualTo(1);
        assertThat(hito.getFechaLimiteReentrega()).isNotNull();
        verify(entregaService).registrarRevision(
            hito, cliente, "Cambiar la pose");
    }

    @Test
    void solicitarRevision_alcanzadoElTope_lanzaConflicto() {
        pedido.setEstado(EstadoPedido.EN_CURSO);
        hito.setEstadoPago(EstadoPagoHito.PAGADO);
        hito.setEstadoEntrega(EstadoEntregaHito.ENTREGADO);
        hito.setRevisiones(2);
        bloquearPedidoYHito();

        assertThatThrownBy(() -> pedidoService.solicitarRevision(
            PEDIDO, HITO, CLIENTE, new SolicitudRevisionRequest()))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("revisiones");
        verify(entregaService, never()).registrarRevision(any(), any(), any());
    }

    @Test
    void entregar_limpiaElPlazoDeReentrega() {
        pedido.setEstado(EstadoPedido.EN_CURSO);
        hito.setEstadoPago(EstadoPagoHito.PAGADO);
        hito.setEstadoEntrega(EstadoEntregaHito.CON_REVISION);
        hito.setFechaLimiteReentrega(LocalDateTime.now().plusDays(3));
        bloquearPedidoYHito();

        pedidoService.entregar(
            PEDIDO, HITO, ARTISTA,
            new EntregaHitoMultipartRequest(), List.<MultipartFile>of());

        assertThat(hito.getFechaLimiteReentrega()).isNull();
    }

    @Test
    void cancelar_pedidoEnCurso_cancelaHitosPendientes() {
        pedido.setEstado(EstadoPedido.EN_CURSO);
        hito.setEstadoPago(EstadoPagoHito.PENDIENTE);
        when(pedidoRepository.findByIdPedidoForUpdate(PEDIDO))
            .thenReturn(Optional.of(pedido));

        PedidoDTO dto = pedidoService.cancelar(PEDIDO, CLIENTE);

        assertThat(dto).isNotNull();
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.CANCELADO);
        assertThat(hito.getEstadoPago()).isEqualTo(EstadoPagoHito.CANCELADO);
        verify(pagoService).cancelarPendientes(hito);
    }

    @Test
    void pagarHito_conPagoPendiente_lanzaConflicto() {
        pedido.setEstado(EstadoPedido.PENDIENTE_PAGO);
        bloquearPedidoYHito();
        Pago pago = new Pago();
        pago.setEstado(EstadoPago.PENDIENTE);
        when(pagoRepository
            .findByHitoPedidoIdHitoPedidoOrderByFechaCreacionDesc(HITO))
            .thenReturn(List.of(pago));

        assertThatThrownBy(() -> pedidoService.pagarHito(PEDIDO, HITO, CLIENTE))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("pago pendiente");
    }

    @Test
    void solicitarRevision_sinEntregar_lanzaConflicto() {
        pedido.setEstado(EstadoPedido.EN_CURSO);
        hito.setEstadoPago(EstadoPagoHito.PAGADO);
        hito.setEstadoEntrega(EstadoEntregaHito.NO_INICIADO);
        bloquearPedidoYHito();

        assertThatThrownBy(() -> pedidoService.solicitarRevision(
            PEDIDO, HITO, CLIENTE, new SolicitudRevisionRequest()))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("revisión");
    }

    @Test
    void cancelar_pedidoFinalizado_lanzaConflicto() {
        pedido.setEstado(EstadoPedido.FINALIZADO);
        when(pedidoRepository.findByIdPedidoForUpdate(PEDIDO))
            .thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.cancelar(PEDIDO, CLIENTE))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("cerrado");
    }

    @Test
    void entregar_otroArtista_lanzaAccesoDenegado() {
        when(pedidoRepository.findByIdPedidoForUpdate(PEDIDO))
            .thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.entregar(
            PEDIDO, HITO, 99L,
            new EntregaHitoMultipartRequest(), List.<MultipartFile>of()))
            .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void aprobarAutomaticamente_ignoraEntregasFinales() {
        hito.getHitoPresupuesto().setTipo(TipoHito.ENTREGA);
        hito.setEstadoEntrega(EstadoEntregaHito.ENTREGADO);
        hito.setFechaLimiteAprobacion(LocalDateTime.now().minusDays(1));
        when(hitoPedidoRepository
            .findByEstadoEntregaAndFechaLimiteAprobacionBefore(
                eq(EstadoEntregaHito.ENTREGADO), any()))
            .thenReturn(List.of(hito));
        when(hitoPedidoRepository.findByIdHitoPedidoForUpdate(HITO))
            .thenReturn(Optional.of(hito));

        int aprobados = pedidoService.aprobarAutomaticamente();

        assertThat(aprobados).isZero();
        verify(pedidoCierreService, never()).finalizar(any(), any());
    }

    @Test
    void aprobarAutomaticamente_apruebaYFinaliza() {
        hito.setEstadoEntrega(EstadoEntregaHito.ENTREGADO);
        hito.setFechaLimiteAprobacion(LocalDateTime.now().minusDays(1));
        pedido.setEstado(EstadoPedido.EN_CURSO);
        when(hitoPedidoRepository
            .findByEstadoEntregaAndFechaLimiteAprobacionBefore(
                eq(EstadoEntregaHito.ENTREGADO), any()))
            .thenReturn(List.of(hito));
        when(hitoPedidoRepository.findByIdHitoPedidoForUpdate(HITO))
            .thenReturn(Optional.of(hito));
        when(pedidoRepository.findByIdPedidoForUpdate(PEDIDO))
            .thenReturn(Optional.of(pedido));
        when(hitoFlujoService.habilitarSiguiente(any(), any(), any()))
            .thenReturn(null);

        int aprobados = pedidoService.aprobarAutomaticamente();

        assertThat(aprobados).isEqualTo(1);
        assertThat(hito.getEstadoEntrega())
            .isEqualTo(EstadoEntregaHito.APROBADO_AUTOMATICAMENTE);
        verify(pedidoCierreService).finalizar(eq(pedido), any());
    }

    @Test
    void expirarHitos_marcaElPedidoImpago() {
        hito.setEstadoPago(EstadoPagoHito.PENDIENTE);
        hito.setFechaLimitePago(LocalDateTime.now().minusDays(1));
        pedido.setEstado(EstadoPedido.EN_CURSO);
        when(hitoPedidoRepository
            .findByEstadoPagoAndFechaLimitePagoBefore(
                eq(EstadoPagoHito.PENDIENTE), any()))
            .thenReturn(List.of(hito));
        when(hitoPedidoRepository.findByIdHitoPedidoForUpdate(HITO))
            .thenReturn(Optional.of(hito));
        when(pedidoRepository.findByIdPedidoForUpdate(PEDIDO))
            .thenReturn(Optional.of(pedido));

        int expirados = pedidoService.expirarHitos();

        assertThat(expirados).isEqualTo(1);
        assertThat(hito.getEstadoPago()).isEqualTo(EstadoPagoHito.VENCIDO);
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.IMPAGO);
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.CANCELADA);
        verify(pagoService).cancelarPendientes(hito);
    }

    @Test
    void expirarHitos_entregaFinalImpaga_noMarcaImpago() {
        hito.getHitoPresupuesto().setTipo(TipoHito.ENTREGA);
        hito.setEstadoPago(EstadoPagoHito.PENDIENTE);
        hito.setFechaLimitePago(LocalDateTime.now().minusDays(5));
        pedido.setEstado(EstadoPedido.EN_CURSO);
        when(hitoPedidoRepository
            .findByEstadoPagoAndFechaLimitePagoBefore(
                eq(EstadoPagoHito.PENDIENTE), any()))
            .thenReturn(List.of(hito));
        when(hitoPedidoRepository.findByIdHitoPedidoForUpdate(HITO))
            .thenReturn(Optional.of(hito));

        int expirados = pedidoService.expirarHitos();

        assertThat(expirados).isZero();
        assertThat(hito.getEstadoPago()).isEqualTo(EstadoPagoHito.PENDIENTE);
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.EN_CURSO);
        verify(pagoService, never()).cancelarPendientes(any());
    }

    @Test
    void expirarHitos_dentroDeLaGracia_noMarcaImpago() {
        hito.setEstadoPago(EstadoPagoHito.PENDIENTE);
        hito.setFechaLimitePago(LocalDateTime.now().minusDays(1));
        pedido.setEstado(EstadoPedido.EN_CURSO);
        when(hitoPedidoRepository
            .findByEstadoPagoAndFechaLimitePagoBefore(
                eq(EstadoPagoHito.PENDIENTE), any()))
            .thenReturn(List.of(hito));
        when(hitoPedidoRepository.findByIdHitoPedidoForUpdate(HITO))
            .thenReturn(Optional.of(hito));
        when(hitoFlujoService.diasGraciaPago()).thenReturn(3);

        int expirados = pedidoService.expirarHitos();

        assertThat(expirados).isZero();
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.EN_CURSO);
    }

    @Test
    void listarRecibidos_conGrupoArchivo_consultaFinalizadosCanceladosEImpagos() {
        when(pedidoRepository.buscarRecibidos(
            eq(ARTISTA), any(), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(pedido)));
        ArgumentCaptor<Collection<EstadoPedido>> estados =
            ArgumentCaptor.forClass(Collection.class);

        pedidoService.listarRecibidos(
            ARTISTA, filtro(null, "archivo"), 0, 20);

        verify(pedidoRepository).buscarRecibidos(
            eq(ARTISTA), estados.capture(), any(), any(),
            any(Pageable.class));
        assertThat(estados.getValue()).containsExactlyInAnyOrder(
            EstadoPedido.FINALIZADO,
            EstadoPedido.CANCELADO,
            EstadoPedido.IMPAGO,
            EstadoPedido.INCUMPLIMIENTO);
    }

    @Test
    void contadores_incluyeArchivoYAccion() {
        pedido.setEstado(EstadoPedido.EN_CURSO);
        hito.setEstadoPago(EstadoPagoHito.PAGADO);
        hito.setEstadoEntrega(EstadoEntregaHito.NO_INICIADO);
        when(pedidoRepository.countByArtistaIdUsuario(ARTISTA))
            .thenReturn(10L);
        when(pedidoRepository.countByArtistaIdUsuarioAndEstadoIn(
            eq(ARTISTA), any())).thenReturn(3L);
        when(pedidoRepository.candidatosAccionRecibidos(
            eq(ARTISTA), any(), any(), any(), any()))
            .thenReturn(List.of(pedido));

        ContadoresPedidosDTO contadores = pedidoService.contadores(
            ARTISTA, true);

        assertThat(contadores.getTodos()).isEqualTo(10);
        assertThat(contadores.getEnCurso()).isEqualTo(3);
        assertThat(contadores.getArchivo()).isEqualTo(3);
        assertThat(contadores.getAccion()).isEqualTo(1);
    }

    @Test
    void listarEnviados_devuelveElResumen() {
        when(pedidoRepository.buscarEnviados(
            eq(CLIENTE), any(), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(pedido)));

        PaginaDTO<PedidoResumenDTO> pagina = pedidoService.listarEnviados(
            CLIENTE, filtro(null, null), 0, 20);

        assertThat(pagina.getContenido()).hasSize(1);
        assertThat(pagina.getContenido().get(0).getIdPedido())
            .isEqualTo(PEDIDO);
    }

    @Test
    void listarRecibidos_devuelveElResumen() {
        when(pedidoRepository.buscarRecibidos(
            eq(ARTISTA), any(), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(pedido)));

        PaginaDTO<PedidoResumenDTO> pagina = pedidoService.listarRecibidos(
            ARTISTA, filtro(null, null), 0, 20);

        assertThat(pagina.getContenido()).hasSize(1);
    }

    @Test
    void listarRecibidos_conGrupoEnCurso_consultaLosEstadosActivos() {
        when(pedidoRepository.buscarRecibidos(
            eq(ARTISTA), any(), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(pedido)));
        ArgumentCaptor<Collection<EstadoPedido>> estados =
            ArgumentCaptor.forClass(Collection.class);

        pedidoService.listarRecibidos(
            ARTISTA, filtro(null, "en-curso"), 0, 20);

        verify(pedidoRepository).buscarRecibidos(
            eq(ARTISTA), estados.capture(), any(), any(),
            any(Pageable.class));
        assertThat(estados.getValue()).containsExactlyInAnyOrder(
            EstadoPedido.PENDIENTE_PAGO, EstadoPedido.EN_CURSO);
    }

    @Test
    void listarRecibidos_conGrupoCerrados_consultaCanceladosEImpagos() {
        when(pedidoRepository.buscarRecibidos(
            eq(ARTISTA), any(), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(pedido)));
        ArgumentCaptor<Collection<EstadoPedido>> estados =
            ArgumentCaptor.forClass(Collection.class);

        pedidoService.listarRecibidos(
            ARTISTA, filtro(null, "cerrados"), 0, 20);

        verify(pedidoRepository).buscarRecibidos(
            eq(ARTISTA), estados.capture(), any(), any(),
            any(Pageable.class));
        assertThat(estados.getValue()).containsExactlyInAnyOrder(
            EstadoPedido.CANCELADO, EstadoPedido.IMPAGO,
            EstadoPedido.INCUMPLIMIENTO);
    }

    @Test
    void listarRecibidos_conOrdenLimite_ordenaPorFechaDeEntrega() {
        when(pedidoRepository.buscarRecibidos(
            eq(ARTISTA), any(), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(pedido)));
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);

        pedidoService.listarRecibidos(
            ARTISTA, filtro("limite", "en-curso"), 0, 20);

        verify(pedidoRepository).buscarRecibidos(
            eq(ARTISTA), any(), any(), any(), pageable.capture());
        Sort.Order orden = pageable.getValue().getSort()
            .getOrderFor("limiteEntrega");
        assertThat(orden).isNotNull();
        assertThat(orden.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void listarRecibidos_grupoAccion_soloDevuelvePedidosEntregables() {
        pedido.setEstado(EstadoPedido.EN_CURSO);
        hito.setEstadoPago(EstadoPagoHito.PAGADO);
        hito.setEstadoEntrega(EstadoEntregaHito.NO_INICIADO);
        when(pedidoRepository.candidatosAccionRecibidos(
            eq(ARTISTA), any(), any(), any(), any()))
            .thenReturn(List.of(pedido));

        PaginaDTO<PedidoResumenDTO> pagina = pedidoService.listarRecibidos(
            ARTISTA, filtro(null, "accion"), 0, 20);

        assertThat(pagina.getContenido()).hasSize(1);
        assertThat(pagina.getContenido().get(0).isPuedeEntregar()).isTrue();
    }

    @Test
    void listarEnviados_grupoAccion_soloDevuelvePedidosAAprobar() {
        pedido.setEstado(EstadoPedido.EN_CURSO);
        hito.setEstadoPago(EstadoPagoHito.PAGADO);
        hito.setEstadoEntrega(EstadoEntregaHito.ENTREGADO);
        when(pedidoRepository.candidatosAccionEnviados(
            eq(CLIENTE), any(), any(), any(), any(), any()))
            .thenReturn(List.of(pedido));

        PaginaDTO<PedidoResumenDTO> pagina = pedidoService.listarEnviados(
            CLIENTE, filtro(null, "accion"), 0, 20);

        assertThat(pagina.getContenido()).hasSize(1);
        assertThat(pagina.getContenido().get(0).isPuedeAprobar()).isTrue();
    }

    @Test
    void listarRecibidos_grupoAccion_paginaEnMemoria() {
        pedido.setEstado(EstadoPedido.EN_CURSO);
        hito.setEstadoPago(EstadoPagoHito.PAGADO);
        hito.setEstadoEntrega(EstadoEntregaHito.NO_INICIADO);
        when(pedidoRepository.candidatosAccionRecibidos(
            eq(ARTISTA), any(), any(), any(), any()))
            .thenReturn(List.of(
                pedido, pedido, pedido, pedido, pedido, pedido, pedido));

        PaginaDTO<PedidoResumenDTO> primera = pedidoService.listarRecibidos(
            ARTISTA, filtro(null, "accion"), 0, 5);
        PaginaDTO<PedidoResumenDTO> segunda = pedidoService.listarRecibidos(
            ARTISTA, filtro(null, "accion"), 1, 5);

        assertThat(primera.getContenido()).hasSize(5);
        assertThat(primera.getTotalElementos()).isEqualTo(7);
        assertThat(primera.isHayMas()).isTrue();
        assertThat(segunda.getContenido()).hasSize(2);
        assertThat(segunda.isHayMas()).isFalse();
    }

    private PedidoService.FiltroPedidos filtro(
            String orden, String grupo) {
        return new PedidoService.FiltroPedidos(
            null, grupo, null, null, orden);
    }

    @Test
    void obtener_comoAdmin_permiteVerYPermiteLaComision() {
        when(pedidoRepository.findById(PEDIDO)).thenReturn(Optional.of(pedido));

        PedidoDTO dto = pedidoService.obtener(PEDIDO, 99L, true);

        assertThat(dto.getRolVista()).isEqualTo("ADMIN");
        assertThat(dto.getComisionMonto()).isNotNull();
        assertThat(dto.getArancelMpPorcentaje())
            .isEqualByComparingTo("4.2229");
        assertThat(dto.getArancelMpEstimado()).isNotNull();
        assertThat(dto.getHitos()).isNotEmpty();
        assertThat(dto.getHitos().get(0).getArancelMpEstimado()).isNotNull();
    }

    @Test
    void obtener_otroUsuario_lanzaAccesoDenegado() {
        when(pedidoRepository.findById(PEDIDO)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.obtener(PEDIDO, 99L))
            .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void obtener_incluyeConversacionYNoLeidos() {
        Conversacion conversacion = conversacion(44L, EstadoConversacion.ACEPTADA);
        when(pedidoRepository.findById(PEDIDO)).thenReturn(Optional.of(pedido));
        when(conversacionRepository.buscarPorPar(CLIENTE, ARTISTA))
            .thenReturn(Optional.of(conversacion));
        when(mensajeRepository.contarNoLeidosPorConversacion(
            eq(CLIENTE), any(), eq(EstadoMensaje.ENVIADO)))
            .thenReturn(List.<Object[]>of(new Object[] {44L, 3L}));

        PedidoDTO dto = pedidoService.obtener(PEDIDO, CLIENTE);

        assertThat(dto.getIdConversacion()).isEqualTo(44L);
        assertThat(dto.getEstadoConversacion()).isEqualTo("ACEPTADA");
        assertThat(dto.getNoLeidos()).isEqualTo(3L);
    }

    @Test
    void obtener_sinConversacion_devuelveSinChatYSinNoLeidos() {
        when(pedidoRepository.findById(PEDIDO)).thenReturn(Optional.of(pedido));
        when(conversacionRepository.buscarPorPar(CLIENTE, ARTISTA))
            .thenReturn(Optional.empty());

        PedidoDTO dto = pedidoService.obtener(PEDIDO, CLIENTE);

        assertThat(dto.getIdConversacion()).isNull();
        assertThat(dto.getEstadoConversacion()).isNull();
        assertThat(dto.getNoLeidos()).isZero();
        verify(mensajeRepository, never())
            .contarNoLeidosPorConversacion(anyLong(), any(), any());
    }

    @Test
    void obtener_marcaLaReentregaAtrasada() {
        hito.setEstadoEntrega(EstadoEntregaHito.CON_REVISION);
        hito.setFechaLimiteReentrega(LocalDateTime.now().minusDays(1));
        when(pedidoRepository.findById(PEDIDO)).thenReturn(Optional.of(pedido));
        when(conversacionRepository.buscarPorPar(CLIENTE, ARTISTA))
            .thenReturn(Optional.empty());

        PedidoDTO dto = pedidoService.obtener(PEDIDO, CLIENTE);

        assertThat(dto.getHitos().get(0).isReentregaAtrasada()).isTrue();
    }

    @Test
    void obtener_comoAdmin_noResuelveConversacion() {
        when(pedidoRepository.findById(PEDIDO)).thenReturn(Optional.of(pedido));

        PedidoDTO dto = pedidoService.obtener(PEDIDO, 99L, true);

        assertThat(dto.getIdConversacion()).isNull();
        assertThat(dto.getNoLeidos()).isZero();
        verify(conversacionRepository, never()).buscarPorPar(anyLong(), anyLong());
    }

    @Test
    void listarEnviados_incluyeConversacionYNoLeidos() {
        Conversacion conversacion = conversacion(44L, EstadoConversacion.ACEPTADA);
        when(pedidoRepository.buscarEnviados(
            eq(CLIENTE), any(), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(pedido)));
        when(conversacionRepository.buscarPorPar(CLIENTE, ARTISTA))
            .thenReturn(Optional.of(conversacion));
        when(mensajeRepository.contarNoLeidosPorConversacion(
            eq(CLIENTE), any(), eq(EstadoMensaje.ENVIADO)))
            .thenReturn(List.<Object[]>of(new Object[] {44L, 2L}));

        PaginaDTO<PedidoResumenDTO> pagina =
            pedidoService.listarEnviados(CLIENTE, filtro(null, null), 0, 12);

        PedidoResumenDTO resumen = pagina.getContenido().get(0);
        assertThat(resumen.getIdConversacion()).isEqualTo(44L);
        assertThat(resumen.getEstadoConversacion()).isEqualTo("ACEPTADA");
        assertThat(resumen.getNoLeidos()).isEqualTo(2L);
    }

    private Conversacion conversacion(Long id, EstadoConversacion estado) {
        Conversacion conversacion = new Conversacion();
        conversacion.setIdConversacion(id);
        conversacion.setEstado(estado);
        return conversacion;
    }

    private void prepararCreacion() {
        when(solicitudRepository.findByIdSolicitudForUpdate(
            solicitud.getIdSolicitud())).thenReturn(Optional.of(solicitud));
        when(pedidoRepository.findBySolicitudIdSolicitud(
            solicitud.getIdSolicitud())).thenReturn(Optional.empty());

        AceptacionTerminosPlataforma aceptacion = new AceptacionTerminosPlataforma();
        aceptacion.setIdAceptacion(1L);
        aceptacion.setVersion(4);
        aceptacion.setFechaAceptacion(LocalDateTime.now());
        when(terminosPlataformaService.obtenerAceptacionVigente(anyLong()))
            .thenReturn(aceptacion);
    }

    private void bloquearPedidoYHito() {
        when(pedidoRepository.findByIdPedidoForUpdate(PEDIDO))
            .thenReturn(Optional.of(pedido));
        when(hitoPedidoRepository.findByIdHitoPedidoForUpdate(HITO))
            .thenReturn(Optional.of(hito));
    }

    private Usuario usuario(Long id, String nombre) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(id);
        usuario.setNombreUsuario(nombre);
        usuario.setNombreDisplay(nombre);
        return usuario;
    }
}
