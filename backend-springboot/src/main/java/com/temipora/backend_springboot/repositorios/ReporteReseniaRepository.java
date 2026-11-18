package com.temipora.backend_springboot.repositorios;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.EstadoReporteResenia;
import com.temipora.backend_springboot.modelo.ReporteResenia;

public interface ReporteReseniaRepository
        extends JpaRepository<ReporteResenia, Long> {

    boolean existsByReseniaIdReseniaAndReportanteIdUsuario(
        Long idResenia, Long idReportante);

    long countByReseniaIdReseniaAndEstado(
        Long idResenia, EstadoReporteResenia estado);

    @EntityGraph(attributePaths = "reportante")
    List<ReporteResenia> findByReseniaIdReseniaInOrderByFechaCreacionAsc(
        Collection<Long> idsResenia);

    @EntityGraph(attributePaths = "reportante")
    List<ReporteResenia> findByReseniaIdReseniaOrderByFechaCreacionAsc(
        Long idResenia);

    @EntityGraph(attributePaths = "reportante")
    List<ReporteResenia> findByReseniaIdReseniaAndEstadoOrderByFechaCreacionAsc(
        Long idResenia, EstadoReporteResenia estado);

    @Query("""
        SELECT rr.resenia.idResenia FROM ReporteResenia rr
        WHERE rr.reportante.idUsuario = :idUsuario
        ORDER BY rr.fechaCreacion DESC
        """)
    List<Long> findIdsReseniasReportadasPorUsuario(
        @Param("idUsuario") Long idUsuario);

    @Query(value = """
        SELECT rr FROM ReporteResenia rr
        JOIN FETCH rr.resenia r
        LEFT JOIN FETCH r.pedido p
        LEFT JOIN FETCH p.servicio s
        LEFT JOIN FETCH r.producto prod
        JOIN FETCH r.autor a
        JOIN FETCH rr.reportante rep
        LEFT JOIN FETCH rr.resueltoPor res
        WHERE (:estado IS NULL OR rr.estado = :estado)
        ORDER BY COALESCE(rr.fechaResolucion, rr.fechaCreacion) DESC
        """,
        countQuery = """
        SELECT COUNT(rr) FROM ReporteResenia rr
        WHERE (:estado IS NULL OR rr.estado = :estado)
        """)
    Page<ReporteResenia> findHistorial(
        @Param("estado") EstadoReporteResenia estado,
        Pageable pageable);
}
