package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.AccionSolicitud;
import com.temipora.backend_springboot.modelo.CierreSustitucionDTO;
import com.temipora.backend_springboot.modelo.ConfiguracionSustitucion;
import com.temipora.backend_springboot.modelo.CrearSustitucionRequest;
import com.temipora.backend_springboot.modelo.DisparadorSustitucion;
import com.temipora.backend_springboot.modelo.EPreferenciaContenidoSensible;
import com.temipora.backend_springboot.modelo.EstadoPagoHito;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.EstadoServicio;
import com.temipora.backend_springboot.modelo.EstadoSolicitud;
import com.temipora.backend_springboot.modelo.EstadoSustitucion;
import com.temipora.backend_springboot.modelo.HitoPedido;
import com.temipora.backend_springboot.modelo.ModoRecepcionSolicitud;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.MotivoSustitucion;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.ServicioDTO;
import com.temipora.backend_springboot.modelo.Solicitud;
import com.temipora.backend_springboot.modelo.Sustitucion;
import com.temipora.backend_springboot.modelo.SustitucionDTO;
import com.temipora.backend_springboot.modelo.SustitucionOpcion;
import com.temipora.backend_springboot.modelo.SustitucionOpcionDTO;
import com.temipora.backend_springboot.modelo.SustitucionResumenDTO;
import com.temipora.backend_springboot.modelo.TipoAccionSolicitud;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.AccionSolicitudRepository;
import com.temipora.backend_springboot.repositorios.PedidoRepository;
import com.temipora.backend_springboot.repositorios.ServicioRepository;
import com.temipora.backend_springboot.repositorios.SolicitudRepository;
import com.temipora.backend_springboot.repositorios.SustitucionOpcionRepository;
import com.temipora.backend_springboot.repositorios.SustitucionRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

/**
 * Proceso de sustituciones: cuando el servicio que el cliente quiere
 * contratar no puede recibir una solicitud, se ofrecen servicios similares
 * de otros artistas que sí tienen cupo. El evento y las opciones mostradas
 * quedan registrados para medir conversión y auditar el proceso.
 */
@Service
public class SustitucionService {

    private static final List<EstadoSolicitud> ESTADOS_ACTIVOS = List.of(
        EstadoSolicitud.PENDIENTE,
        EstadoSolicitud.EN_LISTA_ESPERA,
        EstadoSolicitud.PRESUPUESTADA,
        EstadoSolicitud.PAGADA);

    private static final List<EstadoSustitucion> ESTADOS_RESUELTOS = List.of(
        EstadoSustitucion.RESUELTA_ALTERNATIVA,
        EstadoSustitucion.RESUELTA_COLA,
        EstadoSustitucion.DESCARTADA);

    private final SustitucionRepository sustitucionRepository;
    private final SustitucionOpcionRepository opcionRepository;
    private final ServicioRepository servicioRepository;
    private final SolicitudRepository solicitudRepository;
    private final PedidoRepository pedidoRepository;
    private final AccionSolicitudRepository accionSolicitudRepository;
    private final UsuarioRepository usuarioRepository;
    private final ServicioService servicioService;
    private final RecomendacionService recomendacionService;
    private final ColaSolicitudesService colaSolicitudesService;
    private final ConfiguracionSustitucionService configuracionService;
    private final MonedaService monedaService;

    public SustitucionService(
            SustitucionRepository sustitucionRepository,
            SustitucionOpcionRepository opcionRepository,
            ServicioRepository servicioRepository,
            SolicitudRepository solicitudRepository,
            PedidoRepository pedidoRepository,
            AccionSolicitudRepository accionSolicitudRepository,
            UsuarioRepository usuarioRepository,
            ServicioService servicioService,
            RecomendacionService recomendacionService,
            ColaSolicitudesService colaSolicitudesService,
            ConfiguracionSustitucionService configuracionService,
            MonedaService monedaService) {
        this.sustitucionRepository = sustitucionRepository;
        this.opcionRepository = opcionRepository;
        this.servicioRepository = servicioRepository;
        this.solicitudRepository = solicitudRepository;
        this.pedidoRepository = pedidoRepository;
        this.accionSolicitudRepository = accionSolicitudRepository;
        this.usuarioRepository = usuarioRepository;
        this.servicioService = servicioService;
        this.recomendacionService = recomendacionService;
        this.colaSolicitudesService = colaSolicitudesService;
        this.configuracionService = configuracionService;
        this.monedaService = monedaService;
    }

