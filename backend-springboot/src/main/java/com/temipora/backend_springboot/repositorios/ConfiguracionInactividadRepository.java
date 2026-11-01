package com.temipora.backend_springboot.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.ConfiguracionInactividad;

public interface ConfiguracionInactividadRepository
        extends JpaRepository<ConfiguracionInactividad, Long> {
}
