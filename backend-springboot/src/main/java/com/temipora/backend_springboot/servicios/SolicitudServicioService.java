package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.AccionSolicitud;
import com.temipora.backend_springboot.modelo.AlcanceSancion;
import com.temipora.backend_springboot.modelo.Conversacion;
import com.temipora.backend_springboot.modelo.DerechoTermino;
import com.temipora.backend_springboot.modelo.DisponibilidadServicioDTO;
import com.temipora.backend_springboot.modelo.EstadoServicio;
import com.temipora.backend_springboot.modelo.EstadoSolicitud;
import com.temipora.backend_springboot.modelo.Formulario;
import com.temipora.backend_springboot.modelo.FormularioDTO;
import com.temipora.backend_springboot.modelo.ItemSolicitud;
import com.temipora.backend_springboot.modelo.ItemSolicitudDerecho;
import com.temipora.backend_springboot.modelo.ItemSolicitudRequest;
import com.temipora.backend_springboot.modelo.ListaEspera;
import com.temipora.backend_springboot.modelo.ModoRecepcionSolicitud;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.MotivoSustitucion;
import com.temipora.backend_springboot.modelo.OfertaReactivacion;
import com.temipora.backend_springboot.modelo.Pregunta;
import com.temipora.backend_springboot.modelo.PreguntaDTO;
import com.temipora.backend_springboot.modelo.Respuesta;
import com.temipora.backend_springboot.modelo.RespuestaSolicitudRequest;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.ReputacionClienteDTO;
import com.temipora.backend_springboot.modelo.ServicioDTO;
import com.temipora.backend_springboot.modelo.Solicitud;
import com.temipora.backend_springboot.modelo.SolicitudDTO;
import com.temipora.backend_springboot.modelo.SolicitudPublicaDTO;
import com.temipora.backend_springboot.modelo.SolicitudResumenDTO;
import com.temipora.backend_springboot.modelo.SolicitudServicioRequest;
import com.temipora.backend_springboot.modelo.TerminoDerecho;
import com.temipora.backend_springboot.modelo.TerminoDerechoDTO;
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

@Service
public class SolicitudServicioService {

    private static final List<EstadoSolicitud> ESTADOS_ACTIVOS = List.of(
        EstadoSolicitud.PENDIENTE,
        EstadoSolicitud.EN_LISTA_ESPERA,
        EstadoSolicitud.PRESUPUESTADA,
        EstadoSolicitud.PAGADA);

    private static final List<EstadoSolicitud> ESTADOS_RECHAZABLES = List.of(
        EstadoSolicitud.PENDIENTE,
        EstadoSolicitud.EN_LISTA_ESPERA);

    private static final List<EstadoSolicitud> ESTADOS_QUE_SE_CIERRAN_AL_ARCHIVAR = List.of(
        EstadoSolicitud.PENDIENTE,
        EstadoSolicitud.EN_LISTA_ESPERA);

    private static final List<EstadoSolicitud> ESTADOS_CANCELABLES = List.of(
        EstadoSolicitud.PENDIENTE,
        EstadoSolicitud.EN_LISTA_ESPERA,
        EstadoSolicitud.PRESUPUESTADA);

    private static final String ORIGEN_ARTISTA = "ARTISTA";
    private static final String ORIGEN_CLIENTE = "CLIENTE";
    private static final String ORIGEN_BAJA_SERVICIO = "BAJA_SERVICIO";
    private static final String DERECHO_PERSONAL = "PERSONAL";

    private static final String NOTIFICACION_SOLICITUD = "SOLICITUD_RECIBIDA";
    private static final String NOTIFICACION_ESPERA = "SOLICITUD_EN_LISTA_ESPERA";
    private static final String NOTIFICACION_RECHAZO = "SOLICITUD_RECHAZADA";
    private static final String NOTIFICACION_CANCELACION = "SOLICITUD_CANCELADA";
    private static final String TIPO_REFERENCIA_SOLICITUD = "SOLICITUD";

    private static final String ENTIDAD_SOLICITUD = "Solicitud";
    private static final List<CampoAuditado> CAMPOS_SOLICITUD = List.of(
        CampoAuditado.de("estado", "Estado"),
        CampoAuditado.de("cantidadDibujos", "Cantidad de dibujos"),
        CampoAuditado.de("precioBase", "Precio base"),
        CampoAuditado.de("moneda", "Moneda"),
        CampoAuditado.de("prioridadDefinidaPorCliente", "Prioridad definida"));

    private static final String TEXTO = "TEXTO";
    private static final String CASILLAS = "CASILLAS";
    private static final String OPCIONES = "OPCIONES";
    private static final String ENCABEZADO = "ENCABEZADO";
    private static final String SUBIR_IMAGEN = "SUBIR_IMAGEN";

    private final ServicioRepository servicioRepository;
    private final UsuarioRepository usuarioRepository;
    private final FormularioRepository formularioRepository;
    private final TerminosRepository terminosRepository;
    private final SolicitudRepository solicitudRepository;
    private final AccionSolicitudRepository accionSolicitudRepository;
    private final ServicioService servicioService;
    private final FormularioService formularioService;
    private final TerminosService terminosService;
    private final ListaEsperaService listaEsperaService;
    private final NotificacionService notificacionService;
    private final ColaSolicitudesService colaSolicitudesService;
    private final MonedaVistaService monedaVistaService;
    private final ReseniaPublicaService reseniaPublicaService;
    private final ConversacionService conversacionService;
    private final ConversacionRepository conversacionRepository;
    private final OfertaReactivacionService ofertaReactivacionService;
    private final AuditoriaService auditoria;
    private final ArancelEstimadoService arancelEstimadoService;
    private final SustitucionService sustitucionService;
    private final CuposEventPublisher cuposEventPublisher;
    private final CierreEmailService cierreEmailService;
    private final SancionService sancionService;

