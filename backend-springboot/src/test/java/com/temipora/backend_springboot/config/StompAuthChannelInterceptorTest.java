package com.temipora.backend_springboot.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

@ExtendWith(MockitoExtension.class)
class StompAuthChannelInterceptorTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private StompAuthChannelInterceptor interceptor;

    private final MessageChannel channel = mock(MessageChannel.class);

    @Test
    void connectConTokenValido_autenticaLaSesion() {
        when(jwtUtils.tokenEsValido("token")).thenReturn(true);
        when(jwtUtils.obtenerNombreUsuarioToken("token")).thenReturn("cliente");
        when(userDetailsService.loadUserByUsername("cliente"))
            .thenReturn(userDetails());

        Message<?> resultado = interceptor.preSend(
            mensaje(StompCommand.CONNECT, "/", "Bearer token"), channel);

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
            resultado, StompHeaderAccessor.class);
        assertThat(accessor).isNotNull();
        assertThat(accessor.getUser()).isNotNull();
        assertThat(accessor.getUser().getName()).isEqualTo("cliente");
    }

    @Test
    void connectSinToken_lanzaError() {
        assertThatThrownBy(() -> interceptor.preSend(
                mensaje(StompCommand.CONNECT, "/", null), channel))
            .isInstanceOf(MessagingException.class)
            .hasMessageContaining("token");
    }

    @Test
    void connectConTokenInvalido_lanzaError() {
        when(jwtUtils.tokenEsValido("token")).thenReturn(false);

        assertThatThrownBy(() -> interceptor.preSend(
                mensaje(StompCommand.CONNECT, "/", "Bearer token"), channel))
            .isInstanceOf(MessagingException.class)
            .hasMessageContaining("inválido");
    }

    @Test
    void subscribeAPropioUsuario_esPermitido() {
        Message<?> message = mensajeConUsuario(
            StompCommand.SUBSCRIBE, "/usuario/cola/mensajeria");

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);
    }

    @Test
    void subscribeAOtroUsuario_esRechazado() {
        Message<?> message = mensajeConUsuario(
            StompCommand.SUBSCRIBE, "/usuario/otro/cola/mensajeria");

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
            .isInstanceOf(MessagingException.class)
            .hasMessageContaining("Destino no permitido");
    }

    @Test
    void sendAUnDestinoDeAplicacion_esPermitido() {
        Message<?> message = mensajeConUsuario(
            StompCommand.SEND, "/app/conversacion/10/escribiendo");

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);
    }

    @Test
    void sendAUnDestinoDelBroker_esRechazado() {
        Message<?> message = mensajeConUsuario(
            StompCommand.SEND, "/cola/mensajeria");

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
            .isInstanceOf(MessagingException.class)
            .hasMessageContaining("Destino no permitido");
    }

    private Message<byte[]> mensaje(
            StompCommand comando,
            String destino,
            String token) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(comando);
        accessor.setLeaveMutable(true);
        accessor.setDestination(destino);
        if (token != null) {
            accessor.setNativeHeader("Authorization", token);
        }
        return MessageBuilder.createMessage(
            new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> mensajeConUsuario(
            StompCommand comando,
            String destino) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(comando);
        accessor.setLeaveMutable(true);
        accessor.setDestination(destino);
        accessor.setUser(new UsernamePasswordAuthenticationToken(
            "cliente", null,
            List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"))));
        return MessageBuilder.createMessage(
            new byte[0], accessor.getMessageHeaders());
    }

    private UserDetails userDetails() {
        return new User(
            "cliente",
            "secreto",
            List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")));
    }
}
