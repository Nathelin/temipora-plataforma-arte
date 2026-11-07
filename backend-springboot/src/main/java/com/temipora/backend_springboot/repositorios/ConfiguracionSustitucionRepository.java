package com.temipora.backend_springboot.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.ConfiguracionSustitucion;

public interface ConfiguracionSustitucionRepository
        extends JpaRepository<ConfiguracionSustitucion, Long> {
}
