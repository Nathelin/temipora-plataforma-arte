package com.temipora.backend_springboot.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.ConfiguracionComision;

public interface ConfiguracionComisionRepository
        extends JpaRepository<ConfiguracionComision, Long> {
}
