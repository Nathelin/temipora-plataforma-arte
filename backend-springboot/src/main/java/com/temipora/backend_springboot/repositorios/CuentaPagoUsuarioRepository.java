package com.temipora.backend_springboot.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.CuentaPagoUsuario;
import com.temipora.backend_springboot.modelo.EstadoCuentaPago;

public interface CuentaPagoUsuarioRepository
        extends JpaRepository<CuentaPagoUsuario, Long> {

    Optional<CuentaPagoUsuario> findByUsuarioIdUsuarioAndProveedor(
        Long idUsuario, String proveedor);

    List<CuentaPagoUsuario> findByProveedorAndEstado(
        String proveedor, EstadoCuentaPago estado);
}
