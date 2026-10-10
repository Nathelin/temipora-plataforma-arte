package com.temipora.backend_springboot.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.Pais;

public interface PaisRepository extends JpaRepository<Pais, Long> {

    List<Pais> findAllByOrderByNombreAsc();

    List<Pais> findByEstadoTrueOrderByNombreAsc();

    Optional<Pais> findByIdPais(Long idPais);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdPaisNot(String nombre, Long idPais);

    boolean existsByCodigoIsoIgnoreCase(String codigoIso);

    boolean existsByCodigoIsoIgnoreCaseAndIdPaisNot(String codigoIso, Long idPais);
}
