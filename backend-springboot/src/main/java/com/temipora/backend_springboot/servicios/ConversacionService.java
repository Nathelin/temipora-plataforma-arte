package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.eventos.ConversacionActualizadaEvent;
import com.temipora.backend_springboot.eventos.EscrituraEvent;
import com.temipora.backend_springboot.modelo.BandejaMensajes;
import com.temipora.backend_springboot.modelo.ConfiguracionMensajeria;
import com.temipora.backend_springboot.modelo.ContadorMensajesDTO;
import com.temipora.backend_springboot.modelo.Conversacion;
import com.temipora.backend_springboot.modelo.ConversacionAccion;
import com.temipora.backend_springboot.modelo.ConversacionDTO;
import com.temipora.backend_springboot.modelo.ConversacionParticipante;
import com.temipora.backend_springboot.modelo.ConversacionResumenDTO;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.EscribiendoDTO;
import com.temipora.backend_springboot.modelo.EstadoConversacion;
import com.temipora.backend_springboot.modelo.EstadoMensaje;
import com.temipora.backend_springboot.modelo.EstadoRelacionMensaje;
import com.temipora.backend_springboot.modelo.EstadoRelacionMensajeDTO;
import com.temipora.backend_springboot.modelo.Mensaje;
import com.temipora.backend_springboot.modelo.OrigenConversacion;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.RechazoConversacionRequest;
import com.temipora.backend_springboot.modelo.SolicitudMensajeRequest;
import com.temipora.backend_springboot.modelo.TipoAccionConversacion;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.modelo.UsuarioResumenDTO;
import com.temipora.backend_springboot.repositorios.ConversacionAccionRepository;
import com.temipora.backend_springboot.repositorios.ConversacionParticipanteRepository;
import com.temipora.backend_springboot.repositorios.ConversacionRepository;
import com.temipora.backend_springboot.repositorios.MensajeRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@Service
public class ConversacionService {

    public static final String TIPO_REFERENCIA_CONVERSACION = "CONVERSACION";
    public static final String NOTIFICACION_SOLICITUD = "SOLICITUD_MENSAJE_RECIBIDA";
    public static final String NOTIFICACION_ACEPTADA = "SOLICITUD_MENSAJE_ACEPTADA";
    public static final String NOTIFICACION_RECHAZADA = "SOLICITUD_MENSAJE_RECHAZADA";
    public static final String NOTIFICACION_CANCELADA = "SOLICITUD_MENSAJE_CANCELADA";
    public static final String NOTIFICACION_VENCIDA = "SOLICITUD_MENSAJE_VENCIDA";

    private static final int TAMANIO_MAXIMO_BANDEJA = 50;
    private static final int LARGO_PREVIEW = 120;
    private static final DateTimeFormatter FORMATO_FECHA =
        DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm");

    private final ConversacionRepository conversacionRepository;
    private final ConversacionParticipanteRepository participanteRepository;
    private final ConversacionAccionRepository accionRepository;
    private final MensajeRepository mensajeRepository;
    private final UsuarioRepository usuarioRepository;
    private final ConfiguracionMensajeriaService configuracionService;
    private final NotificacionService notificacionService;
    private final IndicadorEscritura indicadorEscritura;
    private final ApplicationEventPublisher eventoPublisher;
    private final MoraClienteService moraClienteService;

    public ConversacionService(
            ConversacionRepository conversacionRepository,
            ConversacionParticipanteRepository participanteRepository,
            ConversacionAccionRepository accionRepository,
            MensajeRepository mensajeRepository,
            UsuarioRepository usuarioRepository,
            ConfiguracionMensajeriaService configuracionService,
            NotificacionService notificacionService,
            IndicadorEscritura indicadorEscritura,
            ApplicationEventPublisher eventoPublisher,
            MoraClienteService moraClienteService) {
        this.conversacionRepository = conversacionRepository;
        this.participanteRepository = participanteRepository;
        this.accionRepository = accionRepository;
        this.mensajeRepository = mensajeRepository;
        this.usuarioRepository = usuarioRepository;
        this.configuracionService = configuracionService;
        this.notificacionService = notificacionService;
        this.indicadorEscritura = indicadorEscritura;
        this.eventoPublisher = eventoPublisher;
        this.moraClienteService = moraClienteService;
    }

    @Transactional
    public ConversacionDTO crear(Long idRemitente, SolicitudMensajeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException(
                "Los datos de la solicitud son obligatorios.");
        }
        ConfiguracionMensajeria config = configuracionService.obtener();
        Usuario remitente = obtenerUsuario(idRemitente);
        validarRemitente(remitente);

        String nombreDestinatario = TextoMensajeria.obligatorio(
            request.getNombreUsuarioDestinatario(), 25, "El usuario destinatario");
        Usuario destinatario = usuarioRepository
            .findByNombreUsuarioIgnoreCase(nombreDestinatario)
            .orElseThrow(() -> new NoSuchElementException(
                "No existe el usuario @" + nombreDestinatario + "."));
        validarDestinatario(remitente, destinatario);
        if (!moraClienteService.chatPermitido(
                remitente.getIdUsuario(), destinatario.getIdUsuario())) {
            throw new ConflictoException(
                MoraClienteService.MENSAJE_CHAT_BLOQUEADO);
        }

