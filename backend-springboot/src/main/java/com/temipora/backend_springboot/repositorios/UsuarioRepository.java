package com.temipora.backend_springboot.repositorios;


import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.Usuario;

import java.util.Optional;


public interface UsuarioRepository extends JpaRepository<Usuario,Long>{
    Optional <Usuario> findByIdUsuario(Long idUsuario);
    Optional <Usuario> findBynombreUsuario(String nombreUsuario);

    Optional<Usuario> findByNombreUsuarioIgnoreCase(String nombreUsuario);

    Optional <Usuario> findBycorreo(String correo);

    boolean existsByNombreUsuario(String nombreUsuario);

    boolean existsByNombreUsuarioIgnoreCase(String nombreUsuario); // ignoreCase es una keyword

    boolean existsByRol_Nombre(ERol nombre);
}
