package com.temipora.backend_springboot.servicios;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.ConfiguracionRecomendacionProducto;
import com.temipora.backend_springboot.repositorios.ConfiguracionRecomendacionProductoRepository;

@Service
public class ConfiguracionRecomendacionProductoService {

    private final ConfiguracionRecomendacionProductoRepository repository;

    public ConfiguracionRecomendacionProductoService(
            ConfiguracionRecomendacionProductoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ConfiguracionRecomendacionProducto obtener() {
        return repository.findById(ConfiguracionRecomendacionProducto.ID_UNICO)
            .orElseGet(() -> {
                ConfiguracionRecomendacionProducto config =
                    new ConfiguracionRecomendacionProducto();
                config.setIdConfiguracion(ConfiguracionRecomendacionProducto.ID_UNICO);
                return repository.save(config);
            });
    }
}
