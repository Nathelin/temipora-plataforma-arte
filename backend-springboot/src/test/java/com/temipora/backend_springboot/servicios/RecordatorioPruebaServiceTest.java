package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import com.temipora.backend_springboot.modelo.EnvioEmail;
import com.temipora.backend_springboot.modelo.EstadoEnvioEmail;
import com.temipora.backend_springboot.modelo.Usuario;

@ExtendWith(MockitoExtension.class)
class RecordatorioPruebaServiceTest {

    @Mock
    private EnvioEmailService envioEmailService;
    @Mock
    private AccesoEnlaceService accesoEnlaceService;

    private RecordatorioPruebaService service;
    private Usuario admin;

    @BeforeEach
    void setUp() {
        service = new RecordatorioPruebaService(
            new EmailPlantillaService(engine()),
            envioEmailService,
            accesoEnlaceService);
        admin = Usuario.builder()
            .idUsuario(1L)
            .nombreUsuario("admin")
            .nombreDisplay("Admin")
            .correo("admin@test.local")
            .estadoCuenta(true)
            .build();
    }

    @Test
    void enviar_mandaUnMailDePagosYOtraDeEntregas() {
        when(accesoEnlaceService.generar(any(), any(), any(), anyInt()))
            .thenReturn("tok");
        EnvioEmail envio = new EnvioEmail();
        envio.setEstado(EstadoEnvioEmail.ENVIADO);
        when(envioEmailService.enviar(
            any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(envio);

        int enviados = service.enviar(admin);

        assertThat(enviados).isEqualTo(2);
        ArgumentCaptor<String> asuntos = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> htmls = ArgumentCaptor.forClass(String.class);
        verify(envioEmailService, times(2)).enviar(
            eq(admin), eq("RECORDATORIO_PRUEBA"), isNull(), isNull(),
            asuntos.capture(), htmls.capture(), anyString());

        List<String> asunto = asuntos.getAllValues();
        List<String> html = htmls.getAllValues();
        assertThat(asunto.get(0)).contains("pagos pendientes");
        assertThat(html.get(0)).contains("Pago pendiente");
        assertThat(html.get(0)).contains("Cuota 2");
        assertThat(html.get(0)).doesNotContain("Entrega pendiente");

        assertThat(asunto.get(1)).contains("entrega");
        assertThat(html.get(1)).contains("Entrega pendiente");
        assertThat(html.get(1)).contains("Boceto");
        assertThat(html.get(1)).contains("damos de baja tu cuenta");
        assertThat(html.get(1)).doesNotContain("Pago pendiente");
    }

    @Test
    void enviar_sinCorreo_noEnvia() {
        Usuario sinCorreo = Usuario.builder()
            .idUsuario(2L)
            .nombreUsuario("admin2")
            .build();

        assertThat(service.enviar(sinCorreo)).isZero();
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
