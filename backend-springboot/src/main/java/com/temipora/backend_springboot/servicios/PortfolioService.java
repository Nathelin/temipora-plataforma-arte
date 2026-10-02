package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.EPreferenciaContenidoSensible;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.Etiqueta;
import com.temipora.backend_springboot.modelo.EtiquetaDTO;
import com.temipora.backend_springboot.modelo.EtiquetaEstado;
import com.temipora.backend_springboot.modelo.Imagen;
import com.temipora.backend_springboot.modelo.ImagenEtiqueta;
import com.temipora.backend_springboot.modelo.ImagenPortfolioDTO;
import com.temipora.backend_springboot.modelo.ImagenPortfolioRequest;
import com.temipora.backend_springboot.modelo.Portfolio;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.EtiquetaRepository;
import com.temipora.backend_springboot.repositorios.ImagenEtiquetaRepository;
import com.temipora.backend_springboot.repositorios.ImagenRepository;
import com.temipora.backend_springboot.repositorios.PortfolioRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@Service
public class PortfolioService {

    private static final int MAXIMO_ETIQUETAS = 5;

    private final UsuarioRepository usuarioRepository;
    private final PortfolioRepository portfolioRepository;
    private final ImagenRepository imagenRepository;
    private final EtiquetaRepository etiquetaRepository;
    private final ImagenEtiquetaRepository imagenEtiquetaRepository;

    public PortfolioService(
            UsuarioRepository usuarioRepository,
            PortfolioRepository portfolioRepository,
            ImagenRepository imagenRepository,
            EtiquetaRepository etiquetaRepository,
            ImagenEtiquetaRepository imagenEtiquetaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.portfolioRepository = portfolioRepository;
        this.imagenRepository = imagenRepository;
        this.etiquetaRepository = etiquetaRepository;
        this.imagenEtiquetaRepository = imagenEtiquetaRepository;
    }

    @Transactional(readOnly = true)
    public List<ImagenPortfolioDTO> listar(String nombreUsuario) {
        Usuario artista = obtenerArtista(nombreUsuario);
        return portfolioRepository.findByArtista(artista)
            .map(portfolio -> {
                List<Imagen> imagenes = imagenRepository
                    .findByPortfolioAndEstadoTrueOrderByFechaSubidaDesc(portfolio);
                Map<Long, List<EtiquetaDTO>> porImagen = etiquetasPorImagen(idsDe(imagenes));
                return imagenes.stream()
                    .map(img -> {
                        ImagenPortfolioDTO dto = ImagenPortfolioDTO.desdeImagen(img);
                        dto.setEtiquetas(porImagen.getOrDefault(img.getIdImagen(), List.of()));
                        return dto;
                    })
                    .toList();
            })
            .orElseGet(List::of);
    }

    @Transactional(readOnly = true)
    public Page<ImagenPortfolioDTO> listarParaVisor(Long portfolioId, Long viewerId, Pageable pageable) {
        // 1. Obtener preferencia del visitante (o default ADVERTIR)
        EPreferenciaContenidoSensible pref = (viewerId != null)
            ? usuarioRepository.findByIdUsuario(viewerId)
                .map(Usuario::getPreferenciaSensible)
                .orElse(EPreferenciaContenidoSensible.ADVERTIR)
            : EPreferenciaContenidoSensible.ADVERTIR;

        // 2. Consultar imágenes visibles según preferencia
        Page<Imagen> page = pref == EPreferenciaContenidoSensible.OCULTAR
            ? imagenRepository.findVisibleForViewerOcultandoSensibles(portfolioId, viewerId, pageable)
            : imagenRepository.findVisibleForViewer(portfolioId, pageable);

        // 3. Mapear a DTO con campo calculado requiereAdvertencia
        Map<Long, List<EtiquetaDTO>> porImagen = etiquetasPorImagen(idsDe(page.getContent()));
        return page.map(img -> {
            boolean requiereAdvertencia = img.getContenidoSensible() != null
                && pref == EPreferenciaContenidoSensible.ADVERTIR
                && (viewerId == null || !img.getUsuario().getIdUsuario().equals(viewerId));
            ImagenPortfolioDTO dto = ImagenPortfolioDTO.desdeImagen(img, requiereAdvertencia);
            dto.setEtiquetas(porImagen.getOrDefault(img.getIdImagen(), List.of()));
            return dto;
        });
    }

