package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import com.temipora.backend_springboot.modelo.AlcanceSancion;

class EmailPlantillaServiceSancionTest {

    private EmailPlantillaService service;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver =
            new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        service = new EmailPlantillaService(engine);
    }

    @Test
    void sancionEntregas_incluyePedidoMotivoYConsecuencia() {
        String html = service.sancion("ArtistaMoroso",
            new EmailPlantillaService.SancionEmail(
                AlcanceSancion.ENTREGAS,
                31L,
                "Retrato angelical",
                "Cliente2",
                "No entregó 'Seña' del pedido #31 en el plazo previsto.",
                null));

        assertThat(html)
            .contains("Tu cuenta quedó limitada a entregas")
            .contains("#31")
            .contains("Retrato angelical")
            .contains("Cliente2")
            .contains("No entregó &#39;Seña&#39; del pedido")
            .contains("Solo podés subir los entregables pendientes")
            .contains("/cuenta/restringida")
            .contains("Subir entrega");
    }

    @Test
    void sancionEntregas_conToken_incluyeMagicLink() {
        String html = service.sancion("ArtistaMoroso",
            new EmailPlantillaService.SancionEmail(
                AlcanceSancion.ENTREGAS,
                31L,
                "Retrato angelical",
                "Cliente2",
                "No entregó la seña a tiempo.",
                null),
            "token-abc");

        assertThat(html)
            .contains("/cuenta/restringida?acceso=token-abc");
    }

    @Test
    void sancionAcceso_incluyeBloqueoYContactoSoporte() {
        String html = service.sancion("ArtistaMoroso",
            new EmailPlantillaService.SancionEmail(
                AlcanceSancion.ACCESO,
                31L,
                "Retrato angelical",
                "Cliente2",
                "No regularizó la entrega pendiente.",
                null));

        assertThat(html)
            .contains("Tu cuenta quedó inhabilitada")
            .contains("No podés iniciar sesión ni usar la plataforma")
            .contains("Escribir a soporte");
    }

    @Test
    void sancionVentas_incluyeFechaFinYEnlaceAlPedido() {
        String html = service.sancion("ArtistaMoroso",
            new EmailPlantillaService.SancionEmail(
                AlcanceSancion.VENTAS,
                31L,
                "Retrato angelical",
                "Cliente2",
                "No entregó 'Seña' del pedido #31 en el plazo previsto.",
                LocalDateTime.of(2026, 12, 9, 10, 0)));

        assertThat(html)
            .contains("Tus ventas quedaron suspendidas")
            .contains("09/12/2026")
            .contains("/panel/pedidos/31")
            .contains("Ver mis pedidos");
    }
}
