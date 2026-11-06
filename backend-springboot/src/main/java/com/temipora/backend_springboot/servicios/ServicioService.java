package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.temipora.backend_springboot.modelo.EPreferenciaContenidoSensible;
import com.temipora.backend_springboot.modelo.EstadoServicio;
import com.temipora.backend_springboot.modelo.EstadoSolicitud;
import com.temipora.backend_springboot.modelo.Etiqueta;
import com.temipora.backend_springboot.modelo.EtiquetaEstado;
import com.temipora.backend_springboot.modelo.FiltroServicioRequest;
import com.temipora.backend_springboot.modelo.Medio;
import com.temipora.backend_springboot.modelo.ModoRecepcionSolicitud;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.OrdenServicio;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.PromedioReseniasDTO;
import com.temipora.backend_springboot.modelo.RecomendacionArtistaDTO;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.ServicioDTO;
import com.temipora.backend_springboot.modelo.ServicioEtiqueta;
import com.temipora.backend_springboot.modelo.ServicioImagen;
import com.temipora.backend_springboot.modelo.ServicioRequest;
import com.temipora.backend_springboot.modelo.Solicitud;
import com.temipora.backend_springboot.modelo.TipoServicio;
import com.temipora.backend_springboot.modelo.Usuario;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import com.temipora.backend_springboot.repositorios.EtiquetaRepository;
import com.temipora.backend_springboot.repositorios.MedioRepository;
import com.temipora.backend_springboot.repositorios.ServicioRepository;
import com.temipora.backend_springboot.repositorios.SolicitudRepository;
import com.temipora.backend_springboot.repositorios.TipoServicioRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@Service
@Validated
public class ServicioService {

    private static final List<EstadoSolicitud> ESTADOS_QUE_OCUPAN_CUPO = List.of(
        EstadoSolicitud.PRESUPUESTADA,
        EstadoSolicitud.PAGADA);

    private static final List<EstadoSolicitud> ESTADOS_ACTIVOS = List.of(
        EstadoSolicitud.PENDIENTE,
        EstadoSolicitud.EN_LISTA_ESPERA,
        EstadoSolicitud.PRESUPUESTADA,
        EstadoSolicitud.PAGADA);

    private final ServicioRepository servicioRepository;
    private final SolicitudRepository solicitudRepository;
    private final UsuarioRepository usuarioRepository;
    private final TipoServicioRepository tipoServicioRepository;
    private final MedioRepository medioRepository;
    private final EtiquetaRepository etiquetaRepository;
    private final FormularioService formularioService;
    private final TerminosService terminosService;
    private final ConversionMonedaService conversionMonedaService;
    private final MonedaService monedaService;
    private final ReseniaPublicaService reseniaPublicaService;
    private final RecomendacionService recomendacionService;
    private final OfertaReactivacionService ofertaReactivacionService;
    private final AuditoriaService auditoria;

    private static final String ENTIDAD = "Servicio";
    private static final List<CampoAuditado> CAMPOS = List.of(
        CampoAuditado.de("nombre", "Nombre"),
        CampoAuditado.de("descripcion", "Descripción"),
        CampoAuditado.de("precioBase", "Precio base"),
        CampoAuditado.de("moneda", "Moneda"),
        CampoAuditado.de("tipoServicio", "Tipo de servicio"),
        CampoAuditado.de("medio", "Medio"),
        CampoAuditado.de("cupos", "Cupos"),
        CampoAuditado.de("tiempoEntregaValor", "Tiempo de entrega"),
        CampoAuditado.de("tiempoEntregaUnidad", "Unidad de entrega"),
        CampoAuditado.de("modoRecepcion", "Modo de recepción"),
        CampoAuditado.de("maxDibujosPorSolicitud", "Máximo de dibujos por solicitud"),
        CampoAuditado.de("permiteListaEspera", "Lista de espera"),
        CampoAuditado.de("habilitarCuposTrasEntrega", "Cupos tras la entrega"),
        CampoAuditado.de("contenidoSensible", "Contenido sensible"),
        CampoAuditado.de("estado", "Estado"));

    @PersistenceContext
    private EntityManager entityManager;