    @Transactional
    public SustitucionDTO crear(Long idCliente, CrearSustitucionRequest request) {
        if (request == null || request.getIdServicioOrigen() == null) {
            throw new IllegalArgumentException("El servicio de origen es obligatorio.");
        }

        ConfiguracionSustitucion config = configuracionService.obtener();
        Servicio origen = servicioRepository
            .findByIdServicio(request.getIdServicioOrigen())
            .orElseThrow(() -> new NoSuchElementException(
                "Servicio no encontrado: " + request.getIdServicioOrigen()));

        int cuposTotales = origen.getCupos() == null ? 0 : origen.getCupos();
        int cuposOcupados = colaSolicitudesService.cuposOcupados(origen.getIdServicio());
        int cuposDisponibles = Math.max(0, cuposTotales - cuposOcupados);
        MotivoSustitucion motivo = motivoDe(origen, cuposDisponibles);

        if (motivo == null || !config.isHabilitado()) {
            return SustitucionDTO.builder()
                .motivo(motivo != null ? motivo.name() : null)
                .mensaje(motivo != null ? mensaje(motivo) : null)
                .puedeQuedarEnCola(false)
                .cuposDisponibles(cuposDisponibles)
                .cuposTotales(cuposTotales)
                .opciones(List.of())
                .build();
        }

        DisparadorSustitucion disparador = parseDisparador(request.getDisparador());

        Sustitucion reutilizable = buscarReutilizable(
            idCliente, origen.getIdServicio(), motivo, config);
        if (reutilizable != null) {
            return aDTO(reutilizable, idCliente);
        }

        List<RecomendacionService.ServicioPuntuado> ranked =
            rankear(idCliente, origen, config);
        Sustitucion guardada = guardarEvento(
            idCliente, origen, motivo, disparador, null, null, ranked);
        return armarDto(guardada, ranked, idCliente, motivo,
            cuposDisponibles, cuposTotales);
    }

    private List<RecomendacionService.ServicioPuntuado> rankear(
            Long idCliente,
            Servicio origen,
            ConfiguracionSustitucion config) {
        return recomendacionService.similares(
            origen,
            candidatos(idCliente, origen),
            idCliente,
            monedaPreferidaDe(idCliente),
            opcionesDeSimilitud(config));
    }

    private Sustitucion guardarEvento(
            Long idCliente,
            Servicio origen,
            MotivoSustitucion motivo,
            DisparadorSustitucion disparador,
            Long idSolicitud,
            Long idPedido,
            List<RecomendacionService.ServicioPuntuado> ranked) {
        Sustitucion sustitucion = new Sustitucion();
        sustitucion.setCliente(idCliente != null
            ? usuarioRepository.findByIdUsuario(idCliente).orElse(null)
            : null);
        sustitucion.setServicioOrigen(origen);
        sustitucion.setArtistaOrigen(origen.getArtista());
        sustitucion.setMotivo(motivo);
        sustitucion.setDisparador(disparador);
        sustitucion.setEstado(EstadoSustitucion.ABIERTA);
        sustitucion.setCantidadOpciones(ranked.size());
        if (idSolicitud != null) {
            sustitucion.setSolicitud(
                solicitudRepository.findByIdSolicitud(idSolicitud).orElse(null));
        }
        if (idPedido != null) {
            sustitucion.setPedido(
                pedidoRepository.findById(idPedido).orElse(null));
        }

        int orden = 0;
        for (RecomendacionService.ServicioPuntuado item : ranked) {
            Servicio servicio = item.servicio();
            SustitucionOpcion opcion = new SustitucionOpcion();
            opcion.setServicio(servicio);
            opcion.setArtista(servicio.getArtista());
            opcion.setOrden(orden++);
            opcion.setScore(BigDecimal.valueOf(item.score())
                .setScale(4, RoundingMode.HALF_UP));
            opcion.setMotivo(recortar(item.motivo(), 80));
            opcion.setPrecioMomento(servicio.getPrecioBase());
            opcion.setMoneda(servicio.getMoneda());
            sustitucion.agregarOpcion(opcion);
        }
        return sustitucionRepository.save(sustitucion);
    }

    @Transactional
    public SustitucionDTO elegir(Long idSustitucion, Long idCliente, Long idServicio) {
        Sustitucion sustitucion = buscarPropia(idSustitucion, idCliente);
        if (ESTADOS_RESUELTOS.contains(sustitucion.getEstado())) {
            throw new ConflictoException(
                "Esta búsqueda de alternativas ya está cerrada.");
        }

        SustitucionOpcion elegida = opcionRepository
            .findBySustitucionIdSustitucionAndServicioIdServicio(
                idSustitucion, idServicio)
            .orElseThrow(() -> new IllegalArgumentException(
                "Ese servicio no forma parte de las alternativas ofrecidas."));

        Servicio servicio = elegida.getServicio();
        boolean disponible = servicio.getEstado() == EstadoServicio.PUBLICADO
            && servicio.getModoRecepcion() == ModoRecepcionSolicitud.ABIERTA
            && colaSolicitudesService.cuposDisponibles(servicio) > 0;
        if (!disponible) {
            throw new ConflictoException(
                "Esa alternativa ya no tiene cupos disponibles. Volvé a ver las opciones.");
        }

        for (SustitucionOpcion opcion : sustitucion.getOpciones()) {
            opcion.setElegida(opcion.getIdSustitucionOpcion() != null
                && opcion.getIdSustitucionOpcion()
                    .equals(elegida.getIdSustitucionOpcion()));
        }
        sustitucion.setEstado(EstadoSustitucion.ALTERNATIVA_ELEGIDA);
        sustitucion.setServicioElegido(servicio);
        sustitucionRepository.save(sustitucion);
        return aDTO(sustitucion, idCliente);
    }

