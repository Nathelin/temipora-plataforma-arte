package com.temipora.backend_springboot.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import com.temipora.backend_springboot.modelo.AlcanceSancion;
import com.temipora.backend_springboot.servicios.CuentaSancionadaException;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String PREFIJO_SUBSCRIBIBLE = "/usuario/cola/";
    private static final String PREFIJO_ENVIO = "/app/";

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    public StompAuthChannelInterceptor(
            JwtUtils jwtUtils,
            UserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
            message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        StompCommand comando = accessor.getCommand();
        if (comando == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(comando)) {
            autenticar(accessor);
            return message;
        }

        if (StompCommand.SUBSCRIBE.equals(comando)
                || StompCommand.SEND.equals(comando)) {
            String destino = accessor.getDestination();
            String prefijo = StompCommand.SUBSCRIBE.equals(comando)
                ? PREFIJO_SUBSCRIBIBLE
                : PREFIJO_ENVIO;
            if (destino == null || !destino.startsWith(prefijo)) {
                throw new MessagingException("Destino no permitido.");
            }
        }

        return message;
    }

    private void autenticar(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null) {
            header = accessor.getFirstNativeHeader("authorization");
        }
        if (header == null || !header.startsWith("Bearer ")) {
            throw new MessagingException(
                "El token de autenticación es obligatorio.");
        }

        String token = header.substring(7);
        if (!jwtUtils.tokenEsValido(token)) {
            throw new MessagingException("El token es inválido o venció.");
        }

        String nombreUsuario = jwtUtils.obtenerNombreUsuarioToken(token);
        if (nombreUsuario == null) {
            throw new MessagingException("El token no tiene usuario.");
        }

        UserDetails userDetails;
        try {
            userDetails = userDetailsService instanceof AccesoUsuarioService acceso
                ? acceso.loadUserForSession(nombreUsuario)
                : userDetailsService.loadUserByUsername(nombreUsuario);
            if (userDetailsService instanceof AccesoUsuarioService acceso
                    && acceso.restriccionVigente(nombreUsuario)
                        .filter(alcance -> alcance == AlcanceSancion.ENTREGAS)
                        .isPresent()) {
                throw new MessagingException(
                    "La cuenta está restringida a entregas.");
            }
        } catch (CuentaSancionadaException e) {
            throw new MessagingException("La cuenta está inhabilitada.");
        } catch (MessagingException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new MessagingException("El usuario ya no existe.");
        }

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        accessor.setUser(authentication);
    }
}
