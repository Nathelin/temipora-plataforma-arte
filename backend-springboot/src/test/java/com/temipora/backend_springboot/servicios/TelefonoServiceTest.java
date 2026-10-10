package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TelefonoServiceTest {

    private final TelefonoService telefonoService = new TelefonoService();

    @Test
    void numeroValidoConRegion_normalizaAE164() {
        assertThat(telefonoService.validarYNormalizar("650-253-0000", "US"))
                .isEqualTo("+16502530000");
    }

    @Test
    void numeroInternacionalSinRegion_normalizaAE164() {
        assertThat(telefonoService.validarYNormalizar("+1 650 253 0000", null))
                .isEqualTo("+16502530000");
    }

    @Test
    void numeroInvalidoParaElPais_lanzaError() {
        assertThatThrownBy(() -> telefonoService.validarYNormalizar("123", "US"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void numeroSinRegionNiPrefijo_lanzaError() {
        assertThatThrownBy(() -> telefonoService.validarYNormalizar("6502530000", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void numeroVacio_lanzaError() {
        assertThatThrownBy(() -> telefonoService.validarYNormalizar("   ", "US"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El teléfono no puede estar vacío.");
    }
}
