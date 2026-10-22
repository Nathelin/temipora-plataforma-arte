package com.temipora.backend_springboot.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.ConfiguracionCotizacion;

public interface ConfiguracionCotizacionRepository extends JpaRepository<ConfiguracionCotizacion, Long> {
}
