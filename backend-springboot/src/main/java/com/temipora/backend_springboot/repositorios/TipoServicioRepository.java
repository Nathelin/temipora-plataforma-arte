package com.temipora.backend_springboot.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.TipoServicio;

public interface TipoServicioRepository extends JpaRepository<TipoServicio, Long> {

    List<TipoServicio> findAllByOrderByNombreAsc();

    List<TipoServicio> findByActivoTrueOrderByNombreAsc();

    Optional<TipoServicio> findByIdTipoServicio(Long idTipoServicio);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdTipoServicioNot(String nombre, Long idTipoServicio);
}