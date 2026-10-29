package com.temipora.backend_springboot.repositorios;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.ReseniaValor;

public interface ReseniaValorRepository
        extends JpaRepository<ReseniaValor, Long> {

    @EntityGraph(attributePaths = "criterio")
    List<ReseniaValor> findByVersionIdVersionIn(Collection<Long> idsVersion);

    boolean existsByCriterioIdCriterioResenia(Long idCriterio);

    @Query("""
        SELECT COUNT(DISTINCT rv.version.resenia.idResenia)
        FROM ReseniaValor rv
        WHERE rv.criterio.idCriterioResenia = :idCriterio
        """)
    long contarReseniasPorCriterio(@Param("idCriterio") Long idCriterio);
}
