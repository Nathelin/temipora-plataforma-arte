package com.temipora.backend_springboot.servicios.pagos.mercadopago;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MercadoPagoOAuthStateTest {

    private final MercadoPagoOAuthState state =
        new MercadoPagoOAuthState("secreto-de-prueba");

    @Test
    void generarYValidar_devuelvenElUsuario() {
        String generado = state.generar(5L);

        assertThat(state.validar(generado)).isEqualTo(5L);
    }

    @Test
    void stateAlterado_esRechazado() {
        String generado = state.generar(5L);

        assertThatThrownBy(() -> state.validar(generado + "x"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void stateVacio_esRechazado() {
        assertThatThrownBy(() -> state.validar(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void otroSecreto_noValidaElState() {
        MercadoPagoOAuthState otro =
            new MercadoPagoOAuthState("otro-secreto");

        assertThatThrownBy(() -> otro.validar(state.generar(5L)))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
