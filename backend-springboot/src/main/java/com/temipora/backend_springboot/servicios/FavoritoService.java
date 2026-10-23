package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.ArtistaFavorito;
import com.temipora.backend_springboot.modelo.ArtistaFavoritoDTO;
import com.temipora.backend_springboot.modelo.ArtistaFavoritoId;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.PerfilArtisticoDTO;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.ArtistaFavoritoRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@Service
public class FavoritoService {

    private final ArtistaFavoritoRepository artistaFavoritoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PerfilArtisticoService perfilArtisticoService;

    public FavoritoService(
            ArtistaFavoritoRepository artistaFavoritoRepository,
            UsuarioRepository usuarioRepository,
            PerfilArtisticoService perfilArtisticoService) {
        this.artistaFavoritoRepository = artistaFavoritoRepository;
        this.usuarioRepository = usuarioRepository;
        this.perfilArtisticoService = perfilArtisticoService;
    }

    @Transactional(readOnly = true)
    public List<ArtistaFavoritoDTO> listar(String nombreUsuario) {
        Usuario usuario = obtenerUsuario(nombreUsuario);
        List<ArtistaFavorito> favoritos = artistaFavoritoRepository
            .findByUsuarioIdUsuarioOrderByFechaAgregadoDesc(usuario.getIdUsuario());

        Map<Long, PerfilArtisticoDTO> perfiles = perfilArtisticoService.calcularParaArtistas(
            favoritos.stream()
                .map(favorito -> favorito.getArtista().getIdUsuario())
                .toList());

        return favoritos.stream()
            .map(favorito -> ArtistaFavoritoDTO.desde(
                favorito,
                perfiles.get(favorito.getArtista().getIdUsuario())))
            .toList();
    }

    @Transactional
    public ArtistaFavoritoDTO agregar(String nombreUsuario, String nombreArtista) {
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
    public boolean eliminar(String nombreUsuario, String nombreArtista) {
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
    public boolean esFavorito(String nombreUsuario, String nombreArtista) {
        Usuario usuario = obtenerUsuario(nombreUsuario);
        Usuario artista = obtenerArtista(nombreArtista);

        return artistaFavoritoRepository.existsByUsuarioIdUsuarioAndArtistaIdUsuario(
            usuario.getIdUsuario(), artista.getIdUsuario());
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