        String motivo = TextoMensajeria.obligatorio(
            request.getMotivoContacto(), config.getLongitudMaximaMotivo(),
            "El motivo de contacto");
        String primerMensaje = TextoMensajeria.obligatorio(
            request.getPrimerMensaje(), config.getLongitudMaximaMensaje(),
            "El primer mensaje");

        Optional<Conversacion> existente = conversacionRepository
            .buscarPorPar(remitente.getIdUsuario(), destinatario.getIdUsuario());
        if (existente.isPresent()) {
            Conversacion conversacion = existente.get();
            switch (conversacion.getEstado()) {
                case PENDIENTE:
                    throw new ConflictoException(
                        "Ya tenés una solicitud pendiente con @"
                            + destinatario.getNombreUsuario() + ".");
                case ACEPTADA:
                    throw new ConflictoException(
                        "Ya tenés una conversación con @"
                            + destinatario.getNombreUsuario() + ".");
                case RECHAZADA: {
                    LocalDateTime disponible = fechaDisponibleReenvio(conversacion, config);
                    if (disponible != null && LocalDateTime.now().isBefore(disponible)) {
                        throw new ConflictoException(
                            "Podés volver a enviar la solicitud el "
                                + formatearFecha(disponible) + ".");
                    }
                    return reenviar(
                        conversacion, remitente, destinatario, motivo, primerMensaje, config);
                }
                case CANCELADA:
                case VENCIDA:
                    return reenviar(
                        conversacion, remitente, destinatario, motivo, primerMensaje, config);
                default:
                    throw new IllegalStateException(
                        "Estado de conversación no soportado: "
                            + conversacion.getEstado());
            }
        }

        validarCuposSolicitudes(remitente.getIdUsuario(), config);

        LocalDateTime ahora = LocalDateTime.now();
        Conversacion conversacion = new Conversacion();
        conversacion.setIniciador(remitente);
        conversacion.setDestinatario(destinatario);
        conversacion.setEstado(EstadoConversacion.PENDIENTE);
        conversacion.setMotivoContacto(motivo);
        conversacion.setFechaSolicitud(ahora);
        conversacion.setFechaActualizacion(ahora);
        conversacionRepository.save(conversacion);

        asegurarParticipante(conversacion, remitente);
        asegurarParticipante(conversacion, destinatario);

        Mensaje mensaje = guardarMensaje(conversacion, remitente, primerMensaje, ahora);
        conversacion.setFechaUltimoMensaje(mensaje.getFechaEnvio());

        registrarAccion(conversacion, TipoAccionConversacion.SOLICITUD, remitente, null);
        notificarSolicitud(destinatario, remitente, conversacion, motivo);
        publicarCambio(conversacion);

