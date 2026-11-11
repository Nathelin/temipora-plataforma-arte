package com.temipora.backend_springboot.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.ConfiguracionRecomendacionProducto;

public interface ConfiguracionRecomendacionProductoRepository
        extends JpaRepository<ConfiguracionRecomendacionProducto, Long> {
}
