package com.temipora.backend_springboot.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.ConfiguracionResenia;

public interface ConfiguracionReseniaRepository
        extends JpaRepository<ConfiguracionResenia, Long> {
}