    @Transactional(readOnly = true)
    public Page<ImagenPortfolioDTO> listarParaVisorPorNombreUsuario(String nombreUsuario, Long viewerId, Pageable pageable) {
        Usuario artista = obtenerArtista(nombreUsuario);
        Portfolio portfolio = portfolioRepository.findByArtista(artista)
            .orElse(null);
        if (portfolio == null) {
            return Page.empty(pageable);
        }
        return listarParaVisor(portfolio.getIdPortfolio(), viewerId, pageable);
    }

    @Transactional
    public ImagenPortfolioDTO crear(Long idUsuario, ImagenPortfolioRequest request) {
        Usuario artista = usuarioRepository.findByIdUsuario(idUsuario)
            .orElseThrow(() -> new NoSuchElementException("Artista no encontrado: " + idUsuario));
        validarArtista(artista);

        List<Long> pedidos = normalizarPedido(request.getIdsEtiqueta());
        List<Etiqueta> etiquetas = cargarEtiquetasAsignables(artista, pedidos);

        Portfolio portfolio = portfolioRepository.findByArtista(artista)
            .orElseGet(() -> {
                Portfolio nuevo = new Portfolio();
                nuevo.setArtista(artista);
                return portfolioRepository.save(nuevo);
            });

        String titulo = request.getTitulo().trim();
        if (imagenRepository.existsByPortfolioAndTituloIgnoreCaseAndMedioAndEstadoTrue(
                portfolio, titulo, request.getMedio())) {
            throw new IllegalArgumentException("Ya existe una imagen con ese título y medio en el portfolio.");
        }

        Imagen imagen = new Imagen();
        imagen.setUrl(request.getUrl().trim());
        imagen.setTitulo(titulo);
        imagen.setDescripcion(request.getDescripcion().trim());
        imagen.setFechaSubida(LocalDateTime.now());
        imagen.setMedio(request.getMedio());
        imagen.setMateriales(request.getMateriales() == null ? null : request.getMateriales().trim());
        imagen.setContenidoSensible(request.getContenidoSensible());
        imagen.setEstado(true);
        imagen.setPortfolio(portfolio);
        imagen.setUsuario(artista); // asignar el usuario que sube la imagen

        Imagen guardada = imagenRepository.save(imagen);
        for (Etiqueta etiqueta : etiquetas) {
            imagenEtiquetaRepository.save(ImagenEtiqueta.builder()
                .imagen(guardada)
                .etiqueta(etiqueta)
                .build());
        }
        return dtoConEtiquetas(guardada, false);
    }

    @Transactional
    public ImagenPortfolioDTO modificar(Long idUsuario, Long idImagen, ImagenPortfolioRequest request) {
        Usuario artista = usuarioRepository.findByIdUsuario(idUsuario)
            .orElseThrow(() -> new NoSuchElementException("Artista no encontrado: " + idUsuario));
        validarArtista(artista);

        List<Long> pedidos = normalizarPedido(request.getIdsEtiqueta());

        Imagen imagen = imagenRepository.findById(idImagen)
            .orElseThrow(() -> new NoSuchElementException("Imagen no encontrada: " + idImagen));
        if (!artista.getIdUsuario().equals(imagen.getPortfolio().getArtista().getIdUsuario())) {
            throw new NoSuchElementException("Imagen no encontrada: " + idImagen);
        }

        List<ImagenEtiqueta> actuales = imagenEtiquetaRepository.findByImagenIdImagen(imagen.getIdImagen());
        Set<Long> idsActuales = actuales.stream()
            .map(ie -> ie.getEtiqueta().getIdEtiqueta())
            .collect(Collectors.toSet());
        List<Etiqueta> nuevas = cargarEtiquetasAsignables(artista,
            pedidos.stream().filter(id -> !idsActuales.contains(id)).toList());

        String titulo = request.getTitulo().trim();
        boolean tituloYMedioPropios = imagen.getTitulo().equalsIgnoreCase(titulo)
            && imagen.getMedio() == request.getMedio();
        if (!tituloYMedioPropios && imagenRepository.existsByPortfolioAndTituloIgnoreCaseAndMedioAndEstadoTrue(
                imagen.getPortfolio(), titulo, request.getMedio())) {
            throw new IllegalArgumentException("Ya existe una imagen con ese título y medio en el portfolio.");
        }

        imagen.setTitulo(titulo);
        imagen.setDescripcion(request.getDescripcion().trim());
        imagen.setUrl(request.getUrl().trim());
        imagen.setMedio(request.getMedio());
        imagen.setMateriales(request.getMateriales() == null ? null : request.getMateriales().trim());
        imagen.setContenidoSensible(request.getContenidoSensible());
        Imagen guardada = imagenRepository.save(imagen);

        for (ImagenEtiqueta fila : actuales) {
            if (!pedidos.contains(fila.getEtiqueta().getIdEtiqueta())) {
                imagenEtiquetaRepository.delete(fila);
            }
        }
        for (Etiqueta etiqueta : nuevas) {
            imagenEtiquetaRepository.save(ImagenEtiqueta.builder()
                .imagen(guardada)
                .etiqueta(etiqueta)
                .build());
        }
        return dtoConEtiquetas(guardada, false);
    }

