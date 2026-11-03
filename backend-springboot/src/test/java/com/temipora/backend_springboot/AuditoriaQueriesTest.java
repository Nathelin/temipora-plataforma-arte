package com.temipora.backend_springboot;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.temipora.backend_springboot.modelo.FiltroAuditoria;
import com.temipora.backend_springboot.servicios.AuditoriaService;

@SpringBootTest
class AuditoriaQueriesTest {

    @Autowired
    private AuditoriaService auditoriaService;

    @Test
    void listar_conTodosLosFiltrosNoFalla() {
        FiltroAuditoria filtro = new FiltroAuditoria(
            "admin",
            "ADMIN",
            "Pedido",
            "1",
            null,
            null,
            "pedido",
            "COMERCIO",
            true,
            LocalDateTime.now().minusDays(7),
            LocalDateTime.now());

        assertThat(auditoriaService.listar(filtro, 0, 10)).isNotNull();
    }

    @Test
    void resumenNoFalla() {
        assertThat(auditoriaService.resumen(
            LocalDateTime.now().minusDays(1), LocalDateTime.now()))
            .isNotNull();
    }
}
