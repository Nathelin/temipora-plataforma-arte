package com.temipora.backend_springboot.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.TerminosPlataforma;

public interface TerminosPlataformaRepository
        extends JpaRepository<TerminosPlataforma, Long> {

    Optional<TerminosPlataforma> findFirstByVigenteTrueOrderByVersionDesc();

    Optional<TerminosPlataforma> findFirstByOrderByVersionDesc();

    List<TerminosPlataforma> findAllByOrderByVersionDesc();

    List<TerminosPlataforma> findByVigenteTrue();
}
