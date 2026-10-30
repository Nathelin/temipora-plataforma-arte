package com.temipora.backend_springboot.servicios.pagos.mercadopago;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;

import org.junit.jupiter.api.Test;

class CifradorTokensTest {

    private static final String CLAVE = Base64.getEncoder()
        .encodeToString(new byte[32]);

    @Test
    void cifrarYDescifrar_hacenRoundTrip() {
        CifradorTokens cifrador = new CifradorTokens(CLAVE);

        String cifrado = cifrador.cifrar("access-token-secreto");

        assertThat(cifrado).isNotEqualTo("access-token-secreto");
        assertThat(cifrador.descifrar(cifrado))
            .isEqualTo("access-token-secreto");
    }

    @Test
    void dosCifradosDelMismoValor_sonDistintos() {
        CifradorTokens cifrador = new CifradorTokens(CLAVE);

        assertThat(cifrador.cifrar("token"))
            .isNotEqualTo(cifrador.cifrar("token"));
    }

    @Test
    void sinClave_lanza() {
        CifradorTokens cifrador = new CifradorTokens("");

        assertThatThrownBy(() -> cifrador.cifrar("token"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("PAGOS_TOKEN_KEY");
    }

    @Test
    void claveDeTamanoIncorrecto_lanza() {
        CifradorTokens cifrador = new CifradorTokens(
            Base64.getEncoder().encodeToString(new byte[10]));

        assertThatThrownBy(() -> cifrador.cifrar("token"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("32 bytes");
    }

    @Test
    void valorAlterado_noSePuedeDescifrar() {
        CifradorTokens cifrador = new CifradorTokens(CLAVE);
        String cifrado = cifrador.cifrar("token");
        String alterado = cifrado.substring(0, cifrado.length() - 2) + "AA";

        assertThatThrownBy(() -> cifrador.descifrar(alterado))
            .isInstanceOf(IllegalStateException.class);
    }
}
