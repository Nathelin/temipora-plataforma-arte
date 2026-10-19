package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.temipora.backend_springboot.modelo.EPreferenciaContenidoSensible;
import com.temipora.backend_springboot.modelo.FiltroServicioRequest;
import com.temipora.backend_springboot.modelo.Medio;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.ServicioDTO;
import com.temipora.backend_springboot.modelo.ServicioImagen;
import com.temipora.backend_springboot.modelo.ServicioRequest;
import com.temipora.backend_springboot.modelo.TipoServicio;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.MedioRepository;
import com.temipora.backend_springboot.repositorios.ServicioRepository;
import com.temipora.backend_springboot.repositorios.TipoServicioRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@Service
@Validated
public class ServicioService {

    private final ServicioRepository servicioRepository;
    private final UsuarioRepository usuarioRepository;
    private final TipoServicioRepository tipoServicioRepository;
    private final MedioRepository medioRepository;
    private final ConversionMonedaService conversionMonedaService;
    private final MonedaService monedaService;

    public ServicioService(
            ServicioRepository servicioRepository,
            UsuarioRepository usuarioRepository,
            TipoServicioRepository tipoServicioRepository,
            MedioRepository medioRepository,
            ConversionMonedaService conversionMonedaService,
            MonedaService monedaService) {
        this.servicioRepository = servicioRepository;
        this.usuarioRepository = usuarioRepository;
        this.tipoServicioRepository = tipoServicioRepository;
        this.medioRepository = medioRepository;
        this.conversionMonedaService = conversionMonedaService;
        this.monedaService = monedaService;
    }

    @Transactional(readOnly = true)
    public List<ServicioDTO> listarMios(Long idArtista) {
        return servicioRepository.findByArtistaIdUsuarioOrderByNombreAsc(idArtista).stream()
            .map(ServicioDTO::desdeServicio)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ServicioDTO> listarActivos(FiltroServicioRequest filtro, Long viewerId) {
        EPreferenciaContenidoSensible pref = preferenciaDe(viewerId);
        boolean ocultarSensibles = pref == EPreferenciaContenidoSensible.OCULTAR;
        Moneda moneda = monedaPreferidaDe(viewerId);

        Specification<Servicio> spec = activos(ocultarSensibles);
        spec = aplicarFiltros(spec, filtro);
        return servicioRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "nombre")).stream()
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

    private Specification<Servicio> activos(boolean ocultarSensibles) {
        return (root, query, cb) -> {
            root.fetch("artista");
            if (ocultarSensibles) {
                return cb.and(
                    cb.isTrue(root.get("estado")),
                    cb.isFalse(root.get("contenidoSensible")));
            }
            return cb.isTrue(root.get("estado"));
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
        return ServicioDTO.desdeServicio(s, requiereAdvertencia, monedaDestino, factor);
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
        servicio.setEstado(true);
        aplicar(servicio, request);

        return ServicioDTO.desdeServicio(servicioRepository.save(servicio));
    }

    @Transactional
    public ServicioDTO modificar(Long idServicio, Long idArtista, ServicioRequest request) {
        Servicio servicio = obtenerEntidad(idServicio);
        verificarDuenio(servicio, idArtista);
        validarUnico(idArtista, request.getIdTipoServicio(), request.getNombre(), idServicio);
        aplicar(servicio, request);
        return ServicioDTO.desdeServicio(servicioRepository.save(servicio));
    }

    @Transactional
    public ServicioDTO darDeBaja(Long idServicio, Long idArtista) {
        Servicio servicio = obtenerEntidad(idServicio);
        verificarDuenio(servicio, idArtista);
        servicio.setEstado(false);
        return ServicioDTO.desdeServicio(servicioRepository.save(servicio));
    }

    @Transactional
    public ServicioDTO darDeAlta(Long idServicio, Long idArtista) {
        Servicio servicio = obtenerEntidad(idServicio);
        verificarDuenio(servicio, idArtista);
        servicio.setEstado(true);
        return ServicioDTO.desdeServicio(servicioRepository.save(servicio));
    }

    private void aplicar(Servicio servicio, ServicioRequest r) {
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
        servicio.setHabilitarCuposTrasEntrega(r.isHabilitarCuposTrasEntrega());
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
            ? servicioRepository.existsByArtistaIdUsuarioAndTipoServicioIdTipoServicioAndNombreIgnoreCase(
                idArtista, idTipoServicio, limpio)
            : servicioRepository.existsByArtistaIdUsuarioAndTipoServicioIdTipoServicioAndNombreIgnoreCaseAndIdServicioNot(
                idArtista, idTipoServicio, limpio, idExcluido);

        if (existe) {
            throw new IllegalArgumentException("Ya tenés un servicio con ese nombre y tipo.");
        }
    }

    private void verificarDuenio(Servicio servicio, Long idArtista) {
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
