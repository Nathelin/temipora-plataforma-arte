package com.temipora.backend_springboot.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.Medio;

public interface MedioRepository extends JpaRepository<Medio, Long> {

    List<Medio> findAllByOrderByNombreAsc();

    List<Medio> findByActivoTrueOrderByNombreAsc();

    Optional<Medio> findByIdMedio(Long idMedio);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdMedioNot(String nombre, Long idMedio);
}
