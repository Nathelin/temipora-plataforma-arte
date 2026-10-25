package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.ServicioDTO;
import com.temipora.backend_springboot.modelo.ServicioImagen;
import com.temipora.backend_springboot.modelo.ServicioRequest;
import com.temipora.backend_springboot.modelo.TipoServicio;
import com.temipora.backend_springboot.modelo.Usuario;
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
            MonedaService monedaService) {
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
    }

    @Transactional(readOnly = true)
    public List<ServicioDTO> listarMios(Long idArtista) {
        Moneda moneda = monedaPreferidaDe(idArtista);
        Map<Long, Long> activas = new HashMap<>();
        for (Object[] fila : solicitudRepository.contarActivasPorServicioDelArtista(
                idArtista, ESTADOS_ACTIVOS)) {
            activas.put((Long) fila[0], (Long) fila[1]);
        }
        return servicioRepository
            .findByArtistaIdUsuarioAndEstadoNotOrderByNombreAsc(idArtista, EstadoServicio.ARCHIVADO)
            .stream()
            .map(s -> {
                ServicioDTO dto = aDTO(s, false, moneda);
                dto.setSolicitudesActivas(activas.getOrDefault(s.getIdServicio(), 0L));
                return dto;
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ServicioDTO> listarActivos(FiltroServicioRequest filtro, Long viewerId) {
        EPreferenciaContenidoSensible pref = preferenciaDe(viewerId);
        boolean ocultarSensibles = pref == EPreferenciaContenidoSensible.OCULTAR;
        Moneda moneda = monedaPreferidaDe(viewerId);

        Specification<Servicio> spec = activos(ocultarSensibles);
        if (viewerId != null) {
            spec = spec.and((root, query, cb) ->
                cb.notEqual(root.get("artista").get("idUsuario"), viewerId));
        }
        spec = aplicarFiltros(spec, filtro);
        return servicioRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "nombre")).stream()
            .filter(this::aceptaSolicitudesYPodriaPresupuestar)
            .map(s -> aDTO(s, requiereAdvertencia(s, pref, viewerId), moneda))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ServicioDTO> listarActivosDeArtista(String nombreUsuario, Long viewerId) {
        EPreferenciaContenidoSensible pref = preferenciaDe(viewerId);
        boolean ocultarSensibles = pref == EPreferenciaContenidoSensible.OCULTAR;
        Moneda moneda = monedaPreferidaDe(viewerId);

        Specification<Servicio> spec = activos(ocultarSensibles)
            .and((root, query, cb) ->
                cb.equal(cb.lower(root.get("artista").get("nombreUsuario")),
                    nombreUsuario.trim().toLowerCase()));
        return servicioRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "nombre")).stream()
            .map(s -> aDTO(s, requiereAdvertencia(s, pref, viewerId), moneda))
            .toList();
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
        return resultado;
    }

    private Specification<Servicio> activos(boolean ocultarSensibles) {
        return (root, query, cb) -> {
            root.fetch("artista");
            if (ocultarSensibles) {
                return cb.and(
                    cb.equal(root.get("estado"), EstadoServicio.PUBLICADO),
                    cb.isFalse(root.get("contenidoSensible")));
            }
            return cb.equal(root.get("estado"), EstadoServicio.PUBLICADO);
        };
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
        BigDecimal factor = monedaDestino != null
            ? conversionMonedaService.factor(s.getMoneda(), monedaDestino, LocalDateTime.now())
            : null;
        ServicioDTO dto = ServicioDTO.desdeServicio(s, requiereAdvertencia, monedaDestino, factor);
        int ocupados = calcularCuposOcupados(s.getIdServicio());
        dto.setCuposOcupados(ocupados);
        dto.setCuposDisponibles(Math.max(0, (s.getCupos() == null ? 0 : s.getCupos()) - ocupados));
        return dto;
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
        return aDTO(servicio, requiereAdvertencia(servicio, pref, viewerId), monedaPreferidaDe(viewerId));
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
        return ServicioDTO.desdeServicio(guardado);
    }

    @Transactional
    public ServicioDTO modificar(Long idServicio, Long idArtista, ServicioRequest request) {
        Servicio servicio = obtenerEntidad(idServicio);
        verificarDuenio(servicio, idArtista);
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
        return ServicioDTO.desdeServicio(guardado);
    }

    @Transactional
    public ServicioDTO pausar(Long idServicio, Long idArtista) {
        Servicio servicio = bloquearPropio(idServicio, idArtista);
        if (servicio.getEstado() == EstadoServicio.ARCHIVADO) {
            throw new ConflictoException("El servicio está archivado y no se puede pausar.");
        }
        if (servicio.getEstado() != EstadoServicio.PAUSADO) {
            servicio.setEstado(EstadoServicio.PAUSADO);
            servicio.setPausadoEn(LocalDateTime.now());
            servicioRepository.save(servicio);
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
            return ServicioDTO.desdeServicio(servicio);
        }
        if (!terminosService.tieneTerminosValidos(idServicio, idArtista)) {
            throw new IllegalArgumentException(
                "Completá tus términos de uso y servicio antes de publicar el servicio.");
        }
        servicio.setEstado(EstadoServicio.PUBLICADO);
        servicio.setPausadoEn(null);
        servicioRepository.save(servicio);
        return ServicioDTO.desdeServicio(servicio);
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
