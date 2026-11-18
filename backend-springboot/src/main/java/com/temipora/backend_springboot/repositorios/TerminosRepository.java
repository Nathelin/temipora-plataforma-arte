package com.temipora.backend_springboot.repositorios;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.Terminos;

public interface TerminosRepository extends JpaRepository<Terminos, Long> {

    Optional<Terminos> findByIdTerminos(Long idTerminos);

    Optional<Terminos> findFirstByArtistaIdUsuarioAndServicioIsNullAndVigenteTrueOrderByVersionDesc(
        Long idUsuario);

    Optional<Terminos> findFirstByArtistaIdUsuarioAndServicioIdServicioAndVigenteTrueOrderByVersionDesc(
        Long idUsuario, Long idServicio);

    Optional<Terminos> findFirstByArtistaIdUsuarioAndServicioIsNullOrderByVersionDesc(
        Long idUsuario);

    Optional<Terminos> findFirstByArtistaIdUsuarioAndServicioIdServicioOrderByVersionDesc(
        Long idUsuario, Long idServicio);
}
