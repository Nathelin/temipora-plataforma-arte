package com.temipora.backend_springboot.repositorios;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.ArtistaFavorito;
import com.temipora.backend_springboot.modelo.ArtistaFavoritoId;

public interface ArtistaFavoritoRepository extends JpaRepository<ArtistaFavorito, ArtistaFavoritoId> {

    @Query(value = """
        SELECT f FROM ArtistaFavorito f
        JOIN f.artista a
        WHERE f.usuario.idUsuario = :idUsuario
          AND (:q IS NULL
               OR LOWER(a.nombreUsuario) LIKE :q
               OR LOWER(COALESCE(a.nombreDisplay, '')) LIKE :q)
        """,
        countQuery = """
        SELECT COUNT(f) FROM ArtistaFavorito f
        JOIN f.artista a
        WHERE f.usuario.idUsuario = :idUsuario
          AND (:q IS NULL
               OR LOWER(a.nombreUsuario) LIKE :q
               OR LOWER(COALESCE(a.nombreDisplay, '')) LIKE :q)
        """)
    @EntityGraph(attributePaths = "artista")
    Page<ArtistaFavorito> buscar(
        @Param("idUsuario") Long idUsuario,
        @Param("q") String q,
        Pageable pageable);

    Optional<ArtistaFavorito> findByUsuarioIdUsuarioAndArtistaIdUsuario(Long idUsuario, Long idArtista);

    boolean existsByUsuarioIdUsuarioAndArtistaIdUsuario(Long idUsuario, Long idArtista);

    long countByArtistaIdUsuario(Long idArtista);
}