    public ServicioService(
            ServicioRepository servicioRepository,
            SolicitudRepository solicitudRepository,
            UsuarioRepository usuarioRepository,
            TipoServicioRepository tipoServicioRepository,
            MedioRepository medioRepository,
            EtiquetaRepository etiquetaRepository,
            FormularioService formularioService,
            TerminosService terminosService,
            ConversionMonedaService conversionMonedaService,
            MonedaService monedaService,
            ReseniaPublicaService reseniaPublicaService,
            RecomendacionService recomendacionService,
            OfertaReactivacionService ofertaReactivacionService,
            AuditoriaService auditoria) {
        this.servicioRepository = servicioRepository;
        this.solicitudRepository = solicitudRepository;
        this.usuarioRepository = usuarioRepository;
        this.tipoServicioRepository = tipoServicioRepository;
        this.medioRepository = medioRepository;
        this.etiquetaRepository = etiquetaRepository;
        this.formularioService = formularioService;
        this.terminosService = terminosService;
        this.conversionMonedaService = conversionMonedaService;
        this.monedaService = monedaService;
        this.reseniaPublicaService = reseniaPublicaService;
        this.recomendacionService = recomendacionService;
        this.ofertaReactivacionService = ofertaReactivacionService;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<ServicioDTO> listarMios(Long idArtista) {
        Moneda moneda = monedaPreferidaDe(idArtista);
        Map<Long, Long> activas = new HashMap<>();
        for (Object[] fila : solicitudRepository.contarActivasPorServicioDelArtista(
                idArtista, ESTADOS_ACTIVOS)) {
            activas.put((Long) fila[0], (Long) fila[1]);
        }
        List<Servicio> servicios = servicioRepository
            .findByArtistaIdUsuarioAndEstadoNotOrderByNombreAsc(
                idArtista, EstadoServicio.ARCHIVADO);
        Map<Long, Integer> ocupados = cuposOcupados(
            servicios.stream().map(Servicio::getIdServicio).toList());
        List<ServicioDTO> dtos = servicios.stream()
            .map(s -> {
                ServicioDTO dto = aDTO(s, false, moneda,
                    ocupados.getOrDefault(s.getIdServicio(), 0));
                dto.setSolicitudesActivas(activas.getOrDefault(s.getIdServicio(), 0L));
                return dto;
            })
            .toList();
        aplicarPromedios(dtos);
        return dtos;
    }

    @Transactional(readOnly = true)
    public List<ServicioDTO> listarActivos(FiltroServicioRequest filtro, Long viewerId) {
        EPreferenciaContenidoSensible pref = preferenciaDe(viewerId);
        boolean ocultarSensibles = pref == EPreferenciaContenidoSensible.OCULTAR;
        Moneda moneda = monedaPreferidaDe(viewerId);

        Specification<Servicio> spec = conArtista(activos(ocultarSensibles));
        if (viewerId != null) {
            spec = spec.and((root, query, cb) ->
                cb.notEqual(root.get("artista").get("idUsuario"), viewerId));
        }
        spec = aplicarFiltros(spec, filtro);
        List<Servicio> servicios = servicioRepository.findAll(
                spec, Sort.by(Sort.Direction.ASC, "nombre"))
            .stream()
            .filter(this::aceptaSolicitudesYPodriaPresupuestar)
            .toList();
        List<ServicioDTO> dtos = servicios.stream()
            .map(s -> aDTO(s, requiereAdvertencia(s, pref, viewerId), moneda))
            .toList();
        aplicarPromedios(dtos);
        aplicarOfertas(dtos, viewerId);
        return dtos;
    }

    @Transactional(readOnly = true)
    public PaginaDTO<ServicioDTO> listarActivosPaginado(
            FiltroServicioRequest filtro,
            Long viewerId,
            int pagina,
            int tamanio,
            OrdenServicio orden) {
        int paginaSegura = Math.max(0, pagina);
        int tamanioSeguro = Math.min(48, Math.max(1, tamanio));
        OrdenServicio ordenSeguro = orden != null ? orden : OrdenServicio.RELEVANCIA;

        EPreferenciaContenidoSensible pref = preferenciaDe(viewerId);
        boolean ocultarSensibles = pref == EPreferenciaContenidoSensible.OCULTAR;
        Moneda moneda = monedaPreferidaDe(viewerId);

        Specification<Servicio> base = activos(ocultarSensibles);
        if (viewerId != null) {
            base = base.and((root, query, cb) ->
                cb.notEqual(root.get("artista").get("idUsuario"), viewerId));
        }
        base = aplicarFiltros(base, filtro);

        List<Long> idsDisponibles = idsSegunSpec(base.and(disponibleAhora()));
        boolean incluirSinCupos = filtro == null
            || filtro.getIncluirSinCupos() == null
            || filtro.getIncluirSinCupos();
        List<Long> idsSinCupos = incluirSinCupos
            ? idsSegunSpec(base.and(noDisponibleAhora()))
            : List.of();

        int totalDisponibles = idsDisponibles.size();
        int total = totalDisponibles + idsSinCupos.size();
        if (total == 0) {
            return paginaVacia(paginaSegura, tamanioSeguro);
        }

        int desde = Math.min(paginaSegura * tamanioSeguro, total);
        int hasta = Math.min(desde + tamanioSeguro, total);

        boolean personalizado = false;
        List<Servicio> paginaServicios = new ArrayList<>();

        if (desde < totalDisponibles) {
            Ordenados disponibles = ordenar(
                idsDisponibles, ordenSeguro, viewerId, moneda);
            personalizado = disponibles.personalizado();
            int fin = Math.min(hasta, totalDisponibles);
            paginaServicios.addAll(disponibles.items().subList(desde, fin));
        }
        if (hasta > totalDisponibles) {
            Ordenados sinCupos = ordenar(
                idsSinCupos, ordenSeguro, viewerId, moneda);
            int inicioSin = Math.max(0, desde - totalDisponibles);
            int finSin = Math.min(hasta - totalDisponibles, sinCupos.items().size());
            if (inicioSin < finSin) {
                paginaServicios.addAll(sinCupos.items().subList(inicioSin, finSin));
            }
        }

        Map<Long, Integer> ocupados = cuposOcupados(
            paginaServicios.stream().map(Servicio::getIdServicio).toList());
        List<ServicioDTO> dtos = paginaServicios.stream()
            .map(s -> aDTO(s, requiereAdvertencia(s, pref, viewerId), moneda,
                ocupados.getOrDefault(s.getIdServicio(), 0)))
            .toList();
        aplicarPromedios(dtos);
        aplicarOfertas(dtos, viewerId);

        int totalPaginas = (int) Math.ceil(total / (double) tamanioSeguro);
        return PaginaDTO.<ServicioDTO>builder()
            .contenido(dtos)
            .pagina(paginaSegura)
            .tamanio(tamanioSeguro)
            .totalElementos(total)
            .totalPaginas(totalPaginas)
            .hayMas(hasta < total)
            .personalizado(personalizado)
            .build();
    }

    private record Ordenados(List<Servicio> items, boolean personalizado) {
    }

    private Ordenados ordenar(
            List<Long> ids,
            OrdenServicio orden,
            Long viewerId,
            Moneda moneda) {
        if (ids == null || ids.isEmpty()) {
            return new Ordenados(List.of(), false);
        }

        List<Servicio> candidatos = servicioRepository.findByIdServicioIn(ids);
        Map<Long, Servicio> porId = new HashMap<>();
        for (Servicio servicio : candidatos) {
            porId.put(servicio.getIdServicio(), servicio);
        }
        List<Servicio> ordenados = new ArrayList<>();
        for (Long id : ids) {
            Servicio servicio = porId.get(id);
            if (servicio != null) {
                ordenados.add(servicio);
            }
        }
        ordenados.sort(Comparator.comparing(
            Servicio::getNombre, String.CASE_INSENSITIVE_ORDER));

        boolean personalizado = false;
        if (orden == OrdenServicio.RELEVANCIA) {
            RecomendacionService.ResultadoRanking ranking =
                recomendacionService.rankear(ordenados, viewerId, moneda);
            ordenados = new ArrayList<>(ranking.items().stream()
                .map(RecomendacionService.ServicioPuntuado::servicio)
                .toList());
            personalizado = ranking.personalizado();
        } else if (orden == OrdenServicio.PRECIO_ASC
                || orden == OrdenServicio.PRECIO_DESC) {
            Map<Long, BigDecimal> precios = new HashMap<>();
            for (Servicio servicio : ordenados) {
                precios.put(
                    servicio.getIdServicio(),
                    precioConvertido(servicio, moneda));
            }
            Comparator<Servicio> comparator = Comparator.comparing(
                s -> precios.getOrDefault(s.getIdServicio(), BigDecimal.ZERO));
            if (orden == OrdenServicio.PRECIO_DESC) {
                comparator = comparator.reversed();
            }
            ordenados.sort(comparator.thenComparing(
                Servicio::getNombre, String.CASE_INSENSITIVE_ORDER));
        }
        return new Ordenados(ordenados, personalizado);
    }

    @Transactional(readOnly = true)
    public List<ServicioDTO> listarActivosDeArtista(String nombreUsuario, Long viewerId) {
        EPreferenciaContenidoSensible pref = preferenciaDe(viewerId);
        boolean ocultarSensibles = pref == EPreferenciaContenidoSensible.OCULTAR;
        Moneda moneda = monedaPreferidaDe(viewerId);

        Specification<Servicio> spec = conArtista(activos(ocultarSensibles))
            .and((root, query, cb) ->
                cb.equal(cb.lower(root.get("artista").get("nombreUsuario")),
                    nombreUsuario.trim().toLowerCase()));
        List<Servicio> servicios = servicioRepository.findAll(
            spec, Sort.by(Sort.Direction.ASC, "nombre"));
        Map<Long, Integer> ocupados = cuposOcupados(
            servicios.stream().map(Servicio::getIdServicio).toList());
        List<ServicioDTO> dtos = servicios.stream()
            .map(s -> aDTO(s, requiereAdvertencia(s, pref, viewerId), moneda,
                ocupados.getOrDefault(s.getIdServicio(), 0)))
            .toList();
        aplicarPromedios(dtos);
        aplicarOfertas(dtos, viewerId);
        return dtos;
    }

    /**
     * Servicios publicados que hoy pueden recibir una solicitud. Se usa una
     * sola vez por corrida del proceso de reactivación para no repetir la
     * consulta por cada usuario.
     */
    @Transactional(readOnly = true)
    public List<Servicio> candidatosDisponibles() {
        Specification<Servicio> spec = activos(false).and(disponibleAhora());
        List<Long> ids = idsSegunSpec(spec);
        if (ids.isEmpty()) {
            return List.of();
        }
        return servicioRepository.findByIdServicioIn(ids);
    }

    @Transactional(readOnly = true)
    public List<RecomendacionArtistaDTO> artistasSimilares(
            Long idServicio,
            Long viewerId) {
        Servicio origen = obtenerEntidad(idServicio);
        if (origen.getEstado() != EstadoServicio.PUBLICADO) {
            return List.of();
        }
        if (origen.getArtista() == null) {
            return List.of();
        }

        EPreferenciaContenidoSensible pref = preferenciaDe(viewerId);
        boolean ocultarSensibles = pref == EPreferenciaContenidoSensible.OCULTAR;
        Moneda moneda = monedaPreferidaDe(viewerId);
        Long idArtistaOrigen = origen.getArtista().getIdUsuario();

        Specification<Servicio> spec = activos(ocultarSensibles)
            .and(disponibleAhora())
            .and((root, query, cb) ->
                cb.notEqual(root.get("artista").get("idUsuario"), idArtistaOrigen));
        if (viewerId != null) {
            spec = spec.and((root, query, cb) ->
                cb.notEqual(root.get("artista").get("idUsuario"), viewerId));
        }

        List<Long> ids = idsSegunSpec(spec);
        if (ids.isEmpty()) {
            return List.of();
        }
        List<Servicio> candidatos = servicioRepository.findByIdServicioIn(ids);
        List<RecomendacionService.ServicioPuntuado> ranked =
            recomendacionService.similares(origen, candidatos, viewerId, moneda);
        if (ranked.isEmpty()) {
            return List.of();
        }

        Map<Long, PromedioReseniasDTO> promedios =
            reseniaPublicaService.promediosPorServicios(
                ranked.stream()
                    .map(item -> item.servicio().getIdServicio())
                    .toList());

        Map<Long, List<RecomendacionService.ServicioPuntuado>> porArtista =
            new LinkedHashMap<>();
        for (RecomendacionService.ServicioPuntuado item : ranked) {
            Long idArtista = item.servicio().getArtista().getIdUsuario();
            porArtista.computeIfAbsent(idArtista, clave -> new ArrayList<>())
                .add(item);
        }

        List<RecomendacionArtistaDTO> resultado = new ArrayList<>();
        for (Map.Entry<Long, List<RecomendacionService.ServicioPuntuado>> entrada
                : porArtista.entrySet()) {
            List<RecomendacionService.ServicioPuntuado> items = entrada.getValue();
            RecomendacionService.ServicioPuntuado mejor = items.get(0);
            Servicio servicio = mejor.servicio();
            Usuario artista = servicio.getArtista();

            BigDecimal precioDesde = items.stream()
                .map(item -> precioConvertido(item.servicio(), moneda))
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(null);

            long cantidadResenias = 0;
            double sumaPromedios = 0;
            for (RecomendacionService.ServicioPuntuado item : items) {
                PromedioReseniasDTO promedio = promedios.get(
                    item.servicio().getIdServicio());
                if (promedio != null && promedio.getCantidad() > 0
                        && promedio.getPromedio() != null) {
                    cantidadResenias += promedio.getCantidad();
                    sumaPromedios += promedio.getPromedio().doubleValue()
                        * promedio.getCantidad();
                }
            }
            BigDecimal calificacion = cantidadResenias > 0
                ? BigDecimal.valueOf(sumaPromedios / cantidadResenias)
                    .setScale(2, RoundingMode.HALF_UP)
                : null;

            Map<String, Integer> frecuenciaEtiquetas = new HashMap<>();
            for (RecomendacionService.ServicioPuntuado item : items) {
                if (item.servicio().getEtiquetas() == null) {
                    continue;
                }
                for (ServicioEtiqueta asignacion : item.servicio().getEtiquetas()) {
                    if (asignacion.getEtiqueta() != null
                            && asignacion.getEtiqueta().getNombre() != null) {
                        frecuenciaEtiquetas.merge(
                            asignacion.getEtiqueta().getNombre(), 1, Integer::sum);
                    }
                }
            }
            List<String> etiquetas = frecuenciaEtiquetas.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();

            String imagen = servicio.getImagenPortada();
            if ((imagen == null || imagen.isBlank())
                    && servicio.getImagenes() != null
                    && !servicio.getImagenes().isEmpty()) {
                imagen = servicio.getImagenes().get(0).getUrl();
            }

            resultado.add(RecomendacionArtistaDTO.builder()
                .idArtista(entrada.getKey())
                .nombreUsuario(artista.getNombreUsuario())
                .nombreDisplay(artista.getNombreDisplay())
                .urlFotoPerfil(artista.getUrlFotoPerfil())
                .imagenRepresentativa(imagen)
                .nombreServicio(servicio.getNombre())
                .idServicio(servicio.getIdServicio())
                .precioDesde(precioDesde)
                .moneda(moneda != null ? moneda.getCodigo() : null)
                .calificacionPromedio(calificacion)
                .cantidadResenias(cantidadResenias)
                .etiquetasDestacadas(etiquetas)
                .motivo(mejor.motivo())
                .score(mejor.score())
                .build());
        }
        return resultado.stream().limit(6).toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, ServicioDTO> convertirParaVisor(Collection<Servicio> servicios, Long viewerId) {
        if (servicios == null || servicios.isEmpty()) {
            return Map.of();
        }
        EPreferenciaContenidoSensible pref = preferenciaDe(viewerId);
        Moneda moneda = monedaPreferidaDe(viewerId);
        Map<Long, ServicioDTO> resultado = new HashMap<>();
        for (Servicio servicio : servicios) {
            resultado.put(
                servicio.getIdServicio(),
                aDTO(servicio, requiereAdvertencia(servicio, pref, viewerId), moneda));
        }
        aplicarPromedios(resultado.values());
        aplicarOfertas(resultado.values(), viewerId);
        return resultado;
    }

    private void aplicarOfertas(Collection<ServicioDTO> dtos, Long viewerId) {
        if (viewerId == null || dtos == null || dtos.isEmpty()) {
            return;
        }
        List<Long> ids = dtos.stream()
            .map(ServicioDTO::getIdServicio)
            .filter(Objects::nonNull)
            .toList();
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, OfertaReactivacionService.OfertaVigente> ofertas =
            ofertaReactivacionService.ofertasVigentesPorServicios(viewerId, ids);
        if (ofertas == null || ofertas.isEmpty()) {
            return;
        }
        for (ServicioDTO dto : dtos) {
            OfertaReactivacionService.OfertaVigente oferta =
                ofertas.get(dto.getIdServicio());
            if (oferta != null) {
                dto.setIdOferta(oferta.idOferta());
                dto.setDescuentoOfertaPorcentaje(oferta.porcentajeDescuento());
                BigDecimal base = dto.getPrecioBaseMostrado() != null
                    ? dto.getPrecioBaseMostrado()
                    : dto.getPrecioBase();
                if (base != null) {
                    BigDecimal descuento = base
                        .multiply(oferta.porcentajeDescuento())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    dto.setPrecioConDescuentoEstimado(
                        base.subtract(descuento).max(BigDecimal.ZERO)
                            .setScale(2, RoundingMode.HALF_UP));
                }
            }
        }
    }

    private void aplicarPromedios(Collection<ServicioDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }
        List<Long> ids = dtos.stream()
            .map(ServicioDTO::getIdServicio)
            .filter(java.util.Objects::nonNull)
            .toList();
        Map<Long, PromedioReseniasDTO> promediosCrudos =
            reseniaPublicaService.promediosPorServicios(ids);
        final Map<Long, PromedioReseniasDTO> promedios =
            promediosCrudos != null ? promediosCrudos : Map.of();

        for (ServicioDTO dto : dtos) {
            PromedioReseniasDTO promedio = promedios.get(dto.getIdServicio());
            if (promedio != null && promedio.getCantidad() > 0) {
                dto.setCalificacionPromedio(promedio.getPromedio());
                dto.setCantidadResenias(promedio.getCantidad());
            } else {
                dto.setCantidadResenias(0L);
            }
        }
    }

    private Specification<Servicio> activos(boolean ocultarSensibles) {
        return (root, query, cb) -> {
            if (ocultarSensibles) {
                return cb.and(
                    cb.equal(root.get("estado"), EstadoServicio.PUBLICADO),
                    cb.isFalse(root.get("contenidoSensible")));
            }
            return cb.equal(root.get("estado"), EstadoServicio.PUBLICADO);
        };
    }

    private Specification<Servicio> conArtista(Specification<Servicio> spec) {
        return (root, query, cb) -> {
            root.fetch("artista", JoinType.LEFT);
            return spec.toPredicate(root, query, cb);
        };
    }

    /**
     * Sólo servicios que pueden recibir una solicitud ahora: recepción abierta
     * y al menos un cupo libre. Se resuelve en SQL para poder paginar sin
     * traer todo el catálogo a memoria.
     */
    private Specification<Servicio> disponibleAhora() {
        return (root, query, cb) -> {
            Subquery<Long> ocupados = query.subquery(Long.class);
            Root<Solicitud> solicitud = ocupados.from(Solicitud.class);
            ocupados.select(cb.count(solicitud));
            ocupados.where(
                cb.equal(solicitud.get("servicio").get("idServicio"),
                    root.get("idServicio")),
                solicitud.get("estado").in(ESTADOS_QUE_OCUPAN_CUPO));

            Path<Integer> cupos = root.get("cupos");
            return cb.and(
                cb.equal(root.get("modoRecepcion"), ModoRecepcionSolicitud.ABIERTA),
                cb.greaterThan(cupos.as(Long.class), ocupados));
        };
    }

    private Specification<Servicio> noDisponibleAhora() {
        return (root, query, cb) -> {
            Subquery<Long> ocupados = query.subquery(Long.class);
            Root<Solicitud> solicitud = ocupados.from(Solicitud.class);
            ocupados.select(cb.count(solicitud));
            ocupados.where(
                cb.equal(solicitud.get("servicio").get("idServicio"),
                    root.get("idServicio")),
                solicitud.get("estado").in(ESTADOS_QUE_OCUPAN_CUPO));

            Path<Integer> cupos = root.get("cupos");
            Predicate hayLugar = cb.and(
                cb.equal(root.get("modoRecepcion"), ModoRecepcionSolicitud.ABIERTA),
                cb.greaterThan(cupos.as(Long.class), ocupados));
            return cb.not(hayLugar);
        };
    }

    private List<Long> idsSegunSpec(Specification<Servicio> spec) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Servicio> root = query.from(Servicio.class);
        Predicate predicate = spec != null
            ? spec.toPredicate(root, query, cb)
            : null;
        query.select(root.get("idServicio"));
        if (predicate != null) {
            query.where(predicate);
        }
        return entityManager.createQuery(query).getResultList();
    }

