package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NombrePiezaVisibleTest {

    @Test
    void productoConVariantePropiaMuestraElNombreDeLaVariante() {
        assertThat(NombrePiezaVisible.deReventa("Roja", "Remera"))
            .isEqualTo("Roja");
        assertThat(NombrePiezaVisible.nombreDeVariante("Roja")).isEqualTo("Roja");
    }

    @Test
    void productoSinVarianteMuestraElTituloDelProducto() {
        assertThat(NombrePiezaVisible.deReventa("Única", "Remera"))
            .isEqualTo("Remera");
        assertThat(NombrePiezaVisible.nombreDeVariante("Única")).isNull();
    }

    @Test
    void reconoceLaPresentacionUnicaSinDistinguirMayusculasNiAcentos() {
        assertThat(NombrePiezaVisible.deReventa("única", "Remera"))
            .isEqualTo("Remera");
        assertThat(NombrePiezaVisible.deReventa("UNICA", "Remera"))
            .isEqualTo("Remera");
        assertThat(NombrePiezaVisible.deReventa("  ", "Remera"))
            .isEqualTo("Remera");
        assertThat(NombrePiezaVisible.deReventa(null, "Remera"))
            .isEqualTo("Remera");
    }

    @Test
    void recortaEspaciosDelNombrePropio() {
        assertThat(NombrePiezaVisible.deReventa("  Roja  ", "Remera"))
            .isEqualTo("Roja");
    }
}
