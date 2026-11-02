package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class EmailPlantillaServiceTest {

    @Test
    void reactivacion_incluyeLaOfertaYEscapaElContenido() {
        EmailPlantillaService service = new EmailPlantillaService();
        ReflectionTestUtils.setField(service, "apiUrl", "http://localhost:8080");

        OfertaEmail oferta = new OfertaEmail(
            7L,
            "Retrato <script>",
            "artista_uno",
            "/uploads/portada.png",
            "1200.00",
            "ARS",
            new BigDecimal("10.00"),
            "Mismo estilo",
            "http://localhost:5173/ofertas/token?oferta=7");

        String html = service.reactivacion(
            "Ana & Cía", List.of(oferta), "token-baja");

        assertThat(html)
            .contains("Ana &amp; Cía")
            .contains("Retrato &lt;script&gt;")
            .contains("10% de descuento")
            .contains("http://localhost:8080/uploads/portada.png")
            .contains("http://localhost:5173/ofertas/token?oferta=7")
            .contains("http://localhost:8080/api/emails/baja/token-baja");
    }

    @Test
    void bajaConfirmada_devuelveUnaPaginaSimple() {
        EmailPlantillaService service = new EmailPlantillaService();

        String html = service.bajaConfirmada("Ana");

        assertThat(html)
            .contains("Damos de baja las promociones")
            .contains("Ana");
    }
}
