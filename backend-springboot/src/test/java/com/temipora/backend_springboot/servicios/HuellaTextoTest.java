package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HuellaTextoTest {

    @Test
    void calculaElSha256EnHexadecimal() {
        assertThat(HuellaTexto.sha256("hola")).isEqualTo(
            "b221d9dbb083a7f33428d7c2a3c3198ae925614d70210e28716ccaa7cd4ddb79");
    }

    @Test
    void cambiaCuandoCambiaElTexto() {
        assertThat(HuellaTexto.sha256("hola"))
            .isNotEqualTo(HuellaTexto.sha256("hola "));
    }

    @Test
    void devuelveNuloSinTexto() {
        assertThat(HuellaTexto.sha256(null)).isNull();
    }
}
