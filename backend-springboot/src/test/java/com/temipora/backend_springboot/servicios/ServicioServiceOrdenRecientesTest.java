package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.temipora.backend_springboot.modelo.Servicio;

/**
 * Orden "Más recientes" del catálogo.
 *
 * La fecha vive en la propia tabla servicio desde V114; antes solo existía en
 * la auditoría (evento_auditoria, acción SERVICIO_CREAR).
 */
class ServicioServiceOrdenRecientesTest {

    @Test
    void recientes_ordenaDeMasNuevoAMasViejo() {
        Servicio viejo = servicio(1L, LocalDateTime.of(2024, 1, 10, 9, 0));
        Servicio medio = servicio(2L, LocalDateTime.of(2025, 6, 1, 12, 0));
        Servicio nuevo = servicio(3L, LocalDateTime.of(2026, 3, 15, 18, 0));

        List<Servicio> ordenados = new ArrayList<>(List.of(medio, nuevo, viejo));
        ordenados.sort(ServicioService.comparadorRecientes());

        assertThat(ordenados)
            .extracting(Servicio::getIdServicio)
            .containsExactly(3L, 2L, 1L);
    }

    @Test
    void recientes_conLaMismaFecha_desempataPorIdDescendente() {
        LocalDateTime momento = LocalDateTime.of(2025, 5, 1, 10, 0);
        List<Servicio> ordenados = new ArrayList<>(List.of(
            servicio(1L, momento),
            servicio(3L, momento),
            servicio(2L, momento)));

        ordenados.sort(ServicioService.comparadorRecientes());

        assertThat(ordenados)
            .extracting(Servicio::getIdServicio)
            .containsExactly(3L, 2L, 1L);
    }

    @Test
    void recientes_sinFecha_dejaEsosServiciosAlFinal() {
        Servicio conFecha = servicio(1L, LocalDateTime.of(2024, 1, 1, 0, 0));
        Servicio sinFecha = servicio(2L, null);

        List<Servicio> ordenados = new ArrayList<>(List.of(sinFecha, conFecha));
        ordenados.sort(ServicioService.comparadorRecientes());

        // nullsFirst + reversed: los nulos caen al final del descendente.
        assertThat(ordenados)
            .extracting(Servicio::getIdServicio)
            .containsExactly(1L, 2L);
    }

    private Servicio servicio(Long id, LocalDateTime fechaCreacion) {
        Servicio s = new Servicio();
        s.setIdServicio(id);
        s.setFechaCreacion(fechaCreacion);
        return s;
    }
}
