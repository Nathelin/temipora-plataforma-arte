package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;

public record FiltroAuditoria(
        String actor,
        String origen,
        String entidad,
        String entidadId,
        String accion,
        String requestId,
        String texto,
        String categoria,
        Boolean critica,
        LocalDateTime desde,
        LocalDateTime hasta) {
}