    private Map<Long, Integer> cuposOcupados(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> resultado = new HashMap<>();
        for (Object[] fila : solicitudRepository.contarOcupadosPorServicios(
                ids, ESTADOS_QUE_OCUPAN_CUPO)) {
            Long id = ((Number) fila[0]).longValue();
            long ocupados = ((Number) fila[1]).longValue();
            resultado.put(id, (int) Math.min(ocupados, Integer.MAX_VALUE));
        }
        return resultado;
    }

    private PaginaDTO<ServicioDTO> paginaVacia(int pagina, int tamanio) {
        return PaginaDTO.<ServicioDTO>builder()
            .contenido(List.of())
            .pagina(pagina)
            .tamanio(tamanio)
            .totalElementos(0)
            .totalPaginas(0)
            .hayMas(false)
            .personalizado(false)
            .build();
    }

    private BigDecimal precioConvertido(Servicio servicio, Moneda monedaDestino) {
        if (servicio == null || servicio.getPrecioBase() == null) {
            return null;
        }
        if (monedaDestino == null) {
            return servicio.getPrecioBase();
        }
        BigDecimal factor = conversionMonedaService.factor(
            servicio.getMoneda(), monedaDestino, LocalDateTime.now());
        if (factor == null) {
            return servicio.getPrecioBase();
        }
        return servicio.getPrecioBase().multiply(factor)
            .setScale(2, RoundingMode.HALF_UP);
    }