    @Transactional
    public void descartar(Long idSustitucion, Long idCliente) {
        Sustitucion sustitucion = buscarPropia(idSustitucion, idCliente);
        if (sustitucion.getEstado() == EstadoSustitucion.DESCARTADA) {
            return;
        }
        sustitucion.setEstado(EstadoSustitucion.DESCARTADA);
        sustitucion.setFechaResolucion(LocalDateTime.now());
        sustitucionRepository.save(sustitucion);
    }

    /**
     * Cierra el evento cuando la solicitud se crea efectivamente. Si el
     * servicio solicitado es el de origen se resolvió quedando en cola; si es
     * otro, se resolvió con una alternativa.
     */
    @Transactional
    public void resolverPorSolicitud(
            Long idSustitucion,
            Long idCliente,
            Long idSolicitud,
            Long idServicioSolicitado) {
        if (idSustitucion == null) {
            return;
        }
        Sustitucion sustitucion = sustitucionRepository.findById(idSustitucion)
            .orElse(null);
        if (sustitucion == null
                || ESTADOS_RESUELTOS.contains(sustitucion.getEstado())) {
            return;
        }
        if (sustitucion.getCliente() != null
                && !sustitucion.getCliente().getIdUsuario().equals(idCliente)) {
            return;
        }
        boolean fueCola = sustitucion.getServicioOrigen() != null
            && sustitucion.getServicioOrigen().getIdServicio()
                .equals(idServicioSolicitado);
        sustitucion.setEstado(fueCola
            ? EstadoSustitucion.RESUELTA_COLA
            : EstadoSustitucion.RESUELTA_ALTERNATIVA);
        if (!fueCola) {
            sustitucion.setServicioElegido(
                servicioRepository.findByIdServicio(idServicioSolicitado).orElse(null));
        }
        sustitucion.setSolicitud(
            solicitudRepository.findByIdSolicitud(idSolicitud).orElse(null));
        sustitucion.setFechaResolucion(LocalDateTime.now());
        sustitucionRepository.save(sustitucion);
    }

    @Transactional(readOnly = true)
    public SustitucionResumenDTO resumen() {
        long total = sustitucionRepository.count();
        long alternativa = sustitucionRepository.countByEstado(
            EstadoSustitucion.RESUELTA_ALTERNATIVA);
        long cola = sustitucionRepository.countByEstado(
            EstadoSustitucion.RESUELTA_COLA);
        long descartadas = sustitucionRepository.countByEstado(
            EstadoSustitucion.DESCARTADA);
        long abiertas = sustitucionRepository.countByEstado(
            EstadoSustitucion.ABIERTA);

        long resueltas = alternativa + cola;
        double tasa = resueltas > 0
            ? BigDecimal.valueOf(alternativa * 100.0 / resueltas)
                .setScale(2, RoundingMode.HALF_UP).doubleValue()
            : 0;

        return SustitucionResumenDTO.builder()
            .totalEventos(total)
            .resueltasConAlternativa(alternativa)
            .resueltasConCola(cola)
            .descartadas(descartadas)
            .abiertas(abiertas)
            .tasaSustitucion(tasa)
            .build();
    }

    @Transactional(readOnly = true)
    public SustitucionDTO obtener(Long idSustitucion, Long idCliente) {
        Sustitucion sustitucion = sustitucionRepository.findById(idSustitucion)
            .orElseThrow(() -> new NoSuchElementException(
                "Búsqueda de alternativas no encontrada: " + idSustitucion));
        if (sustitucion.getCliente() == null
                || !sustitucion.getCliente().getIdUsuario().equals(idCliente)) {
            throw new IllegalArgumentException(
                "No podés ver una búsqueda de alternativas que no es tuya.");
        }
        return aDTO(sustitucion, idCliente);
    }

