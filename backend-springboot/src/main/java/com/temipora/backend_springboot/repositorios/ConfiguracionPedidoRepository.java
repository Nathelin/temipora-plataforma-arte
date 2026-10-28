package com.temipora.backend_springboot.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.ConfiguracionPedido;

public interface ConfiguracionPedidoRepository
        extends JpaRepository<ConfiguracionPedido, Long> {
}
