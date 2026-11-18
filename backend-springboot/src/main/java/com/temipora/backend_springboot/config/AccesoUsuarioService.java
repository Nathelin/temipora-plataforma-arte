package com.temipora.backend_springboot.config;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Verificación de acceso para una sesión ya emitida (token JWT o WebSocket):
 * bloquea cuentas desactivadas y con sanción vigente.
 */
public interface AccesoUsuarioService {

    UserDetails loadUserForSession(String nombreUsuario) throws UsernameNotFoundException;
}