        return aDTO(conversacion, remitente.getIdUsuario(), config);
    }

    @Transactional
    public void asegurarPorSolicitud(
            Usuario cliente,
            Usuario artista,
            Long idSolicitud,
            String nombreServicio) {
        if (!participantesValidos(cliente, artista)
                || !moraClienteService.chatPermitido(
                    cliente.getIdUsuario(), artista.getIdUsuario())) {
            return;
        }

        LocalDateTime ahora = LocalDateTime.now();
        String motivo = motivoAperturaPorSolicitud(idSolicitud, nombreServicio);
        Optional<Conversacion> existente = conversacionRepository
            .buscarPorPar(cliente.getIdUsuario(), artista.getIdUsuario());

        if (existente.isEmpty()) {
            Conversacion conversacion = new Conversacion();
            conversacion.setIniciador(cliente);
            conversacion.setDestinatario(artista);
            conversacion.setEstado(EstadoConversacion.ACEPTADA);
            conversacion.setOrigen(OrigenConversacion.SOLICITUD_SERVICIO);
            conversacion.setMotivoContacto(motivo);
            conversacion.setFechaSolicitud(ahora);
            conversacion.setFechaRespuesta(ahora);
            conversacion.setFechaActualizacion(ahora);
            conversacionRepository.save(conversacion);

            asegurarParticipante(conversacion, cliente);
            asegurarParticipante(conversacion, artista);
            registrarAccion(
                conversacion, TipoAccionConversacion.APERTURA_AUTOMATICA, cliente, motivo);
            publicarCambio(conversacion);
            return;
        }

        Conversacion conversacion = existente.get();
        EstadoConversacion anterior = conversacion.getEstado();
        boolean reabierta = anterior != EstadoConversacion.ACEPTADA;
        if (reabierta) {
            conversacion.setEstado(EstadoConversacion.ACEPTADA);
            conversacion.setOrigen(OrigenConversacion.SOLICITUD_SERVICIO);
            conversacion.setMotivoContacto(motivo);
            conversacion.setFechaSolicitud(ahora);
            conversacion.setFechaRespuesta(ahora);
            conversacion.setFechaActualizacion(ahora);
            registrarAccion(
                conversacion,
                TipoAccionConversacion.APERTURA_AUTOMATICA,
                cliente,
                motivoReaperturaPorSolicitud(idSolicitud, anterior));
        }

        boolean desarchivada = desarchivar(conversacion, cliente);
        desarchivada |= desarchivar(conversacion, artista);
        if (reabierta || desarchivada) {
            publicarCambio(conversacion);
        }
    }

    @Transactional
    public void archivarPorSolicitud(Usuario cliente, Usuario artista) {
        if (!participantesValidos(cliente, artista)) {
            return;
        }

        Optional<Conversacion> existente = conversacionRepository
            .buscarPorPar(cliente.getIdUsuario(), artista.getIdUsuario());
        if (existente.isEmpty()) {
            return;
        }

        Conversacion conversacion = existente.get();
        if (conversacion.getOrigen() != OrigenConversacion.SOLICITUD_SERVICIO) {
            return;
        }

        boolean archivada = archivar(conversacion, cliente);
        archivada |= archivar(conversacion, artista);
        if (archivada) {
            publicarCambio(conversacion);
        }
    }

    @Transactional
    public ConversacionDTO aceptar(Long idConversacion, Long idUsuario) {
        Conversacion conversacion = obtenerParaActualizar(idConversacion);
        Usuario usuario = obtenerUsuario(idUsuario);
        if (!conversacion.esDestinatario(idUsuario)) {
            throw new AccesoDenegadoException(
                "No podés aceptar esta solicitud de mensaje.");
        }
        if (conversacion.getEstado() == EstadoConversacion.ACEPTADA) {
            throw new ConflictoException("La solicitud ya fue aceptada.");
        }
        if (conversacion.getEstado() != EstadoConversacion.PENDIENTE) {
            throw new ConflictoException(
                "La solicitud ya no está pendiente.");
        }
        validarCuentaActiva(usuario);
        if (!conversacion.getIniciador().isEstadoCuenta()) {
            throw new ConflictoException(
                "El usuario que envió la solicitud ya no está disponible.");
        }

        LocalDateTime ahora = LocalDateTime.now();
        conversacion.setEstado(EstadoConversacion.ACEPTADA);
        conversacion.setFechaRespuesta(ahora);
        conversacion.setFechaActualizacion(ahora);

        ConversacionParticipante participante =
            asegurarParticipante(conversacion, usuario);
        participante.setUltimoLeidoEn(ahora);
        participante.setArchivada(false);

        registrarAccion(
            conversacion, TipoAccionConversacion.ACEPTACION, usuario, null);
        notificacionService.crearParaUsuario(
            conversacion.getIniciador().getIdUsuario(),
            NOTIFICACION_ACEPTADA,
            "Solicitud de mensaje aceptada",
            "@" + usuario.getNombreUsuario() + " aceptó tu solicitud de mensaje.",
            idConversacion,
            TIPO_REFERENCIA_CONVERSACION);
        publicarCambio(conversacion);

        return aDTO(conversacion, idUsuario, configuracionService.obtener());
    }

    @Transactional
    public ConversacionDTO rechazar(
            Long idConversacion,
            Long idUsuario,
            RechazoConversacionRequest request) {
        Conversacion conversacion = obtenerParaActualizar(idConversacion);
        Usuario usuario = obtenerUsuario(idUsuario);
        if (!conversacion.esDestinatario(idUsuario)) {
            throw new AccesoDenegadoException(
                "No podés rechazar esta solicitud de mensaje.");
        }
        if (conversacion.getEstado() != EstadoConversacion.PENDIENTE) {
            throw new ConflictoException("La solicitud ya no está pendiente.");
        }
        validarCuentaActiva(usuario);

        ConfiguracionMensajeria config = configuracionService.obtener();
        String motivo = TextoMensajeria.opcional(
            request != null ? request.getMotivo() : null,
            config.getLongitudMaximaMotivo(),
            "El motivo del rechazo");

        LocalDateTime ahora = LocalDateTime.now();
        conversacion.setEstado(EstadoConversacion.RECHAZADA);
        conversacion.setFechaRespuesta(ahora);
        conversacion.setFechaActualizacion(ahora);

        registrarAccion(
            conversacion, TipoAccionConversacion.RECHAZO, usuario, motivo);
        notificacionService.crearParaUsuario(
            conversacion.getIniciador().getIdUsuario(),
            NOTIFICACION_RECHAZADA,
            "Solicitud de mensaje rechazada",
            "@" + usuario.getNombreUsuario() + " rechazó tu solicitud de mensaje.",
            idConversacion,
            TIPO_REFERENCIA_CONVERSACION);
        publicarCambio(conversacion);

        return aDTO(conversacion, idUsuario, config);
    }

    @Transactional
    public ConversacionDTO cancelar(Long idConversacion, Long idUsuario) {
        Conversacion conversacion = obtenerParaActualizar(idConversacion);
        Usuario usuario = obtenerUsuario(idUsuario);
        if (!conversacion.esIniciador(idUsuario)) {
            throw new AccesoDenegadoException(
                "No podés cancelar esta solicitud de mensaje.");
        }
        if (conversacion.getEstado() != EstadoConversacion.PENDIENTE) {
            throw new ConflictoException("La solicitud ya no está pendiente.");
        }

        LocalDateTime ahora = LocalDateTime.now();
        conversacion.setEstado(EstadoConversacion.CANCELADA);
        conversacion.setFechaRespuesta(ahora);
        conversacion.setFechaActualizacion(ahora);

        registrarAccion(
            conversacion, TipoAccionConversacion.CANCELACION, usuario, null);
        notificacionService.crearParaUsuario(
            conversacion.getDestinatario().getIdUsuario(),
            NOTIFICACION_CANCELADA,
            "Solicitud de mensaje cancelada",
            "@" + usuario.getNombreUsuario() + " canceló su solicitud de mensaje.",
            idConversacion,
            TIPO_REFERENCIA_CONVERSACION);
        publicarCambio(conversacion);

        return aDTO(conversacion, idUsuario, configuracionService.obtener());
    }

    @Transactional
    public ConversacionDTO archivar(
            Long idConversacion,
            Long idUsuario,
            boolean archivada) {
        Conversacion conversacion = obtenerConversacion(idConversacion);
        validarParticipante(conversacion, idUsuario);
        ConversacionParticipante participante =
            participanteRepository
                .findByConversacionIdConversacionAndUsuarioIdUsuario(
                    idConversacion, idUsuario)
                .orElseThrow(() -> new NoSuchElementException(
                    "Conversación no encontrada: " + idConversacion));
        participante.setArchivada(archivada);
        return aDTO(conversacion, idUsuario, configuracionService.obtener());
    }

    @Transactional(readOnly = true)
    public ConversacionDTO obtener(Long idConversacion, Long idUsuario) {
        Conversacion conversacion = obtenerConversacion(idConversacion);
        validarParticipante(conversacion, idUsuario);
        return aDTO(conversacion, idUsuario, configuracionService.obtener());
    }

    @Transactional(readOnly = true)
    public PaginaDTO<ConversacionResumenDTO> listar(
            Long idUsuario,
            EstadoConversacion estado,
            boolean archivadas,
            BandejaMensajes bandeja,
            int pagina,
            int tamanio) {
        int paginaSegura = Math.max(0, pagina);
        int tamanioSeguro = Math.min(Math.max(1, tamanio), TAMANIO_MAXIMO_BANDEJA);
        Collection<EstadoConversacion> estados = estado != null
            ? List.of(estado)
            : List.of(EstadoConversacion.values());
        boolean soloRecibidas = bandeja == BandejaMensajes.RECIBIDAS;
        boolean soloEnviadas = bandeja == BandejaMensajes.ENVIADAS;

        Page<ConversacionParticipante> page = participanteRepository.listarBandeja(
            idUsuario,
            archivadas,
            estados,
            soloRecibidas,
            soloEnviadas,
            PageRequest.of(paginaSegura, tamanioSeguro));

        List<ConversacionParticipante> filas = page.getContent();
        if (filas.isEmpty()) {
            return paginar(page, List.<ConversacionResumenDTO>of());
        }

        List<Long> ids = filas.stream()
            .map(fila -> fila.getConversacion().getIdConversacion())
            .toList();
        Map<Long, Long> noLeidos = noLeidosPorConversacion(idUsuario, ids);
        Map<Long, Mensaje> ultimos = ultimosMensajes(ids);

        List<ConversacionResumenDTO> contenido = filas.stream()
            .map(fila -> {
                Conversacion conversacion = fila.getConversacion();
                Mensaje ultimo = ultimos.get(conversacion.getIdConversacion());
                return ConversacionResumenDTO.builder()
                    .idConversacion(conversacion.getIdConversacion())
                    .estado(conversacion.getEstado())
                    .motivoContacto(conversacion.getMotivoContacto())
                    .contraparte(UsuarioResumenDTO.desdeUsuario(
                        conversacion.contraparteDe(idUsuario)))
                    .soyIniciador(conversacion.esIniciador(idUsuario))
                    .fechaSolicitud(conversacion.getFechaSolicitud())
                    .fechaUltimoMensaje(conversacion.getFechaUltimoMensaje())
                    .ultimoMensajePreview(ultimo != null
                        ? TextoMensajeria.recortar(ultimo.getContenido(), LARGO_PREVIEW)
                        : null)
                    .noLeidos(noLeidos.getOrDefault(
                        conversacion.getIdConversacion(), 0L))
                    .archivada(fila.isArchivada())
                    .build();
            })
            .toList();

        return paginar(page, contenido);
    }

    @Transactional(readOnly = true)
    public EstadoRelacionMensajeDTO estadoConUsuario(
            Long idUsuario,
            String nombreUsuario) {
        ConfiguracionMensajeria config = configuracionService.obtener();
        Usuario yo = obtenerUsuario(idUsuario);
        Usuario otro = usuarioRepository
            .findByNombreUsuarioIgnoreCase(nombreUsuario)
            .orElseThrow(() -> new NoSuchElementException(
                "No existe el usuario @" + nombreUsuario + "."));

        if (yo.getRol() == null || yo.getRol().getNombre() == ERol.ADMIN) {
            return relacion(EstadoRelacionMensaje.NINGUNA, null, false,
                "La mensajería privada es entre artistas y clientes.", null);
        }
        if (!yo.isEstadoCuenta()) {
            return relacion(EstadoRelacionMensaje.NINGUNA, null, false,
                "Tu cuenta está deshabilitada para enviar mensajes.", null);
        }
        if (otro.getIdUsuario().equals(idUsuario)) {
            return relacion(EstadoRelacionMensaje.NINGUNA, null, false,
                "No podés enviarte un mensaje a vos mismo.", null);
        }
        if (otro.getRol() == null
                || otro.getRol().getNombre() == ERol.ADMIN
                || !otro.isEstadoCuenta()) {
            return relacion(EstadoRelacionMensaje.NINGUNA, null, false,
                "El usuario no está disponible para recibir mensajes.", null);
        }

        if (!moraClienteService.chatPermitido(
                idUsuario, otro.getIdUsuario())) {
            return conContraparte(
                relacion(EstadoRelacionMensaje.NINGUNA, null, false,
                    MoraClienteService.MENSAJE_CHAT_BLOQUEADO, null),
                otro);
        }

        Optional<Conversacion> existente = conversacionRepository
            .buscarPorPar(idUsuario, otro.getIdUsuario());
        if (existente.isEmpty()) {
            String motivo = motivoCupoSolicitudes(idUsuario, config);
            return conContraparte(
                relacion(EstadoRelacionMensaje.NINGUNA, null,
                    motivo == null, motivo, null),
                otro);
        }

        Conversacion conversacion = existente.get();
        return conContraparte(switch (conversacion.getEstado()) {
            case ACEPTADA -> relacion(
                EstadoRelacionMensaje.ACEPTADA,
                conversacion.getIdConversacion(),
                false,
                "Ya tenés una conversación con este usuario.",
                null);
            case PENDIENTE -> conversacion.esIniciador(idUsuario)
                ? relacion(
                    EstadoRelacionMensaje.PENDIENTE_ENVIADA,
                    conversacion.getIdConversacion(),
                    false,
                    "Ya enviaste una solicitud a este usuario.",
                    null)
                : relacion(
                    EstadoRelacionMensaje.PENDIENTE_RECIBIDA,
                    conversacion.getIdConversacion(),
                    false,
                    "Este usuario te envió una solicitud de mensaje.",
                    null);
            case CANCELADA -> {
                String motivo = motivoCupoSolicitudes(idUsuario, config);
                yield relacion(
                    EstadoRelacionMensaje.CANCELADA,
                    conversacion.getIdConversacion(),
                    motivo == null,
                    motivo,
                    null);
            }
            case VENCIDA -> {
                String motivo = motivoCupoSolicitudes(idUsuario, config);
                yield relacion(
                    EstadoRelacionMensaje.VENCIDA,
                    conversacion.getIdConversacion(),
                    motivo == null,
                    motivo,
                    null);
            }
            case RECHAZADA -> {
                LocalDateTime disponible =
                    fechaDisponibleReenvio(conversacion, config);
                if (disponible != null && LocalDateTime.now().isBefore(disponible)) {
                    yield relacion(
                        EstadoRelacionMensaje.RECHAZADA,
                        conversacion.getIdConversacion(),
                        false,
                        "Podés volver a enviar la solicitud el "
                            + formatearFecha(disponible) + ".",
                        disponible);
                }
                String motivo = motivoCupoSolicitudes(idUsuario, config);
                yield relacion(
                    EstadoRelacionMensaje.RECHAZADA,
                    conversacion.getIdConversacion(),
                    motivo == null,
                    motivo,
                    disponible);
            }
        }, otro);
    }

    @Transactional(readOnly = true)
    public ContadorMensajesDTO contador(Long idUsuario) {
        long pendientes = conversacionRepository
            .countByDestinatarioIdUsuarioAndEstado(
                idUsuario, EstadoConversacion.PENDIENTE);
        long noLeidos = mensajeRepository.contarNoLeidos(
            idUsuario,
            EstadoMensaje.ENVIADO,
            EstadoConversacion.ACEPTADA);
        return ContadorMensajesDTO.builder()
            .solicitudesPendientes(pendientes)
            .noLeidos(noLeidos)
            .build();
    }

    @Transactional
    public int vencerPendientes() {
        ConfiguracionMensajeria config = configuracionService.obtener();
        int dias = config.getDiasVencimientoSolicitud() != null
            ? config.getDiasVencimientoSolicitud()
            : 0;
        if (dias <= 0) {
            return 0;
        }

        LocalDateTime limite = LocalDateTime.now().minusDays(dias);
        List<Conversacion> candidatas = conversacionRepository
            .findByEstadoAndFechaSolicitudBefore(
                EstadoConversacion.PENDIENTE, limite);
        int vencidas = 0;
        for (Conversacion candidata : candidatas) {
            Conversacion conversacion = conversacionRepository
                .findByIdConversacionForUpdate(candidata.getIdConversacion())
                .orElse(null);
            if (conversacion == null
                    || conversacion.getEstado() != EstadoConversacion.PENDIENTE) {
                continue;
            }

            LocalDateTime ahora = LocalDateTime.now();
            conversacion.setEstado(EstadoConversacion.VENCIDA);
            conversacion.setFechaRespuesta(ahora);
            conversacion.setFechaActualizacion(ahora);

            notificacionService.crearParaUsuario(
                conversacion.getIniciador().getIdUsuario(),
                NOTIFICACION_VENCIDA,
                "Solicitud de mensaje vencida",
                "Tu solicitud para contactar a @"
                    + conversacion.getDestinatario().getNombreUsuario()
                    + " venció sin respuesta.",
                conversacion.getIdConversacion(),
                TIPO_REFERENCIA_CONVERSACION);
            publicarCambio(conversacion);
            vencidas++;
        }
        return vencidas;
    }

    @Transactional(readOnly = true)
    public void marcarEscribiendo(Long idConversacion, Long idUsuario) {
        Conversacion conversacion = obtenerConversacion(idConversacion);
        validarParticipante(conversacion, idUsuario);
        if (conversacion.getEstado() != EstadoConversacion.ACEPTADA) {
            return;
        }
        Usuario usuario = obtenerUsuario(idUsuario);
        if (!usuario.isEstadoCuenta()) {
            return;
        }
        indicadorEscritura.marcar(idConversacion, idUsuario);
        Usuario contraparte = conversacion.contraparteDe(idUsuario);
        if (contraparte != null) {
            eventoPublisher.publishEvent(new EscrituraEvent(
                idConversacion, contraparte.getNombreUsuario()));
        }
    }

    @Transactional(readOnly = true)
    public EscribiendoDTO estadoEscritura(Long idConversacion, Long idUsuario) {
        Conversacion conversacion = obtenerConversacion(idConversacion);
        validarParticipante(conversacion, idUsuario);
        if (conversacion.getEstado() != EstadoConversacion.ACEPTADA) {
            return EscribiendoDTO.builder().escribiendo(false).build();
        }
        Usuario contraparte = conversacion.contraparteDe(idUsuario);
        if (contraparte == null || !contraparte.isEstadoCuenta()) {
            return EscribiendoDTO.builder().escribiendo(false).build();
        }
        return EscribiendoDTO.builder()
            .escribiendo(indicadorEscritura.estaEscribiendo(
                idConversacion, contraparte.getIdUsuario()))
            .build();
    }

    private ConversacionDTO reenviar(
            Conversacion conversacion,
            Usuario remitente,
            Usuario destinatario,
            String motivo,
            String primerMensaje,
            ConfiguracionMensajeria config) {
        validarCuposSolicitudes(remitente.getIdUsuario(), config);

        LocalDateTime ahora = LocalDateTime.now();
        conversacion.setEstado(EstadoConversacion.PENDIENTE);
        conversacion.setMotivoContacto(motivo);
        conversacion.setFechaSolicitud(ahora);
        conversacion.setFechaRespuesta(null);
        conversacion.setFechaActualizacion(ahora);

        ConversacionParticipante participanteRemitente =
            asegurarParticipante(conversacion, remitente);
        participanteRemitente.setArchivada(false);
        ConversacionParticipante participanteDestinatario =
            asegurarParticipante(conversacion, destinatario);
        participanteDestinatario.setArchivada(false);

        Mensaje mensaje = guardarMensaje(
            conversacion, remitente, primerMensaje, ahora);
        conversacion.setFechaUltimoMensaje(mensaje.getFechaEnvio());

        registrarAccion(
            conversacion, TipoAccionConversacion.REENVIO, remitente, null);
        notificarSolicitud(destinatario, remitente, conversacion, motivo);
        publicarCambio(conversacion);

        return aDTO(conversacion, remitente.getIdUsuario(), config);
    }

    private ConversacionDTO aDTO(
            Conversacion conversacion,
            Long idUsuario,
            ConfiguracionMensajeria config) {
        ConversacionParticipante mio = participanteRepository
            .findByConversacionIdConversacionAndUsuarioIdUsuario(
                conversacion.getIdConversacion(), idUsuario)
            .orElse(null);
        ConversacionParticipante otro = participanteRepository
            .findByConversacionIdConversacionAndUsuarioIdUsuarioNot(
                conversacion.getIdConversacion(), idUsuario)
            .orElse(null);

        boolean soyIniciador = conversacion.esIniciador(idUsuario);
        boolean soyDestinatario = conversacion.esDestinatario(idUsuario);
        boolean pendiente = conversacion.getEstado() == EstadoConversacion.PENDIENTE;
        String motivoNoEnviable = motivoNoEnviable(conversacion, idUsuario);

        Optional<ConversacionAccion> rechazo = conversacion.getEstado()
            == EstadoConversacion.RECHAZADA
                ? accionRepository
                    .findFirstByConversacionIdConversacionAndTipoAccionOrderByFechaDesc(
                        conversacion.getIdConversacion(),
                        TipoAccionConversacion.RECHAZO)
                : Optional.empty();

        long noLeidos = 0L;
        if (conversacion.getEstado() == EstadoConversacion.ACEPTADA) {
            List<Object[]> filas = mensajeRepository.contarNoLeidosPorConversacion(
                idUsuario,
                List.of(conversacion.getIdConversacion()),
                EstadoMensaje.ENVIADO);
            if (!filas.isEmpty()) {
                noLeidos = ((Number) filas.get(0)[1]).longValue();
            }
        }

        return ConversacionDTO.builder()
            .idConversacion(conversacion.getIdConversacion())
            .estado(conversacion.getEstado())
            .motivoContacto(conversacion.getMotivoContacto())
            .iniciador(UsuarioResumenDTO.desdeUsuario(conversacion.getIniciador()))
            .destinatario(UsuarioResumenDTO.desdeUsuario(conversacion.getDestinatario()))
            .contraparte(UsuarioResumenDTO.desdeUsuario(
                conversacion.contraparteDe(idUsuario)))
            .soyIniciador(soyIniciador)
            .fechaSolicitud(conversacion.getFechaSolicitud())
            .fechaRespuesta(conversacion.getFechaRespuesta())
            .fechaUltimoMensaje(conversacion.getFechaUltimoMensaje())
            .motivoRechazo(rechazo.map(ConversacionAccion::getMotivo).orElse(null))
            .fechaRechazo(rechazo.map(ConversacionAccion::getFecha).orElse(null))
            .noLeidos(noLeidos)
            .archivada(mio != null && mio.isArchivada())
            .puedeAceptar(pendiente && soyDestinatario)
            .puedeRechazar(pendiente && soyDestinatario)
            .puedeCancelar(pendiente && soyIniciador)
            .puedeEnviar(motivoNoEnviable == null)
            .motivoNoEnviable(motivoNoEnviable)
            .longitudMaximaMensaje(config.getLongitudMaximaMensaje())
            .longitudMaximaMotivo(config.getLongitudMaximaMotivo())
            .fechaProximoEnvio(conversacion.getEstado() == EstadoConversacion.RECHAZADA
                ? fechaDisponibleReenvio(conversacion, config)
                : null)
            .contraparteUltimoLeidoEn(otro != null ? otro.getUltimoLeidoEn() : null)
            .build();
    }

    private String motivoNoEnviable(Conversacion conversacion, Long idUsuario) {
        if (conversacion.getEstado() != EstadoConversacion.ACEPTADA) {
            return switch (conversacion.getEstado()) {
                case PENDIENTE -> "La solicitud todavía no fue aceptada.";
                case RECHAZADA -> "La conversación fue rechazada.";
                case CANCELADA -> "La solicitud fue cancelada.";
                case VENCIDA -> "La solicitud venció.";
                case ACEPTADA -> null;
            };
        }
        Usuario yo = conversacion.esIniciador(idUsuario)
            ? conversacion.getIniciador()
            : conversacion.getDestinatario();
        if (!yo.isEstadoCuenta()) {
            return "Tu cuenta está deshabilitada para enviar mensajes.";
        }
        Usuario contraparte = conversacion.contraparteDe(idUsuario);
        if (!contraparte.isEstadoCuenta()) {
            return "El usuario no está disponible para recibir mensajes.";
        }
        return null;
    }

    private void validarRemitente(Usuario remitente) {
        if (remitente.getRol() == null
                || remitente.getRol().getNombre() == ERol.ADMIN) {
            throw new AccesoDenegadoException(
                "La mensajería privada es entre artistas y clientes.");
        }
        validarCuentaActiva(remitente);
    }

    private void validarDestinatario(Usuario remitente, Usuario destinatario) {
        if (destinatario.getIdUsuario().equals(remitente.getIdUsuario())) {
            throw new IllegalArgumentException(
                "No podés enviarte una solicitud a vos mismo.");
        }
        if (destinatario.getRol() == null
                || destinatario.getRol().getNombre() == ERol.ADMIN
                || !destinatario.isEstadoCuenta()) {
            throw new NoSuchElementException(
                "El usuario no está disponible para recibir mensajes.");
        }
    }

    private void validarCuentaActiva(Usuario usuario) {
        if (!usuario.isEstadoCuenta()) {
            throw new AccesoDenegadoException(
                "Tu cuenta está deshabilitada para enviar mensajes.");
        }
    }

    private void validarCuposSolicitudes(
            Long idUsuario,
            ConfiguracionMensajeria config) {
        String motivo = motivoCupoSolicitudes(idUsuario, config);
        if (motivo != null) {
            throw new ConflictoException(motivo);
        }
    }

    private String motivoCupoSolicitudes(
            Long idUsuario,
            ConfiguracionMensajeria config) {
        long pendientes = conversacionRepository
            .countByIniciadorIdUsuarioAndEstado(
                idUsuario, EstadoConversacion.PENDIENTE);
        if (pendientes >= config.getMaxSolicitudesPendientes()) {
            return "Ya tenés " + pendientes
                + " solicitudes de mensaje pendientes. Esperá a que te respondan"
                + " antes de enviar otra.";
        }
        long enviadas = accionRepository
            .countByUsuarioIdUsuarioAndTipoAccionInAndFechaAfter(
                idUsuario,
                List.of(
                    TipoAccionConversacion.SOLICITUD,
                    TipoAccionConversacion.REENVIO),
                LocalDateTime.now().minusDays(1));
        if (enviadas >= config.getMaxSolicitudesPorDia()) {
            return "Alcanzaste el límite de "
                + config.getMaxSolicitudesPorDia()
                + " solicitudes de mensaje por día. Probá más tarde.";
        }
        return null;
    }

    private EstadoRelacionMensajeDTO relacion(
            EstadoRelacionMensaje estado,
            Long idConversacion,
            boolean puedeSolicitar,
            String motivoNoSolicitable,
            LocalDateTime fechaProximoEnvio) {
        return EstadoRelacionMensajeDTO.builder()
            .estadoRelacion(estado)
            .idConversacion(idConversacion)
            .puedeSolicitar(puedeSolicitar)
            .motivoNoSolicitable(motivoNoSolicitable)
            .fechaProximoEnvio(fechaProximoEnvio)
            .build();
    }

    private EstadoRelacionMensajeDTO conContraparte(
            EstadoRelacionMensajeDTO dto,
            Usuario contraparte) {
        dto.setContraparte(UsuarioResumenDTO.desdeUsuario(contraparte));
        return dto;
    }

    private boolean participantesValidos(Usuario cliente, Usuario artista) {
        return cliente != null
            && artista != null
            && cliente.getIdUsuario() != null
            && artista.getIdUsuario() != null
            && !cliente.getIdUsuario().equals(artista.getIdUsuario());
    }

    private String motivoAperturaPorSolicitud(Long idSolicitud, String nombreServicio) {
        String motivo = idSolicitud != null
            ? "Solicitud #" + idSolicitud
            : "Solicitud de servicio";
        if (nombreServicio != null && !nombreServicio.isBlank()) {
            motivo += " · " + nombreServicio.trim();
        }
        return recortarAMaximo(motivo, 200);
    }

    private String motivoReaperturaPorSolicitud(
            Long idSolicitud, EstadoConversacion anterior) {
        String motivo = (idSolicitud != null
            ? "Solicitud #" + idSolicitud
            : "Solicitud de servicio")
            + " · reabierta desde " + anterior.name();
        return recortarAMaximo(motivo, 200);
    }

    private String recortarAMaximo(String valor, int maximo) {
        return valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }

    private boolean archivar(Conversacion conversacion, Usuario usuario) {
        ConversacionParticipante participante =
            asegurarParticipante(conversacion, usuario);
        if (participante.isArchivada()) {
            return false;
        }
        participante.setArchivada(true);
        return true;
    }

    private boolean desarchivar(Conversacion conversacion, Usuario usuario) {
        ConversacionParticipante participante =
            asegurarParticipante(conversacion, usuario);
        if (!participante.isArchivada()) {
            return false;
        }
        participante.setArchivada(false);
        return true;
    }

    private ConversacionParticipante asegurarParticipante(
            Conversacion conversacion,
            Usuario usuario) {
        return participanteRepository
            .findByConversacionIdConversacionAndUsuarioIdUsuario(
                conversacion.getIdConversacion(), usuario.getIdUsuario())
            .orElseGet(() -> {
                ConversacionParticipante participante =
                    new ConversacionParticipante();
                participante.setConversacion(conversacion);
                participante.setUsuario(usuario);
                return participanteRepository.save(participante);
            });
    }

    private Mensaje guardarMensaje(
            Conversacion conversacion,
            Usuario remitente,
            String contenido,
            LocalDateTime fecha) {
        Mensaje mensaje = new Mensaje();
        mensaje.setConversacion(conversacion);
        mensaje.setRemitente(remitente);
        mensaje.setContenido(contenido);
        mensaje.setFechaEnvio(fecha);
        return mensajeRepository.save(mensaje);
    }

    private void registrarAccion(
            Conversacion conversacion,
            TipoAccionConversacion tipo,
            Usuario usuario,
            String motivo) {
        ConversacionAccion accion = new ConversacionAccion();
        accion.setConversacion(conversacion);
        accion.setTipoAccion(tipo);
        accion.setUsuario(usuario);
        accion.setMotivo(motivo);
        accionRepository.save(accion);
    }

    private void publicarCambio(Conversacion conversacion) {
        eventoPublisher.publishEvent(new ConversacionActualizadaEvent(
            conversacion.getIdConversacion(),
            conversacion.getEstado(),
            conversacion.getIniciador().getNombreUsuario(),
            conversacion.getDestinatario().getNombreUsuario()));
    }

    private void notificarSolicitud(
            Usuario destinatario,
            Usuario remitente,
            Conversacion conversacion,
            String motivo) {
        notificacionService.crearParaUsuario(
            destinatario.getIdUsuario(),
            NOTIFICACION_SOLICITUD,
            "Nueva solicitud de mensaje",
            "@" + remitente.getNombreUsuario()
                + " quiere contactarte: "
                + TextoMensajeria.recortar(motivo, 180),
            conversacion.getIdConversacion(),
            TIPO_REFERENCIA_CONVERSACION);
    }

    private Map<Long, Long> noLeidosPorConversacion(
            Long idUsuario,
            List<Long> ids) {
        Map<Long, Long> resultado = new HashMap<>();
        for (Object[] fila : mensajeRepository.contarNoLeidosPorConversacion(
                idUsuario, ids, EstadoMensaje.ENVIADO)) {
            resultado.put(((Number) fila[0]).longValue(),
                ((Number) fila[1]).longValue());
        }
        return resultado;
    }

    private Map<Long, Mensaje> ultimosMensajes(List<Long> ids) {
        return mensajeRepository
            .ultimosMensajes(ids, EstadoMensaje.ENVIADO)
            .stream()
            .collect(Collectors.toMap(
                mensaje -> mensaje.getConversacion().getIdConversacion(),
                mensaje -> mensaje));
    }

    private LocalDateTime fechaDisponibleReenvio(
            Conversacion conversacion,
            ConfiguracionMensajeria config) {
        if (conversacion.getFechaRespuesta() == null) {
            return null;
        }
        return conversacion.getFechaRespuesta()
            .plusHours(config.getHorasEsperaReenvio());
    }

    private String formatearFecha(LocalDateTime fecha) {
        return fecha.format(FORMATO_FECHA);
    }

    private Conversacion obtenerConversacion(Long idConversacion) {
        return conversacionRepository.findByIdConversacion(idConversacion)
            .orElseThrow(() -> new NoSuchElementException(
                "Conversación no encontrada: " + idConversacion));
    }

    private Conversacion obtenerParaActualizar(Long idConversacion) {
        return conversacionRepository.findByIdConversacionForUpdate(idConversacion)
            .orElseThrow(() -> new NoSuchElementException(
                "Conversación no encontrada: " + idConversacion));
    }

    private Usuario obtenerUsuario(Long idUsuario) {
        return usuarioRepository.findByIdUsuario(idUsuario)
            .orElseThrow(() -> new NoSuchElementException(
                "Usuario no encontrado: " + idUsuario));
    }

    private void validarParticipante(
            Conversacion conversacion,
            Long idUsuario) {
        if (!conversacion.involucra(idUsuario)) {
            throw new NoSuchElementException("Conversación no encontrada.");
        }
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
}