    private EPreferenciaContenidoSensible preferenciaDe(Long viewerId) {
        if (viewerId == null) {
            return EPreferenciaContenidoSensible.ADVERTIR;
        }
        return usuarioRepository.findByIdUsuario(viewerId)
            .map(Usuario::getPreferenciaSensible)
            .orElse(EPreferenciaContenidoSensible.ADVERTIR);
    }

    private Moneda monedaPreferidaDe(Long viewerId) {
        if (viewerId != null) {
            Moneda preferida = usuarioRepository.findByIdUsuario(viewerId)
                .map(Usuario::getMonedaPreferida)
                .orElse(null);
            if (preferida != null) {
                return preferida;
            }
        }
        return monedaService.predeterminada();
    }

    private ServicioDTO aDTO(Servicio s, boolean requiereAdvertencia, Moneda monedaDestino) {
        return aDTO(s, requiereAdvertencia, monedaDestino,
            calcularCuposOcupados(s.getIdServicio()));
    }

    private ServicioDTO aDTO(
            Servicio s,
            boolean requiereAdvertencia,
            Moneda monedaDestino,
            int ocupados) {
        BigDecimal factor = monedaDestino != null
            ? conversionMonedaService.factor(s.getMoneda(), monedaDestino, LocalDateTime.now())
            : null;
        ServicioDTO dto = ServicioDTO.desdeServicio(s, requiereAdvertencia, monedaDestino, factor);
        dto.setCuposOcupados(ocupados);
        int disponibles = Math.max(0,
            (s.getCupos() == null ? 0 : s.getCupos()) - ocupados);
        dto.setCuposDisponibles(disponibles);
        dto.setDisponibleAhora(esDisponibleAhora(s, disponibles));
        return dto;
    }

