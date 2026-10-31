package com.temipora.backend_springboot.servicios;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.temipora.backend_springboot.eventos.ConversacionActualizadaEvent;
import com.temipora.backend_springboot.eventos.EscrituraEvent;
import com.temipora.backend_springboot.eventos.LeidoEvent;
import com.temipora.backend_springboot.eventos.MensajeEnviadoEvent;
import com.temipora.backend_springboot.modelo.EstadoConversacion;
import com.temipora.backend_springboot.modelo.EventoMensajeriaDTO;
import com.temipora.backend_springboot.modelo.MensajePushDTO;

@ExtendWith(MockitoExtension.class)
class MensajeriaPushListenerTest {

    private static final String DESTINO = "/cola/mensajeria";

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private MensajeriaPushListener listener;

    @Test
    void alEnviarMensaje_mandaUnPayloadPorCadaParticipante() {
        listener.alEnviarMensaje(new MensajeEnviadoEvent(
            10L, "cliente", "artista", mensaje(true), mensaje(false)));

        verify(messagingTemplate).convertAndSendToUser(
            eq("cliente"), eq(DESTINO), any(EventoMensajeriaDTO.class));
        verify(messagingTemplate).convertAndSendToUser(
            eq("artista"), eq(DESTINO), any(EventoMensajeriaDTO.class));
    }

    @Test
    void alEscribir_mandaSoloALaContraparte() {
        listener.alEscribir(new EscrituraEvent(10L, "artista"));

        verify(messagingTemplate).convertAndSendToUser(
            eq("artista"), eq(DESTINO), any(EventoMensajeriaDTO.class));
    }

    @Test
    void alLeer_mandaSoloALaContraparte() {
        listener.alLeer(new LeidoEvent(10L, "artista", LocalDateTime.now()));

        verify(messagingTemplate).convertAndSendToUser(
            eq("artista"), eq(DESTINO), any(EventoMensajeriaDTO.class));
    }

    @Test
    void alActualizarConversacion_mandaAAMBOS() {
        listener.alActualizarConversacion(new ConversacionActualizadaEvent(
            10L, EstadoConversacion.ACEPTADA, "cliente", "artista"));

        verify(messagingTemplate).convertAndSendToUser(
            eq("cliente"), eq(DESTINO), any(EventoMensajeriaDTO.class));
        verify(messagingTemplate).convertAndSendToUser(
            eq("artista"), eq(DESTINO), any(EventoMensajeriaDTO.class));
    }

    @Test
    void sinNombreDeUsuario_noMandaNada() {
        listener.alEscribir(new EscrituraEvent(10L, null));

        verifyNoInteractions(messagingTemplate);
    }

    private MensajePushDTO mensaje(boolean propio) {
        return MensajePushDTO.builder()
            .idMensaje(1L)
            .contenido("Hola")
            .fechaEnvio(LocalDateTime.now().toString())
            .esPropio(propio)
            .build();
    }
}
