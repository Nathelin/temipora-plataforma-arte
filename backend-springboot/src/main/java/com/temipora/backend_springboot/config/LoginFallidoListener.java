package com.temipora.backend_springboot.config;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.stereotype.Component;

import com.temipora.backend_springboot.servicios.AccionesAdmin;
import com.temipora.backend_springboot.servicios.AuditoriaService;

@Component
public class LoginFallidoListener {

    private final AuditoriaService auditoria;

    public LoginFallidoListener(AuditoriaService auditoria) {
        this.auditoria = auditoria;
    }

    @EventListener
    public void alFallarAutenticacion(AbstractAuthenticationFailureEvent evento) {
        String usuario = evento.getAuthentication() != null
            ? evento.getAuthentication().getName()
            : null;
        auditoria.registrarSistema(
            AccionesAdmin.LOGIN_FALLIDO, "Usuario", null,
            usuario == null || usuario.isBlank()
                ? "Intento de inicio de sesión fallido (sin usuario)"
                : "Intento de inicio de sesión fallido para '" + usuario + "'");
    }
}
