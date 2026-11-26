package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.AccionSolicitud;
import com.temipora.backend_springboot.modelo.ArancelEstimadoDTO;
import com.temipora.backend_springboot.modelo.Conversacion;
import com.temipora.backend_springboot.modelo.DerechoTermino;
import com.temipora.backend_springboot.modelo.DisponibilidadServicioDTO;
import com.temipora.backend_springboot.modelo.EstadoConversacion;
import com.temipora.backend_springboot.modelo.EstadoServicio;
import com.temipora.backend_springboot.modelo.EstadoSolicitud;
import com.temipora.backend_springboot.modelo.Formulario;
import com.temipora.backend_springboot.modelo.FormularioDTO;
import com.temipora.backend_springboot.modelo.ItemSolicitudDerecho;
import com.temipora.backend_springboot.modelo.ItemSolicitudRequest;
import com.temipora.backend_springboot.modelo.ListaEspera;
import com.temipora.backend_springboot.modelo.ModoRecepcionSolicitud;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.Pregunta;
import com.temipora.backend_springboot.modelo.PreguntaDTO;
import com.temipora.backend_springboot.modelo.RespuestaSolicitudRequest;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.ServicioDTO;
import com.temipora.backend_springboot.modelo.Solicitud;
import com.temipora.backend_springboot.modelo.SolicitudDTO;
import com.temipora.backend_springboot.modelo.SolicitudResumenDTO;
import com.temipora.backend_springboot.modelo.SolicitudServicioRequest;
import com.temipora.backend_springboot.modelo.TerminoDerecho;
import com.temipora.backend_springboot.modelo.Terminos;
import com.temipora.backend_springboot.modelo.TerminosDTO;
import com.temipora.backend_springboot.modelo.TipoAccionSolicitud;
import com.temipora.backend_springboot.modelo.TipoCampo;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.AccionSolicitudRepository;
import com.temipora.backend_springboot.repositorios.ConversacionRepository;
import com.temipora.backend_springboot.repositorios.FormularioRepository;
import com.temipora.backend_springboot.repositorios.ServicioRepository;
import com.temipora.backend_springboot.repositorios.SolicitudRepository;
import com.temipora.backend_springboot.repositorios.TerminosRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;
import com.temipora.backend_springboot.servicios.pagos.ArancelEstimadoService;

@ExtendWith(MockitoExtension.class)
class SolicitudServicioServiceTest {

    private static final Long CLIENTE = 1L;
    private static final Long ARTISTA = 2L;
    private static final Long SERVICIO = 7L;
    private static final Long TERMINOS = 11L;
    private static final Long FORMULARIO = 21L;
    private static final Long PREGUNTA = 31L;

    @Mock
    private ServicioRepository servicioRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private FormularioRepository formularioRepository;

    @Mock
    private TerminosRepository terminosRepository;

    @Mock
    private SolicitudRepository solicitudRepository;

    @Mock
    private AccionSolicitudRepository accionSolicitudRepository;

    @Mock
    private ServicioService servicioService;

    @Mock
    private FormularioService formularioService;

    @Mock
    private TerminosService terminosService;

    @Mock
    private ListaEsperaService listaEsperaService;

    @Mock
    private NotificacionService notificacionService;

    @Mock
    private ColaSolicitudesService colaSolicitudesService;

    @Mock
    private MonedaVistaService monedaVistaService;

    @Mock
    private ReseniaPublicaService reseniaPublicaService;

    @Mock
    private ConversacionService conversacionService;

    @Mock
    private ConversacionRepository conversacionRepository;

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

    @InjectMocks
    private SolicitudServicioService solicitudServicioService;

    private final AtomicLong idsSolicitud = new AtomicLong();

    private Usuario cliente;
    private Usuario artista;
    private Servicio servicio;
    private Terminos terminos;
    private TerminosDTO terminosDTO;
    private Formulario formulario;
    private FormularioDTO formularioDTO;

    @BeforeEach
    void setUp() {
        cliente = new Usuario();
        cliente.setIdUsuario(CLIENTE);
        cliente.setNombreUsuario("cliente");

        artista = new Usuario();
        artista.setIdUsuario(ARTISTA);
        artista.setNombreUsuario("artista");

        servicio = new Servicio();
        servicio.setIdServicio(SERVICIO);
        servicio.setNombre("Ilustración");
        servicio.setEstado(EstadoServicio.PUBLICADO);
        servicio.setArtista(artista);
        servicio.setCupos(3);
        servicio.setModoRecepcion(ModoRecepcionSolicitud.ABIERTA);
        servicio.setMaxDibujosPorSolicitud(3);
        servicio.setPermiteListaEspera(true);
        servicio.setPrecioBase(new BigDecimal("100.00"));
        servicio.setMoneda(moneda());

        terminos = new Terminos();
        terminos.setIdTerminos(TERMINOS);
        terminos.setVersion(1);
        terminosDTO = TerminosDTO.builder()
            .idTerminos(TERMINOS)
            .idServicio(SERVICIO)
            .version(1)
            .informacionGeneral("Términos")
            .derechos(List.of())
            .build();

        formulario = new Formulario();
        formulario.setIdFormulario(FORMULARIO);
        formulario.setIdServicio(SERVICIO);
        Pregunta pregunta = new Pregunta();
        pregunta.setIdPregunta(PREGUNTA);
        pregunta.setTipoCampo(tipoCampo());
        pregunta.setEnunciado("¿Qué querés?");
        pregunta.setObligatoria(true);
        pregunta.setOrden(0);
        formulario.getPreguntas().add(pregunta);
        formularioDTO = FormularioDTO.builder()
            .idFormulario(FORMULARIO)
            .idServicio(SERVICIO)
            .preguntas(List.of(PreguntaDTO.builder()
                .idPregunta(PREGUNTA)
                .idTipoCampo(41L)
                .codigoTipo("TEXTO")
                .enunciado("¿Qué querés?")
                .obligatoria(true)
                .opciones(List.of())
                .build()))
            .build();

        lenient().when(colaSolicitudesService.evaluar(any(Solicitud.class)))
            .thenReturn(new ColaSolicitudesService.EvaluacionPresupuesto(
                false, null, null));
        lenient().when(arancelEstimadoService.estimar())
            .thenReturn(new ArancelEstimadoDTO(
                new BigDecimal("3.49"),
                new BigDecimal("21"),
                new BigDecimal("4.2229")));
    }

