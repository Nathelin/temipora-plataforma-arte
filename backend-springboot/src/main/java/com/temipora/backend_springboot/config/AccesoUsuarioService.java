package com.temipora.backend_springboot.config;

import java.util.Optional;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.temipora.backend_springboot.modelo.AlcanceSancion;

/**
 * Verificación de acceso para una sesión ya emitida (token JWT, enlace de
 * acceso o WebSocket): bloquea cuentas desactivadas y con sanción de acceso, y
 * expone el alcance de una sanción restrictiva (por ejemplo, solo entregas).
 */
public interface AccesoUsuarioService {

    UserDetails loadUserForSession(String nombreUsuario) throws UsernameNotFoundException;

    Optional<AlcanceSancion> restriccionVigente(String nombreUsuario);
}
