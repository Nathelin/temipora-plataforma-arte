package com.temipora.backend_springboot.repositorios;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.Formulario;

public interface FormularioRepository extends JpaRepository<Formulario, Long> {

    Optional<Formulario> findByIdFormulario(Long idFormulario);

    Optional<Formulario> findByArtistaIdUsuarioAndIdServicioIsNullAndEsSistemaFalseAndVigenteTrue(
        Long idUsuario);

    Optional<Formulario> findByArtistaIdUsuarioAndIdServicioAndVigenteTrue(
        Long idUsuario, Long idServicio);

    Optional<Formulario> findFirstByEsSistemaTrue();
}
