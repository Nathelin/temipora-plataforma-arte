package com.temipora.backend_springboot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.temipora.backend_springboot.config.filters.TerminosAceptadosFilter;
import com.temipora.backend_springboot.repositorios.AceptacionTerminosPlataformaRepository;
import com.temipora.backend_springboot.repositorios.TerminosPlataformaRepository;

@Configuration
public class TerminosAceptadosFilterConfig {

    @Bean
    public TerminosAceptadosFilter terminosAceptadosFilter(
            AceptacionTerminosPlataformaRepository aceptacionRepository,
            TerminosPlataformaRepository terminosRepository) {
        return new TerminosAceptadosFilter(aceptacionRepository, terminosRepository);
    }
}
