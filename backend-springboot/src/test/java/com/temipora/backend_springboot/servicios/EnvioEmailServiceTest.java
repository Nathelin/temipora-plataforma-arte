package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.EnvioEmail;
import com.temipora.backend_springboot.modelo.EstadoEnvioEmail;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.EnvioEmailRepository;

@ExtendWith(MockitoExtension.class)
class EnvioEmailServiceTest {

    @Mock
    private EnvioEmailRepository envioEmailRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EnvioEmailService service;

    @Test
    void enviar_claveYaRegistrada_devuelveElExistenteSinEnviar() {
        EnvioEmail existente = new EnvioEmail();
        when(envioEmailRepository.findByClaveIdempotencia("k"))
            .thenReturn(Optional.of(existente));

        EnvioEmail resultado = service.enviar(
            usuario("a@mail.com"), "REACTIVACION", 1L, "REF",
            "Asunto", "<p>hola</p>", "k");

        assertThat(resultado).isSameAs(existente);
    }

    @Test
    void enviar_usuarioSinCorreo_marcaFallido() {
        guardarDevuelveElMismo();
        when(envioEmailRepository.findByClaveIdempotencia("k"))
            .thenReturn(Optional.empty());

        EnvioEmail resultado = service.enviar(
            usuario("   "), "T", 1L, "REF", "Asunto", "<p>", "k");

        assertThat(resultado.getEstado()).isEqualTo(EstadoEnvioEmail.FALLIDO);
        assertThat(resultado.getError()).contains("correo");
        assertThat(resultado.getDestinatario()).isBlank();
    }

    @Test
    void enviar_correoNulo_marcaFallido() {
        guardarDevuelveElMismo();
        when(envioEmailRepository.findByClaveIdempotencia("k"))
            .thenReturn(Optional.empty());

        EnvioEmail resultado = service.enviar(
            usuario(null), "T", 1L, "REF", "Asunto", "<p>", "k");

        assertThat(resultado.getEstado()).isEqualTo(EstadoEnvioEmail.FALLIDO);
    }

    @Test
    void enviar_proveedorAcepta_marcaEnviado() {
        guardarDevuelveElMismo();
        when(envioEmailRepository.findByClaveIdempotencia("k"))
            .thenReturn(Optional.empty());
        when(emailService.enviar(any(EmailMensaje.class))).thenReturn(true);

        EnvioEmail resultado = service.enviar(
            usuario("a@mail.com"), "T", 1L, "REF", "Asunto", "<p>", "k");

        assertThat(resultado.getEstado()).isEqualTo(EstadoEnvioEmail.ENVIADO);
        assertThat(resultado.getProveedor()).isEqualTo("smtp");
        assertThat(resultado.getFechaEnvio()).isNotNull();
        assertThat(resultado.getError()).isNull();
    }

    @Test
    void enviar_envioDeshabilitado_quedaPendienteConError() {
        guardarDevuelveElMismo();
        when(envioEmailRepository.findByClaveIdempotencia("k"))
            .thenReturn(Optional.empty());
        when(emailService.enviar(any(EmailMensaje.class))).thenReturn(false);

        EnvioEmail resultado = service.enviar(
            usuario("a@mail.com"), "T", 1L, "REF", "Asunto", "<p>", "k");

        assertThat(resultado.getEstado()).isEqualTo(EstadoEnvioEmail.PENDIENTE);
        assertThat(resultado.getError()).contains("deshabilitado");
    }

    @Test
    void enviar_excepcionDelProveedor_marcaFallidoConElMensaje() {
        guardarDevuelveElMismo();
        when(envioEmailRepository.findByClaveIdempotencia("k"))
            .thenReturn(Optional.empty());
        when(emailService.enviar(any(EmailMensaje.class)))
            .thenThrow(new RuntimeException("SMTP caído"));

        EnvioEmail resultado = service.enviar(
            usuario("a@mail.com"), "T", 1L, "REF", "Asunto", "<p>", "k");

        assertThat(resultado.getEstado()).isEqualTo(EstadoEnvioEmail.FALLIDO);
        assertThat(resultado.getError()).isEqualTo("SMTP caído");
    }

    @Test
    void enviar_excepcionSinMensaje_usaErrorDesconocido() {
        guardarDevuelveElMismo();
        when(envioEmailRepository.findByClaveIdempotencia("k"))
            .thenReturn(Optional.empty());
        when(emailService.enviar(any(EmailMensaje.class)))
            .thenThrow(new RuntimeException());

        EnvioEmail resultado = service.enviar(
            usuario("a@mail.com"), "T", 1L, "REF", "Asunto", "<p>", "k");

        assertThat(resultado.getError()).isEqualTo("Error desconocido");
    }

    @Test
    void enviar_excepcionLarga_truncaElErrorA500() {
        guardarDevuelveElMismo();
        when(envioEmailRepository.findByClaveIdempotencia("k"))
            .thenReturn(Optional.empty());
        String largo = "x".repeat(900);
        when(emailService.enviar(any(EmailMensaje.class)))
            .thenThrow(new RuntimeException(largo));

        EnvioEmail resultado = service.enviar(
            usuario("a@mail.com"), "T", 1L, "REF", "Asunto", "<p>", "k");

        assertThat(resultado.getError()).hasSize(500);
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private void guardarDevuelveElMismo() {
        when(envioEmailRepository.save(any(EnvioEmail.class)))
            .thenAnswer(inv -> inv.getArgument(0));
    }

    private Usuario usuario(String correo) {
        return Usuario.builder()
            .idUsuario(1L)
            .nombreUsuario("cosmiq")
            .correo(correo)
            .build();
    }
}
