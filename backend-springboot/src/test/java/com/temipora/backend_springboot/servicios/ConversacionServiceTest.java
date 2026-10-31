package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
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
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.temipora.backend_springboot.eventos.ConversacionActualizadaEvent;
import com.temipora.backend_springboot.eventos.EscrituraEvent;
import com.temipora.backend_springboot.modelo.ConfiguracionMensajeria;
import com.temipora.backend_springboot.modelo.ContadorMensajesDTO;
import com.temipora.backend_springboot.modelo.Conversacion;
import com.temipora.backend_springboot.modelo.ConversacionAccion;
import com.temipora.backend_springboot.modelo.ConversacionDTO;
import com.temipora.backend_springboot.modelo.ConversacionParticipante;
import com.temipora.backend_springboot.modelo.ConversacionResumenDTO;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.EstadoConversacion;
import com.temipora.backend_springboot.modelo.EstadoRelacionMensaje;
import com.temipora.backend_springboot.modelo.Mensaje;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.RechazoConversacionRequest;
import com.temipora.backend_springboot.modelo.Rol;
import com.temipora.backend_springboot.modelo.SolicitudMensajeRequest;
import com.temipora.backend_springboot.modelo.TipoAccionConversacion;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.ConversacionAccionRepository;
import com.temipora.backend_springboot.repositorios.ConversacionParticipanteRepository;
import com.temipora.backend_springboot.repositorios.ConversacionRepository;
import com.temipora.backend_springboot.repositorios.MensajeRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConversacionServiceTest {

    @Mock
    private ConversacionRepository conversacionRepository;

    @Mock
    private ConversacionParticipanteRepository participanteRepository;

    @Mock
    private ConversacionAccionRepository accionRepository;

    @Mock
    private MensajeRepository mensajeRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ConfiguracionMensajeriaService configuracionService;

    @Mock
    private NotificacionService notificacionService;

    @Mock
    private IndicadorEscritura indicadorEscritura;

    @Mock
    private ApplicationEventPublisher eventoPublisher;

    @InjectMocks
    private ConversacionService conversacionService;

    @Test
    void crear_ok_creaPendienteYNotificaAlDestinatario() {
        Usuario remitente = usuario(1L, "cliente", ERol.CLIENTE);
        Usuario destinatario = usuario(2L, "artista", ERol.ARTISTA);
        prepararCreacion(remitente, destinatario);

        ConversacionDTO dto = conversacionService.crear(
            1L, request("artista", "Consulta por una comisión", "Hola, me interesa tu trabajo."));

        assertThat(dto.getEstado()).isEqualTo(EstadoConversacion.PENDIENTE);
        assertThat(dto.getMotivoContacto()).isEqualTo("Consulta por una comisión");
        assertThat(dto.getContraparte().getIdUsuario()).isEqualTo(2L);
        assertThat(dto.isSoyIniciador()).isTrue();
        assertThat(dto.isPuedeCancelar()).isTrue();
        assertThat(dto.isPuedeEnviar()).isFalse();
        assertThat(dto.getMotivoNoEnviable()).contains("todavía no fue aceptada");

        ArgumentCaptor<ConversacionAccion> accion =
            ArgumentCaptor.forClass(ConversacionAccion.class);
        verify(accionRepository).save(accion.capture());
        assertThat(accion.getValue().getTipoAccion())
            .isEqualTo(TipoAccionConversacion.SOLICITUD);

        verify(notificacionService).crearParaUsuario(
            eq(2L),
            eq(ConversacionService.NOTIFICACION_SOLICITUD),
            anyString(),
            anyString(),
            eq(10L),
            eq(ConversacionService.TIPO_REFERENCIA_CONVERSACION));
        verify(eventoPublisher).publishEvent(any(ConversacionActualizadaEvent.class));
    }

    @Test
    void crear_aSiMismo_lanzaError() {
        Usuario remitente = usuario(1L, "cliente", ERol.CLIENTE);
        when(configuracionService.obtener()).thenReturn(new ConfiguracionMensajeria());
        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(remitente));
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(remitente));

        assertThatThrownBy(() -> conversacionService.crear(
                1L, request("cliente", "Motivo", "Mensaje")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("vos mismo");
    }

    @Test
    void crear_destinatarioAdmin_noDisponible() {
        Usuario remitente = usuario(1L, "cliente", ERol.CLIENTE);
        Usuario admin = usuario(2L, "admin", ERol.ADMIN);
        when(configuracionService.obtener()).thenReturn(new ConfiguracionMensajeria());
        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(remitente));
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("admin"))
            .thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> conversacionService.crear(
                1L, request("admin", "Motivo", "Mensaje")))
            .isInstanceOf(java.util.NoSuchElementException.class)
            .hasMessageContaining("no está disponible");
    }

    @Test
    void crear_conSolicitudPendiente_lanzaConflicto() {
        Usuario remitente = usuario(1L, "cliente", ERol.CLIENTE);
        Usuario destinatario = usuario(2L, "artista", ERol.ARTISTA);
        Conversacion existente = conversacion(
            EstadoConversacion.PENDIENTE, remitente, destinatario);
        when(configuracionService.obtener()).thenReturn(new ConfiguracionMensajeria());
        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(remitente));
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("artista"))
            .thenReturn(Optional.of(destinatario));
        when(conversacionRepository.buscarPorPar(1L, 2L))
            .thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> conversacionService.crear(
                1L, request("artista", "Motivo", "Mensaje")))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("pendiente");
    }

    @Test
    void crear_rechazadaDentroDelCooldown_muestraFechaConcreta() {
        Usuario remitente = usuario(1L, "cliente", ERol.CLIENTE);
        Usuario destinatario = usuario(2L, "artista", ERol.ARTISTA);
        Conversacion existente = conversacion(
            EstadoConversacion.RECHAZADA, remitente, destinatario);
        existente.setFechaRespuesta(LocalDateTime.now().minusHours(2));
        when(configuracionService.obtener()).thenReturn(new ConfiguracionMensajeria());
        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(remitente));
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("artista"))
            .thenReturn(Optional.of(destinatario));
        when(conversacionRepository.buscarPorPar(1L, 2L))
            .thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> conversacionService.crear(
                1L, request("artista", "Motivo", "Mensaje")))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("Podés volver a enviar la solicitud el");
    }

    @Test
    void crear_rechazadaFueraDelCooldown_reenviaElMismoHilo() {
        Usuario remitente = usuario(1L, "cliente", ERol.CLIENTE);
        Usuario destinatario = usuario(2L, "artista", ERol.ARTISTA);
        Conversacion existente = conversacion(
            EstadoConversacion.RECHAZADA, remitente, destinatario);
        existente.setFechaRespuesta(LocalDateTime.now().minusHours(200));
        prepararCreacionSegundoIntento(remitente, destinatario, existente);

        ConversacionDTO dto = conversacionService.crear(
            1L, request("artista", "Nuevo motivo", "Nuevo primer mensaje"));

        assertThat(dto.getEstado()).isEqualTo(EstadoConversacion.PENDIENTE);
        assertThat(existente.getFechaRespuesta()).isNull();
        assertThat(existente.getMotivoContacto()).isEqualTo("Nuevo motivo");
        ArgumentCaptor<ConversacionAccion> accion =
            ArgumentCaptor.forClass(ConversacionAccion.class);
        verify(accionRepository).save(accion.capture());
        assertThat(accion.getValue().getTipoAccion())
            .isEqualTo(TipoAccionConversacion.REENVIO);
    }

    @Test
    void aceptar_soloElDestinatarioPuede() {
        Usuario iniciador = usuario(1L, "cliente", ERol.CLIENTE);
        Usuario destinatario = usuario(2L, "artista", ERol.ARTISTA);
        Conversacion conversacion = conversacion(
            EstadoConversacion.PENDIENTE, iniciador, destinatario);
        when(conversacionRepository.findByIdConversacionForUpdate(10L))
            .thenReturn(Optional.of(conversacion));
        when(usuarioRepository.findByIdUsuario(1L))
            .thenReturn(Optional.of(iniciador));

        assertThatThrownBy(() -> conversacionService.aceptar(10L, 1L))
            .isInstanceOf(AccesoDenegadoException.class)
            .hasMessageContaining("No podés aceptar");
    }

    @Test
    void aceptar_ok_marcaLeidoYNotificaAlIniciador() {
        Usuario iniciador = usuario(1L, "cliente", ERol.CLIENTE);
        Usuario destinatario = usuario(2L, "artista", ERol.ARTISTA);
        Conversacion conversacion = conversacion(
            EstadoConversacion.PENDIENTE, iniciador, destinatario);
        ConversacionParticipante participante = new ConversacionParticipante();
        participante.setConversacion(conversacion);
        participante.setUsuario(destinatario);
        when(conversacionRepository.findByIdConversacionForUpdate(10L))
            .thenReturn(Optional.of(conversacion));
        when(usuarioRepository.findByIdUsuario(2L))
            .thenReturn(Optional.of(destinatario));
        when(participanteRepository
            .findByConversacionIdConversacionAndUsuarioIdUsuario(10L, 2L))
            .thenReturn(Optional.of(participante));
        when(participanteRepository.save(any(ConversacionParticipante.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(accionRepository.save(any(ConversacionAccion.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(configuracionService.obtener()).thenReturn(new ConfiguracionMensajeria());

        ConversacionDTO dto = conversacionService.aceptar(10L, 2L);

        assertThat(dto.getEstado()).isEqualTo(EstadoConversacion.ACEPTADA);
        assertThat(participante.getUltimoLeidoEn()).isNotNull();
        assertThat(dto.isPuedeEnviar()).isTrue();
        verify(notificacionService).crearParaUsuario(
            eq(1L),
            eq(ConversacionService.NOTIFICACION_ACEPTADA),
            anyString(),
            anyString(),
            eq(10L),
            eq(ConversacionService.TIPO_REFERENCIA_CONVERSACION));
        verify(eventoPublisher).publishEvent(any(ConversacionActualizadaEvent.class));
    }

    @Test
    void rechazar_guardaMotivoYNotifica() {
        Usuario iniciador = usuario(1L, "cliente", ERol.CLIENTE);
        Usuario destinatario = usuario(2L, "artista", ERol.ARTISTA);
        Conversacion conversacion = conversacion(
            EstadoConversacion.PENDIENTE, iniciador, destinatario);
        ConversacionParticipante participante = new ConversacionParticipante();
        participante.setConversacion(conversacion);
        participante.setUsuario(destinatario);
        ConversacionAccion rechazo = new ConversacionAccion();
        rechazo.setConversacion(conversacion);
        rechazo.setTipoAccion(TipoAccionConversacion.RECHAZO);
        rechazo.setUsuario(destinatario);
        rechazo.setMotivo("No es el tipo de encargo que hago");
        rechazo.setFecha(LocalDateTime.now());

        when(conversacionRepository.findByIdConversacionForUpdate(10L))
            .thenReturn(Optional.of(conversacion));
        when(usuarioRepository.findByIdUsuario(2L))
            .thenReturn(Optional.of(destinatario));
        when(participanteRepository
            .findByConversacionIdConversacionAndUsuarioIdUsuario(10L, 2L))
            .thenReturn(Optional.of(participante));
        when(accionRepository.save(any(ConversacionAccion.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(accionRepository
            .findFirstByConversacionIdConversacionAndTipoAccionOrderByFechaDesc(
                10L, TipoAccionConversacion.RECHAZO))
            .thenReturn(Optional.of(rechazo));
        when(configuracionService.obtener()).thenReturn(new ConfiguracionMensajeria());

        RechazoConversacionRequest request = new RechazoConversacionRequest();
        request.setMotivo("No es el tipo de encargo que hago");
        ConversacionDTO dto = conversacionService.rechazar(10L, 2L, request);

        assertThat(dto.getEstado()).isEqualTo(EstadoConversacion.RECHAZADA);
        assertThat(dto.getMotivoRechazo())
            .isEqualTo("No es el tipo de encargo que hago");
        assertThat(dto.isPuedeEnviar()).isFalse();
        verify(notificacionService).crearParaUsuario(
            eq(1L),
            eq(ConversacionService.NOTIFICACION_RECHAZADA),
            anyString(),
            anyString(),
            eq(10L),
            eq(ConversacionService.TIPO_REFERENCIA_CONVERSACION));
    }

    @Test
    void cancelar_soloElIniciadorPuede() {
        Usuario iniciador = usuario(1L, "cliente", ERol.CLIENTE);
        Usuario destinatario = usuario(2L, "artista", ERol.ARTISTA);
        Conversacion conversacion = conversacion(
            EstadoConversacion.PENDIENTE, iniciador, destinatario);
        when(conversacionRepository.findByIdConversacionForUpdate(10L))
            .thenReturn(Optional.of(conversacion));
        when(usuarioRepository.findByIdUsuario(2L))
            .thenReturn(Optional.of(destinatario));

        assertThatThrownBy(() -> conversacionService.cancelar(10L, 2L))
            .isInstanceOf(AccesoDenegadoException.class)
            .hasMessageContaining("No podés cancelar");
    }

    @Test
    void estadoConUsuario_aceptada_noPermiteSolicitar() {
        Usuario yo = usuario(1L, "cliente", ERol.CLIENTE);
        Usuario otro = usuario(2L, "artista", ERol.ARTISTA);
        Conversacion conversacion = conversacion(
            EstadoConversacion.ACEPTADA, yo, otro);
        when(configuracionService.obtener()).thenReturn(new ConfiguracionMensajeria());
        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(yo));
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("artista"))
            .thenReturn(Optional.of(otro));
        when(conversacionRepository.buscarPorPar(1L, 2L))
            .thenReturn(Optional.of(conversacion));

        var estado = conversacionService.estadoConUsuario(1L, "artista");

        assertThat(estado.getEstadoRelacion())
            .isEqualTo(EstadoRelacionMensaje.ACEPTADA);
        assertThat(estado.isPuedeSolicitar()).isFalse();
        assertThat(estado.getIdConversacion()).isEqualTo(10L);
    }

    @Test
    void estadoConUsuario_rechazadaEnCooldown_muestraFechaProximoEnvio() {
        Usuario yo = usuario(1L, "cliente", ERol.CLIENTE);
        Usuario otro = usuario(2L, "artista", ERol.ARTISTA);
        Conversacion conversacion = conversacion(
            EstadoConversacion.RECHAZADA, yo, otro);
        conversacion.setFechaRespuesta(LocalDateTime.now().minusHours(1));
        when(configuracionService.obtener()).thenReturn(new ConfiguracionMensajeria());
        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(yo));
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("artista"))
            .thenReturn(Optional.of(otro));
        when(conversacionRepository.buscarPorPar(1L, 2L))
            .thenReturn(Optional.of(conversacion));

        var estado = conversacionService.estadoConUsuario(1L, "artista");

        assertThat(estado.getEstadoRelacion())
            .isEqualTo(EstadoRelacionMensaje.RECHAZADA);
        assertThat(estado.isPuedeSolicitar()).isFalse();
        assertThat(estado.getFechaProximoEnvio()).isNotNull();
        assertThat(estado.getMotivoNoSolicitable()).contains("Podés volver");
    }

    @Test
    void contador_sumaPendientesYNoLeidos() {
        when(conversacionRepository
            .countByDestinatarioIdUsuarioAndEstado(
                1L, EstadoConversacion.PENDIENTE))
            .thenReturn(2L);
        when(mensajeRepository.contarNoLeidos(
            1L,
            com.temipora.backend_springboot.modelo.EstadoMensaje.ENVIADO,
            EstadoConversacion.ACEPTADA))
            .thenReturn(5L);

        ContadorMensajesDTO contador = conversacionService.contador(1L);

        assertThat(contador.getSolicitudesPendientes()).isEqualTo(2L);
        assertThat(contador.getNoLeidos()).isEqualTo(5L);
    }

    @Test
    void listar_sinFilas_devuelvePaginaVacia() {
        when(participanteRepository.listarBandeja(
            anyLong(),
            anyBoolean(),
            anyCollection(),
            anyBoolean(),
            anyBoolean(),
            any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));

        PaginaDTO<ConversacionResumenDTO> pagina =
            conversacionService.listar(
                1L, EstadoConversacion.ACEPTADA, false,
                com.temipora.backend_springboot.modelo.BandejaMensajes.TODAS,
                0, 20);

        assertThat(pagina.getContenido()).isEmpty();
        assertThat(pagina.isHayMas()).isFalse();
        verify(conversacionRepository, never())
            .findByIdConversacionForUpdate(anyLong());
    }

    @Test
    void marcarEscribiendo_enConversacionAceptada_marcaAlUsuario() {
        Usuario iniciador = usuario(1L, "cliente", ERol.CLIENTE);
        Usuario destinatario = usuario(2L, "artista", ERol.ARTISTA);
        Conversacion conversacion = conversacion(
            EstadoConversacion.ACEPTADA, iniciador, destinatario);
        when(conversacionRepository.findByIdConversacion(10L))
            .thenReturn(Optional.of(conversacion));
        when(usuarioRepository.findByIdUsuario(2L))
            .thenReturn(Optional.of(destinatario));

        conversacionService.marcarEscribiendo(10L, 2L);

        verify(indicadorEscritura).marcar(10L, 2L);
        verify(eventoPublisher).publishEvent(any(EscrituraEvent.class));
    }

    @Test
    void marcarEscribiendo_enSolicitudPendiente_noMarcaNada() {
        Usuario iniciador = usuario(1L, "cliente", ERol.CLIENTE);
        Usuario destinatario = usuario(2L, "artista", ERol.ARTISTA);
        Conversacion conversacion = conversacion(
            EstadoConversacion.PENDIENTE, iniciador, destinatario);
        when(conversacionRepository.findByIdConversacion(10L))
            .thenReturn(Optional.of(conversacion));

        conversacionService.marcarEscribiendo(10L, 2L);

        verify(indicadorEscritura, never()).marcar(anyLong(), anyLong());
    }

    @Test
    void marcarEscribiendo_noParticipante_lanzaAccesoDenegado() {
        Usuario iniciador = usuario(1L, "cliente", ERol.CLIENTE);
        Usuario destinatario = usuario(2L, "artista", ERol.ARTISTA);
        Conversacion conversacion = conversacion(
            EstadoConversacion.ACEPTADA, iniciador, destinatario);
        when(conversacionRepository.findByIdConversacion(10L))
            .thenReturn(Optional.of(conversacion));

        assertThatThrownBy(() -> conversacionService.marcarEscribiendo(10L, 3L))
            .isInstanceOf(AccesoDenegadoException.class)
            .hasMessageContaining("No podés indicar escritura");
    }

    @Test
    void estadoEscritura_aceptada_consultaALaContraparte() {
        Usuario iniciador = usuario(1L, "cliente", ERol.CLIENTE);
        Usuario destinatario = usuario(2L, "artista", ERol.ARTISTA);
        Conversacion conversacion = conversacion(
            EstadoConversacion.ACEPTADA, iniciador, destinatario);
        when(conversacionRepository.findByIdConversacion(10L))
            .thenReturn(Optional.of(conversacion));
        when(indicadorEscritura.estaEscribiendo(10L, 2L)).thenReturn(true);

        var estado = conversacionService.estadoEscritura(10L, 1L);

        assertThat(estado.isEscribiendo()).isTrue();
    }

    @Test
    void estadoEscritura_enSolicitudPendiente_devuelveFalse() {
        Usuario iniciador = usuario(1L, "cliente", ERol.CLIENTE);
        Usuario destinatario = usuario(2L, "artista", ERol.ARTISTA);
        Conversacion conversacion = conversacion(
            EstadoConversacion.PENDIENTE, iniciador, destinatario);
        when(conversacionRepository.findByIdConversacion(10L))
            .thenReturn(Optional.of(conversacion));

        var estado = conversacionService.estadoEscritura(10L, 1L);

        assertThat(estado.isEscribiendo()).isFalse();
    }

    @Test
    void vencerPendientes_sinPlazo_noHaceNada() {
        ConfiguracionMensajeria config = new ConfiguracionMensajeria();
        config.setDiasVencimientoSolicitud(0);
        when(configuracionService.obtener()).thenReturn(config);

        assertThat(conversacionService.vencerPendientes()).isZero();
        verify(conversacionRepository, never())
            .findByEstadoAndFechaSolicitudBefore(any(), any());
    }

    @Test
    void vencerPendientes_ok_marcaVencidaYNotificaAlIniciador() {
        ConfiguracionMensajeria config = new ConfiguracionMensajeria();
        config.setDiasVencimientoSolicitud(30);
        when(configuracionService.obtener()).thenReturn(config);

        Usuario iniciador = usuario(1L, "cliente", ERol.CLIENTE);
        Usuario destinatario = usuario(2L, "artista", ERol.ARTISTA);
        Conversacion candidata = conversacion(
            EstadoConversacion.PENDIENTE, iniciador, destinatario);
        candidata.setFechaSolicitud(LocalDateTime.now().minusDays(40));
        when(conversacionRepository.findByEstadoAndFechaSolicitudBefore(
            eq(EstadoConversacion.PENDIENTE), any(LocalDateTime.class)))
            .thenReturn(List.of(candidata));
        when(conversacionRepository.findByIdConversacionForUpdate(10L))
            .thenReturn(Optional.of(candidata));

        int vencidas = conversacionService.vencerPendientes();

        assertThat(vencidas).isEqualTo(1);
        assertThat(candidata.getEstado()).isEqualTo(EstadoConversacion.VENCIDA);
        assertThat(candidata.getFechaRespuesta()).isNotNull();
        verify(notificacionService).crearParaUsuario(
            eq(1L),
            eq(ConversacionService.NOTIFICACION_VENCIDA),
            anyString(),
            anyString(),
            eq(10L),
            eq(ConversacionService.TIPO_REFERENCIA_CONVERSACION));
        verify(eventoPublisher)
            .publishEvent(any(ConversacionActualizadaEvent.class));
    }

    private void prepararCreacion(Usuario remitente, Usuario destinatario) {
        when(configuracionService.obtener()).thenReturn(new ConfiguracionMensajeria());
        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(remitente));
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("artista"))
            .thenReturn(Optional.of(destinatario));
        when(conversacionRepository.buscarPorPar(1L, 2L))
            .thenReturn(Optional.empty());
        when(conversacionRepository.countByIniciadorIdUsuarioAndEstado(
            1L, EstadoConversacion.PENDIENTE)).thenReturn(0L);
        when(conversacionRepository.save(any(Conversacion.class)))
            .thenAnswer(inv -> {
                Conversacion c = inv.getArgument(0);
                c.setIdConversacion(10L);
                return c;
            });
        when(participanteRepository.save(any(ConversacionParticipante.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(mensajeRepository.save(any(Mensaje.class)))
            .thenAnswer(inv -> {
                Mensaje m = inv.getArgument(0);
                m.setIdMensaje(100L);
                return m;
            });
        when(accionRepository.save(any(ConversacionAccion.class)))
            .thenAnswer(inv -> inv.getArgument(0));
    }

    private void prepararCreacionSegundoIntento(
            Usuario remitente,
            Usuario destinatario,
            Conversacion existente) {
        when(configuracionService.obtener()).thenReturn(new ConfiguracionMensajeria());
        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(remitente));
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("artista"))
            .thenReturn(Optional.of(destinatario));
        when(conversacionRepository.buscarPorPar(1L, 2L))
            .thenReturn(Optional.of(existente));
        when(conversacionRepository.countByIniciadorIdUsuarioAndEstado(
            1L, EstadoConversacion.PENDIENTE)).thenReturn(0L);
        when(participanteRepository
            .findByConversacionIdConversacionAndUsuarioIdUsuario(10L, 1L))
            .thenReturn(Optional.of(new ConversacionParticipante()));
        when(participanteRepository
            .findByConversacionIdConversacionAndUsuarioIdUsuario(10L, 2L))
            .thenReturn(Optional.of(new ConversacionParticipante()));
        when(mensajeRepository.save(any(Mensaje.class)))
            .thenAnswer(inv -> {
                Mensaje m = inv.getArgument(0);
                m.setIdMensaje(101L);
                return m;
            });
        when(accionRepository.save(any(ConversacionAccion.class)))
            .thenAnswer(inv -> inv.getArgument(0));
    }

    private Usuario usuario(Long id, String nombre, ERol rol) {
        Rol r = new Rol();
        r.setNombre(rol);
        r.setEstado(true);
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(id);
        usuario.setNombreUsuario(nombre);
        usuario.setEstadoCuenta(true);
        usuario.setRol(r);
        return usuario;
    }

    private Conversacion conversacion(
            EstadoConversacion estado,
            Usuario iniciador,
            Usuario destinatario) {
        Conversacion conversacion = new Conversacion();
        conversacion.setIdConversacion(10L);
        conversacion.setEstado(estado);
        conversacion.setIniciador(iniciador);
        conversacion.setDestinatario(destinatario);
        conversacion.setMotivoContacto("Motivo");
        conversacion.setFechaSolicitud(LocalDateTime.now());
        return conversacion;
    }

    private SolicitudMensajeRequest request(
            String nombre, String motivo, String primerMensaje) {
        SolicitudMensajeRequest request = new SolicitudMensajeRequest();
        request.setNombreUsuarioDestinatario(nombre);
        request.setMotivoContacto(motivo);
        request.setPrimerMensaje(primerMensaje);
        return request;
    }
}
