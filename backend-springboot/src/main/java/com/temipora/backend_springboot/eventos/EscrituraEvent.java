package com.temipora.backend_springboot.eventos;

public record EscrituraEvent(
        Long idConversacion,
        String nombreDestinatario) {
}
