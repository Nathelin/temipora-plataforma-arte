package com.temipora.backend_springboot.config;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;

import com.temipora.backend_springboot.servicios.AccionesAdmin;
import com.temipora.backend_springboot.servicios.AuditoriaService;

@ExtendWith(MockitoExtension.class)
class LoginFallidoListenerTest {

    @Mock
    private AuditoriaService auditoria;

    @InjectMocks
    private LoginFallidoListener listener;

    @Test
    void registraElUsuarioQueIntentoIngresar() {
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken("artista777", "secreta");

        listener.alFallarAutenticacion(
            new AuthenticationFailureBadCredentialsEvent(
                auth, new BadCredentialsException("credenciales")));

        verify(auditoria).registrarSistema(
            AccionesAdmin.LOGIN_FALLIDO, "Usuario", null,
            "Intento de inicio de sesión fallido para 'artista777'");
    }

    @Test
    void registraSinUsuarioCuandoElBodyEsInvalido() {
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken("", "");

        listener.alFallarAutenticacion(
            new AuthenticationFailureBadCredentialsEvent(
                auth, new BadCredentialsException("credenciales")));

        verify(auditoria).registrarSistema(
            AccionesAdmin.LOGIN_FALLIDO, "Usuario", null,
            "Intento de inicio de sesión fallido (sin usuario)");
    }
}
