package com.temipora.backend_springboot;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.VentaDTO;
import com.temipora.backend_springboot.servicios.VentasService;

@SpringBootTest
class VentasQueriesTest {

    @Autowired
    private VentasService ventasService;

    @Test
    void sinFiltrosNoFalla() {
        PaginaDTO<VentaDTO> pagina = ventasService.listar(
            1L, null, null, null, 0, 10);

        assertThat(pagina).isNotNull();
        assertThat(pagina.getContenido()).isNotNull();
    }

    @Test
    void conFiltrosNoFalla() {
        PaginaDTO<VentaDTO> pagina = ventasService.listar(
            1L, "APROBADO", "2026-11", 1L, 0, 10);

        assertThat(pagina).isNotNull();
        assertThat(pagina.getContenido()).isNotNull();
    }

    @Test
    void conEstadoPendienteNoFalla() {
        PaginaDTO<VentaDTO> pagina = ventasService.listar(
            1L, "PENDIENTE", null, null, 0, 10);

        assertThat(pagina).isNotNull();
    }
}