    /**
     * Resultado de una solicitud cerrada por una causa que el cliente no
     * eligió: rechazo, retiro o vencimiento del presupuesto, o baja del
     * servicio. Genera (o reutiliza) el evento y devuelve el resumen junto
     * con las alternativas.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public CierreSustitucionDTO detalleCierrePorSolicitud(
            Long idSolicitud,
            Long idCliente) {
        Solicitud solicitud = solicitudRepository.findByIdSolicitud(idSolicitud)
            .orElseThrow(() -> new NoSuchElementException(
                "Solicitud no encontrada: " + idSolicitud));
        if (solicitud.getCliente() == null
                || !solicitud.getCliente().getIdUsuario().equals(idCliente)) {
            throw new IllegalArgumentException(
                "No podés ver el resultado de esta solicitud.");
        }
        MotivoSustitucion motivo = motivoCierreSolicitud(solicitud);
        return construirCierre(
            "SOLICITUD",
            solicitud.getServicio(),
            motivo,
            solicitud.getIdSolicitud(),
            null,
            fechaCierreSolicitud(solicitud),
            idCliente);
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public CierreSustitucionDTO detalleCierrePorPedido(
            Long idPedido,
            Long idCliente) {
        Pedido pedido = pedidoRepository.findById(idPedido)
            .orElseThrow(() -> new NoSuchElementException(
                "Pedido no encontrado: " + idPedido));
        if (pedido.getCliente() == null
                || !pedido.getCliente().getIdUsuario().equals(idCliente)) {
            throw new IllegalArgumentException("No podés ver el resultado de este pedido.");
        }
        if (pedido.getEstado() != EstadoPedido.IMPAGO) {
            throw new ConflictoException(
                "Este pedido no está cerrado por falta de pago.");
        }
        HitoPedido vencido = pedido.getHitos().stream()
            .filter(hito -> hito.getEstadoPago() == EstadoPagoHito.VENCIDO)
            .findFirst()
            .orElse(null);
        LocalDateTime fecha = vencido != null && vencido.getFechaLimitePago() != null
            ? vencido.getFechaLimitePago()
            : pedido.getFechaCreacion();
        return construirCierre(
            "PEDIDO",
            pedido.getServicio(),
            MotivoSustitucion.PEDIDO_IMPAGO,
            pedido.getSolicitud() != null
                ? pedido.getSolicitud().getIdSolicitud()
                : null,
            pedido.getIdPedido(),
            fecha,
            idCliente);
    }

    private MotivoSustitucion motivoCierreSolicitud(Solicitud solicitud) {
        if (solicitud.getEstado() == EstadoSolicitud.VENCIDA) {
            return MotivoSustitucion.PRESUPUESTO_VENCIDO;
        }
        if (solicitud.getEstado() != EstadoSolicitud.RECHAZADA) {
            throw new ConflictoException(
                "Esta solicitud todavía está activa: no hay un cierre para mostrar.");
        }
        if (solicitud.getServicio() != null
                && solicitud.getServicio().getEstado() == EstadoServicio.ARCHIVADO) {
            return MotivoSustitucion.SERVICIO_DADO_DE_BAJA;
        }
        AccionSolicitud ultima = accionSolicitudRepository
            .findFirstBySolicitudIdSolicitudOrderByFechaDescIdAccionSolicitudDesc(
                solicitud.getIdSolicitud())
            .orElse(null);
        if (ultima != null
                && ultima.getTipoAccion() == TipoAccionSolicitud.RETIRO_PRESUPUESTO) {
            return MotivoSustitucion.PRESUPUESTO_RETIRADO;
        }
        return MotivoSustitucion.SOLICITUD_RECHAZADA;
    }

    private LocalDateTime fechaCierreSolicitud(Solicitud solicitud) {
        return accionSolicitudRepository
            .findFirstBySolicitudIdSolicitudOrderByFechaDescIdAccionSolicitudDesc(
                solicitud.getIdSolicitud())
            .map(AccionSolicitud::getFecha)
            .orElse(solicitud.getFechaActualizacion());
    }

    private CierreSustitucionDTO construirCierre(
            String contexto,
            Servicio origen,
            MotivoSustitucion motivo,
            Long idSolicitud,
            Long idPedido,
            LocalDateTime fecha,
            Long idCliente) {
        ConfiguracionSustitucion config = configuracionService.obtener();
        if (origen == null) {
            return CierreSustitucionDTO.builder()
                .contexto(contexto)
                .idSolicitud(idSolicitud)
                .idPedido(idPedido)
                .motivo(motivo.name())
                .titulo(titulo(motivo))
                .detalle(mensaje(motivo))
                .fecha(fecha)
                .procesoHabilitado(false)
                .opciones(List.of())
                .build();
        }

        ServicioDTO origenDto = servicioService
            .convertirParaVisor(List.of(origen), idCliente)
            .get(origen.getIdServicio());

        if (!config.isHabilitado()) {
            return CierreSustitucionDTO.builder()
                .contexto(contexto)
                .idSolicitud(idSolicitud)
                .idPedido(idPedido)
                .motivo(motivo.name())
                .titulo(titulo(motivo))
                .detalle(mensaje(motivo))
                .fecha(fecha)
                .servicio(origenDto)
                .procesoHabilitado(false)
                .opciones(List.of())
                .build();
        }

        Sustitucion evento = buscarReutilizable(
            idCliente, origen.getIdServicio(), motivo, config);
        if (evento == null) {
            List<RecomendacionService.ServicioPuntuado> ranked =
                rankear(idCliente, origen, config);
            evento = guardarEvento(
                idCliente,
                origen,
                motivo,
                DisparadorSustitucion.CIERRE,
                idSolicitud,
                idPedido,
                ranked);
        }
        SustitucionDTO dto = aDTO(evento, idCliente);
        return CierreSustitucionDTO.builder()
            .contexto(contexto)
            .idSolicitud(idSolicitud)
            .idPedido(idPedido)
            .idSustitucion(dto.getIdSustitucion())
            .motivo(motivo.name())
            .titulo(titulo(motivo))
            .detalle(mensaje(motivo))
            .fecha(fecha)
            .servicio(origenDto)
            .procesoHabilitado(true)
            .opciones(dto.getOpciones())
            .build();
    }

    private Sustitucion buscarPropia(Long idSustitucion, Long idCliente) {
        Sustitucion sustitucion = sustitucionRepository.findById(idSustitucion)
            .orElseThrow(() -> new NoSuchElementException(
                "Búsqueda de alternativas no encontrada: " + idSustitucion));
        if (sustitucion.getCliente() == null) {
            // La búsqueda se generó como visitante anónimo: al iniciar sesión
            // se le asigna el cliente para poder cerrar el circuito.
            sustitucion.setCliente(
                usuarioRepository.findByIdUsuario(idCliente).orElse(null));
            return sustitucion;
        }
        if (!sustitucion.getCliente().getIdUsuario().equals(idCliente)) {
            throw new IllegalArgumentException(
                "No podés modificar una búsqueda de alternativas que no es tuya.");
        }
        return sustitucion;
    }

    private Sustitucion buscarReutilizable(
            Long idCliente,
            Long idServicioOrigen,
            MotivoSustitucion motivo,
            ConfiguracionSustitucion config) {
        if (idCliente == null || config.getMinutosDedupe() <= 0) {
            return null;
        }
        LocalDateTime desde = LocalDateTime.now()
            .minusMinutes(config.getMinutosDedupe());
        List<Sustitucion> recientes = sustitucionRepository.recientes(
            idCliente,
            idServicioOrigen,
            motivo,
            EstadoSustitucion.ABIERTA,
            desde,
            PageRequest.of(0, 1));
        if (recientes.isEmpty()) {
            return null;
        }
        Sustitucion reciente = recientes.get(0);
        return reciente.getOpciones().isEmpty() ? null : reciente;
    }

    private List<Servicio> candidatos(Long idCliente, Servicio origen) {
        EPreferenciaContenidoSensible preferencia = idCliente != null
            ? usuarioRepository.findByIdUsuario(idCliente)
                .map(Usuario::getPreferenciaSensible)
                .orElse(EPreferenciaContenidoSensible.ADVERTIR)
            : EPreferenciaContenidoSensible.ADVERTIR;

        Set<Long> conSolicitudActiva = new HashSet<>();
        if (idCliente != null) {
            conSolicitudActiva.addAll(solicitudRepository
                .serviciosConSolicitudActiva(idCliente, ESTADOS_ACTIVOS));
        }

        List<Servicio> filtrados = new ArrayList<>();
        for (Servicio servicio : servicioService.candidatosDisponibles()) {
            if (servicio.getIdServicio() == null
                    || servicio.getIdServicio().equals(origen.getIdServicio())) {
                continue;
            }
            if (servicio.getArtista() == null
                    || !servicio.getArtista().isEstadoCuenta()) {
                continue;
            }
            if (idCliente != null
                    && servicio.getArtista().getIdUsuario().equals(idCliente)) {
                continue;
            }
            if (conSolicitudActiva.contains(servicio.getIdServicio())) {
                continue;
            }
            if (preferencia == EPreferenciaContenidoSensible.OCULTAR
                    && servicio.isContenidoSensible()) {
                continue;
            }
            filtrados.add(servicio);
        }
        return filtrados;
    }

    private RecomendacionService.OpcionesSimilitud opcionesDeSimilitud(
            ConfiguracionSustitucion config) {
        double bonus = config.getBonusFavorito() != null
            ? config.getBonusFavorito().doubleValue()
            : 0;
        double factorTiempo = config.getFactorTiempoMax() != null
            ? config.getFactorTiempoMax().doubleValue()
            : 0;
        return new RecomendacionService.OpcionesSimilitud(
            RecomendacionService.PesosSimilitud.conPesoFavorito(bonus),
            Math.max(1, config.getMaxAlternativas()),
            Math.max(1, config.getMaxPorArtista()),
            factorTiempo);
    }

    private Moneda monedaPreferidaDe(Long idCliente) {
        if (idCliente != null) {
            Moneda preferida = usuarioRepository.findByIdUsuario(idCliente)
                .map(Usuario::getMonedaPreferida)
                .orElse(null);
            if (preferida != null) {
                return preferida;
            }
        }
        return monedaService.predeterminada();
    }

    private MotivoSustitucion motivoDe(Servicio servicio, int cuposDisponibles) {
        if (servicio.getEstado() == EstadoServicio.ARCHIVADO) {
            return MotivoSustitucion.ARCHIVADO;
        }
        if (servicio.getEstado() == EstadoServicio.PAUSADO) {
            return MotivoSustitucion.PAUSADO;
        }
        ModoRecepcionSolicitud modo = servicio.getModoRecepcion() != null
            ? servicio.getModoRecepcion()
            : ModoRecepcionSolicitud.ABIERTA;
        if (modo == ModoRecepcionSolicitud.CERRADA) {
            return MotivoSustitucion.CERRADA;
        }
        if (modo == ModoRecepcionSolicitud.SOLO_ESPERA) {
            return MotivoSustitucion.SOLO_ESPERA;
        }
        if (cuposDisponibles <= 0) {
            return MotivoSustitucion.SIN_CUPOS;
        }
        return null;
    }

    private String mensaje(MotivoSustitucion motivo) {
        return switch (motivo) {
            case SIN_CUPOS ->
                "Este servicio no tiene cupos libres en este momento.";
            case SOLO_ESPERA ->
                "Este servicio recibe solicitudes solo en lista de espera.";
            case CERRADA ->
                "El artista no está recibiendo solicitudes para este servicio.";
            case PAUSADO -> "El servicio está pausado.";
            case ARCHIVADO -> "El servicio ya no está disponible.";
            case SOLICITUD_RECHAZADA ->
                "El artista rechazó tu solicitud. Podés buscar otro artista con "
                    + "características parecidas y volver a intentarlo.";
            case PRESUPUESTO_RETIRADO ->
                "El artista retiró el presupuesto. Estas son otras propuestas con cupo.";
            case PRESUPUESTO_VENCIDO ->
                "El presupuesto venció y el cupo quedó liberado. "
                    + "Si todavía querés la pieza, podés elegir otra propuesta.";
            case SERVICIO_DADO_DE_BAJA ->
                "El artista dio de baja este servicio. Te dejamos alternativas parecidas.";
            case PEDIDO_IMPAGO ->
                "El pedido se cerró por falta de pago. Podés retomar con otro artista.";
        };
    }

    private String titulo(MotivoSustitucion motivo) {
        return switch (motivo) {
            case SIN_CUPOS -> "Sin cupos por ahora";
            case SOLO_ESPERA -> "Solo lista de espera";
            case CERRADA -> "Recepción cerrada";
            case PAUSADO -> "Servicio pausado";
            case ARCHIVADO -> "Servicio no disponible";
            case SOLICITUD_RECHAZADA -> "El artista rechazó tu solicitud";
            case PRESUPUESTO_RETIRADO -> "El artista retiró el presupuesto";
            case PRESUPUESTO_VENCIDO -> "El presupuesto venció";
            case SERVICIO_DADO_DE_BAJA -> "El servicio ya no está disponible";
            case PEDIDO_IMPAGO -> "El pedido se cerró por falta de pago";
        };
    }

    private DisparadorSustitucion parseDisparador(String valor) {
        if (valor == null || valor.isBlank()) {
            return DisparadorSustitucion.DETALLE;
        }
        try {
            return DisparadorSustitucion.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return DisparadorSustitucion.DETALLE;
        }
    }

    private String recortar(String valor, int maximo) {
        if (valor == null) {
            return null;
        }
        return valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }

    private SustitucionDTO armarDto(
            Sustitucion sustitucion,
            List<RecomendacionService.ServicioPuntuado> ranked,
            Long idCliente,
            MotivoSustitucion motivo,
            int cuposDisponibles,
            int cuposTotales) {
        List<ServicioDTO> dtos = new ArrayList<>(servicioService
            .convertirParaVisor(
                ranked.stream()
                    .map(RecomendacionService.ServicioPuntuado::servicio)
                    .toList(),
                idCliente)
            .values());

        List<SustitucionOpcionDTO> opciones = new ArrayList<>();
        int orden = 0;
        for (RecomendacionService.ServicioPuntuado item : ranked) {
            ServicioDTO dto = dtos.stream()
                .filter(d -> d.getIdServicio().equals(item.servicio().getIdServicio()))
                .findFirst()
                .orElse(null);
            if (dto == null) {
                continue;
            }
            opciones.add(SustitucionOpcionDTO.builder()
                .idServicio(dto.getIdServicio())
                .orden(orden++)
                .score(item.score())
                .motivo(item.motivo())
                .caracteristicas(caracteristicas(
                    sustitucion != null ? sustitucion.getServicioOrigen() : null,
                    item.servicio(),
                    dto))
                .disponible(dto.isDisponibleAhora())
                .servicio(dto)
                .build());
        }

        return SustitucionDTO.builder()
            .idSustitucion(sustitucion != null ? sustitucion.getIdSustitucion() : null)
            .motivo(motivo.name())
            .mensaje(mensaje(motivo))
            .puedeQuedarEnCola(puedeQuedarEnCola(motivo))
            .cuposDisponibles(cuposDisponibles)
            .cuposTotales(cuposTotales)
            .idSolicitud(sustitucion != null && sustitucion.getSolicitud() != null
                ? sustitucion.getSolicitud().getIdSolicitud()
                : null)
            .idPedido(sustitucion != null && sustitucion.getPedido() != null
                ? sustitucion.getPedido().getIdPedido()
                : null)
            .fechaCreacion(sustitucion != null ? sustitucion.getFechaCreacion() : null)
            .servicioOrigen(sustitucion != null
                && sustitucion.getServicioOrigen() != null
                ? servicioService.convertirParaVisor(
                    List.of(sustitucion.getServicioOrigen()), idCliente)
                    .get(sustitucion.getServicioOrigen().getIdServicio())
                : null)
            .opciones(opciones)
            .build();
    }

    private SustitucionDTO aDTO(Sustitucion sustitucion, Long idCliente) {
        Servicio origen = sustitucion.getServicioOrigen();
        int cuposTotales = origen.getCupos() == null ? 0 : origen.getCupos();
        int cuposDisponibles = Math.max(0, cuposTotales
            - colaSolicitudesService.cuposOcupados(origen.getIdServicio()));

        List<SustitucionOpcion> opciones = opcionRepository
            .findBySustitucionIdSustitucionOrderByOrdenAsc(
                sustitucion.getIdSustitucion());
        List<Servicio> servicios = opciones.stream()
            .map(SustitucionOpcion::getServicio)
            .toList();
        var dtos = servicioService.convertirParaVisor(servicios, idCliente);

        List<SustitucionOpcionDTO> resultado = new ArrayList<>();
        for (SustitucionOpcion opcion : opciones) {
            ServicioDTO dto = dtos.get(opcion.getServicio().getIdServicio());
            if (dto == null) {
                continue;
            }
            if (origen != null
                    && dto.getIdServicio().equals(origen.getIdServicio())) {
                continue;
            }
            List<String> caracteristicas =
                caracteristicas(origen, opcion.getServicio(), dto);
            resultado.add(SustitucionOpcionDTO.builder()
                .idServicio(dto.getIdServicio())
                .orden(opcion.getOrden())
                .score(opcion.getScore() != null
                    ? opcion.getScore().doubleValue()
                    : 0)
                .motivo(motivoVisible(opcion.getMotivo(), caracteristicas))
                .caracteristicas(caracteristicas)
                .disponible(dto.isDisponibleAhora())
                .servicio(dto)
                .build());
        }

        return SustitucionDTO.builder()
            .idSustitucion(sustitucion.getIdSustitucion())
            .motivo(sustitucion.getMotivo().name())
            .mensaje(mensaje(sustitucion.getMotivo()))
            .puedeQuedarEnCola(puedeQuedarEnCola(sustitucion.getMotivo()))
            .cuposDisponibles(cuposDisponibles)
            .cuposTotales(cuposTotales)
            .idSolicitud(sustitucion.getSolicitud() != null
                ? sustitucion.getSolicitud().getIdSolicitud()
                : null)
            .idPedido(sustitucion.getPedido() != null
                ? sustitucion.getPedido().getIdPedido()
                : null)
            .fechaCreacion(sustitucion.getFechaCreacion())
            .servicioOrigen(origen != null
                ? servicioService.convertirParaVisor(List.of(origen), idCliente)
                    .get(origen.getIdServicio())
                : null)
            .opciones(resultado)
            .build();
    }

    /**
     * Devuelve las características que explican por qué una alternativa se
     * parece al servicio original. Se muestran como distintivos en el mail de
     * cierre y en la pantalla de sustituciones.
     */
    private List<String> caracteristicas(
            Servicio origen, Servicio candidato, ServicioDTO dto) {
        if (origen == null || candidato == null || dto == null) {
            return List.of();
        }
        List<String> caracteristicas = new ArrayList<>();
        List<String> etiquetas = etiquetasEnComun(origen, candidato);
        for (String etiqueta : etiquetas) {
            caracteristicas.add("Estilo: " + etiqueta);
        }
        if (mismoTipo(origen, candidato)) {
            String tipo = candidato.getTipoServicio() != null
                ? candidato.getTipoServicio().getNombre()
                : null;
            caracteristicas.add(tipo != null && !tipo.isBlank()
                ? "Mismo tipo: " + tipo
                : "Mismo tipo de servicio");
        }
        if (mismoMedio(origen, candidato)) {
            String medio = candidato.getMedio() != null
                ? candidato.getMedio().getNombre()
                : null;
            caracteristicas.add(medio != null && !medio.isBlank()
                ? "Mismo medio: " + medio
                : "Mismo medio");
        }
        String precio = compararPrecio(origen, candidato);
        if (precio != null) {
            caracteristicas.add(precio);
        }
        if (dto.getTiempoEntregaDias() != null
                && dto.getTiempoEntregaDias() > 0) {
            caracteristicas.add("Entrega en "
                + dto.getTiempoEntregaDias()
                + (dto.getTiempoEntregaDias() == 1 ? " día" : " días"));
        }
        if (dto.getCantidadResenias() != null && dto.getCantidadResenias() > 0
                && dto.getCalificacionPromedio() != null
                && dto.getCalificacionPromedio().doubleValue() >= 4.0) {
            caracteristicas.add("Buenas reseñas: "
                + dto.getCalificacionPromedio()
                    .setScale(1, RoundingMode.HALF_UP)
                    .toPlainString());
        }
        if (dto.isDisponibleAhora()) {
            caracteristicas.add("Cupos disponibles");
        }
        return caracteristicas;
    }

