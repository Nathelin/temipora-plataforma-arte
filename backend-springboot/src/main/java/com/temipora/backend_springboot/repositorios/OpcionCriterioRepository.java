package com.temipora.backend_springboot.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.OpcionCriterio;

public interface OpcionCriterioRepository extends JpaRepository<OpcionCriterio, Long> {

    List<OpcionCriterio> findByCriterioIdCriterioReseniaOrderByOrdenAscEtiquetaAsc(
        Long idCriterio);

    Optional<OpcionCriterio> findByIdOpcionCriterioAndCriterioIdCriterioResenia(
        Long idOpcion, Long idCriterio);

    boolean existsByCriterioIdCriterioReseniaAndEtiquetaIgnoreCase(
        Long idCriterio, String etiqueta);

    boolean existsByCriterioIdCriterioReseniaAndEtiquetaIgnoreCaseAndIdOpcionCriterioNot(
        Long idCriterio, String etiqueta, Long idOpcion);

    long countByCriterioIdCriterioReseniaAndActivoTrue(Long idCriterio);
}
