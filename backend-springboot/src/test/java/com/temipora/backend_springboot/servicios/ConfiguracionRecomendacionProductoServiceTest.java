package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.ConfiguracionRecomendacionProducto;
import com.temipora.backend_springboot.repositorios.ConfiguracionRecomendacionProductoRepository;

@ExtendWith(MockitoExtension.class)
class ConfiguracionRecomendacionProductoServiceTest {

    @Mock
    private ConfiguracionRecomendacionProductoRepository repository;

    @InjectMocks
    private ConfiguracionRecomendacionProductoService service;

    @Test
    void obtener_devuelveLaExistente() {
        ConfiguracionRecomendacionProducto config =
            new ConfiguracionRecomendacionProducto();
        when(repository.findById(ConfiguracionRecomendacionProducto.ID_UNICO))
            .thenReturn(Optional.of(config));

        assertThat(service.obtener()).isSameAs(config);
    }

    @Test
    void obtener_creaElSingletonSiFalta() {
        when(repository.findById(ConfiguracionRecomendacionProducto.ID_UNICO))
            .thenReturn(Optional.empty());
        when(repository.save(any(ConfiguracionRecomendacionProducto.class)))
            .thenAnswer(invocacion -> invocacion.getArgument(0));

        ConfiguracionRecomendacionProducto config = service.obtener();

        assertThat(config.getIdConfiguracion())
            .isEqualTo(ConfiguracionRecomendacionProducto.ID_UNICO);
        verify(repository).save(config);
    }
}
