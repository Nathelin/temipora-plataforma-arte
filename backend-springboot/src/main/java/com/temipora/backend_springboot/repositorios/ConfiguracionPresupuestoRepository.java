package com.temipora.backend_springboot.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.ConfiguracionPresupuesto;

public interface ConfiguracionPresupuestoRepository
        extends JpaRepository<ConfiguracionPresupuesto, Long> {
}
