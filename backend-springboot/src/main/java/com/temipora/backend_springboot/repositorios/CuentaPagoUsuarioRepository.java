package com.temipora.backend_springboot.repositorios;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.CuentaPagoUsuario;

public interface CuentaPagoUsuarioRepository
        extends JpaRepository<CuentaPagoUsuario, Long> {

    Optional<CuentaPagoUsuario> findByUsuarioIdUsuarioAndProveedor(
        Long idUsuario, String proveedor);
}