    /**
     * El motivo guardado puede ser anterior a la priorización de etiquetas.
     * Para mostrarlo, lo alineamos con las características que se ven en la
     * tarjeta.
     */
    private String motivoVisible(
            String motivoGuardado, List<String> caracteristicas) {
        if (caracteristicas.stream().anyMatch(c -> c.startsWith("Estilo:"))) {
            return "Mismo estilo";
        }
        if (caracteristicas.stream().anyMatch(c -> c.startsWith("Mismo tipo"))) {
            return "Mismo tipo de servicio";
        }
        if (caracteristicas.contains("Precio similar")) {
            return "Precio similar";
        }
        if (caracteristicas.stream().anyMatch(c -> c.startsWith("Precio "))) {
            return "Precio más bajo";
        }
        return motivoGuardado != null && !motivoGuardado.isBlank()
            ? motivoGuardado
            : "Tiene cupos disponibles";
    }

    private boolean mismoTipo(Servicio a, Servicio b) {
        return a.getTipoServicio() != null && b.getTipoServicio() != null
            && Objects.equals(a.getTipoServicio().getIdTipoServicio(),
                b.getTipoServicio().getIdTipoServicio());
    }

    private boolean mismoMedio(Servicio a, Servicio b) {
        return a.getMedio() != null && b.getMedio() != null
            && Objects.equals(a.getMedio().getIdMedio(),
                b.getMedio().getIdMedio());
    }

