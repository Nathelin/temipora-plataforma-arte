package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class EdadCalculadoraTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 26);

    @Test
    void cumpleEdadElDiaDelCumpleanios() {
        LocalDate nacimiento = HOY.minusYears(18);

        assertThat(EdadCalculadora.esMayorDeEdad(nacimiento, HOY)).isTrue();
        assertThat(EdadCalculadora.edad(nacimiento, HOY)).isEqualTo(18);
    }

    @Test
    void noCumpleEdadUnDiaAntesDelCumpleanios() {
        LocalDate nacimiento = HOY.minusYears(18).plusDays(1);

        assertThat(EdadCalculadora.esMayorDeEdad(nacimiento, HOY)).isFalse();
        assertThat(EdadCalculadora.edad(nacimiento, HOY)).isEqualTo(17);
    }

    @Test
    void fechaNulaNoCumpleLaEdadMinima() {
        assertThat(EdadCalculadora.esMayorDeEdad(null, HOY)).isFalse();
    }

    @Test
    void fechaFuturaNoCumpleLaEdadMinima() {
        assertThat(EdadCalculadora.esMayorDeEdad(HOY.plusDays(1), HOY)).isFalse();
    }

    @Test
    void personaMuyMayorCumpleLaEdadMinima() {
        assertThat(EdadCalculadora.esMayorDeEdad(LocalDate.of(1950, 5, 20), HOY)).isTrue();
    }
}
