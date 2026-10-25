package com.temipora.backend_springboot.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.EstadoServicio;
import com.temipora.backend_springboot.modelo.ServicioFavorito;
import com.temipora.backend_springboot.modelo.ServicioFavoritoId;

public interface ServicioFavoritoRepository
        extends JpaRepository<ServicioFavorito, ServicioFavoritoId> {

    @Query(value = """
        SELECT f FROM ServicioFavorito f
        JOIN f.servicio s
        JOIN s.artista a
        WHERE f.usuario.idUsuario = :idUsuario
          AND (:q IS NULL
               OR LOWER(s.nombre) LIKE :q
               OR LOWER(a.nombreUsuario) LIKE :q
               OR LOWER(COALESCE(a.nombreDisplay, '')) LIKE :q)
          AND (:estado IS NULL
               OR (:estado = 'DISPONIBLE' AND s.estado = :publicado)
               OR (:estado = 'NO_DISPONIBLE' AND s.estado <> :publicado))
        """,
        countQuery = """
        SELECT COUNT(f) FROM ServicioFavorito f
        JOIN f.servicio s
        JOIN s.artista a
        WHERE f.usuario.idUsuario = :idUsuario
          AND (:q IS NULL
               OR LOWER(s.nombre) LIKE :q
               OR LOWER(a.nombreUsuario) LIKE :q
               OR LOWER(COALESCE(a.nombreDisplay, '')) LIKE :q)
          AND (:estado IS NULL
               OR (:estado = 'DISPONIBLE' AND s.estado = :publicado)
               OR (:estado = 'NO_DISPONIBLE' AND s.estado <> :publicado))
        """)
    @EntityGraph(attributePaths = {
        "servicio",
        "servicio.artista",
        "servicio.tipoServicio",
        "servicio.medio",
        "servicio.moneda"
    })
    Page<ServicioFavorito> buscar(
        @Param("idUsuario") Long idUsuario,
        @Param("q") String q,
        @Param("estado") String estado,
        @Param("publicado") EstadoServicio publicado,
        Pageable pageable);

    @Query("SELECT f.id.idServicio FROM ServicioFavorito f WHERE f.id.idUsuario = :idUsuario")
    List<Long> findIdsServicioByUsuario(@Param("idUsuario") Long idUsuario);

    Optional<ServicioFavorito> findByUsuarioIdUsuarioAndServicioIdServicio(
            Long idUsuario, Long idServicio);

    boolean existsByUsuarioIdUsuarioAndServicioIdServicio(Long idUsuario, Long idServicio);
}
