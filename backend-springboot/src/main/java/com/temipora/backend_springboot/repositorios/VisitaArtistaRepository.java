package com.temipora.backend_springboot.repositorios;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.VisitaArtista;
import com.temipora.backend_springboot.modelo.VisitaArtistaId;

public interface VisitaArtistaRepository
        extends JpaRepository<VisitaArtista, VisitaArtistaId> {

    @Modifying
    @Query(value = """
        INSERT INTO visita_artista (id_usuario, id_artista, fecha, cantidad, ultima_visita)
        VALUES (:idUsuario, :idArtista, CURRENT_DATE, 1, CURRENT_TIMESTAMP)
        ON CONFLICT (id_usuario, id_artista, fecha)
        DO UPDATE SET cantidad = visita_artista.cantidad + 1,
                      ultima_visita = CURRENT_TIMESTAMP
        """, nativeQuery = true)
    void registrar(
        @Param("idUsuario") Long idUsuario,
        @Param("idArtista") Long idArtista);

    @Query(value = """
        SELECT id_artista, SUM(cantidad), MAX(ultima_visita)
        FROM visita_artista
        WHERE id_usuario = :idUsuario
          AND fecha >= :desde
        GROUP BY id_artista
        """, nativeQuery = true)
    List<Object[]> resumenPorUsuario(
        @Param("idUsuario") Long idUsuario,
        @Param("desde") LocalDate desde);

    @Query(value = """
        SELECT v2.id_artista, COUNT(DISTINCT v2.id_usuario)
        FROM visita_artista v1
        JOIN visita_artista v2 ON v2.id_usuario = v1.id_usuario
        WHERE v1.id_usuario = :idUsuario
          AND v1.fecha >= :desde
          AND v2.fecha >= :desde
          AND v2.id_artista <> v1.id_artista
        GROUP BY v2.id_artista
        """, nativeQuery = true)
    List<Object[]> coVisitas(
        @Param("idUsuario") Long idUsuario,
        @Param("desde") LocalDate desde);

    @Query(value = """
        SELECT id_artista, COUNT(DISTINCT id_usuario)
        FROM visita_artista
        WHERE id_artista IN (:ids)
        GROUP BY id_artista
        """, nativeQuery = true)
    List<Object[]> popularidad(@Param("ids") Collection<Long> ids);
}
