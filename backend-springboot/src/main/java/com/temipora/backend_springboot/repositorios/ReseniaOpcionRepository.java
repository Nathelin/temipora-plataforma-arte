package com.temipora.backend_springboot.repositorios;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.ReseniaOpcion;

public interface ReseniaOpcionRepository
        extends JpaRepository<ReseniaOpcion, Long> {

    @EntityGraph(attributePaths = { "opcion", "opcion.criterio" })
    List<ReseniaOpcion> findByVersionIdVersionIn(Collection<Long> idsVersion);

    boolean existsByOpcionCriterioIdCriterioResenia(Long idCriterio);

    boolean existsByOpcionIdOpcionCriterio(Long idOpcion);

    @Query("""
        SELECT COUNT(DISTINCT ro.version.resenia.idResenia)
        FROM ReseniaOpcion ro
        WHERE ro.opcion.criterio.idCriterioResenia = :idCriterio
        """)
    long contarReseniasPorCriterio(@Param("idCriterio") Long idCriterio);
}
