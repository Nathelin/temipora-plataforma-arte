package com.temipora.backend_springboot;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.temipora.backend_springboot.repositorios.ReactivacionRepository;

@SpringBootTest
class ReactivacionQueriesTest {

    @Autowired
    private ReactivacionRepository reactivacionRepository;

    @Test
    void candidatos_ejecutaLaConsultaSinErrores() {
        LocalDateTime ahora = LocalDateTime.now();

        List<Long> ids = reactivacionRepository.candidatos(
            ahora, ahora, ahora, ahora, 5);

        assertThat(ids).isNotNull();
        assertThat(ids).hasSizeLessThanOrEqualTo(5);
    }
}
