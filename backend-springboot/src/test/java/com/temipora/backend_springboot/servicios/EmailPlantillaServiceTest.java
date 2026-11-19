package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class EmailPlantillaServiceTest {

    private EmailPlantillaService service;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        service = new EmailPlantillaService(engine);
        ReflectionTestUtils.setField(service, "apiUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(service, "frontUrl", "http://localhost:5173");
    }

    @Test
    void reactivacion_incluyeLaOfertaYEscapaElContenido() {
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
            .contains("1.080 ARS")
            .contains("1.200 ARS")
            .contains("http://localhost:8080/uploads/portada.png")
            .contains("http://localhost:5173/ofertas/token?oferta=7")
            .contains("http://localhost:8080/api/emails/baja/token-baja")
            .contains("Tenés ofertas con descuento esperándote en Temipora.");
    }

    @Test
    void reactivacion_sinImagen_muestraLaInicial() {
        OfertaEmail oferta = new OfertaEmail(
            8L,
            "Ilustración digital",
            "taller.norte",
            null,
            "32000",
            "ARS",
            new BigDecimal("10.00"),
            null,
            "http://localhost:5173/ofertas/demo");

        String html = service.reactivacion(
            "Ana", List.of(oferta), "token-baja");

        assertThat(html)
            .contains(">I<")
            .doesNotContain("uploads/undefined");
    }

    @Test
    void cierre_llevaLasAlternativasAlDetalleDelServicio() {
        EmailPlantillaService.CierreEmail cierre =
            new EmailPlantillaService.CierreEmail(
                "SOLICITUD",
                5L,
                null,
                "Retrato",
                "artista",
                null,
                "El artista rechazó tu solicitud",
                "Podés buscar otro artista.",
                LocalDateTime.of(2026, 11, 6, 10, 0));
        EmailPlantillaService.AlternativaEmail alternativa =
            new EmailPlantillaService.AlternativaEmail(
                11L, 99L, "Otro retrato", "otro", null, "120.00", "ARS",
                "Mismo estilo",
                List.of("Mismo tipo: Retrato", "Precio similar"));

        String html = service.cierre(
            "Ana", List.of(cierre), List.of(alternativa), "token-cierre");

        assertThat(html)
            .contains("http://localhost:5173/solicitudes/5/resultado?acceso=token-cierre")
            .contains("http://localhost:5173/servicios/11?acceso=token-cierre&amp;sustitucion=99")
            .contains("Por qué te la recomendamos")
            .contains("Mismo estilo")
            .contains("Mismo tipo: Retrato")
            .contains("Precio similar")
            .contains("Ver y solicitar");
    }

    @Test
    void bajaConfirmada_devuelveUnaPaginaSimple() {
        String html = service.bajaConfirmada("Ana & Cía");

        assertThat(html)
            .contains("Damos de baja las promociones")
            .contains("Ana &amp; Cía");
    }

    @Test
    void bajaInvalida_devuelveUnaPaginaSimple() {
        String html = service.bajaInvalida();

        assertThat(html)
            .contains("No pudimos procesar la baja");
    }
}
