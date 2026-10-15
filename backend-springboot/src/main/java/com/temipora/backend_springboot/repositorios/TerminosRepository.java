package com.temipora.backend_springboot.repositorios;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.Terminos;

public interface TerminosRepository extends JpaRepository<Terminos, Long> {

    Optional<Terminos> findByIdTerminos(Long idTerminos);

    Optional<Terminos> findByArtistaIdUsuarioAndIdProductoIsNullAndIdServicioIsNull(Long idUsuario);
}