    @Test
    void disponibilidad_conSolicitudActiva_bloqueaNuevaSolicitud() {
        when(servicioService.obtener(SERVICIO, CLIENTE))
            .thenReturn(ServicioDTO.builder().idServicio(SERVICIO).build());
        when(servicioRepository.findByIdServicio(SERVICIO)).thenReturn(Optional.of(servicio));
        when(colaSolicitudesService.cuposOcupados(SERVICIO)).thenReturn(2);
        lenient().when(solicitudRepository.existsByServicioIdServicioAndClienteIdUsuarioAndEstadoIn(
            any(), any(), any())).thenReturn(true);
        lenient().when(solicitudRepository.existsByServicioIdServicioAndClienteIdUsuarioAndEstado(
            any(), any(), any())).thenReturn(false);

        DisponibilidadServicioDTO dto = solicitudServicioService.disponibilidad(SERVICIO, CLIENTE);

        assertThat(dto.getCuposTotales()).isEqualTo(3);
        assertThat(dto.getCuposOcupados()).isEqualTo(2);
        assertThat(dto.getCuposDisponibles()).isEqualTo(1);
        assertThat(dto.getModoRecepcion()).isEqualTo("ABIERTA");
        assertThat(dto.getMaxDibujosPorSolicitud()).isEqualTo(3);
        assertThat(dto.isTieneSolicitudActiva()).isTrue();
        assertThat(dto.isPuedeSolicitarCupo()).isFalse();
        assertThat(dto.isPuedeListaEspera()).isFalse();
        assertThat(dto.isPuedeSolicitar()).isFalse();
        assertThat(dto.getMensaje()).contains("solicitud activa");
    }

    @Test
    void disponibilidad_soloEspera_permiteListaSinCupo() {
        servicio.setModoRecepcion(ModoRecepcionSolicitud.SOLO_ESPERA);
        when(servicioService.obtener(SERVICIO, CLIENTE))
            .thenReturn(ServicioDTO.builder().idServicio(SERVICIO).build());
        when(servicioRepository.findByIdServicio(SERVICIO)).thenReturn(Optional.of(servicio));
        when(colaSolicitudesService.cuposOcupados(SERVICIO)).thenReturn(3);
        lenient().when(solicitudRepository.existsByServicioIdServicioAndClienteIdUsuarioAndEstadoIn(
            any(), any(), any())).thenReturn(false);
        lenient().when(solicitudRepository.existsByServicioIdServicioAndClienteIdUsuarioAndEstado(
            any(), any(), any())).thenReturn(false);

        DisponibilidadServicioDTO dto = solicitudServicioService.disponibilidad(SERVICIO, CLIENTE);

        assertThat(dto.getCuposDisponibles()).isZero();
        assertThat(dto.getModoRecepcion()).isEqualTo("SOLO_ESPERA");
        assertThat(dto.isPermiteListaEspera()).isTrue();
        assertThat(dto.isPuedeSolicitarCupo()).isFalse();
        assertThat(dto.isPuedeListaEspera()).isTrue();
        assertThat(dto.isPuedeSolicitar()).isTrue();
        assertThat(dto.getMensaje()).contains("lista de espera");
    }

    @Test
    void crear_conDibujosSuficientes_creaUnaSolaSolicitudPendiente() {
        prepararCreacion();

        SolicitudDTO resultado = solicitudServicioService.crear(CLIENTE, request(3, false));

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(resultado.getCantidadDibujos()).isEqualTo(3);
        assertThat(resultado.getItems()).hasSize(3);
        assertThat(resultado.getIdTerminos()).isEqualTo(TERMINOS);
        assertThat(resultado.getTerminosVersion()).isEqualTo(1);
        assertThat(resultado.getTerminosAceptadosEn()).isNotNull();

        verify(listaEsperaService, never()).crear(any(), any(), any(), anyInt());
        verify(notificacionService).crearParaUsuario(
            eq(ARTISTA), anyString(), anyString(), anyString(), anyLong(), eq("SOLICITUD"));
        verify(conversacionService).asegurarPorSolicitud(
            cliente, artista, resultado.getIdSolicitud(), "Ilustración");
    }

    @Test
    void crear_sinCuposLibres_creaPendienteSinPartirLaSolicitud() {
        prepararCreacion();

        SolicitudDTO resultado = solicitudServicioService.crear(CLIENTE, request(2, false));

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(resultado.getCantidadDibujos()).isEqualTo(2);
        verify(listaEsperaService, never()).crear(any(), any(), any(), anyInt());
    }

    @Test
    void crear_modoSoloEspera_creaUnaSolaSolicitudEnEspera() {
        servicio.setModoRecepcion(ModoRecepcionSolicitud.SOLO_ESPERA);
        prepararCreacion();
        when(listaEsperaService.crear(any(Solicitud.class), any(Servicio.class), any(Usuario.class), eq(3)))
            .thenReturn(listaEspera(1));

        SolicitudDTO resultado = solicitudServicioService.crear(CLIENTE, request(3, false));

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.EN_LISTA_ESPERA);
        assertThat(resultado.getCantidadDibujos()).isEqualTo(3);
        assertThat(resultado.getPosicionListaEspera()).isEqualTo(1);

