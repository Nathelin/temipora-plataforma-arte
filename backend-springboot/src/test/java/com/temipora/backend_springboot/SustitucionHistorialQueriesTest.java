package com.temipora.backend_springboot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import com.temipora.backend_springboot.modelo.InformeSustitucionesHistorialDTO;
import com.temipora.backend_springboot.modelo.MotivoSustitucion;
import com.temipora.backend_springboot.modelo.Sustitucion;
import com.temipora.backend_springboot.repositorios.SustitucionRepository;
import com.temipora.backend_springboot.servicios.SustitucionHistorialService;

@SpringBootTest
class SustitucionHistorialQueriesTest {

    @Autowired
    private SustitucionRepository sustitucionRepository;

    @Autowired
    private SustitucionHistorialService service;

    @Test
    void consultasDelHistorial_ejecutanSinErrores() {
        LocalDate hasta = LocalDate.now();
        LocalDate desde = hasta.minusDays(30);
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.plusDays(1).atStartOfDay();

        var pagina = sustitucionRepository.buscarHistorial(
            inicio, fin, null, null, null, "",
            MotivoSustitucion.PEDIDO_IMPAGO, PageRequest.of(0, 20));

        assertThat(pagina).isNotNull();
        assertThat(pagina.getContent()).isNotNull();
        assertThat(sustitucionRepository.agruparHistorial(
            inicio, fin, null, null, null, "",
            MotivoSustitucion.PEDIDO_IMPAGO)).isNotNull();

        InformeSustitucionesHistorialDTO informe = service.generar(
            desde.toString(), hasta.toString(), null, null, null, null, 0,
            20);

        assertThat(informe.resumen()).isNotNull();
        assertThat(informe.eventos()).isNotNull();
        assertThat(informe.filtro()).isEqualTo("Todos los eventos");

        if (!informe.eventos().isEmpty()) {
            Long id = informe.eventos().get(0).idSustitucion();
            var detalle = service.detalle(id);
            assertThat(detalle.evento().idSustitucion()).isEqualTo(id);
            assertThat(detalle.opciones()).isNotNull();
        }
    }

    @Test
    void filtrosInvalidos_fallan() {
        assertThatThrownBy(() -> service.generar(
            null, null, null, "NO_EXISTE", null, null, 0, 20))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Estado inválido");
    }

    @Test
    void detalleInexistente_falla() {
        assertThatThrownBy(() -> service.detalle(-1L))
            .isInstanceOf(java.util.NoSuchElementException.class);
    }
}