    private boolean esDisponibleAhora(Servicio s, int disponibles) {
        return s.getEstado() == EstadoServicio.PUBLICADO
            && s.getModoRecepcion() == ModoRecepcionSolicitud.ABIERTA
            && disponibles > 0;
    }

    private boolean aceptaSolicitudesYPodriaPresupuestar(Servicio servicio) {
        if (servicio.getModoRecepcion() != ModoRecepcionSolicitud.ABIERTA) {
            return false;
        }
        int cupos = servicio.getCupos() == null ? 0 : servicio.getCupos();
        return cupos - calcularCuposOcupados(servicio.getIdServicio()) > 0;
    }

    private int calcularCuposOcupados(Long idServicio) {
        long ocupados = solicitudRepository.countByServicioIdServicioAndEstadoIn(
            idServicio, ESTADOS_QUE_OCUPAN_CUPO);
        return (int) Math.min(ocupados, Integer.MAX_VALUE);
    }

    private boolean requiereAdvertencia(
            Servicio s,
            EPreferenciaContenidoSensible pref,
            Long viewerId) {
        boolean esDuenio = viewerId != null
            && s.getArtista() != null
            && s.getArtista().getIdUsuario().equals(viewerId);
        return s.isContenidoSensible()
            && pref == EPreferenciaContenidoSensible.ADVERTIR
            && !esDuenio;
    }