        verify(listaEsperaService).crear(any(), any(), any(), eq(3));
        verify(notificacionService).crearParaUsuario(
            eq(ARTISTA), anyString(), anyString(), anyString(), anyLong(), eq("SOLICITUD"));
    }

    @Test
    void crear_modoSoloEsperaSinAceptarLista_lanzaError() {
        servicio.setModoRecepcion(ModoRecepcionSolicitud.SOLO_ESPERA);
        prepararCreacion();
        SolicitudServicioRequest request = request(1, false);
        request.setAceptaListaEspera(false);

        assertThatThrownBy(() -> solicitudServicioService.crear(CLIENTE, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Aceptá la lista de espera");
    }

    @Test
    void crear_conPrioridadMantieneElOrden() {
        prepararCreacion();

        SolicitudServicioRequest request = request(3, true);
        request.getItems().get(0).setPrioridad(2);
        request.getItems().get(1).setPrioridad(1);
        request.getItems().get(2).setPrioridad(3);

        solicitudServicioService.crear(CLIENTE, request);

        ArgumentCaptor<Solicitud> captor = ArgumentCaptor.forClass(Solicitud.class);
        verify(solicitudRepository).save(captor.capture());
        Solicitud solicitud = captor.getValue();

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(solicitud.getItems()).hasSize(3);
        assertThat(solicitud.getItems().get(0).getNombre()).isEqualTo("Dibujo 2");
        assertThat(solicitud.getItems().get(0).getOrdenPrioridad()).isEqualTo(1);
        assertThat(solicitud.getItems().get(1).getNombre()).isEqualTo("Dibujo 1");
        assertThat(solicitud.getItems().get(2).getNombre()).isEqualTo("Dibujo 3");
    }

    @Test
    void crear_excedeElMaximoDeDibujos_lanzaError() {
        servicio.setMaxDibujosPorSolicitud(2);
        prepararCreacion();

        assertThatThrownBy(() -> solicitudServicioService.crear(CLIENTE, request(3, false)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("hasta 2 dibujo");

        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void crear_modoCerrado_lanzaError() {
        servicio.setModoRecepcion(ModoRecepcionSolicitud.CERRADA);
        prepararCreacion();

        assertThatThrownBy(() -> solicitudServicioService.crear(CLIENTE, request(1, false)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no está recibiendo solicitudes");

        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void crear_terminosDesactualizados_lanzaConflicto() {
        lenient().when(usuarioRepository.findByIdUsuario(CLIENTE)).thenReturn(Optional.of(cliente));
        lenient().when(servicioRepository.findByIdServicioForUpdate(SERVICIO))
            .thenReturn(Optional.of(servicio));
        lenient().when(terminosService.obtenerServicio(SERVICIO, ARTISTA)).thenReturn(terminosDTO);

        SolicitudServicioRequest request = request(1, false);
        request.setTerminosVersion(2);

        assertThatThrownBy(() -> solicitudServicioService.crear(CLIENTE, request))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("Los términos del servicio cambiaron");
    }

    @Test
    void crear_conDerechos_guardaSnapshotEnCadaItem() {
        prepararCreacion();
        configurarDerechos();
        SolicitudServicioRequest request = request(1, false);
        request.getItems().get(0).setDerechos(List.of(101L, 102L));

        solicitudServicioService.crear(CLIENTE, request);

        ArgumentCaptor<Solicitud> captor = ArgumentCaptor.forClass(Solicitud.class);
        verify(solicitudRepository).save(captor.capture());
        List<ItemSolicitudDerecho> derechos =
            captor.getValue().getItems().get(0).getDerechos();

        assertThat(derechos).hasSize(2);
        assertThat(derechos.get(0).getCodigo()).isEqualTo("CONTENIDO");
        assertThat(derechos.get(0).getNombre())
            .isEqualTo("Uso comercial en producción de contenido");
        assertThat(derechos.get(0).getPorcentajeExtra())
            .isEqualByComparingTo("30.00");
        assertThat(derechos.get(1).getCodigo()).isEqualTo("MERCHANDISING");
        assertThat(derechos.get(1).getPorcentajeExtra())
            .isEqualByComparingTo("20.00");
    }

    @Test
    void crear_sinDerechos_dejaListaVacia() {
        prepararCreacion();

        solicitudServicioService.crear(CLIENTE, request(1, false));

        ArgumentCaptor<Solicitud> captor = ArgumentCaptor.forClass(Solicitud.class);
        verify(solicitudRepository).save(captor.capture());
        assertThat(captor.getValue().getItems().get(0).getDerechos()).isEmpty();
    }

    @Test
    void crear_derechoNoDisponible_lanzaError() {
        prepararCreacion();
        terminos.reemplazarDerechos(List.of(derechoTermino(
            101L, "CONTENIDO", "Contenido", "30", false, false)));
        SolicitudServicioRequest request = request(1, false);
        request.getItems().get(0).setDerechos(List.of(101L));

        assertThatThrownBy(() -> solicitudServicioService.crear(CLIENTE, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no está disponible");
    }

    @Test
    void crear_derechoPersonal_lanzaError() {
        prepararCreacion();
        configurarDerechos();
        SolicitudServicioRequest request = request(1, false);
        request.getItems().get(0).setDerechos(List.of(103L));

        assertThatThrownBy(() -> solicitudServicioService.crear(CLIENTE, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("uso personal");
    }

    @Test
    void crear_derechoPiezaUnica_lanzaError() {
        prepararCreacion();
        configurarDerechos();
        SolicitudServicioRequest request = request(1, false);
        request.getItems().get(0).setDerechos(List.of(104L));

        assertThatThrownBy(() -> solicitudServicioService.crear(CLIENTE, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no está disponible para comisiones");
    }

    @Test
    void crear_preguntaObligatoriaSinResponder_lanzaError() {
        prepararCreacion();
        SolicitudServicioRequest request = request(1, false);
        request.getItems().get(0).getRespuestas().get(0).setValores(List.of(" "));

        assertThatThrownBy(() -> solicitudServicioService.crear(CLIENTE, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("es obligatoria");
    }

    @Test
    void crear_solicitudActivaExistente_lanzaError() {
        prepararCreacion();
        lenient().when(solicitudRepository.existsByServicioIdServicioAndClienteIdUsuarioAndEstadoIn(
            eq(SERVICIO), eq(CLIENTE), any())).thenReturn(true);

        assertThatThrownBy(() -> solicitudServicioService.crear(CLIENTE, request(1, false)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Ya tenés una solicitud activa");
    }

    @Test
    void listarRecibidas_sinEstado_devuelveLasDelArtista() {
        Solicitud presupuestada = solicitudResumen(EstadoSolicitud.PRESUPUESTADA, 1);
        Solicitud pendiente = solicitudResumen(EstadoSolicitud.PENDIENTE, 5);
        when(solicitudRepository.findByArtistaIdUsuarioOrderByFechaCreacionDesc(ARTISTA))
            .thenReturn(List.of(presupuestada, pendiente));

        List<SolicitudResumenDTO> resultado = solicitudServicioService.listarRecibidas(ARTISTA, null);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getEstado()).isEqualTo(EstadoSolicitud.PRESUPUESTADA);
        assertThat(resultado.get(1).getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(resultado.get(0).getNombreServicio()).isEqualTo("Ilustración");
        assertThat(resultado.get(0).getCliente().getNombreUsuario()).isEqualTo("cliente");
        assertThat(resultado.get(0).getArtista().getIdUsuario()).isEqualTo(ARTISTA);

        verify(solicitudRepository, never())
            .findByArtistaIdUsuarioAndEstadoOrderByFechaCreacionDesc(any(), any());
    }

    @Test
    void listarRecibidas_conEstado_filtraPorEstado() {
        Solicitud pendiente = solicitudResumen(EstadoSolicitud.PENDIENTE, 1);
        when(solicitudRepository.findByArtistaIdUsuarioAndEstadoOrderByFechaCreacionDesc(
            ARTISTA, EstadoSolicitud.PENDIENTE)).thenReturn(List.of(pendiente));

        List<SolicitudResumenDTO> resultado = solicitudServicioService.listarRecibidas(
            ARTISTA, EstadoSolicitud.PENDIENTE);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);

        verify(solicitudRepository, never())
            .findByArtistaIdUsuarioOrderByFechaCreacionDesc(any());
    }

    @Test
    void listarEnviadas_sinEstado_devuelveLasDelCliente() {
        Solicitud rechazada = solicitudResumen(EstadoSolicitud.RECHAZADA, 1);
        when(solicitudRepository.findByClienteIdUsuarioOrderByFechaCreacionDesc(CLIENTE))
            .thenReturn(List.of(rechazada));

        List<SolicitudResumenDTO> resultado = solicitudServicioService.listarEnviadas(CLIENTE, null);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getEstado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        assertThat(resultado.get(0).getCantidadDibujos()).isEqualTo(2);

        verify(solicitudRepository, never())
            .findByClienteIdUsuarioAndEstadoOrderByFechaCreacionDesc(any(), any());
    }

    @Test
    void rechazar_pendiente_registraAccionYNotificaAlCliente() {
        Solicitud solicitud = solicitudResumen(EstadoSolicitud.PENDIENTE, 1);
        when(solicitudRepository.findByIdSolicitudForUpdate(solicitud.getIdSolicitud()))
            .thenReturn(Optional.of(solicitud));
        when(accionSolicitudRepository.save(any(AccionSolicitud.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        SolicitudDTO resultado = solicitudServicioService.rechazar(
            solicitud.getIdSolicitud(), ARTISTA, "  No llego con los tiempos  ");

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        assertThat(resultado.getMotivoRechazo()).isEqualTo("No llego con los tiempos");
        assertThat(resultado.getFechaRechazo()).isNotNull();

        ArgumentCaptor<AccionSolicitud> captor = ArgumentCaptor.forClass(AccionSolicitud.class);
        verify(accionSolicitudRepository).save(captor.capture());
        AccionSolicitud accion = captor.getValue();
        assertThat(accion.getTipoAccion()).isEqualTo(TipoAccionSolicitud.RECHAZO);
        assertThat(accion.getSolicitud()).isSameAs(solicitud);
        assertThat(accion.getUsuario()).isSameAs(artista);
        assertThat(accion.getMotivo()).isEqualTo("No llego con los tiempos");

        verify(notificacionService).crearParaUsuario(
            eq(CLIENTE), eq("SOLICITUD_RECHAZADA"), eq("Solicitud rechazada"),
            contains("No llego con los tiempos"), eq(solicitud.getIdSolicitud()), eq("SOLICITUD"));
        verify(listaEsperaService, never()).marcarRechazada(any());
        verify(conversacionService).archivarPorSolicitud(cliente, artista);
    }

    @Test
    void rechazar_sinMotivo_usaMensajePorDefecto() {
        Solicitud solicitud = solicitudResumen(EstadoSolicitud.PENDIENTE, 1);
        when(solicitudRepository.findByIdSolicitudForUpdate(solicitud.getIdSolicitud()))
            .thenReturn(Optional.of(solicitud));
        when(accionSolicitudRepository.save(any(AccionSolicitud.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        SolicitudDTO resultado = solicitudServicioService.rechazar(
            solicitud.getIdSolicitud(), ARTISTA, "   ");

        assertThat(resultado.getMotivoRechazo()).isNull();

        ArgumentCaptor<AccionSolicitud> captor = ArgumentCaptor.forClass(AccionSolicitud.class);
        verify(accionSolicitudRepository).save(captor.capture());
        assertThat(captor.getValue().getMotivo()).isNull();

        verify(notificacionService).crearParaUsuario(
            eq(CLIENTE), eq("SOLICITUD_RECHAZADA"), anyString(),
            contains("no mencionar el motivo"), eq(solicitud.getIdSolicitud()), eq("SOLICITUD"));
    }

    @Test
    void rechazar_otroUsuario_lanzaAccesoDenegado() {
        Solicitud solicitud = solicitudResumen(EstadoSolicitud.PENDIENTE, 1);
        when(solicitudRepository.findByIdSolicitudForUpdate(solicitud.getIdSolicitud()))
            .thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> solicitudServicioService.rechazar(
            solicitud.getIdSolicitud(), CLIENTE, null))
            .isInstanceOf(AccesoDenegadoException.class)
            .hasMessageContaining("No podés rechazar");

        verify(accionSolicitudRepository, never()).save(any());
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
    }

    @Test
    void rechazar_presupuestada_lanzaConflicto() {
        Solicitud solicitud = solicitudResumen(EstadoSolicitud.PRESUPUESTADA, 1);
        when(solicitudRepository.findByIdSolicitudForUpdate(solicitud.getIdSolicitud()))
            .thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> solicitudServicioService.rechazar(
            solicitud.getIdSolicitud(), ARTISTA, null))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("estado PRESUPUESTADA");

        verify(accionSolicitudRepository, never()).save(any());
    }

    @Test
    void rechazar_yaRechazada_lanzaConflicto() {
        Solicitud solicitud = solicitudResumen(EstadoSolicitud.RECHAZADA, 1);
        when(solicitudRepository.findByIdSolicitudForUpdate(solicitud.getIdSolicitud()))
            .thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> solicitudServicioService.rechazar(
            solicitud.getIdSolicitud(), ARTISTA, null))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("ya fue rechazada");

        verify(accionSolicitudRepository, never()).save(any());
    }

    @Test
    void rechazar_enListaDeEspera_marcaLaLista() {
        Solicitud solicitud = solicitudResumen(EstadoSolicitud.EN_LISTA_ESPERA, 1);
        when(solicitudRepository.findByIdSolicitudForUpdate(solicitud.getIdSolicitud()))
            .thenReturn(Optional.of(solicitud));
        when(accionSolicitudRepository.save(any(AccionSolicitud.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        solicitudServicioService.rechazar(solicitud.getIdSolicitud(), ARTISTA, null);

        verify(listaEsperaService).marcarRechazada(solicitud);
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.RECHAZADA);
    }

    @Test
    void obtenerSolicitud_incluyeElMotivoDeRechazo() {
        Solicitud solicitud = solicitudResumen(EstadoSolicitud.RECHAZADA, 1);
        AccionSolicitud accion = new AccionSolicitud();
        accion.setMotivo("No disponible");
        accion.setFecha(LocalDateTime.now());
        when(solicitudRepository.findByIdSolicitud(solicitud.getIdSolicitud()))
            .thenReturn(Optional.of(solicitud));
        when(accionSolicitudRepository
            .findFirstBySolicitudIdSolicitudAndTipoAccionOrderByFechaDesc(
                solicitud.getIdSolicitud(), TipoAccionSolicitud.RECHAZO))
            .thenReturn(Optional.of(accion));

        SolicitudDTO resultado = solicitudServicioService.obtenerSolicitud(
            solicitud.getIdSolicitud(), CLIENTE);

        assertThat(resultado.getMotivoRechazo()).isEqualTo("No disponible");
        assertThat(resultado.getFechaRechazo()).isNotNull();
    }

    @Test
    void obtenerSolicitud_deOtroUsuario_lanzaAccesoDenegado() {
        Solicitud solicitud = solicitudResumen(EstadoSolicitud.PENDIENTE, 1);
        when(solicitudRepository.findByIdSolicitud(solicitud.getIdSolicitud()))
            .thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> solicitudServicioService.obtenerSolicitud(
            solicitud.getIdSolicitud(), 99L))
            .isInstanceOf(AccesoDenegadoException.class)
            .hasMessageContaining("No podés ver");
    }

    @Test
    void retirarPresupuesto_presupuestadaLaRechazaYNotificaAlCliente() {
        Solicitud solicitud = solicitudResumen(EstadoSolicitud.PRESUPUESTADA, 1);
        when(solicitudRepository.findByIdSolicitudForUpdate(solicitud.getIdSolicitud()))
            .thenReturn(Optional.of(solicitud));
        when(accionSolicitudRepository.save(any(AccionSolicitud.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        SolicitudDTO resultado = solicitudServicioService.retirarPresupuesto(
            solicitud.getIdSolicitud(), ARTISTA);

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.RECHAZADA);

        ArgumentCaptor<AccionSolicitud> captor = ArgumentCaptor.forClass(AccionSolicitud.class);
        verify(accionSolicitudRepository).save(captor.capture());
        assertThat(captor.getValue().getTipoAccion())
            .isEqualTo(TipoAccionSolicitud.RETIRO_PRESUPUESTO);
        assertThat(captor.getValue().getOrigen()).isEqualTo("ARTISTA");

        verify(notificacionService).crearParaUsuario(
            eq(CLIENTE), eq("SOLICITUD_RECHAZADA"), eq("Presupuesto retirado"),
            contains("retiró el presupuesto"), eq(solicitud.getIdSolicitud()), eq("SOLICITUD"));
    }

    @Test
    void retirarPresupuesto_sinPresupuesto_lanzaConflicto() {
        Solicitud solicitud = solicitudResumen(EstadoSolicitud.PENDIENTE, 1);
        when(solicitudRepository.findByIdSolicitudForUpdate(solicitud.getIdSolicitud()))
            .thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> solicitudServicioService.retirarPresupuesto(
            solicitud.getIdSolicitud(), ARTISTA))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("presupuestada");

        verify(accionSolicitudRepository, never()).save(any());
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
    }

    @Test
    void cancelar_pendiente_registraAccionYNotificaAlArtista() {
        Solicitud solicitud = solicitudResumen(EstadoSolicitud.PENDIENTE, 1);
        when(solicitudRepository.findByIdSolicitudForUpdate(solicitud.getIdSolicitud()))
            .thenReturn(Optional.of(solicitud));
        when(accionSolicitudRepository.save(any(AccionSolicitud.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        SolicitudDTO resultado = solicitudServicioService.cancelar(
            solicitud.getIdSolicitud(), CLIENTE, "  Cambié de idea  ");

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.CANCELADA);
        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.CANCELADA);
        assertThat(resultado.getMotivoCancelacion()).isEqualTo("Cambié de idea");
        assertThat(resultado.getFechaCancelacion()).isNotNull();
        assertThat(resultado.isPuedeCancelar()).isFalse();

        ArgumentCaptor<AccionSolicitud> captor = ArgumentCaptor.forClass(AccionSolicitud.class);
        verify(accionSolicitudRepository).save(captor.capture());
        AccionSolicitud accion = captor.getValue();
        assertThat(accion.getTipoAccion()).isEqualTo(TipoAccionSolicitud.CANCELACION);
        assertThat(accion.getSolicitud()).isSameAs(solicitud);
        assertThat(accion.getUsuario()).isSameAs(cliente);
        assertThat(accion.getMotivo()).isEqualTo("Cambié de idea");
        assertThat(accion.getOrigen()).isEqualTo("CLIENTE");

        verify(notificacionService).crearParaUsuario(
            eq(ARTISTA), eq("SOLICITUD_CANCELADA"), eq("Solicitud cancelada"),
            contains("Motivo: Cambié de idea"), eq(solicitud.getIdSolicitud()), eq("SOLICITUD"));
        verify(listaEsperaService, never()).marcarCancelada(any());
    }

    @Test
    void cancelar_sinMotivo_usaMensajePorDefecto() {
        Solicitud solicitud = solicitudResumen(EstadoSolicitud.PENDIENTE, 1);
        when(solicitudRepository.findByIdSolicitudForUpdate(solicitud.getIdSolicitud()))
            .thenReturn(Optional.of(solicitud));
        when(accionSolicitudRepository.save(any(AccionSolicitud.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        SolicitudDTO resultado = solicitudServicioService.cancelar(
            solicitud.getIdSolicitud(), CLIENTE, "   ");

        assertThat(resultado.getMotivoCancelacion()).isNull();

        verify(notificacionService).crearParaUsuario(
            eq(ARTISTA), eq("SOLICITUD_CANCELADA"), eq("Solicitud cancelada"),
            contains("no mencionar el motivo"), eq(solicitud.getIdSolicitud()), eq("SOLICITUD"));
    }

    @Test
    void cancelar_enListaDeEspera_marcaLaLista() {
        Solicitud solicitud = solicitudResumen(EstadoSolicitud.EN_LISTA_ESPERA, 1);
        when(solicitudRepository.findByIdSolicitudForUpdate(solicitud.getIdSolicitud()))
            .thenReturn(Optional.of(solicitud));
        when(accionSolicitudRepository.save(any(AccionSolicitud.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        solicitudServicioService.cancelar(solicitud.getIdSolicitud(), CLIENTE, null);

        verify(listaEsperaService).marcarCancelada(solicitud);
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.CANCELADA);
    }

    @Test
    void cancelar_presupuestada_avisaQueSeLiberoElCupo() {
        Solicitud solicitud = solicitudResumen(EstadoSolicitud.PRESUPUESTADA, 1);
        when(solicitudRepository.findByIdSolicitudForUpdate(solicitud.getIdSolicitud()))
            .thenReturn(Optional.of(solicitud));
        when(accionSolicitudRepository.save(any(AccionSolicitud.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        SolicitudDTO resultado = solicitudServicioService.cancelar(
            solicitud.getIdSolicitud(), CLIENTE, null);

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.CANCELADA);
        verify(notificacionService).crearParaUsuario(
            eq(ARTISTA), eq("SOLICITUD_CANCELADA"), eq("Solicitud cancelada"),
            contains("cupo reservado quedó liberado"),
            eq(solicitud.getIdSolicitud()), eq("SOLICITUD"));
    }

    @Test
    void cancelar_deOtroCliente_lanzaAccesoDenegado() {
        Solicitud solicitud = solicitudResumen(EstadoSolicitud.PENDIENTE, 1);
        when(solicitudRepository.findByIdSolicitudForUpdate(solicitud.getIdSolicitud()))
            .thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> solicitudServicioService.cancelar(
            solicitud.getIdSolicitud(), 99L, null))
            .isInstanceOf(AccesoDenegadoException.class)
            .hasMessageContaining("No podés cancelar");

        verify(accionSolicitudRepository, never()).save(any());
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
    }

    @Test
    void cancelar_artistaDeLaSolicitud_lanzaAccesoDenegado() {
        Solicitud solicitud = solicitudResumen(EstadoSolicitud.PENDIENTE, 1);
        when(solicitudRepository.findByIdSolicitudForUpdate(solicitud.getIdSolicitud()))
            .thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> solicitudServicioService.cancelar(
            solicitud.getIdSolicitud(), ARTISTA, null))
            .isInstanceOf(AccesoDenegadoException.class)
            .hasMessageContaining("No podés cancelar");

        verify(accionSolicitudRepository, never()).save(any());
    }

    @Test
    void cancelar_yaCancelada_lanzaConflicto() {
        Solicitud solicitud = solicitudResumen(EstadoSolicitud.CANCELADA, 1);
        when(solicitudRepository.findByIdSolicitudForUpdate(solicitud.getIdSolicitud()))
            .thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> solicitudServicioService.cancelar(
            solicitud.getIdSolicitud(), CLIENTE, null))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("ya fue cancelada");

        verify(accionSolicitudRepository, never()).save(any());
    }

    @Test
    void cancelar_rechazada_lanzaConflicto() {
        Solicitud solicitud = solicitudResumen(EstadoSolicitud.RECHAZADA, 1);
        when(solicitudRepository.findByIdSolicitudForUpdate(solicitud.getIdSolicitud()))
            .thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> solicitudServicioService.cancelar(
            solicitud.getIdSolicitud(), CLIENTE, null))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("estado RECHAZADA");

        verify(accionSolicitudRepository, never()).save(any());
    }

    @Test
    void obtenerSolicitud_clienteIncluyePuedeCancelarYLosDatosDeCancelacion() {
        Solicitud solicitud = solicitudResumen(EstadoSolicitud.CANCELADA, 1);
        AccionSolicitud accion = new AccionSolicitud();
        accion.setMotivo("Cambié de idea");
        accion.setFecha(LocalDateTime.now());
        when(solicitudRepository.findByIdSolicitud(solicitud.getIdSolicitud()))
            .thenReturn(Optional.of(solicitud));
        lenient().when(accionSolicitudRepository
            .findFirstBySolicitudIdSolicitudAndTipoAccionOrderByFechaDesc(
                solicitud.getIdSolicitud(), TipoAccionSolicitud.CANCELACION))
            .thenReturn(Optional.of(accion));

        SolicitudDTO resultado = solicitudServicioService.obtenerSolicitud(
            solicitud.getIdSolicitud(), CLIENTE);

        assertThat(resultado.getMotivoCancelacion()).isEqualTo("Cambié de idea");
        assertThat(resultado.getFechaCancelacion()).isNotNull();
        assertThat(resultado.isPuedeCancelar()).isFalse();
    }

    @Test
    void obtenerSolicitud_clientePendientePuedeCancelar() {
        Solicitud solicitud = solicitudResumen(EstadoSolicitud.PENDIENTE, 1);
        when(solicitudRepository.findByIdSolicitud(solicitud.getIdSolicitud()))
            .thenReturn(Optional.of(solicitud));

        SolicitudDTO resultado = solicitudServicioService.obtenerSolicitud(
            solicitud.getIdSolicitud(), CLIENTE);

        assertThat(resultado.isPuedeCancelar()).isTrue();
    }

    @Test
    void obtenerSolicitud_artistaNoPuedeCancelar() {
        Solicitud solicitud = solicitudResumen(EstadoSolicitud.PENDIENTE, 1);
        when(solicitudRepository.findByIdSolicitud(solicitud.getIdSolicitud()))
            .thenReturn(Optional.of(solicitud));

        SolicitudDTO resultado = solicitudServicioService.obtenerSolicitud(
            solicitud.getIdSolicitud(), ARTISTA);

        assertThat(resultado.isPuedeCancelar()).isFalse();
    }

    @Test
    void cerrarPorBajaServicio_rechazaPendientesYEnEspera() {
        Solicitud pendiente = solicitudResumen(EstadoSolicitud.PENDIENTE, 2);
        Solicitud enEspera = solicitudResumen(EstadoSolicitud.EN_LISTA_ESPERA, 1);
        when(solicitudRepository.findByServicioIdServicioAndEstadoInOrderByFechaCreacionDesc(
            eq(SERVICIO), any())).thenReturn(List.of(pendiente, enEspera));
        when(accionSolicitudRepository.save(any(AccionSolicitud.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        int cerradas = solicitudServicioService.cerrarPorBajaServicio(SERVICIO);

        assertThat(cerradas).isEqualTo(2);
        assertThat(pendiente.getEstado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        assertThat(enEspera.getEstado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        verify(listaEsperaService).marcarRechazada(enEspera);
        verify(listaEsperaService, never()).marcarRechazada(pendiente);
        verify(notificacionService, times(2)).crearParaUsuario(
            eq(CLIENTE), eq("SOLICITUD_RECHAZADA"), eq("Solicitud rechazada"),
            contains("no mencionar el motivo"), anyLong(), eq("SOLICITUD"));

        ArgumentCaptor<AccionSolicitud> captor = ArgumentCaptor.forClass(AccionSolicitud.class);
        verify(accionSolicitudRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
            .allSatisfy(accion -> {
                assertThat(accion.getOrigen()).isEqualTo("BAJA_SERVICIO");
                assertThat(accion.getMotivo()).isNull();
                assertThat(accion.getTipoAccion()).isEqualTo(TipoAccionSolicitud.RECHAZO);
            });
    }

    @Test
    void obtenerSolicitud_incluyeLaConversacionHabilitada() {
        Solicitud solicitud = solicitudResumen(EstadoSolicitud.PENDIENTE, 1);
        Conversacion conversacion = new Conversacion();
        conversacion.setIdConversacion(44L);
        conversacion.setEstado(EstadoConversacion.ACEPTADA);
        when(solicitudRepository.findByIdSolicitud(solicitud.getIdSolicitud()))
            .thenReturn(Optional.of(solicitud));
        when(conversacionRepository.buscarPorPar(CLIENTE, ARTISTA))
            .thenReturn(Optional.of(conversacion));

        SolicitudDTO resultado = solicitudServicioService.obtenerSolicitud(
            solicitud.getIdSolicitud(), CLIENTE);

        assertThat(resultado.getIdConversacion()).isEqualTo(44L);
        assertThat(resultado.getEstadoConversacion()).isEqualTo("ACEPTADA");
    }

    private void prepararCreacion() {
        lenient().when(usuarioRepository.findByIdUsuario(CLIENTE)).thenReturn(Optional.of(cliente));
        lenient().when(servicioRepository.findByIdServicioForUpdate(SERVICIO))
            .thenReturn(Optional.of(servicio));
        lenient().when(terminosService.obtenerServicio(SERVICIO, ARTISTA)).thenReturn(terminosDTO);
        lenient().when(terminosRepository.findByIdTerminos(TERMINOS)).thenReturn(Optional.of(terminos));
        lenient().when(formularioService.obtenerServicio(SERVICIO, ARTISTA)).thenReturn(formularioDTO);
        lenient().when(formularioRepository.findByIdFormulario(FORMULARIO)).thenReturn(Optional.of(formulario));
        lenient().when(solicitudRepository.existsByServicioIdServicioAndClienteIdUsuarioAndEstadoIn(
            any(), any(), any())).thenReturn(false);
        lenient().when(solicitudRepository.existsByServicioIdServicioAndClienteIdUsuarioAndEstado(
            any(), any(), any())).thenReturn(false);
        lenient().when(solicitudRepository.save(any(Solicitud.class))).thenAnswer(inv -> {
            Solicitud solicitud = inv.getArgument(0);
            solicitud.setIdSolicitud(idsSolicitud.incrementAndGet());
            return solicitud;
        });
    }

    private Solicitud solicitudResumen(EstadoSolicitud estado, int horasAtras) {
        Solicitud solicitud = new Solicitud();
        solicitud.setIdSolicitud(idsSolicitud.incrementAndGet());
        solicitud.setServicio(servicio);
        solicitud.setCliente(cliente);
        solicitud.setArtista(artista);
        solicitud.setEstado(estado);
        solicitud.setCantidadDibujos(2);
        solicitud.setPrecioBase(new BigDecimal("100.00"));
        solicitud.setMoneda(servicio.getMoneda());
        solicitud.setFechaCreacion(LocalDateTime.now().minusHours(horasAtras));
        return solicitud;
    }

    private SolicitudServicioRequest request(int cantidad, boolean prioridadDefinida) {
        SolicitudServicioRequest request = new SolicitudServicioRequest();
        request.setIdServicio(SERVICIO);
        request.setIdTerminos(TERMINOS);
        request.setTerminosVersion(1);
        request.setAceptaTerminos(true);
        request.setAceptaListaEspera(true);
        request.setPrioridadDefinida(prioridadDefinida);

        List<ItemSolicitudRequest> items = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            ItemSolicitudRequest item = new ItemSolicitudRequest();
            item.setNombre("Dibujo " + (i + 1));
            item.setPrioridad(prioridadDefinida ? i + 1 : null);
            RespuestaSolicitudRequest respuesta = new RespuestaSolicitudRequest();
            respuesta.setIdPregunta(PREGUNTA);
            respuesta.setValores(List.of("respuesta " + (i + 1)));
            item.setRespuestas(List.of(respuesta));
            items.add(item);
        }
        request.setItems(items);
        return request;
    }

    private void configurarDerechos() {
        terminos.reemplazarDerechos(List.of(
            derechoTermino(101L, "CONTENIDO",
                "Uso comercial en producción de contenido", "30", true, false),
            derechoTermino(102L, "MERCHANDISING",
                "Uso comercial como merchandising", "20", true, false),
            derechoTermino(103L, "PERSONAL", "Uso personal", null, true, false),
            derechoTermino(104L, "REVENTA", "Reventa", "100", true, true)));
    }

    private TerminoDerecho derechoTermino(
            Long id,
            String codigo,
            String nombre,
            String porcentaje,
            boolean disponible,
            boolean piezaUnica) {
        DerechoTermino derecho = new DerechoTermino();
        derecho.setIdDerechoTermino(id);
        derecho.setCodigo(codigo);
        derecho.setNombre(nombre);
        derecho.setRequierePiezaUnica(piezaUnica);

        TerminoDerecho terminoDerecho = new TerminoDerecho();
        terminoDerecho.setDerecho(derecho);
        terminoDerecho.setDisponible(disponible);
        terminoDerecho.setPorcentajeExtra(
            porcentaje != null ? new BigDecimal(porcentaje) : null);
        return terminoDerecho;
    }

    private ListaEspera listaEspera(int posicion) {
        ListaEspera lista = new ListaEspera();
        lista.setIdListaEspera(99L);
        lista.setPosicion(posicion);
        lista.setCantidadDibujos(1);
        lista.setEstado("ESPERANDO");
        return lista;
    }

    private Moneda moneda() {
        Moneda moneda = new Moneda();
        moneda.setIdMoneda(3L);
        moneda.setCodigo("ARS");
        moneda.setNombre("Peso argentino");
        return moneda;
    }

    private TipoCampo tipoCampo() {
        TipoCampo tipo = new TipoCampo();
        tipo.setIdTipoCampo(41L);
        tipo.setCodigo("TEXTO");
        tipo.setNombre("Bloque de texto");
        return tipo;
    }
}
