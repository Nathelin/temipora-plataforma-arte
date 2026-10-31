package com.temipora.backend_springboot.servicios;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.temipora.backend_springboot.eventos.ConversacionActualizadaEvent;
import com.temipora.backend_springboot.eventos.EscrituraEvent;
import com.temipora.backend_springboot.eventos.LeidoEvent;
import com.temipora.backend_springboot.eventos.MensajeEnviadoEvent;
import com.temipora.backend_springboot.modelo.EventoMensajeriaDTO;

@Component
public class MensajeriaPushListener {

    private static final String DESTINO = "/cola/mensajeria";

    private final SimpMessagingTemplate messagingTemplate;

    public MensajeriaPushListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT,
        fallbackExecution = true)
    public void alEnviarMensaje(MensajeEnviadoEvent evento) {
        enviar(evento.nombreIniciador(), EventoMensajeriaDTO.builder()
            .tipo(EventoMensajeriaDTO.TIPO_MENSAJE)
            .idConversacion(evento.idConversacion())
            .mensaje(evento.paraIniciador())
            .build());
        enviar(evento.nombreDestinatario(), EventoMensajeriaDTO.builder()
            .tipo(EventoMensajeriaDTO.TIPO_MENSAJE)
            .idConversacion(evento.idConversacion())
            .mensaje(evento.paraDestinatario())
            .build());
    }

    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT,
        fallbackExecution = true)
    public void alEscribir(EscrituraEvent evento) {
        enviar(evento.nombreDestinatario(), EventoMensajeriaDTO.builder()
            .tipo(EventoMensajeriaDTO.TIPO_ESCRITURA)
            .idConversacion(evento.idConversacion())
            .build());
    }

    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT,
        fallbackExecution = true)
    public void alLeer(LeidoEvent evento) {
        enviar(evento.nombreContraparte(), EventoMensajeriaDTO.builder()
            .tipo(EventoMensajeriaDTO.TIPO_LEIDO)
            .idConversacion(evento.idConversacion())
            .fechaLectura(evento.fechaLectura() != null
                ? evento.fechaLectura().toString()
                : null)
            .build());
    }

    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT,
        fallbackExecution = true)
    public void alActualizarConversacion(ConversacionActualizadaEvent evento) {
        EventoMensajeriaDTO payload = EventoMensajeriaDTO.builder()
            .tipo(EventoMensajeriaDTO.TIPO_CONVERSACION)
            .idConversacion(evento.idConversacion())
            .estado(evento.estado())
            .build();
        enviar(evento.nombreIniciador(), payload);
        enviar(evento.nombreDestinatario(), payload);
    }

    private void enviar(String nombreUsuario, EventoMensajeriaDTO evento) {
        if (nombreUsuario == null || nombreUsuario.isBlank()) {
            return;
        }
        messagingTemplate.convertAndSendToUser(
            nombreUsuario, DESTINO, evento);
    }
}
