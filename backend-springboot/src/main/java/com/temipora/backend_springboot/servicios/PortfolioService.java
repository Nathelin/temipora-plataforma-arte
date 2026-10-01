package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.EPreferenciaContenidoSensible;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.Imagen;
import com.temipora.backend_springboot.modelo.ImagenPortfolioDTO;
import com.temipora.backend_springboot.modelo.ImagenPortfolioRequest;
import com.temipora.backend_springboot.modelo.Portfolio;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.ImagenRepository;
import com.temipora.backend_springboot.repositorios.PortfolioRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@Service
public class PortfolioService {

    private final UsuarioRepository usuarioRepository;
    private final PortfolioRepository portfolioRepository;
    private final ImagenRepository imagenRepository;

    public PortfolioService(
            UsuarioRepository usuarioRepository,
            PortfolioRepository portfolioRepository,
            ImagenRepository imagenRepository) {
        this.usuarioRepository = usuarioRepository;
        this.portfolioRepository = portfolioRepository;
        this.imagenRepository = imagenRepository;
    }

    @Transactional(readOnly = true)
    public List<ImagenPortfolioDTO> listar(String nombreUsuario) {
        Usuario artista = obtenerArtista(nombreUsuario);
        return portfolioRepository.findByArtista(artista)
            .map(portfolio -> imagenRepository
                .findByPortfolioAndEstadoTrueOrderByFechaSubidaDesc(portfolio)
                .stream()
                .map(ImagenPortfolioDTO::desdeImagen)
                .toList())
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
        return page.map(img -> {
            boolean requiereAdvertencia = img.getContenidoSensible() != null
                && pref == EPreferenciaContenidoSensible.ADVERTIR
                && (viewerId == null || !img.getUsuario().getIdUsuario().equals(viewerId));
            return ImagenPortfolioDTO.desdeImagen(img, requiereAdvertencia);
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

        return ImagenPortfolioDTO.desdeImagen(imagenRepository.save(imagen));
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
}