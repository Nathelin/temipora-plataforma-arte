package com.temipora.backend_springboot.servicios;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.ConfiguracionRecomendacion;
import com.temipora.backend_springboot.repositorios.ConfiguracionRecomendacionRepository;

@Service
public class ConfiguracionRecomendacionService {

    private final ConfiguracionRecomendacionRepository repository;

    public ConfiguracionRecomendacionService(
            ConfiguracionRecomendacionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ConfiguracionRecomendacion obtener() {
        return repository.findById(ConfiguracionRecomendacion.ID_UNICO)
            .orElseGet(() -> {
                ConfiguracionRecomendacion config = new ConfiguracionRecomendacion();
                config.setIdConfiguracion(ConfiguracionRecomendacion.ID_UNICO);
                return repository.save(config);
            });
    }
}
