package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.ConfiguracionInactividad;
import com.temipora.backend_springboot.repositorios.ReactivacionRepository;

@ExtendWith(MockitoExtension.class)
class ReactivacionServiceTest {

    @Mock
    private ConfiguracionInactividadService configuracionService;

    @Mock
    private ReactivacionRepository reactivacionRepository;

    @Mock
    private ServicioService servicioService;

    @Mock
    private ReactivacionProcesador procesador;

    @Mock
    private AuditoriaService auditoria;

    @InjectMocks
    private ReactivacionService service;

    @Test
    void antesDeLaHora_noCorresponde() {
        ConfiguracionInactividad config =
            config(LocalTime.of(9, 30), null);

        assertThat(service.correspondeEjecutar(
                config, LocalDateTime.of(2026, 11, 4, 9, 29)))
            .isFalse();
    }

    @Test
    void aLaHoraCorresponde() {
        ConfiguracionInactividad config =
            config(LocalTime.of(9, 30), null);

        assertThat(service.correspondeEjecutar(
                config, LocalDateTime.of(2026, 11, 4, 9, 30)))
            .isTrue();
    }

    @Test
    void despuesDeLaHoraSinCorridaCorresponde() {
        ConfiguracionInactividad config =
            config(LocalTime.of(9, 30), null);

        assertThat(service.correspondeEjecutar(
                config, LocalDateTime.of(2026, 11, 4, 10, 0)))
            .isTrue();
    }

    @Test
    void yaCorrioHoyNoCorresponde() {
        ConfiguracionInactividad config = config(
            LocalTime.of(9, 30),
            LocalDateTime.of(2026, 11, 4, 9, 30));

        assertThat(service.correspondeEjecutar(
                config, LocalDateTime.of(2026, 11, 4, 10, 0)))
            .isFalse();
    }

    @Test
    void corrioAntesDeLaHoraNuevaCorresponde() {
        ConfiguracionInactividad config = config(
            LocalTime.of(10, 0),
            LocalDateTime.of(2026, 11, 4, 9, 0));

        assertThat(service.correspondeEjecutar(
                config, LocalDateTime.of(2026, 11, 4, 10, 0)))
            .isTrue();
    }

    @Test
    void corrioDespuesDeLaHoraNuevaNoCorresponde() {
        ConfiguracionInactividad config = config(
            LocalTime.of(10, 0),
            LocalDateTime.of(2026, 11, 4, 11, 0));

        assertThat(service.correspondeEjecutar(
                config, LocalDateTime.of(2026, 11, 4, 11, 0)))
            .isFalse();
    }

    @Test
    void corrioAyerCorresponde() {
        ConfiguracionInactividad config = config(
            LocalTime.of(9, 30),
            LocalDateTime.of(2026, 11, 3, 9, 30));

        assertThat(service.correspondeEjecutar(
                config, LocalDateTime.of(2026, 11, 4, 9, 30)))
            .isTrue();
    }

    @Test
    void sinHoraConfiguradaNoCorresponde() {
        ConfiguracionInactividad config = config(null, null);

        assertThat(service.correspondeEjecutar(
                config, LocalDateTime.of(2026, 11, 4, 9, 30)))
            .isFalse();
    }

    private ConfiguracionInactividad config(
            LocalTime hora,
            LocalDateTime ultimaCorrida) {
        ConfiguracionInactividad config = new ConfiguracionInactividad();
        config.setHoraEjecucion(hora);
        config.setUltimaCorridaProgramada(ultimaCorrida);
        return config;
    }
}
