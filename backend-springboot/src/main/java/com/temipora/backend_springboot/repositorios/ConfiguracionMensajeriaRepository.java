package com.temipora.backend_springboot.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.ConfiguracionMensajeria;

public interface ConfiguracionMensajeriaRepository
        extends JpaRepository<ConfiguracionMensajeria, Long> {
}
