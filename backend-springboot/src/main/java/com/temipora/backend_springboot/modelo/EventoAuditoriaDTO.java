package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;
import java.util.List;

public record EventoAuditoriaDTO(
        Long idEventoAuditoria,
        LocalDateTime ocurridoEn,
        Long actorId,
        String actorRol,
        String actorNombre,
        String accion,
        String entidad,
        String entidadId,
        String resumen,
        List<CambioAuditoriaDTO> cambios,
        String motivo,
        String requestId,
        String origen,
        String resultado) {
}
