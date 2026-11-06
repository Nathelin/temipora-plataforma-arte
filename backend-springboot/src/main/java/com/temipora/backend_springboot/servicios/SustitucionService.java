package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.ConfiguracionSustitucion;
import com.temipora.backend_springboot.modelo.CrearSustitucionRequest;
import com.temipora.backend_springboot.modelo.DisparadorSustitucion;
import com.temipora.backend_springboot.modelo.EPreferenciaContenidoSensible;
import com.temipora.backend_springboot.modelo.EstadoServicio;
import com.temipora.backend_springboot.modelo.EstadoSolicitud;
import com.temipora.backend_springboot.modelo.EstadoSustitucion;
import com.temipora.backend_springboot.modelo.ModoRecepcionSolicitud;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.MotivoSustitucion;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.ServicioDTO;
import com.temipora.backend_springboot.modelo.Sustitucion;
import com.temipora.backend_springboot.modelo.SustitucionDTO;
import com.temipora.backend_springboot.modelo.SustitucionOpcion;
import com.temipora.backend_springboot.modelo.SustitucionOpcionDTO;
import com.temipora.backend_springboot.modelo.SustitucionResumenDTO;
import com.temipora.backend_springboot.modelo.Usuario;
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
            idCliente, origen.getIdServicio(), config);
        if (reutilizable != null) {
            return aDTO(reutilizable, idCliente);
        }

        List<Servicio> candidatos = candidatos(idCliente, origen);
        List<RecomendacionService.ServicioPuntuado> ranked =
            recomendacionService.similares(
                origen,
                candidatos,
                idCliente,
                monedaPreferidaDe(idCliente),
                opcionesDeSimilitud(config));

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

        Sustitucion guardada = sustitucionRepository.save(sustitucion);
        return armarDto(guardada, ranked, idCliente, motivo,
            cuposDisponibles, cuposTotales);
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

        List<SustitucionResumenDTO.OrigenFrecuente> origenes = new ArrayList<>();
        for (Object[] fila : sustitucionRepository.origenesFrecuentes(
                PageRequest.of(0, 5))) {
            origenes.add(new SustitucionResumenDTO.OrigenFrecuente(
                ((Number) fila[0]).longValue(),
                (String) fila[1],
                ((Number) fila[2]).longValue()));
        }

        return SustitucionResumenDTO.builder()
            .totalEventos(total)
            .resueltasConAlternativa(alternativa)
            .resueltasConCola(cola)
            .descartadas(descartadas)
            .abiertas(abiertas)
            .tasaSustitucion(tasa)
            .origenesFrecuentes(origenes)
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
            ConfiguracionSustitucion config) {
        if (idCliente == null || config.getMinutosDedupe() <= 0) {
            return null;
        }
        LocalDateTime desde = LocalDateTime.now()
            .minusMinutes(config.getMinutosDedupe());
        List<Sustitucion> recientes = sustitucionRepository.recientes(
            idCliente,
            idServicioOrigen,
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
            RecomendacionService.PesosSimilitud.conBonusFavorito(bonus),
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
            resultado.add(SustitucionOpcionDTO.builder()
                .idServicio(dto.getIdServicio())
                .orden(opcion.getOrden())
                .score(opcion.getScore() != null
                    ? opcion.getScore().doubleValue()
                    : 0)
                .motivo(opcion.getMotivo())
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
            .opciones(resultado)
            .build();
    }

    private boolean puedeQuedarEnCola(MotivoSustitucion motivo) {
        return motivo == MotivoSustitucion.SIN_CUPOS
            || motivo == MotivoSustitucion.SOLO_ESPERA;
    }
}