    public String nombreUsuario(Long idUsuario) {
        return usuarioRepository.findByIdUsuario(idUsuario)
            .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado: " + idUsuario))
            .getNombreUsuario();
    }

    private Usuario obtenerArtista(String nombreUsuario) {
        Usuario usuario = usuarioRepository.findByNombreUsuarioIgnoreCase(nombreUsuario)
            .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado: " + nombreUsuario));
        validarArtista(usuario);
        return usuario;
    }

    private void validarArtista(Usuario usuario) {
        if (usuario.getRol() == null || usuario.getRol().getNombre() != ERol.ARTISTA) {
            throw new IllegalArgumentException("El usuario no es un artista.");
        }
    }

    // ---------------------------------------------------------------
    // Etiquetas de la imagen (Fase B)
    // ---------------------------------------------------------------

    private List<Long> normalizarPedido(List<Long> idsPedidos) {
        List<Long> pedidos = idsPedidos == null ? List.of()
            : idsPedidos.stream().filter(Objects::nonNull).distinct().toList();
        if (pedidos.size() > MAXIMO_ETIQUETAS) {
            throw new IllegalArgumentException(
                "Una imagen puede tener como máximo " + MAXIMO_ETIQUETAS + " etiquetas.");
        }
        return pedidos;
    }

    private List<Etiqueta> cargarEtiquetasAsignables(Usuario artista, List<Long> ids) {
        List<Etiqueta> etiquetas = new ArrayList<>();
        for (Long id : ids) {
            Etiqueta etiqueta = etiquetaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Etiqueta no encontrada: " + id));
            validarAsignable(etiqueta, artista);
            etiquetas.add(etiqueta);
        }
        return etiquetas;
    }

    private void validarAsignable(Etiqueta etiqueta, Usuario artista) {
        boolean propiaPendiente = etiqueta.getEstado() == EtiquetaEstado.PENDIENTE
            && etiqueta.getCreadoPor() != null
            && etiqueta.getCreadoPor().getIdUsuario().equals(artista.getIdUsuario());
        if (etiqueta.getEstado() == EtiquetaEstado.VALIDA || propiaPendiente) {
            return;
        }
        if (etiqueta.getEstado() == EtiquetaEstado.INACTIVA) {
            throw new IllegalArgumentException(
                "La etiqueta '" + etiqueta.getNombre() + "' fue dada de baja y no puede asignarse a piezas nuevas.");
        }
        if (etiqueta.getEstado() == EtiquetaEstado.RECHAZADA) {
            throw new IllegalArgumentException(
                "La etiqueta '" + etiqueta.getNombre() + "' fue rechazada y no puede asignarse a una imagen.");
        }
        throw new IllegalArgumentException(
            "La etiqueta '" + etiqueta.getNombre() + "' está pendiente de evaluación y solo su creador puede asignarla.");
    }

    private ImagenPortfolioDTO dtoConEtiquetas(Imagen imagen, boolean requiereAdvertencia) {
        ImagenPortfolioDTO dto = ImagenPortfolioDTO.desdeImagen(imagen, requiereAdvertencia);
        dto.setEtiquetas(imagenEtiquetaRepository.findByImagenIdImagen(imagen.getIdImagen()).stream()
            .map(ie -> EtiquetaDTO.desdeEtiqueta(ie.getEtiqueta()))
            .toList());
        return dto;
    }

    private Map<Long, List<EtiquetaDTO>> etiquetasPorImagen(Collection<Long> idsImagen) {
        if (idsImagen.isEmpty()) {
            return Map.of();
        }
        return imagenEtiquetaRepository.findByImagenIdImagenIn(idsImagen).stream()
            .collect(Collectors.groupingBy(
                ie -> ie.getImagen().getIdImagen(),
                Collectors.mapping(ie -> EtiquetaDTO.desdeEtiqueta(ie.getEtiqueta()), Collectors.toList())));
    }

    private List<Long> idsDe(List<Imagen> imagenes) {
        return imagenes.stream().map(Imagen::getIdImagen).filter(Objects::nonNull).toList();
    }
}