    public SolicitudServicioService(
            ServicioRepository servicioRepository,
            UsuarioRepository usuarioRepository,
            FormularioRepository formularioRepository,
            TerminosRepository terminosRepository,
            SolicitudRepository solicitudRepository,
            AccionSolicitudRepository accionSolicitudRepository,
            ServicioService servicioService,
            FormularioService formularioService,
            TerminosService terminosService,
            ListaEsperaService listaEsperaService,
            NotificacionService notificacionService,
            ColaSolicitudesService colaSolicitudesService,
            MonedaVistaService monedaVistaService,
            ReseniaPublicaService reseniaPublicaService,
            ConversacionService conversacionService,
            ConversacionRepository conversacionRepository,
            OfertaReactivacionService ofertaReactivacionService,
            AuditoriaService auditoria,
            ArancelEstimadoService arancelEstimadoService,
            SustitucionService sustitucionService,
            CuposEventPublisher cuposEventPublisher,
            CierreEmailService cierreEmailService,
            SancionService sancionService) {
        this.servicioRepository = servicioRepository;
        this.usuarioRepository = usuarioRepository;
        this.formularioRepository = formularioRepository;
        this.terminosRepository = terminosRepository;
        this.solicitudRepository = solicitudRepository;
        this.accionSolicitudRepository = accionSolicitudRepository;
        this.servicioService = servicioService;
        this.formularioService = formularioService;
        this.terminosService = terminosService;
        this.listaEsperaService = listaEsperaService;
        this.notificacionService = notificacionService;
        this.colaSolicitudesService = colaSolicitudesService;
        this.monedaVistaService = monedaVistaService;
        this.reseniaPublicaService = reseniaPublicaService;
        this.conversacionService = conversacionService;
        this.conversacionRepository = conversacionRepository;
        this.ofertaReactivacionService = ofertaReactivacionService;
        this.auditoria = auditoria;
        this.arancelEstimadoService = arancelEstimadoService;
        this.sustitucionService = sustitucionService;
        this.cuposEventPublisher = cuposEventPublisher;
        this.cierreEmailService = cierreEmailService;
        this.sancionService = sancionService;
    }

    @Transactional(readOnly = true)
    public DisponibilidadServicioDTO disponibilidad(Long idServicio, Long idCliente) {
        servicioService.obtener(idServicio, idCliente);
        Servicio servicio = servicioRepository.findByIdServicio(idServicio)
            .orElseThrow(() -> new NoSuchElementException("Servicio no encontrado: " + idServicio));

        int cuposTotales = servicio.getCupos() == null ? 0 : servicio.getCupos();
        int cuposOcupados = calcularOcupados(idServicio);
        int cuposDisponibles = Math.max(0, cuposTotales - cuposOcupados);
        ModoRecepcionSolicitud modo = modoDe(servicio);

        boolean tieneSolicitudActiva = idCliente != null
            && solicitudRepository.existsByServicioIdServicioAndClienteIdUsuarioAndEstadoIn(
                idServicio, idCliente, ESTADOS_ACTIVOS);
        boolean tieneSolicitudEnEspera = idCliente != null
            && solicitudRepository.existsByServicioIdServicioAndClienteIdUsuarioAndEstado(
                idServicio, idCliente, EstadoSolicitud.EN_LISTA_ESPERA);

        boolean permiteListaEspera = modo == ModoRecepcionSolicitud.SOLO_ESPERA;
        boolean puedeSolicitarCupo = idCliente != null
            && !tieneSolicitudActiva
            && modo == ModoRecepcionSolicitud.ABIERTA
            && cuposDisponibles > 0;
        boolean puedeListaEspera = idCliente != null
            && !tieneSolicitudActiva
            && modo == ModoRecepcionSolicitud.SOLO_ESPERA;
        boolean puedeQuedarEnCola = idCliente != null
            && !tieneSolicitudActiva
            && modo == ModoRecepcionSolicitud.ABIERTA
            && cuposDisponibles <= 0;
        boolean puedeSolicitar = puedeSolicitarCupo
            || puedeListaEspera
            || puedeQuedarEnCola;

        String mensaje;
        if (idCliente == null) {
            mensaje = "Iniciá sesión para solicitar este servicio.";
        } else if (tieneSolicitudActiva) {
            mensaje = "Ya tenés una solicitud activa para este servicio.";
        } else if (modo == ModoRecepcionSolicitud.CERRADA) {
            mensaje = "El artista no está recibiendo solicitudes para este servicio.";
        } else if (modo == ModoRecepcionSolicitud.SOLO_ESPERA) {
            mensaje = "El servicio recibe solicitudes solo en lista de espera.";
        } else if (cuposDisponibles > 0) {
            mensaje = null;
        } else {
            mensaje = "No hay cupos libres. Podés enviar la solicitud y el artista la "
                + "presupuestará cuando se libere un cupo.";
        }

        return DisponibilidadServicioDTO.builder()
            .idServicio(idServicio)
            .cuposTotales(cuposTotales)
            .cuposOcupados(cuposOcupados)
            .cuposDisponibles(cuposDisponibles)
            .modoRecepcion(modo.name())
            .maxDibujosPorSolicitud(maxDibujosDe(servicio))
            .permiteListaEspera(permiteListaEspera)
            .tieneSolicitudActiva(tieneSolicitudActiva)
            .tieneSolicitudEnEspera(tieneSolicitudEnEspera)
            .puedeSolicitarCupo(puedeSolicitarCupo)
            .puedeListaEspera(puedeListaEspera)
            .puedeQuedarEnCola(puedeQuedarEnCola)
            .puedeSolicitar(puedeSolicitar)
            .mensaje(mensaje)
            .build();
    }

    @Transactional(readOnly = true)
    public SolicitudPublicaDTO solicitudPublica(Long idServicio, Long idCliente) {
        ServicioDTO servicio = servicioService.obtener(idServicio, idCliente);
        TerminosDTO terminos = terminosService.obtenerServicio(idServicio, servicio.getIdArtista());
        FormularioDTO formulario = formularioService.obtenerServicio(idServicio, servicio.getIdArtista());
        DisponibilidadServicioDTO disponibilidad = disponibilidad(idServicio, idCliente);
        boolean terminosValidos = terminos.getIdTerminos() != null
            && terminos.getVersion() != null
            && terminos.getVersion() > 0;

        return SolicitudPublicaDTO.builder()
            .servicio(servicio)
            .terminos(terminos)
            .formulario(formulario)
            .disponibilidad(disponibilidad)
            .terminosValidos(terminosValidos)
            .build();
    }

    @Transactional
    public SolicitudDTO crear(Long idCliente, SolicitudServicioRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud es obligatoria.");
        }
        if (request.getIdTerminos() == null || request.getTerminosVersion() == null) {
            throw new IllegalArgumentException("Tenés que aceptar los términos del servicio.");
        }
        if (!request.isAceptaTerminos()) {
            throw new IllegalArgumentException("Tenés que aceptar los términos del servicio.");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Debés agregar al menos un dibujo.");
        }

        Usuario cliente = usuarioRepository.findByIdUsuario(idCliente)
            .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado: " + idCliente));
        Servicio servicio = servicioRepository.findByIdServicioForUpdate(request.getIdServicio())
            .orElseThrow(() -> new NoSuchElementException(
                "Servicio no encontrado: " + request.getIdServicio()));
        if (servicio.getEstado() == EstadoServicio.ARCHIVADO) {
            throw new ServicioNoDisponibleException("El servicio ya no está disponible.");
        }
        if (servicio.getEstado() != EstadoServicio.PUBLICADO) {
            throw new ConflictoException(
                "El servicio está pausado y no recibe solicitudes nuevas.");
        }

