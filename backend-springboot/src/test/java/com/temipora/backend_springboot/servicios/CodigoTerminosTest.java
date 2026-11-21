package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CodigoTerminosTest {

    private static final String HUELLA =
        "b00994ee58432ff1a76c14fc395e51da35e9a4ebd68e0803e25a8351b87d4320";

    @Test
    void codificaYDecodifica() {
        String codigo = CodigoTerminos.codificar(34L, HUELLA);

        assertThat(codigo).isEqualTo("TA-Y-b00994ee58432ff1");

        CodigoTerminos.Datos datos = CodigoTerminos.decodificar(codigo)
            .orElseThrow();
        assertThat(datos.idSolicitud()).isEqualTo(34L);
        assertThat(datos.huella()).isEqualTo("b00994ee58432ff1");
    }

    @Test
    void aceptaElCodigoEnMinusculas() {
        assertThat(CodigoTerminos.decodificar("ta-y-b00994ee58432ff1"))
            .isPresent();
    }

    @Test
    void rechazaCodigosInvalidos() {
        assertThat(CodigoTerminos.decodificar(null)).isEmpty();
        assertThat(CodigoTerminos.decodificar("TP-Y-b00994ee58432ff1"))
            .isEmpty();
        assertThat(CodigoTerminos.decodificar("TA-Y-b00994ee58432ff"))
            .isEmpty();
        assertThat(CodigoTerminos.decodificar("TA-Y-zzzzzzzzzzzzzzzz"))
            .isEmpty();
        assertThat(CodigoTerminos.decodificar("TA--b00994ee58432ff1"))
            .isEmpty();
        assertThat(CodigoTerminos.decodificar("TA-Y")).isEmpty();
    }

    @Test
    void exigeSolicitudYHuellaSuficiente() {
        assertThatThrownBy(() -> CodigoTerminos.codificar(null, HUELLA))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CodigoTerminos.codificar(1L, "abc"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
