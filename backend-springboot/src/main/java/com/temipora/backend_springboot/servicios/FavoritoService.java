package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.ArtistaFavorito;
import com.temipora.backend_springboot.modelo.ArtistaFavoritoDTO;
import com.temipora.backend_springboot.modelo.ArtistaFavoritoId;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.EstadoServicio;
import com.temipora.backend_springboot.modelo.EstadoSolicitud;
import com.temipora.backend_springboot.modelo.ModoRecepcionSolicitud;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.PerfilArtisticoDTO;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.ProductoFavorito;
import com.temipora.backend_springboot.modelo.ProductoFavoritoId;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.ServicioDTO;
import com.temipora.backend_springboot.modelo.ServicioFavorito;
import com.temipora.backend_springboot.modelo.ServicioFavoritoDTO;
import com.temipora.backend_springboot.modelo.ServicioFavoritoId;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.ArtistaFavoritoRepository;
import com.temipora.backend_springboot.repositorios.ProductoFavoritoRepository;
import com.temipora.backend_springboot.repositorios.ProductoRepository;
import com.temipora.backend_springboot.repositorios.ServicioFavoritoRepository;
import com.temipora.backend_springboot.repositorios.ServicioRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@Service
public class FavoritoService {

    private static final int TAMANIO_MAXIMO = 48;

    private static final List<EstadoSolicitud> ESTADOS_QUE_OCUPAN_CUPO = List.of(
        EstadoSolicitud.PRESUPUESTADA,
        EstadoSolicitud.PAGADA);

    private final ArtistaFavoritoRepository artistaFavoritoRepository;
    private final ServicioFavoritoRepository servicioFavoritoRepository;
    private final ProductoFavoritoRepository productoFavoritoRepository;
    private final ServicioRepository servicioRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PerfilArtisticoService perfilArtisticoService;
    private final ServicioService servicioService;

    public FavoritoService(
            ArtistaFavoritoRepository artistaFavoritoRepository,
            ServicioFavoritoRepository servicioFavoritoRepository,
            ProductoFavoritoRepository productoFavoritoRepository,
            ServicioRepository servicioRepository,
            ProductoRepository productoRepository,
            UsuarioRepository usuarioRepository,
            PerfilArtisticoService perfilArtisticoService,
            ServicioService servicioService) {
        this.artistaFavoritoRepository = artistaFavoritoRepository;
        this.servicioFavoritoRepository = servicioFavoritoRepository;
        this.productoFavoritoRepository = productoFavoritoRepository;
        this.servicioRepository = servicioRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.perfilArtisticoService = perfilArtisticoService;
        this.servicioService = servicioService;
    }

    // ------------------------------------------------------------------
    // Artistas
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PaginaDTO<ArtistaFavoritoDTO> listarArtistas(
            String nombreUsuario,
            int pagina,
            int tamanio,
            String q,
            String orden) {
        Usuario usuario = obtenerUsuario(nombreUsuario);
        Page<ArtistaFavorito> paginaFavoritos = artistaFavoritoRepository.buscar(
            usuario.getIdUsuario(),
            patronBusqueda(q),
            pageRequest(pagina, tamanio, ordenArtistas(orden)));

        Map<Long, PerfilArtisticoDTO> perfiles = perfilArtisticoService.calcularParaArtistas(
            paginaFavoritos.getContent().stream()
                .map(favorito -> favorito.getArtista().getIdUsuario())
                .toList());

        List<ArtistaFavoritoDTO> contenido = paginaFavoritos.getContent().stream()
            .map(favorito -> ArtistaFavoritoDTO.desde(
                favorito,
                perfiles.get(favorito.getArtista().getIdUsuario())))
            .toList();

        return paginar(paginaFavoritos, contenido);
    }

    @Transactional
    public ArtistaFavoritoDTO agregarArtista(String nombreUsuario, String nombreArtista) {
        Usuario usuario = obtenerUsuario(nombreUsuario);
        Usuario artista = obtenerArtistaDisponible(nombreArtista);

        if (usuario.getIdUsuario().equals(artista.getIdUsuario())) {
            throw new IllegalArgumentException("No podés agregarte a vos mismo a tus favoritos.");
        }

        ArtistaFavorito favorito = artistaFavoritoRepository
            .findByUsuarioIdUsuarioAndArtistaIdUsuario(
                usuario.getIdUsuario(), artista.getIdUsuario())
            .orElseGet(() -> {
                ArtistaFavorito nuevo = new ArtistaFavorito();
                nuevo.setId(new ArtistaFavoritoId(
                    usuario.getIdUsuario(), artista.getIdUsuario()));
                nuevo.setUsuario(usuario);
                nuevo.setArtista(artista);
                nuevo.setFechaAgregado(LocalDateTime.now());
                return artistaFavoritoRepository.save(nuevo);
            });

        return ArtistaFavoritoDTO.desde(
            favorito, perfilArtisticoService.calcular(artista.getIdUsuario()));
    }

