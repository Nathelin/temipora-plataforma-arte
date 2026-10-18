package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.temipora.backend_springboot.modelo.FiltroServicioRequest;
import com.temipora.backend_springboot.modelo.Medio;
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

    public ServicioService(
            ServicioRepository servicioRepository,
            UsuarioRepository usuarioRepository,
            TipoServicioRepository tipoServicioRepository,
            MedioRepository medioRepository) {
        this.servicioRepository = servicioRepository;
        this.usuarioRepository = usuarioRepository;
        this.tipoServicioRepository = tipoServicioRepository;
        this.medioRepository = medioRepository;
    }

    @Transactional(readOnly = true)
    public List<ServicioDTO> listarMios(Long idArtista) {
        return servicioRepository.findByArtistaIdUsuarioOrderByNombreAsc(idArtista).stream()
            .map(ServicioDTO::desdeServicio)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ServicioDTO> listarActivos(FiltroServicioRequest filtro) {
        Specification<Servicio> spec = activos();
        spec = aplicarFiltros(spec, filtro);
        return servicioRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "nombre")).stream()
            .map(ServicioDTO::desdeServicio)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ServicioDTO> listarActivosDeArtista(String nombreUsuario) {
        Specification<Servicio> spec = activos()
            .and((root, query, cb) ->
                cb.equal(cb.lower(root.get("artista").get("nombreUsuario")),
                    nombreUsuario.trim().toLowerCase()));
        return servicioRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "nombre")).stream()
            .map(ServicioDTO::desdeServicio)
            .toList();
    }

    private Specification<Servicio> activos() {
        return (root, query, cb) -> {
            root.fetch("artista");
            return cb.isTrue(root.get("estado"));
        };
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
                sensible ? cb.isNotNull(root.get("contenidoSensible"))
                         : cb.isNull(root.get("contenidoSensible")));
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
    public ServicioDTO obtener(Long idServicio) {
        return ServicioDTO.desdeServicio(obtenerEntidad(idServicio));
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
        servicio.setContenidoSensible(r.getContenidoSensible());
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
}
