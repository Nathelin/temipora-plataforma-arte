package com.temipora.backend_springboot.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.TemaTermino;

public interface TemaTerminoRepository extends JpaRepository<TemaTermino, Long> {

    List<TemaTermino> findByActivoTrueOrderByOrdenAsc();

    Optional<TemaTermino> findByIdTemaTermino(Long idTemaTermino);
}
