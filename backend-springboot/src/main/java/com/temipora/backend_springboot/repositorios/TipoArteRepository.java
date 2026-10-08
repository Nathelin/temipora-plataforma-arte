package com.temipora.backend_springboot.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.TipoArte;

public interface TipoArteRepository extends JpaRepository<TipoArte, Long> {

    List<TipoArte> findAllByOrderByNombreAsc();

    List<TipoArte> findByActivoTrueOrderByNombreAsc();

    Optional<TipoArte> findByIdTipoArte(Long idTipoArte);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdTipoArteNot(String nombre, Long idTipoArte);
}
