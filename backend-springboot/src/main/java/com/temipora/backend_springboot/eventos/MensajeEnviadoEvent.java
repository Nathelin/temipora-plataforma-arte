package com.temipora.backend_springboot.eventos;

import com.temipora.backend_springboot.modelo.MensajePushDTO;

public record MensajeEnviadoEvent(
        Long idConversacion,
        String nombreIniciador,
        String nombreDestinatario,
        MensajePushDTO paraIniciador,
        MensajePushDTO paraDestinatario) {
}
