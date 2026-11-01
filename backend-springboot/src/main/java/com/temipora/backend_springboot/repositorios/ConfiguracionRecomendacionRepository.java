package com.temipora.backend_springboot.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.ConfiguracionRecomendacion;

public interface ConfiguracionRecomendacionRepository
        extends JpaRepository<ConfiguracionRecomendacion, Long> {
}
