package com.temipora.backend_springboot.repositorios;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.AceptacionTerminosPlataforma;

public interface AceptacionTerminosPlataformaRepository
        extends JpaRepository<AceptacionTerminosPlataforma, Long> {

    Optional<AceptacionTerminosPlataforma> findByUsuarioIdUsuarioAndVersion(
        Long idUsuario, Integer version);

    boolean existsByUsuarioNombreUsuarioIgnoreCaseAndVersion(
        String nombreUsuario, Integer version);
}
