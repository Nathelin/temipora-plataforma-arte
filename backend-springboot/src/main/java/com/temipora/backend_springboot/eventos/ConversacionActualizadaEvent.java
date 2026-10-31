package com.temipora.backend_springboot.eventos;

import com.temipora.backend_springboot.modelo.EstadoConversacion;

public record ConversacionActualizadaEvent(
        Long idConversacion,
        EstadoConversacion estado,
        String nombreIniciador,
        String nombreDestinatario) {
}