    private List<String> etiquetasEnComun(Servicio origen, Servicio candidato) {
        Set<Long> ids = new HashSet<>();
        for (var enlace : origen.getEtiquetas()) {
            if (enlace != null && enlace.getEtiqueta() != null
                    && enlace.getEtiqueta().getIdEtiqueta() != null) {
                ids.add(enlace.getEtiqueta().getIdEtiqueta());
            }
        }
        if (ids.isEmpty()) {
            return List.of();
        }
        List<String> nombres = new ArrayList<>();
        for (var enlace : candidato.getEtiquetas()) {
            if (enlace == null || enlace.getEtiqueta() == null) {
                continue;
            }
            var etiqueta = enlace.getEtiqueta();
            if (etiqueta.getIdEtiqueta() != null
                    && ids.contains(etiqueta.getIdEtiqueta())
                    && etiqueta.getNombre() != null
                    && !etiqueta.getNombre().isBlank()) {
                nombres.add(etiqueta.getNombre());
            }
        }
        nombres.sort(String.CASE_INSENSITIVE_ORDER);
        return nombres.stream().distinct().limit(3).toList();
    }

    private String compararPrecio(Servicio origen, Servicio candidato) {
        BigDecimal base = origen.getPrecioBase();
        BigDecimal otro = candidato.getPrecioBase();
        if (base == null || otro == null || base.signum() <= 0) {
            return null;
        }
        if (origen.getMoneda() == null || candidato.getMoneda() == null
                || !Objects.equals(origen.getMoneda().getIdMoneda(),
                    candidato.getMoneda().getIdMoneda())) {
            return null;
        }
        // Si la alternativa es más cara que el servicio original no mostramos
        // el detalle: desalienta la recomendación en lugar de explicarla.
        if (otro.compareTo(base) > 0) {
            return null;
        }
        BigDecimal diferencia = base.subtract(otro)
            .multiply(BigDecimal.valueOf(100))
            .divide(base, 0, RoundingMode.HALF_UP);
        if (diferencia.compareTo(BigDecimal.valueOf(5)) <= 0) {
            return "Precio similar";
        }
        return "Precio " + diferencia.toPlainString() + "% más bajo";
    }

    private boolean puedeQuedarEnCola(MotivoSustitucion motivo) {
        return motivo == MotivoSustitucion.SIN_CUPOS
            || motivo == MotivoSustitucion.SOLO_ESPERA;
    }
}
