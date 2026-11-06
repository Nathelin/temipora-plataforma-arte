package com.temipora.backend_springboot;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import com.temipora.backend_springboot.modelo.EstadoSustitucion;
import com.temipora.backend_springboot.modelo.MotivoSustitucion;
import com.temipora.backend_springboot.repositorios.SustitucionRepository;

@SpringBootTest
class SustitucionQueriesTest {

    @Autowired
    private SustitucionRepository sustitucionRepository;

    @Test
    void recientes_usuarioInexistente_devuelveVacio() {
        assertThat(sustitucionRepository.recientes(
            -1L,
            -1L,
            MotivoSustitucion.SIN_CUPOS,
            EstadoSustitucion.ABIERTA,
            LocalDateTime.now().minusDays(1),
            PageRequest.of(0, 1))).isEmpty();
    }

    @Test
    void contarPorEstado_esValido() {
        assertThat(sustitucionRepository.countByEstado(EstadoSustitucion.ABIERTA))
            .isGreaterThanOrEqualTo(0L);
    }

    @Test
    void origenesFrecuentes_esValido() {
        assertThat(sustitucionRepository.origenesFrecuentes(PageRequest.of(0, 5)))
            .isNotNull();
    }
}
