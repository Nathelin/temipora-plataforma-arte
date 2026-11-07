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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.temipora.backend_springboot.modelo.ContadorNotificacionesDTO;
import com.temipora.backend_springboot.modelo.Notificacion;
import com.temipora.backend_springboot.modelo.NotificacionDTO;
import com.temipora.backend_springboot.modelo.NotificacionUsuario;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.Solicitud;
import com.temipora.backend_springboot.modelo.TipoNotificacion;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.ConversacionRepository;
import com.temipora.backend_springboot.repositorios.NotificacionRepository;
import com.temipora.backend_springboot.repositorios.NotificacionUsuarioRepository;
import com.temipora.backend_springboot.repositorios.PedidoRepository;
import com.temipora.backend_springboot.repositorios.SolicitudRepository;
import com.temipora.backend_springboot.repositorios.TipoNotificacionRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock
    private NotificacionRepository notificacionRepository;

    @Mock
    private NotificacionUsuarioRepository notificacionUsuarioRepository;

    @Mock
    private TipoNotificacionRepository tipoNotificacionRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private SolicitudRepository solicitudRepository;

    @Mock
    private ConversacionRepository conversacionRepository;

    @InjectMocks
    private NotificacionService service;

    // ---------------------------------------------------------------
    // crearParaUsuario
    // ---------------------------------------------------------------

    @Test
    void crearParaUsuario_ok_guardaYDevuelveDTO() {
        when(tipoNotificacionRepository.findByCodigo("OFERTA"))
            .thenReturn(Optional.of(tipo("OFERTA")));
        when(usuarioRepository.findByIdUsuario(1L))
            .thenReturn(Optional.of(usuario(1L)));
        when(notificacionRepository.save(any(Notificacion.class)))
            .thenAnswer(inv -> {
                Notificacion n = inv.getArgument(0);
                n.getDestinatarios().get(0).setIdNotificacionUsuario(9L);
                return n;
            });

        NotificacionDTO dto = service.crearParaUsuario(
            1L, "OFERTA", "Nombre", "Descripción", 5L, "PEDIDO");

        assertThat(dto.getIdNotificacionUsuario()).isEqualTo(9L);
        assertThat(dto.getNombre()).isEqualTo("Nombre");
        assertThat(dto.getCodigoTipo()).isEqualTo("OFERTA");
        assertThat(dto.isLeida()).isFalse();
        verify(notificacionRepository).save(any(Notificacion.class));
    }

    @Test
    void crearParaUsuario_tipoInexistente_lanzaIllegalState() {
        when(tipoNotificacionRepository.findByCodigo("NO_EXISTE"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crearParaUsuario(
            1L, "NO_EXISTE", "n", "d", 1L, "PEDIDO"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("NO_EXISTE");
    }

    @Test
    void crearParaUsuario_usuarioInexistente_lanzaNoSuchElement() {
        when(tipoNotificacionRepository.findByCodigo("OFERTA"))
            .thenReturn(Optional.of(tipo("OFERTA")));
        when(usuarioRepository.findByIdUsuario(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crearParaUsuario(
            99L, "OFERTA", "n", "d", 1L, "PEDIDO"))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void crearParaUsuario_textosLargos_seTruncan() {
        when(tipoNotificacionRepository.findByCodigo("OFERTA"))
            .thenReturn(Optional.of(tipo("OFERTA")));
        when(usuarioRepository.findByIdUsuario(1L))
            .thenReturn(Optional.of(usuario(1L)));
        when(notificacionRepository.save(any(Notificacion.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        NotificacionDTO dto = service.crearParaUsuario(
            1L, "OFERTA", "n".repeat(150), "d".repeat(300), 1L, "PEDIDO");

        assertThat(dto.getNombre()).hasSize(100);
        assertThat(dto.getDescripcion()).hasSize(250);
    }

    // ---------------------------------------------------------------
    // listar / recientes / contador / marcar
    // ---------------------------------------------------------------

    @Test
    void listar_completaElRolDeArtista() {
        Notificacion notificacion = notificacion(1L, "PEDIDO", 7L);
        when(notificacionUsuarioRepository.listarPaginado(
                eq(1L), eq(""), eq(false), any(Pageable.class)))
            .thenReturn(pagina(destinatario(notificacion, 3L)));

        Pedido pedido = new Pedido();
        pedido.setIdPedido(7L);
        pedido.setArtista(usuario(1L));
        pedido.setCliente(usuario(2L));
        when(pedidoRepository.findAllById(any())).thenReturn(List.of(pedido));

        PaginaDTO<NotificacionDTO> resultado = service.listar(1L, null, false, 0, 20);

        assertThat(resultado.getContenido()).hasSize(1);
        assertThat(resultado.getContenido().get(0).getRolReferencia()).isEqualTo("ARTISTA");
    }

    @Test
    void listar_rolClienteYReferenciaDesconocida() {
        Notificacion solicitud = notificacion(1L, "SOLICITUD", 4L);
        Notificacion otra = notificacion(2L, "OTRO", 5L);
        when(notificacionUsuarioRepository.listarPaginado(
                eq(1L), eq(""), eq(false), any(Pageable.class)))
            .thenReturn(pagina(
                destinatario(solicitud, 3L),
                destinatario(otra, 4L)));

        Solicitud s = new Solicitud();
        s.setIdSolicitud(4L);
        s.setArtista(usuario(8L));
        s.setCliente(usuario(1L));
        when(solicitudRepository.findAllById(any())).thenReturn(List.of(s));

        PaginaDTO<NotificacionDTO> resultado = service.listar(1L, null, false, 0, 20);

        assertThat(resultado.getContenido().get(0).getRolReferencia()).isEqualTo("CLIENTE");
        assertThat(resultado.getContenido().get(1).getRolReferencia()).isNull();
    }

    @Test
    void listar_sinReferenciaNoConsultaRepositorios() {
        when(notificacionUsuarioRepository.listarPaginado(
                eq(1L), eq(""), eq(false), any(Pageable.class)))
            .thenReturn(pagina(destinatario(notificacion(1L, null, null), 3L)));

        service.listar(1L, null, false, 0, 20);

        verify(pedidoRepository, never()).findAllById(any());
        verify(solicitudRepository, never()).findAllById(any());
        verify(conversacionRepository, never()).findAllById(any());
    }

    @Test
    void listar_normalizaLaCategoriaYDelegaLosFiltros() {
        when(notificacionUsuarioRepository.listarPaginado(
                eq(1L), eq("PEDIDOS"), eq(true), any(Pageable.class)))
            .thenReturn(pagina());

        PaginaDTO<NotificacionDTO> resultado = service.listar(1L, " pedidos ", true, 0, 20);

        assertThat(resultado.getContenido()).isEmpty();
        assertThat(resultado.isHayMas()).isFalse();
        verify(notificacionUsuarioRepository).listarPaginado(
            1L, "PEDIDOS", true, PageRequest.of(0, 20));
    }

    @Test
    void listar_acotaElTamanioDePagina() {
        when(notificacionUsuarioRepository.listarPaginado(
                eq(1L), eq(""), eq(false), any(Pageable.class)))
            .thenReturn(pagina());

        service.listar(1L, null, false, -3, 999);

        verify(notificacionUsuarioRepository).listarPaginado(
            1L, "", false, PageRequest.of(0, NotificacionService.TAMANIO_MAXIMO));
    }

    @Test
    void recientes_usaElTopePorDefecto() {
        when(notificacionUsuarioRepository.listarRecientes(
                eq(1L), any(Pageable.class)))
            .thenReturn(List.of());

        service.recientes(1L, null);

        verify(notificacionUsuarioRepository).listarRecientes(
            1L, PageRequest.of(0, NotificacionService.LIMITE_RECIENTES));
    }

    @Test
    void contador_devuelveTotalYPorCategoria() {
        when(notificacionUsuarioRepository.countByUsuarioIdUsuarioAndLeidaFalse(1L))
            .thenReturn(4L);
        when(notificacionUsuarioRepository.contarNoLeidasPorCategoria(1L))
            .thenReturn(List.of(
                new Object[] {"PEDIDOS", 3L},
                new Object[] {"MENSAJES", 1L}));

        ContadorNotificacionesDTO contador = service.contador(1L);

        assertThat(contador.noLeidas()).isEqualTo(4L);
        assertThat(contador.porCategoria())
            .containsEntry("PEDIDOS", 3L)
            .containsEntry("MENSAJES", 1L);
    }

    @Test
    void marcarTodasLeidas_delegaEnElRepositorio() {
        when(notificacionUsuarioRepository.marcarTodasLeidas(
                eq(1L), any(LocalDateTime.class)))
            .thenReturn(5);

        assertThat(service.marcarTodasLeidas(1L)).isEqualTo(5);
    }

    @Test
    void marcarLeida_laMarcaYGuardaFecha() {
        NotificacionUsuario destinatario = destinatario(notificacion(1L, "PEDIDO", 7L), 3L);
        when(notificacionUsuarioRepository
            .findByIdNotificacionUsuarioAndUsuarioIdUsuario(3L, 1L))
            .thenReturn(Optional.of(destinatario));

        NotificacionDTO dto = service.marcarLeida(3L, 1L);

        assertThat(destinatario.isLeida()).isTrue();
        assertThat(destinatario.getFechaLectura()).isNotNull();
        assertThat(dto.isLeida()).isTrue();
    }

    @Test
    void marcarLeida_yaLeida_noCambiaLaFecha() {
        NotificacionUsuario destinatario = destinatario(notificacion(1L, null, null), 3L);
        destinatario.setLeida(true);
        when(notificacionUsuarioRepository
            .findByIdNotificacionUsuarioAndUsuarioIdUsuario(3L, 1L))
            .thenReturn(Optional.of(destinatario));

        service.marcarLeida(3L, 1L);

        assertThat(destinatario.getFechaLectura()).isNull();
    }

    @Test
    void marcarLeida_inexistente_lanzaNoSuchElement() {
        when(notificacionUsuarioRepository
            .findByIdNotificacionUsuarioAndUsuarioIdUsuario(3L, 1L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.marcarLeida(3L, 1L))
            .isInstanceOf(NoSuchElementException.class);
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private Page<NotificacionUsuario> pagina(NotificacionUsuario... filas) {
        return new PageImpl<>(List.of(filas), PageRequest.of(0, 20), filas.length);
    }

    private Usuario usuario(Long id) {
        return Usuario.builder().idUsuario(id).nombreUsuario("u" + id).build();
    }

    private TipoNotificacion tipo(String codigo) {
        TipoNotificacion tipo = new TipoNotificacion();
        tipo.setCodigo(codigo);
        tipo.setNombre(codigo);
        return tipo;
    }

    private Notificacion notificacion(Long id, String tipoReferencia, Long idReferencia) {
        Notificacion notificacion = new Notificacion();
        notificacion.setIdNotificacion(id);
        notificacion.setNombre("Nombre");
        notificacion.setDescripcion("Descripción");
        notificacion.setEstado("ACTIVA");
        notificacion.setTipoNotificacion(tipo("T"));
        notificacion.setTipoReferencia(tipoReferencia);
        notificacion.setIdReferencia(idReferencia);
        notificacion.setFechaCreacion(LocalDateTime.of(2026, 1, 1, 10, 0));
        return notificacion;
    }

    private NotificacionUsuario destinatario(Notificacion notificacion, Long id) {
        NotificacionUsuario destinatario = new NotificacionUsuario();
        destinatario.setIdNotificacionUsuario(id);
        destinatario.setUsuario(usuario(1L));
        destinatario.setNotificacion(notificacion);
        return destinatario;
    }
}
