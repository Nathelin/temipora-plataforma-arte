package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.temipora.backend_springboot.eventos.LeidoEvent;
import com.temipora.backend_springboot.eventos.MensajeEnviadoEvent;
import com.temipora.backend_springboot.modelo.ConfiguracionMensajeria;
import com.temipora.backend_springboot.modelo.Conversacion;
import com.temipora.backend_springboot.modelo.ConversacionParticipante;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.EstadoConversacion;
import com.temipora.backend_springboot.modelo.HistorialMensajesDTO;
import com.temipora.backend_springboot.modelo.Mensaje;
import com.temipora.backend_springboot.modelo.MensajeDTO;
import com.temipora.backend_springboot.modelo.MensajeRequest;
import com.temipora.backend_springboot.modelo.Rol;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.ConversacionParticipanteRepository;
import com.temipora.backend_springboot.repositorios.ConversacionRepository;
import com.temipora.backend_springboot.repositorios.MensajeRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MensajeServiceTest {

    @Mock
    private MensajeRepository mensajeRepository;

    @Mock
    private ConversacionRepository conversacionRepository;

    @Mock
    private ConversacionParticipanteRepository participanteRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ConfiguracionMensajeriaService configuracionService;

    @Mock
    private IndicadorEscritura indicadorEscritura;

    @Mock
    private ApplicationEventPublisher eventoPublisher;

    @InjectMocks
    private MensajeService mensajeService;

    @Test
    void enviar_noParticipante_lanzaNoEncontrada() {
        Conversacion conversacion = conversacion(
            EstadoConversacion.ACEPTADA,
            usuario(1L, "cliente", ERol.CLIENTE),
            usuario(2L, "artista", ERol.ARTISTA));
        when(conversacionRepository.findByIdConversacion(10L))
            .thenReturn(Optional.of(conversacion));

        assertThatThrownBy(() -> mensajeService.enviar(
                10L, 3L, request("Hola")))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("Conversación no encontrada");
    }

    @Test
    void enviar_pendiente_lanzaConflicto() {
        Conversacion conversacion = conversacion(
            EstadoConversacion.PENDIENTE,
            usuario(1L, "cliente", ERol.CLIENTE),
            usuario(2L, "artista", ERol.ARTISTA));
        when(conversacionRepository.findByIdConversacion(10L))
            .thenReturn(Optional.of(conversacion));
        when(usuarioRepository.findByIdUsuario(1L))
            .thenReturn(Optional.of(conversacion.getIniciador()));

        assertThatThrownBy(() -> mensajeService.enviar(
                10L, 1L, request("Hola")))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("todavía no fue aceptada");
    }

    @Test
    void enviar_vacio_lanzaError() {
        Conversacion conversacion = conversacion(
            EstadoConversacion.ACEPTADA,
            usuario(1L, "cliente", ERol.CLIENTE),
            usuario(2L, "artista", ERol.ARTISTA));
        prepararEnvio(conversacion, new ConfiguracionMensajeria());

        assertThatThrownBy(() -> mensajeService.enviar(
                10L, 1L, request("   ")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("obligatorio");
    }

    @Test
    void enviar_limiteDiario_lanzaConflicto() {
        Conversacion conversacion = conversacion(
            EstadoConversacion.ACEPTADA,
            usuario(1L, "cliente", ERol.CLIENTE),
            usuario(2L, "artista", ERol.ARTISTA));
        ConfiguracionMensajeria config = new ConfiguracionMensajeria();
        config.setMaxMensajesPorDia(2);
        prepararEnvio(conversacion, config);
        when(mensajeRepository.countByRemitenteIdUsuarioAndFechaEnvioAfter(
            eq(1L), any(LocalDateTime.class))).thenReturn(2L);

        assertThatThrownBy(() -> mensajeService.enviar(
                10L, 1L, request("Hola")))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("límite");
    }

    @Test
    void enviar_ok_guardaDesarchivaYApagaElIndicadorDeEscritura() {
        Conversacion conversacion = conversacion(
            EstadoConversacion.ACEPTADA,
            usuario(1L, "cliente", ERol.CLIENTE),
            usuario(2L, "artista", ERol.ARTISTA));
        prepararEnvio(conversacion, new ConfiguracionMensajeria());
        when(mensajeRepository.countByRemitenteIdUsuarioAndFechaEnvioAfter(
            eq(1L), any(LocalDateTime.class))).thenReturn(0L);
        when(mensajeRepository.save(any(Mensaje.class)))
            .thenAnswer(inv -> {
                Mensaje m = inv.getArgument(0);
                m.setIdMensaje(100L);
                return m;
            });
        ConversacionParticipante participanteDestinatario =
            new ConversacionParticipante();
        participanteDestinatario.setConversacion(conversacion);
        participanteDestinatario.setUsuario(conversacion.getDestinatario());
        participanteDestinatario.setArchivada(true);
        when(participanteRepository
            .findByConversacionIdConversacionAndUsuarioIdUsuario(10L, 2L))
            .thenReturn(Optional.of(participanteDestinatario));

        MensajeDTO dto = mensajeService.enviar(
            10L, 1L, request("Un contenido privado"));

        assertThat(dto.getIdMensaje()).isEqualTo(100L);
        assertThat(dto.getContenido()).isEqualTo("Un contenido privado");
        assertThat(dto.isEsPropio()).isTrue();
        assertThat(dto.isLeido()).isFalse();
        assertThat(conversacion.getFechaUltimoMensaje()).isNotNull();
        assertThat(participanteDestinatario.isArchivada()).isFalse();
        verify(indicadorEscritura).limpiar(10L, 1L);
        verify(eventoPublisher).publishEvent(any(MensajeEnviadoEvent.class));
    }

    @Test
    void historial_devuelveMensajesAscendentesYElCursor() {
        Conversacion conversacion = conversacion(
            EstadoConversacion.ACEPTADA,
            usuario(1L, "cliente", ERol.CLIENTE),
            usuario(2L, "artista", ERol.ARTISTA));
        Usuario remitente = conversacion.getIniciador();
        List<Mensaje> filas = List.of(
            mensaje(10L, remitente, "tercero"),
            mensaje(9L, remitente, "segundo"),
            mensaje(8L, remitente, "primero"));
        when(conversacionRepository.findByIdConversacion(10L))
            .thenReturn(Optional.of(conversacion));
        when(mensajeRepository.listarHistorial(
            eq(10L),
            eq(null),
            eq(com.temipora.backend_springboot.modelo.EstadoMensaje.ENVIADO),
            any()))
            .thenReturn(filas);
        ConversacionParticipante contraparte = new ConversacionParticipante();
        contraparte.setUltimoLeidoEn(LocalDateTime.now());
        when(participanteRepository
            .findByConversacionIdConversacionAndUsuarioIdUsuarioNot(10L, 1L))
            .thenReturn(Optional.of(contraparte));

        HistorialMensajesDTO historial = mensajeService.historial(
            10L, 1L, null, 2);

        assertThat(historial.isHayMas()).isTrue();
        assertThat(historial.getIdCursorAnterior()).isEqualTo(9L);
        assertThat(historial.getMensajes())
            .extracting(MensajeDTO::getIdMensaje)
            .containsExactly(9L, 10L);
        assertThat(historial.getMensajes())
            .allMatch(MensajeDTO::isEsPropio);
        assertThat(historial.getContraparteUltimoLeidoEn()).isNotNull();
    }

    @Test
    void marcarLeido_enSolicitudPendiente_noMarcaNada() {
        Conversacion conversacion = conversacion(
            EstadoConversacion.PENDIENTE,
            usuario(1L, "cliente", ERol.CLIENTE),
            usuario(2L, "artista", ERol.ARTISTA));
        when(conversacionRepository.findByIdConversacion(10L))
            .thenReturn(Optional.of(conversacion));

        mensajeService.marcarLeido(10L, 2L);

        verify(participanteRepository, never())
            .findByConversacionIdConversacionAndUsuarioIdUsuario(10L, 2L);
    }

    @Test
    void marcarLeido_enConversacionAceptada_avanzaElTimestamp() {
        Conversacion conversacion = conversacion(
            EstadoConversacion.ACEPTADA,
            usuario(1L, "cliente", ERol.CLIENTE),
            usuario(2L, "artista", ERol.ARTISTA));
        ConversacionParticipante participante = new ConversacionParticipante();
        participante.setConversacion(conversacion);
        participante.setUsuario(conversacion.getDestinatario());
        when(conversacionRepository.findByIdConversacion(10L))
            .thenReturn(Optional.of(conversacion));
        when(participanteRepository
            .findByConversacionIdConversacionAndUsuarioIdUsuario(10L, 2L))
            .thenReturn(Optional.of(participante));

        mensajeService.marcarLeido(10L, 2L);

        assertThat(participante.getUltimoLeidoEn()).isNotNull();
        verify(eventoPublisher).publishEvent(any(LeidoEvent.class));
    }

    private void prepararEnvio(
            Conversacion conversacion,
            ConfiguracionMensajeria config) {
        when(conversacionRepository.findByIdConversacion(10L))
            .thenReturn(Optional.of(conversacion));
        when(usuarioRepository.findByIdUsuario(1L))
            .thenReturn(Optional.of(conversacion.getIniciador()));
        when(configuracionService.obtener()).thenReturn(config);
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

    private Mensaje mensaje(Long id, Usuario remitente, String contenido) {
        Mensaje mensaje = new Mensaje();
        mensaje.setIdMensaje(id);
        mensaje.setRemitente(remitente);
        mensaje.setContenido(contenido);
        mensaje.setFechaEnvio(LocalDateTime.now());
        return mensaje;
    }

    private MensajeRequest request(String contenido) {
        MensajeRequest request = new MensajeRequest();
        request.setContenido(contenido);
        return request;
    }
}
