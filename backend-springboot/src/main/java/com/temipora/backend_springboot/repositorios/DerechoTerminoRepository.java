package com.temipora.backend_springboot.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.DerechoTermino;

public interface DerechoTerminoRepository extends JpaRepository<DerechoTermino, Long> {

    List<DerechoTermino> findByActivoTrueOrderByOrdenAsc();

    Optional<DerechoTermino> findByIdDerechoTermino(Long idDerechoTermino);
}
