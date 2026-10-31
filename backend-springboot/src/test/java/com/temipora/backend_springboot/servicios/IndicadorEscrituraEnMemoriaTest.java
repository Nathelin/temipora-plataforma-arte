package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class IndicadorEscrituraEnMemoriaTest {

    @Test
    void marcarYConsultar_devuelveTrueSoloParaEseUsuario() {
        IndicadorEscrituraEnMemoria indicador = new IndicadorEscrituraEnMemoria();

        indicador.marcar(10L, 2L);

        assertThat(indicador.estaEscribiendo(10L, 2L)).isTrue();
        assertThat(indicador.estaEscribiendo(10L, 1L)).isFalse();
        assertThat(indicador.estaEscribiendo(11L, 2L)).isFalse();
    }

    @Test
    void limpiar_apagaElIndicador() {
        IndicadorEscrituraEnMemoria indicador = new IndicadorEscrituraEnMemoria();
        indicador.marcar(10L, 2L);

        indicador.limpiar(10L, 2L);

        assertThat(indicador.estaEscribiendo(10L, 2L)).isFalse();
    }

    @Test
    void vencerLaVentana_apagaElIndicador() {
        IndicadorEscrituraEnMemoria indicador =
            new IndicadorEscrituraEnMemoria(Duration.ZERO);

        indicador.marcar(10L, 2L);

        assertThat(indicador.estaEscribiendo(10L, 2L)).isFalse();
    }

    @Test
    void limpiarVencidos_noFallaConMapaVacio() {
        new IndicadorEscrituraEnMemoria().limpiarVencidos();
    }
}