    private Specification<Servicio> aplicarFiltros(Specification<Servicio> spec, FiltroServicioRequest f) {
        if (f == null) {
            return spec;
        }
        if (hayTexto(f.getNombre())) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("nombre")), "%" + f.getNombre().trim().toLowerCase() + "%"));
        }
        if (hayTexto(f.getNombreArtista())) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("artista").get("nombreUsuario")),
                    "%" + f.getNombreArtista().trim().toLowerCase() + "%"));
        }
        if (f.getIdTipoServicio() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("tipoServicio").get("idTipoServicio"), f.getIdTipoServicio()));
        }
        if (f.getSensible() != null) {
            boolean sensible = f.getSensible();
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("contenidoSensible"), sensible));
        }
        if (f.getPrecioMin() != null) {
            BigDecimal min = f.getPrecioMin();
            spec = spec.and((root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("precioBase"), min));
        }
        if (f.getPrecioMax() != null) {
            BigDecimal max = f.getPrecioMax();
            spec = spec.and((root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("precioBase"), max));
        }
        if (f.getTiempoMaxDias() != null) {
            Integer dias = f.getTiempoMaxDias();
            spec = spec.and((root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("tiempoEntregaDias"), dias));
        }
        return spec;
    }

    private boolean hayTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    @Transactional(readOnly = true)
    public ServicioDTO obtener(Long idServicio, Long viewerId) {
        Servicio servicio = obtenerEntidad(idServicio);
        EPreferenciaContenidoSensible pref = preferenciaDe(viewerId);

        boolean esDuenio = viewerId != null
            && servicio.getArtista() != null
            && servicio.getArtista().getIdUsuario().equals(viewerId);
        if (servicio.getEstado() == EstadoServicio.ARCHIVADO
                || (!esDuenio && servicio.getEstado() != EstadoServicio.PUBLICADO)) {
            throw new NoSuchElementException("Servicio no encontrado: " + idServicio);
        }
        if (!esDuenio && pref == EPreferenciaContenidoSensible.OCULTAR && servicio.isContenidoSensible()) {
            throw new NoSuchElementException("Servicio no encontrado: " + idServicio);
        }
        ServicioDTO dto = aDTO(servicio, requiereAdvertencia(servicio, pref, viewerId), monedaPreferidaDe(viewerId));
        aplicarPromedios(List.of(dto));
        aplicarOfertas(List.of(dto), viewerId);
        return dto;
    }

    @Transactional
    public ServicioDTO crear(Long idArtista, ServicioRequest request) {
        validarUnico(idArtista, request.getIdTipoServicio(), request.getNombre(), null);

        Usuario artista = usuarioRepository.findByIdUsuario(idArtista)
            .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado: " + idArtista));

        Servicio servicio = new Servicio();
        servicio.setArtista(artista);
        servicio.setEstado(EstadoServicio.PUBLICADO);
        aplicar(servicio, request);
        validarTerminos(idArtista, null, request);

        Servicio guardado = servicioRepository.save(servicio);
        guardarConfiguracion(guardado.getIdServicio(), idArtista, request);
        auditoria.registrar(
            AccionesAdmin.SERVICIO_CREAR, ENTIDAD, guardado.getIdServicio(),
            "Creó el servicio “" + guardado.getNombre() + "”", null, guardado, CAMPOS);
        return aDTO(guardado, false, monedaPreferidaDe(idArtista));
    }

    @Transactional
    public ServicioDTO modificar(Long idServicio, Long idArtista, ServicioRequest request) {
        Servicio servicio = obtenerEntidad(idServicio);
        verificarDuenio(servicio, idArtista);
        Snapshot antes = Snapshot.de(servicio, CAMPOS);
        validarUnico(idArtista, request.getIdTipoServicio(), request.getNombre(), idServicio);
        int cuposOcupados = calcularCuposOcupados(idServicio);
        if (request.getCupos() != null && request.getCupos() < cuposOcupados) {
            throw new IllegalArgumentException(
                "No podés reducir los cupos por debajo de los " + cuposOcupados
                    + " ocupados o reservados.");
        }
        aplicar(servicio, request);
        validarTerminos(idArtista, idServicio, request);
        Servicio guardado = servicioRepository.save(servicio);
        guardarConfiguracion(idServicio, idArtista, request);
        auditoria.registrar(
            AccionesAdmin.SERVICIO_MODIFICAR, ENTIDAD, guardado.getIdServicio(),
            "Modificó el servicio “" + guardado.getNombre() + "”", antes, guardado, CAMPOS);
        return aDTO(guardado, false, monedaPreferidaDe(idArtista), cuposOcupados);
    }

    @Transactional
    public ServicioDTO pausar(Long idServicio, Long idArtista) {
        Servicio servicio = bloquearPropio(idServicio, idArtista);
        if (servicio.getEstado() == EstadoServicio.ARCHIVADO) {
            throw new ConflictoException("El servicio está archivado y no se puede pausar.");
        }
        if (servicio.getEstado() != EstadoServicio.PAUSADO) {
            Snapshot antes = Snapshot.de(servicio, CAMPOS);
            servicio.setEstado(EstadoServicio.PAUSADO);
            servicio.setPausadoEn(LocalDateTime.now());
            servicioRepository.save(servicio);
            auditoria.registrar(
                AccionesAdmin.SERVICIO_PAUSAR, ENTIDAD, servicio.getIdServicio(),
                "Pausó el servicio “" + servicio.getNombre() + "”", antes, servicio, CAMPOS);
        }
        return ServicioDTO.desdeServicio(servicio);
    }

    @Transactional
    public ServicioDTO reactivar(Long idServicio, Long idArtista) {
        Servicio servicio = bloquearPropio(idServicio, idArtista);
        if (servicio.getEstado() == EstadoServicio.ARCHIVADO) {
            throw new ConflictoException(
                "El servicio está archivado: solo un administrador puede restaurarlo.");
        }
        if (servicio.getEstado() == EstadoServicio.PUBLICADO) {
            return aDTO(servicio, false, monedaPreferidaDe(idArtista));
        }
        if (!terminosService.tieneTerminosValidos(idServicio, idArtista)) {
            throw new IllegalArgumentException(
                "Completá tus términos de uso y servicio antes de publicar el servicio.");
        }
        Snapshot antes = Snapshot.de(servicio, CAMPOS);
        servicio.setEstado(EstadoServicio.PUBLICADO);
        servicio.setPausadoEn(null);
        servicioRepository.save(servicio);
        auditoria.registrar(
            AccionesAdmin.SERVICIO_REACTIVAR, ENTIDAD, servicio.getIdServicio(),
            "Reactivó el servicio “" + servicio.getNombre() + "”", antes, servicio, CAMPOS);
        return aDTO(servicio, false, monedaPreferidaDe(idArtista));
    }

    private Servicio bloquearPropio(Long idServicio, Long idArtista) {
        Servicio servicio = servicioRepository.findByIdServicioForUpdate(idServicio)
            .orElseThrow(() -> new NoSuchElementException("Servicio no encontrado: " + idServicio));
        verificarDuenio(servicio, idArtista);
        return servicio;
    }

    private void validarTerminos(Long idArtista, Long idServicio, ServicioRequest request) {
        if (request.getTerminos() != null) {
            return;
        }
        boolean valido = idServicio == null
            ? terminosService.tieneGlobalValido(idArtista)
            : terminosService.tieneTerminosValidos(idServicio, idArtista);
        if (!valido) {
            throw new IllegalArgumentException(
                "Completá tus términos de uso y servicio antes de publicar el servicio.");
        }
    }

    private void aplicar(Servicio servicio, ServicioRequest r) {
        validarImagenes(r);
        if (r.getPrecioBase() == null || r.getPrecioBase().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio base debe ser mayor a cero.");
        }
        if (r.getCupos() == null || r.getCupos() <= 0) {
            throw new IllegalArgumentException("La cantidad de cupos debe ser mayor a cero.");
        }
        if (r.getCupos() > 10) {
            throw new IllegalArgumentException("La cantidad de cupos no puede superar 10.");
        }
        ModoRecepcionSolicitud modo = r.getModoRecepcion() != null
            ? r.getModoRecepcion()
            : ModoRecepcionSolicitud.ABIERTA;
        int maxDibujos = r.getMaxDibujosPorSolicitud() != null
            ? r.getMaxDibujosPorSolicitud()
            : 3;
        if (maxDibujos < 1 || maxDibujos > 50) {
            throw new IllegalArgumentException(
                "La cantidad máxima de dibujos debe estar entre 1 y 50.");
        }

        TipoServicio tipo = tipoServicioRepository.findByIdTipoServicio(r.getIdTipoServicio())
            .orElseThrow(() -> new IllegalArgumentException("Tipo de servicio no encontrado: " + r.getIdTipoServicio()));
        Medio medio = medioRepository.findByIdMedio(r.getIdMedio())
            .orElseThrow(() -> new IllegalArgumentException("Medio no encontrado: " + r.getIdMedio()));

        servicio.setTipoServicio(tipo);
        servicio.setMedio(medio);
        servicio.setNombre(r.getNombre().trim());
        servicio.setDescripcion(r.getDescripcion().trim());
        servicio.setPrecioBase(r.getPrecioBase());
        servicio.setMoneda(resolverMoneda(r.getIdMoneda()));
        validarContenidoSensible(r);
        servicio.setContenidoSensible(r.isContenidoSensible());
        servicio.reemplazarContenidosSensibles(
            r.isContenidoSensible() && r.getContenidosSensibles() != null
                ? new HashSet<>(r.getContenidosSensibles())
                : Set.of());
        servicio.setTiempoEntregaValor(r.getTiempoEntregaValor());
        servicio.setTiempoEntregaUnidad(r.getTiempoEntregaUnidad());
        servicio.setTiempoEntregaDias(r.getTiempoEntregaValor() * r.getTiempoEntregaUnidad().getFactorDias());
        servicio.setCupos(r.getCupos());
        servicio.setModoRecepcion(modo);
        servicio.setMaxDibujosPorSolicitud(maxDibujos);
        servicio.setPermiteListaEspera(modo == ModoRecepcionSolicitud.SOLO_ESPERA);
        servicio.setHabilitarCuposTrasEntrega(false);
        servicio.setImagenPortada(limpiar(r.getImagenPortada()));

        List<ServicioImagen> imagenes = new ArrayList<>();
        if (r.getImagenes() != null) {
            int orden = 0;
            for (String url : r.getImagenes()) {
                if (url == null || url.isBlank()) {
                    continue;
                }
                ServicioImagen imagen = new ServicioImagen();
                imagen.setUrl(url.trim());
                imagen.setOrden(orden++);
                imagenes.add(imagen);
            }
        }
        servicio.reemplazarImagenes(imagenes);
        servicio.reemplazarEtiquetas(cargarEtiquetas(servicio.getArtista(), r.getIdsEtiquetas()));
    }

    private void guardarConfiguracion(Long idServicio, Long idArtista, ServicioRequest request) {
        if (request.getFormulario() != null) {
            formularioService.guardarServicio(idServicio, idArtista, request.getFormulario());
        }
        if (request.getTerminos() != null) {
            terminosService.guardarServicio(idServicio, idArtista, request.getTerminos());
        }
    }

    private void validarImagenes(ServicioRequest r) {
        if (limpiar(r.getImagenPortada()) == null) {
            throw new IllegalArgumentException("La imagen de portada es obligatoria.");
        }
        boolean hayEjemplo = r.getImagenes() != null
            && r.getImagenes().stream().anyMatch(url -> limpiar(url) != null);
        if (!hayEjemplo) {
            throw new IllegalArgumentException("Debés agregar al menos una imagen de ejemplo.");
        }
    }

    private Set<Etiqueta> cargarEtiquetas(Usuario artista, Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        if (ids.size() > 5) {
            throw new IllegalArgumentException("Un servicio puede tener como máximo 5 etiquetas.");
        }

        Set<Etiqueta> etiquetas = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id == null) {
                continue;
            }
            Etiqueta etiqueta = etiquetaRepository.findByIdEtiqueta(id)
                .orElseThrow(() -> new NoSuchElementException("Etiqueta no encontrada: " + id));
            validarEtiquetaAsignable(etiqueta, artista);
            etiquetas.add(etiqueta);
        }
        return etiquetas;
    }

    private void validarEtiquetaAsignable(Etiqueta etiqueta, Usuario artista) {
        boolean propiaPendiente = etiqueta.getEstado() == EtiquetaEstado.PENDIENTE
            && etiqueta.getCreadoPor() != null
            && artista != null
            && etiqueta.getCreadoPor().getIdUsuario().equals(artista.getIdUsuario());
        if (etiqueta.getEstado() == EtiquetaEstado.VALIDA || propiaPendiente) {
            return;
        }
        if (etiqueta.getEstado() == EtiquetaEstado.INACTIVA) {
            throw new IllegalArgumentException(
                "La etiqueta '" + etiqueta.getNombre() + "' fue dada de baja y no puede asignarse a un servicio.");
        }
        if (etiqueta.getEstado() == EtiquetaEstado.RECHAZADA) {
            throw new IllegalArgumentException(
                "La etiqueta '" + etiqueta.getNombre() + "' fue rechazada y no puede asignarse a un servicio.");
        }
        throw new IllegalArgumentException(
            "La etiqueta '" + etiqueta.getNombre() + "' está pendiente de evaluación y solo su creador puede asignarla.");
    }

    private void validarContenidoSensible(ServicioRequest r) {
        if (r.isContenidoSensible()
                && (r.getContenidosSensibles() == null || r.getContenidosSensibles().isEmpty())) {
            throw new IllegalArgumentException(
                "Seleccioná qué tipo de contenido sensible puede aparecer en el servicio.");
        }
    }

    private void validarUnico(Long idArtista, Long idTipoServicio, String nombre, Long idExcluido) {
        String limpio = nombre.trim();
        boolean existe = (idExcluido == null)
            ? servicioRepository
                .existsByArtistaIdUsuarioAndTipoServicioIdTipoServicioAndNombreIgnoreCaseAndEstadoNot(
                    idArtista, idTipoServicio, limpio, EstadoServicio.ARCHIVADO)
            : servicioRepository
                .existsByArtistaIdUsuarioAndTipoServicioIdTipoServicioAndNombreIgnoreCaseAndIdServicioNotAndEstadoNot(
                    idArtista, idTipoServicio, limpio, idExcluido, EstadoServicio.ARCHIVADO);

        if (existe) {
            throw new IllegalArgumentException("Ya tenés un servicio con ese nombre y tipo.");
        }
    }

    @Transactional(readOnly = true)
    public void verificarPropietario(Long idServicio, Long idArtista) {
        verificarDuenio(obtenerEntidad(idServicio), idArtista);
    }

    public void verificarDuenio(Servicio servicio, Long idArtista) {
        if (servicio.getArtista() == null || !servicio.getArtista().getIdUsuario().equals(idArtista)) {
            throw new IllegalArgumentException("No podés modificar un servicio que no es tuyo.");
        }
    }

    private Servicio obtenerEntidad(Long idServicio) {
        return servicioRepository.findByIdServicio(idServicio)
            .orElseThrow(() -> new NoSuchElementException("Servicio no encontrado: " + idServicio));
    }

    private String limpiar(String valor) {
        return valor != null && !valor.isBlank() ? valor.trim() : null;
    }

    private Moneda resolverMoneda(Long idMoneda) {
        if (idMoneda != null) {
            return monedaService.obtener(idMoneda);
        }
        Moneda predeterminada = monedaService.predeterminada();
        if (predeterminada == null) {
            throw new IllegalStateException(
                "No hay monedas configuradas en el sistema.");
        }
        return predeterminada;
    }
}
