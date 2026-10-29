package com.temipora.backend_springboot.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.AmbitoResenia;
import com.temipora.backend_springboot.modelo.CriterioResenia;

public interface CriterioReseniaRepository extends JpaRepository<CriterioResenia, Long> {

    Optional<CriterioResenia> findByIdCriterioResenia(Long idCriterioResenia);

    List<CriterioResenia> findAllByOrderByAmbitoAscOrdenAscNombreAsc();

    List<CriterioResenia> findByActivoTrueAndAmbitoOrderByOrdenAscNombreAsc(
        AmbitoResenia ambito);

    boolean existsByNombreIgnoreCaseAndAmbito(String nombre, AmbitoResenia ambito);

    boolean existsByNombreIgnoreCaseAndAmbitoAndIdCriterioReseniaNot(
        String nombre, AmbitoResenia ambito, Long idCriterioResenia);
}
