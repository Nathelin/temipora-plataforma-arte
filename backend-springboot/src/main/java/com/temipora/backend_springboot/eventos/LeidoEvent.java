package com.temipora.backend_springboot.eventos;

import java.time.LocalDateTime;

public record LeidoEvent(
        Long idConversacion,
        String nombreContraparte,
        LocalDateTime fechaLectura) {
}
