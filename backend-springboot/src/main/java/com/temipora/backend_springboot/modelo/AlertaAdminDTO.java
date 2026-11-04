package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;

public record AlertaAdminDTO(
        String codigo,
        String severidad,
        String titulo,
        String detalle,
        long cantidad,
        LocalDateTime desde,
        LocalDateTime hasta,
        String accion,
        String actorNombre) {
}
