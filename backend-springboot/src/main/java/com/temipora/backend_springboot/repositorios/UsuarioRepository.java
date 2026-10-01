package com.temipora.backend_springboot.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.Usuario;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByIdUsuario(Long idUsuario);
    Optional<Usuario> findByNombreUsuario(String nombreUsuario);
    Optional<Usuario> findByNombreUsuarioIgnoreCase(String nombreUsuario);
    Optional<Usuario> findByCorreo(String correo);
    boolean existsByNombreUsuario(String nombreUsuario);
    boolean existsByNombreUsuarioIgnoreCase(String nombreUsuario);
    boolean existsByRol_Nombre(ERol nombre);

    @Query("""
        SELECT u FROM Usuario u WHERE
        (:nombreUsuario IS NULL OR lower(u.nombreUsuario) LIKE lower(CONCAT('%', :nombreUsuario, '%'))) AND
        (:correo IS NULL OR lower(u.correo) LIKE lower(CONCAT('%', :correo, '%'))) AND
        (:estadoCuenta IS NULL OR u.estadoCuenta = :estadoCuenta) AND
        (:rolNombre IS NULL OR u.rol.nombre = :rolNombre)
        """)
    Page<Usuario> findByFilters(
            @Param("nombreUsuario") String nombreUsuario,
            @Param("correo") String correo,
            @Param("estadoCuenta") Boolean estadoCuenta,
            @Param("rolNombre") ERol rolNombre,
            Pageable pageable);
}