package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.ContadorNotificacionesDTO;
import com.temipora.backend_springboot.modelo.Conversacion;
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

@Service
public class NotificacionService {

    public static final int TAMANIO_MAXIMO = 50;
    public static final int LIMITE_RECIENTES = 8;

    private static final int LARGO_NOMBRE = 100;
    private static final int LARGO_DESCRIPCION = 250;
    private static final int LIMITE_RECIENTES_MAXIMO = 20;

    private final NotificacionRepository notificacionRepository;
    private final NotificacionUsuarioRepository notificacionUsuarioRepository;
    private final TipoNotificacionRepository tipoNotificacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final PedidoRepository pedidoRepository;
    private final SolicitudRepository solicitudRepository;
    private final ConversacionRepository conversacionRepository;

    public NotificacionService(
            NotificacionRepository notificacionRepository,
            NotificacionUsuarioRepository notificacionUsuarioRepository,
            TipoNotificacionRepository tipoNotificacionRepository,
            UsuarioRepository usuarioRepository,
            PedidoRepository pedidoRepository,
            SolicitudRepository solicitudRepository,
            ConversacionRepository conversacionRepository) {
        this.notificacionRepository = notificacionRepository;
        this.notificacionUsuarioRepository = notificacionUsuarioRepository;
        this.tipoNotificacionRepository = tipoNotificacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.pedidoRepository = pedidoRepository;
        this.solicitudRepository = solicitudRepository;
        this.conversacionRepository = conversacionRepository;
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
    public PaginaDTO<NotificacionDTO> listar(
            Long idUsuario,
            String categoria,
            boolean soloNoLeidas,
            int pagina,
            int tamanio) {
        int paginaSegura = Math.max(0, pagina);
        int tamanioSeguro = Math.min(Math.max(1, tamanio), TAMANIO_MAXIMO);
        Page<NotificacionUsuario> page = notificacionUsuarioRepository.listarPaginado(
            idUsuario,
            normalizarCategoria(categoria),
            soloNoLeidas,
            PageRequest.of(paginaSegura, tamanioSeguro));
        return paginar(page, aDTOs(page.getContent(), idUsuario));
    }

    @Transactional(readOnly = true)
    public List<NotificacionDTO> recientes(Long idUsuario, Integer limite) {
        int tope = limite == null
            ? LIMITE_RECIENTES
            : Math.min(Math.max(1, limite), LIMITE_RECIENTES_MAXIMO);
        return aDTOs(
            notificacionUsuarioRepository.listarRecientes(
                idUsuario, PageRequest.of(0, tope)),
            idUsuario);
    }

    @Transactional(readOnly = true)
    public ContadorNotificacionesDTO contador(Long idUsuario) {
        Map<String, Long> porCategoria = new LinkedHashMap<>();
        for (Object[] fila : notificacionUsuarioRepository
                .contarNoLeidasPorCategoria(idUsuario)) {
            String categoria = (String) fila[0];
            if (categoria != null) {
                porCategoria.put(categoria, ((Number) fila[1]).longValue());
            }
        }
        return new ContadorNotificacionesDTO(
            notificacionUsuarioRepository.countByUsuarioIdUsuarioAndLeidaFalse(idUsuario),
            porCategoria);
    }

    @Transactional
    public int marcarTodasLeidas(Long idUsuario) {
        return notificacionUsuarioRepository.marcarTodasLeidas(
            idUsuario, LocalDateTime.now());
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

    private List<NotificacionDTO> aDTOs(
            List<NotificacionUsuario> destinatarios,
            Long idUsuario) {
        Set<Long> idsPedidos = idsDe(destinatarios, "PEDIDO");
        Map<Long, Pedido> pedidos = idsPedidos.isEmpty()
            ? Map.of()
            : pedidoRepository.findAllById(idsPedidos).stream()
                .collect(Collectors.toMap(Pedido::getIdPedido, pedido -> pedido));

        Set<Long> idsSolicitudes = idsDe(destinatarios, "SOLICITUD");
        Map<Long, Solicitud> solicitudes = idsSolicitudes.isEmpty()
            ? Map.of()
            : solicitudRepository.findAllById(idsSolicitudes).stream()
                .collect(Collectors.toMap(Solicitud::getIdSolicitud, solicitud -> solicitud));

        Set<Long> idsConversaciones = idsDe(destinatarios, "CONVERSACION");
        Map<Long, Conversacion> conversaciones = idsConversaciones.isEmpty()
            ? Map.of()
            : conversacionRepository.findAllById(idsConversaciones).stream()
                .collect(Collectors.toMap(
                    Conversacion::getIdConversacion, conversacion -> conversacion));

        return destinatarios.stream()
            .map(destinatario -> aDTO(
                destinatario, idUsuario, pedidos, solicitudes, conversaciones))
            .toList();
    }

    private Set<Long> idsDe(
            List<NotificacionUsuario> destinatarios,
            String tipoReferencia) {
        Set<Long> ids = new HashSet<>();
        for (NotificacionUsuario destinatario : destinatarios) {
            Notificacion notificacion = destinatario.getNotificacion();
            if (notificacion == null
                    || !tipoReferencia.equals(notificacion.getTipoReferencia())
                    || notificacion.getIdReferencia() == null) {
                continue;
            }
            ids.add(notificacion.getIdReferencia());
        }
        return ids;
    }

    private NotificacionDTO aDTO(NotificacionUsuario destinatario, Long idUsuario) {
        return aDTOs(List.of(destinatario), idUsuario).get(0);
    }

    private NotificacionDTO aDTO(
            NotificacionUsuario destinatario,
            Long idUsuario,
            Map<Long, Pedido> pedidos,
            Map<Long, Solicitud> solicitudes,
            Map<Long, Conversacion> conversaciones) {
        NotificacionDTO dto = NotificacionDTO.desdeNotificacionUsuario(destinatario);
        Notificacion notificacion = destinatario.getNotificacion();
        if (notificacion == null || idUsuario == null) {
            return dto;
        }
        String tipoReferencia = notificacion.getTipoReferencia();
        Long idReferencia = notificacion.getIdReferencia();
        if (tipoReferencia == null || idReferencia == null) {
            return dto;
        }

        switch (tipoReferencia) {
            case "PEDIDO" -> {
                Pedido pedido = pedidos.get(idReferencia);
                if (pedido != null) {
                    dto.setRolReferencia(rolEnReferencia(
                        idUsuario, pedido.getArtista(), pedido.getCliente()));
                }
            }
            case "SOLICITUD" -> {
                Solicitud solicitud = solicitudes.get(idReferencia);
                if (solicitud != null) {
                    dto.setRolReferencia(rolEnReferencia(
                        idUsuario, solicitud.getArtista(), solicitud.getCliente()));
                }
            }
            case "CONVERSACION" -> {
                Conversacion conversacion = conversaciones.get(idReferencia);
                if (conversacion != null && conversacion.involucra(idUsuario)) {
                    Usuario contraparte = conversacion.contraparteDe(idUsuario);
                    if (contraparte != null) {
                        dto.setNombreUsuarioReferencia(contraparte.getNombreUsuario());
                    }
                }
            }
            default -> {
                // Sin rol asociado para este tipo de referencia.
            }
        }
        return dto;
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

    private <T> PaginaDTO<T> paginar(Page<?> page, List<T> contenido) {
        return PaginaDTO.<T>builder()
            .contenido(contenido)
            .pagina(page.getNumber())
            .tamanio(page.getSize())
            .totalElementos(page.getTotalElements())
            .totalPaginas(page.getTotalPages())
            .hayMas(page.hasNext())
            .build();
    }

    private String normalizarCategoria(String categoria) {
        if (categoria == null) {
            return "";
        }
        String limpia = categoria.trim().toUpperCase(Locale.ROOT);
        if (limpia.length() > 30) {
            throw new IllegalArgumentException("Categoría de notificación inválida.");
        }
        return limpia;
    }

    private String limitar(String valor, int largo) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.length() <= largo ? limpio : limpio.substring(0, largo);
    }
}
