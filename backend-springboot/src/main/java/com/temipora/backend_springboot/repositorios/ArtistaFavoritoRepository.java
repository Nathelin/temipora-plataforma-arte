package com.temipora.backend_springboot.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.ArtistaFavorito;
import com.temipora.backend_springboot.modelo.ArtistaFavoritoId;

public interface ArtistaFavoritoRepository extends JpaRepository<ArtistaFavorito, ArtistaFavoritoId> {

    @EntityGraph(attributePaths = "artista")
    List<ArtistaFavorito> findByUsuarioIdUsuarioOrderByFechaAgregadoDesc(Long idUsuario);

    Optional<ArtistaFavorito> findByUsuarioIdUsuarioAndArtistaIdUsuario(Long idUsuario, Long idArtista);

    boolean existsByUsuarioIdUsuarioAndArtistaIdUsuario(Long idUsuario, Long idArtista);

    long countByArtistaIdUsuario(Long idArtista);
}