        Usuario artista = servicio.getArtista();
        if (artista != null && artista.getIdUsuario().equals(idCliente)) {
            throw new IllegalArgumentException("No podés solicitar tu propio servicio.");
        }
        if (artista != null && sancionService.tieneSancionVigente(
                artista.getIdUsuario(), AlcanceSancion.VENTAS)) {
            throw new ConflictoException(
                "El artista no está recibiendo encargos en este momento.");
        }

        TerminosDTO terminos = terminosService.obtenerServicio(
            servicio.getIdServicio(), artista.getIdUsuario());
        if (terminos.getIdTerminos() == null || terminos.getVersion() == null
                || terminos.getVersion() <= 0) {
            throw new IllegalArgumentException(
                "El artista no tiene términos de uso y servicio configurados.");
        }
        if (!request.getIdTerminos().equals(terminos.getIdTerminos())
                || !request.getTerminosVersion().equals(terminos.getVersion())) {
            throw new ConflictoException(
                "Los términos del servicio cambiaron. Revisalos y volvé a aceptarlos.");
        }

        Terminos terminosEntidad = terminosRepository.findByIdTerminos(terminos.getIdTerminos())
            .orElseThrow(() -> new NoSuchElementException(
                "Términos no encontrados: " + terminos.getIdTerminos()));
        FormularioDTO formularioDTO = formularioService.obtenerServicio(
            servicio.getIdServicio(), artista.getIdUsuario());
        Formulario formulario = formularioRepository
            .findByIdFormulario(formularioDTO.getIdFormulario())
            .orElseThrow(() -> new NoSuchElementException(
                "Formulario no encontrado: " + formularioDTO.getIdFormulario()));

        Map<Long, TerminoDerecho> derechosDisponibles = new LinkedHashMap<>();
        for (TerminoDerecho terminoDerecho : terminosEntidad.getTerminoDerechos()) {
            if (terminoDerecho.getDerecho() != null) {
                derechosDisponibles.put(
                    terminoDerecho.getDerecho().getIdDerechoTermino(), terminoDerecho);
            }
        }

        List<ItemPreparado> preparados = validarYPrepararItems(
            request, formularioDTO, formulario, derechosDisponibles);
        int totalDibujos = preparados.size();
        int maxDibujos = maxDibujosDe(servicio);
        if (totalDibujos > maxDibujos) {
            throw new IllegalArgumentException(
                "Este servicio admite hasta " + maxDibujos + " dibujo(s) por solicitud.");
        }

        ModoRecepcionSolicitud modo = modoDe(servicio);
        if (modo == ModoRecepcionSolicitud.CERRADA) {
            throw new IllegalArgumentException(
                "El artista no está recibiendo solicitudes para este servicio.");
        }
        if (solicitudRepository.existsByServicioIdServicioAndClienteIdUsuarioAndEstadoIn(
                servicio.getIdServicio(), idCliente, ESTADOS_ACTIVOS)) {
            throw new IllegalArgumentException(
                "Ya tenés una solicitud activa para este servicio.");
        }
        if (modo == ModoRecepcionSolicitud.SOLO_ESPERA && !request.isAceptaListaEspera()) {
            throw new IllegalArgumentException(
                "Aceptá la lista de espera para enviar la solicitud.");
        }

        OfertaReactivacion oferta = null;
        if (request.getIdOferta() != null) {
            oferta = ofertaReactivacionService.validarParaSolicitud(
                request.getIdOferta(), idCliente, servicio.getIdServicio());
        }

        List<ItemPreparado> ordenados = new ArrayList<>(preparados);
        if (request.isPrioridadDefinida()) {
            validarPrioridades(ordenados);
            ordenados.sort(Comparator.comparing(ItemPreparado::prioridad));
        }

        LocalDateTime terminosAceptadosEn = LocalDateTime.now();
        String terminosSnapshot = serializarTerminos(terminos);

        EstadoSolicitud estado = modo == ModoRecepcionSolicitud.SOLO_ESPERA
            ? EstadoSolicitud.EN_LISTA_ESPERA
            : EstadoSolicitud.PENDIENTE;
        Solicitud solicitud = crearSolicitud(
            servicio,
            cliente,
            artista,
            formulario,
            terminosEntidad,
            terminos.getVersion(),
            terminosAceptadosEn,
            terminosSnapshot,
            request.isPrioridadDefinida(),
            estado,
            ordenados);
        if (oferta != null) {
            solicitud.setOfertaReactivacion(oferta);
            oferta.setSolicitud(solicitud);
        }
        solicitudRepository.save(solicitud);

        if (request.getIdSustitucion() != null) {
            sustitucionService.resolverPorSolicitud(
                request.getIdSustitucion(),
                idCliente,
                solicitud.getIdSolicitud(),
                servicio.getIdServicio());
        }

        auditoria.registrar(
            AccionesAdmin.SOLICITUD_CREAR, ENTIDAD_SOLICITUD, solicitud.getIdSolicitud(),
            "Envió una solicitud para '" + servicio.getNombre() + "'",
            null, solicitud, CAMPOS_SOLICITUD);

        conversacionService.asegurarPorSolicitud(
            cliente, artista, solicitud.getIdSolicitud(), servicio.getNombre());

        if (estado == EstadoSolicitud.EN_LISTA_ESPERA) {
            ListaEspera lista = listaEsperaService.crear(
                solicitud, servicio, cliente, totalDibujos);
            solicitud.setListaEspera(lista);
            notificacionService.crearParaUsuario(
                artista.getIdUsuario(),
                NOTIFICACION_ESPERA,
                "Nueva solicitud en lista de espera",
                "Recibiste una solicitud para la lista de espera de '" + servicio.getNombre()
                    + "' con " + totalDibujos + " dibujo(s).",
                solicitud.getIdSolicitud(),
                TIPO_REFERENCIA_SOLICITUD);
        } else {
            notificacionService.crearParaUsuario(
                artista.getIdUsuario(),
                NOTIFICACION_SOLICITUD,
                "Nueva solicitud de servicio",
                "Recibiste una solicitud para '" + servicio.getNombre() + "' con "
                    + totalDibujos + " dibujo(s).",
                solicitud.getIdSolicitud(),
                TIPO_REFERENCIA_SOLICITUD);
        }

