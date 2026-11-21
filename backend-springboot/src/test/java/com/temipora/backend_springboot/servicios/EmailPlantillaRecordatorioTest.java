package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class EmailPlantillaRecordatorioTest {

    @Test
    void recordatorio_renderizaPagosYEntregasConSusEnlaces() {
        EmailPlantillaService servicio = new EmailPlantillaService(engine());
        EmailPlantillaService.RecordatorioEmail pago =
            new EmailPlantillaService.RecordatorioEmail(
                "PAGO", 10L, "Retrato", null, "artista",
                "Cuota 2", "18/12/2026",
                new BigDecimal("1200"), "ARS", true, false);
        EmailPlantillaService.RecordatorioEmail entrega =
            new EmailPlantillaService.RecordatorioEmail(
                "ENTREGA", 11L, "Ilustración", null, "cliente",
                "Boceto", "20/12/2026", null, "ARS", true, false);

        String html = servicio.recordatorio(
            "Ana", List.of(pago), List.of(entrega), "tok");

        assertThat(html).contains("Pago pendiente");
        assertThat(html).contains("Cuota 2");
        assertThat(html).contains("/pedidos/10");
        assertThat(html).contains("Entrega pendiente");
        assertThat(html).contains("Subir entrega");
        assertThat(html).contains("/panel/pedidos/11");
        assertThat(html).contains("URGENTE");
        assertThat(html).contains("1.200");
    }

    private SpringTemplateEngine engine() {
        ClassLoaderTemplateResolver resolver =
            new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