    @Transactional
    public boolean eliminarArtista(String nombreUsuario, String nombreArtista) {
        Usuario usuario = obtenerUsuario(nombreUsuario);
        Usuario artista = obtenerArtista(nombreArtista);

        return artistaFavoritoRepository
            .findByUsuarioIdUsuarioAndArtistaIdUsuario(
                usuario.getIdUsuario(), artista.getIdUsuario())
            .map(favorito -> {
                artistaFavoritoRepository.delete(favorito);
                return true;
            })
            .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean esFavoritoArtista(String nombreUsuario, String nombreArtista) {
        Usuario usuario = obtenerUsuario(nombreUsuario);
        Usuario artista = obtenerArtista(nombreArtista);

        return artistaFavoritoRepository.existsByUsuarioIdUsuarioAndArtistaIdUsuario(
            usuario.getIdUsuario(), artista.getIdUsuario());
    }

    // ------------------------------------------------------------------
    // Servicios
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PaginaDTO<ServicioFavoritoDTO> listarServicios(
            String nombreUsuario,
            int pagina,
            int tamanio,
            String q,
            String orden,
            String estado) {
        Usuario usuario = obtenerUsuario(nombreUsuario);
        Page<ServicioFavorito> paginaFavoritos = servicioFavoritoRepository.buscar(
            usuario.getIdUsuario(),
            patronBusqueda(q),
            normalizarEstado(estado),
            EstadoServicio.PUBLICADO,
            ModoRecepcionSolicitud.ABIERTA,
            ESTADOS_QUE_OCUPAN_CUPO,
            pageRequest(pagina, tamanio, ordenServicios(orden)));

        List<Long> idsServicios = paginaFavoritos.getContent().stream()
            .map(favorito -> favorito.getId().getIdServicio())
            .toList();

        Map<Long, ServicioDTO> servicios = idsServicios.isEmpty()
            ? Map.of()
            : servicioService.convertirParaVisor(
                servicioRepository.findByIdServicioIn(idsServicios),
                usuario.getIdUsuario());

        List<ServicioFavoritoDTO> contenido = paginaFavoritos.getContent().stream()
            .map(favorito -> {
                ServicioDTO dto = servicios.get(favorito.getId().getIdServicio());
                return ServicioFavoritoDTO.desde(
                    favorito,
                    dto,
                    dto != null && dto.isDisponibleAhora());
            })
            .toList();

        return paginar(paginaFavoritos, contenido);
    }

    @Transactional(readOnly = true)
    public List<Long> listarIdsServicios(String nombreUsuario) {
        Usuario usuario = obtenerUsuario(nombreUsuario);
        return servicioFavoritoRepository.findIdsServicioByUsuario(usuario.getIdUsuario());
    }

    @Transactional
    public ServicioFavoritoDTO agregarServicio(String nombreUsuario, Long idServicio) {
        Usuario usuario = obtenerUsuario(nombreUsuario);
        Servicio servicio = servicioRepository.findByIdServicio(idServicio)
            .orElseThrow(() -> new NoSuchElementException(
                "Servicio no encontrado: " + idServicio));

        if (servicio.getArtista() != null
                && servicio.getArtista().getIdUsuario().equals(usuario.getIdUsuario())) {
            throw new IllegalArgumentException(
                "No podés agregar tu propio servicio a tus favoritos.");
        }
        if (servicio.getEstado() != EstadoServicio.PUBLICADO) {
            throw new IllegalArgumentException(
                "Solo podés agregar servicios publicados a tus favoritos.");
        }

        ServicioFavorito favorito = servicioFavoritoRepository
            .findByUsuarioIdUsuarioAndServicioIdServicio(
                usuario.getIdUsuario(), servicio.getIdServicio())
            .orElseGet(() -> {
                ServicioFavorito nuevo = new ServicioFavorito();
                nuevo.setId(new ServicioFavoritoId(
                    usuario.getIdUsuario(), servicio.getIdServicio()));
                nuevo.setUsuario(usuario);
                nuevo.setServicio(servicio);
                nuevo.setFechaAgregado(LocalDateTime.now());
                return servicioFavoritoRepository.save(nuevo);
            });

        ServicioDTO dto = servicioService
            .convertirParaVisor(List.of(servicio), usuario.getIdUsuario())
            .get(servicio.getIdServicio());

        return ServicioFavoritoDTO.desde(
            favorito, dto, dto != null && dto.isDisponibleAhora());
    }

    @Transactional
    public boolean eliminarServicio(String nombreUsuario, Long idServicio) {
        Usuario usuario = obtenerUsuario(nombreUsuario);

        return servicioFavoritoRepository
            .findByUsuarioIdUsuarioAndServicioIdServicio(
                usuario.getIdUsuario(), idServicio)
            .map(favorito -> {
                servicioFavoritoRepository.delete(favorito);
                return true;
            })
            .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean esFavoritoServicio(String nombreUsuario, Long idServicio) {
        Usuario usuario = obtenerUsuario(nombreUsuario);

        return servicioFavoritoRepository
            .existsByUsuarioIdUsuarioAndServicioIdServicio(
                usuario.getIdUsuario(), idServicio);
    }

    // ------------------------------------------------------------------
    // Productos
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Long> listarIdsProductos(String nombreUsuario) {
        Usuario usuario = obtenerUsuario(nombreUsuario);
        return productoFavoritoRepository.findIdsProductoByUsuario(
            usuario.getIdUsuario());
    }

    @Transactional
    public void agregarProducto(String nombreUsuario, Long idProducto) {
        Usuario usuario = obtenerUsuario(nombreUsuario);
        if (productoFavoritoRepository
                .existsByUsuarioIdUsuarioAndProductoIdProducto(
                    usuario.getIdUsuario(), idProducto)) {
            return;
        }
        Producto producto = productoRepository.findByIdProducto(idProducto)
            .orElseThrow(() -> new NoSuchElementException(
                "Producto no encontrado: " + idProducto));
        ProductoFavorito favorito = new ProductoFavorito();
        favorito.setId(new ProductoFavoritoId(
            usuario.getIdUsuario(), idProducto));
        favorito.setUsuario(usuario);
        favorito.setProducto(producto);
        productoFavoritoRepository.save(favorito);
    }

    @Transactional
    public void eliminarProducto(String nombreUsuario, Long idProducto) {
        Usuario usuario = obtenerUsuario(nombreUsuario);
        productoFavoritoRepository
            .findByUsuarioIdUsuarioAndProductoIdProducto(
                usuario.getIdUsuario(), idProducto)
            .ifPresent(productoFavoritoRepository::delete);
    }

    @Transactional(readOnly = true)
    public boolean esFavoritoProducto(String nombreUsuario, Long idProducto) {
        Usuario usuario = obtenerUsuario(nombreUsuario);
        return productoFavoritoRepository
            .existsByUsuarioIdUsuarioAndProductoIdProducto(
                usuario.getIdUsuario(), idProducto);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Pageable pageRequest(int pagina, int tamanio, Sort sort) {
        int paginaSegura = Math.max(pagina, 0);
        int tamanioSeguro = Math.min(Math.max(tamanio, 1), TAMANIO_MAXIMO);
        return PageRequest.of(paginaSegura, tamanioSeguro, sort);
    }

    private Sort ordenArtistas(String orden) {
        if (orden == null || orden.isBlank() || "recientes".equalsIgnoreCase(orden)) {
            return Sort.by(Sort.Direction.DESC, "fechaAgregado");
        }
        if ("alfabetico".equalsIgnoreCase(orden) || "a-z".equalsIgnoreCase(orden)) {
            return Sort.by(Sort.Direction.ASC, "artista.nombreUsuario");
        }
        throw new IllegalArgumentException("Orden inválido: " + orden);
    }

    private Sort ordenServicios(String orden) {
        if (orden == null || orden.isBlank() || "recientes".equalsIgnoreCase(orden)) {
            return Sort.by(Sort.Direction.DESC, "fechaAgregado");
        }
        if ("alfabetico".equalsIgnoreCase(orden) || "a-z".equalsIgnoreCase(orden)) {
            return Sort.by(Sort.Direction.ASC, "servicio.nombre");
        }
        throw new IllegalArgumentException("Orden inválido: " + orden);
    }

    private String normalizarEstado(String estado) {
        if (estado == null || estado.isBlank() || "todos".equalsIgnoreCase(estado)) {
            return null;
        }
        if ("disponibles".equalsIgnoreCase(estado)) {
            return "DISPONIBLE";
        }
        if ("no-disponibles".equalsIgnoreCase(estado)
                || "no_disponibles".equalsIgnoreCase(estado)) {
            return "NO_DISPONIBLE";
        }
        throw new IllegalArgumentException("Estado inválido: " + estado);
    }

    private String patronBusqueda(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        return "%" + q.trim().toLowerCase() + "%";
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

    private Usuario obtenerUsuario(String nombreUsuario) {
        return usuarioRepository.findByNombreUsuarioIgnoreCase(nombreUsuario)
            .orElseThrow(() -> new NoSuchElementException(
                "Usuario no encontrado: " + nombreUsuario));
    }

    private Usuario obtenerArtista(String nombreArtista) {
        Usuario artista = usuarioRepository.findByNombreUsuarioIgnoreCase(nombreArtista)
            .orElseThrow(() -> new NoSuchElementException(
                "Artista no encontrado: " + nombreArtista));

        if (artista.getRol() == null || artista.getRol().getNombre() != ERol.ARTISTA) {
            throw new IllegalArgumentException("El usuario indicado no es un artista.");
        }
        return artista;
    }

    private Usuario obtenerArtistaDisponible(String nombreArtista) {
        Usuario artista = obtenerArtista(nombreArtista);
        if (!artista.isEstadoCuenta()) {
            throw new IllegalArgumentException("El artista no está disponible.");
        }
        return artista;
    }
}