        return aDTO(solicitud, idCliente);
    }

    @Transactional(readOnly = true)
    public SolicitudDTO obtenerSolicitud(Long idSolicitud, Long idUsuario) {
        Solicitud solicitud = solicitudRepository.findByIdSolicitud(idSolicitud)
            .orElseThrow(() -> new NoSuchElementException(
                "Solicitud no encontrada: " + idSolicitud));
        boolean esCliente = solicitud.getCliente() != null
            && solicitud.getCliente().getIdUsuario().equals(idUsuario);
        boolean esArtista = solicitud.getArtista() != null
            && solicitud.getArtista().getIdUsuario().equals(idUsuario);
        if (!esCliente && !esArtista) {
            throw new AccesoDenegadoException("No podés ver esta solicitud.");
        }
        SolicitudDTO dto = aDTO(solicitud, idUsuario);
        accionSolicitudRepository
            .findFirstBySolicitudIdSolicitudAndTipoAccionOrderByFechaDesc(
                idSolicitud, TipoAccionSolicitud.RECHAZO)
            .ifPresent(accion -> {
                dto.setMotivoRechazo(accion.getMotivo());
                dto.setFechaRechazo(accion.getFecha());
            });
        accionSolicitudRepository
            .findFirstBySolicitudIdSolicitudAndTipoAccionOrderByFechaDesc(
                idSolicitud, TipoAccionSolicitud.CANCELACION)
            .ifPresent(accion -> {
                dto.setMotivoCancelacion(accion.getMotivo());
                dto.setFechaCancelacion(accion.getFecha());
            });

        ColaSolicitudesService.EvaluacionPresupuesto evaluacion =
            colaSolicitudesService.evaluar(solicitud);
        dto.setPosicionCola(evaluacion.posicion());
        if (esArtista) {
            dto.setPuedePresupuestar(evaluacion.puede());
            dto.setMotivoNoPresupuestable(evaluacion.motivo());
        }
        if (esCliente) {
            dto.setPuedeCancelar(ESTADOS_CANCELABLES.contains(solicitud.getEstado()));
        }
        return dto;
    }

    @Transactional
    public SolicitudDTO rechazar(Long idSolicitud, Long idArtista, String motivo) {
        Solicitud solicitud = solicitudRepository.findByIdSolicitudForUpdate(idSolicitud)
            .orElseThrow(() -> new NoSuchElementException(
                "Solicitud no encontrada: " + idSolicitud));
        boolean esArtista = solicitud.getArtista() != null
            && solicitud.getArtista().getIdUsuario().equals(idArtista);
        if (!esArtista) {
            throw new AccesoDenegadoException("No podés rechazar esta solicitud.");
        }
        if (solicitud.getEstado() == EstadoSolicitud.RECHAZADA) {
            throw new ConflictoException("La solicitud ya fue rechazada.");
        }
        if (!ESTADOS_RECHAZABLES.contains(solicitud.getEstado())) {
            throw new ConflictoException(
                "No podés rechazar una solicitud en estado " + solicitud.getEstado() + ".");
        }

        Snapshot antes = Snapshot.de(solicitud, CAMPOS_SOLICITUD);
        String motivoLimpio = limpiarMotivo(motivo);
        LocalDateTime ahora = registrarRechazo(solicitud, motivoLimpio, ORIGEN_ARTISTA);

        auditoria.registrar(
            AccionesAdmin.SOLICITUD_RECHAZAR, ENTIDAD_SOLICITUD, solicitud.getIdSolicitud(),
            motivoLimpio != null
                ? "Rechazó la solicitud. Motivo: " + motivoLimpio
                : "Rechazó la solicitud",
            antes, solicitud, CAMPOS_SOLICITUD);

        SolicitudDTO dto = aDTO(solicitud, idArtista);
        dto.setMotivoRechazo(motivoLimpio);
        dto.setFechaRechazo(ahora);
        return dto;
    }

    @Transactional
    public SolicitudDTO cancelar(Long idSolicitud, Long idCliente, String motivo) {
        Solicitud solicitud = solicitudRepository.findByIdSolicitudForUpdate(idSolicitud)
            .orElseThrow(() -> new NoSuchElementException(
                "Solicitud no encontrada: " + idSolicitud));
        boolean esCliente = solicitud.getCliente() != null
            && solicitud.getCliente().getIdUsuario().equals(idCliente);
        if (!esCliente) {
            throw new AccesoDenegadoException("No podés cancelar esta solicitud.");
        }
        if (solicitud.getEstado() == EstadoSolicitud.CANCELADA) {
            throw new ConflictoException("La solicitud ya fue cancelada.");
        }
        if (!ESTADOS_CANCELABLES.contains(solicitud.getEstado())) {
            throw new ConflictoException(
                "No podés cancelar una solicitud en estado " + solicitud.getEstado() + ".");
        }

        EstadoSolicitud estadoAnterior = solicitud.getEstado();
        Snapshot antes = Snapshot.de(solicitud, CAMPOS_SOLICITUD);
        String motivoLimpio = limpiarMotivo(motivo);
        LocalDateTime ahora = LocalDateTime.now();

        if (estadoAnterior == EstadoSolicitud.EN_LISTA_ESPERA) {
            listaEsperaService.marcarCancelada(solicitud);
        }

        solicitud.setEstado(EstadoSolicitud.CANCELADA);
        solicitud.setFechaActualizacion(ahora);

        AccionSolicitud accion = new AccionSolicitud();
        accion.setSolicitud(solicitud);
        accion.setTipoAccion(TipoAccionSolicitud.CANCELACION);
        accion.setUsuario(solicitud.getCliente());
        accion.setFecha(ahora);
        accion.setMotivo(motivoLimpio);
        accion.setOrigen(ORIGEN_CLIENTE);
        accionSolicitudRepository.save(accion);

        notificarCancelacion(solicitud, motivoLimpio, estadoAnterior);
        if (estadoAnterior == EstadoSolicitud.PRESUPUESTADA
                && solicitud.getServicio() != null) {
            cuposEventPublisher.publicar(solicitud.getServicio().getIdServicio());
        }

        auditoria.registrar(
            AccionesAdmin.SOLICITUD_CANCELAR, ENTIDAD_SOLICITUD, solicitud.getIdSolicitud(),
            motivoLimpio != null
                ? "Canceló la solicitud. Motivo: " + motivoLimpio
                : "Canceló la solicitud",
            antes, solicitud, CAMPOS_SOLICITUD);

        SolicitudDTO dto = aDTO(solicitud, idCliente);
        dto.setMotivoCancelacion(motivoLimpio);
        dto.setFechaCancelacion(ahora);
        dto.setPuedeCancelar(false);
        return dto;
    }

    @Transactional
    public SolicitudDTO retirarPresupuesto(Long idSolicitud, Long idArtista) {
        Solicitud solicitud = solicitudRepository.findByIdSolicitudForUpdate(idSolicitud)
            .orElseThrow(() -> new NoSuchElementException(
                "Solicitud no encontrada: " + idSolicitud));
        boolean esArtista = solicitud.getArtista() != null
            && solicitud.getArtista().getIdUsuario().equals(idArtista);
        if (!esArtista) {
            throw new AccesoDenegadoException(
                "No podés retirar el presupuesto de esta solicitud.");
        }
        if (solicitud.getEstado() != EstadoSolicitud.PRESUPUESTADA) {
            throw new ConflictoException(
                "Solo podés retirar el presupuesto de una solicitud presupuestada.");
        }

        Snapshot antes = Snapshot.de(solicitud, CAMPOS_SOLICITUD);
        LocalDateTime ahora = LocalDateTime.now();
        solicitud.setEstado(EstadoSolicitud.RECHAZADA);
        solicitud.setFechaActualizacion(ahora);

        AccionSolicitud accion = new AccionSolicitud();
        accion.setSolicitud(solicitud);
        accion.setTipoAccion(TipoAccionSolicitud.RETIRO_PRESUPUESTO);
        accion.setUsuario(solicitud.getArtista());
        accion.setFecha(ahora);
        accion.setOrigen(ORIGEN_ARTISTA);
        accionSolicitudRepository.save(accion);

        notificacionService.crearParaUsuario(
            solicitud.getCliente().getIdUsuario(),
            NOTIFICACION_RECHAZO,
            "Presupuesto retirado",
            "El artista retiró el presupuesto de '" + nombreServicioDe(solicitud)
                + "'. Podés volver a enviar tu solicitud.",
            solicitud.getIdSolicitud(),
            TIPO_REFERENCIA_SOLICITUD);

        cierreEmailService.encolar(
            solicitud.getCliente(),
            solicitud,
            null,
            MotivoSustitucion.PRESUPUESTO_RETIRADO,
            ahora);

        if (solicitud.getServicio() != null) {
            cuposEventPublisher.publicar(solicitud.getServicio().getIdServicio());
        }

        auditoria.registrar(
            AccionesAdmin.SOLICITUD_RETIRAR_PRESUPUESTO, ENTIDAD_SOLICITUD,
            solicitud.getIdSolicitud(), "Retiró el presupuesto de la solicitud",
            antes, solicitud, CAMPOS_SOLICITUD);

        SolicitudDTO dto = aDTO(solicitud, idArtista);
        dto.setFechaRechazo(ahora);
        return dto;
    }

    @Transactional
    public int cerrarPorBajaServicio(Long idServicio) {
        List<Solicitud> abiertas = solicitudRepository
            .findByServicioIdServicioAndEstadoInOrderByFechaCreacionDesc(
                idServicio, ESTADOS_QUE_SE_CIERRAN_AL_ARCHIVAR);
        for (Solicitud solicitud : abiertas) {
            registrarRechazo(solicitud, null, ORIGEN_BAJA_SERVICIO);
        }
        return abiertas.size();
    }

    private LocalDateTime registrarRechazo(Solicitud solicitud, String motivo, String origen) {
        if (solicitud.getEstado() == EstadoSolicitud.EN_LISTA_ESPERA) {
            listaEsperaService.marcarRechazada(solicitud);
        }

        LocalDateTime ahora = LocalDateTime.now();
        solicitud.setEstado(EstadoSolicitud.RECHAZADA);
        solicitud.setFechaActualizacion(ahora);

        AccionSolicitud accion = new AccionSolicitud();
        accion.setSolicitud(solicitud);
        accion.setTipoAccion(TipoAccionSolicitud.RECHAZO);
        accion.setUsuario(solicitud.getArtista());
        accion.setFecha(ahora);
        accion.setMotivo(motivo);
        accion.setOrigen(origen);
        accionSolicitudRepository.save(accion);

        String nombreServicio = nombreServicioDe(solicitud);
        String descripcion = motivo != null
            ? "El artista rechazó tu solicitud de '" + nombreServicio + "'. Motivo: " + motivo
            : "El artista rechazó tu solicitud de '" + nombreServicio
                + "'. El artista decidió no mencionar el motivo.";
        notificacionService.crearParaUsuario(
            solicitud.getCliente().getIdUsuario(),
            NOTIFICACION_RECHAZO,
            "Solicitud rechazada",
            descripcion,
            solicitud.getIdSolicitud(),
            TIPO_REFERENCIA_SOLICITUD);

        cierreEmailService.encolar(
            solicitud.getCliente(),
            solicitud,
            null,
            ORIGEN_BAJA_SERVICIO.equals(origen)
                ? MotivoSustitucion.SERVICIO_DADO_DE_BAJA
                : MotivoSustitucion.SOLICITUD_RECHAZADA,
            ahora);

        conversacionService.archivarPorSolicitud(
            solicitud.getCliente(), solicitud.getArtista());

        return ahora;
    }

    private String nombreServicioDe(Solicitud solicitud) {
        return solicitud.getServicio() != null
            ? solicitud.getServicio().getNombre()
            : "tu servicio";
    }

    private void notificarCancelacion(
            Solicitud solicitud, String motivo, EstadoSolicitud estadoAnterior) {
        StringBuilder descripcion = new StringBuilder()
            .append("El cliente canceló la solicitud de '")
            .append(nombreServicioDe(solicitud))
            .append("'. ");
        if (motivo != null) {
            descripcion.append("Motivo: ").append(motivo);
        } else {
            descripcion.append("El cliente decidió no mencionar el motivo.");
        }
        if (estadoAnterior == EstadoSolicitud.PRESUPUESTADA) {
            descripcion.append(" El cupo reservado quedó liberado.");
        }

        notificacionService.crearParaUsuario(
            solicitud.getArtista().getIdUsuario(),
            NOTIFICACION_CANCELACION,
            "Solicitud cancelada",
            descripcion.toString(),
            solicitud.getIdSolicitud(),
            TIPO_REFERENCIA_SOLICITUD);
    }

    @Transactional(readOnly = true)
    public List<SolicitudResumenDTO> listarRecibidas(Long idArtista, EstadoSolicitud estado) {
        List<Solicitud> solicitudes = estado == null
            ? solicitudRepository.findByArtistaIdUsuarioOrderByFechaCreacionDesc(idArtista)
            : solicitudRepository.findByArtistaIdUsuarioAndEstadoOrderByFechaCreacionDesc(
                idArtista, estado);
        return aResumenesConReputacion(solicitudes, idArtista);
    }

    @Transactional(readOnly = true)
    public List<SolicitudResumenDTO> listarRecibidasEnEstados(
            Long idArtista, Collection<EstadoSolicitud> estados) {
        return aResumenesConReputacion(
            solicitudRepository.findByArtistaIdUsuarioAndEstadoInOrderByFechaCreacionDesc(
                idArtista, estados),
            idArtista);
    }

    private List<SolicitudResumenDTO> aResumenesConReputacion(
            List<Solicitud> solicitudes, Long idArtista) {
        List<SolicitudResumenDTO> dtos = solicitudes.stream()
            .map(solicitud -> aResumen(solicitud, idArtista))
            .toList();

        List<Long> idsCliente = solicitudes.stream()
            .map(solicitud -> solicitud.getCliente().getIdUsuario())
            .distinct()
            .toList();
        Map<Long, ReputacionClienteDTO> reputacionesCrudas =
            reseniaPublicaService.reputacionPorClientes(idsCliente);
        final Map<Long, ReputacionClienteDTO> reputaciones =
            reputacionesCrudas != null ? reputacionesCrudas : Map.of();
        dtos.forEach(dto -> {
            if (dto.getCliente() != null) {
                dto.setReputacionCliente(
                    reputaciones.get(dto.getCliente().getIdUsuario()));
            }
        });
        return dtos;
    }

    @Transactional(readOnly = true)
    public List<SolicitudResumenDTO> listarEnviadas(Long idCliente, EstadoSolicitud estado) {
        List<Solicitud> solicitudes = estado == null
            ? solicitudRepository.findByClienteIdUsuarioOrderByFechaCreacionDesc(idCliente)
            : solicitudRepository.findByClienteIdUsuarioAndEstadoOrderByFechaCreacionDesc(
                idCliente, estado);
        return solicitudes.stream()
            .map(solicitud -> aResumen(solicitud, idCliente))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<SolicitudResumenDTO> listarPorServicio(Long idServicio) {
        return listarPorServicio(idServicio, null);
    }

    @Transactional(readOnly = true)
    public List<SolicitudResumenDTO> listarPorServicio(Long idServicio, Long idVista) {
        return solicitudRepository.findByServicioIdServicioOrderByFechaCreacionDesc(idServicio)
            .stream()
            .map(solicitud -> aResumen(solicitud, idVista))
            .toList();
    }

    private SolicitudDTO aDTO(Solicitud solicitud, Long idVista) {
        LocalDateTime ahora = LocalDateTime.now();
        Moneda mostrada = monedaVistaService.preferidaDe(idVista);
        BigDecimal factorOrigen = mostrada != null
            ? monedaVistaService.factorOrigenAMostrada(
                solicitud.getMoneda(), mostrada, ahora)
            : null;
        BigDecimal factorCobro = mostrada != null
            ? monedaVistaService.factorCobroAMostrada(mostrada, ahora)
            : null;
        boolean incluirComision = idVista != null
            && solicitud.getArtista() != null
            && idVista.equals(solicitud.getArtista().getIdUsuario());
        SolicitudDTO dto = SolicitudDTO.desdeSolicitud(
            solicitud, mostrada, factorOrigen, factorCobro, incluirComision);
        if (incluirComision && dto.getPresupuesto() != null) {
            dto.getPresupuesto().aplicarArancelMp(
                arancelEstimadoService.estimar().porcentajeTotal());
        }
        if (incluirComision && solicitud.getCliente() != null) {
            dto.setReputacionCliente(reputacionDe(
                solicitud.getCliente().getIdUsuario()));
        }
        aplicarConversacion(dto, solicitud);
        return dto;
    }

    private void aplicarConversacion(SolicitudDTO dto, Solicitud solicitud) {
        if (solicitud.getCliente() == null
                || solicitud.getArtista() == null
                || solicitud.getCliente().getIdUsuario() == null
                || solicitud.getArtista().getIdUsuario() == null) {
            return;
        }
        Conversacion conversacion = conversacionRepository
            .buscarPorPar(
                solicitud.getCliente().getIdUsuario(),
                solicitud.getArtista().getIdUsuario())
            .orElse(null);
        if (conversacion == null) {
            return;
        }
        dto.setIdConversacion(conversacion.getIdConversacion());
        if (conversacion.getEstado() != null) {
            dto.setEstadoConversacion(conversacion.getEstado().name());
        }
    }

    private ReputacionClienteDTO reputacionDe(Long idCliente) {
        Map<Long, ReputacionClienteDTO> reputaciones =
            reseniaPublicaService.reputacionPorClientes(List.of(idCliente));
        return reputaciones != null ? reputaciones.get(idCliente) : null;
    }

    private SolicitudResumenDTO aResumen(Solicitud solicitud, Long idVista) {
        LocalDateTime ahora = LocalDateTime.now();
        Moneda mostrada = monedaVistaService.preferidaDe(idVista);
        BigDecimal factorOrigen = mostrada != null
            ? monedaVistaService.factorOrigenAMostrada(
                solicitud.getMoneda(), mostrada, ahora)
            : null;
        BigDecimal factorCobro = mostrada != null
            ? monedaVistaService.factorCobroAMostrada(mostrada, ahora)
            : null;
        return SolicitudResumenDTO.desdeSolicitud(
            solicitud, mostrada, factorOrigen, factorCobro);
    }

    private int calcularOcupados(Long idServicio) {
        return colaSolicitudesService.cuposOcupados(idServicio);
    }

    private ModoRecepcionSolicitud modoDe(Servicio servicio) {
        return servicio.getModoRecepcion() != null
            ? servicio.getModoRecepcion()
            : ModoRecepcionSolicitud.ABIERTA;
    }

    private int maxDibujosDe(Servicio servicio) {
        return servicio.getMaxDibujosPorSolicitud() != null
            ? servicio.getMaxDibujosPorSolicitud()
            : 3;
    }

    private List<ItemPreparado> validarYPrepararItems(
            SolicitudServicioRequest request,
            FormularioDTO formularioDTO,
            Formulario formulario,
            Map<Long, TerminoDerecho> derechosDisponibles) {
        if (request.getItems().size() > 50) {
            throw new IllegalArgumentException(
                "Una solicitud puede tener como máximo 50 dibujos.");
        }

        Map<Long, PreguntaDTO> preguntasDto = new LinkedHashMap<>();
        for (PreguntaDTO pregunta : formularioDTO.getPreguntas()) {
            preguntasDto.put(pregunta.getIdPregunta(), pregunta);
        }
        Map<Long, Pregunta> preguntasEntidad = new LinkedHashMap<>();
        for (Pregunta pregunta : formulario.getPreguntas()) {
            preguntasEntidad.put(pregunta.getIdPregunta(), pregunta);
        }

        List<ItemPreparado> preparados = new ArrayList<>();
        for (ItemSolicitudRequest item : request.getItems()) {
            if (item == null || item.getNombre() == null || item.getNombre().isBlank()) {
                throw new IllegalArgumentException("El nombre de cada dibujo es obligatorio.");
            }

            Map<Long, RespuestaSolicitudRequest> respuestasPorPregunta = new LinkedHashMap<>();
            List<RespuestaSolicitudRequest> respuestas = item.getRespuestas() != null
                ? item.getRespuestas()
                : List.of();
            for (RespuestaSolicitudRequest respuesta : respuestas) {
                if (respuesta == null || respuesta.getIdPregunta() == null) {
                    throw new IllegalArgumentException("Cada respuesta debe indicar su pregunta.");
                }
                if (respuestasPorPregunta.putIfAbsent(
                        respuesta.getIdPregunta(), respuesta) != null) {
                    throw new IllegalArgumentException(
                        "La pregunta " + respuesta.getIdPregunta() + " está respondida más de una vez.");
                }
                if (!preguntasDto.containsKey(respuesta.getIdPregunta())) {
                    throw new IllegalArgumentException(
                        "La pregunta " + respuesta.getIdPregunta() + " no pertenece al formulario.");
                }
            }

            List<RespuestaPreparada> respuestasPreparadas = new ArrayList<>();
            int orden = 0;
            for (PreguntaDTO pregunta : formularioDTO.getPreguntas()) {
                RespuestaSolicitudRequest respuesta = respuestasPorPregunta.get(pregunta.getIdPregunta());
                List<String> valores = respuesta != null
                    ? limpiarValores(respuesta.getValores())
                    : List.of();

                if (ENCABEZADO.equals(pregunta.getCodigoTipo())) {
                    if (!valores.isEmpty()) {
                        throw new IllegalArgumentException(
                            "La pregunta '" + pregunta.getEnunciado() + "' es un encabezado y no admite respuestas.");
                    }
                    orden++;
                    continue;
                }
                if (pregunta.isObligatoria() && valores.isEmpty()) {
                    throw new IllegalArgumentException(
                        "La pregunta '" + pregunta.getEnunciado() + "' es obligatoria.");
                }
                if (valores.isEmpty()) {
                    orden++;
                    continue;
                }

                validarValores(pregunta, valores);
                Pregunta preguntaEntidad = preguntasEntidad.get(pregunta.getIdPregunta());
                if (preguntaEntidad == null) {
                    throw new IllegalStateException(
                        "La pregunta " + pregunta.getIdPregunta() + " no existe en la entidad.");
                }
                TipoCampo tipoCampo = preguntaEntidad.getTipoCampo();

                int suborden = 0;
                for (String valor : valores) {
                    respuestasPreparadas.add(new RespuestaPreparada(
                        preguntaEntidad,
                        tipoCampo,
                        pregunta.getEnunciado(),
                        valor,
                        orden,
                        suborden++));
                }
                orden++;
            }

            Integer prioridad = request.isPrioridadDefinida() ? item.getPrioridad() : null;
            List<ItemSolicitudDerecho> derechos = prepararDerechos(
                item.getDerechos(), derechosDisponibles);
            preparados.add(new ItemPreparado(
                item.getNombre().trim(),
                prioridad,
                respuestasPreparadas,
                derechos));
        }
        return preparados;
    }

    private void validarValores(PreguntaDTO pregunta, List<String> valores) {
        String tipo = pregunta.getCodigoTipo();
        if (tipo == null) {
            throw new IllegalArgumentException(
                "La pregunta '" + pregunta.getEnunciado() + "' no tiene un tipo de campo válido.");
        }
        switch (tipo) {
            case TEXTO -> {
                if (valores.size() != 1) {
                    throw new IllegalArgumentException(
                        "La pregunta '" + pregunta.getEnunciado() + "' admite un solo valor.");
                }
            }
            case OPCIONES -> {
                if (valores.size() != 1) {
                    throw new IllegalArgumentException(
                        "La pregunta '" + pregunta.getEnunciado() + "' admite una sola opción.");
                }
                if (pregunta.getOpciones() == null
                        || !pregunta.getOpciones().contains(valores.get(0))) {
                    throw new IllegalArgumentException(
                        "La opción elegida en '" + pregunta.getEnunciado() + "' no es válida.");
                }
            }
            case CASILLAS -> {
                Set<String> unicas = new HashSet<>();
                for (String valor : valores) {
                    if (!unicas.add(valor)) {
                        throw new IllegalArgumentException(
                            "La pregunta '" + pregunta.getEnunciado() + "' tiene opciones repetidas.");
                    }
                    if (pregunta.getOpciones() == null
                            || !pregunta.getOpciones().contains(valor)) {
                        throw new IllegalArgumentException(
                            "La opción '" + valor + "' no es válida para '"
                                + pregunta.getEnunciado() + "'.");
                    }
                }
            }
            case SUBIR_IMAGEN -> {
                if (valores.size() > 10) {
                    throw new IllegalArgumentException(
                        "La pregunta '" + pregunta.getEnunciado()
                            + "' admite como máximo 10 imágenes.");
                }
            }
            case ENCABEZADO -> throw new IllegalArgumentException(
                "La pregunta '" + pregunta.getEnunciado() + "' no admite respuestas.");
            default -> throw new IllegalArgumentException(
                "El tipo de campo '" + tipo + "' no está soportado.");
        }
    }

    private String limpiarMotivo(String motivo) {
        if (motivo == null) {
            return null;
        }
        String limpio = motivo.trim();
        if (limpio.isEmpty()) {
            return null;
        }
        if (limpio.length() > 200) {
            throw new IllegalArgumentException(
                "El motivo no puede superar los 200 caracteres.");
        }
        return limpio;
    }

    private List<String> limpiarValores(List<String> valores) {
        if (valores == null) {
            return List.of();
        }
        return valores.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(valor -> !valor.isEmpty())
            .toList();
    }

    private List<ItemSolicitudDerecho> prepararDerechos(
            List<Long> ids,
            Map<Long, TerminoDerecho> derechosDisponibles) {
        List<ItemSolicitudDerecho> derechos = new ArrayList<>();
        if (ids == null || ids.isEmpty()) {
            return derechos;
        }

        Set<Long> vistos = new HashSet<>();
        for (Long id : ids) {
            if (id == null || !vistos.add(id)) {
                continue;
            }
            TerminoDerecho terminoDerecho = derechosDisponibles.get(id);
            if (terminoDerecho == null || !terminoDerecho.isDisponible()) {
                throw new IllegalArgumentException(
                    "El derecho de uso seleccionado no está disponible para este servicio.");
            }

            DerechoTermino derecho = terminoDerecho.getDerecho();
            if (derecho == null) {
                throw new IllegalArgumentException(
                    "El derecho de uso seleccionado no está disponible para este servicio.");
            }
            if (DERECHO_PERSONAL.equals(derecho.getCodigo())) {
                throw new IllegalArgumentException(
                    "El uso personal ya está incluido y no se elige como derecho extra.");
            }
            if (derecho.isRequierePiezaUnica()) {
                throw new IllegalArgumentException(
                    "El derecho '" + derecho.getNombre()
                        + "' no está disponible para comisiones.");
            }

            ItemSolicitudDerecho itemDerecho = new ItemSolicitudDerecho();
            itemDerecho.setDerecho(derecho);
            itemDerecho.setCodigo(derecho.getCodigo());
            itemDerecho.setNombre(derecho.getNombre());
            itemDerecho.setPorcentajeExtra(
                terminoDerecho.getPorcentajeExtra() != null
                    ? terminoDerecho.getPorcentajeExtra()
                    : BigDecimal.ZERO);
            itemDerecho.setOrden(derechos.size());
            derechos.add(itemDerecho);
        }
        return derechos;
    }

    private void validarPrioridades(List<ItemPreparado> items) {
        int total = items.size();
        Set<Integer> usadas = new HashSet<>();
        for (ItemPreparado item : items) {
            Integer prioridad = item.prioridad();
            if (prioridad == null) {
                throw new IllegalArgumentException(
                    "Definí la prioridad de todos los dibujos o elegí 'sin preferencia'.");
            }
            if (prioridad < 1 || prioridad > total) {
                throw new IllegalArgumentException(
                    "La prioridad debe estar entre 1 y " + total + ".");
            }
            if (!usadas.add(prioridad)) {
                throw new IllegalArgumentException(
                    "No podés repetir la prioridad " + prioridad + ".");
            }
        }
        if (usadas.size() != total) {
            throw new IllegalArgumentException(
                "Las prioridades deben ser únicas y estar entre 1 y " + total + ".");
        }
    }

    private Solicitud crearSolicitud(
            Servicio servicio,
            Usuario cliente,
            Usuario artista,
            Formulario formulario,
            Terminos terminos,
            Integer terminosVersion,
            LocalDateTime terminosAceptadosEn,
            String terminosSnapshot,
            boolean prioridadDefinida,
            EstadoSolicitud estado,
            List<ItemPreparado> items) {
        Solicitud solicitud = new Solicitud();
        solicitud.setServicio(servicio);
        solicitud.setCliente(cliente);
        solicitud.setArtista(artista);
        solicitud.setFormulario(formulario);
        solicitud.setTerminos(terminos);
        solicitud.setTerminosVersion(terminosVersion);
        solicitud.setTerminosAceptadosEn(terminosAceptadosEn);
        solicitud.setTerminosSnapshot(terminosSnapshot);
        solicitud.setPrioridadDefinidaPorCliente(prioridadDefinida);
        solicitud.setEstado(estado);
        solicitud.setCantidadDibujos(items.size());
        solicitud.setPrecioBase(servicio.getPrecioBase());
        solicitud.setMoneda(servicio.getMoneda());
        solicitud.setFechaActualizacion(LocalDateTime.now());

        int orden = 0;
        for (ItemPreparado itemPreparado : items) {
            ItemSolicitud item = new ItemSolicitud();
            item.setNombre(itemPreparado.nombre());
            item.setOrden(orden++);
            item.setOrdenPrioridad(prioridadDefinida ? itemPreparado.prioridad() : null);
            item.setEstado("PENDIENTE");

            for (RespuestaPreparada respuestaPreparada : itemPreparado.respuestas()) {
                Respuesta respuesta = new Respuesta();
                respuesta.setPregunta(respuestaPreparada.pregunta());
                respuesta.setTipoCampo(respuestaPreparada.tipoCampo());
                respuesta.setEnunciado(respuestaPreparada.enunciado());
                respuesta.setRespuesta(respuestaPreparada.valor());
                respuesta.setOrden(respuestaPreparada.orden());
                respuesta.setSuborden(respuestaPreparada.suborden());
                item.agregarRespuesta(respuesta);
            }
            for (ItemSolicitudDerecho derecho : itemPreparado.derechos()) {
                item.agregarDerecho(derecho);
            }
            solicitud.agregarItem(item);
        }
        return solicitud;
    }

    private String serializarTerminos(TerminosDTO terminos) {
        StringBuilder json = new StringBuilder("{");
        json.append("\"idTerminos\":").append(terminos.getIdTerminos()).append(',');
        json.append("\"idServicio\":").append(terminos.getIdServicio()).append(',');
        json.append("\"version\":").append(terminos.getVersion()).append(',');
        json.append("\"informacionGeneral\":").append(jsonTexto(terminos.getInformacionGeneral())).append(',');
        json.append("\"politicaRevisiones\":").append(jsonTexto(terminos.getPoliticaRevisiones())).append(',');
        json.append("\"politicaCancelaciones\":").append(jsonTexto(terminos.getPoliticaCancelaciones())).append(',');
        json.append("\"usoArtista\":").append(jsonTexto(terminos.getUsoArtista())).append(',');
        json.append("\"usoCliente\":").append(jsonTexto(terminos.getUsoCliente())).append(',');
        json.append("\"pagoExtra\":").append(jsonTexto(terminos.getPagoExtra())).append(',');
        json.append("\"queDibuja\":").append(jsonTexto(terminos.getQueDibuja())).append(',');
        json.append("\"queNoDibuja\":").append(jsonTexto(terminos.getQueNoDibuja())).append(',');
        json.append("\"derechos\":[");
        if (terminos.getDerechos() != null) {
            boolean primero = true;
            for (TerminoDerechoDTO derecho : terminos.getDerechos()) {
                if (!primero) {
                    json.append(',');
                }
                primero = false;
                json.append("{\"idDerechoTermino\":").append(derecho.getIdDerechoTermino());
                json.append(",\"codigo\":").append(jsonTexto(derecho.getCodigo()));
                json.append(",\"nombre\":").append(jsonTexto(derecho.getNombre()));
                json.append(",\"descripcion\":").append(jsonTexto(derecho.getDescripcion()));
                json.append(",\"requierePiezaUnica\":").append(derecho.isRequierePiezaUnica());
                json.append(",\"disponible\":").append(derecho.isDisponible());
                json.append(",\"porcentajeExtra\":").append(derecho.getPorcentajeExtra());
                json.append('}');
            }
        }
        json.append("]}");
        return json.toString();
    }

    private String jsonTexto(String valor) {
        if (valor == null) {
            return "null";
        }
        StringBuilder texto = new StringBuilder("\"");
        for (char c : valor.toCharArray()) {
            switch (c) {
                case '"' -> texto.append("\\\"");
                case '\\' -> texto.append("\\\\");
                case '\n' -> texto.append("\\n");
                case '\r' -> texto.append("\\r");
                case '\t' -> texto.append("\\t");
                default -> {
                    if (c < 0x20) {
                        texto.append(String.format("\\u%04x", (int) c));
                    } else {
                        texto.append(c);
                    }
                }
            }
        }
        return texto.append('"').toString();
    }

    private record ItemPreparado(
            String nombre,
            Integer prioridad,
            List<RespuestaPreparada> respuestas,
            List<ItemSolicitudDerecho> derechos) {
    }

    private record RespuestaPreparada(
            Pregunta pregunta,
            TipoCampo tipoCampo,
            String enunciado,
            String valor,
            int orden,
            int suborden) {
    }
}
