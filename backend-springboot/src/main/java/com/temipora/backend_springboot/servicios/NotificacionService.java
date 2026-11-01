package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.Notificacion;
import com.temipora.backend_springboot.modelo.NotificacionDTO;
import com.temipora.backend_springboot.modelo.NotificacionUsuario;
import com.temipora.backend_springboot.modelo.TipoNotificacion;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.NotificacionRepository;
import com.temipora.backend_springboot.repositorios.NotificacionUsuarioRepository;
import com.temipora.backend_springboot.repositorios.PedidoRepository;
import com.temipora.backend_springboot.repositorios.SolicitudRepository;
import com.temipora.backend_springboot.repositorios.TipoNotificacionRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@Service
public class NotificacionService {

    private static final int LARGO_NOMBRE = 100;
    private static final int LARGO_DESCRIPCION = 250;

    private final NotificacionRepository notificacionRepository;
    private final NotificacionUsuarioRepository notificacionUsuarioRepository;
    private final TipoNotificacionRepository tipoNotificacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final PedidoRepository pedidoRepository;
    private final SolicitudRepository solicitudRepository;

    public NotificacionService(
            NotificacionRepository notificacionRepository,
            NotificacionUsuarioRepository notificacionUsuarioRepository,
            TipoNotificacionRepository tipoNotificacionRepository,
            UsuarioRepository usuarioRepository,
            PedidoRepository pedidoRepository,
            SolicitudRepository solicitudRepository) {
        this.notificacionRepository = notificacionRepository;
        this.notificacionUsuarioRepository = notificacionUsuarioRepository;
        this.tipoNotificacionRepository = tipoNotificacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.pedidoRepository = pedidoRepository;
        this.solicitudRepository = solicitudRepository;
    }

    @Transactional
    public NotificacionDTO crearParaUsuario(
            Long idUsuario,
            String codigoTipo,
            String nombre,
            String descripcion,
            Long idReferencia,
            String tipoReferencia) {
        TipoNotificacion tipo = tipoNotificacionRepository.findByCodigo(codigoTipo)
            .orElseThrow(() -> new IllegalStateException(
                "No existe el tipo de notificación: " + codigoTipo));
        Usuario usuario = usuarioRepository.findByIdUsuario(idUsuario)
            .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado: " + idUsuario));

        Notificacion notificacion = new Notificacion();
        notificacion.setTipoNotificacion(tipo);
        notificacion.setEsSistema(true);
        notificacion.setEstado("ACTIVA");
        notificacion.setNombre(limitar(nombre, LARGO_NOMBRE));
        notificacion.setDescripcion(limitar(descripcion, LARGO_DESCRIPCION));
        notificacion.setIdReferencia(idReferencia);
        notificacion.setTipoReferencia(tipoReferencia);

        NotificacionUsuario destinatario = new NotificacionUsuario();
        destinatario.setUsuario(usuario);
        notificacion.agregarDestinatario(destinatario);

        Notificacion guardada = notificacionRepository.save(notificacion);
        return NotificacionDTO.desdeNotificacionUsuario(guardada.getDestinatarios().get(0));
    }

    @Transactional(readOnly = true)
    public List<NotificacionDTO> listar(Long idUsuario) {
        return notificacionUsuarioRepository.listarPorUsuario(idUsuario).stream()
            .map(destinatario -> aDTO(destinatario, idUsuario))
            .toList();
    }

    @Transactional(readOnly = true)
    public long contarNoLeidas(Long idUsuario) {
        return notificacionUsuarioRepository.countByUsuarioIdUsuarioAndLeidaFalse(idUsuario);
    }

    @Transactional
    public NotificacionDTO marcarLeida(Long idNotificacionUsuario, Long idUsuario) {
        NotificacionUsuario destinatario = notificacionUsuarioRepository
            .findByIdNotificacionUsuarioAndUsuarioIdUsuario(idNotificacionUsuario, idUsuario)
            .orElseThrow(() -> new NoSuchElementException(
                "Notificación no encontrada: " + idNotificacionUsuario));
        if (!destinatario.isLeida()) {
            destinatario.setLeida(true);
            destinatario.setFechaLectura(LocalDateTime.now());
        }
        return aDTO(destinatario, idUsuario);
    }

    private NotificacionDTO aDTO(NotificacionUsuario destinatario, Long idUsuario) {
        NotificacionDTO dto = NotificacionDTO.desdeNotificacionUsuario(destinatario);
        completarRolReferencia(dto, idUsuario);
        return dto;
    }

    private void completarRolReferencia(NotificacionDTO dto, Long idUsuario) {
        if (dto == null || idUsuario == null
                || dto.getTipoReferencia() == null || dto.getIdReferencia() == null) {
            return;
        }
        String rol = switch (dto.getTipoReferencia()) {
            case "PEDIDO" -> rolEnPedido(dto.getIdReferencia(), idUsuario);
            case "SOLICITUD" -> rolEnSolicitud(dto.getIdReferencia(), idUsuario);
            default -> null;
        };
        dto.setRolReferencia(rol);
    }

    private String rolEnPedido(Long idPedido, Long idUsuario) {
        return pedidoRepository.findById(idPedido)
            .map(pedido -> rolEnReferencia(
                idUsuario, pedido.getArtista(), pedido.getCliente()))
            .orElse(null);
    }

    private String rolEnSolicitud(Long idSolicitud, Long idUsuario) {
        return solicitudRepository.findById(idSolicitud)
            .map(solicitud -> rolEnReferencia(
                idUsuario, solicitud.getArtista(), solicitud.getCliente()))
            .orElse(null);
    }

    private String rolEnReferencia(Long idUsuario, Usuario artista, Usuario cliente) {
        if (artista != null && idUsuario.equals(artista.getIdUsuario())) {
            return "ARTISTA";
        }
        if (cliente != null && idUsuario.equals(cliente.getIdUsuario())) {
            return "CLIENTE";
        }
        return null;
    }

    private String limitar(String valor, int largo) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.length() <= largo ? limpio : limpio.substring(0, largo);
    }
}
