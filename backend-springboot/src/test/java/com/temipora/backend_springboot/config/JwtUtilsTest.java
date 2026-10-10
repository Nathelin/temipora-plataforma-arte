package com.temipora.backend_springboot.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.JwtException;

class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "key", claveBase64("otra-clave-secreta-distinta-1234"));
        ReflectionTestUtils.setField(jwtUtils, "vencimientoSesion", "3600000"); // 1 hora
    }

    @Test
    void generarTokenAcceso_tokenValidoConSubjectYRol() {
        String token = jwtUtils.generarTokenAcceso("Cosmiq", "ADMIN");

        assertThat(jwtUtils.tokenEsValido(token)).isTrue();
        assertThat(jwtUtils.obtenerNombreUsuarioToken(token)).isEqualTo("Cosmiq");
        assertThat(jwtUtils.obtenerRolToken(token)).isEqualTo("ADMIN");
    }

    @Test
    void tokenEsValido_tokenBasura_devuelveFalse() {
        assertThat(jwtUtils.tokenEsValido("esto-no-es-un-token")).isFalse();
        assertThat(jwtUtils.tokenEsValido("")).isFalse();
        assertThat(jwtUtils.tokenEsValido(null)).isFalse();
    }

    @Test
    void tokenEsValido_firmadoConOtraClave_devuelveFalse() {
        JwtUtils otroJwt = new JwtUtils();
        ReflectionTestUtils.setField(otroJwt, "key", claveBase64("clave-completamente-diferente-999"));
        ReflectionTestUtils.setField(otroJwt, "vencimientoSesion", "3600000");
        String tokenDeOtro = otroJwt.generarTokenAcceso("Cosmiq", "ADMIN");

        assertThat(jwtUtils.tokenEsValido(tokenDeOtro)).isFalse();
    }

    @Test
    void tokenEsValido_tokenExpirado_devuelveFalse() {
        ReflectionTestUtils.setField(jwtUtils, "vencimientoSesion", "-1000"); // expiró hace 1s
        String tokenExpirado = jwtUtils.generarTokenAcceso("Cosmiq", "ADMIN");

        assertThat(jwtUtils.tokenEsValido(tokenExpirado)).isFalse();
    }

    @Test
    void obtenerNombreUsuarioToken_tokenInvalido_lanzaJwtException() {
        assertThatThrownBy(() -> jwtUtils.obtenerNombreUsuarioToken("token-basura"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void obtenerClaimsToken_tokenValido_devuelveClaims() {
        String token = jwtUtils.generarTokenAcceso("Artista777", "ARTISTA");

        assertThat(jwtUtils.obtenerClaimsToken(token).getSubject()).isEqualTo("Artista777");
        assertThat(jwtUtils.obtenerClaimsToken(token).get("rol", String.class)).isEqualTo("ARTISTA");
    }

    private String claveBase64(String texto) {
        return Base64.getEncoder().encodeToString(texto.getBytes(StandardCharsets.UTF_8));
    }
}
